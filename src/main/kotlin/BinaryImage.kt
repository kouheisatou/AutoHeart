import Application.settings
import java.awt.image.BufferedImage

const val BLACK = false
const val WHITE = true
const val BLACK_3BYTE = 0x000000
const val WHITE_3BYTE = 0xffffff

class BinaryImage(bufferedImage: BufferedImage, threshold: Int = bufferedImage.calcBinalizeThreshold()) {


    private val bitmap = Array(bufferedImage.width * bufferedImage.height) { BLACK }
    val whitePixels: Array<BinaryPixel>
    val representativePixel: BinaryPixel

    val width = bufferedImage.width
    val height = bufferedImage.height

    init {
        val edgedImage = bufferedImage.grayScale().edge()
        val whitePixels = mutableListOf<BinaryPixel>()
        var representativePixel: BinaryPixel? = null

        for (x in 0 until edgedImage.width) {
            for (y in 0 until edgedImage.height) {
                val color = edgedImage.getRGB(x, y)
                val r = (color shr 16) and 0xff
                val g = (color shr 8) and 0xff
                val b = color and 0xff
                if ((r + g + b) / 3 > threshold) {
                    if (representativePixel == null) {
                        representativePixel = BinaryPixel(Vector(x, y), WHITE, null)
                    }

                    setColorAt(x, y, WHITE)
                    whitePixels.add(BinaryPixel(Vector(x, y), WHITE, representativePixel))
                }
            }
        }


        this.whitePixels = whitePixels.toTypedArray()
        this.representativePixel = representativePixel!!
    }

    fun getColorAt(x: Int, y: Int): Boolean {
        return bitmap[y * width + x]
    }
    fun getColorAt(coordinate: Vector): Boolean{
        return getColorAt(coordinate.x, coordinate.y)
    }

    fun setColorAt(x: Int, y: Int, color: Boolean) {
        bitmap[y * width + x] = color
    }
    fun setColorAt(coordinate: Vector, color: Boolean){
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

    fun find(
        templateImage: BinaryImage,
        currentSearchCoordinateChanged: ((coordinate: Vector) -> Unit)? = null,
        onFindOut: ((coordinate: Vector) -> Unit)? = null,
        onSearchFinished: ((result: List<Vector>) -> Unit)? = null,
    ) {
        val matchedPixels = mutableListOf<BinaryPixel>()
        for (whitePixel in whitePixels) {
            currentSearchCoordinateChanged?.invoke(whitePixel.coordinate)
            var pixelMatchErrorCount = 0
            for(templateWhitePixel in templateImage.whitePixels.withIndex()){

                val representativePixelCoordinate = whitePixel.coordinate - templateWhitePixel.value.relativeVectorFromRepresentativePixel
                if(representativePixelCoordinate.x < 0 || representativePixelCoordinate.y < 0) continue
                if(getColorAt(representativePixelCoordinate) != templateImage.getColorAt(templateImage.representativePixel.coordinate)){
                    pixelMatchErrorCount++
                    if(pixelMatchErrorCount > settings.imageMatchingThreshold){
                        break
                    }

                    if(templateWhitePixel.index == templateImage.whitePixels.size-1){
                        matchedPixels.add(whitePixel)
                        onFindOut?.invoke(whitePixel.coordinate)
                    }
                }
            }
        }

        var xSum = 0
        var ySum = 0
        for(pixel in matchedPixels){
            xSum += pixel.coordinate.x
            ySum += pixel.coordinate.y
        }
        // 1つしか画像内で一致したtemplateを認識できない
        onSearchFinished?.invoke(listOf(Vector(xSum/matchedPixels.size, ySum/matchedPixels.size)))
    }
}
