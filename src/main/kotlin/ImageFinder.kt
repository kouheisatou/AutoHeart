import Application.settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.image.BufferedImage
import java.io.File
import java.util.Calendar

class ImageFinder(image: BufferedImage, template: BufferedImage) {
    private val threshold = template.grayScale().edge().calcBinalizeThreshold()
    val image = convertBufferedImageToBinaryImage(image, threshold)
    val template = convertBufferedImageToBinaryImage(template, threshold)
    val weightImageMap = mutableStateOf<BufferedImage?>(null)

    val currentSearchX = mutableStateOf(0)
    val currentSearchY = mutableStateOf(0)

    val searchResult = mutableStateOf<List<Vector>?>(null)

    var searching = mutableStateOf(false)
    var percentage = mutableStateOf(0f)
    val processingTime = mutableStateOf<Long?>(null)

    fun startSearching() {
        if (searching.value) {
            return
        }
        searching.value = true
        val startTime = Calendar.getInstance().timeInMillis

        CoroutineScope(Dispatchers.IO).launch {
            image.find(
                template,
                currentSearchCoordinateChanged = { coordinate, p ->
                    currentSearchX.value = coordinate.x
                    currentSearchY.value = coordinate.y
                    percentage.value = p
                },
                onSearchFinished = { result, map ->
                    searchResult.value = result
                    weightImageMap.value = map
                    searching.value = false
                    processingTime.value = Calendar.getInstance().timeInMillis - startTime
                },
            )
        }
    }
}

@Composable
fun ImageFinderComponent(imageFinder: ImageFinder) {
    var imageSize by remember { mutableStateOf<IntSize>(IntSize.Zero) }

    Column {
        LinearProgressIndicator(
            progress = imageFinder.percentage.value,
            modifier = Modifier.fillMaxWidth().alpha(
                if (imageFinder.searching.value) {
                    1f
                } else {
                    0f
                }
            )
        )

        Row {
            Button(
                onClick = {
                    imageFinder.startSearching()
                },
                enabled = !imageFinder.searching.value
            ) {
                if (imageFinder.searching.value) {
                    Text("Searching...  ${String.format("%.2f", imageFinder.percentage.value * 100)}%")
                } else {
                    Text("Start")
                }
            }
            if (imageFinder.processingTime.value != null) {
                Text("${imageFinder.processingTime.value!!.toDouble() / 1000.0}[s]")
            }
        }

        Box {
            Image(bitmap = imageFinder.template.toBufferedImage().toComposeImageBitmap(), null)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(3.dp)
                    .offsetMultiResolutionDisplay(
                        imageFinder.template.representativePixel.x.toFloat(),
                        imageFinder.template.representativePixel.y.toFloat(),
                        settings.displayScalingFactor,
                    )
                    .background(Color.Red)
            )
        }

        Box {
            Image(
                bitmap = imageFinder.image.toBufferedImage().toComposeImageBitmap(), null,
                modifier = Modifier
                    .onSizeChanged {
                        imageSize = it
                    }
            )
            if (imageFinder.weightImageMap.value != null) {
                Image(bitmap = imageFinder.weightImageMap.value!!.toComposeImageBitmap(), null)
            }

            // current searching point
            Box(
                modifier = Modifier
                    .offsetMultiResolutionDisplay(
                        (imageFinder.currentSearchX.value.toFloat() / imageFinder.image.width.toFloat() * imageSize.width).toFloat(),
                        (imageFinder.currentSearchY.value.toFloat() / imageFinder.image.height.toFloat() * imageSize.height).toFloat(),
                        settings.displayScalingFactor,
                    )
                    .width(3.dp)
                    .height(3.dp)
                    .background(Color.Red)
            )

            // search result point
            for (coordinate in imageFinder.searchResult.value ?: listOf()) {
                Box(
                    modifier = Modifier
                        .offsetMultiResolutionDisplay(
                            coordinate.x.toFloat() / imageFinder.image.width.toFloat() * imageSize.width,
                            coordinate.y.toFloat() / imageFinder.image.height.toFloat() * imageSize.height,
                            settings.displayScalingFactor,
                        )
                        .widthMultiResolutionDisplay(imageFinder.template.width.toFloat() / imageFinder.image.width.toFloat() * imageSize.width, settings.displayScalingFactor)
                        .heightMultiResolutionDisplay(imageFinder.template.height.toFloat() / imageFinder.image.height.toFloat() * imageSize.height, settings.displayScalingFactor)
                        .border(width = 1.dp, shape = RectangleShape, color = Color.Green)
                )
            }
        }
    }
}
