import kotlinx.serialization.Serializable
import java.lang.Integer.max
import java.lang.Integer.min
import kotlin.math.abs

@Serializable
class Area(){

    var startX: Int = 0
    var startY: Int = 0
    var endX: Int = 0
    var endY: Int = 0
    val width: Int
        get() = abs(endX - startX)
    val height: Int
        get() = abs(endY - startY)

    constructor(startX: Int, startY: Int, endX: Int, endY: Int) : this(){
        this.startX = min(startX, endX)
        this.startY = min(startY, endY)
        this.endX = if(abs(startX - endX) == 0){ startX + 1 }else{ max(startX, endX) }
        this.endY = if(abs(startY - endY) == 0){ startY + 1 }else{ max(startY, endY) }
    }

    fun isInside(x: Int, y: Int): Boolean{
        return (x in startX..endX) && (y in startY..endY)
    }

    override fun toString(): String {
        return "Area(startX=$startX, startY=$startY, endX=$endX, endY=$endY)"
    }

}
