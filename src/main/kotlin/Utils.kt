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

fun getDisplayScalingFactor(): Double {
    return GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.defaultTransform.scaleX
}