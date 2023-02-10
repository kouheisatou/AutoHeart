class SearchResult(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val weight: Double,
    val representativePointX: Int,
    val representativePointY: Int,
) {
    override fun toString(): String {
        return "boundingBox=(x=${x} y=${y} width=${width} height=${height}) representativePoint=(${representativePointX},${representativePointY}) weight=${weight}"
    }
}