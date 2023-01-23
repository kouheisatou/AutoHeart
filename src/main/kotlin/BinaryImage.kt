import Application.settings
import java.awt.image.BufferedImage
import java.io.File

const val BLACK = false
const val WHITE = true
const val BLACK_3BYTE = 0x000000
const val WHITE_3BYTE = 0xffffff

open class BinaryImage(
    val width: Int,
    val height: Int,
    private val bitmap: Array<Boolean> = Array(width * height) { BLACK }
) {

    val whitePixels: Array<Vector>
    val representativePixel: Vector

    init {
        val whitePixels = mutableListOf<Vector>()
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (getColorAt(x, y) == WHITE) {
                    whitePixels.add(Vector(x, y))
                }
            }
        }
        this.whitePixels = whitePixels.toTypedArray()
        this.representativePixel = whitePixels[0]
    }

    fun getColorAt(x: Int, y: Int): Boolean {
        return bitmap[y * width + x]
    }

    fun getColorAt(coordinate: Vector): Boolean {
        return getColorAt(coordinate.x, coordinate.y)
    }

    fun setColorAt(x: Int, y: Int, color: Boolean) {
        bitmap[y * width + x] = color
    }

    fun setColorAt(coordinate: Vector, color: Boolean) {
        setColorAt(coordinate.x, coordinate.y, color)
    }

    fun toBufferedImage(): BufferedImage {
        val bi = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (getColorAt(x, y) == WHITE) {
                    bi.setRGB(x, y, WHITE_3BYTE)
                } else {
                    bi.setRGB(x, y, BLACK_3BYTE)
                }
            }
        }
        return bi
    }

    private fun flipped(): BinaryImage {

        val copiedBitmap = Array(width * height) { BLACK }

        for (x in 0 until width) {
            for (y in 0 until height) {
                copiedBitmap[y * width + x] = getColorAt(width - 1 - x, height - 1 - y)
            }
        }
        return BinaryImage(width, height, copiedBitmap)
    }

    fun find(
        templateImage: BinaryImage,
        currentSearchCoordinateChanged: ((coordinate: Vector, percentage: Int) -> Unit)? = null,
        onSearchFinished: ((result: List<Vector>, weightMapAlphaImage: BufferedImage) -> Unit)? = null,
    ) {
        val flippedImage = templateImage.flipped()

        var maxWeight = 0
        val result = mutableListOf<Vector>()

        // row:y, column:x
        val weightMap = Array(height) { Array(width) { 0 } }
        for (whitePixel in whitePixels.withIndex()) {
            currentSearchCoordinateChanged?.invoke(whitePixel.value, whitePixel.index * 100 / whitePixels.size)

            for (templateWhitePixel in flippedImage.whitePixels) {
                val x = whitePixel.value.x - flippedImage.representativePixel.x + templateWhitePixel.x
                val y = whitePixel.value.y - flippedImage.representativePixel.y + templateWhitePixel.y
                if ((y in 0 until height) && (x in 0 until width)) {
                    weightMap[y][x]++
                    if (maxWeight < weightMap[y][x]) {
                        maxWeight = weightMap[y][x]
                    }
                }
            }
        }

        val weightMapAlphaImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (y in weightMap.indices) {
            for (x in weightMap[y].indices) {

                val alpha = 0xff * weightMap[y][x] / maxWeight
                val red = 0x00
                val green = 0xff
                val blue = 0x00
                val color = (alpha shl 24) + (red shl 16) + (green shl 8) + blue
                weightMapAlphaImage.setRGB(x,y,color)

                if (weightMap[y][x].toDouble() / maxWeight > settings.detectionAccuracy) {
                    val templateImageCoordinateX = x - (templateImage.width - templateImage.representativePixel.x)
                    val templateImageCoordinateY = y - templateImage.representativePixel.y
                    if (templateImageCoordinateX in 0 until width && templateImageCoordinateY in 0 until height) {
                        result.add(Vector(templateImageCoordinateX, templateImageCoordinateY))
                        println(result.last())
                    }
                }
            }
        }
        onSearchFinished?.invoke(result, weightMapAlphaImage)
    }
}

fun convertBufferedImageToBinaryImage(
    bufferedImage: BufferedImage,
    threshold: Int = bufferedImage.calcBinalizeThreshold()
): BinaryImage {

    val bitMap = Array(bufferedImage.width * bufferedImage.height) { BLACK }
    val edgedImage = bufferedImage.grayScale().edge()

    for (x in 0 until edgedImage.width) {
        for (y in 0 until edgedImage.height) {
            val color = edgedImage.getRGB(x, y)
            val r = (color shr 16) and 0xff
            val g = (color shr 8) and 0xff
            val b = color and 0xff
            if ((r + g + b) / 3 > threshold) {
                bitMap[y * bufferedImage.width + x] = WHITE
            }
        }
    }

    return BinaryImage(bufferedImage.width, bufferedImage.height, bitMap)
}