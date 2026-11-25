package com.example.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils
import com.example.model.CardStyle
import com.example.model.ClockFontId
import com.example.model.WeatherSnapshot
import com.example.model.WidgetConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal object ClockFloatingStyles {

  private fun fmtHour(cal: Calendar, is24Hour: Boolean): String {
    val h = if (is24Hour) cal.get(Calendar.HOUR_OF_DAY) else cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    return String.format(Locale.US, "%02d", h)
  }

  private fun fmtMin(cal: Calendar): String = String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE))

  private fun fmtSec(cal: Calendar): String = String.format(Locale.US, "%02d", cal.get(Calendar.SECOND))

  private fun fmtLowerDate(cal: Calendar): String =
    SimpleDateFormat("EEEE · MMM d", Locale.getDefault()).format(cal.time).lowercase(Locale.getDefault())

  private fun fmtUpperDate(cal: Calendar): String =
    SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(cal.time).uppercase(Locale.getDefault())

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

  private fun applyFloatingShadow(paint: Paint, config: WidgetConfig) {
    if (config.cardStyle == CardStyle.TRANSPARENT || config.depthIntensity > 0.1f) {
      val alpha = (155 * config.depthIntensity.coerceAtLeast(0.45f)).roundToInt().coerceIn(0, 220)
      paint.setShadowLayer(10f, 0f, 3f, Color.argb(alpha, 0, 0, 0))
    }
  }

  // ---------------------------------------------------------------------------
  // 1. WEIGHT STACK FLOAT (iOS / HyperOS Bold-Light Stacked Contrast, No Card)
  // ---------------------------------------------------------------------------
  fun drawWeightStackFloat(
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
    val wide = w > h * 1.45f

    val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      isFakeBoldText = true
      applyFloatingShadow(this, config)
    }
    val minPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.accentColor
      applyFloatingShadow(this, config)
    }
    val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.secondaryTextColor
      letterSpacing = 0.12f
      applyFloatingShadow(this, config)
    }

    if (wide) {
      // Wide floating editorial layout: giant HH : MM left, thin vertical divider + date/weather right
      val timeStr = "$hh:$mm"
      hourPaint.textAlign = Paint.Align.LEFT
      hourPaint.textSize = fitText(hourPaint, timeStr, w * 0.56f, h * 0.78f, h * 0.74f * config.fontScale)
      val fm = hourPaint.fontMetrics
      val timeY = rect.centerY() - (fm.ascent + fm.descent) / 2f
      canvas.drawText(hh, rect.left + 4f, timeY, hourPaint)

      val hhW = hourPaint.measureText(hh)
      minPaint.textSize = hourPaint.textSize
      canvas.drawText(":", rect.left + 4f + hhW, timeY - hourPaint.textSize * 0.05f, minPaint)
      val colonW = minPaint.measureText(":")
      hourPaint.isFakeBoldText = false
      hourPaint.alpha = 230
      canvas.drawText(mm, rect.left + 4f + hhW + colonW, timeY, hourPaint)

      val divX = rect.left + w * 0.62f
      val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.accentColor
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
      }
      canvas.drawLine(divX, rect.top + h * 0.22f, divX, rect.bottom - h * 0.22f, linePaint)

      val rx = divX + minD * 0.10f
      metaPaint.textSize = (h * 0.14f).coerceIn(10f, 18f)
      if (config.showDate) {
        canvas.drawText(fmtLowerDate(cal), rx, rect.centerY() - metaPaint.textSize * 0.4f, metaPaint)
      }
      if (config.showWeather && weather.isLoaded) {
        val iconR = (h * 0.11f).coerceIn(8f, 16f)
        val wy = rect.centerY() + metaPaint.textSize * 1.15f
        ClockAdvancedStyles.drawWeatherIcon(canvas, rx + iconR, wy, iconR, weather.weatherCode, weather.isDay, config.accentColor, config.foregroundColor)
        metaPaint.color = config.foregroundColor
        canvas.drawText("${weather.displayTemp(config.useFahrenheit)} ${weather.cityName}", rx + iconR * 2.5f, wy + metaPaint.textSize * 0.34f, metaPaint)
      }
    } else {
      // Stacked bold HH above light/accented MM floating directly on wallpaper
      hourPaint.textAlign = Paint.Align.CENTER
      minPaint.textAlign = Paint.Align.CENTER
      val numSize = fitText(hourPaint, "88", w * 0.85f, h * 0.36f, minD * 0.44f * config.fontScale)
      hourPaint.textSize = numSize
      minPaint.textSize = numSize
      val fm = hourPaint.fontMetrics
      val lineH = -fm.ascent + fm.descent
      val topOffset = if (config.showDate || config.showWeather) h * 0.06f else 0f

      canvas.drawText(hh, rect.centerX(), rect.centerY() - lineH * 0.10f - topOffset, hourPaint)
      canvas.drawText(mm, rect.centerX(), rect.centerY() + lineH * 0.68f - topOffset, minPaint)

      metaPaint.textAlign = Paint.Align.CENTER
      metaPaint.textSize = (minD * 0.088f).coerceIn(9f, 18f)
      val footer = buildString {
        if (config.showDate) append(fmtLowerDate(cal))
        if (config.showWeather && weather.isLoaded) {
          if (isNotEmpty()) append("  •  ")
          append(weather.displayTemp(config.useFahrenheit))
        }
      }
      if (footer.isNotEmpty()) {
        canvas.drawText(footer, rect.centerX(), rect.bottom - 2f, metaPaint)
      }
    }
  }

  // ---------------------------------------------------------------------------
  // 2. NOTHING DOT MATRIX FLOAT (5x7 Dot-Matrix Numerals, Zero Card)
  // ---------------------------------------------------------------------------
  private val dotGlyphs = mapOf(
    '0' to intArrayOf(0b01110, 0b10001, 0b10011, 0b10101, 0b11001, 0b10001, 0b01110),
    '1' to intArrayOf(0b00100, 0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110),
    '2' to intArrayOf(0b01110, 0b10001, 0b00001, 0b00110, 0b01000, 0b10000, 0b11111),
    '3' to intArrayOf(0b11110, 0b00001, 0b00001, 0b01110, 0b00001, 0b00001, 0b11110),
    '4' to intArrayOf(0b00010, 0b00110, 0b01010, 0b10010, 0b11111, 0b00010, 0b00010),
    '5' to intArrayOf(0b11111, 0b10000, 0b11110, 0b00001, 0b00001, 0b10001, 0b01110),
    '6' to intArrayOf(0b00110, 0b01000, 0b10000, 0b11110, 0b10001, 0b10001, 0b01110),
    '7' to intArrayOf(0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b01000, 0b01000),
    '8' to intArrayOf(0b01110, 0b10001, 0b10001, 0b01110, 0b10001, 0b10001, 0b01110),
    '9' to intArrayOf(0b01110, 0b10001, 0b10001, 0b01111, 0b00001, 0b00010, 0b01100),
    ':' to intArrayOf(0b00000, 0b00100, 0b00100, 0b00000, 0b00100, 0b00100, 0b00000),
  )

  fun drawNothingDotFloat(
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
    val useTrueDotMatrix = config.fontId == ClockFontId.SHARE_TECH_MONO || config.fontId == ClockFontId.ORBITRON

    if (useTrueDotMatrix) {
      // Draw crisp 5x7 dot-matrix glyphs directly on wallpaper!
      val totalCols = 5 * 4 + 3 + 4 * 2 // 4 digits * 5 + colon 3 + spacing
      val maxAreaW = w * 0.90f
      val maxAreaH = h * 0.54f * config.fontScale.coerceIn(0.8f, 1.2f)
      val dotStep = min(maxAreaW / totalCols, maxAreaH / 7f)
      val dotR = dotStep * 0.36f
      val totalDrawW = totalCols * dotStep
      val totalDrawH = 7f * dotStep
      var curX = rect.centerX() - totalDrawW / 2f
      val topY = rect.centerY() - totalDrawH / 2f - (if (config.showDate) h * 0.08f else 0f)

      val fgDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.foregroundColor
        applyFloatingShadow(this, config)
      }
      val accDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.accentColor
        applyFloatingShadow(this, config)
      }
      val dimDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(config.foregroundColor, 26)
      }

      for (ch in timeStr) {
        val cols = if (ch == ':') 3 else 5
        val glyph = dotGlyphs[ch] ?: continue
        val activePaint = if (ch == ':') accDot else fgDot
        for (r in 0 until 7) {
          val mask = glyph[r]
          for (c in 0 until cols) {
            val bit = if (ch == ':') (mask shr (2 - c + 1)) and 1 else (mask shr (4 - c)) and 1
            val cx = curX + (c + 0.5f) * dotStep
            val cy = topY + (r + 0.5f) * dotStep
            if (bit == 1) {
              canvas.drawCircle(cx, cy, dotR, activePaint)
            } else if (ch != ':') {
              canvas.drawCircle(cx, cy, dotR * 0.55f, dimDot)
            }
          }
        }
        curX += (cols + 2) * dotStep
      }
    } else {
      // Custom selected font with Nothing-style accent dot indicator
      val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.foregroundColor
        textAlign = Paint.Align.CENTER
        applyFloatingShadow(this, config)
      }
      tp.textSize = fitText(tp, timeStr, w * 0.88f, h * 0.56f, minD * 0.52f * config.fontScale)
      val fm = tp.fontMetrics
      canvas.drawText(timeStr, rect.centerX(), rect.centerY() - (fm.ascent + fm.descent) / 2f - h * 0.07f, tp)
    }

    // Bottom tracked-out metadata + crimson/accent indicator dot
    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.secondaryTextColor
      letterSpacing = 0.16f
      textSize = (minD * 0.09f).coerceIn(9f, 18f)
      applyFloatingShadow(this, config)
    }
    val footerStr = buildString {
      if (config.showDate) append(fmtLowerDate(cal))
      if (config.showWeather && weather.isLoaded) {
        if (isNotEmpty()) append("  /  ")
        append("${weather.displayTemp(config.useFahrenheit)} ${weather.cityName.lowercase(Locale.getDefault())}")
      }
    }.ifBlank { config.customLabel.lowercase(Locale.getDefault()) }

    val tw = footerPaint.measureText(footerStr)
    val dotR = (footerPaint.textSize * 0.28f).coerceIn(3f, 6f)
    val startX = rect.centerX() - (tw + dotR * 3.5f) / 2f
    val footerY = rect.bottom - 4f
    val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = config.accentColor }
    canvas.drawCircle(startX + dotR, footerY - footerPaint.textSize * 0.34f, dotR, pulsePaint)
    canvas.drawText(footerStr, startX + dotR * 3.5f, footerY, footerPaint)
  }

  // ---------------------------------------------------------------------------
  // 3. HAUTE DIDONE FLOAT (High-Fashion Editorial Serif, Zero Card)
  // ---------------------------------------------------------------------------
  fun drawHauteDidoneFloat(
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

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.accentColor
      letterSpacing = 0.30f
      textAlign = Paint.Align.CENTER
      textSize = (minD * 0.082f).coerceIn(9f, 16f)
      applyFloatingShadow(this, config)
    }

    if (h > w * 0.42f && config.customLabel.isNotBlank()) {
      canvas.drawText(config.customLabel.uppercase(Locale.getDefault()), rect.centerX(), rect.top + labelPaint.textSize, labelPaint)
    }

    val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      applyFloatingShadow(this, config)
    }
    val colonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.accentColor
      applyFloatingShadow(this, config)
    }

    val full = "$hh:$mm"
    val maxTimeH = if (h < w * 0.42f) h * 0.72f else h * 0.50f
    val timeSize = fitText(numPaint, full, w * 0.88f, maxTimeH, minD * 0.56f * config.fontScale)
    numPaint.textSize = timeSize
    colonPaint.textSize = timeSize * 0.76f

    val hhW = numPaint.measureText(hh)
    val colW = colonPaint.measureText(" : ")
    val mmW = numPaint.measureText(mm)
    val totalW = hhW + colW + mmW
    val startX = rect.centerX() - totalW / 2f
    val fm = numPaint.fontMetrics
    val baseline = rect.centerY() - (fm.ascent + fm.descent) / 2f

    canvas.drawText(hh, startX, baseline, numPaint)
    canvas.drawText(" : ", startX + hhW, baseline - timeSize * 0.06f, colonPaint)
    canvas.drawText(mm, startX + hhW + colW, baseline, numPaint)

    // Razor-thin hairline rule + date/weather below
    if (h > w * 0.42f) {
      val ruleY = rect.bottom - minD * 0.14f
      val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(config.accentColor, 180)
        strokeWidth = 1.6f
      }
      canvas.drawLine(rect.centerX() - w * 0.20f, ruleY, rect.centerX() + w * 0.20f, ruleY, rulePaint)

      val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.secondaryTextColor
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.14f
        textSize = (minD * 0.088f).coerceIn(9f, 17f)
        applyFloatingShadow(this, config)
      }
      val info = buildString {
        if (config.showDate) append(fmtUpperDate(cal))
        if (config.showWeather && weather.isLoaded) {
          if (isNotEmpty()) append("  —  ")
          append("${weather.displayTemp(config.useFahrenheit)} ${weather.cityName.uppercase(Locale.getDefault())}")
        }
      }
      if (info.isNotEmpty()) {
        canvas.drawText(info, rect.centerX(), rect.bottom - 2f, datePaint)
      }
    }
  }

  // ---------------------------------------------------------------------------
  // 4. HYPER PILL FLOAT (Sleek Horizontal Floating Split, Zero Card)
  // ---------------------------------------------------------------------------
  fun drawHyperPillFloat(
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

    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      applyFloatingShadow(this, config)
    }
    timePaint.textSize = fitText(timePaint, timeStr, w * 0.56f, h * 0.72f, minD * 0.52f * config.fontScale)
    val fm = timePaint.fontMetrics
    val timeY = rect.centerY() - (fm.ascent + fm.descent) / 2f
    canvas.drawText(timeStr, rect.left + 4f, timeY, timePaint)

    // Thin glowing accent bar
    val barX = rect.left + w * 0.60f
    val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = config.accentColor
      strokeWidth = (minD * 0.025f).coerceIn(2.5f, 5f)
      strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(barX, rect.top + h * 0.22f, barX, rect.bottom - h * 0.22f, barPaint)

    val infoX = barX + minD * 0.09f
    val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.secondaryTextColor
      textSize = (minD * 0.13f).coerceIn(10f, 19f)
      applyFloatingShadow(this, config)
    }

    if (config.showDate) {
      canvas.drawText(fmtUpperDate(cal), infoX, rect.centerY() - infoPaint.textSize * 0.35f, infoPaint)
    }
    if (config.showWeather && weather.isLoaded) {
      val iconR = (minD * 0.11f).coerceIn(8f, 16f)
      val wy = rect.centerY() + infoPaint.textSize * 1.15f
      ClockAdvancedStyles.drawWeatherIcon(canvas, infoX + iconR, wy, iconR, weather.weatherCode, weather.isDay, config.accentColor, config.foregroundColor)
      infoPaint.color = config.foregroundColor
      canvas.drawText("${weather.displayTemp(config.useFahrenheit)} ${weather.conditionText()}", infoX + iconR * 2.5f, wy + infoPaint.textSize * 0.34f, infoPaint)
    } else if (config.customLabel.isNotBlank()) {
      infoPaint.color = config.accentColor
      canvas.drawText(config.customLabel, infoX, rect.centerY() + infoPaint.textSize * 1.2f, infoPaint)
    }
  }

  // ---------------------------------------------------------------------------
  // 5. FLOATING ANALOG PURE (Frameless Watch Hands on Wallpaper, Zero Card)
  // ---------------------------------------------------------------------------
  fun drawFloatingAnalogPure(
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
    val hasFooter = !wide && (config.showDate || (config.showWeather && weather.isLoaded))
    val reservedBot = if (hasFooter) min(w, h) * 0.14f else 0f
    val dialR = if (wide) h * 0.45f else min(w, h - reservedBot) * 0.46f
    val cx = if (wide) rect.left + dialR + 6f else rect.centerX()
    val cy = if (wide) rect.centerY() else rect.top + (h - reservedBot) / 2f

    // Subtle floating outer ring & markers (no solid background fill!)
    val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      applyFloatingShadow(this, config)
    }
    for (i in 0 until 12) {
      val a = Math.toRadians((i * 30 - 90).toDouble())
      val isCardinal = i % 3 == 0
      val px = cx + (cos(a) * dialR * 0.88f).toFloat()
      val py = cy + (sin(a) * dialR * 0.88f).toFloat()
      markerPaint.color = if (isCardinal) config.accentColor else ColorUtils.setAlphaComponent(config.foregroundColor, 200)
      canvas.drawCircle(px, py, if (isCardinal) dialR * 0.048f else dialR * 0.022f, markerPaint)
    }

    val sec = cal.get(Calendar.SECOND)
    val min = cal.get(Calendar.MINUTE) + sec / 60f
    val hour = (cal.get(Calendar.HOUR) % 12) + min / 60f
    val hrA = Math.toRadians((hour * 30f - 90f).toDouble())
    val minA = Math.toRadians((min * 6f - 90f).toDouble())
    val secA = Math.toRadians((sec * 6f - 90f).toDouble())

    val hrP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = config.foregroundColor
      strokeWidth = (dialR * 0.078f).coerceAtLeast(3f)
      strokeCap = Paint.Cap.ROUND
      applyFloatingShadow(this, config)
    }
    canvas.drawLine(
      cx - (cos(hrA) * dialR * 0.14f).toFloat(),
      cy - (sin(hrA) * dialR * 0.14f).toFloat(),
      cx + (cos(hrA) * dialR * 0.52f).toFloat(),
      cy + (sin(hrA) * dialR * 0.52f).toFloat(),
      hrP,
    )

    val minP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = config.foregroundColor
      strokeWidth = (dialR * 0.046f).coerceAtLeast(2f)
      strokeCap = Paint.Cap.ROUND
      applyFloatingShadow(this, config)
    }
    canvas.drawLine(
      cx - (cos(minA) * dialR * 0.16f).toFloat(),
      cy - (sin(minA) * dialR * 0.16f).toFloat(),
      cx + (cos(minA) * dialR * 0.78f).toFloat(),
      cy + (sin(minA) * dialR * 0.78f).toFloat(),
      minP,
    )

    if (config.showSeconds) {
      val secP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.accentColor
        strokeWidth = (dialR * 0.022f).coerceAtLeast(1.5f)
        strokeCap = Paint.Cap.ROUND
      }
      canvas.drawLine(
        cx - (cos(secA) * dialR * 0.22f).toFloat(),
        cy - (sin(secA) * dialR * 0.22f).toFloat(),
        cx + (cos(secA) * dialR * 0.85f).toFloat(),
        cy + (sin(secA) * dialR * 0.85f).toFloat(),
        secP,
      )
    }
    canvas.drawCircle(cx, cy, dialR * 0.062f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = config.accentColor })

    if (wide) {
      val rx = cx + dialR + h * 0.12f
      val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.foregroundColor
        applyFloatingShadow(this, config)
      }
      val timeStr = "${fmtHour(cal, config.is24Hour)}:${fmtMin(cal)}"
      tp.textSize = fitText(tp, timeStr, (rect.right - rx).coerceAtLeast(40f), h * 0.42f, h * 0.38f * config.fontScale)
      canvas.drawText(timeStr, rx, rect.centerY() - h * 0.04f, tp)

      val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.accentColor
        textSize = (h * 0.14f).coerceIn(10f, 18f)
        applyFloatingShadow(this, config)
      }
      val line = buildString {
        if (config.showDate) append(fmtLowerDate(cal))
        if (config.showWeather && weather.isLoaded) {
          if (isNotEmpty()) append(" • ")
          append(weather.displayTemp(config.useFahrenheit))
        }
      }
      if (line.isNotEmpty()) canvas.drawText(line, rx, rect.centerY() + sub.textSize * 1.45f, sub)
    } else if (hasFooter) {
      val fp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.secondaryTextColor
        textAlign = Paint.Align.CENTER
        textSize = (min(w, h) * 0.088f).coerceIn(9f, 17f)
        applyFloatingShadow(this, config)
      }
      val line = buildString {
        if (config.showDate) append(fmtLowerDate(cal))
        if (config.showWeather && weather.isLoaded) {
          if (isNotEmpty()) append("  •  ")
          append(weather.displayTemp(config.useFahrenheit))
        }
      }
      canvas.drawText(line, rect.centerX(), rect.bottom - 2f, fp)
    }
  }

  // ---------------------------------------------------------------------------
  // 6. EDITORIAL SPINE FLOAT (Asymmetric Spine + Week & Weather, Zero Card)
  // ---------------------------------------------------------------------------
  fun drawVerticalSpineFloat(
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
    val spineX = rect.left + minD * 0.04f

    val spinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = config.accentColor
      strokeWidth = (minD * 0.024f).coerceIn(2.5f, 5f)
      strokeCap = Paint.Cap.ROUND
    }
    canvas.drawLine(spineX, rect.top + 6f, spineX, rect.bottom - 6f, spinePaint)

    val contentLeft = spineX + minD * 0.09f
    val availW = (rect.right - contentLeft).coerceAtLeast(40f)
    val timeStr = "${fmtHour(cal, config.is24Hour)}:${fmtMin(cal)}"

    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      applyFloatingShadow(this, config)
    }
    val tall = h >= w * 0.78f
    tp.textSize = fitText(tp, timeStr, availW * 0.94f, if (tall) h * 0.36f else h * 0.56f, minD * 0.48f * config.fontScale)
    val fm = tp.fontMetrics
    val timeY = if (tall) rect.top + tp.textSize * 1.0f else rect.centerY() - (fm.ascent + fm.descent) / 2f - h * 0.10f
    canvas.drawText(timeStr, contentLeft, timeY, tp)

    val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.secondaryTextColor
      letterSpacing = 0.10f
      textSize = (minD * 0.10f).coerceIn(10f, 18f)
      applyFloatingShadow(this, config)
    }
    val subLine = buildString {
      if (config.showDate) append(fmtLowerDate(cal))
      if (config.showWeather && weather.isLoaded) {
        if (isNotEmpty()) append("  •  ")
        append("${weather.displayTemp(config.useFahrenheit)} ${weather.cityName}")
      }
    }
    if (subLine.isNotEmpty()) {
      canvas.drawText(subLine, contentLeft, timeY + subP.textSize * 1.45f, subP)
    }

    if (tall) {
      ClockAdvancedStyles.drawWeekStrip(
        canvas = canvas,
        rect = RectF(contentLeft, rect.bottom - h * 0.28f, rect.right, rect.bottom - 4f),
        config = config,
        typeface = typeface,
        cal = cal,
      )
    }
  }
}
