package com.example.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.IconCompat
import com.example.model.AppIconOverride
import com.example.model.IconBadgeStyle
import com.example.model.IconFinishStyle
import com.example.model.IconPackConfig
import com.example.model.IconShapeStyle
import com.example.model.LaunchableAppInfo
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object IconPackRenderer {

  fun loadLaunchableApps(context: Context): List<LaunchableAppInfo> {
    val pm = context.packageManager
    val queryIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      pm.queryIntentActivities(queryIntent, PackageManager.ResolveInfoFlags.of(0L))
    } else {
      @Suppress("DEPRECATION")
      pm.queryIntentActivities(queryIntent, 0)
    }

    val seen = HashSet<String>()
    val result = ArrayList<LaunchableAppInfo>(resolveInfos.size)
    for (info in resolveInfos) {
      val activityInfo = info.activityInfo ?: continue
      val pkg = activityInfo.packageName ?: continue
      val cls = activityInfo.name ?: continue
      val componentKey = "$pkg/$cls"
      if (!seen.add(componentKey)) continue
      val label = info.loadLabel(pm)?.toString()?.trim().takeUnless { it.isNullOrEmpty() } ?: pkg
      val dominant = runCatching {
        val drawable = info.loadIcon(pm)
        extractDominantColor(drawable)
      }.getOrDefault(0xFF38BDF8.toInt())
      result.add(
        LaunchableAppInfo(
          packageName = pkg,
          activityName = cls,
          label = label,
          componentKey = componentKey,
          dominantColor = dominant,
        ),
      )
    }
    return result.sortedBy { it.label.lowercase() }
  }

  fun loadAppIconBitmap(
    context: Context,
    packageName: String,
    activityName: String,
    sizePx: Int = 192,
  ): Bitmap? {
    return runCatching {
      val drawable = context.packageManager.getActivityIcon(ComponentName(packageName, activityName))
      drawableToBitmap(drawable, sizePx)
    }.getOrElse {
      runCatching {
        val drawable = context.packageManager.getApplicationIcon(packageName)
        drawableToBitmap(drawable, sizePx)
      }.getOrNull()
    }
  }

  fun loadCustomPhotoBitmap(context: Context, uriString: String, maxPx: Int = 256): Bitmap? {
    return runCatching {
      val uri = Uri.parse(uriString)
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
      }
      var sample = 1
      val largest = maxOf(bounds.outWidth, bounds.outHeight)
      while (largest > 0 && largest / sample > maxPx * 2) {
        sample *= 2
      }
      val opts = BitmapFactory.Options().apply { inSampleSize = sample }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, opts)
      }
    }.getOrNull()
  }

  private fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
      return Bitmap.createScaledBitmap(drawable.bitmap, sizePx, sizePx, true)
    }
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawable.setBounds(0, 0, sizePx, sizePx)
    drawable.draw(canvas)
    return bmp
  }

  private fun extractDominantColor(drawable: Drawable): Int {
    val bmp = drawableToBitmap(drawable, 24)
    var rSum = 0L
    var gSum = 0L
    var bSum = 0L
    var count = 0L
    for (y in 2 until 22 step 2) {
      for (x in 2 until 22 step 2) {
        val px = bmp.getPixel(x, y)
        val a = AndroidColor.alpha(px)
        if (a < 180) continue
        val r = AndroidColor.red(px)
        val g = AndroidColor.green(px)
        val b = AndroidColor.blue(px)
        val maxC = maxOf(r, g, b)
        val minC = minOf(r, g, b)
        // Prefer colorful pixels over pure white/black backgrounds
        val weight = if (maxC - minC > 28) 3L else 1L
        rSum += r * weight
        gSum += g * weight
        bSum += b * weight
        count += weight
      }
    }
    if (count == 0L) return 0xFF38BDF8.toInt()
    return AndroidColor.argb(
      255,
      (rSum / count).toInt().coerceIn(0, 255),
      (gSum / count).toInt().coerceIn(0, 255),
      (bSum / count).toInt().coerceIn(0, 255),
    )
  }

  fun renderStyledIconBitmap(
    sourceBitmap: Bitmap?,
    customPhotoBitmap: Bitmap?,
    config: IconPackConfig,
    dominantAppColor: Int,
    override: AppIconOverride? = null,
    sizePx: Int = 256,
  ): Bitmap {
    val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val pad = sizePx * 0.085f
    val rect = RectF(pad, pad, sizePx - pad, sizePx - pad)
    val shapePath = buildShapePath(rect, config.shapeStyle, config.cornerRadiusRatio)

    val dynamicAccent = override?.customTintColor ?: if (config.useDynamicAppColor) dominantAppColor else config.iconTintColor
    val effectiveBorderColor = override?.customTintColor ?: if (config.useDynamicAppColor) dominantAppColor else config.borderColor

    // 1. Optional glow or drop shadow under the tile
    if (config.showGlowHalo && (config.finishStyle == IconFinishStyle.NEON_GLOW || config.finishStyle == IconFinishStyle.DUOTONE_GRADIENT || config.useDynamicAppColor)) {
      val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(effectiveBorderColor, 125)
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(sizePx * 0.06f, BlurMaskFilter.Blur.NORMAL)
      }
      canvas.drawPath(shapePath, glowPaint)
    } else if (config.showDropShadow) {
      val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(84, 0, 0, 0)
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(sizePx * 0.045f, BlurMaskFilter.Blur.NORMAL)
      }
      canvas.save()
      canvas.translate(0f, sizePx * 0.025f)
      canvas.drawPath(shapePath, shadowPaint)
      canvas.restore()
    }

    // 2. Clip inside shape and paint tile background
    canvas.save()
    canvas.clipPath(shapePath)

    if (customPhotoBitmap != null) {
      val scaled = Bitmap.createScaledBitmap(customPhotoBitmap, sizePx, sizePx, true)
      canvas.drawBitmap(scaled, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    } else {
      val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
      when (config.finishStyle) {
        IconFinishStyle.GLASS_FROST -> {
          bgPaint.shader = LinearGradient(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            intArrayOf(
              ColorUtils.setAlphaComponent(config.backgroundColor, 95),
              ColorUtils.setAlphaComponent(config.secondaryColor, 55),
            ),
            null,
            Shader.TileMode.CLAMP,
          )
        }
        IconFinishStyle.DUOTONE_GRADIENT -> {
          bgPaint.shader = LinearGradient(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            intArrayOf(config.backgroundColor, config.secondaryColor),
            null,
            Shader.TileMode.CLAMP,
          )
        }
        IconFinishStyle.NEON_GLOW -> {
          val darkBg = ColorUtils.blendARGB(config.backgroundColor, 0xFF050814.toInt(), 0.55f)
          bgPaint.shader = LinearGradient(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            intArrayOf(darkBg, ColorUtils.blendARGB(darkBg, effectiveBorderColor, 0.16f)),
            null,
            Shader.TileMode.CLAMP,
          )
        }
        IconFinishStyle.PASTEL_CLAY -> {
          bgPaint.shader = LinearGradient(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            intArrayOf(
              ColorUtils.blendARGB(config.backgroundColor, AndroidColor.WHITE, 0.18f),
              config.backgroundColor,
            ),
            null,
            Shader.TileMode.CLAMP,
          )
        }
        IconFinishStyle.OLED_OUTLINE -> {
          bgPaint.color = 0xFF050507.toInt()
        }
        IconFinishStyle.ORIGINAL_FRAMED,
        IconFinishStyle.MONOCHROME_INK -> {
          val base = if (config.useDynamicAppColor) {
            ColorUtils.blendARGB(config.backgroundColor, dominantAppColor, 0.24f)
          } else {
            config.backgroundColor
          }
          bgPaint.color = base
        }
      }
      canvas.drawRect(rect, bgPaint)

      // Soft top sheen for Frosted Glass & Clay
      if (config.finishStyle == IconFinishStyle.GLASS_FROST || config.finishStyle == IconFinishStyle.PASTEL_CLAY) {
        val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader = LinearGradient(
            rect.left,
            rect.top,
            rect.left,
            rect.top + rect.height() * 0.45f,
            AndroidColor.argb(68, 255, 255, 255),
            AndroidColor.TRANSPARENT,
            Shader.TileMode.CLAMP,
          )
        }
        canvas.drawRect(rect, sheenPaint)
      }

      // 3. Draw App Icon Glyph
      if (sourceBitmap != null) {
        val innerSize = (rect.width() * config.iconScale.coerceIn(0.42f, 0.88f)).toInt().coerceAtLeast(32)
        val innerLeft = rect.centerX() - innerSize / 2f
        val innerTop = rect.centerY() - innerSize / 2f
        val innerRect = RectF(innerLeft, innerTop, innerLeft + innerSize, innerTop + innerSize)

        val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val shouldTintMonochrome =
          config.monochromeGlyph ||
            config.finishStyle == IconFinishStyle.MONOCHROME_INK ||
            config.finishStyle == IconFinishStyle.OLED_OUTLINE

        if (shouldTintMonochrome) {
          // Convert icon luminance into unified tint color while preserving edges/shapes
          val tint = dynamicAccent
          val r = AndroidColor.red(tint) / 255f
          val g = AndroidColor.green(tint) / 255f
          val b = AndroidColor.blue(tint) / 255f
          val matrix = ColorMatrix(
            floatArrayOf(
              0.21f * r, 0.72f * r, 0.07f * r, 0f, 35f * r,
              0.21f * g, 0.72f * g, 0.07f * g, 0f, 35f * g,
              0.21f * b, 0.72f * b, 0.07f * b, 0f, 35f * b,
              0f, 0f, 0f, 1f, 0f,
            ),
          )
          glyphPaint.colorFilter = ColorMatrixColorFilter(matrix)
        } else if (config.finishStyle == IconFinishStyle.PASTEL_CLAY) {
          val satMatrix = ColorMatrix().apply { setSaturation(0.75f) }
          glyphPaint.colorFilter = ColorMatrixColorFilter(satMatrix)
        } else if (override?.customTintColor != null) {
          glyphPaint.colorFilter = PorterDuffColorFilter(override.customTintColor, PorterDuff.Mode.SRC_ATOP)
        }

        // Clip glyph slightly inside an inner squircle if it's a full-square legacy icon
        val glyphClipPath = Path().apply {
          val cr = innerRect.width() * 0.24f
          addRoundRect(innerRect, cr, cr, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(glyphClipPath)
        canvas.drawBitmap(sourceBitmap, null, innerRect, glyphPaint)
        canvas.restore()
      }
    }

    // 4. Optional decorative badge
    when (config.badgeStyle) {
      IconBadgeStyle.NONE -> Unit
      IconBadgeStyle.DOT -> {
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dynamicAccent }
        canvas.drawCircle(rect.right - rect.width() * 0.16f, rect.top + rect.height() * 0.16f, rect.width() * 0.055f, dotPaint)
      }
      IconBadgeStyle.SPARK -> {
        val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = ColorUtils.setAlphaComponent(dynamicAccent, 220)
          style = Paint.Style.STROKE
          strokeWidth = sizePx * 0.014f
          strokeCap = Paint.Cap.ROUND
        }
        val cx = rect.right - rect.width() * 0.18f
        val cy = rect.top + rect.height() * 0.18f
        val r = rect.width() * 0.06f
        canvas.drawLine(cx - r, cy, cx + r, cy, sparkPaint)
        canvas.drawLine(cx, cy - r, cx, cy + r, sparkPaint)
      }
      IconBadgeStyle.RING -> {
        val inset = rect.width() * 0.085f
        val innerR = RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset)
        val ringPath = buildShapePath(innerR, config.shapeStyle, config.cornerRadiusRatio)
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = ColorUtils.setAlphaComponent(dynamicAccent, 110)
          style = Paint.Style.STROKE
          strokeWidth = sizePx * 0.012f
        }
        canvas.drawPath(ringPath, ringPaint)
      }
    }

    canvas.restore()

    // 5. Outer border stroke
    val borderStrokePx = (config.borderWidthDp / 48f) * rect.width()
    if (borderStrokePx > 0.5f || config.finishStyle == IconFinishStyle.NEON_GLOW || config.finishStyle == IconFinishStyle.OLED_OUTLINE) {
      val finalStroke = if (borderStrokePx > 0.5f) borderStrokePx else sizePx * 0.018f
      val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = finalStroke
        color = when (config.finishStyle) {
          IconFinishStyle.GLASS_FROST -> ColorUtils.setAlphaComponent(AndroidColor.WHITE, 145)
          else -> ColorUtils.setAlphaComponent(effectiveBorderColor, 225)
        }
      }
      canvas.drawPath(shapePath, borderPaint)
    }

    return output
  }

  fun buildShapePath(rect: RectF, shape: IconShapeStyle, cornerRatio: Float): Path {
    val path = Path()
    val w = rect.width()
    val h = rect.height()
    val cx = rect.centerX()
    val cy = rect.centerY()

    when (shape) {
      IconShapeStyle.CIRCLE -> {
        path.addOval(rect, Path.Direction.CW)
      }
      IconShapeStyle.ROUNDED_SQUARE -> {
        val r = w * cornerRatio.coerceIn(0.10f, 0.48f)
        path.addRoundRect(rect, r, r, Path.Direction.CW)
      }
      IconShapeStyle.SQUIRCLE -> {
        // Superellipse-inspired smooth continuous corners
        val r = w * 0.36f
        path.addRoundRect(rect, r, r, Path.Direction.CW)
      }
      IconShapeStyle.TEARDROP -> {
        val bigR = w * 0.48f
        val smallR = w * 0.14f
        path.addRoundRect(
          rect,
          floatArrayOf(bigR, bigR, bigR, bigR, smallR, smallR, bigR, bigR),
          Path.Direction.CW,
        )
      }
      IconShapeStyle.HEXAGON -> {
        val rx = w * 0.48f
        val ry = h * 0.50f
        for (i in 0 until 6) {
          val angle = (PI / 3.0) * i - PI / 2.0
          val px = cx + (rx * cos(angle)).toFloat()
          val py = cy + (ry * sin(angle)).toFloat()
          if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
      }
      IconShapeStyle.SOFT_CUT -> {
        val cut = w * 0.22f
        path.moveTo(rect.left + cut, rect.top)
        path.lineTo(rect.right - cut, rect.top)
        path.lineTo(rect.right, rect.top + cut)
        path.lineTo(rect.right, rect.bottom - cut)
        path.lineTo(rect.right - cut, rect.bottom)
        path.lineTo(rect.left + cut, rect.bottom)
        path.lineTo(rect.left, rect.bottom - cut)
        path.lineTo(rect.left, rect.top + cut)
        path.close()
      }
    }
    return path
  }

  fun pinThemedShortcutToHome(
    context: Context,
    app: LaunchableAppInfo,
    config: IconPackConfig,
    override: AppIconOverride?,
    externalPack: com.example.model.ExternalIconPackInfo? = null,
  ): Boolean {
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
      return false
    }
    val extBmp = IconPackImporter.resolveExternalIconBitmap(context, externalPack, app, override, sizePx = 192)
    val rawAppBmp = extBmp ?: loadAppIconBitmap(context, app.packageName, app.activityName, sizePx = 192)
    val customPhotoBmp = override?.customPhotoUri?.let { loadCustomPhotoBitmap(context, it) }
    val styledBitmap = renderStyledIconBitmap(
      sourceBitmap = rawAppBmp,
      customPhotoBitmap = customPhotoBmp,
      config = config,
      dominantAppColor = app.dominantColor,
      override = override,
      sizePx = 256,
    )

    val launchIntent = Intent(Intent.ACTION_MAIN).apply {
      addCategory(Intent.CATEGORY_LAUNCHER)
      component = ComponentName(app.packageName, app.activityName)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
    }

    val label = override?.customLabel?.trim()?.takeIf { it.isNotEmpty() } ?: app.label
    val shortcutId = "chrono_icon_${app.packageName}_${app.activityName}".replace('.', '_').take(64)

    val shortcut = ShortcutInfoCompat.Builder(context, shortcutId)
      .setShortLabel(label)
      .setLongLabel(label)
      .setIcon(IconCompat.createWithBitmap(styledBitmap))
      .setIntent(launchIntent)
      .build()

    return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
  }
}
