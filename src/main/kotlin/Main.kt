import Application.autoClicker
import Application.settings
import Application.state
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.serialization.json.Json
import java.awt.image.BufferedImage
import kotlin.system.exitProcess


object Application {
    var settings = Settings()
    val jsonFormatter = Json { encodeDefaults = true }
    var autoClicker: AutoClicker? = null
    val state: MutableState<MainWindowState> = mutableStateOf(MainWindowState.SettingState)

    init {
        settings = settings.load()
    }
}

sealed class MainWindowState {
    object SettingState : MainWindowState()
    object ImageFinderState : MainWindowState()
    object CaptureAreaSelectorState : MainWindowState()
    object TemplateAreaSelectorState : MainWindowState()
    object ErrorState : MainWindowState()
}


@OptIn(ExperimentalMaterialApi::class)
fun main() {
    return application {
        Window(
            onCloseRequest = {
                exitProcess(0)
            },
            title = "AutoHart",
        ) {

            var captureAreaImage by remember { mutableStateOf<BufferedImage?>(null) }
            var templateAreaImage by remember { mutableStateOf<BufferedImage?>(null) }

            when (state.value) {
                is MainWindowState.ImageFinderState -> {
                    if (settings.captureArea.value != null && settings.templateArea.value != null && captureAreaImage != null && templateAreaImage != null) {
                        // main window layout
                        autoClicker = AutoClicker(
                            settings.captureArea.value!!,
                            templateAreaImage!!,
                        )
                        Column {
                            Row(
                                modifier = Modifier
                                    .background(Color.LightGray)
                                    .fillMaxWidth(),
                            ) {
                                Text("画像検索")
                                TextButton(onClick = {
                                    state.value = MainWindowState.SettingState
                                }) {
                                    Text("設定に戻る")
                                }
                            }
                            AutoClickerComponent(autoClicker!!)
                        }
                    } else {
                        state.value = MainWindowState.ErrorState
                    }
                }

                is MainWindowState.SettingState -> {

                    Column {
                        TitleNavigationComponent(
                            title = "設定",
                            onNavigate = {
                                state.value =
                                    if (settings.captureArea.value == null || settings.templateArea.value == null) {
                                        MainWindowState.ErrorState
                                    } else {
                                        MainWindowState.ImageFinderState
                                    }
                            },
                            navigationButtonTitle = "次へ",
                        )
                        Row {
                            Text("キャプチャエリア")
                            Button(
                                onClick = {
                                    state.value = MainWindowState.CaptureAreaSelectorState
                                }
                            ) {
                                if (captureAreaImage != null) {
                                    Image(
                                        bitmap = captureAreaImage!!.toComposeImageBitmap(),
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
                                if (settings.templateArea.value != null) {
                                    Image(
                                        bitmap = templateAreaImage!!.toComposeImageBitmap(),
                                        null
                                    )
                                }
                            }
                        }
                    }
                }

                is MainWindowState.CaptureAreaSelectorState -> {
                    val captureAreaSelector by remember {
                        mutableStateOf(
                            AreaSelector(
                                onCloseRequest = {
                                    state.value = MainWindowState.SettingState
                                },
                                onSelected = { selectedArea, selectedAreaImage ->
                                    settings.captureArea.value = selectedArea
                                    captureAreaImage = selectedAreaImage
                                    println("Capture$selectedArea")
                                },
                            ),
                        )
                    }
                    AreaSelectorComponent(captureAreaSelector, "キャプチャエリア選択してください")
                }

                is MainWindowState.TemplateAreaSelectorState -> {
                    val captureAreaSelector by remember {
                        mutableStateOf(
                            AreaSelector(
                                onCloseRequest = {
                                    state.value = MainWindowState.SettingState
                                },
                                onSelected = { selectedArea, selectedAreaImage ->
                                    settings.templateArea.value = selectedArea
                                    templateAreaImage = selectedAreaImage
                                    println("Capture$selectedArea")
                                },
                            ),
                        )
                    }

                    // layout
                    AreaSelectorComponent(captureAreaSelector, "検索対象の画像がある領域を選択してください")
                }

                is MainWindowState.ErrorState -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(30.dp)
                    ) {
                        AlertDialog(
                            modifier = Modifier.fillMaxWidth(),
                            title = { Text("設定値エラー") },
                            text = { Text("キャプチャエリアまたは検索画像が選択されていません") },
                            onDismissRequest = {
                                state.value = MainWindowState.SettingState
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    state.value = MainWindowState.SettingState
                                }) {
                                    Text("OK")
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TitleNavigationComponent(title: String, onNavigate: () -> Unit, navigationButtonTitle: String) {
    Row(
        modifier = Modifier
            .background(Color.LightGray)
            .fillMaxWidth(),
    ) {
        Text(title)
        TextButton(onClick = onNavigate) {
            Text(navigationButtonTitle)
        }
    }
}
