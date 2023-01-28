import kotlinx.coroutines.flow.MutableStateFlow
import java.awt.Rectangle

class Settings {
    val captureArea = MutableStateFlow<Rectangle?>(null)
    val templateArea = MutableStateFlow<Rectangle?>(null)
    val displayScalingFactor = getDisplayScalingFactor()
    val detectionAccuracy = 0.96
    val mouseDownTimeMillis = 1
    val scrollDownAmount = 10
}
