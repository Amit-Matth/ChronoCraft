package com.example.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.model.WallpaperOption
import com.example.model.WeatherSnapshot
import com.example.model.WidgetConfig
import com.example.widget.ClockCanvasRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun rememberLiveClockMillis(intervalMs: Long = 1000L): Long {
  var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(intervalMs) {
    while (true) {
      now = System.currentTimeMillis()
      val wait = intervalMs - (now % intervalMs)
      delay(wait.coerceAtLeast(100L))
    }
  }
  return now
}

@Composable
fun LiveClockCanvas(
  config: WidgetConfig,
  timeMillis: Long,
  modifier: Modifier = Modifier,
  weather: WeatherSnapshot = WeatherSnapshot(),
) {
  val context = LocalContext.current
  Canvas(modifier = modifier) {
    if (size.width > 4f && size.height > 4f) {
      drawIntoCanvas { canvas ->
        ClockCanvasRenderer.drawClock(
          context = context,
          canvas = canvas.nativeCanvas,
          width = size.width,
          height = size.height,
          config = config,
          timeMillis = timeMillis,
          weather = weather,
        )
      }
    }
  }
}

@Composable
fun WallpaperBackdrop(
  wallpaper: WallpaperOption,
  customPhotoUri: String?,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val customBitmap by
    produceState<ImageBitmap?>(initialValue = null, wallpaper, customPhotoUri) {
      value =
        if (wallpaper == WallpaperOption.CUSTOM_PHOTO && !customPhotoUri.isNullOrBlank()) {
          withContext(Dispatchers.IO) {
            runCatching {
              val uri = Uri.parse(customPhotoUri)
              context.contentResolver.openInputStream(uri)?.use { stream ->
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
              }
            }.getOrNull()
          }
        } else {
          null
        }
    }

  Box(modifier = modifier) {
    when (wallpaper) {
      WallpaperOption.AURORA ->
        Image(
          painter = painterResource(id = R.drawable.img_wallpaper_aurora_1795495861674),
          contentDescription = "Obsidian Aurora wallpaper",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      WallpaperOption.TERRACOTTA ->
        Image(
          painter = painterResource(id = R.drawable.img_wallpaper_terracotta_1795495862707),
          contentDescription = "Bauhaus Sand wallpaper",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      WallpaperOption.GLASS ->
        Image(
          painter = painterResource(id = R.drawable.img_wallpaper_glass_1795495862804),
          contentDescription = "Frosted Pearl wallpaper",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      WallpaperOption.AMOLED ->
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF030407)))
      WallpaperOption.CUSTOM_PHOTO -> {
        val bmp = customBitmap
        if (bmp != null) {
          Image(
            bitmap = bmp,
            contentDescription = "Custom photo wallpaper",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Image(
            painter = painterResource(id = R.drawable.img_wallpaper_aurora_1795495861674),
            contentDescription = "Fallback aurora wallpaper",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }
  }
}
