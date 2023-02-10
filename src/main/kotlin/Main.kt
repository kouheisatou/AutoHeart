import Application.state
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.serialization.json.Json
import kotlin.system.exitProcess


object Application {
    val jsonFormatter = Json { encodeDefaults = true }
    var autoClicker: AutoClicker? = null
    val state: MutableState<MainWindowState> = mutableStateOf(MainWindowState.SettingState)
}

sealed class MainWindowState {
    object SettingState : MainWindowState()
    object AutoClickerState : MainWindowState()
    object CaptureAreaSelectorState : MainWindowState()
    object TemplateAreaSelectorState : MainWindowState()
    class ErrorState(val msg: String) : MainWindowState()
}


@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    return application {
        Window(
            onCloseRequest = {
                exitProcess(0)
            },
            title = "AutoHart",
            onKeyEvent = {
                when(it.key){
                    Key.DirectionRight -> weightDebugCursorPosition.value = Offset(weightDebugCursorPosition.value.x +1, weightDebugCursorPosition.value.y)
                    Key.DirectionLeft -> weightDebugCursorPosition.value = Offset(weightDebugCursorPosition.value.x -1, weightDebugCursorPosition.value.y)
                    Key.DirectionUp -> weightDebugCursorPosition.value = Offset(weightDebugCursorPosition.value.x, weightDebugCursorPosition.value.y -1)
                    Key.DirectionDown -> weightDebugCursorPosition.value = Offset(weightDebugCursorPosition.value.x, weightDebugCursorPosition.value.y +1)
                }
                false
            }
        ) {
            when (state.value) {
                is MainWindowState.AutoClickerState -> {
                    AutoClickerScreen()
                }

                is MainWindowState.SettingState -> {
                    SettingScreen()
                }

                is MainWindowState.CaptureAreaSelectorState -> {
                    ImageAreaSelectorScreen()
                }

                is MainWindowState.TemplateAreaSelectorState -> {
                    TemplateImageAreaSelectorScreen()
                }

                is MainWindowState.ErrorState -> {
                    ErrorDialog()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ErrorDialog(){

    val msg = try {
        (state.value as MainWindowState.ErrorState).msg
    }catch (e: Exception){
        ""
    }
    Box(
        modifier = Modifier.fillMaxSize().padding(30.dp)
    ) {
        AlertDialog(
            modifier = Modifier.fillMaxWidth(),
            title = { Text("Error") },
            text = { Text(msg) },
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