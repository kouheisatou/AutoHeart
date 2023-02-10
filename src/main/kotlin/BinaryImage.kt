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
    val representativePixel: Vector?
    val weightMap = Array(width) { Array(height) { 0 } }
    var maxWeight = 0
    val weightMapAlphaImage = BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)

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
    fun calcCorrectWeightAverage(flippedBinaryImage: BinaryImage): Double {
        representativePixel!!
        flippedBinaryImage.representativePixel!!

        val weightMap = Array(width) { Array(height) { 0 } }
        for (whitePixel in whitePixels) {
            for (flippedBinaryImageWhitePixel in flippedBinaryImage.whitePixels) {
                val x = whitePixel.x - flippedBinaryImage.representativePixel.x + flippedBinaryImageWhitePixel.x
                val y = whitePixel.y - flippedBinaryImage.representativePixel.y + flippedBinaryImageWhitePixel.y
                if (x in 0 until width && y in 0 until height) {
                    weightMap[x][y]++
                }
            }
        }

        var weightSum = 0
        for (x in weightMap.indices) {
            for (y in weightMap[x].indices) {
                weightSum += weightMap[x][y]
            }
        }
        return weightSum.toDouble() / (width * height).toDouble()
    }

    suspend fun find(templateImage: BinaryImage): List<SearchResult> {
        if (templateImage.representativePixel == null) throw ImageConversionException("No representativePixel")
        val flippedImage = templateImage.flipped()
        flippedImage.representativePixel!!

        // 正しい画像が検出された時の重み平均を計算
        val correctWeightAverage = calcCorrectWeightAverage(flippedImage)

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
            for (y in weightMap[x].indices) {

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
                if (weightMap[x][y].toDouble() / templateImage.whitePixels.size < Settings.detectionThreshold.value) continue

                val templateImageCoordinateX = x - (templateImage.width - templateImage.representativePixel.x)
                val templateImageCoordinateY = y - (templateImage.height - templateImage.representativePixel.y)
                // 走査位置が画像の外にある時除外
                if (!(templateImageCoordinateX in 0 until width && templateImageCoordinateY in 0 until height)) continue

                var alreadyRegistered = false
                for (coordinate in results) {
                    if ((templateImageCoordinateX * 2 + templateImage.width) / 2 in coordinate.x..coordinate.x + templateImage.width && (templateImageCoordinateY * 2 + templateImage.height) / 2 in coordinate.y..coordinate.y + templateImage.height) {
                        alreadyRegistered = true
                    }
                }
                // 検出範囲が被っていたら除外
                if (alreadyRegistered) continue

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

                val result = SearchResult(
                    Settings.getNewBoundingBoxId(),
                    templateImageCoordinateX,
                    templateImageCoordinateY,
                    templateImage.width,
                    templateImage.height,
                    weightMap[x][y].toDouble(),
                    weightMap[x][y].toDouble() / templateImage.whitePixels.size,
                    weightAvg,
                    x,
                    y,
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