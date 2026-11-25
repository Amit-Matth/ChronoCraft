package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import com.example.MainActivity
import com.example.R
import com.example.data.WidgetConfigRepository
import com.example.model.QuickShortcutAction
import com.example.model.ShortcutActionType
import com.example.model.SuperIconWidgetConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChronoSuperIconWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        for (id in appWidgetIds) {
          SuperIconWidgetUpdater.updateSuperIconWidget(context.applicationContext, appWidgetManager, id)
        }
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
          repo.deleteSuperIconConfig(id)
        }
      } finally {
        pendingResult.finish()
      }
    }
  }
}

class PinSuperIconReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val repo = WidgetConfigRepository(context.applicationContext)
        val pending = repo.consumePendingSuperIconConfig()
        if (pending != null) {
          repo.saveSuperIconConfig(appWidgetId, pending.copy(widgetId = appWidgetId))
        }
        val mgr = AppWidgetManager.getInstance(context.applicationContext)
        SuperIconWidgetUpdater.updateSuperIconWidget(context.applicationContext, mgr, appWidgetId)
      } finally {
        pendingResult.finish()
      }
    }
  }
}

object SuperIconWidgetUpdater {

  suspend fun updateSuperIconWidget(
    context: Context,
    mgr: AppWidgetManager,
    appWidgetId: Int,
  ) {
    val repo = WidgetConfigRepository(context)
    val cfg = repo.getSuperIconConfig(appWidgetId) ?: repo.superIconDraftFlow.first()
    val iconPackCfg = repo.iconPackConfigFlow.first()
    val overrides = repo.appIconOverridesFlow.first()
    val externalPacks = IconPackImporter.discoverInstalledIconPacks(context) + repo.importedPacksFlow.first()
    val activeExtPack = iconPackCfg.activeExternalPackId?.let { id -> externalPacks.firstOrNull { it.packId == id } }

    val views = RemoteViews(context.packageName, R.layout.widget_super_icon)

    // Render sleek ColorOS-style glass/gradient backdrop bitmap
    val bgBitmap = renderSuperCardBg(cfg)
    views.setImageViewBitmap(R.id.super_icon_bg, bgBitmap)

    // Render styled primary app icon
    val apps = IconPackRenderer.loadLaunchableApps(context)
    val matchedApp = apps.firstOrNull { it.packageName == cfg.primaryPackage && it.activityName == cfg.primaryActivity }
      ?: apps.firstOrNull { it.packageName == cfg.primaryPackage }
      ?: apps.firstOrNull()

    val primaryBmp = if (matchedApp != null) {
      val ov = overrides[matchedApp.componentKey]
      val extBmp = IconPackImporter.resolveExternalIconBitmap(context, activeExtPack, matchedApp, ov, 192)
      val raw = extBmp ?: IconPackRenderer.loadAppIconBitmap(context, matchedApp.packageName, matchedApp.activityName, 192)
      val photo = ov?.customPhotoUri?.let { IconPackRenderer.loadCustomPhotoBitmap(context, it, 192) }
      IconPackRenderer.renderStyledIconBitmap(raw, photo, iconPackCfg, matchedApp.dominantColor, ov, 224)
    } else {
      renderShortcutGlyphBitmap(ShortcutActionType.CAMERA_PHOTO, cfg.accentColor, 192)
    }

    views.setImageViewBitmap(R.id.super_icon_primary_image, primaryBmp)
    views.setTextViewText(R.id.super_icon_primary_label, cfg.primaryLabel.ifBlank { matchedApp?.label ?: "App" })
    views.setTextColor(R.id.super_icon_primary_label, cfg.textColor)

    val primaryIntent = if (matchedApp != null) {
      Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        component = ComponentName(matchedApp.packageName, matchedApp.activityName)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
      }
    } else {
      Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
    }
    views.setOnClickPendingIntent(
      R.id.super_icon_primary_btn,
      PendingIntent.getActivity(
        context,
        appWidgetId * 100 + 99,
        primaryIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      ),
    )

