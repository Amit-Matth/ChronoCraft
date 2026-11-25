package com.example.widget

import android.content.Context
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
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.example.model.BackgroundFillMode
import com.example.model.CardStyle
import com.example.model.ClockFontId
import com.example.model.ClockStyleId
import com.example.model.DisplayMode
import com.example.model.WeatherSnapshot
import com.example.model.WidgetBackdropId
import com.example.model.WidgetConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

object ClockCanvasRenderer {

  private val typefaceCache = ConcurrentHashMap<ClockFontId, Typeface>()
  private val photoUriCache = ConcurrentHashMap<String, Bitmap>()

  private fun getTypeface(context: Context, fontId: ClockFontId): Typeface {
    return typefaceCache.getOrPut(fontId) {
      runCatching { ResourcesCompat.getFont(context, fontId.fontRes) }.getOrNull()
        ?: Typeface.DEFAULT_BOLD
    }
  }

  private fun loadCustomPhoto(context: Context, uriStr: String): Bitmap? {
    photoUriCache[uriStr]?.let { if (!it.isRecycled) return it }
    return runCatching {
      val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
      context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { stream ->
        BitmapFactory.decodeStream(stream, null, opts)?.also { photoUriCache[uriStr] = it }
      }
    }.getOrNull()
  }

  /**
   * Computes pixel dimensions capped strictly under ~2.2 MB (ARGB_8888) so RemoteViews never
   * exceeds launcher bitmap memory limits even on large foldable or high-DPI screens.
   */
  fun computeSafeBitmapSize(
    widthDp: Int,
    heightDp: Int,
    density: Float,
    maxBytes: Int = 2_200_000,
  ): Pair<Int, Int> {
    val safeWdp = max(64, widthDp)
    val safeHdp = max(48, heightDp)
    val clampedDensity = density.coerceIn(1.0f, 2.5f)
    var wPx = (safeWdp * clampedDensity).roundToInt().coerceAtLeast(120)
    var hPx = (safeHdp * clampedDensity).roundToInt().coerceAtLeast(90)
    val bytes = wPx.toLong() * hPx.toLong() * 4L
    if (bytes > maxBytes) {
      val scale = kotlin.math.sqrt(maxBytes.toDouble() / bytes.toDouble())
      wPx = max(120, (wPx * scale).roundToInt())
      hPx = max(90, (hPx * scale).roundToInt())
    }
    return wPx to hPx
  }

  fun renderToBitmap(
    context: Context,
    config: WidgetConfig,
    widthDp: Int,
    heightDp: Int,
    density: Float,
    timeMillis: Long = System.currentTimeMillis(),
    weather: WeatherSnapshot = WeatherSnapshot(),
    maxBytes: Int = 2_200_000,
    displayMode: DisplayMode = DisplayMode.HOME_SCREEN,
  ): Bitmap {
    val (wPx, hPx) = computeSafeBitmapSize(widthDp, heightDp, density, maxBytes)
    val bitmap = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawClock(context, canvas, wPx.toFloat(), hPx.toFloat(), config, timeMillis, weather, displayMode, 0f)
    return bitmap
  }

