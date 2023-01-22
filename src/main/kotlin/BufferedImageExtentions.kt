import Application.settings
import java.awt.image.BufferedImage
import java.io.File
import java.lang.Math.pow
import java.lang.StringBuilder
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

fun BufferedImage.binalized(threshold: Int = calcBinalizeThreshold()): BufferedImage {

    if (threshold < 0x00 || threshold > 0xff) {
        throw ImageConversionException("invalid threshold : $threshold")
    }
    if (type != BufferedImage.TYPE_BYTE_GRAY) {
        throw ImageConversionException("unsupported image color type : $type")
    }

    val newImage = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
    for (x in 0 until width) {
        for (y in 0 until height) {
            val color = getRGB(x, y)
            val r = (color shr 16) and 0xff
            val g = (color shr 8) and 0xff
            val b = color and 0xff
            val newColor = if ((r + g + b) / 3 < threshold) {
                0x000000
            } else {
                0xffffff
            }
            newImage.setRGB(x, y, newColor)
        }
    }
    return newImage
}

fun BufferedImage.grayScale(): BufferedImage {
    if (type != BufferedImage.TYPE_4BYTE_ABGR) {
        throw ImageConversionException("unsupported image color type : $type")
    }

    val newImage = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
    for (x in 0 until this.width) {
        for (y in 0 until this.height) {
            val color = getRGB(x, y)
            val r = (color shr 16) and 0xff
            val g = (color shr 8) and 0xff
            val b = color and 0xff
            newImage.setRGB(
                x, y,
                ((r + g + b) / 3 shl 16)
                        + ((r + g + b) / 3 shl 8)
                        + ((r + g + b) / 3)
            )
        }
    }
    return newImage
}

fun BufferedImage.calcBinalizeThreshold(): Int {
    if (type != BufferedImage.TYPE_BYTE_GRAY) {
        throw ImageConversionException("unsupported image color type : $type")
    }

    val distribution = Array(256) { 0 }
    for (x in 0 until width) {
        for (y in 0 until height) {
            val level = getRGB(x, y) and 0xff
//            println(String.format("%x", getRGB(x, y)))
            distribution[level]++
        }
    }

    var max1Index = 0
    var max1 = distribution[0]
    var max2Index = 0
    for (i in distribution.indices) {
        if (max1 < distribution[i]) {
            max2Index = max1Index
            max1 = distribution[i]
            max1Index = i
        }
    }
    //    println(distribution.toList())
    return (max1Index + max2Index) / 2
}

suspend fun BufferedImage.find(
    target: BufferedImage,
    currentSearchCoordinateChanged: ((coordinate: Pair<Int, Int>) -> Unit)? = null,
    onYChanged: ((y: Int, height: Int) -> Unit)? = null,
    onSearchFinished: ((rMap: BufferedImage) -> Unit)? = null,
) {
    if (type != BufferedImage.TYPE_BYTE_GRAY || target.type != BufferedImage.TYPE_BYTE_GRAY) {
        throw ImageConversionException("unsupported image color type : $type")
    }

    val rMap = BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)
    val r = Array(height) { Array(width) { 0.0 } }
    var minR: Double? = null
    var maxR: Double? = null

    for (y in 0 until height) {
        onYChanged?.invoke(y, height)
        for (x in 0 until width) {

            // search out of bounds
            if (x + target.width > width || y + target.height > height) {
                continue
            }
            currentSearchCoordinateChanged?.invoke(Pair(x, y))

            var imageSum = 0
            var squaredImageSum = 0
            var targetSum = 0
            var squaredTargetSum = 0
            var covariance = 0
            for (targetY in 0 until target.height) {
                for (targetX in 0 until target.width) {
                    val imageColor = getRGB(x + targetX, y + targetY)
                    val targetColor = target.getRGB(targetX, targetY)
                    imageSum += imageColor
                    squaredImageSum += imageColor * imageColor
                    targetSum += targetColor
                    squaredTargetSum += targetColor * targetColor
                    covariance += imageColor * targetColor
                }
            }
            r[y][x] =
                (target.width * target.height * covariance - imageSum * targetSum) / sqrt(
                    (
                            target.width.toDouble() * target.height.toDouble() * squaredImageSum.toDouble() - imageSum.toDouble().pow(2.0)
                            ) * (target.width.toDouble() * target.height.toDouble() * squaredTargetSum.toDouble() - targetSum.toDouble().pow(2.0))
                )
            r[y][x]=if(r[y][x].isNaN()){0.0}else{r[y][x]}

            if (minR == null || minR > r[y][x]) {
                minR = r[y][x]
            }
            if (maxR == null || maxR < r[y][x]) {
                maxR = r[y][x]
            }
        }
    }

    val csvString = StringBuilder()
    for(y in r.indices){
        for(x in 0 until r[y].size){
            val alpha = ((r[y][x] - minR!!) / abs(minR - maxR!!) * 0xff).toInt()
            val color = alpha shl 24
            rMap.setRGB(x, y, color)

            csvString.append(alpha)
            if(x != r[y].size-1){
                csvString.append(",")
            }else{
                csvString.append("\n")
            }
        }
    }

    File("./out.csv").writeText(csvString.toString())
    onSearchFinished?.invoke(rMap)
}

class ImageConversionException(msg: String) : Exception(msg)