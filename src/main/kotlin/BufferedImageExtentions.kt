import Application.settings
import java.awt.image.BufferedImage

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
    onFindOut: ((coordinate: Pair<Int, Int>) -> Unit)? = null,
    onSearchFinished: ((List<Pair<Int, Int>>) -> Unit)? = null,
) {
    if (type != BufferedImage.TYPE_BYTE_GRAY || target.type != BufferedImage.TYPE_BYTE_GRAY) {
        throw ImageConversionException("unsupported image color type : $type")
    }

    val result = mutableListOf<Pair<Int, Int>>()

    for (x in 0 until width) {
        for (y in 0 until height) {

            // search out of bounds
            if (x + target.width > width || y + target.height > height) {
                continue
            }

            var incorrectCount = 0
            targetLoop@ for (targetX in 0 until target.width) {
                for (targetY in 0 until target.height) {

                    currentSearchCoordinateChanged?.invoke(Pair(x + targetX, y + targetY))

                    // unmatched pixel
                    if (getRGB(x + targetX, y + targetY) != target.getRGB(targetX, targetY)) {
                        incorrectCount++
                        if (incorrectCount > settings.imageMatchingThreshold) {
                            break@targetLoop
                        }
                    }

                    // matched all
                    if (targetX == target.width - 1 && targetY == target.height - 1) {
                        result.add(Pair(x, y))
                        onFindOut?.invoke(Pair(x, y))
                    }
                }
            }
        }
    }

    onSearchFinished?.invoke(result)
}

class ImageConversionException(msg: String) : Exception(msg)