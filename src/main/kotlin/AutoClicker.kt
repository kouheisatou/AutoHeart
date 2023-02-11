import Application.autoClicker
import Application.state
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.input.key.nativeKeyCode
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

var weightDebugCursorPosition = mutableStateOf(Offset(0f, 0f))

class AutoClicker(val area: Rectangle, val templateImage: BufferedImage) {
    private val threshold = templateImage.grayScale().edge().calcBinalizeThreshold()
    val binaryTemplateImage = convertBufferedImageToBinaryImage(templateImage, threshold)
    val capturedImage = mutableStateOf<BufferedImage>(Robot().createScreenCapture(area))
    var binaryCapturedImage: BinaryImage? = null
    val weightMapAlphaImage = mutableStateOf<BufferedImage?>(null)
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
            if (!Settings.testMode.value) {
                r.mouseMove(area.x, area.y)
                r.delay(Settings.clickInterval)
            }

            // あらかじめ重み傾き閾値を自動決定しておく
            val steepThreshold = binaryTemplateImage.calcSteepThreshold()
            println("steepThreshold = $steepThreshold")
            println("detectionThreshold = ${Settings.weightThreshold.value}")

            while (processing.value) {

                percentage.value = count.toFloat() / Settings.stopCount.value.toFloat()

                // 自動クリックエリアのキャプチャを取得
                capturedImage.value = Robot().createScreenCapture(area)

                // 2値画像に変換
                binaryCapturedImage = convertBufferedImageToBinaryImage(capturedImage.value, threshold)

                // 画像検索
                try {
                    searchResult.value = binaryCapturedImage!!.find(
                        binaryTemplateImage,
                        Settings.weightThreshold.value,
                        steepThreshold,
                        Settings.steepThresholdAllowance.value,
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    println(e.message)
                }

                // 画像検索に利用した重みマップを重ねて表示
                if (Settings.testMode.value) {
                    weightMapAlphaImage.value = binaryCapturedImage!!.weightMapAlphaImage
                }

                // マウスを自動クリック範囲外に持っていくと終了
                if (!Settings.testMode.value && isCursorOutside()) {
                    stop("Mouse moved outside of auto click area.")
                    break
                }

                // 検索結果の座標をすべてクリックする
                for (result in searchResult.value) {

                    // クリック位置
                    val clickPointX = (area.x + (result.x * 2 + result.width).toFloat() / 2.0f).toInt()
                    val clickPointY = (area.y + (result.y * 2 + result.height).toFloat() / 2.0f).toInt()

                    // クリック位置が自動クリックエリア外の場合スキップ
                    if (!(clickPointX in area.x..area.x + area.width && clickPointY in area.y..area.y + area.height)) {
                        continue
                    }

                    if (!Settings.testMode.value) {
                        // 自動クリック
                        r.mouseMove(
                            clickPointX,
                            clickPointY,
                        )
                        r.mousePress(InputEvent.BUTTON1_DOWN_MASK)
                        r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)

                        // マウスを自動クリック範囲外に持っていくと終了
                        if (isCursorOutside()) {
                            stop("Mouse moved outside of auto click area.")
                            break
                        }

                        // マウス位置リセット（マウスホバーでダイアログが出るのを回避するため）
                        r.mouseMove(area.x, area.y)
                        r.delay(Settings.clickInterval)

                        // マウスを自動クリック範囲外に持っていくと終了
                        if (isCursorOutside()) {
                            stop("Mouse moved outside of auto click area.")
                            break
                        }
                    }
                }
                count++

                // クリックすべきものがなくなったらスクロールして次の画像を読み込む
                if (!Settings.testMode.value) {
                    if (searchResult.value.isEmpty()) {
                        r.keyPress(Key.PageDown.nativeKeyCode)
                        r.keyRelease(Key.PageDown.nativeKeyCode)
                    }
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
    val resultPointedByCursor = mutableListOf<SearchResult>()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
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
                if (!Settings.testMode.value) {
                    Text("マウスを自動クリック範囲外に持っていくと終了")
                }
            } else {
                Text("テストモード")
                Switch(Settings.testMode.value, onCheckedChange = {
                    Settings.testMode.value = it
                })
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
                            getDisplayScalingFactor(),
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
            // 重みマップアルファ画像
            if (autoClicker.weightMapAlphaImage.value != null && Settings.testMode.value) {
                Image(
                    bitmap = autoClicker.weightMapAlphaImage.value!!.toComposeImageBitmap(),
                    null,
                    modifier = Modifier
                        .onPointerEvent(PointerEventType.Press) {
                            val cursor = it.changes.first().position
                            weightDebugCursorPosition.value = Offset(
                                cursor.x / imageSize.width * autoClicker.weightMapAlphaImage.value!!.width,
                                cursor.y / imageSize.height * autoClicker.weightMapAlphaImage.value!!.height,
                            )
                        }
                )
            }

            // search result point
            for (result in autoClicker.searchResult.value) {
                // bounding box
                Box(
                    modifier = Modifier
                        .offsetMultiResolutionDisplay(
                            result.x.toFloat() / autoClicker.area.width.toFloat() * imageSize.width,
                            result.y.toFloat() / autoClicker.area.height.toFloat() * imageSize.height,
                            getDisplayScalingFactor(),
                        )
                        .widthMultiResolutionDisplay(
                            autoClicker.binaryTemplateImage.width.toFloat() / autoClicker.area.width.toFloat() * imageSize.width,
                            getDisplayScalingFactor(),
                        )
                        .heightMultiResolutionDisplay(
                            autoClicker.binaryTemplateImage.height.toFloat() / autoClicker.area.height.toFloat() * imageSize.height,
                            getDisplayScalingFactor(),
                        )
                        .border(width = 1.dp, shape = RectangleShape, color = Color.Red)
                ) {
                    if (Settings.testMode.value) {
                        Text(result.id.toString())
                    }
                }
                // representative point
                if (Settings.testMode.value) {
                    Box(
                        modifier = Modifier
                            .offsetMultiResolutionDisplay(
                                result.representativePointX.toFloat() / autoClicker.area.width.toFloat() * imageSize.width,
                                result.representativePointY.toFloat() / autoClicker.area.height.toFloat() * imageSize.height,
                                getDisplayScalingFactor(),
                            )
                            .width(3.dp)
                            .height(3.dp)
                            .background(color = Color.Red)
                    )

                    // クリックした部分にかぶるboundingBoxをprint
                    val cursorPosition = weightDebugCursorPosition.value
                    if (cursorPosition.x.toInt() in result.x..result.x + result.width && cursorPosition.y.toInt() in result.y..result.y + result.height) {
                        resultPointedByCursor.add(result)
                    }
                }
            }

            // 重みデバッグ用カーソル
            if (Settings.testMode.value) {
                if (autoClicker.binaryCapturedImage?.weightMap != null) {
                    val cursorPosition = weightDebugCursorPosition.value
                    if (cursorPosition.x.toInt() in 0 until autoClicker.binaryCapturedImage!!.weightMap.size && cursorPosition.y.toInt() in 0 until autoClicker.binaryCapturedImage!!.weightMap[0].size) {
                        Column(
                            modifier = Modifier
                                .offsetMultiResolutionDisplay(
                                    cursorPosition.x / autoClicker.binaryCapturedImage!!.weightMap.size * imageSize.width,
                                    cursorPosition.y / autoClicker.binaryCapturedImage!!.weightMap[0].size * imageSize.height,
                                    getDisplayScalingFactor()
                                ),
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(1.dp)
                                    .width(1.dp)
                                    .background(color = Color.Black),
                            )
                            Text(
                                "(${cursorPosition.x.toInt()},${cursorPosition.y.toInt()})\nweight=" +
                                        String.format(
                                            "%.2f",
                                            autoClicker.binaryCapturedImage!!.weightMap[cursorPosition.x.toInt()][cursorPosition.y.toInt()].toDouble()
                                        ),
                            )
                            LazyColumn {
                                items(resultPointedByCursor) {
                                    Text(it.toString())
                                }
                            }
                        }
                    }
                }
            }
        }
        if (Settings.testMode.value) {
            Divider()
            LazyColumn {
                items(autoClicker.searchResult.value) {
                    Text(it.toString())
                }
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
                        autoClicker!!.stop()
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