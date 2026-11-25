package com.example.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.core.graphics.ColorUtils
import com.example.R
import com.example.model.ColorPalettePreset
import com.example.model.WallpaperOption
import kotlin.math.max

/**
 * Lightweight bitmap color sampler that extracts harmonious, contrast-safe
 * clock color palettes from bundled wallpapers or user-picked photos.
 */
object WallpaperColorExtractor {

  fun extractSuggestions(
    context: Context,
    wallpaper: WallpaperOption,
    customPhotoUri: String?,
  ): List<ColorPalettePreset> {
    if (wallpaper == WallpaperOption.AMOLED) {
      return listOf(
        ColorPalettePreset("amoled_ice", "AMOLED Ice", 0xFF030407.toInt(), 0xFF38BDF8.toInt(), 0xFFFFFFFF.toInt(), 0xFF94A3B8.toInt(), 0.0f),
        ColorPalettePreset("amoled_gold", "AMOLED Gold", 0xFF030407.toInt(), 0xFFFBBF24.toInt(), 0xFFFFFBEB.toInt(), 0xFFD6CFC2.toInt(), 0.0f),
      )
    }

    val bmp = loadSampledBitmap(context, wallpaper, customPhotoUri)
      ?: return defaultFallback()

    var sumR = 0L
    var sumG = 0L
    var sumB = 0L
    var count = 0L

    var bestSatColor = 0xFF38BDF8.toInt()
    var bestSatScore = -1f
    var secondColor = 0xFFF43F5E.toInt()
    var secondScore = -1f
    val hsv = FloatArray(3)

    val stepX = max(1, bmp.width / 12)
    val stepY = max(1, bmp.height / 12)
    var x = 0
    while (x < bmp.width) {
      var y = 0
      while (y < bmp.height) {
        val px = bmp.getPixel(x, y)
        val r = Color.red(px)
        val g = Color.green(px)
        val b = Color.blue(px)
        sumR += r
        sumG += g
        sumB += b
        count++

        Color.colorToHSV(px, hsv)
        val score = hsv[1] * 0.7f + hsv[2] * 0.3f
        if (score > bestSatScore && hsv[2] > 0.25f) {
          secondScore = bestSatScore
          secondColor = bestSatColor
          bestSatScore = score
          bestSatColor = px
        } else if (score > secondScore && hsv[2] > 0.25f) {
          secondScore = score
          secondColor = px
        }
        y += stepY
      }
      x += stepX
    }

    if (count == 0L) return defaultFallback()

    val avgColor = Color.rgb((sumR / count).toInt(), (sumG / count).toInt(), (sumB / count).toInt())
    val avgLum = ColorUtils.calculateLuminance(avgColor)
    val darkBg = ColorUtils.blendARGB(avgColor, Color.BLACK, 0.65f)
    val lightBg = ColorUtils.blendARGB(avgColor, Color.WHITE, 0.78f)
    val brightAccent = ensureBrightAccent(bestSatColor)
    val altAccent = ensureBrightAccent(secondColor)

    val matchFg = if (avgLum > 0.48) 0xFF0F172A.toInt() else 0xFFFFFFFF.toInt()
    val matchSec = if (avgLum > 0.48) 0xFF334155.toInt() else 0xFFE2E8F0.toInt()

    return listOf(
      ColorPalettePreset(
        id = "wp_floating",
        name = "Wallpaper Float",
        backgroundColor = darkBg,
        accentColor = brightAccent,
        foregroundColor = matchFg,
        secondaryTextColor = matchSec,
        backgroundAlpha = 0.0f,
      ),
      ColorPalettePreset(
        id = "wp_frosted",
        name = "Tinted Glass",
        backgroundColor = darkBg,
        accentColor = brightAccent,
        foregroundColor = 0xFFFFFFFF.toInt(),
        secondaryTextColor = 0xFFE2E8F0.toInt(),
        backgroundAlpha = 0.42f,
      ),
      ColorPalettePreset(
        id = "wp_complementary",
        name = "Adaptive Card",
        backgroundColor = if (avgLum > 0.45) lightBg else darkBg,
        accentColor = altAccent,
        foregroundColor = if (avgLum > 0.45) 0xFF0F172A.toInt() else 0xFFF8FAFC.toInt(),
        secondaryTextColor = if (avgLum > 0.45) 0xFF475569.toInt() else 0xFFCBD5E1.toInt(),
        backgroundAlpha = 0.92f,
      ),
    )
  }

  private fun ensureBrightAccent(color: Int): Int {
    val hsv = FloatArray(3)
    Color.colorToHSV(color, hsv)
    hsv[1] = hsv[1].coerceAtLeast(0.45f)
    hsv[2] = hsv[2].coerceAtLeast(0.78f)
    return Color.HSVToColor(hsv)
  }

  private fun loadSampledBitmap(
    context: Context,
    wallpaper: WallpaperOption,
    customPhotoUri: String?,
  ): Bitmap? {
    val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
    return when (wallpaper) {
      WallpaperOption.AURORA -> BitmapFactory.decodeResource(context.resources, R.drawable.img_wallpaper_aurora_1795495861674, opts)
      WallpaperOption.TERRACOTTA -> BitmapFactory.decodeResource(context.resources, R.drawable.img_wallpaper_terracotta_1795495862707, opts)
      WallpaperOption.GLASS -> BitmapFactory.decodeResource(context.resources, R.drawable.img_wallpaper_glass_1795495862804, opts)
      WallpaperOption.CUSTOM_PHOTO -> {
        if (customPhotoUri.isNullOrBlank()) null
        else runCatching {
          context.contentResolver.openInputStream(Uri.parse(customPhotoUri))?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
          }
        }.getOrNull()
      }
      WallpaperOption.AMOLED -> null
    }
  }

  private fun defaultFallback(): List<ColorPalettePreset> = listOf(
    ColorPalettePreset("wp_def_1", "Wallpaper Float", 0xFF0A0D14.toInt(), 0xFF38BDF8.toInt(), 0xFFFFFFFF.toInt(), 0xFFE2E8F0.toInt(), 0.0f),
    ColorPalettePreset("wp_def_2", "Tinted Glass", 0xFF0F172A.toInt(), 0xFF7DD3FC.toInt(), 0xFFFFFFFF.toInt(), 0xFFCBD5E1.toInt(), 0.42f),
  )
}
