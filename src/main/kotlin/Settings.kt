import Application.state
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import java.awt.Rectangle
import java.awt.image.BufferedImage

object Settings {
    val captureArea = MutableStateFlow<Rectangle?>(null)
    val templateArea = MutableStateFlow<Rectangle?>(null)
    val displayScalingFactor = getDisplayScalingFactor()
    val detectionAccuracy = 0.99
    val clickTime = 3
    val clickInterval = 10
    val nextImageInterval = 100

    var captureAreaImage = mutableStateOf<BufferedImage?>(null)
    var templateAreaImage = mutableStateOf<BufferedImage?>(null)
    val stopCount = 100
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
                        }){
                            Text(">")
                        }
                    }
                }
            )
        }
    ) {

        Column {
            Row {
                Text("キャプチャエリア")
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
                    }
                }
            }
            Row {
                Text("検索画像")
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
                    }
                }
            }
        }
    }
}