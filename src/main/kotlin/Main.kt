import Application.imageFinder
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
    val isTemplateAreaSelectorWindowOpened = mutableStateOf(false)
    val isSettingWindowOpened = mutableStateOf(false)
    var settings = Settings()
    val jsonFormatter = Json { encodeDefaults = true }
    val imageFinder = ImageFinder(
        ImageIO.read(File("./src/main/resources/sample.png")),
        ImageIO.read(File("./src/main/resources/hart.png"))
    )

    init {
//        settings = settings.load()
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


                ImageFinderComponent(imageFinder)
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
