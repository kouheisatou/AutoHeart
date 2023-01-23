class Vector(val x: Int, val y: Int) {
    override fun equals(other: Any?): Boolean {
        return other is Vector && other.x == x && other.y == y
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        return result
    }

    operator fun minus(coordinate: Vector): Vector {
        return Vector(x - coordinate.x, y - coordinate.y)
    }

    operator fun plus(coordinate: Vector): Vector {
        return Vector(x + coordinate.x, y + coordinate.y)
    }
}