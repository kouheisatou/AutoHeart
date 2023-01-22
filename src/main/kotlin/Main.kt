import Application.isCaptureAreaSelectorWindowOpened
import Application.isSettingWindowOpened
import Application.settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import javax.imageio.ImageIO
import javax.naming.Context
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess


object Application {
    val isCaptureAreaSelectorWindowOpened = mutableStateOf(false)
    val isSettingWindowOpened = mutableStateOf(false)
    var settings = Settings()
    val jsonFormatter = Json { encodeDefaults = true }

    init {
        settings = settings.load()
    }
}


@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    return application {
        Window(
            onCloseRequest = {
                exitProcess(0)
            },
            title = "AutoHart",
        ) {

            if (isCaptureAreaSelectorWindowOpened.value) {
                val captureAreaSelector by remember {
                    mutableStateOf(CaptureAreaSelector {
                        settings.captureArea.value = it
                        println("Capture$it")
                    })
                }
                CaptureAreaSelectorWindow(captureAreaSelector)
            }
            if (isSettingWindowOpened.value) {
                SettingWindow(settings)
            }

            if (settings.captureArea.value != null) {
                // main window layout

                val image = ImageIO.read(File("./src/main/resources/sample.png"))
                val target = ImageIO.read(File("./src/main/resources/hart.png"))

                val grayImage = image.grayScale()
                val grayTarget = target.grayScale()

                val imageBinalizeThreshold = target.grayScale().calcBinalizeThreshold()
                println("threshold=$imageBinalizeThreshold")
                val binImage = grayImage.binalized(imageBinalizeThreshold)
                val binTarget = grayTarget.binalized(imageBinalizeThreshold)

                Image(
                    bitmap = image.toComposeImageBitmap(), null,
                )
                var coordinates: List<Pair<Int, Int>> = mutableListOf()
                binImage.find(binTarget, onSearchFinished = {result -> coordinates = result})
                println(coordinates)
            } else {
                Column {
                    Text("文字認識領域と自動クリック領域を設定してください")
                    Button(onClick = {
                        isCaptureAreaSelectorWindowOpened.value = settings.captureArea.value == null
                    }) {
                        Text("OK")
                    }
                }
            }

            MenuBar {
                Menu("ウィンドウ") {
                    Item("設定", shortcut = KeyShortcut(key = Key.Comma, meta = true)) {
                        isSettingWindowOpened.value = true
                    }
                }
            }
        }
    }
}
