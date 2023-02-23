import Application.state
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.text.DecimalFormat

object Settings {
    val captureArea = MutableStateFlow<Rectangle?>(null)
    val templateArea = MutableStateFlow<Rectangle?>(null)
    val weightThreshold = mutableStateOf(0.60)
    val clickInterval = mutableStateOf(500)
    val nextImageInterval = mutableStateOf(2000)

    var captureAreaImage = mutableStateOf<BufferedImage?>(null)
    var templateAreaImage = mutableStateOf<BufferedImage?>(null)
    val stopCount = mutableStateOf(10000)
    val testMode = mutableStateOf(true)
    val steepDelta = mutableStateOf(2)
    var steepThresholdAllowance = mutableStateOf(12.0)
}


@Composable
fun SettingScreen() {

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.fillMaxWidth(),
                title = {
                    Row {
                        Spacer(modifier = Modifier.weight(1f))
                        Text("設定")
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            state.value = MainWindowState.AutoClickerState
                        }) {
                            Text(">")
                        }
                    }
                }
            )
        }
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row {
                Text("自動終了回数 : ${String.format("%06d", Settings.stopCount.value)}")
                Slider(
                    value = Settings.stopCount.value.toFloat(),
                    onValueChange = {
                        try {
                            Settings.stopCount.value = it.toInt()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    valueRange = 1f..100000f
                )
            }
            Row {
                Text("重み閾値 : ${DecimalFormat("0.00").format(Settings.weightThreshold.value)}")
                Slider(
                    value = Settings.weightThreshold.value.toFloat(),
                    onValueChange = {
                        try {
                            Settings.weightThreshold.value = it.toDouble()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    valueRange = 0f..1f,
                )
            }
            Row {
                Text("重み勾配許容度 : ${DecimalFormat("00.00").format(Settings.steepThresholdAllowance.value)}")
                Slider(
                    value = Settings.steepThresholdAllowance.value.toFloat(),
                    onValueChange = {
                        try {
                            Settings.steepThresholdAllowance.value = it.toDouble()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    valueRange = 0f..100f,
                )
            }
            Row {
                Text("重み勾配測定距離 : ${String.format("%02d", Settings.steepDelta.value)}")
                Slider(
                    value = Settings.steepDelta.value.toFloat(),
                    onValueChange = {
                        try {
                            Settings.steepDelta.value = it.toInt()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    valueRange = 1f..99f,
                )
            }
            Row {
                Text("クリック間隔 : ${DecimalFormat("00.00").format(Settings.clickInterval.value.toFloat()/1000f)}s")
                Slider(
                    value = Settings.clickInterval.value.toFloat()/1000f,
                    onValueChange = {
                        try {
                            Settings.clickInterval.value = (it * 1000f).toInt()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    valueRange = 0f..10f,
                )
            }
            Row {
                Text("画像認識間隔 : ${DecimalFormat("00.00").format(Settings.nextImageInterval.value.toFloat()/1000f)}s")
                Slider(
                    value = Settings.nextImageInterval.value.toFloat()/1000f,
                    onValueChange = {
                        try {
                            Settings.nextImageInterval.value = (it * 1000f).toInt()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    valueRange = 0f..10f,
                )
            }
            Row {
                Text("自動クリックエリア")
                Button(
                    onClick = {
                        state.value = MainWindowState.CaptureAreaSelectorState
                    }
                ) {
                    if (Settings.captureAreaImage.value != null) {
                        Image(
                            bitmap = Settings.captureAreaImage.value!!.toComposeImageBitmap(),
                            null
                        )
                    } else {
                        Text("ここを押してエリアを指定")
                    }
                }
            }
            Row {
                Text("検索対象画像")
                Button(
                    onClick = {
                        state.value = MainWindowState.TemplateAreaSelectorState
                    }
                ) {
                    if (Settings.templateArea.value != null) {
                        Image(
                            bitmap = Settings.templateAreaImage.value!!.toComposeImageBitmap(),
                            null
                        )
                    } else {
                        Text("ここを押して検索対象の画像をキャプチャ")
                    }
                }
            }
        }
    }
}