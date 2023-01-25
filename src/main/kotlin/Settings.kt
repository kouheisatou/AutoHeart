import Application.jsonFormatter
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.Window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File

@Serializable
class Settings {
    @Serializable(with = MutableStateFlowOfAreasSerializer::class)
    val captureArea = MutableStateFlow<Area?>(null)
    @Serializable(with = MutableStateFlowOfAreasSerializer::class)
    val templateArea = MutableStateFlow<Area?>(null)
    val detectionAccuracy = 0.80
    val debugMode = true

    fun save() {
        try {
            val filepath = javaClass.getResource("/")?.path + "settings.json"
            println(filepath)
            val file = File(filepath)
            val jsonString = jsonFormatter.encodeToString(this)
            println("save settings to $filepath\n\t$jsonString")
            file.writeText(jsonString, Charsets.UTF_8)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun load(): Settings {
        val file = File(javaClass.getResource("/")?.path + "settings.json")
        return if (file.exists()) {
            val jsonString = file.readText(Charsets.UTF_8)
            println("load settings from ${file.path}\n\t$jsonString")
            try {
                jsonFormatter.decodeFromString<Settings>(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                return Settings()
            }
        } else {
            Settings()
        }
    }
}

@Composable
fun SettingWindow(settings: Settings) {
    Window(
        onCloseRequest = {
            Application.isSettingWindowOpened.value = false
        },
        title = "settings",
    ) {
        Column {
            Row {
                Text("文字認識範囲設定")
                Button(
                    onClick = {
                        Application.isCaptureAreaSelectorWindowOpened.value = true
                    }
                ) {
                    Text(settings.captureArea.value.toString())
                }
            }
        }
    }
}