  fun drawClock(
    context: Context,
    canvas: Canvas,
    width: Float,
    height: Float,
    config: WidgetConfig,
    timeMillis: Long = System.currentTimeMillis(),
    weather: WeatherSnapshot = WeatherSnapshot(),
    displayMode: DisplayMode = DisplayMode.HOME_SCREEN,
    animPhase: Float = 0f,
  ) {
    val tz = config.timeZoneId?.let { runCatching { TimeZone.getTimeZone(it) }.getOrNull() } ?: TimeZone.getDefault()
    val cal = Calendar.getInstance(tz).apply { timeInMillis = timeMillis }
    val moodConfig = if (displayMode != DisplayMode.AOD) {
      ClockAnimationEffects.applyTimeBasedMood(config, cal)
    } else {
      config
    }
    val resolvedConfig = ClockAdaptiveDisplayRenderer.resolveConfigForMode(moodConfig, displayMode)
    val typeface = getTypeface(context, resolvedConfig.fontId)
    val minDim = min(width, height)
    val cornerPx = ((resolvedConfig.cornerRadiusDp / 160f) * minDim * 1.4f).coerceIn(0f, minDim * 0.48f)
    val fillMode = resolvedConfig.effectiveFillMode(displayMode)

    // Draw background strictly according to user's chosen BackgroundFillMode; NONE leaves zero container artifacts
    when (fillMode) {
      BackgroundFillMode.NONE -> Unit
      BackgroundFillMode.ARTWORK -> {
        if (resolvedConfig.backdropId != WidgetBackdropId.NONE) {
          ClockArtStyles.drawRoundedBackdropArtwork(context, canvas, width, height, cornerPx, resolvedConfig.backdropId, resolvedConfig)
        } else {
          drawCardBase(canvas, width, height, cornerPx, resolvedConfig, fillMode)
        }
      }
      BackgroundFillMode.PHOTO -> {
        val uri = resolvedConfig.customPhotoUri
        val photoBmp = if (!uri.isNullOrBlank()) loadCustomPhoto(context, uri) else null
        if (photoBmp != null) {
          drawPhotoBackdrop(canvas, width, height, cornerPx, photoBmp, resolvedConfig)
        } else {
          drawCardBase(canvas, width, height, cornerPx, resolvedConfig, BackgroundFillMode.GRADIENT)
        }
      }
      BackgroundFillMode.SOLID, BackgroundFillMode.GRADIENT, BackgroundFillMode.GLASS -> {
        if (displayMode != DisplayMode.HOME_SCREEN ||
          (resolvedConfig.styleId != ClockStyleId.BENTO_CALENDAR && resolvedConfig.styleId != ClockStyleId.EXPANDABLE_STACK)
        ) {
          drawCardBase(canvas, width, height, cornerPx, resolvedConfig, fillMode)
        }
      }
    }

    // Battery-conscious animation & decorative effects layer (always off on AOD)
    ClockAnimationEffects.drawAnimationLayer(canvas, width, height, cornerPx, resolvedConfig, displayMode, animPhase, cal)

    // Tight balanced padding when transparent/no background; standard card padding otherwise
    val pad = if (fillMode == BackgroundFillMode.NONE) (minDim * 0.04f).coerceAtLeast(4f) else (minDim * 0.08f).coerceAtLeast(10f)
    val innerRect = RectF(pad, pad, width - pad, height - pad)

    if (displayMode == DisplayMode.AOD) {
      ClockAdaptiveDisplayRenderer.drawAodVariant(
        canvas = canvas,
        rect = innerRect,
        config = resolvedConfig,
        typeface = typeface,
        cal = cal,
        weather = weather,
        batteryPercent = ClockArtStyles.getBatteryPercent(context),
      )
      return
    }

    if (displayMode == DisplayMode.LOCK_SCREEN &&
      ClockAdaptiveDisplayRenderer.drawLockScreenTransformedIfApplicable(canvas, innerRect, resolvedConfig, typeface, cal, weather)
    ) {
      return
    }

    when (resolvedConfig.styleId) {
      ClockStyleId.NEON_FRAME_GLOW -> ClockArtStyles.drawNeonFrameGlow(canvas, width, height, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.CIRCULAR_BATTERY_RING -> ClockArtStyles.drawCircularBatteryRing(canvas, innerRect, resolvedConfig, typeface, cal, weather, ClockArtStyles.getBatteryPercent(context))
      ClockStyleId.HANDWRITTEN_SCRIPT -> ClockArtStyles.drawHandwrittenScript(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.PIXEL_SPACE_RETRO -> ClockArtStyles.drawPixelSpaceRetro(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.SUNSET_SCENIC_ART -> ClockArtStyles.drawSunsetScenicArt(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.DREAMY_NIGHT_ART -> ClockArtStyles.drawDreamyNightArt(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.KAWAII_KITTY_PEEK -> ClockArtStyles.drawKawaiiKittyPeek(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.PLANET_GLOW_ART -> ClockArtStyles.drawPlanetGlowArt(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.LIQUID_GLASS_PRISM -> ClockCreativeStyles.drawLiquidGlassPrism(canvas, innerRect, resolvedConfig, typeface, cal, weather, animPhase)
      ClockStyleId.AURORA_RIBBON_FLOW -> ClockCreativeStyles.drawAuroraRibbonFlow(canvas, innerRect, resolvedConfig, typeface, cal, weather, animPhase)
      ClockStyleId.SOLAR_ORBIT_PLANETS -> ClockCreativeStyles.drawSolarOrbitPlanets(canvas, innerRect, resolvedConfig, typeface, cal, weather, animPhase)
      ClockStyleId.STARFIELD_CONSTELLATION -> ClockCreativeStyles.drawStarfieldConstellation(canvas, innerRect, resolvedConfig, typeface, cal, weather, animPhase)
      ClockStyleId.DOODLE_SKETCH_NOTE -> ClockCreativeStyles.drawDoodleSketchNote(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.CLOUD_DREAMER -> ClockCreativeStyles.drawCloudDreamer(canvas, innerRect, resolvedConfig, typeface, cal, weather, animPhase)
      ClockStyleId.NATURE_BOTANICAL -> ClockCreativeStyles.drawNatureBotanical(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.LIQUID_CHROME_WAVE -> ClockCreativeStyles.drawLiquidChromeWave(canvas, innerRect, resolvedConfig, typeface, cal, weather, animPhase)
      ClockStyleId.COUNTDOWN_PROGRESS -> ClockCreativeStyles.drawCountdownProgress(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.EXPERIMENTAL_KINETIC -> ClockCreativeStyles.drawExperimentalKinetic(canvas, innerRect, resolvedConfig, typeface, cal, weather, animPhase)
      ClockStyleId.MOOD_DAYLIGHT -> ClockCreativeStyles.drawMoodDaylight(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.CUSTOM_COMPOSER -> ClockCreativeStyles.drawCustomComposer(canvas, innerRect, resolvedConfig, typeface, cal, weather, animPhase)
      ClockStyleId.WEIGHT_STACK_FLOAT -> ClockFloatingStyles.drawWeightStackFloat(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.NOTHING_DOT_FLOAT -> ClockFloatingStyles.drawNothingDotFloat(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.HAUTE_DIDONE_FLOAT -> ClockFloatingStyles.drawHauteDidoneFloat(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.HYPER_PILL_FLOAT -> ClockFloatingStyles.drawHyperPillFloat(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.FLOATING_ANALOG_PURE -> ClockFloatingStyles.drawFloatingAnalogPure(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.VERTICAL_SPINE_FLOAT -> ClockFloatingStyles.drawVerticalSpineFloat(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.BENTO_CALENDAR -> ClockAdvancedStyles.drawBentoCalendar(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.DEPTH_GLASS_WEATHER -> ClockAdvancedStyles.drawDepthGlassWeather(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.EXPANDABLE_STACK -> ClockAdvancedStyles.drawExpandableStack(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.WORLD_AGENDA_PRO -> ClockAdvancedStyles.drawWorldAgendaPro(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.CHRONO_3D_DEPTH -> ClockAdvancedStyles.drawChrono3dDepth(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.NEUMORPH_SCULPT -> ClockAdvancedStyles.drawNeumorphSculpt(canvas, innerRect, resolvedConfig, typeface, cal, weather)
      ClockStyleId.CYBER_HUD -> drawCyberHud(canvas, innerRect, resolvedConfig, typeface, cal)
      ClockStyleId.RETRO_FLIP -> drawRetroFlip(canvas, innerRect, resolvedConfig, typeface, cal)
      ClockStyleId.GLASS_PILL -> drawGlassPill(canvas, innerRect, resolvedConfig, typeface, cal)
      ClockStyleId.BRUTALIST_POSTER -> drawBrutalistPoster(canvas, width, height, cornerPx, resolvedConfig.let { innerRect }, resolvedConfig, typeface, cal)
      ClockStyleId.TERMINAL_MONO -> drawTerminalMono(canvas, innerRect, resolvedConfig, typeface, cal)
      ClockStyleId.EDITORIAL_SERIF -> drawEditorialSerif(canvas, innerRect, resolvedConfig, typeface, cal)
      ClockStyleId.ANALOG_SWISS,
      ClockStyleId.ANALOG_CHRONO,
      ClockStyleId.ANALOG_BAUHAUS,
      ClockStyleId.ANALOG_ORBIT,
      ClockStyleId.ANALOG_ROMAN -> drawAnalogSuite(canvas, innerRect, resolvedConfig, typeface, cal)
      ClockStyleId.WORD_MATRIX -> drawWordMatrix(canvas, innerRect, resolvedConfig, typeface, cal)
      ClockStyleId.WORD_STATEMENT -> drawWordStatement(canvas, innerRect, resolvedConfig, typeface, cal)
      ClockStyleId.WORD_DUO_BAR -> drawWordDuoBar(canvas, innerRect, resolvedConfig, typeface, cal)
    }

    ClockAnimationEffects.drawGlobalSecondsAccent(canvas, innerRect, cornerPx, resolvedConfig, displayMode, cal)
  }

  private fun drawPhotoBackdrop(
    canvas: Canvas,
    width: Float,
    height: Float,
    cornerPx: Float,
    bmp: Bitmap,
    config: WidgetConfig,
  ) {
    val rect = RectF(0f, 0f, width, height)
    val clipPath = Path().apply { addRoundRect(rect, cornerPx, cornerPx, Path.Direction.CW) }
    canvas.save()
    canvas.clipPath(clipPath)
    val scale = maxOf(width / bmp.width.toFloat(), height / bmp.height.toFloat())
    val drawW = bmp.width * scale
    val drawH = bmp.height * scale
    val left = (width - drawW) / 2f
    val top = (height - drawH) / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
      alpha = (config.backgroundAlpha.coerceIn(0.25f, 1.0f) * 255).roundToInt()
    }
    canvas.drawBitmap(bmp, null, RectF(left, top, left + drawW, top + drawH), paint)
    val scrim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = LinearGradient(0f, 0f, 0f, height, Color.argb(65, 0, 0, 0), Color.argb(95, 0, 0, 0), Shader.TileMode.CLAMP)
    }
    canvas.drawRect(rect, scrim)
    canvas.restore()
    if (config.showBorder) {
      val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = ColorUtils.setAlphaComponent(config.borderColor, 110)
      }
      canvas.drawRoundRect(RectF(1.5f, 1.5f, width - 1.5f, height - 1.5f), cornerPx, cornerPx, borderPaint)
    }
  }

  private fun drawCardBase(
    canvas: Canvas,
    width: Float,
    height: Float,
    cornerPx: Float,
    config: WidgetConfig,
    fillMode: BackgroundFillMode,
  ) {
    if (fillMode == BackgroundFillMode.NONE || config.cardStyle == CardStyle.TRANSPARENT || config.backgroundAlpha <= 0.02f) return
    if (config.styleId == ClockStyleId.BRUTALIST_POSTER) {
      val offset = (min(width, height) * 0.04f).coerceIn(4f, 12f)
      val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.accentColor
        alpha = (config.backgroundAlpha * 255).roundToInt().coerceIn(0, 255)
      }
      canvas.drawRoundRect(RectF(offset, offset, width, height), cornerPx, cornerPx, shadowPaint)
      val cardRect = RectF(0f, 0f, width - offset, height - offset)
      val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.backgroundColor
        alpha = (config.backgroundAlpha * 255).roundToInt().coerceIn(0, 255)
      }
      canvas.drawRoundRect(cardRect, cornerPx, cornerPx, bgPaint)
      if (config.showBorder) {
        val strokeW = (min(width, height) * 0.02f).coerceIn(2f, 6f)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = strokeW
          color = config.borderColor
        }
        canvas.drawRoundRect(RectF(strokeW / 2f, strokeW / 2f, cardRect.right - strokeW / 2f, cardRect.bottom - strokeW / 2f), cornerPx, cornerPx, borderPaint)
      }
      return
    }

    val rect = RectF(1f, 1f, width - 1f, height - 1f)
    val effectiveAlpha = if (fillMode == BackgroundFillMode.GLASS || config.cardStyle == CardStyle.FROSTED) {
      config.backgroundAlpha.coerceIn(0.18f, 0.56f)
    } else {
      config.backgroundAlpha
    }
    val alphaInt = (effectiveAlpha * 255).roundToInt().coerceIn(0, 255)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      if (fillMode == BackgroundFillMode.GRADIENT) {
        val rad = Math.toRadians(config.gradientAngleDeg.toDouble())
        val dx = (cos(rad) * width * 0.5f).toFloat()
        val dy = (sin(rad) * height * 0.5f).toFloat()
        val topColor = ColorUtils.setAlphaComponent(config.backgroundColor, alphaInt)
        val bottomColor = ColorUtils.setAlphaComponent(config.backgroundSecondaryColor, alphaInt)
        shader = LinearGradient(width * 0.5f - dx, height * 0.5f - dy, width * 0.5f + dx, height * 0.5f + dy, topColor, bottomColor, Shader.TileMode.CLAMP)
      } else {
        val topColor = ColorUtils.setAlphaComponent(config.backgroundColor, alphaInt)
        val bottomColor = ColorUtils.setAlphaComponent(
          ColorUtils.blendARGB(config.backgroundColor, config.accentColor, 0.08f),
          alphaInt,
        )
        shader = LinearGradient(0f, 0f, width, height, topColor, bottomColor, Shader.TileMode.CLAMP)
      }
    }
    canvas.drawRoundRect(rect, cornerPx, cornerPx, bgPaint)

    if (fillMode == BackgroundFillMode.GLASS || config.backgroundAlpha < 0.85f || config.styleId == ClockStyleId.GLASS_PILL) {
      val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, height * 0.55f, Color.argb(46, 255, 255, 255), Color.argb(0, 255, 255, 255), Shader.TileMode.CLAMP)
      }
      canvas.drawRoundRect(rect, cornerPx, cornerPx, sheenPaint)
    }

    if (config.showBorder && alphaInt > 10) {
      val strokeW = (min(width, height) * 0.012f).coerceIn(1.5f, 4f)
      val borderCol = if (config.styleId == ClockStyleId.CYBER_HUD) {
        ColorUtils.setAlphaComponent(config.accentColor, 150)
      } else {
        ColorUtils.setAlphaComponent(config.borderColor, 95)
      }
      val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeW
        color = borderCol
      }
      canvas.drawRoundRect(RectF(strokeW, strokeW, width - strokeW, height - strokeW), cornerPx, cornerPx, borderPaint)
    }
  }

  private fun formattedHours(cal: Calendar, is24Hour: Boolean): String {
    val h = if (is24Hour) {
      cal.get(Calendar.HOUR_OF_DAY)
    } else {
      val h12 = cal.get(Calendar.HOUR)
      if (h12 == 0) 12 else h12
    }
    return String.format(Locale.US, "%02d", h)
  }

  private fun formattedMinutes(cal: Calendar): String =
    String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE))

  private fun formattedSeconds(cal: Calendar): String =
    String.format(Locale.US, "%02d", cal.get(Calendar.SECOND))

  private fun formattedAmPm(cal: Calendar): String =
    if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"

  private fun formattedDate(cal: Calendar): String =
    SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(cal.time).uppercase(Locale.getDefault())

  private fun formattedLongDate(cal: Calendar): String =
    SimpleDateFormat("EEEE • MMMM d", Locale.getDefault()).format(cal.time)

  private fun fitTextSize(
    paint: Paint,
    text: String,
    maxWidth: Float,
    maxHeight: Float,
    initialSize: Float,
  ): Float {
    var size = initialSize
    val bounds = Rect()
    while (size > 8f) {
      paint.textSize = size
      paint.getTextBounds(text, 0, text.length, bounds)
      if (bounds.width() <= maxWidth && bounds.height() <= maxHeight) {
        break
      }
      size *= 0.9f
    }
    return size
  }

  private fun applyFloatShadow(paint: Paint, config: WidgetConfig) {
    if (config.cardStyle == CardStyle.TRANSPARENT || config.backgroundAlpha <= 0.05f) {
      val alpha = (165 * config.depthIntensity.coerceAtLeast(0.45f)).roundToInt().coerceIn(0, 220)
      paint.setShadowLayer(10f, 0f, 3f, Color.argb(alpha, 0, 0, 0))
    }
  }

  // ---------------------------------------------------------------------------
  // 1. NEO CYBER HUD
  // ---------------------------------------------------------------------------
  private fun drawCyberHud(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
  ) {
    val w = rect.width()
    val h = rect.height()
    val minD = min(w, h)
    val isCompact = h < w * 0.42f

    // Corner brackets
    val bracketLen = (minD * 0.14f).coerceIn(8f, 24f)
    val bracketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = (minD * 0.018f).coerceIn(2f, 4.5f)
      color = config.accentColor
      strokeCap = Paint.Cap.SQUARE
    }
    // Top-left & bottom-right brackets
    canvas.drawLine(rect.left, rect.top + bracketLen, rect.left, rect.top, bracketPaint)
    canvas.drawLine(rect.left, rect.top, rect.left + bracketLen, rect.top, bracketPaint)
    canvas.drawLine(rect.right - bracketLen, rect.bottom, rect.right, rect.bottom, bracketPaint)
    canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - bracketLen, bracketPaint)

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.accentColor
      letterSpacing = 0.14f
      textSize = (minD * 0.095f).coerceIn(10f, 24f)
    }
    val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.secondaryTextColor
      textSize = (minD * 0.095f).coerceIn(10f, 24f)
    }

    val topText = config.customLabel.ifBlank { "CHRONO // HUD" }.uppercase(Locale.getDefault())
    if (!isCompact) {
      canvas.drawText(topText, rect.left + bracketLen * 0.5f, rect.top + labelPaint.textSize * 1.15f, labelPaint)
      if (!config.is24Hour) {
        val amPm = formattedAmPm(cal)
        val amW = secondaryPaint.measureText(amPm)
        canvas.drawText(amPm, rect.right - amW - 4f, rect.top + labelPaint.textSize * 1.15f, secondaryPaint)
      }
    }

    val timeStr = buildString {
      append(formattedHours(cal, config.is24Hour))
      append(":")
      append(formattedMinutes(cal))
      if (config.showSeconds && w > h * 1.1f) {
        append(":")
        append(formattedSeconds(cal))
      }
    }

    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      textAlign = Paint.Align.CENTER
      applyFloatShadow(this, config)
    }
    val targetH = if (isCompact) h * 0.66f else h * 0.48f
    val targetSize = fitTextSize(
      timePaint,
      timeStr,
      w * 0.90f,
      targetH,
      minD * 0.52f * config.fontScale,
    )
    timePaint.textSize = targetSize

    val fm = timePaint.fontMetrics
    val centerY = rect.centerY() - (fm.ascent + fm.descent) / 2f - (if (config.showDate && !isCompact) h * 0.03f else 0f)
    canvas.drawText(timeStr, rect.centerX(), centerY, timePaint)

    // Seconds progress bar + date
    val bottomY = rect.bottom - minD * 0.04f
    if (config.showSeconds) {
      val secFrac = cal.get(Calendar.SECOND) / 59f
      val barY = bottomY - (if (config.showDate && !isCompact) minD * 0.14f else minD * 0.04f)
      val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(config.secondaryTextColor, 65)
        strokeWidth = (minD * 0.025f).coerceIn(2f, 5f)
        strokeCap = Paint.Cap.ROUND
      }
      val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.accentColor
        strokeWidth = trackPaint.strokeWidth
        strokeCap = Paint.Cap.ROUND
      }
      val barLeft = rect.left + bracketLen * 0.4f
      val barRight = rect.right - bracketLen * 0.4f
      canvas.drawLine(barLeft, barY, barRight, barY, trackPaint)
      canvas.drawLine(barLeft, barY, barLeft + (barRight - barLeft) * secFrac, barY, activePaint)
    }

    if (config.showDate) {
      val dateText = formattedDate(cal)
      secondaryPaint.textAlign = if (isCompact) Paint.Align.RIGHT else Paint.Align.CENTER
      val dateX = if (isCompact) rect.right - 6f else rect.centerX()
      val dateY = if (isCompact) rect.top + secondaryPaint.textSize else bottomY
      canvas.drawText(dateText, dateX, dateY, secondaryPaint)
    }
  }

  // ---------------------------------------------------------------------------
  // 2. RETRO FLIP CARD
  // ---------------------------------------------------------------------------
  private fun drawRetroFlip(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
  ) {
    val w = rect.width()
    val h = rect.height()
    val minD = min(w, h)
    val showBottomRow = config.showDate || config.customLabel.isNotBlank()
    val tilesBottom = if (showBottomRow && h > w * 0.42f) rect.bottom - minD * 0.16f else rect.bottom
    val tilesRect = RectF(rect.left, rect.top, rect.right, tilesBottom)

    val gap = (minD * 0.055f).coerceIn(4f, 14f)
    val tileW = (tilesRect.width() - gap) / 2f
    val leftTile = RectF(tilesRect.left, tilesRect.top, tilesRect.left + tileW, tilesRect.bottom)
    val rightTile = RectF(leftTile.right + gap, tilesRect.top, tilesRect.right, tilesRect.bottom)

    if (config.cardStyle != CardStyle.TRANSPARENT && config.backgroundAlpha > 0.02f) {
      val tileColor = ColorUtils.blendARGB(config.backgroundColor, config.foregroundColor, 0.08f)
      val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tileColor }
      val tileRadius = (minD * 0.10f).coerceIn(6f, 20f)
      canvas.drawRoundRect(leftTile, tileRadius, tileRadius, tilePaint)
      canvas.drawRoundRect(rightTile, tileRadius, tileRadius, tilePaint)
    }

    val tileRadius = (minD * 0.10f).coerceIn(6f, 20f)
    val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      textAlign = Paint.Align.CENTER
      applyFloatShadow(this, config)
    }
    val hh = formattedHours(cal, config.is24Hour)
    val mm = formattedMinutes(cal)
    numPaint.textSize = fitTextSize(
      numPaint,
      "88",
      tileW * 0.82f,
      tilesRect.height() * 0.78f,
      min(tileW, tilesRect.height()) * 0.82f * config.fontScale,
    )
    val fm = numPaint.fontMetrics
    val numY = tilesRect.centerY() - (fm.ascent + fm.descent) / 2f
    canvas.drawText(hh, leftTile.centerX(), numY, numPaint)
    canvas.drawText(mm, rightTile.centerX(), numY, numPaint)

    // Center mechanical hinge line across both tiles
    val hingePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = ColorUtils.setAlphaComponent(config.backgroundColor, 220)
      strokeWidth = (minD * 0.018f).coerceIn(1.5f, 4f)
    }
    canvas.drawLine(leftTile.left, leftTile.centerY(), leftTile.right, leftTile.centerY(), hingePaint)
    canvas.drawLine(rightTile.left, rightTile.centerY(), rightTile.right, rightTile.centerY(), hingePaint)

    // AM/PM badge on top-left of left tile
    if (!config.is24Hour) {
      val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.accentColor
        textSize = (minD * 0.09f).coerceIn(9f, 18f)
      }
      canvas.drawText(
        formattedAmPm(cal),
        leftTile.left + tileRadius * 0.6f,
        leftTile.top + badgePaint.textSize * 1.2f,
        badgePaint,
      )
    }

