package com.example.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.view.View
import com.example.data.WidgetConfigRepository
import com.example.model.ClockPresets
import com.example.model.ClockStyleId
import com.example.model.DisplayMode
import com.example.model.WeatherSnapshot
import com.example.model.WidgetConfig
import com.example.weather.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Android Screen Saver (DreamService) providing an Always-On / Ambient Clock display
 * while docked or charging, rendered with the user's active ChronoCraft AOD variant.
 */
class ChronoDreamService : DreamService() {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var dreamView: AodClockDreamView? = null

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    isInteractive = false
    isFullscreen = true
    isScreenBright = false

    val view = AodClockDreamView(this)
    dreamView = view
    setContentView(view)

    val configRepo = WidgetConfigRepository(applicationContext)
    val weatherRepo = WeatherRepository(applicationContext)

    scope.launch {
      configRepo.activeDraftFlow.collectLatest { cfg ->
        view.updateConfig(cfg)
      }
    }
    scope.launch {
      weatherRepo.weatherFlow.collectLatest { snap ->
        view.updateWeather(snap)
      }
    }
  }

  override fun onDreamingStarted() {
    super.onDreamingStarted()
    dreamView?.startTicker()
  }

  override fun onDreamingStopped() {
    dreamView?.stopTicker()
    super.onDreamingStopped()
  }

  override fun onDetachedFromWindow() {
    dreamView?.stopTicker()
    scope.cancel()
    super.onDetachedFromWindow()
  }
}

private class AodClockDreamView(context: Context) : View(context) {

  private var config: WidgetConfig = ClockPresets.defaultConfigForStyle(ClockStyleId.WEIGHT_STACK_FLOAT)
  private var weather: WeatherSnapshot = WeatherSnapshot()
  private val handler = Handler(Looper.getMainLooper())
  private var running = false

  private val tickRunnable = object : Runnable {
    override fun run() {
      invalidate()
      if (running) {
        val now = System.currentTimeMillis()
        val delayMs = (60_000L - (now % 60_000L)).coerceIn(1_000L, 60_000L)
        handler.postDelayed(this, delayMs)
      }
    }
  }

  fun updateConfig(newConfig: WidgetConfig) {
    config = newConfig
    invalidate()
  }

  fun updateWeather(snapshot: WeatherSnapshot) {
    weather = snapshot
    invalidate()
  }

  fun startTicker() {
    running = true
    handler.removeCallbacks(tickRunnable)
    handler.post(tickRunnable)
  }

  fun stopTicker() {
    running = false
    handler.removeCallbacks(tickRunnable)
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    canvas.drawColor(Color.BLACK)

    val w = width.toFloat()
    val h = height.toFloat()
    if (w < 64f || h < 64f) return

    val clockW = (w * 0.84f).coerceAtMost(960f)
    val clockH = (h * 0.34f).coerceIn(220f, 440f)

    // Gentle minute-based burn-in protection offset
    val minuteIdx = ((System.currentTimeMillis() / 60_000L) % 6L).toInt()
    val shiftX = ((minuteIdx % 3) - 1) * 14f
    val shiftY = ((minuteIdx / 3) - 0.5f) * 28f

    val left = (w - clockW) / 2f + shiftX
    val top = (h - clockH) / 2f + shiftY

    canvas.save()
    canvas.translate(left, top)
    ClockCanvasRenderer.drawClock(
      context = context,
      canvas = canvas,
      width = clockW,
      height = clockH,
      config = config,
      timeMillis = System.currentTimeMillis(),
      weather = weather,
      displayMode = DisplayMode.AOD,
      animPhase = 0f,
    )
    canvas.restore()
  }
}