    val slotContainers = intArrayOf(R.id.super_slot_0, R.id.super_slot_1, R.id.super_slot_2, R.id.super_slot_3)
    val slotIcons = intArrayOf(R.id.super_slot_icon_0, R.id.super_slot_icon_1, R.id.super_slot_icon_2, R.id.super_slot_icon_3)
    val slotLabels = intArrayOf(R.id.super_slot_label_0, R.id.super_slot_label_1, R.id.super_slot_label_2, R.id.super_slot_label_3)

    val actions = cfg.shortcuts.take(4)
    views.setViewVisibility(R.id.super_bottom_row, if (actions.size > 2) View.VISIBLE else View.GONE)

    for (i in 0 until 4) {
      val action = actions.getOrNull(i)
      if (action == null) {
        views.setViewVisibility(slotContainers[i], View.GONE)
      } else {
        views.setViewVisibility(slotContainers[i], View.VISIBLE)
        views.setTextViewText(slotLabels[i], action.label)
        views.setTextColor(slotLabels[i], cfg.textColor)

        val glyphBmp = if (action.actionType == ShortcutActionType.LAUNCH_APP && !action.targetPackage.isNullOrBlank()) {
          val subApp = apps.firstOrNull { it.packageName == action.targetPackage }
          if (subApp != null) {
            val ov = overrides[subApp.componentKey]
            val ext = IconPackImporter.resolveExternalIconBitmap(context, activeExtPack, subApp, ov, 96)
            val raw = ext ?: IconPackRenderer.loadAppIconBitmap(context, subApp.packageName, subApp.activityName, 96)
            IconPackRenderer.renderStyledIconBitmap(raw, null, iconPackCfg, subApp.dominantColor, ov, 96)
          } else {
            renderShortcutGlyphBitmap(action.actionType, cfg.accentColor, 96)
          }
        } else {
          renderShortcutGlyphBitmap(action.actionType, cfg.accentColor, 96)
        }
        views.setImageViewBitmap(slotIcons[i], glyphBmp)

        val actionIntent = buildShortcutIntent(context, action)
        views.setOnClickPendingIntent(
          slotContainers[i],
          PendingIntent.getActivity(
            context,
            appWidgetId * 100 + i,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
          ),
        )
      }
    }

