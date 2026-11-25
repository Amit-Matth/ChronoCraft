package com.example.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils
import com.example.model.BuilderLayoutType
import com.example.model.BuilderMotif
import com.example.model.CardStyle
import com.example.model.ClockAlignment
import com.example.model.ClockDateFormat
import com.example.model.SecondsStyle
import com.example.model.WeatherSnapshot
import com.example.model.WidgetConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal object ClockCreativeStyles {

  private fun fmtHour(cal: Calendar, is24Hour: Boolean): String {
    val h = if (is24Hour) cal.get(Calendar.HOUR_OF_DAY) else cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    return String.format(Locale.US, "%02d", h)
  }

  private fun fmtMin(cal: Calendar): String = String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE))

  private fun fmtSec(cal: Calendar): String = String.format(Locale.US, "%02d", cal.get(Calendar.SECOND))

  fun fmtDateWithFormat(cal: Calendar, format: ClockDateFormat): String {
    return SimpleDateFormat(format.pattern, Locale.getDefault()).format(cal.time)
  }

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

  private fun applyDepthShadow(paint: Paint, config: WidgetConfig) {
    if (config.showShadow && (config.cardStyle == CardStyle.TRANSPARENT || config.depthIntensity > 0.1f)) {
      paint.setShadowLayer(10f, 0f, 3f, Color.argb((150 * config.depthIntensity.coerceAtLeast(0.4f)).roundToInt().coerceIn(0, 220), 0, 0, 0))
    }
  }

  private fun alignX(rect: RectF, alignment: ClockAlignment, pad: Float = 8f): Pair<Float, Paint.Align> {
    return when (alignment) {
      ClockAlignment.START -> (rect.left + pad) to Paint.Align.LEFT
      ClockAlignment.CENTER -> rect.centerX() to Paint.Align.CENTER
      ClockAlignment.END -> (rect.right - pad) to Paint.Align.RIGHT
    }
  }

  private fun drawAlignedTime(
    canvas: Canvas,
    rect: RectF,
    baselineY: Float,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
    maxW: Float,
    maxH: Float,
    startSize: Float,
  ) {
    val hh = fmtHour(cal, config.is24Hour)
    val mm = fmtMin(cal)
    val ss = if (config.showSeconds && config.secondsStyle == SecondsStyle.DIGITS) ":${fmtSec(cal)}" else ""
    val fullStr = "$hh:$mm$ss"

    val hPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.hourColor ?: config.foregroundColor
      isFakeBoldText = true
      applyDepthShadow(this, config)
    }
    val size = fitText(hPaint, fullStr, maxW, maxH, startSize)
    hPaint.textSize = size
    val cPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.accentColor
      textSize = size
      applyDepthShadow(this, config)
    }
    val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.minuteColor ?: config.foregroundColor
      textSize = size
      applyDepthShadow(this, config)
    }
    val sPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.secondsColor ?: config.accentColor
      textSize = size * 0.56f
      applyDepthShadow(this, config)
    }

    val totalW = hPaint.measureText(hh) + cPaint.measureText(":") + mPaint.measureText(mm) +
      (if (ss.isNotEmpty()) sPaint.measureText(ss) else 0f)
    val startX = when (config.alignment) {
      ClockAlignment.START -> rect.left + 8f
      ClockAlignment.CENTER -> rect.centerX() - totalW / 2f
      ClockAlignment.END -> rect.right - totalW - 8f
    }

    canvas.drawText(hh, startX, baselineY, hPaint)
    val x1 = startX + hPaint.measureText(hh)
    canvas.drawText(":", x1, baselineY - size * 0.04f, cPaint)
    val x2 = x1 + cPaint.measureText(":")
    canvas.drawText(mm, x2, baselineY, mPaint)
    if (ss.isNotEmpty()) {
      canvas.drawText(ss, x2 + mPaint.measureText(mm), baselineY, sPaint)
    }
  }

  // ---------------------------------------------------------------------------
  // 1. LIQUID GLASS PRISM (Refractive specular glass bubbles & luminous time)
  // ---------------------------------------------------------------------------
  fun drawLiquidGlassPrism(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
    weather: WeatherSnapshot,
    animPhase: Float,
  ) {
    val w = rect.width()
    val h = rect.height()
    val minD = min(w, h)

    if (config.showDecorations) {
      val drift = sin(animPhase * 2.0 * PI).toFloat() * (minD * 0.05f)
      val b1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
          rect.left + w * 0.25f + drift, rect.top + h * 0.28f, minD * 0.45f,
          ColorUtils.setAlphaComponent(config.accentColor, 95),
          Color.TRANSPARENT, Shader.TileMode.CLAMP,
        )
      }
      val b2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
          rect.right - w * 0.22f - drift, rect.bottom - h * 0.25f, minD * 0.42f,
          ColorUtils.setAlphaComponent(config.foregroundColor, 55),
          Color.TRANSPARENT, Shader.TileMode.CLAMP,
        )
      }
      canvas.drawCircle(rect.left + w * 0.25f + drift, rect.top + h * 0.28f, minD * 0.45f, b1)
      canvas.drawCircle(rect.right - w * 0.22f - drift, rect.bottom - h * 0.25f, minD * 0.42f, b2)

      // Specular glass pill outline inside widget
      val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        shader = LinearGradient(
          rect.left, rect.top, rect.right, rect.bottom,
          Color.argb(130, 255, 255, 255), Color.argb(24, 255, 255, 255), Shader.TileMode.CLAMP,
        )
      }
      canvas.drawRoundRect(RectF(rect.left + 4f, rect.top + 4f, rect.right - 4f, rect.bottom - 4f), minD * 0.16f, minD * 0.16f, rim)
    }

    val baseline = rect.centerY() + (minD * 0.10f) - (if (config.showDate) h * 0.08f else 0f)
    drawAlignedTime(canvas, rect, baseline, config, typeface, cal, w * 0.88f, h * 0.54f, minD * 0.48f * config.fontScale)

    val (ax, align) = alignX(rect, config.alignment, 12f)
    val metaP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.secondaryTextColor
      textAlign = align
      textSize = (minD * 0.095f).coerceIn(10f, 17f)
    }
    val sub = buildString {
      if (config.showDate) append(fmtDateWithFormat(cal, config.dateFormat))
      if (config.showWeather && weather.isLoaded) {
        if (isNotEmpty()) append("  •  ")
        append(weather.displayTemp(config.useFahrenheit))
      }
    }
    if (sub.isNotEmpty()) {
      canvas.drawText(sub, ax, rect.bottom - 10f, metaP)
    }
  }

  // ---------------------------------------------------------------------------
  // 2. AURORA RIBBON FLOW (Layered flowing gradient ribbons + floating time)
  // ---------------------------------------------------------------------------
  fun drawAuroraRibbonFlow(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
    weather: WeatherSnapshot,
    animPhase: Float,
  ) {
    val w = rect.width()
    val h = rect.height()
    val minD = min(w, h)
    val twoPi = animPhase * 2.0 * PI

    if (config.showDecorations) {
      for (layer in 0..1) {
        val path = Path().apply {
          val baseY = rect.top + h * (0.48f + layer * 0.16f)
          moveTo(rect.left, rect.bottom)
          lineTo(rect.left, baseY)
          for (i in 0..18) {
            val t = i / 18f
            val wave = sin(t * PI * 2.2 + twoPi + layer * 1.3).toFloat() * (h * 0.10f)
            lineTo(rect.left + w * t, baseY + wave)
          }
          lineTo(rect.right, rect.bottom)
          close()
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            ColorUtils.setAlphaComponent(config.accentColor, 65 - layer * 20),
            ColorUtils.setAlphaComponent(config.backgroundSecondaryColor, 95),
            Shader.TileMode.CLAMP,
          )
        }
        canvas.drawPath(path, paint)
      }
    }

    val baseline = rect.centerY() + minD * 0.08f
    drawAlignedTime(canvas, rect, baseline, config, typeface, cal, w * 0.88f, h * 0.52f, minD * 0.46f * config.fontScale)

    val (ax, align) = alignX(rect, config.alignment, 10f)
    val metaP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.secondaryTextColor
      textAlign = align
      textSize = (minD * 0.09f).coerceIn(10f, 16f)
    }
    if (config.showDate) {
      canvas.drawText(fmtDateWithFormat(cal, config.dateFormat).uppercase(Locale.getDefault()), ax, rect.top + metaP.textSize * 1.4f, metaP)
    }
    if (config.showWeather && weather.isLoaded) {
      metaP.color = config.accentColor
      canvas.drawText("${weather.displayTemp(config.useFahrenheit)}  ${weather.conditionText()}", ax, rect.bottom - 8f, metaP)
    }
  }

  // ---------------------------------------------------------------------------
  // 3. SOLAR ORBIT PLANETS (Concentric planetary rings + live revolving satellites)
  // ---------------------------------------------------------------------------
  fun drawSolarOrbitPlanets(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
    weather: WeatherSnapshot,
    animPhase: Float,
  ) {
    val w = rect.width()
    val h = rect.height()
    val wide = w > h * 1.45f
    val minD = min(w, h)
    val dialR = (if (wide) h * 0.44f else minD * 0.43f)
    val cx = if (wide) rect.left + dialR + 10f else rect.centerX()
    val cy = rect.centerY()

    val ringP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 1.6f
    }
    for (idx in 1..3) {
      val r = dialR * (0.45f + idx * 0.18f)
      ringP.color = ColorUtils.setAlphaComponent(if (idx == 2) config.accentColor else config.secondaryTextColor, 65 + idx * 15)
      canvas.drawCircle(cx, cy, r, ringP)
    }

    val secFrac = cal.get(Calendar.SECOND) / 60f
    val minFrac = cal.get(Calendar.MINUTE) / 60f
    val pAngle1 = Math.toRadians(((secFrac + animPhase * 0.15f) * 360f - 90f).toDouble())
    val pAngle2 = Math.toRadians((minFrac * 360f - 90f).toDouble())

    val outerR = dialR * 0.99f
    val midR = dialR * 0.81f
    val planet1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = config.secondsColor ?: config.accentColor
      setShadowLayer(8f, 0f, 0f, color)
    }
    val planet2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = config.secondaryTextColor
    }
    canvas.drawCircle(cx + (cos(pAngle1) * outerR).toFloat(), cy + (sin(pAngle1) * outerR).toFloat(), (dialR * 0.08f).coerceIn(4f, 9f), planet1)
    canvas.drawCircle(cx + (cos(pAngle2) * midR).toFloat(), cy + (sin(pAngle2) * midR).toFloat(), (dialR * 0.06f).coerceIn(3f, 7f), planet2)

    val timeStr = "${fmtHour(cal, config.is24Hour)}:${fmtMin(cal)}"
    val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      isFakeBoldText = true
      applyDepthShadow(this, config)
    }

    if (wide) {
      val rx = cx + dialR + 18f
      tPaint.textAlign = Paint.Align.LEFT
      tPaint.textSize = fitText(tPaint, timeStr, (rect.right - rx).coerceAtLeast(50f), h * 0.48f, h * 0.44f * config.fontScale)
      val fm = tPaint.fontMetrics
      val by = rect.centerY() - (fm.ascent + fm.descent) / 2f - h * 0.08f
      canvas.drawText(timeStr, rx, by, tPaint)

      val sunP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = config.accentColor }
      canvas.drawCircle(cx, cy, dialR * 0.14f, sunP)

      val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.secondaryTextColor
        textSize = (h * 0.13f).coerceIn(10f, 16f)
      }
      val meta = buildString {
        if (config.showDate) append(fmtDateWithFormat(cal, config.dateFormat))
        if (config.showWeather && weather.isLoaded) {
          if (isNotEmpty()) append(" • ")
          append(weather.displayTemp(config.useFahrenheit))
        }
      }
      if (meta.isNotEmpty()) canvas.drawText(meta, rx, by + mPaint.textSize * 1.8f, mPaint)
    } else {
      tPaint.textAlign = Paint.Align.CENTER
      tPaint.textSize = fitText(tPaint, timeStr, dialR * 1.15f, dialR * 0.65f, dialR * 0.46f * config.fontScale)
      val fm = tPaint.fontMetrics
      canvas.drawText(timeStr, cx, cy - (fm.ascent + fm.descent) / 2f, tPaint)
      if (config.showDate) {
        val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          this.typeface = typeface
          color = config.secondaryTextColor
          textAlign = Paint.Align.CENTER
          textSize = (minD * 0.085f).coerceIn(9f, 15f)
        }
        canvas.drawText(fmtDateWithFormat(cal, config.dateFormat), cx, rect.bottom - 2f, mPaint)
      }
    }
  }

  // ---------------------------------------------------------------------------
  // 4. STARFIELD CONSTELLATION (Star map nodes + crescent moon + serif clock)
  // ---------------------------------------------------------------------------
  fun drawStarfieldConstellation(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
    weather: WeatherSnapshot,
    animPhase: Float,
  ) {
    val w = rect.width()
    val h = rect.height()
    val minD = min(w, h)

    if (config.showDecorations) {
      val pts = listOf(
        rect.left + w * 0.12f to rect.top + h * 0.24f,
        rect.left + w * 0.28f to rect.top + h * 0.14f,
        rect.left + w * 0.42f to rect.top + h * 0.28f,
        rect.right - w * 0.26f to rect.top + h * 0.16f,
        rect.right - w * 0.12f to rect.top + h * 0.30f,
      )
      val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(config.secondaryTextColor, 72)
        strokeWidth = 1.4f
      }
      for (i in 0 until pts.size - 1) {
        canvas.drawLine(pts[i].first, pts[i].second, pts[i + 1].first, pts[i + 1].second, lineP)
      }
      val starP = Paint(Paint.ANTI_ALIAS_FLAG)
      pts.forEachIndexed { idx, (px, py) ->
        val twinkle = 0.55f + 0.45f * sin((animPhase * 2.0 * PI) + idx * 1.4).toFloat()
        starP.color = ColorUtils.setAlphaComponent(config.accentColor, (220 * twinkle).roundToInt().coerceIn(80, 255))
        canvas.drawCircle(px, py, (minD * 0.018f).coerceIn(2f, 4.5f), starP)
      }

      // Crescent Moon top-right
      val mx = rect.right - minD * 0.14f
      val my = rect.top + minD * 0.16f
      val mr = (minD * 0.085f).coerceIn(7f, 16f)
      canvas.drawCircle(mx, my, mr, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = config.accentColor })
      canvas.drawCircle(mx + mr * 0.38f, my - mr * 0.24f, mr * 0.82f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = config.backgroundColor })
    }

    val baseline = rect.centerY() + minD * 0.15f
    drawAlignedTime(canvas, rect, baseline, config, typeface, cal, w * 0.86f, h * 0.50f, minD * 0.46f * config.fontScale)

    val (ax, align) = alignX(rect, config.alignment, 12f)
    val metaP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.secondaryTextColor
      textAlign = align
      textSize = (minD * 0.092f).coerceIn(10f, 16f)
    }
    val sub = buildString {
      if (config.showDate) append(fmtDateWithFormat(cal, config.dateFormat))
      if (config.showWeather && weather.isLoaded) {
        if (isNotEmpty()) append("  ✦  ")
        append(weather.displayTemp(config.useFahrenheit))
      }
    }
    if (sub.isNotEmpty()) canvas.drawText(sub, ax, rect.bottom - 6f, metaP)
  }

  // ---------------------------------------------------------------------------
  // 5. DOODLE SKETCH NOTE (Hand-drawn dashed marker border, squiggles & cute notes)
  // ---------------------------------------------------------------------------
  fun drawDoodleSketchNote(
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

    if (config.showDecorations) {
      val sketchP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
        color = config.foregroundColor
        pathEffect = DashPathEffect(floatArrayOf(18f, 8f), 0f)
      }
      val r = (minD * 0.12f).coerceIn(10f, 22f)
      canvas.drawRoundRect(RectF(rect.left + 4f, rect.top + 4f, rect.right - 4f, rect.bottom - 4f), r, r, sketchP)

      // Hand-drawn wavy squiggle under time
      val sqY = rect.centerY() + minD * 0.16f
      val sqPath = Path().apply {
        moveTo(rect.centerX() - w * 0.24f, sqY)
        val segs = 6
        val segW = (w * 0.48f) / segs
        for (i in 0 until segs) {
          val x1 = rect.centerX() - w * 0.24f + i * segW + segW * 0.5f
          val y1 = sqY + if (i % 2 == 0) -6f else 6f
          val x2 = rect.centerX() - w * 0.24f + (i + 1) * segW
          quadTo(x1, y1, x2, sqY)
        }
      }
      canvas.drawPath(
        sqPath,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = 3f
          strokeCap = Paint.Cap.ROUND
          color = config.accentColor
        },
      )

      // Little doodle star corner
      val sx = rect.right - minD * 0.16f
      val sy = rect.top + minD * 0.18f
      val sr = (minD * 0.06f).coerceIn(5f, 11f)
      val starStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.accentColor
        strokeWidth = 2.5f
        strokeCap = Paint.Cap.ROUND
      }
      canvas.drawLine(sx - sr, sy, sx + sr, sy, starStroke)
      canvas.drawLine(sx, sy - sr, sx, sy + sr, starStroke)
    }

    canvas.save()
    canvas.rotate(-2.2f, rect.centerX(), rect.centerY())
    drawAlignedTime(canvas, rect, rect.centerY() + minD * 0.04f, config, typeface, cal, w * 0.84f, h * 0.52f, minD * 0.50f * config.fontScale)
    canvas.restore()

    if (config.showDate) {
      val mp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.secondaryTextColor
        textAlign = Paint.Align.CENTER
        textSize = (minD * 0.115f).coerceIn(11f, 20f)
      }
      val label = buildString {
        append(fmtDateWithFormat(cal, config.dateFormat))
        if (config.showWeather && weather.isLoaded) append(" • ${weather.displayTemp(config.useFahrenheit)}")
      }
      canvas.drawText(label, rect.centerX(), rect.bottom - 10f, mp)
    }
  }

  // ---------------------------------------------------------------------------
  // 6. CLOUD DREAMER (Soft layered drifting clouds + pastel twilight typography)
  // ---------------------------------------------------------------------------
  fun drawCloudDreamer(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
    weather: WeatherSnapshot,
    animPhase: Float,
  ) {
    val w = rect.width()
    val h = rect.height()
    val minD = min(w, h)
    val drift = sin(animPhase * 2.0 * PI).toFloat() * (w * 0.04f)

    if (config.showDecorations) {
      val cloudP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(config.accentColor, 48)
      }
      drawFluffyCloud(canvas, rect.left + w * 0.24f + drift, rect.bottom - h * 0.16f, minD * 0.28f, cloudP)
      cloudP.color = ColorUtils.setAlphaComponent(config.foregroundColor, 34)
      drawFluffyCloud(canvas, rect.right - w * 0.22f - drift, rect.top + h * 0.24f, minD * 0.24f, cloudP)
    }

    drawAlignedTime(canvas, rect, rect.centerY() + minD * 0.08f, config, typeface, cal, w * 0.88f, h * 0.52f, minD * 0.48f * config.fontScale)

    val (ax, align) = alignX(rect, config.alignment, 12f)
    val mp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.secondaryTextColor
      textAlign = align
      textSize = (minD * 0.095f).coerceIn(10f, 16f)
    }
    val sub = buildString {
      if (config.showDate) append(fmtDateWithFormat(cal, config.dateFormat))
      if (config.showWeather && weather.isLoaded) {
        if (isNotEmpty()) append("  •  ")
        append(weather.displayTemp(config.useFahrenheit))
      }
    }
    if (sub.isNotEmpty()) canvas.drawText(sub, ax, rect.bottom - 8f, mp)
  }

  private fun drawFluffyCloud(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
    canvas.drawCircle(cx - r * 0.42f, cy + r * 0.12f, r * 0.40f, paint)
    canvas.drawCircle(cx + r * 0.42f, cy + r * 0.14f, r * 0.36f, paint)
    canvas.drawCircle(cx, cy - r * 0.12f, r * 0.50f, paint)
    canvas.drawRoundRect(RectF(cx - r * 0.82f, cy, cx + r * 0.78f, cy + r * 0.48f), r * 0.24f, r * 0.24f, paint)
  }

  // ---------------------------------------------------------------------------
  // 7. NATURE BOTANICAL (Alpine mountain ridge silhouette + rising sun + serif)
  // ---------------------------------------------------------------------------
  fun drawNatureBotanical(
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

    if (config.showDecorations) {
      // Glowing sun behind horizon ridge
      val sunY = rect.bottom - h * 0.26f
      val sunR = (minD * 0.14f).coerceIn(10f, 24f)
      canvas.drawCircle(rect.centerX(), sunY, sunR, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = config.accentColor })

      // Layered mountain ridge silhouette at bottom
      val ridge = Path().apply {
        moveTo(rect.left, rect.bottom)
        lineTo(rect.left, rect.bottom - h * 0.18f)
        lineTo(rect.left + w * 0.22f, rect.bottom - h * 0.30f)
        lineTo(rect.left + w * 0.45f, rect.bottom - h * 0.16f)
        lineTo(rect.left + w * 0.72f, rect.bottom - h * 0.32f)
        lineTo(rect.right, rect.bottom - h * 0.15f)
        lineTo(rect.right, rect.bottom)
        close()
      }
      canvas.drawPath(
        ridge,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = ColorUtils.setAlphaComponent(config.backgroundSecondaryColor, 200)
        },
      )
    }

    drawAlignedTime(canvas, rect, rect.centerY() - minD * 0.02f, config, typeface, cal, w * 0.86f, h * 0.48f, minD * 0.46f * config.fontScale)

    val (ax, align) = alignX(rect, config.alignment, 10f)
    val mp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.secondaryTextColor
      textAlign = align
      textSize = (minD * 0.09f).coerceIn(10f, 16f)
    }
    if (config.showDate) {
      canvas.drawText(fmtDateWithFormat(cal, config.dateFormat), ax, rect.top + mp.textSize * 1.4f, mp)
    }
    if (config.showWeather && weather.isLoaded) {
      mp.color = config.foregroundColor
      canvas.drawText("${weather.displayTemp(config.useFahrenheit)} • ${weather.conditionText()}", ax, rect.bottom - 8f, mp)
    }
  }

  // ---------------------------------------------------------------------------
  // 8. LIQUID CHROME WAVE (Sculptural fluid blob + high-contrast display digits)
  // ---------------------------------------------------------------------------
  fun drawLiquidChromeWave(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
    weather: WeatherSnapshot,
    animPhase: Float,
  ) {
    val w = rect.width()
    val h = rect.height()
    val minD = min(w, h)
    val twoPi = animPhase * 2.0 * PI

    if (config.showDecorations) {
      val blobPath = Path()
      val steps = 36
      val rx = w * 0.38f
      val ry = h * 0.34f
      for (i in 0..steps) {
        val a = (i / steps.toDouble()) * 2.0 * PI
        val wobble = 1.0f + 0.08f * sin(a * 3.0 + twoPi).toFloat()
        val px = rect.centerX() + (cos(a) * rx * wobble).toFloat()
        val py = rect.centerY() + (sin(a) * ry * wobble).toFloat()
        if (i == 0) blobPath.moveTo(px, py) else blobPath.lineTo(px, py)
      }
      blobPath.close()

      val chromePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (minD * 0.025f).coerceIn(2f, 5f)
        shader = LinearGradient(
          rect.left, rect.top, rect.right, rect.bottom,
          config.accentColor, ColorUtils.setAlphaComponent(config.foregroundColor, 85), Shader.TileMode.CLAMP,
        )
      }
      canvas.drawPath(blobPath, chromePaint)
    }

    drawAlignedTime(canvas, rect, rect.centerY() + minD * 0.12f, config, typeface, cal, w * 0.84f, h * 0.52f, minD * 0.48f * config.fontScale)
    if (config.showDate) {
      val mp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.secondaryTextColor
        textAlign = Paint.Align.CENTER
        textSize = (minD * 0.09f).coerceIn(9f, 15f)
        letterSpacing = 0.10f
      }
      val text = fmtDateWithFormat(cal, config.dateFormat).uppercase(Locale.getDefault()) +
        if (config.showWeather && weather.isLoaded) "  //  ${weather.displayTemp(config.useFahrenheit)}" else ""
      canvas.drawText(text, rect.centerX(), rect.bottom - 8f, mp)
    }
  }

  // ---------------------------------------------------------------------------
  // 9. COUNTDOWN / DAY PROGRESS (Day progress ring & telemetry bar)
  // ---------------------------------------------------------------------------
  fun drawCountdownProgress(
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
    val minsPassed = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val dayFrac = (minsPassed / 1440f).coerceIn(0.01f, 1f)
    val hoursLeft = ((1440 - minsPassed) / 60f)

    val topLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.accentColor
      textSize = (minD * 0.09f).coerceIn(10f, 15f)
      letterSpacing = 0.08f
    }
    canvas.drawText("DAY ${(dayFrac * 100).roundToInt()}%  •  ${String.format(Locale.US, "%.1fh LEFT", hoursLeft)}", rect.left + 8f, rect.top + topLabel.textSize * 1.4f, topLabel)

    drawAlignedTime(canvas, rect, rect.centerY() + minD * 0.08f, config, typeface, cal, w * 0.86f, h * 0.46f, minD * 0.44f * config.fontScale)

    // Day completion bar
    val barTop = rect.bottom - minD * 0.16f
    val barRect = RectF(rect.left + 8f, barTop, rect.right - 8f, barTop + (minD * 0.045f).coerceIn(5f, 10f))
    val trackP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ColorUtils.setAlphaComponent(config.secondaryTextColor, 55) }
    val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = config.accentColor }
    canvas.drawRoundRect(barRect, barRect.height() / 2f, barRect.height() / 2f, trackP)
    val fillW = barRect.width() * dayFrac
    canvas.drawRoundRect(RectF(barRect.left, barRect.top, barRect.left + fillW, barRect.bottom), barRect.height() / 2f, barRect.height() / 2f, fillP)

    if (config.showDate) {
      val dp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.secondaryTextColor
        textSize = (minD * 0.085f).coerceIn(9f, 14f)
      }
      val sub = buildString {
        append(fmtDateWithFormat(cal, config.dateFormat))
        if (config.showWeather && weather.isLoaded) append(" • ${weather.displayTemp(config.useFahrenheit)}")
      }
      canvas.drawText(sub, rect.left + 8f, rect.bottom - 2f, dp)
    }
  }

  // ---------------------------------------------------------------------------
  // 10. EXPERIMENTAL KINETIC SPLIT (Diagonal slash, staggered HH / MM, sweep dot)
  // ---------------------------------------------------------------------------
  fun drawExperimentalKinetic(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
    weather: WeatherSnapshot,
    animPhase: Float,
  ) {
    val w = rect.width()
    val h = rect.height()
    val minD = min(w, h)
    val hh = fmtHour(cal, config.is24Hour)
    val mm = fmtMin(cal)

    // Diagonal kinetic slash
    val slashP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = config.accentColor
      strokeWidth = (minD * 0.025f).coerceIn(2.5f, 5f)
      strokeCap = Paint.Cap.ROUND
    }
    val x0 = rect.centerX() - w * 0.07f
    val x1 = rect.centerX() + w * 0.07f
    canvas.drawLine(x1, rect.top + h * 0.18f, x0, rect.bottom - h * 0.22f, slashP)

    val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.hourColor ?: config.foregroundColor
      isFakeBoldText = true
      textAlign = Paint.Align.RIGHT
      applyDepthShadow(this, config)
    }
    val mp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.minuteColor ?: config.secondaryTextColor
      textAlign = Paint.Align.LEFT
      applyDepthShadow(this, config)
    }
    val sz = fitText(hp, "88", w * 0.36f, h * 0.44f, minD * 0.42f * config.fontScale)
    hp.textSize = sz
    mp.textSize = sz

    canvas.drawText(hh, rect.centerX() - minD * 0.06f, rect.centerY() - minD * 0.02f, hp)
    canvas.drawText(mm, rect.centerX() + minD * 0.06f, rect.centerY() + sz * 0.56f, mp)

    if (config.showDecorations) {
      val t = 0.5f + 0.5f * sin(animPhase * 2.0 * PI).toFloat()
      val dx = x1 + (x0 - x1) * t
      val dy = (rect.top + h * 0.18f) + ((rect.bottom - h * 0.22f) - (rect.top + h * 0.18f)) * t
      canvas.drawCircle(dx, dy, (minD * 0.032f).coerceIn(4f, 8f), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = config.foregroundColor })
    }

    if (config.showDate) {
      val fp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.secondaryTextColor
        textAlign = Paint.Align.CENTER
        textSize = (minD * 0.088f).coerceIn(9f, 15f)
      }
      val txt = buildString {
        append(fmtDateWithFormat(cal, config.dateFormat))
        if (config.showWeather && weather.isLoaded) append("  /  ${weather.displayTemp(config.useFahrenheit)}")
      }
      canvas.drawText(txt, rect.centerX(), rect.bottom - 4f, fp)
    }
  }

  // ---------------------------------------------------------------------------
  // 11. MOOD DAYLIGHT / SOLAR SHIFT (Dynamic Dawn/Day/Sunset/Midnight solar arc)
  // ---------------------------------------------------------------------------
  fun drawMoodDaylight(
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
    val hour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f
    val phaseLabel = when {
      hour in 5f..8.5f -> "SUNRISE"
      hour in 8.5f..16.5f -> "DAYLIGHT"
      hour in 16.5f..20.5f -> "GOLDEN HOUR"
      else -> "MIDNIGHT"
    }

    if (config.showDecorations) {
      val arcR = min(w * 0.36f, h * 0.56f)
      val arcCx = rect.centerX()
      val arcCy = rect.bottom - h * 0.20f
      val arcP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
        color = ColorUtils.setAlphaComponent(config.accentColor, 125)
      }
      canvas.drawArc(RectF(arcCx - arcR, arcCy - arcR, arcCx + arcR, arcCy + arcR), 180f, 180f, false, arcP)
      val sunProg = (hour / 24f).coerceIn(0f, 1f)
      val angle = Math.toRadians((180f + sunProg * 180f).toDouble())
      val ox = arcCx + (cos(angle) * arcR).toFloat()
      val oy = arcCy + (sin(angle) * arcR).toFloat()
      canvas.drawCircle(ox, oy, (minD * 0.045f).coerceIn(5f, 10f), Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.accentColor
        setShadowLayer(10f, 0f, 0f, config.accentColor)
      })
    }

    drawAlignedTime(canvas, rect, rect.centerY() + minD * 0.08f, config, typeface, cal, w * 0.86f, h * 0.50f, minD * 0.48f * config.fontScale)

    val topP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.accentColor
      textAlign = Paint.Align.CENTER
      letterSpacing = 0.14f
      textSize = (minD * 0.085f).coerceIn(9f, 15f)
    }
    canvas.drawText(phaseLabel, rect.centerX(), rect.top + topP.textSize * 1.4f, topP)

    if (config.showDate) {
      val botP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.secondaryTextColor
        textAlign = Paint.Align.CENTER
        textSize = (minD * 0.09f).coerceIn(9f, 16f)
      }
      val sub = buildString {
        append(fmtDateWithFormat(cal, config.dateFormat))
        if (config.showWeather && weather.isLoaded) append(" • ${weather.displayTemp(config.useFahrenheit)}")
      }
      canvas.drawText(sub, rect.centerX(), rect.bottom - 6f, botP)
    }
  }

  // ---------------------------------------------------------------------------
  // 12. CUSTOM COMPOSER (Clock Builder modular composition)
  // ---------------------------------------------------------------------------
  fun drawCustomComposer(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
    weather: WeatherSnapshot,
    animPhase: Float,
  ) {
    val spec = config.customBuilderSpec
    val w = rect.width()
    val h = rect.height()
    val minD = min(w, h)

    // Render selected decorative motif
    if (config.showDecorations) {
      when (spec.motif) {
        BuilderMotif.NONE -> Unit
        BuilderMotif.STARS -> drawStarfieldConstellation(canvas, rect, config.copy(showDate = false, showWeather = false, foregroundColor = Color.TRANSPARENT, hourColor = Color.TRANSPARENT, minuteColor = Color.TRANSPARENT), typeface, cal, weather, animPhase)
        BuilderMotif.ORBIT -> {
          val rx = w * 0.44f
          val ry = h * 0.40f
          val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
            color = ColorUtils.setAlphaComponent(config.accentColor, 95)
          }
          canvas.drawOval(RectF(rect.centerX() - rx, rect.centerY() - ry, rect.centerX() + rx, rect.centerY() + ry), p)
        }
        BuilderMotif.HORIZON -> {
          val ly = rect.bottom - minD * 0.16f
          canvas.drawLine(rect.left + w * 0.15f, ly, rect.right - w * 0.15f, ly, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.accentColor
            strokeWidth = 2.5f
            strokeCap = Paint.Cap.ROUND
          })
        }
        BuilderMotif.DOODLE -> {
          val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.4f
            color = config.accentColor
            pathEffect = DashPathEffect(floatArrayOf(14f, 7f), 0f)
          }
          canvas.drawRoundRect(RectF(rect.left + 4f, rect.top + 4f, rect.right - 4f, rect.bottom - 4f), 20f, 20f, p)
        }
        BuilderMotif.CORNER_HUD -> {
          val bl = (minD * 0.14f).coerceIn(10f, 22f)
          val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.accentColor
            strokeWidth = 3f
            strokeCap = Paint.Cap.SQUARE
          }
          canvas.drawLine(rect.left, rect.top + bl, rect.left, rect.top, bp)
          canvas.drawLine(rect.left, rect.top, rect.left + bl, rect.top, bp)
          canvas.drawLine(rect.right - bl, rect.bottom, rect.right, rect.bottom, bp)
          canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - bl, bp)
        }
      }
    }

    val effectiveCfg = if (spec.splitDigitColors) {
      config.copy(minuteColor = config.minuteColor ?: config.accentColor)
    } else {
      config
    }

    when (spec.layoutType) {
      BuilderLayoutType.CENTERED_HERO -> {
        drawAlignedTime(canvas, rect, rect.centerY() + minD * 0.08f, effectiveCfg, typeface, cal, w * 0.86f, h * 0.52f, minD * 0.48f * config.fontScale)
        if (config.showDate) {
          val (ax, align) = alignX(rect, config.alignment, 12f)
          val dp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = config.secondaryTextColor
            textAlign = align
            textSize = (minD * 0.095f).coerceIn(10f, 16f)
          }
          canvas.drawText(fmtDateWithFormat(cal, config.dateFormat), ax, rect.bottom - 6f, dp)
        }
      }
      BuilderLayoutType.STACKED_DUO -> {
        val hh = fmtHour(cal, config.is24Hour)
        val mm = fmtMin(cal)
        val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          this.typeface = typeface
          color = effectiveCfg.hourColor ?: effectiveCfg.foregroundColor
          isFakeBoldText = true
          textAlign = Paint.Align.CENTER
          applyDepthShadow(this, effectiveCfg)
        }
        val mp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          this.typeface = typeface
          color = effectiveCfg.minuteColor ?: effectiveCfg.accentColor
          textAlign = Paint.Align.CENTER
          applyDepthShadow(this, effectiveCfg)
        }
        val sz = fitText(hp, "88", w * 0.80f, h * 0.36f, minD * 0.42f * config.fontScale)
        hp.textSize = sz
        mp.textSize = sz
        canvas.drawText(hh, rect.centerX(), rect.centerY() - sz * 0.12f, hp)
        canvas.drawText(mm, rect.centerX(), rect.centerY() + sz * 0.72f, mp)
      }
      BuilderLayoutType.SPLIT_SPINE -> {
        val hh = fmtHour(cal, config.is24Hour)
        val mm = fmtMin(cal)
        val timeStr = "$hh:$mm"
        val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          this.typeface = typeface
          color = effectiveCfg.foregroundColor
          isFakeBoldText = true
          applyDepthShadow(this, effectiveCfg)
        }
        hp.textSize = fitText(hp, timeStr, w * 0.52f, h * 0.65f, minD * 0.46f * config.fontScale)
        val fm = hp.fontMetrics
        val by = rect.centerY() - (fm.ascent + fm.descent) / 2f
        canvas.drawText(timeStr, rect.left + 8f, by, hp)
        val divX = rect.left + w * 0.58f
        canvas.drawLine(divX, rect.top + h * 0.2f, divX, rect.bottom - h * 0.2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = config.accentColor
          strokeWidth = 3f
          strokeCap = Paint.Cap.ROUND
        })
        val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          this.typeface = typeface
          color = config.secondaryTextColor
          textSize = (minD * 0.105f).coerceIn(10f, 16f)
        }
        if (config.showDate) canvas.drawText(fmtDateWithFormat(cal, config.dateFormat), divX + 12f, rect.centerY() - 6f, sp)
        if (config.showWeather && weather.isLoaded) {
          sp.color = config.accentColor
          canvas.drawText(weather.displayTemp(config.useFahrenheit), divX + 12f, rect.centerY() + sp.textSize * 1.4f, sp)
        }
      }
      BuilderLayoutType.RING_DIGITAL -> {
        val r = minD * 0.44f
        if (spec.showRing) {
          val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (r * 0.12f).coerceIn(4f, 10f)
            color = ColorUtils.setAlphaComponent(config.secondaryTextColor, 55)
          }
          val prog = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = track.strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = config.accentColor
          }
          val arcR = RectF(rect.centerX() - r, rect.centerY() - r, rect.centerX() + r, rect.centerY() + r)
          canvas.drawCircle(rect.centerX(), rect.centerY(), r, track)
          val sweep = ((cal.get(Calendar.MINUTE) / 60f) * 360f).coerceAtLeast(15f)
          canvas.drawArc(arcR, -90f, sweep, false, prog)
        }
        drawAlignedTime(canvas, rect, rect.centerY() + minD * 0.08f, effectiveCfg.copy(alignment = ClockAlignment.CENTER), typeface, cal, r * 1.45f, r * 0.78f, r * 0.56f * config.fontScale)
      }
      BuilderLayoutType.ANALOG_HYBRID -> {
        val r = min(w * 0.24f, h * 0.40f)
        val cx = rect.left + r + 10f
        val cy = rect.centerY()
        val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = 2.5f
          color = config.accentColor
        }
        canvas.drawCircle(cx, cy, r, rim)
        val minVal = cal.get(Calendar.MINUTE).toFloat()
        val hrVal = (cal.get(Calendar.HOUR) % 12) + minVal / 60f
        val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = config.foregroundColor
          strokeWidth = 3f
          strokeCap = Paint.Cap.ROUND
        }
        val hrA = Math.toRadians((hrVal * 30f - 90f).toDouble())
        val minA = Math.toRadians((minVal * 6f - 90f).toDouble())
        canvas.drawLine(cx, cy, cx + (cos(hrA) * r * 0.52f).toFloat(), cy + (sin(hrA) * r * 0.52f).toFloat(), hp)
        hp.strokeWidth = 2f
        canvas.drawLine(cx, cy, cx + (cos(minA) * r * 0.78f).toFloat(), cy + (sin(minA) * r * 0.78f).toFloat(), hp)

        val rightRect = RectF(cx + r + 12f, rect.top, rect.right, rect.bottom)
        drawAlignedTime(canvas, rightRect, rect.centerY() + minD * 0.06f, effectiveCfg.copy(alignment = ClockAlignment.START), typeface, cal, rightRect.width() * 0.92f, h * 0.48f, minD * 0.40f * config.fontScale)
      }
    }
  }
}
