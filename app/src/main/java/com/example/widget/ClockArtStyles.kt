package com.example.widget

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.os.BatteryManager
import androidx.core.graphics.ColorUtils
import com.example.model.CardStyle
import com.example.model.WeatherSnapshot
import com.example.model.WidgetBackdropId
import com.example.model.WidgetConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import kotlin.math.roundToInt

internal object ClockArtStyles {

  private val backdropCache = ConcurrentHashMap<Int, Bitmap>()

  fun getBatteryPercent(context: Context): Int {
    return runCatching {
      val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
      val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
      val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
      if (level >= 0 && scale > 0) ((level * 100f) / scale).roundToInt().coerceIn(0, 100) else 85
    }.getOrDefault(85)
  }

  private fun loadBackdropBitmap(context: Context, resId: Int): Bitmap? {
    backdropCache[resId]?.let { if (!it.isRecycled) return it }
    return runCatching {
      val opts = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
      }
      BitmapFactory.decodeResource(context.resources, resId, opts)
      val maxDim = maxOf(opts.outWidth, opts.outHeight)
      var sample = 1
      while (maxDim / sample > 640) sample *= 2
      val decodeOpts = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
      }
      BitmapFactory.decodeResource(context.resources, resId, decodeOpts)?.also {
        backdropCache[resId] = it
      }
    }.getOrNull()
  }

  fun drawRoundedBackdropArtwork(
    context: Context,
    canvas: Canvas,
    width: Float,
    height: Float,
    cornerPx: Float,
    backdropId: WidgetBackdropId,
    config: WidgetConfig,
  ) {
    val resId = backdropId.drawableRes ?: return
    val bmp = loadBackdropBitmap(context, resId) ?: return
    val rect = RectF(0f, 0f, width, height)
    val clipPath = Path().apply {
      addRoundRect(rect, cornerPx, cornerPx, Path.Direction.CW)
    }

    canvas.save()
    canvas.clipPath(clipPath)

    // Center-crop image into widget bounds
    val scale = maxOf(width / bmp.width.toFloat(), height / bmp.height.toFloat())
    val drawW = bmp.width * scale
    val drawH = bmp.height * scale
    val left = (width - drawW) / 2f
    val top = (height - drawH) / 2f
    val dst = RectF(left, top, left + drawW, top + drawH)
    val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(bmp, null, dst, imgPaint)

    // Subtle legibility vignette for dark scenes (skip heavy scrim for pastel Kawaii Cat)
    if (backdropId != WidgetBackdropId.KAWAII_KITTY) {
      val scrim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
          0f,
          0f,
          0f,
          height,
          Color.argb(62, 6, 8, 18),
          Color.argb(28, 6, 8, 18),
          Shader.TileMode.CLAMP,
        )
      }
      canvas.drawRect(rect, scrim)
    }
    canvas.restore()

    if (config.showBorder) {
      val strokeW = (min(width, height) * 0.012f).coerceIn(1.5f, 3.5f)
      val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeW
        color = Color.argb(70, 255, 255, 255)
      }
      canvas.drawRoundRect(
        RectF(strokeW / 2f, strokeW / 2f, width - strokeW / 2f, height - strokeW / 2f),
        cornerPx,
        cornerPx,
        borderPaint,
      )
    }
  }

  private fun fmtHour(cal: Calendar, is24Hour: Boolean): String {
    val h = if (is24Hour) cal.get(Calendar.HOUR_OF_DAY) else cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    return if (is24Hour) String.format(Locale.US, "%02d", h) else h.toString()
  }

  private fun fmtMin(cal: Calendar): String = String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE))

  private fun fmtDate(cal: Calendar): String =
    SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(cal.time)

  private fun fitText(paint: Paint, text: String, maxW: Float, maxH: Float, startSize: Float): Float {
    var size = startSize
    val b = Rect()
    while (size > 8f) {
      paint.textSize = size
      paint.getTextBounds(text, 0, text.length, b)
      if (b.width() <= maxW && b.height() <= maxH) break
      size *= 0.9f
    }
    return size
  }

  // ---------------------------------------------------------------------------
  // 27. NEON FRAME GLOW (Cyan-Magenta Glowing Frame & Gradient Time)
  // ---------------------------------------------------------------------------
  fun drawNeonFrameGlow(
    canvas: Canvas,
    width: Float,
    height: Float,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
    weather: WeatherSnapshot,
  ) {
    val minD = min(width, height)
    val c1 = config.accentColor
    val c2 = config.foregroundColor
    val r = (minD * 0.16f).coerceIn(14f, 34f)
    val frameRect = RectF(rect.left + 2f, rect.top + 2f, rect.right - 2f, rect.bottom - 2f)

    // Layered neon tube glow border
    for (pass in listOf(14f to 42, 8f to 90, 3.2f to 255)) {
      val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = pass.first * (minD / 220f).coerceIn(0.65f, 1.35f)
        alpha = pass.second
        shader = LinearGradient(
          frameRect.left,
          frameRect.top,
          frameRect.right,
          frameRect.bottom,
          c1,
          c2,
          Shader.TileMode.CLAMP,
        )
      }
      canvas.drawRoundRect(frameRect, r, r, glowPaint)
    }

    val timeStr = "${fmtHour(cal, config.is24Hour)}:${fmtMin(cal)}"
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      textAlign = Paint.Align.CENTER
      isFakeBoldText = true
    }
    tp.textSize = fitText(tp, timeStr, rect.width() * 0.80f, rect.height() * 0.50f, minD * 0.44f * config.fontScale)
    val fm = tp.fontMetrics
    val baseline = rect.centerY() - (fm.ascent + fm.descent) / 2f - (if (config.showDate) rect.height() * 0.08f else 0f)

    tp.shader = LinearGradient(
      rect.centerX() - rect.width() * 0.30f,
      baseline - tp.textSize,
      rect.centerX() + rect.width() * 0.30f,
      baseline,
      c2,
      c1,
      Shader.TileMode.CLAMP,
    )
    tp.setShadowLayer(12f, 0f, 0f, ColorUtils.setAlphaComponent(c1, 140))
    canvas.drawText(timeStr, rect.centerX(), baseline, tp)

    val sub = buildString {
      if (config.showDate) append(fmtDate(cal))
      if (config.showWeather && weather.isLoaded) {
        if (isNotEmpty()) append("  •  ")
        append(weather.displayTemp(config.useFahrenheit))
      }
    }
    if (sub.isNotEmpty()) {
      val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.secondaryTextColor
        textAlign = Paint.Align.CENTER
        textSize = (minD * 0.088f).coerceIn(9f, 17f)
      }
      canvas.drawText(sub, rect.centerX(), frameRect.bottom - minD * 0.09f, sp)
    }
  }

  // ---------------------------------------------------------------------------
  // 28. CIRCULAR BATTERY RING (Glowing Progress Arc + Live Device Battery)
  // ---------------------------------------------------------------------------
  fun drawCircularBatteryRing(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
    weather: WeatherSnapshot,
    batteryPercent: Int,
  ) {
    val w = rect.width()
    val h = rect.height()
    val wide = w > h * 1.48f
    val ringR = if (wide) h * 0.44f else min(w, h) * 0.44f
    val cx = if (wide) rect.left + ringR + 8f else rect.centerX()
    val cy = rect.centerY()
    val strokeW = (ringR * 0.13f).coerceIn(5f, 15f)
    val arcBounds = RectF(cx - ringR + strokeW / 2f, cy - ringR + strokeW / 2f, cx + ringR - strokeW / 2f, cy + ringR - strokeW / 2f)

    // Subtle track
    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = strokeW
      color = ColorUtils.setAlphaComponent(config.foregroundColor, 34)
    }
    canvas.drawOval(arcBounds, trackPaint)

    // Glowing gradient sweep arc for battery %
    val sweepAngle = (360f * (batteryPercent.coerceIn(5, 100) / 100f))
    val c1 = config.accentColor
    val c2 = config.secondaryTextColor
    val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = strokeW
      strokeCap = Paint.Cap.ROUND
      shader = SweepGradient(cx, cy, intArrayOf(c2, c1, c2), floatArrayOf(0f, 0.55f, 1f))
      setShadowLayer(10f * config.depthIntensity, 0f, 0f, ColorUtils.setAlphaComponent(c1, 140))
    }
    canvas.drawArc(arcBounds, -90f, sweepAngle, false, arcPaint)

    // Lightning bolt badge at top of ring
    val boltR = strokeW * 0.65f
    val boltCy = arcBounds.top
    val boltPath = Path().apply {
      moveTo(cx + boltR * 0.18f, boltCy - boltR)
      lineTo(cx - boltR * 0.52f, boltCy + boltR * 0.12f)
      lineTo(cx + boltR * 0.05f, boltCy + boltR * 0.12f)
      lineTo(cx - boltR * 0.22f, boltCy + boltR)
      lineTo(cx + boltR * 0.55f, boltCy - boltR * 0.10f)
      lineTo(cx - boltR * 0.05f, boltCy - boltR * 0.10f)
      close()
    }
    canvas.drawPath(boltPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c2 })

    // Center time + date + battery
    val timeStr = "${fmtHour(cal, config.is24Hour)}:${fmtMin(cal)}"
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      textAlign = Paint.Align.CENTER
      isFakeBoldText = true
      setShadowLayer(8f, 0f, 2f, Color.argb(140, 0, 0, 0))
    }
    tp.textSize = fitText(tp, timeStr, ringR * 1.32f, ringR * 0.56f, ringR * 0.52f * config.fontScale)
    val fm = tp.fontMetrics
    canvas.drawText(timeStr, cx, cy - (fm.ascent + fm.descent) / 2f - ringR * 0.08f, tp)

    val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      alpha = 210
      textAlign = Paint.Align.CENTER
      textSize = (ringR * 0.17f).coerceIn(8f, 15f)
    }
    if (config.showDate) {
      canvas.drawText(fmtDate(cal), cx, cy + ringR * 0.30f, subP)
    }
    subP.color = c1
    canvas.drawText("⚡ $batteryPercent%", cx, cy + ringR * 0.54f, subP)

    if (wide) {
      val rx = cx + ringR + h * 0.14f
      val rp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.foregroundColor
        textSize = (h * 0.18f).coerceIn(11f, 20f)
        setShadowLayer(8f, 0f, 2f, Color.argb(140, 0, 0, 0))
      }
      if (config.showWeather && weather.isLoaded) {
        val iconR = (h * 0.13f).coerceIn(9f, 18f)
        ClockAdvancedStyles.drawWeatherIcon(canvas, rx + iconR, rect.centerY() - h * 0.12f, iconR, weather.weatherCode, weather.isDay, c1, config.foregroundColor)
        canvas.drawText("${weather.displayTemp(config.useFahrenheit)} ${weather.conditionText()}", rx + iconR * 2.5f, rect.centerY() - h * 0.06f, rp)
        rp.color = config.secondaryTextColor
        rp.textSize = (h * 0.14f).coerceIn(10f, 17f)
        canvas.drawText(weather.cityName, rx, rect.centerY() + h * 0.22f, rp)
      } else {
        canvas.drawText(config.customLabel.ifBlank { fmtDate(cal) }, rx, rect.centerY(), rp)
      }
    }
  }

  // ---------------------------------------------------------------------------
  // 29. HANDWRITTEN SCRIPT (Expressive Brush Typography + Swoop & Heart)
  // ---------------------------------------------------------------------------
  fun drawHandwrittenScript(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
    weather: WeatherSnapshot,
  ) {
    val w = rect.width()
    val h = rect.height()
    val minD = min(w, h)
    val timeStr = "${fmtHour(cal, config.is24Hour)}:${fmtMin(cal)}"

    canvas.save()
    canvas.rotate(-5.5f, rect.centerX(), rect.centerY())

    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      textAlign = Paint.Align.CENTER
      setShadowLayer(12f, 0f, 4f, Color.argb(165, 0, 0, 0))
    }
    tp.textSize = fitText(tp, timeStr, w * 0.84f, h * 0.50f, minD * 0.56f * config.fontScale)
    val fm = tp.fontMetrics
    val timeY = rect.centerY() - (fm.ascent + fm.descent) / 2f - h * 0.07f
    canvas.drawText(timeStr, rect.centerX(), timeY, tp)

    val subStr = buildString {
      if (config.showDate) append(fmtDate(cal))
      if (config.showWeather && weather.isLoaded) {
        if (isNotEmpty()) append(" · ")
        append(weather.displayTemp(config.useFahrenheit))
      }
    }
    val dp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      textAlign = Paint.Align.CENTER
      textSize = (minD * 0.125f).coerceIn(11f, 22f)
      setShadowLayer(8f, 0f, 3f, Color.argb(150, 0, 0, 0))
    }
    if (subStr.isNotEmpty()) {
      canvas.drawText(subStr, rect.centerX() - minD * 0.04f, timeY + dp.textSize * 1.65f, dp)
    }

    // Curved brush underline
    val swash = Path().apply {
      val sy = timeY + dp.textSize * 2.15f
      moveTo(rect.centerX() - w * 0.24f, sy + minD * 0.03f)
      quadTo(rect.centerX(), sy - minD * 0.03f, rect.centerX() + w * 0.20f, sy + minD * 0.01f)
    }
    val swashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = (minD * 0.022f).coerceIn(2f, 4.5f)
      strokeCap = Paint.Cap.ROUND
      color = config.accentColor
    }
    canvas.drawPath(swash, swashPaint)

    // Hand-drawn heart accent on right
    val hx = rect.centerX() + w * 0.28f
    val hy = timeY + dp.textSize * 1.45f
    val hr = (minD * 0.055f).coerceIn(5f, 11f)
    val heart = Path().apply {
      moveTo(hx, hy + hr * 0.9f)
      cubicTo(hx - hr * 1.4f, hy - hr * 0.1f, hx - hr * 0.6f, hy - hr * 1.2f, hx, hy - hr * 0.35f)
      cubicTo(hx + hr * 0.6f, hy - hr * 1.2f, hx + hr * 1.4f, hy - hr * 0.1f, hx, hy + hr * 0.9f)
    }
    canvas.drawPath(
      heart,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (minD * 0.018f).coerceIn(1.8f, 3.5f)
        strokeJoin = Paint.Join.ROUND
        color = config.accentColor
      },
    )
    canvas.restore()
  }

  // ---------------------------------------------------------------------------
  // 30. PIXEL SPACE RETRO (8-Bit Crisp Digits + Offset Shadow Over Pixel Art)
  // ---------------------------------------------------------------------------
  fun drawPixelSpaceRetro(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
    weather: WeatherSnapshot,
  ) {
    val w = rect.width()
    val h = rect.height()
    val minD = min(w, h)
    val timeStr = "${fmtHour(cal, config.is24Hour)}:${fmtMin(cal)}"

    // Position cleanly in the upper starry space sky above the pixel moon/astronaut
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      textAlign = Paint.Align.CENTER
    }
    tp.textSize = fitText(tp, timeStr, w * 0.86f, h * 0.38f, minD * 0.36f * config.fontScale)
    val yCenter = rect.top + h * 0.30f

    // Hard 8-bit purple/accent drop shadow
    val off = (minD * 0.022f).coerceIn(2.5f, 6f)
    tp.color = config.accentColor
    canvas.drawText(timeStr, rect.centerX() + off, yCenter + off, tp)

    tp.color = config.foregroundColor
    canvas.drawText(timeStr, rect.centerX(), yCenter, tp)

    val sub = buildString {
      if (config.showDate) append(fmtDate(cal))
      if (config.showWeather && weather.isLoaded) {
        if (isNotEmpty()) append(" • ")
        append(weather.displayTemp(config.useFahrenheit))
      }
    }
    if (sub.isNotEmpty()) {
      val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.secondaryTextColor
        textAlign = Paint.Align.CENTER
        textSize = (minD * 0.082f).coerceIn(8f, 15f)
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
      }
      canvas.drawText(sub, rect.centerX(), yCenter + sp.textSize * 1.9f, sp)
    }
  }

  // ---------------------------------------------------------------------------
  // 31. SUNSET SCENIC ART (Editorial Serif + Diamond Rule Over Sunset Sky)
  // ---------------------------------------------------------------------------
  fun drawSunsetScenicArt(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
    weather: WeatherSnapshot,
  ) {
    val w = rect.width()
    val h = rect.height()
    val minD = min(w, h)
    val timeStr = "${fmtHour(cal, config.is24Hour)}:${fmtMin(cal)}"

    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      textAlign = Paint.Align.CENTER
      setShadowLayer(12f, 0f, 3f, Color.argb(140, 20, 8, 35))
    }
    tp.textSize = fitText(tp, timeStr, w * 0.88f, h * 0.42f, minD * 0.48f * config.fontScale)
    val timeY = rect.top + h * 0.42f
    canvas.drawText(timeStr, rect.centerX(), timeY, tp)

    val dp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.secondaryTextColor
      textAlign = Paint.Align.CENTER
      letterSpacing = 0.08f
      textSize = (minD * 0.092f).coerceIn(10f, 18f)
      setShadowLayer(6f, 0f, 2f, Color.argb(150, 0, 0, 0))
    }
    val sub = buildString {
      if (config.showDate) append(fmtDate(cal))
      if (config.showWeather && weather.isLoaded) {
        if (isNotEmpty()) append("  •  ")
        append(weather.displayTemp(config.useFahrenheit))
      }
    }
    if (sub.isNotEmpty()) {
      canvas.drawText(sub, rect.centerX(), timeY + dp.textSize * 1.55f, dp)
    }

    // Warm golden hairline + center diamond ornament
    val lineY = timeY + dp.textSize * 2.35f
    val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = config.accentColor
      strokeWidth = 2f
    }
    val half = w * 0.28f
    val d = (minD * 0.025f).coerceIn(3f, 6f)
    canvas.drawLine(rect.centerX() - half, lineY, rect.centerX() - d * 1.8f, lineY, lineP)
    canvas.drawLine(rect.centerX() + d * 1.8f, lineY, rect.centerX() + half, lineY, lineP)
    val diamond = Path().apply {
      moveTo(rect.centerX(), lineY - d)
      lineTo(rect.centerX() + d, lineY)
      lineTo(rect.centerX(), lineY + d)
      lineTo(rect.centerX() - d, lineY)
      close()
    }
    canvas.drawPath(diamond, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = config.accentColor })
  }

  // ---------------------------------------------------------------------------
  // 32. DREAMY NIGHT ART (Crescent Moon Clouds + Luminous Floating Clock)
  // ---------------------------------------------------------------------------
  fun drawDreamyNightArt(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
    weather: WeatherSnapshot,
  ) {
    val w = rect.width()
    val h = rect.height()
    val minD = min(w, h)
    val timeStr = "${fmtHour(cal, config.is24Hour)}:${fmtMin(cal)}"

    // Shift slightly right & mid-upper to harmonize with the top-left crescent moon
    val cx = rect.centerX() + w * 0.04f
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      textAlign = Paint.Align.CENTER
      isFakeBoldText = true
      setShadowLayer(14f, 0f, 4f, Color.argb(170, 12, 8, 36))
    }
    tp.textSize = fitText(tp, timeStr, w * 0.78f, h * 0.40f, minD * 0.44f * config.fontScale)
    val cy = rect.centerY() - h * 0.04f
    canvas.drawText(timeStr, cx, cy, tp)

    val dp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.secondaryTextColor
      textAlign = Paint.Align.CENTER
      textSize = (minD * 0.095f).coerceIn(10f, 18f)
      setShadowLayer(8f, 0f, 2f, Color.argb(160, 0, 0, 0))
    }
    val sub = buildString {
      if (config.showDate) append(fmtDate(cal))
      if (config.showWeather && weather.isLoaded) {
        if (isNotEmpty()) append("  •  ")
        append(weather.displayTemp(config.useFahrenheit))
      }
    }
    if (sub.isNotEmpty()) {
      canvas.drawText(sub, cx, cy + dp.textSize * 1.65f, dp)
    }
  }

  // ---------------------------------------------------------------------------
  // 33. KAWAII KITTY PEEK (Crisp Warm Ink Time Inside Peeking Kitty's Card)
  // ---------------------------------------------------------------------------
  fun drawKawaiiKittyPeek(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
    weather: WeatherSnapshot,
  ) {
    val w = rect.width()
    val h = rect.height()
    val minD = min(w, h)
    val timeStr = "${fmtHour(cal, config.is24Hour)}:${fmtMin(cal)}"

    // Lower cream box spans roughly 46%..88% of widget height
    val boxTop = rect.top + h * 0.45f
    val boxBottom = rect.bottom - h * 0.08f
    val boxCenterY = (boxTop + boxBottom) / 2f

    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      textAlign = Paint.Align.CENTER
      isFakeBoldText = true
    }
    tp.textSize = fitText(tp, timeStr, w * 0.74f, (boxBottom - boxTop) * 0.56f, minD * 0.36f * config.fontScale)
    val fm = tp.fontMetrics
    val baseline = boxCenterY - (fm.ascent + fm.descent) / 2f - h * 0.04f
    canvas.drawText(timeStr, rect.centerX(), baseline, tp)

    val sub = buildString {
      if (config.showDate) append(fmtDate(cal))
      if (config.showWeather && weather.isLoaded) {
        if (isNotEmpty()) append("  ♥  ")
        append(weather.displayTemp(config.useFahrenheit))
      }
    }
    if (sub.isNotEmpty()) {
      val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.secondaryTextColor
        textAlign = Paint.Align.CENTER
        textSize = (minD * 0.082f).coerceIn(8f, 15f)
      }
      canvas.drawText(sub, rect.centerX(), boxBottom - minD * 0.03f, sp)
    }
  }

  // ---------------------------------------------------------------------------
  // 34. PLANET GLOW ART (Cosmic Rim + Frosted Pill Capsule)
  // ---------------------------------------------------------------------------
  fun drawPlanetGlowArt(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
    weather: WeatherSnapshot,
  ) {
    val w = rect.width()
    val h = rect.height()
    val minD = min(w, h)
    val timeStr = "${fmtHour(cal, config.is24Hour)}:${fmtMin(cal)}"

    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      textAlign = Paint.Align.CENTER
      isFakeBoldText = true
      setShadowLayer(12f, 0f, 3f, Color.argb(180, 0, 0, 0))
    }
    tp.textSize = fitText(tp, timeStr, w * 0.80f, h * 0.42f, minD * 0.45f * config.fontScale)
    val fm = tp.fontMetrics
    val timeY = rect.centerY() - (fm.ascent + fm.descent) / 2f - h * 0.11f
    canvas.drawText(timeStr, rect.centerX(), timeY, tp)

    if (config.showDate) {
      val dp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.secondaryTextColor
        textAlign = Paint.Align.CENTER
        textSize = (minD * 0.095f).coerceIn(10f, 18f)
      }
      canvas.drawText(fmtDate(cal), rect.centerX(), rect.centerY() + h * 0.11f, dp)
    }

    // Translucent glass capsule pill at bottom
    val pillText = buildString {
      if (config.showWeather && weather.isLoaded) {
        append("${weather.displayTemp(config.useFahrenheit)} • ${weather.cityName}")
      } else {
        append(config.customLabel.ifBlank { "CHRONO" })
      }
    }
    val pillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      textAlign = Paint.Align.CENTER
      textSize = (minD * 0.082f).coerceIn(8f, 15f)
    }
    val tw = pillP.measureText(pillText)
    val pillH = pillP.textSize * 1.95f
    val pillW = (tw + minD * 0.16f).coerceAtMost(w * 0.86f)
    val pillY = rect.bottom - pillH - minD * 0.04f
    val pillRect = RectF(rect.centerX() - pillW / 2f, pillY, rect.centerX() + pillW / 2f, pillY + pillH)
    canvas.drawRoundRect(
      pillRect,
      pillH / 2f,
      pillH / 2f,
      Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(120, 20, 22, 38) },
    )
    canvas.drawRoundRect(
      pillRect,
      pillH / 2f,
      pillH / 2f,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.4f
        color = ColorUtils.setAlphaComponent(config.accentColor, 150)
      },
    )
    val pfm = pillP.fontMetrics
    canvas.drawText(pillText, pillRect.centerX(), pillRect.centerY() - (pfm.ascent + pfm.descent) / 2f, pillP)
  }
}
