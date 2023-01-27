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

class Settings {
    val captureArea = MutableStateFlow<Rectangle?>(null)
    val templateArea = MutableStateFlow<Rectangle?>(null)
    val displayScalingFactor = getDisplayScalingFactor()
    val detectionAccuracy = 0.96
}
