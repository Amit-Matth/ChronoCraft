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
import com.example.model.CardStyle
import com.example.model.WeatherSnapshot
import com.example.model.WidgetConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal object ClockAdvancedStyles {

  private fun fmtHour(cal: Calendar, is24Hour: Boolean): String {
    val h = if (is24Hour) cal.get(Calendar.HOUR_OF_DAY) else cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    return String.format(Locale.US, "%02d", h)
  }

  private fun fmtMin(cal: Calendar): String = String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE))

  private fun fmtSec(cal: Calendar): String = String.format(Locale.US, "%02d", cal.get(Calendar.SECOND))

  private fun fmtShortDate(cal: Calendar): String =
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

  fun drawWeatherIcon(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    r: Float,
    weatherCode: Int,
    isDay: Boolean,
    accentColor: Int,
    fgColor: Int,
  ) {
    when {
      weatherCode == 0 && isDay -> {
        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FBBF24") }
        canvas.drawCircle(cx, cy, r * 0.45f, sunPaint)
        val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.parseColor("#FBBF24")
          strokeWidth = (r * 0.12f).coerceAtLeast(1.8f)
          strokeCap = Paint.Cap.ROUND
        }
        for (i in 0 until 8) {
          val a = Math.toRadians(i * 45.0)
          canvas.drawLine(
            cx + (cos(a) * r * 0.62f).toFloat(),
            cy + (sin(a) * r * 0.62f).toFloat(),
            cx + (cos(a) * r * 0.88f).toFloat(),
            cy + (sin(a) * r * 0.88f).toFloat(),
            rayPaint,
          )
        }
      }
      weatherCode == 0 && !isDay -> {
        val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
        canvas.drawCircle(cx, cy, r * 0.56f, moonPaint)
        val cutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = ColorUtils.blendARGB(fgColor, Color.BLACK, 0.85f)
        }
        canvas.drawCircle(cx + r * 0.24f, cy - r * 0.16f, r * 0.44f, cutPaint)
      }
      weatherCode in 1..2 -> {
        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FBBF24") }
        canvas.drawCircle(cx - r * 0.22f, cy - r * 0.22f, r * 0.34f, sunPaint)
        drawCloudShape(canvas, cx + r * 0.08f, cy + r * 0.16f, r * 0.62f, fgColor)
      }
      weatherCode in listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82) -> {
        drawCloudShape(canvas, cx, cy - r * 0.12f, r * 0.64f, fgColor)
        val dropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = accentColor
          strokeWidth = (r * 0.12f).coerceAtLeast(1.8f)
          strokeCap = Paint.Cap.ROUND
        }
        for (dx in listOf(-0.28f, 0f, 0.28f)) {
          val x = cx + r * dx
          canvas.drawLine(x, cy + r * 0.34f, x - r * 0.1f, cy + r * 0.64f, dropPaint)
        }
      }
      weatherCode in listOf(71, 73, 75, 77, 85, 86) -> {
        drawCloudShape(canvas, cx, cy - r * 0.12f, r * 0.64f, fgColor)
        val snowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
        for (dx in listOf(-0.26f, 0f, 0.26f)) {
          canvas.drawCircle(cx + r * dx, cy + r * 0.48f, r * 0.09f, snowPaint)
        }
      }
      weatherCode in listOf(95, 96, 99) -> {
        drawCloudShape(canvas, cx, cy - r * 0.14f, r * 0.64f, fgColor)
        val bolt = Path().apply {
          moveTo(cx + r * 0.05f, cy + r * 0.16f)
          lineTo(cx - r * 0.14f, cy + r * 0.44f)
          lineTo(cx + r * 0.04f, cy + r * 0.44f)
          lineTo(cx - r * 0.08f, cy + r * 0.74f)
        }
        val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          color = Color.parseColor("#FBBF24")
          strokeWidth = (r * 0.14f).coerceAtLeast(2f)
          strokeCap = Paint.Cap.ROUND
          strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(bolt, boltPaint)
      }
      else -> {
        drawCloudShape(canvas, cx, cy + r * 0.06f, r * 0.68f, fgColor)
      }
    }
  }

  private fun drawCloudShape(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    canvas.drawCircle(cx - r * 0.36f, cy + r * 0.08f, r * 0.34f, paint)
    canvas.drawCircle(cx + r * 0.34f, cy + r * 0.10f, r * 0.30f, paint)
    canvas.drawCircle(cx - r * 0.02f, cy - r * 0.14f, r * 0.42f, paint)
    canvas.drawRoundRect(
      RectF(cx - r * 0.68f, cy, cx + r * 0.64f, cy + r * 0.38f),
      r * 0.2f,
      r * 0.2f,
      paint,
    )
  }

  fun drawMonthCalendarGrid(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
    showMonthHeader: Boolean = true,
  ) {
    val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.accentColor
      textSize = (rect.height() * 0.095f).coerceIn(9f, 20f)
      letterSpacing = 0.08f
    }

    var gridTop = rect.top
    if (showMonthHeader) {
      canvas.drawText(
        monthFmt.format(cal.time).uppercase(Locale.getDefault()),
        rect.left,
        rect.top + headerPaint.textSize,
        headerPaint,
      )
      gridTop += headerPaint.textSize * 1.45f
    }

    val daysHeader = arrayOf("M", "T", "W", "T", "F", "S", "S")
    val cols = 7
    val rows = 7 // 1 weekday header + up to 6 week rows
    val cellW = rect.width() / cols
    val cellH = ((rect.bottom - gridTop) / rows).coerceAtLeast(10f)
    val dayFontSize = (min(cellW, cellH) * 0.50f).coerceIn(8f, 18f)

    val dayHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.secondaryTextColor
      textAlign = Paint.Align.CENTER
      textSize = dayFontSize * 0.88f
    }
    val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      textAlign = Paint.Align.CENTER
      textSize = dayFontSize
    }
    val todayBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = config.accentColor }
    val todayTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.backgroundColor
      textAlign = Paint.Align.CENTER
      textSize = dayFontSize
      isFakeBoldText = true
    }

    val fm = dayPaint.fontMetrics
    for (c in 0 until cols) {
      val x = rect.left + cellW * (c + 0.5f)
      val y = gridTop + cellH * 0.5f - (fm.ascent + fm.descent) / 2f
      canvas.drawText(daysHeader[c], x, y, dayHeaderPaint)
    }

    val monthCal = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val firstDow = monthCal.get(Calendar.DAY_OF_WEEK) // Sun=1, Mon=2 .. Sat=7
    val startOffset = (firstDow + 5) % 7 // Mon=0 .. Sun=6
    val maxDay = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val today = cal.get(Calendar.DAY_OF_MONTH)

    for (d in 1..maxDay) {
      val idx = startOffset + (d - 1)
      val row = 1 + idx / 7
      val col = idx % 7
      if (row >= rows) break
      val cx = rect.left + cellW * (col + 0.5f)
      val cy = gridTop + cellH * (row + 0.5f)
      if (d == today) {
        val pillR = (min(cellW, cellH) * 0.42f).coerceAtLeast(6f)
        canvas.drawCircle(cx, cy, pillR, todayBgPaint)
        canvas.drawText(d.toString(), cx, cy - (fm.ascent + fm.descent) / 2f, todayTextPaint)
      } else {
        dayPaint.alpha = if (col >= 5) 165 else 240
        canvas.drawText(d.toString(), cx, cy - (fm.ascent + fm.descent) / 2f, dayPaint)
      }
    }
  }

  fun drawWeekStrip(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
  ) {
    val dowLabels = arrayOf("M", "T", "W", "T", "F", "S", "S")
    val weekStart = (cal.clone() as Calendar).apply {
      val offset = (get(Calendar.DAY_OF_WEEK) + 5) % 7
      add(Calendar.DAY_OF_MONTH, -offset)
    }
    val today = cal.get(Calendar.DAY_OF_MONTH)
    val todayMonth = cal.get(Calendar.MONTH)
    val cellW = rect.width() / 7f
    val labelSize = (min(cellW, rect.height()) * 0.28f).coerceIn(8f, 15f)
    val numSize = (min(cellW, rect.height()) * 0.36f).coerceIn(9f, 19f)

    val lblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.secondaryTextColor
      textAlign = Paint.Align.CENTER
      textSize = labelSize
    }
    val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      textAlign = Paint.Align.CENTER
      textSize = numSize
    }
    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = config.accentColor }

    for (i in 0 until 7) {
      val cx = rect.left + cellW * (i + 0.5f)
      val dayNum = weekStart.get(Calendar.DAY_OF_MONTH)
      val isToday = dayNum == today && weekStart.get(Calendar.MONTH) == todayMonth
      if (isToday) {
        val boxR = RectF(cx - cellW * 0.36f, rect.top + 2f, cx + cellW * 0.36f, rect.bottom - 2f)
        canvas.drawRoundRect(boxR, cellW * 0.25f, cellW * 0.25f, pillPaint)
        lblPaint.color = config.backgroundColor
        numPaint.color = config.backgroundColor
      } else {
        lblPaint.color = config.secondaryTextColor
        numPaint.color = config.foregroundColor
      }
      canvas.drawText(dowLabels[i], cx, rect.top + rect.height() * 0.36f, lblPaint)
      canvas.drawText(dayNum.toString(), cx, rect.top + rect.height() * 0.78f, numPaint)
      weekStart.add(Calendar.DAY_OF_MONTH, 1)
    }
  }

  private fun drawDepthTile(
    canvas: Canvas,
    rect: RectF,
    radius: Float,
    baseColor: Int,
    accentColor: Int,
    depth: Float,
    bgAlpha: Float = 0.94f,
  ) {
    if (bgAlpha <= 0.02f) return
    val alphaInt = (bgAlpha * 255).roundToInt().coerceIn(0, 255)
    if (depth > 0.05f && bgAlpha > 0.35f) {
      val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((75 * depth * bgAlpha).roundToInt(), 0, 0, 0)
      }
      canvas.drawRoundRect(
        RectF(rect.left + 3f * depth, rect.top + 5f * depth, rect.right + 3f * depth, rect.bottom + 6f * depth),
        radius,
        radius,
        shadowPaint,
      )
    }
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      val topC = ColorUtils.setAlphaComponent(ColorUtils.blendARGB(baseColor, Color.WHITE, 0.07f * depth), alphaInt)
      val botC = ColorUtils.setAlphaComponent(ColorUtils.blendARGB(baseColor, accentColor, 0.05f), alphaInt)
      shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom, topC, botC, Shader.TileMode.CLAMP)
    }
    canvas.drawRoundRect(rect, radius, radius, tilePaint)
    val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 1.6f
      color = Color.argb((55 * depth).roundToInt(), 255, 255, 255)
    }
    canvas.drawRoundRect(rect, radius, radius, rimPaint)
  }

  // ---------------------------------------------------------------------------
  // 15. BENTO CAL & WEATHER (Expandable Multi-Tile Depth Dashboard)
  // ---------------------------------------------------------------------------
  fun drawBentoCalendar(
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
    val gap = (minD * 0.045f).coerceIn(5f, 14f)
    val r = (minD * 0.11f).coerceIn(10f, 24f)
    val timeStr = "${fmtHour(cal, config.is24Hour)}:${fmtMin(cal)}"
    val tempStr = weather.displayTemp(config.useFahrenheit)

    val isStrip = h < w * 0.42f
    val isExpandedTall = h >= w * 0.78f
    val isCompactSquare = w < h * 1.35f && !isExpandedTall

    val effectiveAlpha = if (config.cardStyle == CardStyle.TRANSPARENT) 0f else config.backgroundAlpha

    if (isStrip) {
      // 4x1 strip: Left Time tile | Middle Date tile | Right Weather tile
      val tW = w * 0.42f
      val dW = w * 0.30f
      val timeTile = RectF(rect.left, rect.top, rect.left + tW, rect.bottom)
      val dateTile = RectF(timeTile.right + gap, rect.top, timeTile.right + gap + dW, rect.bottom)
      val wTile = RectF(dateTile.right + gap, rect.top, rect.right, rect.bottom)

      drawDepthTile(canvas, timeTile, r, config.backgroundColor, config.accentColor, config.depthIntensity, effectiveAlpha)
      drawDepthTile(canvas, dateTile, r, config.backgroundColor, config.accentColor, config.depthIntensity, effectiveAlpha)
      drawDepthTile(canvas, wTile, r, config.backgroundColor, config.accentColor, config.depthIntensity, effectiveAlpha)

      val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.foregroundColor
        textAlign = Paint.Align.CENTER
      }
      tp.textSize = fitText(tp, timeStr, tW * 0.82f, h * 0.70f, h * 0.56f * config.fontScale)
      val fm = tp.fontMetrics
      canvas.drawText(timeStr, timeTile.centerX(), timeTile.centerY() - (fm.ascent + fm.descent) / 2f, tp)

      val dp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.accentColor
        textAlign = Paint.Align.CENTER
        textSize = (h * 0.24f).coerceIn(10f, 20f)
      }
      canvas.drawText(fmtShortDate(cal), dateTile.centerX(), dateTile.centerY() + dp.textSize * 0.35f, dp)

      drawWeatherIcon(canvas, wTile.left + wTile.width() * 0.30f, wTile.centerY(), h * 0.24f, weather.weatherCode, weather.isDay, config.accentColor, config.foregroundColor)
      dp.color = config.foregroundColor
      canvas.drawText(tempStr, wTile.left + wTile.width() * 0.68f, wTile.centerY() + dp.textSize * 0.35f, dp)
      return
    }

    if (isExpandedTall) {
      // 4x3 or 4x4 Expanded Hero: Top Bento Row (Time + Weather) & Bottom Full Month Calendar!
      val topH = h * 0.38f
      val topLeft = RectF(rect.left, rect.top, rect.left + w * 0.56f - gap / 2f, rect.top + topH)
      val topRight = RectF(topLeft.right + gap, rect.top, rect.right, rect.top + topH)
      val calTile = RectF(rect.left, topLeft.bottom + gap, rect.right, rect.bottom)

      drawDepthTile(canvas, topLeft, r, config.backgroundColor, config.accentColor, config.depthIntensity, effectiveAlpha)
      drawDepthTile(canvas, topRight, r, config.backgroundColor, config.accentColor, config.depthIntensity, effectiveAlpha)
      drawDepthTile(canvas, calTile, r, config.backgroundColor, config.accentColor, config.depthIntensity, effectiveAlpha)

      // Time Tile
      val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.foregroundColor
      }
      tp.textSize = fitText(tp, timeStr, topLeft.width() * 0.84f, topH * 0.56f, topH * 0.46f * config.fontScale)
      val tfm = tp.fontMetrics
      canvas.drawText(timeStr, topLeft.left + minD * 0.05f, topLeft.centerY() - (tfm.ascent + tfm.descent) / 2f - topH * 0.08f, tp)

      val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.accentColor
        textSize = (topH * 0.14f).coerceIn(10f, 18f)
      }
      canvas.drawText(fmtShortDate(cal), topLeft.left + minD * 0.05f, topLeft.bottom - minD * 0.05f, subP)

      // Weather Tile
      val iconR = (min(topRight.width(), topH) * 0.22f).coerceIn(10f, 26f)
      drawWeatherIcon(canvas, topRight.left + iconR + 14f, topRight.top + iconR + 14f, iconR, weather.weatherCode, weather.isDay, config.accentColor, config.foregroundColor)
      val tempP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.foregroundColor
        textAlign = Paint.Align.RIGHT
        textSize = (topH * 0.30f).coerceIn(14f, 34f)
      }
      canvas.drawText(tempStr, topRight.right - 14f, topRight.top + iconR + 18f, tempP)
      val condP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.secondaryTextColor
        textSize = (topH * 0.125f).coerceIn(9f, 16f)
      }
      canvas.drawText(weather.cityName, topRight.left + 14f, topRight.bottom - condP.textSize * 1.85f, condP)
      condP.color = config.accentColor
      canvas.drawText("${weather.conditionText()} • ${weather.displayHighLow(config.useFahrenheit)}", topRight.left + 14f, topRight.bottom - 12f, condP)

      // Full Month Calendar Tile
      val calPad = (minD * 0.05f).coerceAtLeast(10f)
      drawMonthCalendarGrid(
        canvas = canvas,
        rect = RectF(calTile.left + calPad, calTile.top + calPad, calTile.right - calPad, calTile.bottom - calPad),
        config = config,
        typeface = typeface,
        cal = cal,
      )
    } else if (isCompactSquare) {
      // 2x2 Square: Top Time tile + Bottom Weather & Date split
      val topTile = RectF(rect.left, rect.top, rect.right, rect.top + h * 0.56f)
      val botLeft = RectF(rect.left, topTile.bottom + gap, rect.left + w * 0.52f - gap / 2f, rect.bottom)
      val botRight = RectF(botLeft.right + gap, topTile.bottom + gap, rect.right, rect.bottom)
      drawDepthTile(canvas, topTile, r, config.backgroundColor, config.accentColor, config.depthIntensity, effectiveAlpha)
      drawDepthTile(canvas, botLeft, r, config.backgroundColor, config.accentColor, config.depthIntensity, effectiveAlpha)
      drawDepthTile(canvas, botRight, r, config.backgroundColor, config.accentColor, config.depthIntensity, effectiveAlpha)

      val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.foregroundColor
        textAlign = Paint.Align.CENTER
      }
      tp.textSize = fitText(tp, timeStr, topTile.width() * 0.85f, topTile.height() * 0.68f, topTile.height() * 0.58f * config.fontScale)
      val fm = tp.fontMetrics
      canvas.drawText(timeStr, topTile.centerX(), topTile.centerY() - (fm.ascent + fm.descent) / 2f, tp)

      val dp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.accentColor
        textAlign = Paint.Align.CENTER
        textSize = (botLeft.height() * 0.28f).coerceIn(9f, 18f)
      }
      canvas.drawText(fmtShortDate(cal), botLeft.centerX(), botLeft.centerY() + dp.textSize * 0.35f, dp)

      drawWeatherIcon(canvas, botRight.left + botRight.width() * 0.32f, botRight.centerY(), botRight.height() * 0.25f, weather.weatherCode, weather.isDay, config.accentColor, config.foregroundColor)
      dp.color = config.foregroundColor
      canvas.drawText(tempStr, botRight.left + botRight.width() * 0.72f, botRight.centerY() + dp.textSize * 0.35f, dp)
    } else {
      // 4x2 Classic Bento: Left Time+Weather | Right Week Calendar Strip + Date!
      val leftW = w * 0.46f
      val leftTile = RectF(rect.left, rect.top, rect.left + leftW, rect.bottom)
      val topRight = RectF(leftTile.right + gap, rect.top, rect.right, rect.top + h * 0.42f)
      val botRight = RectF(leftTile.right + gap, topRight.bottom + gap, rect.right, rect.bottom)

      drawDepthTile(canvas, leftTile, r, config.backgroundColor, config.accentColor, config.depthIntensity, effectiveAlpha)
      drawDepthTile(canvas, topRight, r, config.backgroundColor, config.accentColor, config.depthIntensity, effectiveAlpha)
      drawDepthTile(canvas, botRight, r, config.backgroundColor, config.accentColor, config.depthIntensity, effectiveAlpha)

      val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.foregroundColor
        textAlign = Paint.Align.CENTER
      }
      tp.textSize = fitText(tp, timeStr, leftTile.width() * 0.84f, leftTile.height() * 0.50f, leftTile.height() * 0.42f * config.fontScale)
      val fm = tp.fontMetrics
      canvas.drawText(timeStr, leftTile.centerX(), leftTile.centerY() - (fm.ascent + fm.descent) / 2f - h * 0.08f, tp)

      val dP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.accentColor
        textAlign = Paint.Align.CENTER
        textSize = (h * 0.115f).coerceIn(9f, 17f)
      }
      canvas.drawText(fmtShortDate(cal), leftTile.centerX(), leftTile.bottom - h * 0.10f, dP)

      // Top-right Weather
      val wIconR = (topRight.height() * 0.28f).coerceIn(9f, 22f)
      drawWeatherIcon(canvas, topRight.left + wIconR + 12f, topRight.centerY(), wIconR, weather.weatherCode, weather.isDay, config.accentColor, config.foregroundColor)
      val wTempP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.foregroundColor
        textSize = (topRight.height() * 0.40f).coerceIn(12f, 24f)
      }
      canvas.drawText(tempStr, topRight.left + wIconR * 2f + 18f, topRight.centerY() + wTempP.textSize * 0.34f, wTempP)
      val cityP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.secondaryTextColor
        textAlign = Paint.Align.RIGHT
        textSize = (topRight.height() * 0.24f).coerceIn(9f, 16f)
      }
      canvas.drawText(weather.cityName, topRight.right - 12f, topRight.centerY() + cityP.textSize * 0.34f, cityP)

      // Bottom-right Week Calendar Strip
      drawWeekStrip(
        canvas = canvas,
        rect = RectF(botRight.left + 8f, botRight.top + 6f, botRight.right - 8f, botRight.bottom - 6f),
        config = config,
        typeface = typeface,
        cal = cal,
      )
    }
  }

  // ---------------------------------------------------------------------------
  // 16. AURORA DEPTH GLASS WEATHER
  // ---------------------------------------------------------------------------
  fun drawDepthGlassWeather(
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

    val effectiveAlpha = if (config.cardStyle == CardStyle.TRANSPARENT) 0f else config.backgroundAlpha

    if (effectiveAlpha > 0.02f) {
      // Ambient 3D radial depth glow
      val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
          rect.right - w * 0.18f,
          rect.top + h * 0.22f,
          minD * 0.72f,
          ColorUtils.setAlphaComponent(config.accentColor, (120 * config.depthIntensity).roundToInt()),
          Color.TRANSPARENT,
          Shader.TileMode.CLAMP,
        )
      }
      canvas.drawCircle(rect.right - w * 0.18f, rect.top + h * 0.22f, minD * 0.72f, glowPaint)
    }

    val wide = w > h * 1.45f
    val tall = h >= w * 0.85f
    val timeStr = buildString {
      append(fmtHour(cal, config.is24Hour))
      append(":")
      append(fmtMin(cal))
      if (config.showSeconds && wide) {
        append(":${fmtSec(cal)}")
      }
    }

    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      setShadowLayer(8f * config.depthIntensity, 0f, 4f * config.depthIntensity, Color.argb(120, 0, 0, 0))
    }

    val tempStr = weather.displayTemp(config.useFahrenheit)
    val cityP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.accentColor
      textSize = (minD * 0.10f).coerceIn(10f, 20f)
    }

    if (tall) {
      // Expanded Vertical: Top weather header -> Middle giant time -> Bottom Week Strip
      val iconR = (minD * 0.11f).coerceIn(12f, 28f)
      drawWeatherIcon(canvas, rect.left + iconR + 6f, rect.top + iconR + 4f, iconR, weather.weatherCode, weather.isDay, config.accentColor, config.foregroundColor)
      val wTitleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.foregroundColor
        textSize = (minD * 0.13f).coerceIn(12f, 26f)
      }
      canvas.drawText("$tempStr  ${weather.conditionText()}", rect.left + iconR * 2f + 16f, rect.top + iconR + wTitleP.textSize * 0.25f, wTitleP)
      canvas.drawText("${weather.cityName} • ${weather.displayHighLow(config.useFahrenheit)}", rect.left + iconR * 2f + 16f, rect.top + iconR + wTitleP.textSize * 1.15f, cityP)

      timePaint.textAlign = Paint.Align.CENTER
      timePaint.textSize = fitText(timePaint, timeStr, w * 0.88f, h * 0.34f, minD * 0.42f * config.fontScale)
      val fm = timePaint.fontMetrics
      canvas.drawText(timeStr, rect.centerX(), rect.centerY() - (fm.ascent + fm.descent) / 2f, timePaint)

      val stripCard = RectF(rect.left, rect.bottom - h * 0.26f, rect.right, rect.bottom)
      drawDepthTile(canvas, stripCard, minD * 0.08f, config.backgroundColor, config.accentColor, config.depthIntensity, effectiveAlpha)
      drawWeekStrip(
        canvas = canvas,
        rect = RectF(stripCard.left + 8f, stripCard.top + 4f, stripCard.right - 8f, stripCard.bottom - 4f),
        config = config,
        typeface = typeface,
        cal = cal,
      )
    } else {
      // Left Time & Date, Right Elevated Weather Depth Card
      timePaint.textAlign = Paint.Align.LEFT
      timePaint.textSize = fitText(timePaint, timeStr, w * 0.56f, h * 0.58f, minD * 0.52f * config.fontScale)
      val fm = timePaint.fontMetrics
      val timeY = rect.centerY() - (fm.ascent + fm.descent) / 2f - (if (config.showDate) h * 0.08f else 0f)
      canvas.drawText(timeStr, rect.left + 6f, timeY, timePaint)

      if (config.showDate) {
        cityP.color = config.secondaryTextColor
        canvas.drawText(fmtShortDate(cal), rect.left + 6f, rect.bottom - 8f, cityP)
      }

      val cardRect = RectF(rect.left + w * 0.60f, rect.top + 4f, rect.right - 2f, rect.bottom - 4f)
      drawDepthTile(canvas, cardRect, minD * 0.14f, config.backgroundColor, config.accentColor, config.depthIntensity, effectiveAlpha)
      val iconR = (min(cardRect.width(), cardRect.height()) * 0.22f).coerceIn(10f, 24f)
      drawWeatherIcon(canvas, cardRect.centerX(), cardRect.top + cardRect.height() * 0.32f, iconR, weather.weatherCode, weather.isDay, config.accentColor, config.foregroundColor)
      val tP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.foregroundColor
        textAlign = Paint.Align.CENTER
        textSize = (cardRect.height() * 0.25f).coerceIn(12f, 26f)
      }
      canvas.drawText(tempStr, cardRect.centerX(), cardRect.top + cardRect.height() * 0.72f, tP)
      val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.accentColor
        textAlign = Paint.Align.CENTER
        textSize = (cardRect.height() * 0.14f).coerceIn(8f, 15f)
      }
      canvas.drawText(weather.cityName, cardRect.centerX(), cardRect.bottom - 10f, subP)
    }
  }

  // ---------------------------------------------------------------------------
  // 17. EXPANDABLE DYNAMIC STACK
  // ---------------------------------------------------------------------------
  fun drawExpandableStack(
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
    val r = (minD * 0.11f).coerceIn(10f, 24f)
    val timeStr = "${fmtHour(cal, config.is24Hour)}:${fmtMin(cal)}"
    val tempStr = weather.displayTemp(config.useFahrenheit)

    val layers = when {
      h >= w * 0.75f -> 3
      h >= w * 0.42f -> 2
      else -> 1
    }

    val effectiveAlpha = if (config.cardStyle == CardStyle.TRANSPARENT) 0f else config.backgroundAlpha

    if (layers == 1) {
      val card = RectF(rect.left, rect.top, rect.right, rect.bottom)
      drawDepthTile(canvas, card, r, config.backgroundColor, config.accentColor, config.depthIntensity, effectiveAlpha)
      val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.foregroundColor
      }
      tp.textSize = fitText(tp, timeStr, w * 0.48f, h * 0.74f, h * 0.62f * config.fontScale)
      val fm = tp.fontMetrics
      canvas.drawText(timeStr, card.left + 16f, card.centerY() - (fm.ascent + fm.descent) / 2f, tp)

      val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.accentColor
        textAlign = Paint.Align.RIGHT
        textSize = (h * 0.24f).coerceIn(10f, 18f)
      }
      canvas.drawText("${fmtShortDate(cal)}  •  $tempStr", card.right - 16f, card.centerY() + sub.textSize * 0.35f, sub)
      return
    }

    val gap = (minD * 0.045f).coerceIn(6f, 12f)
    val layer1H = if (layers == 3) h * 0.42f else h * 0.60f
    val layer1 = RectF(rect.left, rect.top, rect.right, rect.top + layer1H)
    drawDepthTile(canvas, layer1, r, config.backgroundColor, config.accentColor, config.depthIntensity, effectiveAlpha)

    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
    }
    tp.textSize = fitText(tp, timeStr, layer1.width() * 0.60f, layer1.height() * 0.72f, layer1.height() * 0.56f * config.fontScale)
    val fm = tp.fontMetrics
    canvas.drawText(timeStr, layer1.left + 18f, layer1.centerY() - (fm.ascent + fm.descent) / 2f, tp)

    val lblP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.accentColor
      textAlign = Paint.Align.RIGHT
      textSize = (layer1.height() * 0.18f).coerceIn(10f, 18f)
    }
    canvas.drawText(config.customLabel.ifBlank { "STACK" }, layer1.right - 16f, layer1.centerY() - 2f, lblP)
    lblP.color = config.secondaryTextColor
    canvas.drawText(fmtShortDate(cal), layer1.right - 16f, layer1.centerY() + lblP.textSize * 1.35f, lblP)

    if (layers == 2) {
      val layer2 = RectF(rect.left + w * 0.04f, layer1.bottom + gap, rect.right - w * 0.04f, rect.bottom)
      drawDepthTile(canvas, layer2, r * 0.9f, ColorUtils.blendARGB(config.backgroundColor, config.accentColor, 0.10f), config.accentColor, config.depthIntensity, effectiveAlpha)
      val iconR = (layer2.height() * 0.30f).coerceIn(10f, 20f)
      drawWeatherIcon(canvas, layer2.left + iconR + 16f, layer2.centerY(), iconR, weather.weatherCode, weather.isDay, config.accentColor, config.foregroundColor)
      val wTextP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.foregroundColor
        textSize = (layer2.height() * 0.34f).coerceIn(11f, 20f)
      }
      canvas.drawText("$tempStr  ${weather.conditionText()}", layer2.left + iconR * 2f + 24f, layer2.centerY() + wTextP.textSize * 0.34f, wTextP)
      wTextP.color = config.accentColor
      wTextP.textAlign = Paint.Align.RIGHT
      canvas.drawText(weather.cityName, layer2.right - 16f, layer2.centerY() + wTextP.textSize * 0.34f, wTextP)
    } else {
      // 3-Layer Expanded Stack: Layer 2 = Weather Pill, Layer 3 = Full Week Calendar Strip!
      val layer2H = h * 0.22f
      val layer2 = RectF(rect.left + w * 0.025f, layer1.bottom + gap, rect.right - w * 0.025f, layer1.bottom + gap + layer2H)
      val layer3 = RectF(rect.left + w * 0.05f, layer2.bottom + gap, rect.right - w * 0.05f, rect.bottom)

      drawDepthTile(canvas, layer2, r * 0.85f, ColorUtils.blendARGB(config.backgroundColor, config.accentColor, 0.12f), config.accentColor, config.depthIntensity, effectiveAlpha)
      drawDepthTile(canvas, layer3, r * 0.85f, config.backgroundColor, config.accentColor, config.depthIntensity, effectiveAlpha)

      val iconR = (layer2.height() * 0.30f).coerceIn(10f, 20f)
      drawWeatherIcon(canvas, layer2.left + iconR + 14f, layer2.centerY(), iconR, weather.weatherCode, weather.isDay, config.accentColor, config.foregroundColor)
      val wTextP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.foregroundColor
        textSize = (layer2.height() * 0.36f).coerceIn(11f, 20f)
      }
      canvas.drawText("$tempStr • ${weather.conditionText()}", layer2.left + iconR * 2f + 22f, layer2.centerY() + wTextP.textSize * 0.34f, wTextP)
      wTextP.color = config.accentColor
      wTextP.textAlign = Paint.Align.RIGHT
      canvas.drawText(weather.displayHighLow(config.useFahrenheit), layer2.right - 14f, layer2.centerY() + wTextP.textSize * 0.34f, wTextP)

      drawWeekStrip(
        canvas = canvas,
        rect = RectF(layer3.left + 10f, layer3.top + 6f, layer3.right - 10f, layer3.bottom - 6f),
        config = config,
        typeface = typeface,
        cal = cal,
      )
    }
  }

  // ---------------------------------------------------------------------------
  // 18. WORLD AGENDA / CALENDAR MATRIX PRO
  // ---------------------------------------------------------------------------
  fun drawWorldAgendaPro(
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
    val tempStr = weather.displayTemp(config.useFahrenheit)

    if (w >= h * 1.35f) {
      // Split: Left Clock + Weather + Day-of-Year | Right Full Month Calendar!
      val leftRect = RectF(rect.left, rect.top, rect.left + w * 0.44f, rect.bottom)
      val rightRect = RectF(leftRect.right + minD * 0.08f, rect.top, rect.right, rect.bottom)

      val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(config.secondaryTextColor, 70)
        strokeWidth = 2f
      }
      canvas.drawLine(leftRect.right + minD * 0.04f, rect.top + 6f, leftRect.right + minD * 0.04f, rect.bottom - 6f, divPaint)

      val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.foregroundColor
      }
      tp.textSize = fitText(tp, timeStr, leftRect.width() * 0.96f, leftRect.height() * 0.42f, leftRect.height() * 0.36f * config.fontScale)
      canvas.drawText(timeStr, leftRect.left, leftRect.top + tp.textSize * 1.0f, tp)

      val dateP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.accentColor
        textSize = (minD * 0.095f).coerceIn(10f, 18f)
      }
      canvas.drawText(fmtShortDate(cal), leftRect.left, leftRect.top + tp.textSize * 1.45f, dateP)

      // Weather block at bottom-left
      val iconR = (minD * 0.095f).coerceIn(9f, 20f)
      val wy = leftRect.bottom - iconR - 4f
      drawWeatherIcon(canvas, leftRect.left + iconR, wy, iconR, weather.weatherCode, weather.isDay, config.accentColor, config.foregroundColor)
      val wp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.foregroundColor
        textSize = (minD * 0.11f).coerceIn(10f, 20f)
      }
      canvas.drawText("$tempStr  ${weather.cityName}", leftRect.left + iconR * 2.4f, wy + wp.textSize * 0.34f, wp)

      drawMonthCalendarGrid(canvas, rightRect, config, typeface, cal)
    } else {
      // Square/Tall: Top Time+Weather bar, Bottom Full Month Calendar
      val topH = h * 0.28f
      val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.foregroundColor
      }
      tp.textSize = fitText(tp, timeStr, w * 0.56f, topH * 0.80f, topH * 0.68f * config.fontScale)
      canvas.drawText(timeStr, rect.left, rect.top + tp.textSize * 0.92f, tp)

      val iconR = (topH * 0.24f).coerceIn(9f, 20f)
      drawWeatherIcon(canvas, rect.right - iconR * 3.8f, rect.top + topH * 0.45f, iconR, weather.weatherCode, weather.isDay, config.accentColor, config.foregroundColor)
      val wp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.accentColor
        textAlign = Paint.Align.RIGHT
        textSize = (topH * 0.34f).coerceIn(12f, 22f)
      }
      canvas.drawText(tempStr, rect.right, rect.top + topH * 0.58f, wp)

      drawMonthCalendarGrid(
        canvas = canvas,
        rect = RectF(rect.left, rect.top + topH + 6f, rect.right, rect.bottom),
        config = config,
        typeface = typeface,
        cal = cal,
      )
    }
  }

  // ---------------------------------------------------------------------------
  // 19. 3D RECESSED CHRONO DIAL (Advanced Depth Analog + Weather Aperture)
  // ---------------------------------------------------------------------------
  fun drawChrono3dDepth(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
    weather: WeatherSnapshot,
  ) {
    val w = rect.width()
    val h = rect.height()
    val wide = w > h * 1.50f
    val dialR = if (wide) h * 0.46f else min(w, h) * 0.45f
    val cx = if (wide) rect.left + dialR + 6f else rect.centerX()
    val cy = rect.centerY()

    // Outer metallic bevel ring
    val bezelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = LinearGradient(
        cx - dialR,
        cy - dialR,
        cx + dialR,
        cy + dialR,
        ColorUtils.blendARGB(config.backgroundColor, Color.WHITE, 0.26f * config.depthIntensity),
        ColorUtils.blendARGB(config.backgroundColor, Color.BLACK, 0.55f),
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawCircle(cx, cy, dialR, bezelPaint)

    // Recessed inner dial well
    val wellR = dialR * 0.88f
    val wellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = RadialGradient(
        cx,
        cy - wellR * 0.15f,
        wellR,
        ColorUtils.blendARGB(config.backgroundColor, config.accentColor, 0.08f),
        ColorUtils.blendARGB(config.backgroundColor, Color.BLACK, 0.48f),
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawCircle(cx, cy, wellR, wellPaint)

    // Inner depth rim shadow
    val innerRim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = (dialR * 0.04f).coerceAtLeast(2f)
      color = Color.argb((120 * config.depthIntensity).roundToInt(), 0, 0, 0)
    }
    canvas.drawCircle(cx, cy, wellR * 0.98f, innerRim)

    // Raised ticks
    val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    for (i in 0 until 60) {
      val a = Math.toRadians((i * 6 - 90).toDouble())
      val isHr = i % 5 == 0
      val r0 = if (isHr) wellR * 0.76f else wellR * 0.86f
      val r1 = wellR * 0.93f
      tickPaint.strokeWidth = if (isHr) dialR * 0.045f else dialR * 0.016f
      tickPaint.color = if (i % 15 == 0) config.accentColor else config.foregroundColor
      canvas.drawLine(
        cx + (cos(a) * r0).toFloat(),
        cy + (sin(a) * r0).toFloat(),
        cx + (cos(a) * r1).toFloat(),
        cy + (sin(a) * r1).toFloat(),
        tickPaint,
      )
    }

    // Recessed Weather + Date Aperture at 6 o'clock (or right panel if wide)
    if (!wide && (config.showWeather || config.showDate)) {
      val boxW = wellR * 0.72f
      val boxH = wellR * 0.25f
      val box = RectF(cx - boxW / 2f, cy + wellR * 0.30f, cx + boxW / 2f, cy + wellR * 0.30f + boxH)
      val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.blendARGB(config.backgroundColor, Color.BLACK, 0.40f)
      }
      canvas.drawRoundRect(box, boxH / 2f, boxH / 2f, boxPaint)
      val apText = if (config.showWeather) "${weather.displayTemp(config.useFahrenheit)} • ${cal.get(Calendar.DAY_OF_MONTH)}" else fmtShortDate(cal)
      val apP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.accentColor
        textAlign = Paint.Align.CENTER
        textSize = boxH * 0.62f
      }
      val afm = apP.fontMetrics
      canvas.drawText(apText, box.centerX(), box.centerY() - (afm.ascent + afm.descent) / 2f, apP)
    } else if (wide) {
      val rightLeft = cx + dialR + h * 0.12f
      val availW = (rect.right - rightLeft).coerceAtLeast(40f)
      val timeStr = "${fmtHour(cal, config.is24Hour)}:${fmtMin(cal)}"
      val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.foregroundColor
      }
      tp.textSize = fitText(tp, timeStr, availW * 0.94f, h * 0.42f, h * 0.38f * config.fontScale)
      canvas.drawText(timeStr, rightLeft, rect.centerY() - h * 0.04f, tp)

      val wp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.accentColor
        textSize = (h * 0.14f).coerceIn(10f, 20f)
      }
      val sub = "${fmtShortDate(cal)}  •  ${weather.displayTemp(config.useFahrenheit)}"
      canvas.drawText(sub, rightLeft, rect.centerY() + wp.textSize * 1.45f, wp)
    }

    // 3D Drop-Shadowed Hands
    val sec = cal.get(Calendar.SECOND)
    val min = cal.get(Calendar.MINUTE) + sec / 60f
    val hour = (cal.get(Calendar.HOUR) % 12) + min / 60f
    val hrA = Math.toRadians((hour * 30f - 90f).toDouble())
    val minA = Math.toRadians((min * 6f - 90f).toDouble())
    val secA = Math.toRadians((sec * 6f - 90f).toDouble())

    val hrP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = config.foregroundColor
      strokeWidth = (dialR * 0.08f).coerceAtLeast(3f)
      strokeCap = Paint.Cap.ROUND
      setShadowLayer(6f * config.depthIntensity, 2f, 3f, Color.argb(160, 0, 0, 0))
    }
    canvas.drawLine(cx, cy, cx + (cos(hrA) * wellR * 0.50f).toFloat(), cy + (sin(hrA) * wellR * 0.50f).toFloat(), hrP)

    val minP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = config.foregroundColor
      strokeWidth = (dialR * 0.052f).coerceAtLeast(2.2f)
      strokeCap = Paint.Cap.ROUND
      setShadowLayer(6f * config.depthIntensity, 2f, 3f, Color.argb(160, 0, 0, 0))
    }
    canvas.drawLine(cx, cy, cx + (cos(minA) * wellR * 0.76f).toFloat(), cy + (sin(minA) * wellR * 0.76f).toFloat(), minP)

    if (config.showSeconds) {
      val secP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.accentColor
        strokeWidth = (dialR * 0.022f).coerceAtLeast(1.5f)
        strokeCap = Paint.Cap.ROUND
      }
      canvas.drawLine(
        cx - (cos(secA) * wellR * 0.18f).toFloat(),
        cy - (sin(secA) * wellR * 0.18f).toFloat(),
        cx + (cos(secA) * wellR * 0.84f).toFloat(),
        cy + (sin(secA) * wellR * 0.84f).toFloat(),
        secP,
      )
    }

    canvas.drawCircle(cx, cy, dialR * 0.075f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = config.accentColor })
  }

  // ---------------------------------------------------------------------------
  // 20. NEUMORPH SCULPT 3D
  // ---------------------------------------------------------------------------
  fun drawNeumorphSculpt(
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
    val well = RectF(rect.left + minD * 0.04f, rect.top + minD * 0.04f, rect.right - minD * 0.04f, rect.bottom - minD * 0.04f)
    val wellR = (minD * 0.14f).coerceIn(10f, 28f)

    if (config.cardStyle != CardStyle.TRANSPARENT && config.backgroundAlpha > 0.02f) {
      // Light top-left & dark bottom-right neumorphic bevels
      val lightEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (minD * 0.022f).coerceIn(2f, 5f)
        color = Color.argb((45 * config.depthIntensity).roundToInt(), 255, 255, 255)
      }
      val darkEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = lightEdge.strokeWidth
        color = Color.argb((110 * config.depthIntensity).roundToInt(), 0, 0, 0)
      }
      val wellFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.blendARGB(config.backgroundColor, Color.BLACK, 0.22f)
      }
      canvas.drawRoundRect(well, wellR, wellR, wellFill)
      canvas.drawRoundRect(RectF(well.left + 2f, well.top + 2f, well.right, well.bottom), wellR, wellR, lightEdge)
      canvas.drawRoundRect(RectF(well.left, well.top, well.right - 2f, well.bottom - 2f), wellR, wellR, darkEdge)
    }

    val timeStr = "${fmtHour(cal, config.is24Hour)}:${fmtMin(cal)}"
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      textAlign = Paint.Align.CENTER
      if (config.cardStyle == CardStyle.TRANSPARENT || config.backgroundAlpha <= 0.05f) {
        setShadowLayer(10f, 0f, 3f, Color.argb(170, 0, 0, 0))
      }
    }
    tp.textSize = fitText(tp, timeStr, well.width() * 0.84f, well.height() * 0.50f, minD * 0.46f * config.fontScale)
    val fm = tp.fontMetrics
    canvas.drawText(timeStr, well.centerX(), well.centerY() - (fm.ascent + fm.descent) / 2f - well.height() * 0.08f, tp)

    val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.accentColor
      textAlign = Paint.Align.CENTER
      textSize = (minD * 0.10f).coerceIn(10f, 20f)
    }
    val subText = buildString {
      if (config.showDate) append(fmtShortDate(cal))
      if (config.showWeather) {
        if (isNotEmpty()) append("  •  ")
        append("${weather.displayTemp(config.useFahrenheit)} ${weather.cityName}")
      }
    }
    if (subText.isNotEmpty()) {
      canvas.drawText(subText, well.centerX(), well.bottom - minD * 0.10f, subP)
    }
  }
}