    if (config.showSeconds) {
      val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.accentColor
        textAlign = Paint.Align.RIGHT
        textSize = (minD * 0.095f).coerceIn(9f, 19f)
      }
      canvas.drawText(
        formattedSeconds(cal),
        rightTile.right - tileRadius * 0.6f,
        rightTile.bottom - tileRadius * 0.5f,
        secPaint,
      )
    }

    if (showBottomRow && h > w * 0.42f) {
      val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.secondaryTextColor
        textSize = (minD * 0.095f).coerceIn(10f, 20f)
        textAlign = Paint.Align.CENTER
      }
      val footerStr = buildString {
        if (config.showDate) append(formattedDate(cal))
        if (config.showDate && config.customLabel.isNotBlank()) append("  •  ")
        if (config.customLabel.isNotBlank()) append(config.customLabel.uppercase(Locale.getDefault()))
      }
      canvas.drawText(footerStr, rect.centerX(), rect.bottom - 2f, footerPaint)
    }
  }

  // ---------------------------------------------------------------------------
  // 3. FROSTED GLASS PILL
  // ---------------------------------------------------------------------------
  private fun drawGlassPill(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
  ) {
    val w = rect.width()
    val h = rect.height()
    val minD = min(w, h)
    val wide = w > h * 1.65f

    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      textAlign = if (wide) Paint.Align.LEFT else Paint.Align.CENTER
      applyFloatShadow(this, config)
    }
    val timeStr = "${formattedHours(cal, config.is24Hour)}:${formattedMinutes(cal)}"
    val maxTimeW = if (wide) w * 0.58f else w * 0.90f
    val maxTimeH = if (wide) h * 0.78f else h * 0.52f
    timePaint.textSize = fitTextSize(
      timePaint,
      timeStr,
      maxTimeW,
      maxTimeH,
      minD * 0.56f * config.fontScale,
    )
    val fm = timePaint.fontMetrics

    if (wide) {
      val timeY = rect.centerY() - (fm.ascent + fm.descent) / 2f
      canvas.drawText(timeStr, rect.left + minD * 0.06f, timeY, timePaint)

      // Right info stack
      val rightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.secondaryTextColor
        textAlign = Paint.Align.RIGHT
        textSize = (minD * 0.13f).coerceIn(10f, 22f)
      }
      if (config.showDate) {
        canvas.drawText(formattedDate(cal), rect.right - 6f, rect.centerY() - 4f, rightPaint)
      }
      if (config.customLabel.isNotBlank() || config.showSeconds) {
        rightPaint.color = config.accentColor
        val sub = buildString {
          if (config.customLabel.isNotBlank()) append(config.customLabel)
          if (config.showSeconds) {
            if (isNotEmpty()) append(" • ")
            append(":${formattedSeconds(cal)}")
          }
        }
        canvas.drawText(sub, rect.right - 6f, rect.centerY() + rightPaint.textSize * 1.35f, rightPaint)
      }
    } else {
      val timeY = rect.centerY() - (fm.ascent + fm.descent) / 2f - (if (config.showDate) h * 0.08f else 0f)
      canvas.drawText(timeStr, rect.centerX(), timeY, timePaint)

      if (config.showDate || config.customLabel.isNotBlank()) {
        val pillText = buildString {
          if (config.showDate) append(formattedDate(cal))
          if (config.showSeconds) {
            if (isNotEmpty()) append(" • ")
            append(formattedSeconds(cal) + "s")
          }
        }.ifBlank { config.customLabel }
        val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          this.typeface = typeface
          color = config.foregroundColor
          textSize = (minD * 0.095f).coerceIn(10f, 20f)
          textAlign = Paint.Align.CENTER
        }
        val tw = pillTextPaint.measureText(pillText)
        val pillPadH = (minD * 0.08f).coerceAtLeast(10f)
        val pillH = pillTextPaint.textSize * 1.8f
        val pillTop = rect.bottom - pillH - minD * 0.04f
        val pillRect = RectF(
          rect.centerX() - tw / 2f - pillPadH,
          pillTop,
          rect.centerX() + tw / 2f + pillPadH,
          pillTop + pillH,
        )
        val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = ColorUtils.setAlphaComponent(config.accentColor, 68)
        }
        canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillBg)
        val pfm = pillTextPaint.fontMetrics
        canvas.drawText(
          pillText,
          pillRect.centerX(),
          pillRect.centerY() - (pfm.ascent + pfm.descent) / 2f,
          pillTextPaint,
        )
      }
    }
  }

  // ---------------------------------------------------------------------------
  // 4. BRUTALIST POSTER
  // ---------------------------------------------------------------------------
  private fun drawBrutalistPoster(
    canvas: Canvas,
    width: Float,
    height: Float,
    cornerPx: Float,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
  ) {
    val w = rect.width()
    val h = rect.height()
    val minD = min(w, h)
    val stacked = h >= w * 0.72f
    val hh = formattedHours(cal, config.is24Hour)
    val mm = formattedMinutes(cal)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      textAlign = Paint.Align.CENTER
    }

    if (stacked) {
      val numSize = fitTextSize(paint, "88", w * 0.85f, h * 0.36f, minD * 0.44f * config.fontScale)
      paint.textSize = numSize
      val fm = paint.fontMetrics
      val lineH = -fm.ascent + fm.descent
      canvas.drawText(hh, rect.centerX() - minD * 0.02f, rect.centerY() - lineH * 0.12f, paint)
      paint.color = ColorUtils.blendARGB(config.foregroundColor, config.accentColor, 0.25f)
      canvas.drawText(mm, rect.centerX() - minD * 0.02f, rect.centerY() + lineH * 0.72f, paint)
    } else {
      val full = "$hh:$mm"
      paint.textSize = fitTextSize(paint, full, w * 0.86f, h * 0.65f, minD * 0.60f * config.fontScale)
      val fm = paint.fontMetrics
      canvas.drawText(full, rect.centerX() - minD * 0.02f, rect.centerY() - (fm.ascent + fm.descent) / 2f, paint)
    }

    if (config.showDate || config.customLabel.isNotBlank()) {
      val tagText = if (config.showDate) formattedDate(cal) else config.customLabel.uppercase(Locale.getDefault())
      val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.backgroundColor
        textSize = (minD * 0.085f).coerceIn(9f, 18f)
      }
      val tw = tagPaint.measureText(tagText)
      val tagRect = RectF(
        rect.left,
        rect.top,
        min(rect.right - 8f, rect.left + tw + 18f),
        rect.top + tagPaint.textSize * 1.65f,
      )
      val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = config.foregroundColor }
      canvas.drawRoundRect(tagRect, cornerPx * 0.3f, cornerPx * 0.3f, bgPaint)
      val tfm = tagPaint.fontMetrics
      canvas.drawText(tagText, tagRect.left + 9f, tagRect.centerY() - (tfm.ascent + tfm.descent) / 2f, tagPaint)
    }
  }

  // ---------------------------------------------------------------------------
  // 5. TERMINAL MATRIX
  // ---------------------------------------------------------------------------
  private fun drawTerminalMono(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
  ) {
    val w = rect.width()
    val h = rect.height()
    val minD = min(w, h)
    val isCompact = h < w * 0.40f

    val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.accentColor
      textSize = (minD * 0.09f).coerceIn(9f, 20f)
    }

    if (!isCompact) {
      val dotR = (minD * 0.025f).coerceIn(2.5f, 5f)
      val dotY = rect.top + dotR * 2f
      val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
      dotPaint.color = config.accentColor
      canvas.drawCircle(rect.left + dotR, dotY, dotR, dotPaint)
      dotPaint.color = ColorUtils.setAlphaComponent(config.accentColor, 140)
      canvas.drawCircle(rect.left + dotR * 4.2f, dotY, dotR, dotPaint)
      dotPaint.color = ColorUtils.setAlphaComponent(config.secondaryTextColor, 100)
      canvas.drawCircle(rect.left + dotR * 7.4f, dotY, dotR, dotPaint)

      val title = "> ${config.customLabel.ifBlank { "SYS.CLOCK" }}"
      canvas.drawText(title, rect.left + dotR * 11f, dotY + metaPaint.textSize * 0.34f, metaPaint)
    }

    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      textAlign = Paint.Align.LEFT
      applyFloatShadow(this, config)
    }
    val timeStr = buildString {
      append(formattedHours(cal, config.is24Hour))
      append(":")
      append(formattedMinutes(cal))
      if (config.showSeconds) {
        append(":")
        append(formattedSeconds(cal))
      }
    }
    timePaint.textSize = fitTextSize(
      timePaint,
      timeStr,
      w * 0.96f,
      if (isCompact) h * 0.72f else h * 0.48f,
      minD * 0.44f * config.fontScale,
    )
    val fm = timePaint.fontMetrics
    val timeY = rect.centerY() - (fm.ascent + fm.descent) / 2f
    canvas.drawText(timeStr, rect.left, timeY, timePaint)

    if (!isCompact && config.showDate) {
      val dayProgress = ((cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)) / 1440f * 100).roundToInt()
      val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.secondaryTextColor
        textSize = (minD * 0.088f).coerceIn(9f, 18f)
      }
      canvas.drawText("${formattedDate(cal)}  [$dayProgress% DAY]", rect.left, rect.bottom - 4f, footerPaint)
    }
  }

  // ---------------------------------------------------------------------------
  // 6. EDITORIAL SERIF
  // ---------------------------------------------------------------------------
  private fun drawEditorialSerif(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
  ) {
    val w = rect.width()
    val h = rect.height()
    val minD = min(w, h)
    val isCompact = h < w * 0.40f

    val accentLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = config.accentColor
      strokeWidth = (minD * 0.012f).coerceIn(1.5f, 3f)
    }

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.accentColor
      letterSpacing = 0.22f
      textAlign = Paint.Align.CENTER
      textSize = (minD * 0.088f).coerceIn(9f, 18f)
    }

    if (!isCompact && config.customLabel.isNotBlank()) {
      canvas.drawText(config.customLabel.uppercase(Locale.getDefault()), rect.centerX(), rect.top + labelPaint.textSize, labelPaint)
      canvas.drawLine(rect.left + w * 0.22f, rect.top + labelPaint.textSize * 1.6f, rect.right - w * 0.22f, rect.top + labelPaint.textSize * 1.6f, accentLine)
    }

    val timeStr = "${formattedHours(cal, config.is24Hour)}:${formattedMinutes(cal)}"
    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      textAlign = Paint.Align.CENTER
      applyFloatShadow(this, config)
    }
    timePaint.textSize = fitTextSize(
      timePaint,
      timeStr,
      w * 0.90f,
      if (isCompact) h * 0.72f else h * 0.50f,
      minD * 0.56f * config.fontScale,
    )
    val fm = timePaint.fontMetrics
    canvas.drawText(timeStr, rect.centerX(), rect.centerY() - (fm.ascent + fm.descent) / 2f, timePaint)

    if (!isCompact && config.showDate) {
      val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.secondaryTextColor
        textAlign = Paint.Align.CENTER
        textSize = (minD * 0.095f).coerceIn(10f, 20f)
      }
      canvas.drawText(formattedLongDate(cal), rect.centerX(), rect.bottom - 4f, datePaint)
    }
  }

  // ---------------------------------------------------------------------------
  // 7..11. ANALOG SUITE (SWISS, CHRONO, BAUHAUS, ORBIT, ROMAN)
  // ---------------------------------------------------------------------------
  private fun drawAnalogSuite(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
  ) {
    val w = rect.width()
    val h = rect.height()
    val wide = w > h * 1.55f

    val dialRadius: Float
    val cx: Float
    val cy: Float

    if (wide) {
      dialRadius = h * 0.45f
      cx = rect.left + dialRadius + h * 0.05f
      cy = rect.centerY()
      drawAnalogRightCompanion(
        canvas = canvas,
        leftStart = cx + dialRadius + h * 0.14f,
        rect = rect,
        config = config,
        typeface = typeface,
        cal = cal,
      )
    } else {
      val hasLabel = (config.showDate || config.customLabel.isNotBlank()) && h > w * 0.85f
      val reservedBottom = if (hasLabel) min(w, h) * 0.14f else 0f
      dialRadius = min(w, h - reservedBottom) * 0.44f
      cx = rect.centerX()
      cy = rect.top + (h - reservedBottom) / 2f

      if (hasLabel) {
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          this.typeface = typeface
          color = config.secondaryTextColor
          textAlign = Paint.Align.CENTER
          textSize = (min(w, h) * 0.085f).coerceIn(10f, 20f)
        }
        val text = if (config.showDate) formattedDate(cal) else config.customLabel.uppercase(Locale.getDefault())
        canvas.drawText(text, rect.centerX(), rect.bottom - 2f, labelPaint)
      }
    }

    drawAnalogDial(canvas, cx, cy, dialRadius, config, typeface, cal)
  }

  private fun drawAnalogRightCompanion(
    canvas: Canvas,
    leftStart: Float,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
  ) {
    val availW = (rect.right - leftStart).coerceAtLeast(40f)
    val h = rect.height()

    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.foregroundColor
      textAlign = Paint.Align.LEFT
    }
    val timeStr = "${formattedHours(cal, config.is24Hour)}:${formattedMinutes(cal)}"
    timePaint.textSize = fitTextSize(timePaint, timeStr, availW * 0.95f, h * 0.48f, h * 0.46f * config.fontScale)
    val fm = timePaint.fontMetrics
    val timeY = rect.centerY() - (fm.ascent + fm.descent) / 2f - (if (config.showDate) h * 0.10f else 0f)
    canvas.drawText(timeStr, leftStart, timeY, timePaint)

    if (config.showDate || config.customLabel.isNotBlank()) {
      val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.accentColor
        textAlign = Paint.Align.LEFT
        textSize = (h * 0.14f).coerceIn(10f, 22f)
      }
      val subText = if (config.showDate) formattedDate(cal) else config.customLabel
      canvas.drawText(subText, leftStart, timeY + subPaint.textSize * 1.45f, subPaint)
    }
  }

  private fun drawAnalogDial(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    radius: Float,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
  ) {
    if (config.cardStyle != CardStyle.TRANSPARENT && config.backgroundAlpha > 0.02f) {
      val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.blendARGB(config.backgroundColor, config.foregroundColor, 0.05f)
      }
      canvas.drawCircle(cx, cy, radius, facePaint)

      val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (radius * 0.028f).coerceAtLeast(1.5f)
        color = ColorUtils.setAlphaComponent(config.foregroundColor, 55)
      }
      canvas.drawCircle(cx, cy, radius, ringPaint)
    } else if (config.showBorder) {
      val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (radius * 0.024f).coerceAtLeast(1.5f)
        color = ColorUtils.setAlphaComponent(config.foregroundColor, 95)
        applyFloatShadow(this, config)
      }
      canvas.drawCircle(cx, cy, radius, ringPaint)
    }

    when (config.styleId) {
      ClockStyleId.ANALOG_BAUHAUS -> {
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = ColorUtils.setAlphaComponent(config.accentColor, 48)
        }
        canvas.drawArc(
          RectF(cx - radius * 0.88f, cy - radius * 0.88f, cx + radius * 0.88f, cy + radius * 0.88f),
          -90f,
          90f,
          true,
          arcPaint,
        )
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 0 until 12) {
          val angle = Math.toRadians((i * 30 - 90).toDouble())
          val rPos = radius * 0.82f
          val px = cx + (cos(angle) * rPos).toFloat()
          val py = cy + (sin(angle) * rPos).toFloat()
          dotPaint.color = if (i % 3 == 0) config.accentColor else config.foregroundColor
          val dotSize = if (i % 3 == 0) radius * 0.055f else radius * 0.028f
          canvas.drawCircle(px, py, dotSize, dotPaint)
        }
      }
      ClockStyleId.ANALOG_ORBIT -> {
        val innerRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = (radius * 0.02f).coerceAtLeast(1.5f)
          color = ColorUtils.setAlphaComponent(config.secondaryTextColor, 60)
        }
        canvas.drawCircle(cx, cy, radius * 0.62f, innerRing)

        // Glowing minute arc
        val minAngle = (cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f) * 6f
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = (radius * 0.06f).coerceAtLeast(3f)
          strokeCap = Paint.Cap.ROUND
          color = config.accentColor
        }
        canvas.drawArc(
          RectF(cx - radius * 0.90f, cy - radius * 0.90f, cx + radius * 0.90f, cy + radius * 0.90f),
          -90f,
          minAngle,
          false,
          arcPaint,
        )
        // Planet orb at minute tip
        val minRad = Math.toRadians((minAngle - 90f).toDouble())
        val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = config.foregroundColor }
        canvas.drawCircle(
          cx + (cos(minRad) * radius * 0.90f).toFloat(),
          cy + (sin(minRad) * radius * 0.90f).toFloat(),
          radius * 0.075f,
          orbPaint,
        )
        // Hour orb on inner ring
        val hrAngle = ((cal.get(Calendar.HOUR) % 12) + cal.get(Calendar.MINUTE) / 60f) * 30f
        val hrRad = Math.toRadians((hrAngle - 90f).toDouble())
        orbPaint.color = config.accentColor
        canvas.drawCircle(
          cx + (cos(hrRad) * radius * 0.62f).toFloat(),
          cy + (sin(hrRad) * radius * 0.62f).toFloat(),
          radius * 0.085f,
          orbPaint,
        )
        // Center digital pill
        val centerNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          this.typeface = typeface
          color = config.foregroundColor
          textAlign = Paint.Align.CENTER
          textSize = radius * 0.34f * config.fontScale
        }
        val cfm = centerNumPaint.fontMetrics
        canvas.drawText(
          "${formattedHours(cal, config.is24Hour)}:${formattedMinutes(cal)}",
          cx,
          cy - (cfm.ascent + cfm.descent) / 2f,
          centerNumPaint,
        )
        return
      }
      ClockStyleId.ANALOG_ROMAN -> {
        val numerals = arrayOf("XII", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI")
        val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          this.typeface = typeface
          color = config.foregroundColor
          textAlign = Paint.Align.CENTER
          textSize = (radius * 0.16f).coerceIn(8f, 34f)
        }
        val nfm = numPaint.fontMetrics
        for (i in 0 until 12) {
          val angle = Math.toRadians((i * 30 - 90).toDouble())
          val rPos = radius * 0.77f
          val px = cx + (cos(angle) * rPos).toFloat()
          val py = cy + (sin(angle) * rPos).toFloat() - (nfm.ascent + nfm.descent) / 2f
          numPaint.color = if (i % 3 == 0) config.accentColor else config.foregroundColor
          canvas.drawText(numerals[i], px, py, numPaint)
        }
      }
      ClockStyleId.ANALOG_CHRONO -> {
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
        for (i in 0 until 60) {
          val angle = Math.toRadians((i * 6 - 90).toDouble())
          val isHour = i % 5 == 0
          val startR = if (isHour) radius * 0.78f else radius * 0.87f
          val endR = radius * 0.93f
          tickPaint.strokeWidth = if (isHour) radius * 0.042f else radius * 0.016f
          tickPaint.color = if (i % 15 == 0) config.accentColor else ColorUtils.setAlphaComponent(config.foregroundColor, if (isHour) 235 else 110)
          canvas.drawLine(
            cx + (cos(angle) * startR).toFloat(),
            cy + (sin(angle) * startR).toFloat(),
            cx + (cos(angle) * endR).toFloat(),
            cy + (sin(angle) * endR).toFloat(),
            tickPaint,
          )
        }
        // Day-of-month window at 3 o'clock
        if (config.showDate) {
          val boxW = radius * 0.24f
          val boxH = radius * 0.18f
          val boxRect = RectF(cx + radius * 0.42f - boxW / 2f, cy - boxH / 2f, cx + radius * 0.42f + boxW / 2f, cy + boxH / 2f)
          val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ColorUtils.setAlphaComponent(config.accentColor, 55) }
          canvas.drawRoundRect(boxRect, 4f, 4f, boxPaint)
          val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = config.foregroundColor
            textAlign = Paint.Align.CENTER
            textSize = boxH * 0.72f
          }
          val dfm = dayPaint.fontMetrics
          canvas.drawText(
            cal.get(Calendar.DAY_OF_MONTH).toString(),
            boxRect.centerX(),
            boxRect.centerY() - (dfm.ascent + dfm.descent) / 2f,
            dayPaint,
          )
        }
      }
      else -> {
        // ANALOG_SWISS
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.BUTT }
        for (i in 0 until 60) {
          val angle = Math.toRadians((i * 6 - 90).toDouble())
          val isHour = i % 5 == 0
          val startR = if (isHour) radius * 0.74f else radius * 0.86f
          val endR = radius * 0.94f
          tickPaint.strokeWidth = if (isHour) radius * 0.052f else radius * 0.018f
          tickPaint.color = config.foregroundColor
          canvas.drawLine(
            cx + (cos(angle) * startR).toFloat(),
            cy + (sin(angle) * startR).toFloat(),
            cx + (cos(angle) * endR).toFloat(),
            cy + (sin(angle) * endR).toFloat(),
            tickPaint,
          )
        }
      }
    }

    // Draw Hour, Minute, Second Hands
    val sec = cal.get(Calendar.SECOND)
    val min = cal.get(Calendar.MINUTE) + sec / 60f
    val hour = (cal.get(Calendar.HOUR) % 12) + min / 60f

    val hourAngle = Math.toRadians((hour * 30f - 90f).toDouble())
    val minAngle = Math.toRadians((min * 6f - 90f).toDouble())
    val secAngle = Math.toRadians((sec * 6f - 90f).toDouble())

    val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = config.foregroundColor
      strokeWidth = (radius * 0.075f).coerceAtLeast(3f)
      strokeCap = Paint.Cap.ROUND
      applyFloatShadow(this, config)
    }
    canvas.drawLine(
      cx - (cos(hourAngle) * radius * 0.14f).toFloat(),
      cy - (sin(hourAngle) * radius * 0.14f).toFloat(),
      cx + (cos(hourAngle) * radius * 0.52f).toFloat(),
      cy + (sin(hourAngle) * radius * 0.52f).toFloat(),
      hourPaint,
    )

    val minPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = config.foregroundColor
      strokeWidth = (radius * 0.048f).coerceAtLeast(2.2f)
      strokeCap = Paint.Cap.ROUND
      applyFloatShadow(this, config)
    }
    canvas.drawLine(
      cx - (cos(minAngle) * radius * 0.16f).toFloat(),
      cy - (sin(minAngle) * radius * 0.16f).toFloat(),
      cx + (cos(minAngle) * radius * 0.78f).toFloat(),
      cy + (sin(minAngle) * radius * 0.78f).toFloat(),
      minPaint,
    )

    if (config.showSeconds) {
      val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.accentColor
        strokeWidth = (radius * 0.022f).coerceAtLeast(1.5f)
        strokeCap = Paint.Cap.ROUND
      }
      val tipX = cx + (cos(secAngle) * radius * 0.84f).toFloat()
      val tipY = cy + (sin(secAngle) * radius * 0.84f).toFloat()
      canvas.drawLine(
        cx - (cos(secAngle) * radius * 0.22f).toFloat(),
        cy - (sin(secAngle) * radius * 0.22f).toFloat(),
        tipX,
        tipY,
        secPaint,
      )
      if (config.styleId == ClockStyleId.ANALOG_SWISS) {
        canvas.drawCircle(
          cx + (cos(secAngle) * radius * 0.62f).toFloat(),
          cy + (sin(secAngle) * radius * 0.62f).toFloat(),
          radius * 0.055f,
          secPaint,
        )
      }
    }

    val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = config.accentColor }
    canvas.drawCircle(cx, cy, radius * 0.058f, capPaint)
  }

  // ---------------------------------------------------------------------------
  // 12. WORD MATRIX CLOCK (11x10 Grid)
  // ---------------------------------------------------------------------------
  private val matrixRows = listOf(
    "ITLISASTIME",
    "ACQUARTERDC",
    "TWENTYFIVEX",
    "HALFBTENFTO",
    "PASTERUNINE",
    "ONESIXTHREE",
    "FOURFIVETWO",
    "EIGHTELEVEN",
    "SEVENTWELVE",
    "TENSEOCLOCK",
  )

  private fun computeMatrixHighlights(cal: Calendar): Set<Pair<Int, Int>> {
    val active = mutableSetOf<Pair<Int, Int>>()
    fun mark(row: Int, startCol: Int, len: Int) {
      for (c in startCol until startCol + len) active.add(row to c)
    }
    // IT IS
    mark(0, 0, 2)
    mark(0, 3, 2)

    val rawMin = cal.get(Calendar.MINUTE)
    val roundedMin = ((rawMin + 2) / 5) * 5
    var hour = cal.get(Calendar.HOUR)
    if (roundedMin > 30) hour = (hour + 1) % 12
    if (hour == 0) hour = 12

    when (roundedMin % 60) {
      0 -> mark(9, 5, 6) // OCLOCK
      5 -> { mark(2, 6, 4); mark(4, 0, 4) } // FIVE PAST
      10 -> { mark(3, 5, 3); mark(4, 0, 4) } // TEN PAST
      15 -> { mark(1, 2, 7); mark(4, 0, 4) } // QUARTER PAST
      20 -> { mark(2, 0, 6); mark(4, 0, 4) } // TWENTY PAST
      25 -> { mark(2, 0, 10); mark(4, 0, 4) } // TWENTYFIVE PAST
      30 -> { mark(3, 0, 4); mark(4, 0, 4) } // HALF PAST
      35 -> { mark(2, 0, 10); mark(3, 9, 2) } // TWENTYFIVE TO
      40 -> { mark(2, 0, 6); mark(3, 9, 2) } // TWENTY TO
      45 -> { mark(1, 2, 7); mark(3, 9, 2) } // QUARTER TO
      50 -> { mark(3, 5, 3); mark(3, 9, 2) } // TEN TO
      55 -> { mark(2, 6, 4); mark(3, 9, 2) } // FIVE TO
    }

    when (hour) {
      1 -> mark(5, 0, 3)
      2 -> mark(6, 8, 3)
      3 -> mark(5, 6, 5)
      4 -> mark(6, 0, 4)
      5 -> mark(6, 4, 4)
      6 -> mark(5, 3, 3)
      7 -> mark(8, 0, 5)
      8 -> mark(7, 0, 5)
      9 -> mark(4, 7, 4)
      10 -> mark(9, 0, 3)
      11 -> mark(7, 5, 6)
      12 -> mark(8, 5, 6)
    }
    return active
  }

  private fun drawWordMatrix(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
  ) {
    val w = rect.width()
    val h = rect.height()
    val wide = w > h * 1.55f
    val active = computeMatrixHighlights(cal)

    val gridRect = if (wide) {
      val gridW = min(w * 0.56f, h * 1.15f)
      RectF(rect.left, rect.top, rect.left + gridW, rect.bottom)
    } else {
      rect
    }

    if (wide) {
      drawAnalogRightCompanion(canvas, gridRect.right + h * 0.10f, rect, config, typeface, cal)
    }

    val cols = 11
    val rows = 10
    val cellW = gridRect.width() / cols
    val cellH = gridRect.height() / rows
    val letterSize = (min(cellW, cellH) * 0.68f * config.fontScale).coerceIn(7f, 34f)

    val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = ColorUtils.setAlphaComponent(config.secondaryTextColor, 72)
      textAlign = Paint.Align.CENTER
      textSize = letterSize
    }
    val litPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      color = config.accentColor
      textAlign = Paint.Align.CENTER
      textSize = letterSize
      isFakeBoldText = true
    }
    val fm = litPaint.fontMetrics

    for (r in 0 until rows) {
      val rowStr = matrixRows[r]
      for (c in 0 until cols) {
        val ch = rowStr[c].toString()
        val x = gridRect.left + cellW * (c + 0.5f)
        val y = gridRect.top + cellH * (r + 0.5f) - (fm.ascent + fm.descent) / 2f
        val p = if ((r to c) in active) litPaint else dimPaint
        canvas.drawText(ch, x, y, p)
      }
    }
  }

  // ---------------------------------------------------------------------------
  // 13. POETIC WORD STATEMENT
  // ---------------------------------------------------------------------------
  private fun buildPoeticParts(cal: Calendar): Pair<String, String> {
    val rawMin = cal.get(Calendar.MINUTE)
    val rounded = ((rawMin + 2) / 5) * 5
    var hr = cal.get(Calendar.HOUR)
    if (rounded > 30) hr = (hr + 1) % 12
    val hrWord = when (if (hr == 0) 12 else hr) {
      1 -> "one"
      2 -> "two"
      3 -> "three"
      4 -> "four"
      5 -> "five"
      6 -> "six"
      7 -> "seven"
      8 -> "eight"
      9 -> "nine"
      10 -> "ten"
      11 -> "eleven"
      else -> "twelve"
    }
    val phrase = when (rounded % 60) {
      0 -> "$hrWord o'clock"
      5 -> "five past $hrWord"
      10 -> "ten past $hrWord"
      15 -> "quarter past $hrWord"
      20 -> "twenty past $hrWord"
      25 -> "twenty-five past $hrWord"
      30 -> "half past $hrWord"
      35 -> "twenty-five to $hrWord"
      40 -> "twenty to $hrWord"
      45 -> "quarter to $hrWord"
      50 -> "ten to $hrWord"
      else -> "five to $hrWord"
    }
    val period = when (cal.get(Calendar.HOUR_OF_DAY)) {
      in 5..11 -> "in the morning."
      in 12..16 -> "in the afternoon."
      in 17..20 -> "in the evening."
      else -> "at night."
    }
    return "It's $phrase" to period
  }

  private fun drawWordStatement(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
  ) {
    val w = rect.width()
    val h = rect.height()
    val minD = min(w, h)
    val (mainPhrase, periodPhrase) = buildPoeticParts(cal)
    val full = "$mainPhrase $periodPhrase"

    val ssb = SpannableStringBuilder(full).apply {
      setSpan(ForegroundColorSpan(config.foregroundColor), 0, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
      setSpan(ForegroundColorSpan(config.accentColor), 4, mainPhrase.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
      setSpan(ForegroundColorSpan(config.secondaryTextColor), mainPhrase.length, full.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    var textSize = (minD * 0.21f * config.fontScale).coerceIn(13f, 48f)
    val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      this.textSize = textSize
      applyFloatShadow(this, config)
    }
    var layout = StaticLayout.Builder.obtain(ssb, 0, ssb.length, tp, w.roundToInt().coerceAtLeast(40))
      .setAlignment(Layout.Alignment.ALIGN_NORMAL)
      .setLineSpacing(0f, 1.08f)
      .build()

    val maxTextH = if (config.showDate && h > w * 0.42f) h * 0.76f else h * 0.92f
    while (layout.height > maxTextH && textSize > 11f) {
      textSize *= 0.88f
      tp.textSize = textSize
      layout = StaticLayout.Builder.obtain(ssb, 0, ssb.length, tp, w.roundToInt().coerceAtLeast(40))
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(0f, 1.08f)
        .build()
    }

    canvas.save()
    val topY = rect.top + ((maxTextH - layout.height) / 2f).coerceAtLeast(0f)
    canvas.translate(rect.left, topY)
    layout.draw(canvas)
    canvas.restore()

    if (config.showDate && h > w * 0.42f) {
      val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.secondaryTextColor
        textSize = (minD * 0.088f).coerceIn(9f, 18f)
      }
      canvas.drawText(
        "${formattedDate(cal)}  •  ${config.customLabel}".trim(),
        rect.left,
        rect.bottom - 2f,
        datePaint,
      )
    }
  }

  // ---------------------------------------------------------------------------
  // 14. HYBRID DUO BAR (Mini Analog + Bold Digital)
  // ---------------------------------------------------------------------------
  private fun drawWordDuoBar(
    canvas: Canvas,
    rect: RectF,
    config: WidgetConfig,
    typeface: Typeface,
    cal: Calendar,
  ) {
    val w = rect.width()
    val h = rect.height()
    val minD = min(w, h)

    if (w >= h * 1.35f) {
      val dialR = h * 0.43f
      val cx = rect.left + dialR + 4f
      val cy = rect.centerY()
      val analogCfg = config.copy(styleId = ClockStyleId.ANALOG_CHRONO)
      drawAnalogDial(canvas, cx, cy, dialR, analogCfg, typeface, cal)

      val dividerX = cx + dialR + minD * 0.09f
      val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(config.secondaryTextColor, 70)
        strokeWidth = 2f
      }
      canvas.drawLine(dividerX, rect.top + h * 0.14f, dividerX, rect.bottom - h * 0.14f, divPaint)

      drawAnalogRightCompanion(canvas, dividerX + minD * 0.09f, rect, config, typeface, cal)
    } else {
      val dialR = min(w, h) * 0.28f
      val cx = rect.centerX()
      val cy = rect.top + dialR + 4f
      val analogCfg = config.copy(styleId = ClockStyleId.ANALOG_CHRONO)
      drawAnalogDial(canvas, cx, cy, dialR, analogCfg, typeface, cal)

      val timeStr = "${formattedHours(cal, config.is24Hour)}:${formattedMinutes(cal)}"
      val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        color = config.foregroundColor
        textAlign = Paint.Align.CENTER
      }
      val availH = rect.bottom - (cy + dialR + 8f)
      timePaint.textSize = fitTextSize(timePaint, timeStr, w * 0.90f, availH * 0.56f, minD * 0.28f * config.fontScale)
      val fm = timePaint.fontMetrics
      val timeY = cy + dialR + availH * 0.45f - (fm.ascent + fm.descent) / 2f
      canvas.drawText(timeStr, rect.centerX(), timeY, timePaint)

      if (config.showDate) {
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          this.typeface = typeface
          color = config.accentColor
          textAlign = Paint.Align.CENTER
          textSize = (minD * 0.082f).coerceIn(9f, 18f)
        }
        canvas.drawText(formattedDate(cal), rect.centerX(), rect.bottom - 2f, datePaint)
      }
    }
  }
}
