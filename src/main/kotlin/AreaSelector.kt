import Application.state
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.awt.Robot
import java.awt.Toolkit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class AreaSelector(
    val onCloseRequest: () -> Unit,
    val onSelected: (selectedArea: Rectangle, selectedAreaImage: BufferedImage) -> Unit
) {
    val screenShot: BufferedImage
    val mode = mutableStateOf(AreaSelectorState.Hovering)

    init {
        val robot = Robot()
        val screenSize = Rectangle(Toolkit.getDefaultToolkit().screenSize)
        screenShot = robot.createScreenCapture(screenSize)
    }
}

enum class AreaSelectorState {
    Dragging, Hovering
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AreaSelectorComponent(areaSelector: AreaSelector, title: String) {
    var mouseX by remember { mutableStateOf<Float?>(null) }
    var mouseY by remember { mutableStateOf<Float?>(null) }
    var areaStartX by remember { mutableStateOf<Float?>(null) }
    var areaStartY by remember { mutableStateOf<Float?>(null) }
    var imageSize by remember { mutableStateOf<IntSize>(IntSize.Zero) }

    Window(
        onCloseRequest = { areaSelector.onCloseRequest() },
        title = title,
        state = rememberWindowState(WindowPlacement.Maximized),
        onKeyEvent = {
            if(it.type == KeyEventType.KeyDown && it.key == Key.Escape){
                areaSelector.onCloseRequest()
            }
            return@Window false
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ){
            Image(
                bitmap = areaSelector.screenShot.toComposeImageBitmap(),
                contentDescription = null,
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
                        } else {
                            position.y
                        }
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
                            val x1 = (areaSelector.screenShot.width * min(
                                max(position.x, 0f), imageSize.width.toFloat()
                            ) / imageSize.width).toInt()
                            val y1 = (areaSelector.screenShot.height * min(
                                max(position.y, 0f), imageSize.height.toFloat()
                            ) / imageSize.height).toInt()
                            val x2 = (areaSelector.screenShot.width * min(
                                max(areaStartX!!, 0f), imageSize.width.toFloat()
                            ) / imageSize.width).toInt()
                            val y2 = (areaSelector.screenShot.height * min(
                                max(areaStartY!!, 0f), imageSize.height.toFloat()
                            ) / imageSize.height).toInt()
                            val x = min(x1, x2)
                            val y = min(y1, y2)
                            val width = abs(x1 - x2)
                            val height = abs(y1 - y2)
                            if (width > 0 && height > 0) {
                                val selectedArea = Rectangle(x, y, width, height)
                                val selectedAreaImage = areaSelector.screenShot.getSubimage(
                                    selectedArea.x,
                                    selectedArea.y,
                                    selectedArea.width,
                                    selectedArea.height
                                )
                                areaSelector.onSelected(selectedArea, selectedAreaImage)
                                areaSelector.onCloseRequest()
                            }
                        }
                    }
                    .onSizeChanged {
                        imageSize = it
                    })
        }
        if (mouseX != null && mouseY != null) {

            Divider(
                color = Color.Red,
                thickness = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .offsetMultiResolutionDisplay(x = null, y = mouseY!!, getDisplayScalingFactor())
            )
            Divider(
                color = Color.Red,
                thickness = 1.dp,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .offsetMultiResolutionDisplay(x = mouseX!!, y = null, getDisplayScalingFactor())
            )
            if (areaSelector.mode.value == AreaSelectorState.Dragging && areaStartX != null && areaStartY != null) {
                Box(
                    modifier = Modifier
                        .offsetMultiResolutionDisplay(
                            min(areaStartX!!, mouseX!!), min(areaStartY!!, mouseY!!), getDisplayScalingFactor()
                        )
                        .widthMultiResolutionDisplay(abs(areaStartX!! - mouseX!!), getDisplayScalingFactor())
                        .heightMultiResolutionDisplay(abs(areaStartY!! - mouseY!!), getDisplayScalingFactor())
                        .border(1.dp, color = Color.Red)
                )
            }
        }
    }
}

@Composable
fun ImageAreaSelectorScreen() {

    val captureAreaSelector by remember {
        mutableStateOf(
            AreaSelector(
                onCloseRequest = {
                    state.value = MainWindowState.SettingState
                },
                onSelected = { selectedArea, selectedAreaImage ->
                    Settings.captureArea.value = selectedArea
                    Settings.captureAreaImage.value = selectedAreaImage
                    println("Capture$selectedArea")
                },
            ),
        )
    }
    AreaSelectorComponent(captureAreaSelector, "キャプチャエリア選択してください")
}

@Composable
fun TemplateImageAreaSelectorScreen() {
    val captureAreaSelector by remember {
        mutableStateOf(
            AreaSelector(
                onCloseRequest = {
                    state.value = MainWindowState.SettingState
                },
                onSelected = { selectedArea, selectedAreaImage ->
                    Settings.templateArea.value = selectedArea
                    Settings.templateAreaImage.value = selectedAreaImage
                    println("Capture$selectedArea")
                },
            ),
        )
    }

    // layout
    AreaSelectorComponent(captureAreaSelector, "検索対象の画像がある領域を選択してください")
}