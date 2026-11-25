package com.example.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import com.example.model.ClockAnimationStyle
import com.example.model.DisplayMode
import com.example.model.WallpaperOption
import com.example.model.WeatherSnapshot
import com.example.model.WidgetConfig
import com.example.widget.ClockCanvasRenderer
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

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

fun computeDisplayTimeMillis(liveMillis: Long, timeTravelMinutesOfDay: Int?): Long {
  if (timeTravelMinutesOfDay == null) return liveMillis
  val cal = Calendar.getInstance().apply {
    timeInMillis = liveMillis
    set(Calendar.HOUR_OF_DAY, (timeTravelMinutesOfDay / 60).coerceIn(0, 23))
    set(Calendar.MINUTE, (timeTravelMinutesOfDay % 60).coerceIn(0, 59))
  }
  return cal.timeInMillis
}

@Composable
fun LiveClockCanvas(
  config: WidgetConfig,
  timeMillis: Long,
  modifier: Modifier = Modifier,
  weather: WeatherSnapshot = WeatherSnapshot(),
  displayMode: DisplayMode = DisplayMode.HOME_SCREEN,
  timeTravelMinutesOfDay: Int? = null,
) {
  val context = LocalContext.current
  val animateMotion =
    config.animationsEnabled &&
      config.animationStyle != ClockAnimationStyle.NONE &&
      displayMode != DisplayMode.AOD
  val durationMs = (6000f / config.animationSpeed.coerceIn(0.4f, 2.5f)).roundToInt().coerceIn(1500, 15000)
  val animPhase = if (animateMotion) {
    androidx.compose.runtime.key(durationMs) {
      val transition = rememberInfiniteTransition(label = "clock_ambient_motion")
      val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
          animation = tween(durationMillis = durationMs, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
        label = "ambient_phase",
      )
      phase
    }
  } else {
    0f
  }
  val effectiveMillis = computeDisplayTimeMillis(timeMillis, timeTravelMinutesOfDay)

  Canvas(modifier = modifier) {
    if (size.width > 4f && size.height > 4f) {
      drawIntoCanvas { canvas ->
        ClockCanvasRenderer.drawClock(
          context = context,
          canvas = canvas.nativeCanvas,
          width = size.width,
          height = size.height,
          config = config,
          timeMillis = effectiveMillis,
          weather = weather,
          displayMode = displayMode,
          animPhase = animPhase,
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
