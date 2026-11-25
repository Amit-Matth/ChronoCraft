package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AppIconOverride
import com.example.model.DisplayMode
import com.example.model.ExternalIconPackInfo
import com.example.model.IconBadgeStyle
import com.example.model.IconFinishStyle
import com.example.model.IconPackConfig
import com.example.model.IconPackPreset
import com.example.model.IconPackPresets
import com.example.model.IconPackSourceType
import com.example.model.IconShapeStyle
import com.example.model.LaunchableAppInfo
import com.example.model.SuperIconWidgetConfig
import com.example.ui.components.ColorChannelEditor
import com.example.ui.components.LiveClockCanvas
import com.example.ui.components.WallpaperBackdrop
import com.example.ui.components.rememberLiveClockMillis
import com.example.ui.viewmodel.ClockStudioUiState
import kotlin.math.roundToInt

private enum class IconStudioTab(val label: String) {
  STYLE("Style"),
  SUPER_ICONS("Super Icons"),
  APPS("All Apps"),
  SHAPE("Shape & FX"),
  COLORS("Colors"),
}

@Composable
fun IconPackStudioScreen(
  uiState: ClockStudioUiState,
  onUpdateConfig: (IconPackConfig) -> Unit,
  onApplyPreset: (IconPackPreset) -> Unit,
  onSyncWithClock: () -> Unit,
  onRefreshApps: () -> Unit,
  onSaveAppOverride: (AppIconOverride) -> Unit,
  onClearAppOverride: (String) -> Unit,
  onPinAppToHome: (LaunchableAppInfo) -> Boolean,
  onSelectExternalPack: (String?) -> Unit,
  onImportZipPack: (Uri) -> Unit,
  onRemoveImportedPack: (String) -> Unit,
  onSelectSuperIconApp: (LaunchableAppInfo) -> Unit,
  onUpdateSuperIconConfig: (SuperIconWidgetConfig) -> Unit,
  onPinSuperIcon: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val liveTimeMillis = rememberLiveClockMillis(1000L)
  val config = uiState.iconPackConfig
  var activeTab by remember { mutableStateOf(IconStudioTab.STYLE) }
  var searchQuery by remember { mutableStateOf("") }
  var showCustomizedOnly by remember { mutableStateOf(false) }
  var editingApp by remember { mutableStateOf<LaunchableAppInfo?>(null) }
  var showBatchSheet by remember { mutableStateOf(false) }

  val allExternalPacks: List<ExternalIconPackInfo> = remember(uiState.installedApkIconPacks, uiState.importedIconPacks) {
    uiState.installedApkIconPacks + uiState.importedIconPacks
  }
  val activeExternalPack = remember(allExternalPacks, config.activeExternalPackId) {
    allExternalPacks.firstOrNull { it.packId == config.activeExternalPackId }
  }

  val zipPackLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      onImportZipPack(uri)
    }
  }

  val filteredApps = remember(uiState.installedApps, uiState.appIconOverrides, searchQuery, showCustomizedOnly) {
    val q = searchQuery.trim().lowercase()
    uiState.installedApps.filter { app ->
      val ov = uiState.appIconOverrides[app.componentKey]
      val matchesSearch = q.isEmpty() || app.label.lowercase().contains(q) || app.packageName.lowercase().contains(q)
      val matchesFilter = !showCustomizedOnly || ov != null
      matchesSearch && matchesFilter
    }
  }

  editingApp?.let { app ->
    AppIconEditorSheet(
      app = app,
      config = config,
      externalPack = activeExternalPack,
      existingOverride = uiState.appIconOverrides[app.componentKey],
      onDismiss = { editingApp = null },
      onSaveOverride = onSaveAppOverride,
      onClearOverride = { onClearAppOverride(app.componentKey) },
      onPinToHome = { onPinAppToHome(app) },
    )
  }

  if (showBatchSheet) {
    BatchApplyIconsSheet(
      apps = filteredApps.ifEmpty { uiState.installedApps },
      config = config,
      externalPack = activeExternalPack,
      overrides = uiState.appIconOverrides,
      onPinApp = onPinAppToHome,
      onDismiss = { showBatchSheet = false },
    )
  }

  BoxWithConstraints(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
  ) {
    val isTwoPane = maxWidth >= 720.dp
    val compactHeight = maxHeight < 650.dp

    @Composable
    fun HeaderRow() {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Icon Pack Studio",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${config.packName} • ${uiState.installedApps.size} apps ready",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilledTonalButton(
            onClick = onSyncWithClock,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(40.dp).testTag("icons_sync_clock_btn"),
          ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text("Match Clock", maxLines = 1)
          }
        }
      }
    }

    @Composable
    fun HomePreviewStage() {
      val sampleApps = remember(uiState.installedApps) { uiState.installedApps.take(4) }
      val stageH = if (isTwoPane) 270.dp else if (compactHeight) 168.dp else 196.dp
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(stageH)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
      ) {
        WallpaperBackdrop(
          wallpaper = uiState.previewWallpaper,
          customPhotoUri = uiState.customWallpaperUri,
          modifier = Modifier.fillMaxSize(),
        )
        Column(
          modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
          ) {
            LiveClockCanvas(
              config = uiState.activeConfig,
              timeMillis = liveTimeMillis,
              weather = uiState.weather,
              displayMode = DisplayMode.HOME_SCREEN,
              modifier = Modifier.fillMaxSize(),
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          if (sampleApps.isEmpty()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
              listOf("Clock", "Camera", "Music", "Browser").forEach { name ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Box(
                    modifier =
                      Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(config.backgroundColor))
                        .border(1.2.dp, Color(config.borderColor), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(Icons.Rounded.Apps, contentDescription = null, tint = Color(config.iconTintColor), modifier = Modifier.size(22.dp))
                  }
                  if (config.showHomeLabels) {
                    Text(name, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1)
                  }
                }
              }
            }
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              sampleApps.forEach { app ->
                val ov = uiState.appIconOverrides[app.componentKey]
                val bmp = rememberStyledAppBitmap(
                  app = app,
                  config = config,
                  override = ov,
                  externalPack = activeExternalPack,
                  sizePx = 128,
                )
                Column(
                  modifier =
                    Modifier
                      .width(64.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .clickable { editingApp = app }
                      .padding(vertical = 2.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    if (bmp != null) {
                      Image(bitmap = bmp.asImageBitmap(), contentDescription = app.label, modifier = Modifier.size(48.dp))
                    }
                  }
                  if (config.showHomeLabels) {
                    Text(
                      text = ov?.customLabel?.takeIf { it.isNotBlank() } ?: app.label,
                      style = MaterialTheme.typography.labelSmall,
                      color = Color.White,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    @Composable
    fun TabsStrip() {
      LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(IconStudioTab.entries) { tab ->
          val selected = activeTab == tab
          FilterChip(
            selected = selected,
            onClick = { activeTab = tab },
            label = {
              Text(
                if (tab == IconStudioTab.APPS) "${tab.label} (${uiState.installedApps.size})" else tab.label,
              )
            },
            leadingIcon = {
              val icon = when (tab) {
                IconStudioTab.APPS -> Icons.Rounded.Apps
                IconStudioTab.STYLE -> Icons.Rounded.AutoAwesome
                IconStudioTab.SUPER_ICONS -> Icons.Rounded.DashboardCustomize
                IconStudioTab.SHAPE -> Icons.Rounded.Tune
                IconStudioTab.COLORS -> Icons.Rounded.Palette
              }
              Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            modifier = Modifier.testTag("icons_tab_${tab.name.lowercase()}"),
          )
        }
      }
    }

    @Composable
    fun TabBody(mod: Modifier = Modifier) {
      when (activeTab) {
        IconStudioTab.STYLE -> {
          LazyColumn(
            modifier = mod,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            item {
              Text("CURATED ICON PACKS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.height(6.dp))
              LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(IconPackPresets.presets, key = { it.id }) { preset ->
                  val selected = config.packName == preset.name
                  Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    tonalElevation = if (selected) 4.dp else 1.dp,
                    modifier =
                      Modifier
                        .width(142.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                          width = if (selected) 2.dp else 1.dp,
                          color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                          shape = RoundedCornerShape(18.dp),
                        )
                        .clickable { onApplyPreset(preset) }
                        .testTag("icon_preset_${preset.id}"),
                  ) {
                    Column(
                      modifier = Modifier.padding(12.dp),
                      verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                          modifier =
                            Modifier
                              .size(26.dp)
                              .clip(RoundedCornerShape(8.dp))
                              .background(Color(preset.config.backgroundColor))
                              .border(1.2.dp, Color(preset.config.borderColor), RoundedCornerShape(8.dp)),
                          contentAlignment = Alignment.Center,
                        ) {
                          Box(
                            modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(preset.config.iconTintColor)),
                          )
                        }
                        Box(
                          modifier =
                            Modifier
                              .size(26.dp)
                              .clip(CircleShape)
                              .background(Color(preset.config.secondaryColor))
                              .border(1.2.dp, Color(preset.config.borderColor), CircleShape),
                        )
                      }
                      Text(preset.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                      Text(preset.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                  }
                }
              }
            }

            item {
              Text("SURFACE FINISH", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.height(6.dp))
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(IconFinishStyle.entries) { finish ->
                  FilterChip(
                    selected = config.finishStyle == finish,
                    onClick = { onUpdateConfig(config.copy(finishStyle = finish)) },
                    label = { Text(finish.label) },
                    modifier = Modifier.testTag("icon_finish_${finish.name.lowercase()}"),
                  )
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = config.finishStyle.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            item {
              Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  IconSwitchRow("Monochrome Tinted Glyphs", "Unifies all app logos to your tint color", config.monochromeGlyph) {
                    onUpdateConfig(config.copy(monochromeGlyph = it))
                  }
                  IconSwitchRow("Dynamic App Accent", "Pulls glow & trim hue from each app icon", config.useDynamicAppColor) {
                    onUpdateConfig(config.copy(useDynamicAppColor = it))
                  }
                  IconSwitchRow("Show App Labels", "Display titles under home screen shortcuts", config.showHomeLabels) {
                    onUpdateConfig(config.copy(showHomeLabels = it))
                  }
                }
              }
            }

            item {
              Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text("INSTALLED & CUSTOM ICON PACKS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                      Text(
                        text = if (activeExternalPack != null) "Active: ${activeExternalPack.name}" else "Active: ChronoCraft Native Vector",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                      IconButton(
                        onClick = onRefreshApps,
                        modifier = Modifier.size(38.dp),
                      ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Rescan installed icon packs")
                      }
                      FilledTonalButton(
                        onClick = {
                          zipPackLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*"))
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(38.dp).testTag("icons_import_zip_btn"),
                      ) {
                        Icon(Icons.Rounded.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Import ZIP", maxLines = 1)
                      }
                    }
                  }

                  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                      FilterChip(
                        selected = config.activeExternalPackId == null,
                        onClick = { onSelectExternalPack(null) },
                        label = { Text("ChronoCraft Native") },
                        modifier = Modifier.testTag("ext_pack_native"),
                      )
                    }
                    items(allExternalPacks, key = { it.packId }) { pack ->
                      val selected = config.activeExternalPackId == pack.packId
                      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        FilterChip(
                          selected = selected,
                          onClick = { onSelectExternalPack(pack.packId) },
                          label = {
                            val badge = if (pack.sourceType == IconPackSourceType.IMPORTED_ZIP && pack.iconCount > 0) {
                              "${pack.name} (${pack.iconCount})"
                            } else {
                              pack.name
                            }
                            Text(badge, maxLines = 1)
                          },
                          modifier = Modifier.testTag("ext_pack_${pack.packId}"),
                        )
                        if (pack.sourceType == IconPackSourceType.IMPORTED_ZIP) {
                          IconButton(
                            onClick = { onRemoveImportedPack(pack.packId) },
                            modifier = Modifier.size(32.dp),
                          ) {
                            Icon(
                              Icons.Rounded.DeleteOutline,
                              contentDescription = "Remove ${pack.name}",
                              tint = MaterialTheme.colorScheme.error,
                              modifier = Modifier.size(16.dp),
                            )
                          }
                        }
                      }
                    }
                  }

                  Text(
                    text = if (allExternalPacks.isEmpty()) {
                      "No 3rd-party APK icon packs detected yet. Install any Nova/ADW icon pack from Play Store or tap Import ZIP above to load your own PNG/WebP icon pack."
                    } else {
                      "Tap any installed or imported pack to apply its icons across All Apps, Super Icons, and Home Screen shortcuts."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }

        IconStudioTab.SUPER_ICONS -> {
          SuperIconStudioTab(
            superConfig = uiState.superIconConfig,
            iconPackConfig = config,
            activeExternalPack = activeExternalPack,
            installedApps = uiState.installedApps,
            overrides = uiState.appIconOverrides,
            onSelectPrimaryApp = onSelectSuperIconApp,
            onUpdateSuperConfig = onUpdateSuperIconConfig,
            onPinSuperIcon = onPinSuperIcon,
            modifier = mod,
          )
        }

        IconStudioTab.SHAPE -> {
          LazyColumn(
            modifier = mod,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            item {
              Text("ICON MASK SHAPE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.height(6.dp))
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(IconShapeStyle.entries) { shape ->
                  FilterChip(
                    selected = config.shapeStyle == shape,
                    onClick = { onUpdateConfig(config.copy(shapeStyle = shape)) },
                    label = { Text(shape.label) },
                    modifier = Modifier.testTag("icon_shape_${shape.name.lowercase()}"),
                  )
                }
              }
            }

            item {
              Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Inner Glyph Scale", style = MaterialTheme.typography.bodyMedium)
                    Text("${(config.iconScale * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                  }
                  Slider(
                    value = config.iconScale,
                    onValueChange = { onUpdateConfig(config.copy(iconScale = it)) },
                    valueRange = 0.45f..0.85f,
                  )

                  if (config.shapeStyle == IconShapeStyle.ROUNDED_SQUARE) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                      Text("Corner Roundness", style = MaterialTheme.typography.bodyMedium)
                      Text("${(config.cornerRadiusRatio * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                      value = config.cornerRadiusRatio,
                      onValueChange = { onUpdateConfig(config.copy(cornerRadiusRatio = it)) },
                      valueRange = 0.12f..0.46f,
                    )
                  }

                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Border Thickness", style = MaterialTheme.typography.bodyMedium)
                    Text(String.format("%.1f dp", config.borderWidthDp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                  }
                  Slider(
                    value = config.borderWidthDp,
                    onValueChange = { onUpdateConfig(config.copy(borderWidthDp = it)) },
                    valueRange = 0f..3.6f,
                  )
                }
              }
            }

            item {
              Text("BADGE & EFFECTS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.height(6.dp))
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(IconBadgeStyle.entries) { badge ->
                  FilterChip(
                    selected = config.badgeStyle == badge,
                    onClick = { onUpdateConfig(config.copy(badgeStyle = badge)) },
                    label = { Text(badge.label) },
                  )
                }
              }
              Spacer(modifier = Modifier.height(8.dp))
              Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  IconSwitchRow("Drop Shadow", "Subtle elevation shadow behind icon tiles", config.showDropShadow) {
                    onUpdateConfig(config.copy(showDropShadow = it))
                  }
                  IconSwitchRow("Neon Halo Glow", "Luminous outer aura on Neon & Gradient styles", config.showGlowHalo) {
                    onUpdateConfig(config.copy(showGlowHalo = it))
                  }
                }
              }
            }
          }
        }

        IconStudioTab.COLORS -> {
          LazyColumn(
            modifier = mod,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            item {
              ColorChannelEditor(
                title = "Tile Background",
                selectedColor = config.backgroundColor,
                onColorChange = { onUpdateConfig(config.copy(backgroundColor = it)) },
              )
            }
            item {
              ColorChannelEditor(
                title = "Secondary / Gradient",
                selectedColor = config.secondaryColor,
                onColorChange = { onUpdateConfig(config.copy(secondaryColor = it)) },
              )
            }
            item {
              ColorChannelEditor(
                title = "Icon Glyph Tint",
                selectedColor = config.iconTintColor,
                onColorChange = { onUpdateConfig(config.copy(iconTintColor = it)) },
              )
            }
            item {
              ColorChannelEditor(
                title = "Border & Glow Trim",
                selectedColor = config.borderColor,
                onColorChange = { onUpdateConfig(config.copy(borderColor = it)) },
              )
            }
          }
        }

        IconStudioTab.APPS -> {
          Column(modifier = mod) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search ${uiState.installedApps.size} installed apps...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                  {
                    IconButton(onClick = { searchQuery = "" }) {
                      Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                    }
                  }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).testTag("icons_search_input"),
              )
              IconButton(
                onClick = onRefreshApps,
                modifier = Modifier.size(48.dp),
              ) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Reload installed apps")
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              FilterChip(
                selected = showCustomizedOnly,
                onClick = { showCustomizedOnly = !showCustomizedOnly },
                label = { Text("Customized (${uiState.appIconOverrides.size})") },
              )
              Text(
                text = "Tap any app to customize or add to Home",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            if (uiState.isLoadingApps && filteredApps.isEmpty()) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
              }
            } else if (filteredApps.isEmpty()) {
              Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                  text = "No matching apps found. Clear your search or reload installed apps.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center,
                )
              }
            } else {
              LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 86.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
              ) {
                items(filteredApps, key = { it.componentKey }) { app ->
                  val ov = uiState.appIconOverrides[app.componentKey]
                  val bmp = rememberStyledAppBitmap(
                    app = app,
                    config = config,
                    override = ov,
                    externalPack = activeExternalPack,
                    sizePx = 144,
                  )
                  Column(
                    modifier =
                      Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { editingApp = app }
                        .padding(6.dp)
                        .testTag("icon_app_${app.packageName}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                  ) {
                    Box(modifier = Modifier.size(62.dp), contentAlignment = Alignment.Center) {
                      if (bmp != null) {
                        Image(bitmap = bmp.asImageBitmap(), contentDescription = app.label, modifier = Modifier.size(60.dp))
                      }
                      if (ov?.isPinnedToHome == true) {
                        Icon(
                          imageVector = Icons.Rounded.CheckCircle,
                          contentDescription = "Added to Home",
                          tint = MaterialTheme.colorScheme.primary,
                          modifier =
                            Modifier
                              .align(Alignment.BottomEnd)
                              .size(18.dp)
                              .clip(CircleShape)
                              .background(MaterialTheme.colorScheme.surface),
                        )
                      }
                    }
                    Text(
                      text = ov?.customLabel?.takeIf { it.isNotBlank() } ?: app.label,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      textAlign = TextAlign.Center,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    @Composable
    fun StickyApplyBar() {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedButton(
            onClick = { activeTab = IconStudioTab.APPS },
            modifier = Modifier.weight(0.95f).height(48.dp).testTag("icons_browse_apps_btn"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Rounded.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Pick Apps", maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          Button(
            onClick = { showBatchSheet = true },
            enabled = uiState.installedApps.isNotEmpty(),
            modifier = Modifier.weight(1.35f).height(48.dp).testTag("icons_batch_apply_btn"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Rounded.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Apply Icons to Home", maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }
    }

    if (isTwoPane) {
      Row(modifier = Modifier.fillMaxSize()) {
        Column(
          modifier = Modifier.weight(0.95f).fillMaxHeight(),
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          Column {
            HeaderRow()
            HomePreviewStage()
          }
          if (activeTab != IconStudioTab.SUPER_ICONS) {
            StickyApplyBar()
          }
        }
        Column(modifier = Modifier.weight(1.05f).fillMaxHeight().padding(top = 6.dp)) {
          TabsStrip()
          TabBody(mod = Modifier.weight(1f).fillMaxWidth())
        }
      }
    } else {
      Column(modifier = Modifier.fillMaxSize()) {
        HeaderRow()
        HomePreviewStage()
        TabsStrip()
        TabBody(mod = Modifier.weight(1f).fillMaxWidth())
        if (activeTab != IconStudioTab.SUPER_ICONS) {
          StickyApplyBar()
        }
      }
    }
  }
}

@Composable
private fun IconSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
      Text(title, style = MaterialTheme.typography.titleSmall)
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}
