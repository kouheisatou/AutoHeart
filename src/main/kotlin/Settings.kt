import Application.jsonFormatter
import Application.state
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.Window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.awt.Rectangle
import java.awt.Robot
import java.io.File

@Serializable
class Settings {
    @Transient
    val captureArea = MutableStateFlow<Rectangle?>(null)
    @Transient
    val templateArea = MutableStateFlow<Rectangle?>(null)
    @Transient
    val displayScalingFactor = getDisplayScalingFactor()
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
