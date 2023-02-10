class SearchResult(
    val id: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val weightMax: Double,
    val weightMaxRatio: Double,// 画像全体の重みの最大値を1とした時のこの重みの割合
    val weightAverage: Double,
    val representativePointX: Int,
    val representativePointY: Int,
) {
    override fun toString(): String {
        return "id=$id boundingBox=(x=${x} y=${y} width=${width} height=${height}) representativePoint=(${representativePointX},${representativePointY}) weightMax=${weightMax} weightMaxRatio=$weightMaxRatio weightAvg=$weightAverage"
    }
}