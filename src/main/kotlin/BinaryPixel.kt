class BinaryPixel(val coordinate: Vector, val color: Boolean, val representativePixel: BinaryPixel?) {

    val relativeVectorFromRepresentativePixel: Vector = if(representativePixel == null){
        Vector(0, 0)
    }else{
        coordinate - representativePixel.coordinate
    }

    override fun toString(): String {
        return "BinaryPixel(coordinate=$coordinate, color=$color, representativePixel=$representativePixel, relativeVectorFromRepresentativePixel=$relativeVectorFromRepresentativePixel)"
    }
}