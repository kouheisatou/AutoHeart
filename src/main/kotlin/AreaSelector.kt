import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.awt.Robot
import java.awt.Toolkit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


abstract class AreaSelector {
    val screenShot: BufferedImage
    val mode = mutableStateOf(AreaSelectorState.Hovering)
    val screenSize: Rectangle

    init {
        val robot = Robot()
        screenSize = Rectangle(Toolkit.getDefaultToolkit().screenSize)
        screenShot = robot.createScreenCapture(screenSize)
    }

    abstract fun onCloseRequest()

    abstract fun onSelected(area: Area)
}

enum class AreaSelectorState {
    Dragging, Hovering
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AreaSelectorWindow(areaSelector: AreaSelector) {
    var mouseX by remember { mutableStateOf<Float?>(null) }
    var mouseY by remember { mutableStateOf<Float?>(null) }
    var areaStartX by remember { mutableStateOf<Float?>(null) }
    var areaStartY by remember { mutableStateOf<Float?>(null) }
    var imageSize by remember { mutableStateOf<IntSize>(IntSize.Zero) }

    Window(onCloseRequest = { areaSelector.onCloseRequest() }) {
        Image(
            bitmap = areaSelector.screenShot.toComposeImageBitmap(),
            null,
            modifier = Modifier
                .onPointerEvent(PointerEventType.Move) {
                    val position = it.changes.first().position
                    mouseX = if (position.x < 0) {
                        0f
                    } else if (position.x > imageSize.width) {
                        imageSize.width.toFloat()
                    } else {
                        position.x
                    }
                    mouseY = if (position.y < 0) {
                        0f
                    } else if (position.y > imageSize.height) {
                        imageSize.height.toFloat()
                    } else { position.y }
                }
                .onPointerEvent(PointerEventType.Press) {
                    areaSelector.mode.value = AreaSelectorState.Dragging

                    mouseX = null
                    mouseY = null
                    areaStartX = null
                    areaStartY = null

                    val position = it.changes.first().position
                    areaStartX = position.x
                    areaStartY = position.y
                }
                .onPointerEvent(PointerEventType.Release) {
                    areaSelector.mode.value = AreaSelectorState.Hovering
                    val position = it.changes.first().position

                    if (areaStartX != null && areaStartY != null) {
                        areaSelector.onSelected(
                            Area(
                                (areaSelector.screenSize.width * min(max(position.x, 0f), imageSize.width.toFloat()) / imageSize.width).toInt(),
                                (areaSelector.screenSize.height * min(max(position.y, 0f), imageSize.height.toFloat()) / imageSize.height).toInt(),
                                (areaSelector.screenSize.width * min(max(areaStartX!!, 0f), imageSize.width.toFloat()) / imageSize.width).toInt(),
                                (areaSelector.screenSize.height * min(max(areaStartY!!, 0f), imageSize.height.toFloat()) / imageSize.height).toInt(),
                            )
                        )
                    }
                }
                .onSizeChanged {
                    imageSize = it
                }
        )
        if (mouseX != null && mouseY != null) {

            Divider(
                color = Color.Red,
                thickness = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = mouseY!!.dp)
            )
            Divider(
                color = Color.Red,
                thickness = 1.dp,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .offset(x = mouseX!!.dp)
            )
            if (areaSelector.mode.value == AreaSelectorState.Dragging && areaStartX != null && areaStartY != null) {
                Box(
                    modifier = Modifier
                        .offset(min(areaStartX!!, mouseX!!).dp, min(areaStartY!!, mouseY!!).dp)
                        .width(abs(areaStartX!! - mouseX!!).dp)
                        .height(abs(areaStartY!! - mouseY!!).dp)
                        .background(color = Color.Red)
                )
            }
            Text(
                text = "($mouseX,$mouseY)\n(${areaSelector.screenSize.width * mouseX!! / imageSize.width},${areaSelector.screenSize.height * mouseY!! / imageSize.height})",
            )
            Text(
                text = "($mouseX,$mouseY)\n(${areaSelector.screenSize.width * mouseX!! / imageSize.width},${areaSelector.screenSize.height * mouseY!! / imageSize.height})",
                modifier = Modifier
                    .offset(x = mouseX!!.dp, y = mouseY!!.dp)
                    .background(color = Color.White)
            )
        }
    }
}