package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import com.example.R
import com.example.data.WidgetConfigRepository
import com.example.model.ActiveHomeWidget
import com.example.model.ClockStyleId
import com.example.ui.widgetconfig.WidgetConfigureActivity
import com.example.weather.WeatherRepository
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

open class BaseChronoWidgetProvider(
  private val defaultStyle: ClockStyleId = ClockStyleId.CYBER_HUD,
  private val defaultWidthDp: Int = 280,
  private val defaultHeightDp: Int = 150,
) : AppWidgetProvider() {

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val repo = WidgetConfigRepository(context.applicationContext)
        for (appWidgetId in appWidgetIds) {
          val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
          WidgetUpdater.renderAndPushWidget(
            context = context.applicationContext,
            appWidgetManager = appWidgetManager,
            appWidgetId = appWidgetId,
            options = options,
            repo = repo,
            fallbackStyle = defaultStyle,
            defaultWidthDp = defaultWidthDp,
            defaultHeightDp = defaultHeightDp,
          )
        }
        ClockTickScheduler.scheduleNextMinuteTick(context.applicationContext)
      } finally {
        pendingResult.finish()
      }
    }
  }

  override fun onAppWidgetOptionsChanged(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    newOptions: Bundle,
  ) {
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val repo = WidgetConfigRepository(context.applicationContext)
        WidgetUpdater.renderAndPushWidget(
          context = context.applicationContext,
          appWidgetManager = appWidgetManager,
          appWidgetId = appWidgetId,
          options = newOptions,
          repo = repo,
          fallbackStyle = defaultStyle,
          defaultWidthDp = defaultWidthDp,
          defaultHeightDp = defaultHeightDp,
        )
      } finally {
        pendingResult.finish()
      }
    }
  }

  override fun onDeleted(context: Context, appWidgetIds: IntArray) {
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val repo = WidgetConfigRepository(context.applicationContext)
        for (id in appWidgetIds) {
          repo.deleteWidgetConfig(id)
        }
        if (WidgetUpdater.getAllActiveWidgetIds(context.applicationContext).isEmpty()) {
          ClockTickScheduler.cancelTick(context.applicationContext)
        }
      } finally {
        pendingResult.finish()
      }
    }
  }

  override fun onDisabled(context: Context) {
    if (WidgetUpdater.getAllActiveWidgetIds(context.applicationContext).isEmpty()) {
      ClockTickScheduler.cancelTick(context.applicationContext)
    }
  }
}

class ChronoClockWidgetProvider :
  BaseChronoWidgetProvider(
    defaultStyle = ClockStyleId.CYBER_HUD,
    defaultWidthDp = 280,
    defaultHeightDp = 150,
  )

class ChronoAnalogWidgetProvider :
  BaseChronoWidgetProvider(
    defaultStyle = ClockStyleId.ANALOG_CHRONO,
    defaultWidthDp = 170,
    defaultHeightDp = 170,
  )

class ChronoWideWidgetProvider :
  BaseChronoWidgetProvider(
    defaultStyle = ClockStyleId.GLASS_PILL,
    defaultWidthDp = 310,
    defaultHeightDp = 88,
  )

class ClockTickReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        WidgetUpdater.updateAllWidgets(context.applicationContext)
        if (WidgetUpdater.getAllActiveWidgetIds(context.applicationContext).isNotEmpty()) {
          ClockTickScheduler.scheduleNextMinuteTick(context.applicationContext)
        }
      } finally {
        pendingResult.finish()
      }
    }
  }
}

class PinWidgetReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val appWidgetId =
      intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val repo = WidgetConfigRepository(context.applicationContext)
        val pendingConfig = repo.consumePendingPinConfig()
        if (pendingConfig != null) {
          repo.saveWidgetConfig(appWidgetId, pendingConfig)
        }
        WidgetUpdater.updateWidget(context.applicationContext, appWidgetId)
        ClockTickScheduler.scheduleNextMinuteTick(context.applicationContext)
      } finally {
        pendingResult.finish()
      }
    }
  }
}

object ClockTickScheduler {
  private const val ACTION_MINUTE_TICK = "com.example.ACTION_CHRONO_MINUTE_TICK"

  private fun tickPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, ClockTickReceiver::class.java).apply { action = ACTION_MINUTE_TICK }
    return PendingIntent.getBroadcast(
      context,
      4091,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  fun scheduleNextMinuteTick(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val now = Calendar.getInstance()
    now.add(Calendar.MINUTE, 1)
    now.set(Calendar.SECOND, 0)
    now.set(Calendar.MILLISECOND, 50)
    val triggerAt = now.timeInMillis
    val pi = tickPendingIntent(context)
    // Non-waking RTC alarm so device sleep battery is respected with zero restricted permissions
    alarmManager.setWindow(AlarmManager.RTC, triggerAt, 5_000L, pi)
  }

  fun cancelTick(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    alarmManager.cancel(tickPendingIntent(context))
  }
}

object WidgetUpdater {

  fun getAllActiveWidgetIds(context: Context): List<Pair<Int, String>> {
    val mgr = AppWidgetManager.getInstance(context)
    val providers = listOf(
      ChronoClockWidgetProvider::class.java to "Custom Any-Size",
      ChronoAnalogWidgetProvider::class.java to "Analog Dial",
      ChronoWideWidgetProvider::class.java to "Wide Strip",
    )
    return providers.flatMap { (cls, label) ->
      mgr.getAppWidgetIds(ComponentName(context, cls)).map { it to label }
    }
  }

