import Application.settings
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
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ImageFinder(image: BufferedImage, target: BufferedImage) {
    private val threshold = target.grayScale().calcBinalizeThreshold()
    val image = image.grayScale().binalized(threshold)
    val target = target.grayScale().binalized(threshold)

    val currentSearchX = mutableStateOf(0)
    val currentSearchY = mutableStateOf(0)

    val searchResult = mutableStateListOf<Pair<Int, Int>>()

    var searching = mutableStateOf(false)
    fun startSearching() {
        searching.value = true
        CoroutineScope(Dispatchers.IO).launch {
            image.find(
                target,
                currentSearchCoordinateChanged = { coordinate ->
                    currentSearchX.value = coordinate.first
                    currentSearchY.value = coordinate.second
                },
                onFindOut = { coordinate ->
                    searchResult.add(coordinate)
                    println(coordinate)
                },
                onSearchFinished = { result ->
                    println(result)
                    searching.value = false
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
                }
            ) {
                if (imageFinder.searching.value) {
                    Text("Searching...")
                } else {
                    Text("Start")
                }
            }
        }

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
                        .width((imageFinder.target.width.toFloat() / imageFinder.image.width.toFloat() * imageSize.width).dp)
                        .height((imageFinder.target.height.toFloat() / imageFinder.image.height.toFloat() * imageSize.height).dp)
                        .background(Color.Red)
                )
            }
        }
    }
}