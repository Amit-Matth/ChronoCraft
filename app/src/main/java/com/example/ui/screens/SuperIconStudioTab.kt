package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AppIconOverride
import com.example.model.ExternalIconPackInfo
import com.example.model.IconPackConfig
import com.example.model.LaunchableAppInfo
import com.example.model.QuickShortcutAction
import com.example.model.ShortcutActionType
import com.example.model.SuperIconWidgetConfig

@Composable
fun SuperIconStudioTab(
  superConfig: SuperIconWidgetConfig,
  iconPackConfig: IconPackConfig,
  activeExternalPack: ExternalIconPackInfo?,
  installedApps: List<LaunchableAppInfo>,
  overrides: Map<String, AppIconOverride>,
  onSelectPrimaryApp: (LaunchableAppInfo) -> Unit,
  onUpdateSuperConfig: (SuperIconWidgetConfig) -> Unit,
  onPinSuperIcon: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showAddShortcutSheet by remember { mutableStateOf(false) }
  var showPickAppSheet by remember { mutableStateOf(false) }

  val primaryApp = remember(installedApps, superConfig.primaryPackage, superConfig.primaryActivity) {
    installedApps.firstOrNull {
      it.packageName == superConfig.primaryPackage && it.activityName == superConfig.primaryActivity
    } ?: installedApps.firstOrNull { it.packageName == superConfig.primaryPackage }
      ?: installedApps.firstOrNull()
  }

  if (showPickAppSheet) {
    PickInstalledAppSheet(
      title = "Choose Main Super Icon App",
      apps = installedApps,
      iconPackConfig = iconPackConfig,
      externalPack = activeExternalPack,
      overrides = overrides,
      onSelect = { app ->
        onSelectPrimaryApp(app)
        showPickAppSheet = false
      },
      onDismiss = { showPickAppSheet = false },
    )
  }

  if (showAddShortcutSheet) {
    AddQuickShortcutSheet(
      installedApps = installedApps,
      iconPackConfig = iconPackConfig,
      externalPack = activeExternalPack,
      overrides = overrides,
      onAddShortcut = { newShortcut ->
        val updated = superConfig.copy(
          shortcuts = (superConfig.shortcuts + newShortcut).take(4),
        )
        onUpdateSuperConfig(updated)
        showAddShortcutSheet = false
      },
      onDismiss = { showAddShortcutSheet = false },
    )
  }

  val cardSwatches = listOf(
    0xFF0F172A.toInt() to 0xFF38BDF8.toInt(),
    0xFF18181B.toInt() to 0xFFF43F5E.toInt(),
    0xFF064E3B.toInt() to 0xFF34D399.toInt(),
    0xFF312E81.toInt() to 0xFFC084FC.toInt(),
    0xFFF5E6D3.toInt() to 0xFFC2410C.toInt(),
  )

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("COLOROS 17 SUPER ICON TILE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(
          text = "Turn any app into an interactive 2×2 Home Screen Super Icon with up to 4 quick sub-shortcuts (e.g. Camera → Photo, Video, Selfie).",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    // Live ColorOS 17 Super Icon Mockup Card
    item {
      val bgColor = Color(superConfig.backgroundColor).copy(alpha = superConfig.cardAlpha)
      val accent = Color(superConfig.accentColor)
      val textCol = Color(superConfig.textColor)
      val primaryBmp = primaryApp?.let {
        rememberStyledAppBitmap(
          app = it,
          config = iconPackConfig,
          override = overrides[it.componentKey],
          externalPack = activeExternalPack,
          sizePx = 192,
        )
      }

      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
          shape = RoundedCornerShape(superConfig.cornerRadiusDp.dp),
          color = bgColor,
          modifier =
            Modifier
              .width(230.dp)
              .border(1.5.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(superConfig.cornerRadiusDp.dp)),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(
                modifier = Modifier.weight(0.95f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Box(modifier = Modifier.size(62.dp), contentAlignment = Alignment.Center) {
                  if (primaryBmp != null) {
                    Image(bitmap = primaryBmp.asImageBitmap(), contentDescription = superConfig.primaryLabel, modifier = Modifier.size(62.dp))
                  } else {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = accent, modifier = Modifier.size(40.dp))
                  }
                }
                Text(
                  text = superConfig.primaryLabel.ifBlank { primaryApp?.label ?: "Camera" },
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = textCol,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              Column(
                modifier = Modifier.weight(1.1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                superConfig.shortcuts.take(2).forEach { sc ->
                  SuperShortcutPill(sc = sc, accent = accent, textCol = textCol)
                }
              }
            }

            val bottomActions = superConfig.shortcuts.drop(2).take(2)
            if (bottomActions.isNotEmpty()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                bottomActions.forEach { sc ->
                  Box(modifier = Modifier.weight(1f)) {
                    SuperShortcutPill(sc = sc, accent = accent, textCol = textCol)
                  }
                }
              }
            }
          }
        }
      }
    }

    // Primary App Picker
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("PRIMARY APP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        OutlinedButton(
          onClick = { showPickAppSheet = true },
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
          modifier = Modifier.testTag("super_icon_pick_primary_btn"),
        ) {
          Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(5.dp))
          Text("All Apps (${installedApps.size})")
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(installedApps.take(12), key = { it.componentKey }) { app ->
          val selected = app.packageName == superConfig.primaryPackage
          val bmp = rememberStyledAppBitmap(
            app = app,
            config = iconPackConfig,
            override = overrides[app.componentKey],
            externalPack = activeExternalPack,
            sizePx = 96,
          )
          FilterChip(
            selected = selected,
            onClick = { onSelectPrimaryApp(app) },
            leadingIcon = {
              if (bmp != null) {
                Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(20.dp))
              }
            },
            label = { Text(app.label, maxLines = 1) },
          )
        }
      }
    }

    // Attached Multi-Shortcuts
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "ATTACHED SHORTCUTS (${superConfig.shortcuts.size}/4)",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        if (superConfig.shortcuts.size < 4) {
          FilledTonalButton(
            onClick = { showAddShortcutSheet = true },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.testTag("super_icon_add_shortcut_btn"),
          ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Shortcut")
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        superConfig.shortcuts.forEach { sc ->
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = shortcutVector(sc.actionType),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(sc.label, style = MaterialTheme.typography.titleSmall)
                Text(sc.actionType.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              IconButton(
                onClick = {
                  onUpdateSuperConfig(superConfig.copy(shortcuts = superConfig.shortcuts.filterNot { it.id == sc.id }))
                },
                modifier = Modifier.size(40.dp),
              ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove shortcut", tint = MaterialTheme.colorScheme.error)
              }
            }
          }
        }
      }
    }

    // Card Theme Swatches + Pin Super Icon CTA
    item {
      Text("TILE PALETTE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
      Spacer(modifier = Modifier.height(6.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        cardSwatches.forEach { (bg, acc) ->
          val selected = superConfig.backgroundColor == bg && superConfig.accentColor == acc
          val txt = if (bg == 0xFFF5E6D3.toInt()) 0xFF29170D.toInt() else 0xFFF8FAFC.toInt()
          Box(
            modifier =
              Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(bg))
                .border(if (selected) 2.5.dp else 1.5.dp, Color(acc), CircleShape)
                .clickable {
                  onUpdateSuperConfig(
                    superConfig.copy(backgroundColor = bg, accentColor = acc, textColor = txt),
                  )
                },
          )
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
      Button(
        onClick = onPinSuperIcon,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("super_icon_pin_widget_btn"),
        shape = RoundedCornerShape(16.dp),
      ) {
        Icon(Icons.Rounded.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Pin Super Icon Widget to Home Screen")
      }
    }
  }
}

@Composable
private fun SuperShortcutPill(
  sc: QuickShortcutAction,
  accent: Color,
  textCol: Color,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(Color.White.copy(alpha = 0.14f))
        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
        .padding(horizontal = 8.dp, vertical = 7.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Icon(
      imageVector = shortcutVector(sc.actionType),
      contentDescription = null,
      tint = accent,
      modifier = Modifier.size(15.dp),
    )
    Text(
      text = sc.label,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
      color = textCol,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

private fun shortcutVector(type: ShortcutActionType): ImageVector = when (type) {
  ShortcutActionType.CAMERA_PHOTO -> Icons.Rounded.CameraAlt
  ShortcutActionType.CAMERA_VIDEO -> Icons.Rounded.Videocam
  ShortcutActionType.CAMERA_SELFIE -> Icons.Rounded.Face
  ShortcutActionType.LAUNCH_APP -> Icons.Rounded.Apps
  ShortcutActionType.OPEN_URL -> Icons.Rounded.Language
  ShortcutActionType.WEB_SEARCH -> Icons.Rounded.Search
  ShortcutActionType.SETTINGS_WIFI -> Icons.Rounded.Wifi
  ShortcutActionType.SETTINGS_BLUETOOTH -> Icons.Rounded.Bluetooth
  ShortcutActionType.SET_ALARM -> Icons.Rounded.Alarm
  ShortcutActionType.SET_TIMER -> Icons.Rounded.Timer
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickInstalledAppSheet(
  title: String,
  apps: List<LaunchableAppInfo>,
  iconPackConfig: IconPackConfig,
  externalPack: ExternalIconPackInfo?,
  overrides: Map<String, AppIconOverride>,
  onSelect: (LaunchableAppInfo) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var query by remember { mutableStateOf("") }
  val filtered = remember(apps, query) {
    val q = query.trim().lowercase()
    if (q.isEmpty()) apps else apps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
  }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
      }
      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text("Search apps...") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )
      LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(filtered, key = { it.componentKey }) { app ->
          val bmp = rememberStyledAppBitmap(app, iconPackConfig, overrides[app.componentKey], externalPack, sizePx = 96)
          Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onSelect(app) }.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            if (bmp != null) Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(38.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(app.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddQuickShortcutSheet(
  installedApps: List<LaunchableAppInfo>,
  iconPackConfig: IconPackConfig,
  externalPack: ExternalIconPackInfo?,
  overrides: Map<String, AppIconOverride>,
  onAddShortcut: (QuickShortcutAction) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var customUrl by remember { mutableStateOf("") }
  var customLabel by remember { mutableStateOf("") }

  val presetActions = listOf(
    Triple("Photo Mode", ShortcutActionType.CAMERA_PHOTO, "Photo"),
    Triple("Video Mode", ShortcutActionType.CAMERA_VIDEO, "Video"),
    Triple("Selfie Cam", ShortcutActionType.CAMERA_SELFIE, "Selfie"),
    Triple("Web Search", ShortcutActionType.WEB_SEARCH, "Search"),
    Triple("New Alarm", ShortcutActionType.SET_ALARM, "Alarm"),
    Triple("Start Timer", ShortcutActionType.SET_TIMER, "Timer"),
    Triple("Wi-Fi Panel", ShortcutActionType.SETTINGS_WIFI, "Wi-Fi"),
    Triple("Bluetooth", ShortcutActionType.SETTINGS_BLUETOOTH, "Bluetooth"),
  )

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Add Quick Shortcut", style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
      }

      Text("CAMERA & SYSTEM ACTIONS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(presetActions) { (title, type, shortLabel) ->
          FilterChip(
            selected = false,
            onClick = {
              onAddShortcut(
                QuickShortcutAction(
                  id = "sc_${System.currentTimeMillis()}_${type.name}",
                  label = shortLabel,
                  actionType = type,
                ),
              )
            },
            leadingIcon = { Icon(shortcutVector(type), contentDescription = null, modifier = Modifier.size(16.dp)) },
            label = { Text(title) },
          )
        }
      }

      Text("OR ADD AN INSTALLED APP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(installedApps.take(18), key = { it.componentKey }) { app ->
          val bmp = rememberStyledAppBitmap(app, iconPackConfig, overrides[app.componentKey], externalPack, sizePx = 96)
          Column(
            modifier = Modifier.width(62.dp).clip(RoundedCornerShape(12.dp)).clickable {
              onAddShortcut(
                QuickShortcutAction(
                  id = "sc_app_${System.currentTimeMillis()}",
                  label = app.label.take(10),
                  actionType = ShortcutActionType.LAUNCH_APP,
                  targetPackage = app.packageName,
                  targetActivity = app.activityName,
                ),
              )
            }.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            if (bmp != null) Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp))
            Text(app.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }

      Text("OR CUSTOM WEBSITE / DEEP LINK", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
          value = customLabel,
          onValueChange = { customLabel = it.take(14) },
          placeholder = { Text("Label") },
          singleLine = true,
          modifier = Modifier.weight(0.8f),
        )
        OutlinedTextField(
          value = customUrl,
          onValueChange = { customUrl = it },
          placeholder = { Text("https://...") },
          singleLine = true,
          modifier = Modifier.weight(1.3f),
        )
      }
      Button(
        onClick = {
          onAddShortcut(
            QuickShortcutAction(
              id = "sc_url_${System.currentTimeMillis()}",
              label = customLabel.ifBlank { "Web" },
              actionType = ShortcutActionType.OPEN_URL,
              targetUriOrQuery = customUrl.trim(),
            ),
          )
        },
        enabled = customUrl.isNotBlank(),
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(14.dp),
      ) {
        Text("Attach Web Shortcut")
      }
    }
  }
}
