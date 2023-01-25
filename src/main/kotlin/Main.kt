import Application.imageFinder
import Application.isCaptureAreaSelectorWindowOpened
import Application.isTemplateAreaSelectorWindowOpened
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
import java.awt.Robot
import java.io.File
import javax.imageio.ImageIO
import javax.naming.Context
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess


object Application {
    val isCaptureAreaSelectorWindowOpened = mutableStateOf(false)
    val isTemplateAreaSelectorWindowOpened = mutableStateOf(false)
    var settings = Settings()
    val jsonFormatter = Json { encodeDefaults = true }
    var imageFinder: ImageFinder? = null

    init {
        settings = settings.load()
    }
}


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
                    mutableStateOf(
                        AreaSelector(
                            onCloseRequest = {
                                isCaptureAreaSelectorWindowOpened.value = false
                            },
                            onSelected = {

                                settings.captureArea.value = it
                                println("Capture$it")
                            },
                        ),
                    )
                }
                AreaSelectorWindow(captureAreaSelector, "キャプチャエリアを選択してください")
            }

            if (isTemplateAreaSelectorWindowOpened.value) {
                val captureAreaSelector by remember {
                    mutableStateOf(
                        AreaSelector(
                            onCloseRequest = {
                                isTemplateAreaSelectorWindowOpened.value = false
                            },
                            onSelected = {
                                settings.templateArea.value = it
                                println("Capture$it")
                            },
                        ),
                    )
                }
                AreaSelectorWindow(captureAreaSelector, "検索対象を選択してください")
            }

            if (settings.captureArea.value != null && settings.templateArea.value != null) {
                // main window layout
                if (imageFinder == null) {
                    val robot = Robot()
                    imageFinder = ImageFinder(
                        robot.createScreenCapture(settings.captureArea.value!!),
                        robot.createScreenCapture(settings.templateArea.value!!),
                    )
                }
                ImageFinderComponent(imageFinder!!)
            } else {
                SettingComponent(settings)
            }
        }
    }
}
