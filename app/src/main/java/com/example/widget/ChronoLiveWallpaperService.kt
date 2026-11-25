package com.example.widget

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.example.R
import com.example.data.WidgetConfigRepository
import com.example.model.ClockCategory
import com.example.model.ClockPresets
import com.example.model.ClockStyleId
import com.example.model.LiveWallpaperTarget
import com.example.model.SecondsStyle
import com.example.model.WallpaperOption
import com.example.model.WeatherSnapshot
import com.example.model.WidgetConfig
import com.example.weather.WeatherRepository
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Live Wallpaper Engine rendering the user's ChronoCraft backdrop and clock style
 * on Home Screen and/or Lock Screen via Android's official WallpaperService.
 */
class ChronoLiveWallpaperService : WallpaperService() {

  override fun onCreateEngine(): Engine = ChronoWallpaperEngine()

  private inner class ChronoWallpaperEngine : Engine() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())

    private var config: WidgetConfig = ClockPresets.defaultConfigForStyle(ClockStyleId.WEIGHT_STACK_FLOAT)
    private var weather: WeatherSnapshot = WeatherSnapshot()
    private var target: LiveWallpaperTarget = LiveWallpaperTarget.LOCK_SCREEN
    private var wallpaperOption: WallpaperOption = WallpaperOption.AURORA
    private var customPhotoUri: String? = null

    private var cachedBackdropKey: String? = null
    private var cachedBackdropBitmap: Bitmap? = null
    private var visible = false
    private var xOffset = 0.5f

    private val drawRunnable = object : Runnable {
      override fun run() {
        drawFrame()
        if (visible) {
          val fastTick =
            target != LiveWallpaperTarget.BACKDROP_ONLY &&
              (config.secondsStyle != SecondsStyle.NONE || config.styleId.category == ClockCategory.ANALOG)
          val now = System.currentTimeMillis()
          val delay = if (fastTick) 1000L - (now % 1000L) else 60_000L - (now % 60_000L)
          handler.postDelayed(this, delay.coerceIn(250L, 60_000L))
        }
      }
    }

    override fun onCreate(surfaceHolder: SurfaceHolder) {
      super.onCreate(surfaceHolder)
      val repo = WidgetConfigRepository(applicationContext)
      val weatherRepo = WeatherRepository(applicationContext)

      scope.launch {
        repo.activeDraftFlow.collectLatest { cfg ->
          config = cfg
          if (visible) drawFrame()
        }
      }
      scope.launch {
        repo.liveWallpaperTargetFlow.collectLatest { t ->
          target = t
          if (visible) drawFrame()
        }
      }
      scope.launch {
        repo.liveWallpaperOptionFlow.collectLatest { opt ->
          wallpaperOption = opt
          cachedBackdropKey = null
          if (visible) drawFrame()
        }
      }
      scope.launch {
        repo.liveWallpaperPhotoUriFlow.collectLatest { uri ->
          customPhotoUri = uri
          cachedBackdropKey = null
          if (visible) drawFrame()
        }
      }
      scope.launch {
        weatherRepo.weatherFlow.collectLatest { snap ->
          weather = snap
          if (visible) drawFrame()
        }
      }
    }

    override fun onVisibilityChanged(visible: Boolean) {
      this.visible = visible
      handler.removeCallbacks(drawRunnable)
      if (visible) {
        handler.post(drawRunnable)
      }
    }

    override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
      super.onSurfaceChanged(holder, format, width, height)
      if (visible) drawFrame()
    }

    override fun onOffsetsChanged(
      xOffset: Float,
      yOffset: Float,
      xOffsetStep: Float,
      yOffsetStep: Float,
      xPixelOffset: Int,
      yPixelOffset: Int,
    ) {
      this.xOffset = xOffset
      if (visible) drawFrame()
    }

    override fun onDestroy() {
      visible = false
      handler.removeCallbacks(drawRunnable)
      scope.cancel()
      cachedBackdropBitmap = null
      super.onDestroy()
    }

    private fun resolveBackdropBitmap(): Bitmap? {
      val key = "${wallpaperOption.name}:${customPhotoUri.orEmpty()}"
      if (key == cachedBackdropKey && cachedBackdropBitmap?.isRecycled == false) {
        return cachedBackdropBitmap
      }
      cachedBackdropKey = key
      val bmp = when (wallpaperOption) {
        WallpaperOption.AMOLED -> null
        WallpaperOption.CUSTOM_PHOTO -> {
          val uriStr = customPhotoUri
          if (!uriStr.isNullOrBlank()) {
            runCatching {
              val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
              applicationContext.contentResolver.openInputStream(Uri.parse(uriStr))?.use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
              }
            }.getOrNull() ?: decodeResourceBackdrop(R.drawable.img_wallpaper_aurora_1795495861674)
          } else {
            decodeResourceBackdrop(R.drawable.img_wallpaper_aurora_1795495861674)
          }
        }
        WallpaperOption.AURORA -> decodeResourceBackdrop(R.drawable.img_wallpaper_aurora_1795495861674)
        WallpaperOption.TERRACOTTA -> decodeResourceBackdrop(R.drawable.img_wallpaper_terracotta_1795495862707)
        WallpaperOption.GLASS -> decodeResourceBackdrop(R.drawable.img_wallpaper_glass_1795495862804)
      }
      cachedBackdropBitmap = bmp
      return bmp
    }

    private fun decodeResourceBackdrop(resId: Int): Bitmap? {
      return runCatching {
        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
        BitmapFactory.decodeResource(resources, resId, opts)
      }.getOrNull()
    }

    private fun drawFrame() {
      val holder = surfaceHolder
      var canvas: Canvas? = null
      try {
        canvas = holder.lockCanvas() ?: return
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        canvas.drawColor(Color.rgb(6, 9, 16))

        val bgBitmap = resolveBackdropBitmap()
        if (bgBitmap != null) {
          val scale = maxOf((w * 1.08f) / bgBitmap.width.toFloat(), h / bgBitmap.height.toFloat())
          val drawW = bgBitmap.width * scale
          val drawH = bgBitmap.height * scale
          val slackX = (drawW - w).coerceAtLeast(0f)
          val left = -(slackX * xOffset.coerceIn(0f, 1f))
          val top = (h - drawH) / 2f
          val dst = RectF(left, top, left + drawW, top + drawH)
          canvas.drawBitmap(bgBitmap, null, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
          canvas.drawColor(Color.argb(if (target == LiveWallpaperTarget.LOCK_SCREEN) 68 else 36, 0, 0, 0))
        }

        if (target != LiveWallpaperTarget.BACKDROP_ONLY) {
          val isAnalog = config.styleId.category == ClockCategory.ANALOG ||
            config.styleId == ClockStyleId.FLOATING_ANALOG_PURE ||
            config.styleId == ClockStyleId.SOLAR_ORBIT_PLANETS
          val clockW = if (isAnalog) min(w * 0.72f, h * 0.34f) else (w * 0.86f).coerceAtMost(h * 0.78f)
          val clockH = if (isAnalog) clockW else (h * 0.25f).coerceIn(w * 0.34f, h * 0.34f)
          val clockLeft = (w - clockW) / 2f
          val clockTop = if (target == LiveWallpaperTarget.LOCK_SCREEN) h * 0.14f else h * 0.10f

          canvas.save()
          canvas.translate(clockLeft, clockTop)
          ClockCanvasRenderer.drawClock(
            context = applicationContext,
            canvas = canvas,
            width = clockW,
            height = clockH,
            config = config,
            timeMillis = System.currentTimeMillis(),
            weather = weather,
            displayMode = target.displayMode,
            animPhase = ((System.currentTimeMillis() % 6000L) / 6000f),
          )
          canvas.restore()
        }
      } catch (_: Throwable) {
      } finally {
        if (canvas != null) {
          runCatching { holder.unlockCanvasAndPost(canvas) }
        }
      }
    }
  }

  companion object {
    fun launchWallpaperPicker(context: Context) {
      val component = ComponentName(context, ChronoLiveWallpaperService::class.java)
      val specificIntent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
        putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      val fallbackIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      runCatching { context.startActivity(specificIntent) }
        .onFailure { runCatching { context.startActivity(fallbackIntent) } }
    }
  }
}
