import java.awt.image.BufferedImage

const val BLACK = false
const val WHITE = true
const val BLACK_3BYTE = 0x000000
const val WHITE_3BYTE = 0xffffff

open class BinaryImage(bufferedImage: BufferedImage, threshold: Int = bufferedImage.calcBinalizeThreshold()) {


    private val bitmap = Array(bufferedImage.width * bufferedImage.height) { BLACK }
    val whitePixels: Array<Vector>
    val representativePixel: Vector

    val width = bufferedImage.width
    val height = bufferedImage.height

    init {
        val edgedImage = bufferedImage.grayScale().edge()
        val whitePixels = mutableListOf<Vector>()
        var representativePixel: Vector? = null

        for (x in 0 until edgedImage.width) {
            for (y in 0 until edgedImage.height) {
                val color = edgedImage.getRGB(x, y)
                val r = (color shr 16) and 0xff
                val g = (color shr 8) and 0xff
                val b = color and 0xff
                if ((r + g + b) / 3 > threshold) {
                    if (representativePixel == null) {
                        representativePixel = Vector(x, y)
                    }

                    setColorAt(x, y, WHITE)
                    whitePixels.add(Vector(x, y))
                }
            }
        }


        this.whitePixels = whitePixels.toTypedArray()
        this.representativePixel = representativePixel!!
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

    fun rotate180() {
        for (x in 0 until width) {
            for (y in 0 until height) {
                val temp = getColorAt(x, y)
                setColorAt(x, y, getColorAt(width - 1 - x, height - 1 - y))
                setColorAt(width - 1 - x, height - 1 - y, temp)
            }
        }
    }

    fun find(
        templateImage: BinaryImage,
        currentSearchCoordinateChanged: ((coordinate: Vector, percentage: Int) -> Unit)? = null,
        onSearchFinished: ((result: Array<Array<Int>>) -> Unit)? = null,
    ) {
        templateImage.rotate180()

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
