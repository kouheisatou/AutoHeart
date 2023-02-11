import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.abs

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
    val representativePixel: Vector?
    val weightMap = Array(width) { Array(height) { 0 } }
    var maxWeight = 0
    val weightMapAlphaImage = BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)
    var boundingBoxCount = 0L

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
        this.representativePixel = whitePixels.getOrNull(0)
    }

    fun getColorAt(x: Int, y: Int): Boolean {
        return bitmap[y * width + x]
    }

    fun setColorAt(x: Int, y: Int, color: Boolean) {
        bitmap[y * width + x] = color
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

    fun flipped(): BinaryImage {

        val copiedBitmap = Array(width * height) { BLACK }

        for (x in 0 until width) {
            for (y in 0 until height) {
                copiedBitmap[y * width + x] = getColorAt(width - 1 - x, height - 1 - y)
            }
        }
        return BinaryImage(width, height, copiedBitmap)
    }

    // 同じ画像を反転して重ね合わせて、画像内で正しいtemplateが検出された時の重み平均の値をあらかじめ算出する
    fun calcSteepThreshold(): Double {
        representativePixel!!

        val flippedBinaryImage = flipped()
        flippedBinaryImage.representativePixel!!

        var maxWeight = 0
        var maxWeightX = 0
        var maxWeightY = 0
        val weightMap = Array(width) { Array(height) { 0 } }
        for (whitePixel in whitePixels) {
            for (flippedBinaryImageWhitePixel in flippedBinaryImage.whitePixels) {
                val x = whitePixel.x - flippedBinaryImage.representativePixel.x + flippedBinaryImageWhitePixel.x
                val y = whitePixel.y - flippedBinaryImage.representativePixel.y + flippedBinaryImageWhitePixel.y
                if (x in 0 until width && y in 0 until height) {
                    weightMap[x][y]++
                    if(maxWeight < weightMap[x][y]){
                        maxWeight = weightMap[x][y]
                        maxWeightX = x
                        maxWeightY = y
                    }
                }
            }
        }

        val d = Settings.steepDelta
        var steepSum = 0.0
        var availableSteepValueCount = 0
        if(maxWeightX + d in 0 until width){
            println(abs((weightMap[maxWeightX+d][maxWeightY] - weightMap[maxWeightX][maxWeightY]).toDouble()/d))
            steepSum += abs((weightMap[maxWeightX+d][maxWeightY] - weightMap[maxWeightX][maxWeightY]).toDouble()/d)
            availableSteepValueCount++
        }
        if(maxWeightX - d in 0 until width){
            steepSum += abs((weightMap[maxWeightX][maxWeightY] - weightMap[maxWeightX-d][maxWeightY]).toDouble()/d)
            availableSteepValueCount++
        }
        if(maxWeightY + d in 0 until height){
            steepSum += abs((weightMap[maxWeightX][maxWeightY+d] - weightMap[maxWeightX][maxWeightY]).toDouble()/d)
            availableSteepValueCount++
        }
        if(maxWeightY - d in 0 until height){
            steepSum += abs((weightMap[maxWeightX][maxWeightY] - weightMap[maxWeightX][maxWeightY-d]).toDouble()/d)
            availableSteepValueCount++
        }
        return steepSum/availableSteepValueCount
    }

    suspend fun find(templateImage: BinaryImage, detectionThreshold: Double, steepThreshold: Double, steepThresholdAllowance: Double): List<SearchResult> {
        if (templateImage.representativePixel == null) throw ImageConversionException("No representativePixel")
        val flippedImage = templateImage.flipped()
        flippedImage.representativePixel!!

        val results = mutableListOf<SearchResult>()

        // row:y, column:x
        for (whitePixel in whitePixels.withIndex()) {

            for (templateWhitePixel in flippedImage.whitePixels) {
                val x = whitePixel.value.x - flippedImage.representativePixel.x + templateWhitePixel.x
                val y = whitePixel.value.y - flippedImage.representativePixel.y + templateWhitePixel.y
                if ((y in 0 until height) && (x in 0 until width)) {
                    weightMap[x][y]++
                    if (maxWeight < weightMap[x][y]) {
                        maxWeight = weightMap[x][y]
                    }
                }
            }
        }

        for (x in weightMap.indices) {
            pixels@ for (y in weightMap[x].indices) {

                // 重みアルファマップに書き込み
                val alpha = if (maxWeight != 0) {
                    0xff * weightMap[x][y] / maxWeight
                } else {
                    0x00
                }
                val red = 0x00
                val green = 0xff
                val blue = 0x00
                val color = (alpha shl 24) + (red shl 16) + (green shl 8) + blue
                weightMapAlphaImage.setRGB(x, y, color)

                // 重みが閾値を下回っていたら除外
                if (weightMap[x][y].toDouble() / templateImage.whitePixels.size < detectionThreshold) continue

                // 上下左右の重みの傾きの平均値を求める
                val d = Settings.steepDelta
                var steepSum = 0.0
                var availableSteepValueCount = 0
                if(x + d in 0 until width){
                    steepSum += abs((weightMap[x+d][y] - weightMap[x][y]).toDouble()/d)
                    availableSteepValueCount++
                }
                if(x - d in 0 until width){
                    steepSum += abs((weightMap[x][y] - weightMap[x-d][y]).toDouble()/d)
                    availableSteepValueCount++
                }
                if(y + d in 0 until height){
                    steepSum += abs((weightMap[x][y+d] - weightMap[x][y]).toDouble()/d)
                    availableSteepValueCount++
                }
                if(y - d in 0 until height){
                    steepSum += abs((weightMap[x][y] - weightMap[x][y-d]).toDouble()/d)
                    availableSteepValueCount++
                }
                val steepAverage = steepSum / availableSteepValueCount
                // 重みの傾きが事前に計算した正しい傾きより大幅に小さければ除外
                if(steepThreshold - steepAverage > steepThresholdAllowance) continue

                val templateImageCoordinateX = x - (templateImage.width - templateImage.representativePixel.x)
                val templateImageCoordinateY = y - (templateImage.height - templateImage.representativePixel.y)
                // template画像の左上端が画像の外にある時除外
                if (!(templateImageCoordinateX in 0 until width && templateImageCoordinateY in 0 until height)) continue

                // 重み平均で判定を除外する
                /**
                var weightSum = 0
                for (dx in 0 until templateImage.width) {
                    for (dy in 0 until templateImage.height) {
                        if (templateImageCoordinateX + dx in 0 until width && templateImageCoordinateY + dy in 0 until height) {
                            weightSum += weightMap[templateImageCoordinateX + dx][templateImageCoordinateY + dy]
                        }
                    }
                }
                val weightAvg = weightSum.toDouble() / (templateImage.width * templateImage.height).toDouble()
                // 正しいweightAverageよりも大幅に大きかったら除外
                if (weightAvg >= correctWeightAverage + Settings.weightAverageThreshold.value) continue
                **/

                // すでにboundingBoxがある場合除外
                for (alreadyRegisteredResult in results) {
                    val centerX = (templateImageCoordinateX * 2 + templateImage.width) / 2
                    val centerY = (templateImageCoordinateY * 2 + templateImage.height) / 2
                    if(centerX in alreadyRegisteredResult.x .. alreadyRegisteredResult.x+alreadyRegisteredResult.width && centerY in alreadyRegisteredResult.y .. alreadyRegisteredResult.y + alreadyRegisteredResult.height){
                        continue@pixels
                    }
                }

                val result = SearchResult(
                    boundingBoxCount++,
                    templateImageCoordinateX,
                    templateImageCoordinateY,
                    templateImage.width,
                    templateImage.height,
                    weightMap[x][y].toDouble(),
                    weightMap[x][y].toDouble() / templateImage.whitePixels.size,
                    x,
                    y,
                    steepAverage,
                )
                results.add(result)
                println(result)
            }
        }
        return results
    }
}

fun convertBufferedImageToBinaryImage(
    bufferedImage: BufferedImage,
    threshold: Int
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