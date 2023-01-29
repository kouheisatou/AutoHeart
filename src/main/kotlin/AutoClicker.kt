import Application.autoClicker
import Application.state
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
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

    val searchResult = mutableStateOf<List<Rectangle>>(mutableListOf())

    var processing = mutableStateOf(false)
    var percentage = mutableStateOf(0f)

    fun start() {
        if (processing.value) return
        processing.value = true

        CoroutineScope(Dispatchers.IO).launch {
            println("start")
            while (processing.value) {

                capturedImage.value = Robot().createScreenCapture(area)
                val capturedImage = convertBufferedImageToBinaryImage(capturedImage.value, threshold)
                try {
                    searchResult.value = capturedImage.find(
                        binaryTemplateImage,
                        currentSearchCoordinateChanged = { _, p ->
                            percentage.value = p
                        },
                    )
                }catch (e:Exception){
                    e.printStackTrace()
                    println(e.message)
                }
                weightImageMap.value = capturedImage.weightMapAlphaImage

                val r = Robot()
                for (coordinate in searchResult.value) {
                    r.mouseMove(
                        (area.x + (coordinate.x * 2 + coordinate.width).toFloat() / 2.0f).toInt(),
                        (area.y + (coordinate.y * 2 + coordinate.height).toFloat() / 2.0f).toInt(),
                    )
                    r.mousePress(InputEvent.BUTTON1_DOWN_MASK)
                    Thread.sleep(Settings.mouseDownTimeMillis.toLong())
                    r.mousePress(InputEvent.BUTTON1_DOWN_MASK)
                }
                r.mouseWheel(-Settings.scrollDownAmount)
            }
        }
    }

    fun stop() {
        processing.value = false
        println("stop")
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

        if(autoClicker.binaryTemplateImage.representativePixel != null){
            Box {
                Image(bitmap = autoClicker.templateImage.toBufferedImage().toComposeImageBitmap(), null)
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(3.dp)
                        .offsetMultiResolutionDisplay(
                            autoClicker.binaryTemplateImage.representativePixel.x.toFloat(),
                            autoClicker.binaryTemplateImage.representativePixel.y.toFloat(),
                            Settings.displayScalingFactor,
                        )
                        .background(Color.Red)
                )
            }
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
            for (coordinate in autoClicker.searchResult.value) {
                Box(
                    modifier = Modifier
                        .offsetMultiResolutionDisplay(
                            coordinate.x.toFloat() / autoClicker.area.width.toFloat() * imageSize.width,
                            coordinate.y.toFloat() / autoClicker.area.height.toFloat() * imageSize.height,
                            Settings.displayScalingFactor,
                        )
                        .widthMultiResolutionDisplay(
                            autoClicker.binaryTemplateImage.width.toFloat() / autoClicker.area.width.toFloat() * imageSize.width,
                            Settings.displayScalingFactor
                        )
                        .heightMultiResolutionDisplay(
                            autoClicker.binaryTemplateImage.height.toFloat() / autoClicker.area.height.toFloat() * imageSize.height,
                            Settings.displayScalingFactor
                        )
                        .border(width = 1.dp, shape = RectangleShape, color = Color.Red)
                )
            }
        }
    }
}

@Composable
fun AutoClickerScreen() {
    if (Settings.captureArea.value != null && Settings.templateArea.value != null && Settings.captureAreaImage.value != null && Settings.templateAreaImage.value != null) {
        // main window layout
        autoClicker = AutoClicker(
            Settings.captureArea.value!!,
            Settings.templateAreaImage.value!!,
        )

        if(autoClicker!!.binaryTemplateImage.representativePixel == null){
            state.value = MainWindowState.ErrorState("検索画像から形状を認識できませんでした。もう一度検索画像を選択してください。")
            return
        }

        Scaffold(topBar = {
            TopAppBar(modifier = Modifier.fillMaxWidth()) {
                Row {
                    IconButton(onClick = {
                        state.value = MainWindowState.SettingState
                    }) {
                        Text("<")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("自動クリック")
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }) {
            AutoClickerComponent(autoClicker!!)
        }
    } else {
        state.value = MainWindowState.ErrorState("キャプチャエリアと検索画像の指定が完了していません")
    }
}