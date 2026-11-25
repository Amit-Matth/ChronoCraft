package com.example.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils
import com.example.model.BackgroundFillMode
import com.example.model.CardStyle
import com.example.model.ClockCategory
import com.example.model.ClockFontId
import com.example.model.ClockStyleId
import com.example.model.DisplayMode
import com.example.model.WeatherSnapshot
import com.example.model.WidgetBackdropId
import com.example.model.WidgetConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal object ClockAdaptiveDisplayRenderer {

  fun resolveConfigForMode(config: WidgetConfig, mode: DisplayMode): WidgetConfig {
    return when (mode) {
      DisplayMode.HOME_SCREEN -> config
      DisplayMode.LOCK_SCREEN -> {
        val lockFill = config.effectiveFillMode(DisplayMode.LOCK_SCREEN)
        config.copy(
          cardStyle = config.lockCardStyle,
          backgroundFillMode = lockFill,
          backgroundAlpha = if (lockFill == BackgroundFillMode.NONE) 0f else config.lockBackgroundAlpha,
          backdropId = if (lockFill == BackgroundFillMode.ARTWORK) config.lockBackdropId else WidgetBackdropId.NONE,
          customPhotoUri = if (lockFill == BackgroundFillMode.PHOTO) config.lockCustomPhotoUri ?: config.customPhotoUri else config.customPhotoUri,
          showBorder = lockFill != BackgroundFillMode.NONE && config.showBorder,
          depthIntensity = (config.depthIntensity * 0.7f).coerceAtLeast(0.35f),
          glowIntensity = (config.glowIntensity * 0.75f),
        )
      }
      DisplayMode.AOD -> {
        val dim = config.aodLowPowerDim.coerceIn(0.45f, 0.95f)
        val dimFg = ColorUtils.blendARGB(config.hourColor ?: config.foregroundColor, Color.BLACK, 1f - dim)
        val dimMin = config.minuteColor?.let { ColorUtils.blendARGB(it, Color.BLACK, 1f - dim) }
        val dimAcc = ColorUtils.blendARGB(config.accentColor, Color.BLACK, (1f - dim) * 0.72f)
        val dimSec = ColorUtils.blendARGB(config.secondaryTextColor, Color.BLACK, 1f - dim * 0.92f)
        config.copy(
          cardStyle = config.aodCardStyle,
          backgroundFillMode = if (config.aodCardStyle == CardStyle.TRANSPARENT) BackgroundFillMode.NONE else BackgroundFillMode.SOLID,
          backgroundAlpha = if (config.aodCardStyle == CardStyle.TRANSPARENT) 0f else config.aodBackgroundAlpha.coerceAtMost(0.28f),
          backdropId = WidgetBackdropId.NONE,
          foregroundColor = dimFg,
          hourColor = dimFg,
          minuteColor = dimMin,
          accentColor = dimAcc,
          secondaryTextColor = dimSec,
          showSeconds = false,
          showBorder = config.aodCardStyle != CardStyle.TRANSPARENT,
          showShadow = false,
          depthIntensity = 0f,
          glowIntensity = 0f,
        )
      }
    }
  }

  private fun fmtHour(cal: Calendar, is24Hour: Boolean): String {
    val h = if (is24Hour) cal.get(Calendar.HOUR_OF_DAY) else cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    return String.format(Locale.US, "%02d", h)
  }

  private fun fmtMin(cal: Calendar): String = String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE))

  private fun fmtDate(cal: Calendar, config: WidgetConfig): String =
    SimpleDateFormat(config.dateFormat.pattern, Locale.getDefault()).format(cal.time)

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

  /**
   * Draws subtle ambient glow/parallax lighting on Home Screen & Lock Screen when animations are enabled.
   * Never runs on AOD to preserve battery efficiency.
   */
  fun drawAmbientMotionLayer(
    canvas: Canvas,
    width: Float,
    height: Float,
    config: WidgetConfig,
    mode: DisplayMode,
    animPhase: Float,
  ) {
    if (mode == DisplayMode.AOD || !config.animationsEnabled) return
    val minD = min(width, height)
    val pulse = 0.5f + 0.5f * sin(animPhase * Math.PI * 2.0).toFloat()
    val strength = if (mode == DisplayMode.HOME_SCREEN) 0.34f else 0.22f
    val alpha = (52 * strength * (0.65f + 0.35f * pulse) * config.depthIntensity.coerceAtLeast(0.4f)).roundToInt().coerceIn(0, 65)
    if (alpha <= 2) return

    val cx = width * (0.28f + 0.44f * pulse)
    val cy = height * (0.30f + 0.20f * cos(animPhase * Math.PI * 2.0).toFloat())
    val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = RadialGradient(
        cx,
        cy,
        minD * 0.78f,
        ColorUtils.setAlphaComponent(config.accentColor, alpha),
        Color.TRANSPARENT,
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawCircle(cx, cy, minD * 0.78f, glow)
  }

  // ===========================================================================
  // ALWAYS-ON DISPLAY (AOD) ADAPTIVE RENDERER
  // Low-power horizontal/skeleton identity, zero heavy fills/shadows/motion.
  // ===========================================================================
  fun drawAodVariant(
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
    val minD = min(w, h)
    val style = config.styleId

    // 1. Analog Family -> Ultra-clean skeleton hands & cardinal dots (zero face fill, zero seconds)
    if (style.category == ClockCategory.ANALOG || style == ClockStyleId.FLOATING_ANALOG_PURE) {
      drawAodSkeletonAnalog(canvas, rect, config, typeface, cal, weather)
      return
    }

    // 2. Dot Matrix -> Low-power LED dot-matrix horizontal digits
    if (style == ClockStyleId.NOTHING_DOT_FLOAT) {
      ClockFloatingStyles.drawNothingDotFloat(canvas, rect, config, typeface, cal, weather)
      return
    }

    // 3. Word Family -> Single clean highlighted phrase on AOD
    if (style == ClockStyleId.WORD_STATEMENT || style == ClockStyleId.WORD_MATRIX) {
      val hr = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
      val mn = cal.get(Calendar.MINUTE)
      val phrase = "${fmtHour(cal, config.is24Hour)}:${fmtMin(cal)}"
      val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.foregroundColor
        textAlign = Paint.Align.CENTER
      }
      tp.textSize = fitText(tp, phrase, w * 0.75f, h * 0.45f, minD * 0.48f * config.fontScale)
      val fm = tp.fontMetrics
      canvas.drawText(phrase, rect.centerX(), rect.centerY() - (fm.ascent + fm.descent) / 2f - h * 0.06f, tp)

      val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.accentColor
        textAlign = Paint.Align.CENTER
        textSize = (minD * 0.11f).coerceIn(10f, 18f)
      }
      canvas.drawText("HOUR $hr  •  ${if (mn == 0) "ON THE HOUR" else "$mn MIN"}", rect.centerX(), rect.bottom - 6f, sub)
      return
    }

    // 4. All Digital / Art / Dashboard Styles -> Cohesive Horizontal AOD Identity
    val hh = fmtHour(cal, config.is24Hour)
    val mm = fmtMin(cal)
    val isWeightContrast = style == ClockStyleId.WEIGHT_STACK_FLOAT || style == ClockStyleId.VERTICAL_SPINE_FLOAT
    val isWideSpread = style == ClockStyleId.HYPER_PILL_FLOAT || style == ClockStyleId.TERMINAL_MONO
    val timeStr = if (isWideSpread) "$hh : $mm" else "$hh:$mm"

    // Optional ultra-minimal frame outline for Neon Frame on AOD
    if (style == ClockStyleId.NEON_FRAME_GLOW) {
      val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.style = Paint.Style.STROKE
        strokeWidth = 2f
        color = ColorUtils.setAlphaComponent(config.accentColor, 145)
      }
      val r = (minD * 0.16f).coerceIn(12f, 28f)
      canvas.drawRoundRect(RectF(rect.left + 4f, rect.top + 4f, rect.right - 4f, rect.bottom - 4f), r, r, strokeP)
    }

    val hhPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      isFakeBoldText = isWeightContrast || style == ClockStyleId.BRUTALIST_POSTER
    }
    val mmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = if (isWeightContrast) config.accentColor else config.foregroundColor
      isFakeBoldText = false
    }
    val colonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.accentColor
    }

    val maxTimeH = h * 0.54f
    val targetSize = fitText(hhPaint, timeStr, w * 0.88f, maxTimeH, minD * 0.52f * config.fontScale)
    hhPaint.textSize = targetSize
    mmPaint.textSize = targetSize
    colonPaint.textSize = targetSize

    val sep = if (isWideSpread) " : " else ":"
    val totalW = hhPaint.measureText(hh) + colonPaint.measureText(sep) + mmPaint.measureText(mm)
    val startX = rect.centerX() - totalW / 2f
    val fm = hhPaint.fontMetrics
    val baseline = rect.centerY() - (fm.ascent + fm.descent) / 2f - h * 0.08f

    if (style == ClockStyleId.HANDWRITTEN_SCRIPT) {
      canvas.save()
      canvas.rotate(-3.5f, rect.centerX(), baseline)
    }

    canvas.drawText(hh, startX, baseline, hhPaint)
    val xCol = startX + hhPaint.measureText(hh)
    canvas.drawText(sep, xCol, baseline - targetSize * 0.04f, colonPaint)
    canvas.drawText(mm, xCol + colonPaint.measureText(sep), baseline, mmPaint)

    if (style == ClockStyleId.HANDWRITTEN_SCRIPT) {
      canvas.restore()
    }

    // Minimal style signature motif beneath time on AOD
    if (style == ClockStyleId.SUNSET_SCENIC_ART || style == ClockStyleId.HAUTE_DIDONE_FLOAT || style == ClockStyleId.EDITORIAL_SERIF || style == ClockStyleId.NATURE_BOTANICAL || style == ClockStyleId.MOOD_DAYLIGHT) {
      val lineY = baseline + minD * 0.10f
      val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(config.accentColor, 150)
        strokeWidth = 1.5f
      }
      val d = 4f
      canvas.drawLine(rect.centerX() - w * 0.18f, lineY, rect.centerX() - d * 2f, lineY, lineP)
      canvas.drawLine(rect.centerX() + d * 2f, lineY, rect.centerX() + w * 0.18f, lineY, lineP)
      val diamond = Path().apply {
        moveTo(rect.centerX(), lineY - d)
        lineTo(rect.centerX() + d, lineY)
        lineTo(rect.centerX(), lineY + d)
        lineTo(rect.centerX() - d, lineY)
        close()
      }
      canvas.drawPath(diamond, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = config.accentColor })
    } else if (style == ClockStyleId.SOLAR_ORBIT_PLANETS || style == ClockStyleId.STARFIELD_CONSTELLATION) {
      // Simplified single faint orbit ring around time on AOD
      val ringP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.style = Paint.Style.STROKE
        strokeWidth = 1.2f
        color = ColorUtils.setAlphaComponent(config.accentColor, 95)
      }
      canvas.drawOval(RectF(rect.centerX() - w * 0.36f, rect.top + 6f, rect.centerX() + w * 0.36f, rect.bottom - 14f), ringP)
    } else if (style == ClockStyleId.CIRCULAR_BATTERY_RING || style == ClockStyleId.COUNTDOWN_PROGRESS) {
      // Minimal low-power horizontal progress/battery bar
      val barW = w * 0.42f
      val barY = baseline + minD * 0.12f
      val trackP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(config.secondaryTextColor, 65)
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
      }
      val activeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.accentColor
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
      }
      val frac = if (style == ClockStyleId.COUNTDOWN_PROGRESS) {
        ((cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)) / 1440f).coerceIn(0.05f, 1f)
      } else {
        batteryPercent / 100f
      }
      canvas.drawLine(rect.centerX() - barW / 2f, barY, rect.centerX() + barW / 2f, barY, trackP)
      canvas.drawLine(rect.centerX() - barW / 2f, barY, rect.centerX() - barW / 2f + barW * frac, barY, activeP)
    }

    // Compact Date + Weather / Battery footer on AOD
    val metaP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.secondaryTextColor
      textAlign = Paint.Align.CENTER
      letterSpacing = 0.08f
      textSize = (minD * 0.092f).coerceIn(9f, 16f)
    }
    val footer = buildString {
      if (config.showDate) append(fmtDate(cal, config))
      if (config.showWeather && weather.isLoaded) {
        if (isNotEmpty()) append("  •  ")
        append(weather.displayTemp(config.useFahrenheit))
      }
      if (style == ClockStyleId.CIRCULAR_BATTERY_RING) {
        if (isNotEmpty()) append("  •  ")
        append("⚡$batteryPercent%")
      }
    }
    if (footer.isNotEmpty()) {
      canvas.drawText(footer, rect.centerX(), rect.bottom - 4f, metaP)
    }
  }

  private fun drawAodSkeletonAnalog(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
    weather: WeatherSnapshot,
  ) {
    val w = rect.width()
    val h = rect.height()
    val wide = w > h * 1.45f
    val dialR = if (wide) h * 0.42f else min(w, h) * 0.40f
    val cx = if (wide) rect.left + dialR + 8f else rect.centerX()
    val cy = if (wide) rect.centerY() else rect.centerY() - h * 0.06f

    // Hairline cardinal markers only (minimal OLED pixels)
    val tickP = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    for (i in 0 until 12) {
      val a = Math.toRadians((i * 30 - 90).toDouble())
      val isCardinal = i % 3 == 0
      tickP.color = if (isCardinal) config.accentColor else ColorUtils.setAlphaComponent(config.secondaryTextColor, 140)
      tickP.strokeWidth = if (isCardinal) 3f else 1.5f
      val r0 = if (isCardinal) dialR * 0.80f else dialR * 0.88f
      val r1 = dialR * 0.94f
      canvas.drawLine(
        cx + (cos(a) * r0).toFloat(),
        cy + (sin(a) * r0).toFloat(),
        cx + (cos(a) * r1).toFloat(),
        cy + (sin(a) * r1).toFloat(),
        tickP,
      )
    }

    val min = cal.get(Calendar.MINUTE).toFloat()
    val hour = (cal.get(Calendar.HOUR) % 12) + min / 60f
    val hrA = Math.toRadians((hour * 30f - 90f).toDouble())
    val minA = Math.toRadians((min * 6f - 90f).toDouble())

    val handP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = config.foregroundColor
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
      strokeWidth = (dialR * 0.06f).coerceAtLeast(2.5f)
    }
    canvas.drawLine(cx, cy, cx + (cos(hrA) * dialR * 0.50f).toFloat(), cy + (sin(hrA) * dialR * 0.50f).toFloat(), handP)
    handP.strokeWidth = (dialR * 0.038f).coerceAtLeast(1.8f)
    canvas.drawLine(cx, cy, cx + (cos(minA) * dialR * 0.76f).toFloat(), cy + (sin(minA) * dialR * 0.76f).toFloat(), handP)
    canvas.drawCircle(cx, cy, dialR * 0.055f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = config.accentColor })

    val meta = buildString {
      if (config.showDate) append(fmtDate(cal, config))
      if (config.showWeather && weather.isLoaded) {
        if (isNotEmpty()) append(" • ")
        append(weather.displayTemp(config.useFahrenheit))
      }
    }
    if (wide) {
      val rx = cx + dialR + h * 0.12f
      val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.foregroundColor
      }
      val tStr = "${fmtHour(cal, config.is24Hour)}:${fmtMin(cal)}"
      tp.textSize = fitText(tp, tStr, (rect.right - rx).coerceAtLeast(40f), h * 0.42f, h * 0.36f * config.fontScale)
      canvas.drawText(tStr, rx, rect.centerY(), tp)
      if (meta.isNotEmpty()) {
        val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          this.typeface = typeface
          color = config.secondaryTextColor
          textSize = (h * 0.14f).coerceIn(10f, 16f)
        }
        canvas.drawText(meta, rx, rect.centerY() + sp.textSize * 1.6f, sp)
      }
    } else if (meta.isNotEmpty()) {
      val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.secondaryTextColor
        textAlign = Paint.Align.CENTER
        textSize = (min(w, h) * 0.088f).coerceIn(9f, 15f)
      }
      canvas.drawText(meta, rect.centerX(), rect.bottom - 2f, sp)
    }
  }

  // ===========================================================================
  // LOCK SCREEN ADAPTIVE RENDERER
  // Horizontal hero adaptation for stacked/vertical Home styles, preserving identity.
  // Returns true if Lock Screen provided a dedicated transformed layout, false to
  // fall through to standard style renderer (already horizontal/balanced).
  // ===========================================================================
  fun drawLockScreenTransformedIfApplicable(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
    weather: WeatherSnapshot,
  ): Boolean {
    return when (config.styleId) {
      ClockStyleId.WEIGHT_STACK_FLOAT,
      ClockStyleId.VERTICAL_SPINE_FLOAT,
      ClockStyleId.BRUTALIST_POSTER,
      ClockStyleId.BENTO_CALENDAR,
      ClockStyleId.EXPANDABLE_STACK,
      ClockStyleId.WORLD_AGENDA_PRO,
      ClockStyleId.EXPERIMENTAL_KINETIC -> {
        drawLockHorizontalHero(canvas, rect, config, typeface, cal, weather)
        true
      }
      else -> false
    }
  }

  private fun drawLockHorizontalHero(
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
    val hh = fmtHour(cal, config.is24Hour)
    val mm = fmtMin(cal)

    // Preserve each clock's signature element in horizontal Lock Screen form:
    val hhPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.hourColor ?: config.foregroundColor
      isFakeBoldText = true
      setShadowLayer(10f, 0f, 3f, Color.argb(155, 0, 0, 0))
    }
    val mmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.minuteColor ?: if (config.styleId == ClockStyleId.WEIGHT_STACK_FLOAT || config.styleId == ClockStyleId.EXPERIMENTAL_KINETIC) config.accentColor else config.foregroundColor
      setShadowLayer(10f, 0f, 3f, Color.argb(155, 0, 0, 0))
    }
    val colPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.accentColor
      setShadowLayer(10f, 0f, 3f, Color.argb(155, 0, 0, 0))
    }

    val timeStr = "$hh:$mm"
    val timeSize = fitText(hhPaint, timeStr, w * 0.56f, h * 0.72f, minD * 0.54f * config.fontScale)
    hhPaint.textSize = timeSize
    mmPaint.textSize = timeSize
    colPaint.textSize = timeSize

    val fm = hhPaint.fontMetrics
    val timeY = rect.centerY() - (fm.ascent + fm.descent) / 2f
    val leftEdge = rect.left + 8f
    canvas.drawText(hh, leftEdge, timeY, hhPaint)
    val hhW = hhPaint.measureText(hh)
    canvas.drawText(":", leftEdge + hhW, timeY - timeSize * 0.04f, colPaint)
    canvas.drawText(mm, leftEdge + hhW + colPaint.measureText(":"), timeY, mmPaint)

    // Signature vertical accent spine separating time from lock-screen info
    val divX = rect.left + w * 0.61f
    val spinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = config.accentColor
      strokeWidth = (minD * 0.024f).coerceIn(2.5f, 4.5f)
      strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(divX, rect.top + h * 0.18f, divX, rect.bottom - h * 0.18f, spinePaint)

    val rx = divX + minD * 0.08f
    val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.secondaryTextColor
      textSize = (minD * 0.115f).coerceIn(10f, 18f)
      setShadowLayer(8f, 0f, 2f, Color.argb(145, 0, 0, 0))
    }
    if (config.showDate) {
      canvas.drawText(fmtDate(cal, config), rx, rect.centerY() - subP.textSize * 0.45f, subP)
    }
    if (config.showWeather && weather.isLoaded) {
      val iconR = (minD * 0.10f).coerceIn(8f, 15f)
      val wy = rect.centerY() + subP.textSize * 1.15f
      ClockAdvancedStyles.drawWeatherIcon(canvas, rx + iconR, wy, iconR, weather.weatherCode, weather.isDay, config.accentColor, config.foregroundColor)
      subP.color = config.foregroundColor
      canvas.drawText(weather.displayTemp(config.useFahrenheit), rx + iconR * 2.5f, wy + subP.textSize * 0.34f, subP)
    } else if (config.customLabel.isNotBlank()) {
      subP.color = config.accentColor
      canvas.drawText(config.customLabel, rx, rect.centerY() + subP.textSize * 1.15f, subP)
    }
  }
}
