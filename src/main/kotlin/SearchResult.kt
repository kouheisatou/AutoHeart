class SearchResult(
    val id: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val weight: Double,
    val weightAverage: Double,
    val representativePointX: Int,
    val representativePointY: Int,
) {
    override fun toString(): String {
        return "id=$id boundingBox=(x=${x} y=${y} width=${width} height=${height}) representativePoint=(${representativePointX},${representativePointY}) weight=${weight} weightAvg=$weightAverage"
    }
}