package com.example.myapplication

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    UnsplashImageGrid()
                }
            }

        }
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@ExperimentalFoundationApi
@Composable
fun UnsplashImageGrid() {
    // State to hold the list of image URLs
    val imageUrls = remember { mutableStateOf<List<String>>(emptyList()) }

    // Fetch images from Unsplash API
    GlobalScope.launch(Dispatchers.IO) {
        val fetchedUrls = fetchImageUrls()
        imageUrls.value = fetchedUrls
    }

    // Display images in a grid
    LazyVerticalGrid(columns = GridCells.Fixed(3)) {
        items(imageUrls.value.size) { index ->
            ImageItem(imageUrls.value[index])
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun ImageItem(imageUrl: String) {
    val bitmap = remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current

    // Fetch the image bitmap from the URL
    GlobalScope.launch(Dispatchers.IO) {
        val bitmapValue = fetchImageBitmap(imageUrl)
        bitmap.value = bitmapValue
    }

    // Display the image once loaded
    Box(modifier = Modifier.fillMaxSize()) {
        bitmap.value?.let { BitmapPainter(it.asImageBitmap()) }?.let {
//            Image(
//                painter = it,
//                contentDescription = "Processing",
//                modifier = Modifier.fillMaxSize(),
//                contentScale = ContentScale.Crop,
//            )
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .build(),
                placeholder = painterResource(R.drawable.baseline_image_24),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Crop,
            )
        }

    }
}

fun fetchImageUrls(): List<String> {
    val connection =
        URL("https://api.unsplash.com/photos/random?count=20&client_id=y7ndeUsmm12J8G_5KWi9PEVftG4oT4VXIAWxrJTsv8c").openConnection() as HttpURLConnection
    connection.requestMethod = "GET"

    val inputStream = connection.inputStream
    val reader = BufferedReader(InputStreamReader(inputStream))
    val response = StringBuilder()

    var line: String?
    while (reader.readLine().also { line = it } != null) {
        response.append(line)

    }

    val imageUrlList = mutableListOf<String>()
    val jsonArray = JSONArray(response.toString())
    for (i in 0 until jsonArray.length()) {
        val jsonObject = jsonArray.getJSONObject(i)
        val imageUrl = jsonObject.getJSONObject("urls").getString("regular")
        imageUrlList.add(imageUrl)
    }

    return imageUrlList
}

@ExperimentalFoundationApi
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    UnsplashImageGrid()
}

@Throws(IOException::class)
suspend fun fetchImageBitmap(imageUrl: String): Bitmap? {
    val connection = withContext(Dispatchers.IO) {
        URL(imageUrl).openConnection()
    } as HttpURLConnection
    connection.doInput = true
    withContext(Dispatchers.IO) {
        connection.connect()
    }
    val inputStream = connection.inputStream
    val bitmap = BitmapFactory.decodeStream(inputStream)
    withContext(Dispatchers.IO) {
        inputStream.close()
    }
    return bitmap
}
