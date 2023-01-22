import Application.settings
import androidx.compose.runtime.Composable

class CaptureAreaSelector(private val onSelected: (area: Area) -> Unit): AreaSelector() {
    override fun onCloseRequest() {
        Application.isCaptureAreaSelectorWindowOpened.value = false
        settings.save()
    }

    override fun onSelected(area: Area) {
        this.onSelected.invoke(area)
        onCloseRequest()
    }
}

@Composable
fun CaptureAreaSelectorWindow(captureAreaSelector: CaptureAreaSelector){
    AreaSelectorWindow(captureAreaSelector)
}