  suspend fun loadActiveWidgets(context: Context, repo: WidgetConfigRepository): List<ActiveHomeWidget> {
    return getAllActiveWidgetIds(context).map { (id, label) ->
      ActiveHomeWidget(
        appWidgetId = id,
        providerLabel = label,
        config = repo.getWidgetConfig(id),
      )
    }
  }

  suspend fun updateAllWidgets(context: Context) {
    val mgr = AppWidgetManager.getInstance(context)
    val repo = WidgetConfigRepository(context)
    val specs = listOf(
      Triple(ChronoClockWidgetProvider::class.java, ClockStyleId.CYBER_HUD, 280 to 150),
      Triple(ChronoAnalogWidgetProvider::class.java, ClockStyleId.ANALOG_CHRONO, 170 to 170),
      Triple(ChronoWideWidgetProvider::class.java, ClockStyleId.GLASS_PILL, 310 to 88),
    )
    for ((cls, style, defaults) in specs) {
      val ids = mgr.getAppWidgetIds(ComponentName(context, cls))
      for (id in ids) {
        val options = mgr.getAppWidgetOptions(id)
        renderAndPushWidget(context, mgr, id, options, repo, style, defaults.first, defaults.second)
      }
    }
  }

  suspend fun updateWidget(context: Context, appWidgetId: Int) {
    val mgr = AppWidgetManager.getInstance(context)
    val repo = WidgetConfigRepository(context)
    val options = mgr.getAppWidgetOptions(appWidgetId)
    renderAndPushWidget(context, mgr, appWidgetId, options, repo, ClockStyleId.CYBER_HUD, 280, 150)
  }

  suspend fun renderAndPushWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    options: Bundle,
    repo: WidgetConfigRepository,
    fallbackStyle: ClockStyleId,
    defaultWidthDp: Int,
    defaultHeightDp: Int,
  ) {
    val config = repo.getWidgetConfig(appWidgetId, fallbackStyle)
    val weather = runCatching { WeatherRepository(context).refreshIfStale() }.getOrDefault(com.example.model.WeatherSnapshot())
    val (widthDp, heightDp) = resolveWidgetDpSize(options, defaultWidthDp, defaultHeightDp)
    val density = context.resources.displayMetrics.density
    val animateOverlay = config.animationsEnabled && config.animationStyle != com.example.model.ClockAnimationStyle.NONE

    val views = RemoteViews(context.packageName, R.layout.widget_chrono_clock)
    val baseMaxBytes = if (animateOverlay) 360_000 else 1_600_000
    val bitmap = ClockCanvasRenderer.renderToBitmap(
      context = context,
      config = config,
      widthDp = widthDp,
      heightDp = heightDp,
      density = density,
      weather = weather,
      maxBytes = baseMaxBytes,
      drawAnimationOverlay = !animateOverlay,
    )
    views.setImageViewBitmap(R.id.widget_clock_image, bitmap)

    val frameViewIds = intArrayOf(
      R.id.widget_anim_frame_0,
      R.id.widget_anim_frame_1,
      R.id.widget_anim_frame_2,
      R.id.widget_anim_frame_3,
      R.id.widget_anim_frame_4,
      R.id.widget_anim_frame_5,
      R.id.widget_anim_frame_6,
      R.id.widget_anim_frame_7,
    )

    if (animateOverlay) {
      val frames = ClockCanvasRenderer.renderAnimationOverlayFrames(
        config = config,
        widthDp = widthDp,
        heightDp = heightDp,
        density = density,
        frameCount = frameViewIds.size,
        maxBytesPerFrame = 32_000,
      )
      views.setViewVisibility(R.id.widget_anim_flipper, android.view.View.VISIBLE)
      val intervalMs = (135f / config.animationSpeed.coerceIn(0.4f, 2.2f)).toInt().coerceIn(70, 260)
      views.setInt(R.id.widget_anim_flipper, "setFlipInterval", intervalMs)
      for (i in frameViewIds.indices) {
        views.setImageViewBitmap(frameViewIds[i], frames[i])
      }
    } else {
      views.setViewVisibility(R.id.widget_anim_flipper, android.view.View.GONE)
    }

    val clickIntent = Intent(context, WidgetConfigureActivity::class.java).apply {
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingClick = PendingIntent.getActivity(
      context,
      appWidgetId,
      clickIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.widget_root, pendingClick)
    appWidgetManager.updateAppWidget(appWidgetId, views)
  }

  private fun resolveWidgetDpSize(
    options: Bundle,
    defaultW: Int,
    defaultH: Int,
  ): Pair<Int, Int> {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      @Suppress("DEPRECATION")
      val sizes = options.getParcelableArrayList<android.util.SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
      if (!sizes.isNullOrEmpty()) {
        val largest = sizes.maxByOrNull { it.width * it.height }
        if (largest != null && largest.width > 10f && largest.height > 10f) {
          return largest.width.toInt() to largest.height.toInt()
        }
      }
    }
    val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
    val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
    val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
    val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
    val w = if (minW > 0 && maxW > 0) (minW + maxW) / 2 else defaultW
    val h = if (minH > 0 && maxH > 0) (minH + maxH) / 2 else defaultH
    return w.coerceAtLeast(80) to h.coerceAtLeast(48)
  }
}
