import Application.settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Rectangle
import java.awt.Robot
import java.awt.event.InputEvent
import java.awt.image.BufferedImage

class AutoClicker(val area: Rectangle, val templateImage: BufferedImage) {
    private val threshold = templateImage.grayScale().edge().calcBinalizeThreshold()
    val binaryTemplateImage = convertBufferedImageToBinaryImage(templateImage, threshold)
    val capturedImage = mutableStateOf<BufferedImage>(Robot().createScreenCapture(area))
    val weightImageMap = mutableStateOf<BufferedImage?>(null)

    val searchResult = mutableStateOf<List<Pair<Rectangle, Vector>>?>(null)

    var processing = mutableStateOf(false)
    var percentage = mutableStateOf(0f)

    fun start() {
        if (processing.value) {
            return
        }
        processing.value = true

        CoroutineScope(Dispatchers.IO).launch {
            println("start")
            autoClick()
        }
    }

    fun stop() {
        processing.value = false
        println("stop")
    }

    private fun autoClick() {

        capturedImage.value = Robot().createScreenCapture(area)
        val capturedImage = convertBufferedImageToBinaryImage(capturedImage.value, threshold)
        capturedImage.find(
            binaryTemplateImage,
            currentSearchCoordinateChanged = { _, p ->
                percentage.value = p
            },
            onSearchFinished = { result, map ->
                searchResult.value = result
                weightImageMap.value = map
                for (coordinate in result) {
                    Robot().mouseMove(
                        (area.x + (coordinate.first.x * 2 + coordinate.first.width).toFloat() / 2.0f).toInt(),
                        (area.y + (coordinate.first.y * 2 + coordinate.first.height).toFloat() / 2.0f).toInt(),
                    )
                    Robot().mousePress(InputEvent.BUTTON1_DOWN_MASK)
                    Thread.sleep(1)
                    Robot().mousePress(InputEvent.BUTTON1_DOWN_MASK)
                }
                if (processing.value) {
                    autoClick()
                }
            },
        )
    }
}

@Composable
fun AutoClickerComponent(autoClicker: AutoClicker) {
    var imageSize by remember { mutableStateOf<IntSize>(IntSize.Zero) }

    Column {
        LinearProgressIndicator(
            progress = if (autoClicker.processing.value) {
                autoClicker.percentage.value
            } else {
                0f
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row {
            Button(
                onClick = {
                    if (autoClicker.processing.value) {
                        autoClicker.stop()
                    } else {
                        autoClicker.start()
                    }
                },
            ) {
                if (autoClicker.processing.value) {
                    Text("Stop")
                } else {
                    Text("Start")
                }
            }
        }

        Box {
            Image(bitmap = autoClicker.templateImage.toBufferedImage().toComposeImageBitmap(), null)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(3.dp)
                    .offsetMultiResolutionDisplay(
                        autoClicker.binaryTemplateImage.representativePixel.x.toFloat(),
                        autoClicker.binaryTemplateImage.representativePixel.y.toFloat(),
                        settings.displayScalingFactor,
                    )
                    .background(Color.Red)
            )
        }

        Box {
            Image(
                bitmap = autoClicker.capturedImage.value.toComposeImageBitmap(), null,
                modifier = Modifier
                    .onSizeChanged {
                        imageSize = it
                    }
            )
            if (autoClicker.weightImageMap.value != null) {
                Image(bitmap = autoClicker.weightImageMap.value!!.toComposeImageBitmap(), null)
            }

            // search result point
            for (coordinate in autoClicker.searchResult.value ?: listOf()) {
                Box(
                    modifier = Modifier
                        .offsetMultiResolutionDisplay(
                            coordinate.first.x.toFloat() / autoClicker.area.width.toFloat() * imageSize.width,
                            coordinate.first.y.toFloat() / autoClicker.area.height.toFloat() * imageSize.height,
                            settings.displayScalingFactor,
                        )
                        .widthMultiResolutionDisplay(
                            autoClicker.binaryTemplateImage.width.toFloat() / autoClicker.area.width.toFloat() * imageSize.width,
                            settings.displayScalingFactor
                        )
                        .heightMultiResolutionDisplay(
                            autoClicker.binaryTemplateImage.height.toFloat() / autoClicker.area.height.toFloat() * imageSize.height,
                            settings.displayScalingFactor
                        )
                        .border(width = 1.dp, shape = RectangleShape, color = Color.Red)
                )
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(3.dp)
                        .offsetMultiResolutionDisplay(
                            coordinate.second.x.toFloat(),
                            coordinate.second.y.toFloat(),
                            settings.displayScalingFactor,
                        )
                        .background(Color.Red)
                )
            }
        }
    }
}
