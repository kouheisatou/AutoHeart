import Application.settings
import java.awt.image.BufferedImage
import kotlin.math.abs

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

fun BufferedImage.edge(): BufferedImage {
    if (type != BufferedImage.TYPE_BYTE_GRAY) {
        throw ImageConversionException("unsupported image color type : $type")
    }

    val newImage = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (x != width - 1) {
                val steep = getRGB(x + 1, y) - getRGB(x, y)
                newImage.setRGB(x, y, abs(steep))
            } else {
                newImage.setRGB(x, y, 0)
            }
        }
    }
    for (x in 0 until width) {
        for (y in 0 until height) {
            if (y != height - 1) {
                val steep = getRGB(x, y + 1) - getRGB(x, y)
                newImage.setRGB(x, y, abs(steep))
            } else {
                newImage.setRGB(x, y, 0)
            }
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

    var max1Index: Int? = null
    var max2Index: Int? = null
    for (i in distribution.indices) {
        if (max1Index == null || distribution[max1Index] < distribution[i]) {
            max1Index = i
        }
    }
    for (i in distribution.indices) {
        if (i == max1Index) continue
        if (max2Index == null || distribution[max2Index] < distribution[i] && distribution[i] < distribution[max1Index!!]) {
            max2Index = i
        }
    }
//    println(distribution.toList())
//    println(max1Index)
//    println(max2Index)
    return (max1Index!! + max2Index!!) / 2
}

fun BufferedImage.flipXY(): BufferedImage {
    val result = BufferedImage(width, height, type)

    for (x in 0 until width) {
        for (y in 0 until height) {
            result.setRGB(width - 1 - x, height - 1 - y, getRGB(x, y))
        }
    }
    return result
}

class ImageConversionException(msg: String) : Exception(msg)