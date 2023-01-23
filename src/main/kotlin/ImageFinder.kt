import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
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
    var percentage = mutableStateOf(0)
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

        Row {
            Button(
                onClick = {
                    imageFinder.startSearching()
                },
                enabled = !imageFinder.searching.value
            ) {
                if (imageFinder.searching.value) {
                    Text("Searching...  ${imageFinder.percentage.value}%")
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
                    .offset(imageFinder.template.representativePixel.x.dp, imageFinder.template.representativePixel.y.dp)
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
                    .offset(
                        (imageFinder.currentSearchX.value.toFloat() / imageFinder.image.width.toFloat() * imageSize.width).dp,
                        (imageFinder.currentSearchY.value.toFloat() / imageFinder.image.height.toFloat() * imageSize.height).dp,
                    )
                    .width(3.dp)
                    .height(3.dp)
                    .background(Color.Red)
            )

            // search result point
            for (coordinate in imageFinder.searchResult.value ?: listOf()) {
                Box(
                    modifier = Modifier
                        .offset(
                            (coordinate.x.toFloat() / imageFinder.image.width.toFloat() * imageSize.width).dp,
                            (coordinate.y.toFloat() / imageFinder.image.height.toFloat() * imageSize.height).dp,
                        )
                        .width(3.dp)
                        .height(3.dp)
                        .background(Color.Green)
                        .alpha(0.5f)
                )
                Box(
                    modifier = Modifier
                        .offset(
                            (coordinate.x.toFloat() / imageFinder.image.width.toFloat() * imageSize.width).dp,
                            (coordinate.y.toFloat() / imageFinder.image.height.toFloat() * imageSize.height).dp,
                        )
                        .width((imageFinder.template.width.toFloat() / imageFinder.image.width.toFloat() * imageSize.width).dp)
                        .height((imageFinder.template.height.toFloat() / imageFinder.image.height.toFloat() * imageSize.height).dp)
                        .border(width = 1.dp, shape = RectangleShape, color = Color.Green)
                        .alpha(0.7f)
                )
            }
        }
    }
}
