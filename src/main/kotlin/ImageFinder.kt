import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.image.BufferedImage
import java.util.Calendar

class ImageFinder(image: BufferedImage, template: BufferedImage) {
    private val threshold = template.grayScale().edge().calcBinalizeThreshold()
    val image = image.grayScale().edge().binalized(threshold)
    val template = template.grayScale().edge().binalized(threshold).toTemplateBufferedImage()

    val currentSearchX = mutableStateOf(0)
    val currentSearchY = mutableStateOf(0)

    val searchResult = mutableStateListOf<Pair<Int, Int>>()

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
                currentSearchCoordinateChanged = { coordinate ->
                    currentSearchX.value = coordinate.first
                    currentSearchY.value = coordinate.second
                },
                onFindOut = { coordinate ->
                    searchResult.add(coordinate)
                    println(coordinate)
                },
                onYChanged = { y, height ->
                    percentage.value = y * 100 / height
                },
                onSearchFinished = { result ->
                    println(result)
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

        Image(bitmap = imageFinder.template.toComposeImageBitmap(), null)

        Box {
            Image(
                bitmap = imageFinder.image.toComposeImageBitmap(), null,
                modifier = Modifier
                    .onSizeChanged {
                        imageSize = it
                    }
            )

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
            for (coordinate in imageFinder.searchResult) {
                Box(
                    modifier = Modifier
                        .offset(
                            (coordinate.first.toFloat() / imageFinder.image.width.toFloat() * imageSize.width).dp,
                            (coordinate.second.toFloat() / imageFinder.image.height.toFloat() * imageSize.height).dp,
                        )
                        .width((imageFinder.template.width.toFloat() / imageFinder.image.width.toFloat() * imageSize.width).dp)
                        .height((imageFinder.template.height.toFloat() / imageFinder.image.height.toFloat() * imageSize.height).dp)
                        .background(Color.Red)
                )
            }
        }
    }
}
