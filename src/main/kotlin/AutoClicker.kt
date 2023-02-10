import Application.autoClicker
import Application.state
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.MouseInfo
import java.awt.Rectangle
import java.awt.Robot
import java.awt.event.InputEvent
import java.awt.image.BufferedImage

class AutoClicker(val area: Rectangle, val templateImage: BufferedImage) {
    private val threshold = templateImage.grayScale().edge().calcBinalizeThreshold()
    val binaryTemplateImage = convertBufferedImageToBinaryImage(templateImage, threshold)
    val capturedImage = mutableStateOf<BufferedImage>(Robot().createScreenCapture(area))
    var binaryCapturedImage: BinaryImage? = null
    val weightImageMap = mutableStateOf<BufferedImage?>(null)

    val searchResult = mutableStateOf<List<SearchResult>>(mutableListOf())

    var processing = mutableStateOf(false)
    var percentage = mutableStateOf(0f)
    private var count = 0

    @OptIn(ExperimentalComposeUiApi::class)
    fun start() {
        if (processing.value) return
        processing.value = true
        count = 0

        CoroutineScope(Dispatchers.IO).launch {
            println("START")

            // マウス位置リセット
            val r = Robot()
            r.mouseMove(area.x, area.y)
            r.delay(Settings.clickInterval)

            while (processing.value) {

                percentage.value = count.toFloat() / Settings.stopCount.value.toFloat()

                // 自動クリックエリアのキャプチャを取得
                capturedImage.value = Robot().createScreenCapture(area)

                // 2値画像に変換
                binaryCapturedImage = convertBufferedImageToBinaryImage(capturedImage.value, threshold)

                // 画像検索
                try {
                    searchResult.value = binaryCapturedImage!!.find(binaryTemplateImage)
                } catch (e: Exception) {
                    e.printStackTrace()
                    println(e.message)
                }

                // 画像検索に利用した重みマップを重ねて表示
                weightImageMap.value = binaryCapturedImage!!.weightMapAlphaImage

                // debugModeでは1回のキャプチャで終了
                if (Settings.debugMode) {
                    stop()
                    return@launch
                }

                // マウスを自動クリック範囲外に持っていくと終了
                if (isCursorOutside()) {
                    stop("Mouse moved outside of auto click area.")
                    break
                }

                // 検索結果の座標をすべてクリックする
                for (coordinate in searchResult.value) {

                    // boundingBoxが自動クリックエリア外の場合、スキップ
                    if (!((area.x + (coordinate.x * 2 + coordinate.width).toFloat() / 2.0f).toInt() in area.x..area.x + area.width && (area.y + (coordinate.y * 2 + coordinate.height).toFloat() / 2.0f).toInt() in area.y..area.y + area.height)) {
                        continue
                    }

                    // 自動クリック
                    r.mouseMove(
                        (area.x + (coordinate.x * 2 + coordinate.width).toFloat() / 2.0f).toInt(),
                        (area.y + (coordinate.y * 2 + coordinate.height).toFloat() / 2.0f).toInt(),
                    )
                    r.mousePress(InputEvent.BUTTON1_DOWN_MASK)
                    r.delay(Settings.clickTime)
                    r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)

                    // マウスを自動クリック範囲外に持っていくと終了
                    if (isCursorOutside()) {
                        stop("Mouse moved outside of auto click area.")
                        break
                    }

                    // マウス位置リセット（マウスホバーでダイアログが出るwebページを回避するため）
                    r.mouseMove(area.x, area.y)
                    r.delay(Settings.clickInterval)

                    // マウスを自動クリック範囲外に持っていくと終了
                    if (isCursorOutside()) {
                        stop("Mouse moved outside of auto click area.")
                        break
                    }
                }
                count++

                // クリックすべきものがなくなったらスクロールして次の画像を読み込む
                if (searchResult.value.isEmpty()) {
                    r.keyPress(Key.PageDown.nativeKeyCode)
                    r.delay(Settings.clickTime)
                    r.keyRelease(Key.PageDown.nativeKeyCode)
                }

                // stopCountを超えたら自動終了
                if (count >= Settings.stopCount.value) {
                    stop()
                    break
                }

                // キャプチャインターバル
                r.delay(Settings.nextImageInterval)
            }
        }
    }

    private fun isCursorOutside(): Boolean {
        val mouseLocation = MouseInfo.getPointerInfo().location
        return !(mouseLocation.x in area.x..area.x + area.width && mouseLocation.y in area.y..area.y + area.height)
    }

    fun stop(msg: String? = null) {
        processing.value = false
        if (msg == null) {
            println("STOP")
        } else {
            println("STOP : $msg")
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
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

            if (autoClicker.processing.value) {
                Text("マウスを自動クリック範囲外に持っていくと終了")
            }
        }

        // template画像表示
        if (autoClicker.binaryTemplateImage.representativePixel != null) {
            Box {
                Image(bitmap = autoClicker.templateImage.toBufferedImage().toComposeImageBitmap(), null)
                // 代表点
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

        // 認識結果
        Box {
            // キャプチャ画像
            Image(
                bitmap = autoClicker.capturedImage.value.toComposeImageBitmap(), null,
                modifier = Modifier
                    .onSizeChanged {
                        imageSize = it
                    }
            )
            // 重みマップ
            if (autoClicker.weightImageMap.value != null && autoClicker.binaryCapturedImage != null) {
                var cursorPosition by remember { mutableStateOf<Offset?>(null) }
                var weight by remember { mutableStateOf<Double?>(null) }
                Image(
                    bitmap = autoClicker.weightImageMap.value!!.toComposeImageBitmap(),
                    null,
                    modifier = Modifier
                        .onPointerEvent(PointerEventType.Move) {
                            cursorPosition = it.changes.first().position
                            weight =
                                autoClicker.binaryCapturedImage!!.weightMap[cursorPosition!!.x.toInt()][cursorPosition!!.y.toInt()].toDouble() / autoClicker.binaryCapturedImage!!.maxWeight
                        }
                        .onKeyEvent {
                            println(it.key)
                            when(it.key){
                                Key.DirectionRight -> cursorPosition = Offset(cursorPosition!!.x -1, cursorPosition!!.y)
                                Key.DirectionLeft -> cursorPosition = Offset(cursorPosition!!.x +1, cursorPosition!!.y)
                                Key.DirectionDown -> cursorPosition = Offset(cursorPosition!!.x, cursorPosition!!.y -1)
                                Key.DirectionUp -> cursorPosition = Offset(cursorPosition!!.x, cursorPosition!!.y +1)
                            }
                            false
                        },
                )
                if (cursorPosition != null) {
                    Box(
                        modifier = Modifier
                            .offsetMultiResolutionDisplay(
                                cursorPosition!!.x,
                                cursorPosition!!.y,
                                Settings.displayScalingFactor
                            )
                            .height(1.dp)
                            .width(1.dp)
                            .background(color = Color.Blue),
                    )
                    Text(
                        weight.toString(),
                        modifier = Modifier
                            .offsetMultiResolutionDisplay(
                                cursorPosition!!.x + 10,
                                cursorPosition!!.y,
                                Settings.displayScalingFactor
                            ),
                    )
                }
            }

            // search result point
            for (result in autoClicker.searchResult.value) {
                Box(
                    modifier = Modifier
                        .offsetMultiResolutionDisplay(
                            result.x.toFloat() / autoClicker.area.width.toFloat() * imageSize.width,
                            result.y.toFloat() / autoClicker.area.height.toFloat() * imageSize.height,
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
                Box(
                    modifier = Modifier
                        .offsetMultiResolutionDisplay(
                            result.representativePointX.toFloat() / autoClicker.area.width.toFloat() * imageSize.width,
                            result.representativePointY.toFloat() / autoClicker.area.height.toFloat() * imageSize.height,
                            Settings.displayScalingFactor,
                        )
                        .width(3.dp)
                        .height(3.dp)
                        .background(color = Color.Red)
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

        if (autoClicker!!.binaryTemplateImage.representativePixel == null) {
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