    mgr.updateAppWidget(appWidgetId, views)
  }

  fun buildShortcutIntent(context: Context, action: QuickShortcutAction): Intent {
    val intent = when (action.actionType) {
      ShortcutActionType.CAMERA_PHOTO -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
      ShortcutActionType.CAMERA_VIDEO -> Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA)
      ShortcutActionType.CAMERA_SELFIE -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
        putExtra("android.intent.extras.CAMERA_FACING", 1)
        putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
        putExtra("com.google.assistant.extra.USE_FRONT_CAMERA", true)
      }
      ShortcutActionType.LAUNCH_APP -> {
        val pkg = action.targetPackage
        val cls = action.targetActivity
        if (!pkg.isNullOrBlank() && !cls.isNullOrBlank()) {
          Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(pkg, cls)
          }
        } else if (!pkg.isNullOrBlank()) {
          context.packageManager.getLaunchIntentForPackage(pkg) ?: Intent(context, MainActivity::class.java)
        } else {
          Intent(context, MainActivity::class.java)
        }
      }
      ShortcutActionType.OPEN_URL -> {
        val rawUrl = action.targetUriOrQuery?.trim().orEmpty().ifBlank { "https://www.google.com" }
        val normalized = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) rawUrl else "https://$rawUrl"
        Intent(Intent.ACTION_VIEW, Uri.parse(normalized))
      }
      ShortcutActionType.WEB_SEARCH -> Intent(Intent.ACTION_WEB_SEARCH)
      ShortcutActionType.SETTINGS_WIFI -> Intent(Settings.ACTION_WIFI_SETTINGS)
      ShortcutActionType.SETTINGS_BLUETOOTH -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
      ShortcutActionType.SET_ALARM -> Intent(AlarmClock.ACTION_SET_ALARM)
      ShortcutActionType.SET_TIMER -> Intent(AlarmClock.ACTION_SET_TIMER)
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    return intent
  }

  private fun renderSuperCardBg(cfg: SuperIconWidgetConfig): Bitmap {
    val w = 360
    val h = 360
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val rect = RectF(2f, 2f, w - 2f, h - 2f)
    val radius = (cfg.cornerRadiusDp * 2.1f).coerceIn(18f, 68f)
    val alpha = (cfg.cardAlpha.coerceIn(0.15f, 1f) * 255).toInt()
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = LinearGradient(
        0f,
        0f,
        w.toFloat(),
        h.toFloat(),
        ColorUtils.setAlphaComponent(cfg.backgroundColor, alpha),
        ColorUtils.setAlphaComponent(ColorUtils.blendARGB(cfg.backgroundColor, cfg.accentColor, 0.20f), alpha),
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawRoundRect(rect, radius, radius, bgPaint)
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 3f
      color = ColorUtils.setAlphaComponent(cfg.accentColor, 130)
    }
    canvas.drawRoundRect(rect, radius, radius, borderPaint)
    return bmp
  }

  private fun renderShortcutGlyphBitmap(type: ShortcutActionType, tint: Int, size: Int): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = tint
      style = Paint.Style.STROKE
      strokeWidth = size * 0.10f
      strokeCap = Paint.Cap.ROUND
      strokeJoin = Paint.Join.ROUND
    }
    val cx = size / 2f
    val cy = size / 2f
    when (type) {
      ShortcutActionType.CAMERA_PHOTO -> {
        canvas.drawRoundRect(RectF(size * 0.16f, size * 0.28f, size * 0.84f, size * 0.78f), size * 0.10f, size * 0.10f, p)
        canvas.drawCircle(cx, cy + size * 0.04f, size * 0.15f, p)
      }
      ShortcutActionType.CAMERA_VIDEO -> {
        canvas.drawRoundRect(RectF(size * 0.14f, size * 0.28f, size * 0.64f, size * 0.72f), size * 0.09f, size * 0.09f, p)
        val tri = Path().apply {
          moveTo(size * 0.68f, size * 0.42f)
          lineTo(size * 0.86f, size * 0.30f)
          lineTo(size * 0.86f, size * 0.70f)
          lineTo(size * 0.68f, size * 0.58f)
          close()
        }
        canvas.drawPath(tri, p)
      }
      ShortcutActionType.CAMERA_SELFIE -> {
        canvas.drawCircle(cx, cy - size * 0.10f, size * 0.16f, p)
        canvas.drawArc(RectF(size * 0.22f, size * 0.50f, size * 0.78f, size * 0.92f), 190f, 160f, false, p)
      }
      ShortcutActionType.WEB_SEARCH, ShortcutActionType.OPEN_URL -> {
        canvas.drawCircle(cx - size * 0.06f, cy - size * 0.06f, size * 0.24f, p)
        canvas.drawLine(cx + size * 0.11f, cy + size * 0.11f, size * 0.82f, size * 0.82f, p)
      }
      ShortcutActionType.SETTINGS_WIFI -> {
        canvas.drawArc(RectF(size * 0.16f, size * 0.26f, size * 0.84f, size * 0.94f), 225f, 90f, false, p)
        canvas.drawArc(RectF(size * 0.30f, size * 0.42f, size * 0.70f, size * 0.82f), 225f, 90f, false, p)
        canvas.drawCircle(cx, size * 0.74f, size * 0.05f, p)
      }
      ShortcutActionType.SET_ALARM, ShortcutActionType.SET_TIMER -> {
        canvas.drawCircle(cx, cy + size * 0.04f, size * 0.30f, p)
        canvas.drawLine(cx, cy + size * 0.04f, cx, cy - size * 0.12f, p)
        canvas.drawLine(cx, cy + size * 0.04f, cx + size * 0.14f, cy + size * 0.08f, p)
      }
      else -> {
        canvas.drawRoundRect(RectF(size * 0.20f, size * 0.20f, size * 0.80f, size * 0.80f), size * 0.14f, size * 0.14f, p)
        canvas.drawCircle(cx, cy, size * 0.09f, p)
      }
    }
    return bmp
  }
}
