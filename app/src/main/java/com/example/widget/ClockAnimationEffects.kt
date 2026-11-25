package com.example.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.ColorUtils
import com.example.model.ClockAnimationStyle
import com.example.model.ClockStyleId
import com.example.model.DisplayMode
import com.example.model.SecondsStyle
import com.example.model.WidgetConfig
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal object ClockAnimationEffects {

  /**
   * Resolves time-of-day mood transitions (Dawn, Daylight, Golden Sunset, Midnight)
   * when timeBasedStyleEnabled is active or MOOD_DAYLIGHT is selected.
   */
  fun applyTimeBasedMood(config: WidgetConfig, cal: Calendar): WidgetConfig {
    if (!config.timeBasedStyleEnabled && config.styleId != ClockStyleId.MOOD_DAYLIGHT) return config
    val hour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f

    val (topBg, botBg, accent, fg, sec) = when {
      hour in 5.0f..8.5f -> {
        // Sunrise Dawn
        MoodPalette(0xFF1E1B4B.toInt(), 0xFFF97316.toInt(), 0xFFFDE047.toInt(), 0xFFFFFFFF.toInt(), 0xFFFED7AA.toInt())
      }
      hour in 8.5f..16.5f -> {
        // Crisp Daylight Sky
        MoodPalette(0xFF0284C7.toInt(), 0xFF38BDF8.toInt(), 0xFFFEF08A.toInt(), 0xFFFFFFFF.toInt(), 0xFFE0F2FE.toInt())
      }
      hour in 16.5f..20.5f -> {
        // Sunset Ember & Violet
        MoodPalette(0xFF311042.toInt(), 0xFFE11D48.toInt(), 0xFFFBBF24.toInt(), 0xFFFFFBF2.toInt(), 0xFFFECDD3.toInt())
      }
      else -> {
        // Deep Midnight Aurora
        MoodPalette(0xFF050814.toInt(), 0xFF1E1B4B.toInt(), 0xFF38BDF8.toInt(), 0xFFF8FAFC.toInt(), 0xFF94A3B8.toInt())
      }
    }

    return config.copy(
      backgroundColor = topBg,
      backgroundSecondaryColor = botBg,
      accentColor = accent,
      foregroundColor = fg,
      secondaryTextColor = sec,
    )
  }

  private data class MoodPalette(
    val bgTop: Int,
    val bgBot: Int,
    val accent: Int,
    val fg: Int,
    val sec: Int,
  )

  /**
   * Lightweight, battery-conscious animation overlay rendered only when animationsEnabled == true
   * and displayMode != AOD.
   */
  fun drawAnimationLayer(
    canvas: Canvas,
    width: Float,
    height: Float,
    cornerPx: Float,
    config: WidgetConfig,
    mode: DisplayMode,
    animPhase: Float,
    cal: Calendar,
  ) {
    if (mode == DisplayMode.AOD || !config.animationsEnabled || config.animationStyle == ClockAnimationStyle.NONE) {
      return
    }
    val minD = min(width, height)
    val phase = animPhase % 1f
    val twoPi = (phase * 2.0 * PI)
    val pulse = 0.5f + 0.5f * sin(twoPi).toFloat()
    val glowAmt = config.glowIntensity.coerceIn(0f, 1f)
    val modeFactor = if (mode == DisplayMode.HOME_SCREEN) 1.0f else 0.72f

    when (config.animationStyle) {
      ClockAnimationStyle.NONE -> Unit

      ClockAnimationStyle.GLOW_PULSE -> {
        val alpha = (68f * glowAmt * modeFactor * (0.45f + 0.55f * pulse)).roundToInt().coerceIn(0, 96)
        if (alpha > 2) {
          val cx = width * (0.30f + 0.40f * pulse)
          val cy = height * (0.32f + 0.16f * cos(twoPi).toFloat())
          val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
              cx, cy, minD * 0.85f,
              ColorUtils.setAlphaComponent(config.accentColor, alpha),
              Color.TRANSPARENT,
              Shader.TileMode.CLAMP,
            )
          }
          canvas.drawCircle(cx, cy, minD * 0.85f, p)
        }
      }

      ClockAnimationStyle.BREATHE -> {
        val alpha = (50f * (0.3f + glowAmt * 0.7f) * modeFactor * pulse).roundToInt().coerceIn(0, 72)
        if (alpha > 2) {
          val r = minD * (0.48f + 0.18f * pulse)
          val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
              width * 0.5f, height * 0.5f, r,
              ColorUtils.setAlphaComponent(config.accentColor, alpha),
              Color.TRANSPARENT,
              Shader.TileMode.CLAMP,
            )
          }
          canvas.drawCircle(width * 0.5f, height * 0.5f, r, p)
        }
      }

      ClockAnimationStyle.ORBIT_SPIN -> {
        if (config.showDecorations) {
          val cx = width * 0.5f
          val cy = height * 0.5f
          val rx = width * 0.42f
          val ry = height * 0.36f
          val trackP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = ColorUtils.setAlphaComponent(config.accentColor, (48 * modeFactor).roundToInt())
          }
          canvas.drawOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), trackP)
          val satX = cx + (cos(twoPi) * rx).toFloat()
          val satY = cy + (sin(twoPi) * ry).toFloat()
          val satP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.accentColor
            setShadowLayer(8f * glowAmt, 0f, 0f, config.accentColor)
          }
          canvas.drawCircle(satX, satY, (minD * 0.028f).coerceIn(3.5f, 7f), satP)
        }
      }

      ClockAnimationStyle.WAVE_DRIFT -> {
        val waveP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = ColorUtils.setAlphaComponent(config.accentColor, (34 * modeFactor * (0.4f + glowAmt * 0.6f)).roundToInt())
        }
        val path = Path().apply {
          val baseY = height * 0.70f
          moveTo(0f, height)
          lineTo(0f, baseY)
          val steps = 16
          for (i in 0..steps) {
            val t = i / steps.toFloat()
            val y = baseY + sin(t * PI * 2.5 + twoPi).toFloat() * (height * 0.08f)
            lineTo(t * width, y)
          }
          lineTo(width, height)
          close()
        }
        canvas.save()
        canvas.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, width, height), cornerPx, cornerPx, Path.Direction.CW) })
        canvas.drawPath(path, waveP)
        canvas.restore()
      }

      ClockAnimationStyle.PARTICLE_FLOAT -> {
        if (config.showDecorations) {
          val p = Paint(Paint.ANTI_ALIAS_FLAG)
          for (i in 0 until 8) {
            val fx = ((i * 0.173f + phase * (if (i % 2 == 0) 0.35f else -0.25f)) % 1f + 1f) % 1f
            val fy = ((i * 0.291f + sin(twoPi + i).toFloat() * 0.06f) % 1f + 1f) % 1f
            val alpha = (95f * modeFactor * (0.4f + 0.6f * absSin(phase * PI + i))).roundToInt().coerceIn(20, 140)
            p.color = ColorUtils.setAlphaComponent(if (i % 2 == 0) config.accentColor else config.foregroundColor, alpha)
            canvas.drawCircle(fx * width, fy * height, (minD * (0.012f + (i % 3) * 0.005f)).coerceIn(1.5f, 4.5f), p)
          }
        }
      }

      ClockAnimationStyle.BOUNCE_TICK -> {
        val secFrac = (cal.get(Calendar.MILLISECOND) / 1000f)
        val bounce = kotlin.math.abs(sin((phase * 4.0 + secFrac) * PI)).toFloat()
        val dotR = (minD * (0.022f + 0.012f * bounce)).coerceIn(3f, 7f)
        val dotP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = ColorUtils.setAlphaComponent(config.accentColor, (165 * modeFactor).roundToInt())
        }
        canvas.drawCircle(width - dotR * 3f, dotR * 3f, dotR, dotP)
      }

      ClockAnimationStyle.GRADIENT_SHIFT -> {
        val shiftX = width * phase
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader = LinearGradient(
            shiftX - width * 0.5f, 0f,
            shiftX + width * 0.5f, height,
            intArrayOf(
              Color.TRANSPARENT,
              ColorUtils.setAlphaComponent(config.accentColor, (44 * modeFactor).roundToInt()),
              Color.TRANSPARENT,
            ),
            null,
            Shader.TileMode.CLAMP,
          )
        }
        canvas.save()
        canvas.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, width, height), cornerPx, cornerPx, Path.Direction.CW) })
        canvas.drawRect(0f, 0f, width, height, p)
        canvas.restore()
      }

      ClockAnimationStyle.RING_SWEEP -> {
        val inset = (minD * 0.03f).coerceIn(3f, 8f)
        val arcRect = RectF(inset, inset, width - inset, height - inset)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = (minD * 0.018f).coerceIn(2f, 4.5f)
          strokeCap = Paint.Cap.ROUND
          color = ColorUtils.setAlphaComponent(config.accentColor, (135 * modeFactor * (0.4f + glowAmt * 0.6f)).roundToInt())
        }
        canvas.drawArc(arcRect, phase * 360f - 90f, 95f, false, p)
      }
    }
  }

  private fun absSin(radians: Double): Float = kotlin.math.abs(sin(radians)).toFloat()

  /**
   * Optional global seconds accent (ring / dot / bar) when enabled and not AOD.
   */
  fun drawGlobalSecondsAccent(
    canvas: Canvas,
    rect: RectF,
    cornerPx: Float,
    config: WidgetConfig,
    mode: DisplayMode,
    cal: Calendar,
  ) {
    if (mode == DisplayMode.AOD || !config.showSeconds) return
    val secFrac = (cal.get(Calendar.SECOND) / 59f).coerceIn(0f, 1f)
    val secColor = config.secondsColor ?: config.accentColor
    when (config.secondsStyle) {
      SecondsStyle.NONE, SecondsStyle.DIGITS -> Unit
      SecondsStyle.PROGRESS_BAR -> {
        val barH = 3.5f
        val left = rect.left + rect.width() * 0.14f
        val barW = rect.width() * 0.72f
        val y = rect.bottom + 4f
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = ColorUtils.setAlphaComponent(config.secondaryTextColor, 55)
          strokeWidth = barH
          strokeCap = Paint.Cap.ROUND
        }
        val active = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = secColor
          strokeWidth = barH
          strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(left, y, left + barW, y, track)
        canvas.drawLine(left, y, left + barW * secFrac, y, active)
      }
      SecondsStyle.RING -> {
        val r = (min(rect.width(), rect.height()) * 0.47f)
        val cx = rect.centerX()
        val cy = rect.centerY()
        val arcP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = 3f
          strokeCap = Paint.Cap.ROUND
          color = ColorUtils.setAlphaComponent(secColor, 175)
        }
        canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, secFrac * 360f, false, arcP)
      }
      SecondsStyle.DOT_ORBIT -> {
        val r = (min(rect.width(), rect.height()) * 0.45f)
        val angle = Math.toRadians((secFrac * 360f - 90f).toDouble())
        val dx = rect.centerX() + (cos(angle) * r).toFloat()
        val dy = rect.centerY() + (sin(angle) * r).toFloat()
        val dotP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = secColor
          setShadowLayer(6f, 0f, 0f, secColor)
        }
        canvas.drawCircle(dx, dy, 4.5f, dotP)
      }
    }
  }
}
