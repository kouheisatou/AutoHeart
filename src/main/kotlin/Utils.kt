import java.awt.Component
import java.awt.GraphicsConfiguration
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.io.File


fun <T> arrayToCSV(array: Array<Array<T>>, outputFile: File) {
    outputFile.writeText("")
    for (row in array.indices) {
        for (col in 0 until array[row].size) {
            outputFile.appendText("${array[row][col]}${if(col!=array[row].size-1){","}else{"\n"}}")
        }
    }
}

fun tench(array: Array<Array<Int>>): Array<Array<Int>>{
    val result = Array(array[0].size){Array(array.size){0} }
    for(x in array.indices){
        for(y in array[x].indices){
            result[y][x] = array[x][y]
        }
    }
    return result
}

fun getDisplayScalingFactor(): Double {
    return GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.defaultTransform.scaleX
}