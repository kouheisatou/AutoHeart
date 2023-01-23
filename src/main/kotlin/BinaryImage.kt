import java.awt.image.BufferedImage

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

    fun clone(): BinaryImage {
        val copyBitmap = Array(width * height) { BLACK }
        for (i in bitmap.indices) {
            copyBitmap[i] = bitmap[i]
        }
        return BinaryImage(width, height, copyBitmap)
    }

    private fun flipped(): BinaryImage {
        val copy = clone()

        for (x in 0 until width) {
            for (y in 0 until height) {
                copy.setColorAt(width - 1 - x, height - 1 - y, getColorAt(x, y))
            }
        }
        return copy
    }

    fun find(
        templateImage: BinaryImage,
        currentSearchCoordinateChanged: ((coordinate: Vector, percentage: Int) -> Unit)? = null,
        onSearchFinished: ((result: Array<Array<Int>>) -> Unit)? = null,
    ) {
        templateImage.flipped()

        // row:y, column:x
        val result = Array(height) { Array(width) { 0 } }
        for (whitePixel in whitePixels.withIndex()) {
            currentSearchCoordinateChanged?.invoke(whitePixel.value, whitePixel.index * 100 / whitePixels.size)

            for (templateWhitePixel in templateImage.whitePixels) {
                val x = whitePixel.value.x - templateImage.representativePixel.x + templateWhitePixel.x
                val y = whitePixel.value.y - templateImage.representativePixel.y + templateWhitePixel.y
                if ((y in 0 until height) && (x in 0 until width)) {
                    result[y][x]++
                }
            }
        }

        onSearchFinished?.invoke(result)
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