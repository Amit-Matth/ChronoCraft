package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BackgroundFillMode
import com.example.model.CardStyle
import com.example.model.ClockCategory
import com.example.model.ClockStyleId
import com.example.model.ClockThemePack
import com.example.model.ColorPalettePreset
import com.example.model.DisplayMode
import com.example.model.WidgetConfig
import com.example.model.WidgetSizePreset
import com.example.ui.components.LiveClockCanvas
import com.example.ui.components.WallpaperBackdrop
import com.example.ui.components.rememberLiveClockMillis
import com.example.ui.viewmodel.ClockStudioUiState
import com.example.ui.viewmodel.StudioTab
import com.example.weather.GeocodingPlace
import java.util.Locale

@Composable
fun StudioScreen(
  uiState: ClockStudioUiState,
  onSelectTab: (StudioTab) -> Unit,
  onSelectDisplayMode: (DisplayMode) -> Unit = {},
  onSwitchStyle: (ClockStyleId, Boolean) -> Unit,
  onUpdateConfig: (WidgetConfig) -> Unit,
  onSelectCardStyle: (CardStyle) -> Unit = {},
  onSelectFillMode: (BackgroundFillMode, String?) -> Unit = { mode, _ ->
    val card = when (mode) {
      BackgroundFillMode.NONE -> CardStyle.TRANSPARENT
      BackgroundFillMode.GLASS -> CardStyle.FROSTED
      else -> CardStyle.SOLID
    }
    onSelectCardStyle(card)
  },
  onApplyPalette: (ColorPalettePreset) -> Unit,
  onApplyThemePack: (ClockThemePack) -> Unit = {},
  onResetDefaults: () -> Unit = {},
  onOpenBuilder: () -> Unit = {},
  onSetTimeTravel: (Boolean, Int?) -> Unit = { _, _ -> },
  onSelectPreviewSize: (WidgetSizePreset) -> Unit,
  onCycleWallpaper: () -> Unit,
  onOpenFullPreview: () -> Unit,
  onOpenAodExperience: () -> Unit = {},
  onSavePreset: (String) -> Unit,
  onPinWidget: () -> Unit,
  onApplyToActiveWidget: (Int) -> Unit,
  onClearEditingWidget: () -> Unit,
  onSearchCities: (String) -> Unit = {},
  onSelectCity: (GeocodingPlace) -> Unit = {},
  onUseLocationWeather: () -> Unit = {},
  onRefreshWeather: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val liveTimeMillis = rememberLiveClockMillis(1000L)
  val config = uiState.activeConfig
  val timeTravelMinutes = if (uiState.timeTravelEnabled) uiState.timeTravelMinutes else null
  var showSaveDialog by remember { mutableStateOf(false) }
  var presetNameInput by remember(config.styleId) { mutableStateOf(config.styleId.displayName) }
  var keepColorsOnSwitch by remember { mutableStateOf(true) }

  if (showSaveDialog) {
    AlertDialog(
      onDismissRequest = { showSaveDialog = false },
      title = { Text("Save Custom Preset") },
      text = {
        OutlinedTextField(
          value = presetNameInput,
          onValueChange = { presetNameInput = it },
          label = { Text("Preset Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("preset_name_input"),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onSavePreset(presetNameInput.trim())
            showSaveDialog = false
          },
          modifier = Modifier.testTag("confirm_save_preset_btn"),
        ) { Text("Save") }
      },
      dismissButton = {
        TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
      },
    )
  }

  BoxWithConstraints(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
  ) {
    val isTwoPane = maxWidth >= 700.dp
    val compactHeight = maxHeight < 660.dp

    @Composable
    fun EditingBanner() {
      if (uiState.editingAppWidgetId != null) {
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.primaryContainer)
              .padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "Editing Home Widget #${uiState.editingAppWidgetId}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          IconButton(onClick = onClearEditingWidget, modifier = Modifier.size(32.dp)) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "Exit editing",
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }
    }

    @Composable
    fun DisplayModeRow() {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        DisplayMode.entries.forEach { mode ->
          val selected = uiState.activeDisplayMode == mode
          val icon = when (mode) {
            DisplayMode.HOME_SCREEN -> Icons.Rounded.Home
            DisplayMode.LOCK_SCREEN -> Icons.Rounded.Lock
            DisplayMode.AOD -> Icons.Rounded.Bedtime
          }
          Row(
            modifier =
              Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onSelectDisplayMode(mode) }
                .testTag("display_mode_${mode.name.lowercase()}"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = icon,
              contentDescription = mode.label,
              tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = mode.shortLabel,
              style = MaterialTheme.typography.labelLarge,
              color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
            )
          }
        }
      }
    }

    @Composable
    fun PreviewStageCard(stageMod: Modifier = Modifier) {
      val stageHeight = when {
        isTwoPane -> 260.dp
        compactHeight -> 158.dp
        uiState.activeDisplayMode == DisplayMode.HOME_SCREEN && uiState.previewRows >= 3 -> 224.dp
        else -> 190.dp
      }
      Box(
        modifier =
          stageMod
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(stageHeight)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
      ) {
      if (uiState.activeDisplayMode == DisplayMode.AOD) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
      } else {
        WallpaperBackdrop(
          wallpaper = uiState.previewWallpaper,
          customPhotoUri = uiState.customWallpaperUri,
          modifier = Modifier.fillMaxSize(),
        )
        if (uiState.activeDisplayMode == DisplayMode.LOCK_SCREEN) {
          Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)))
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (uiState.activeDisplayMode == DisplayMode.HOME_SCREEN) {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            items(
              listOf(
                WidgetSizePreset.SQUARE_2X2,
                WidgetSizePreset.CLASSIC_4X2,
                WidgetSizePreset.EXPANDED_4X3,
                WidgetSizePreset.HERO_4X4,
              ),
            ) { preset ->
              val selected = uiState.previewCols == preset.cols && uiState.previewRows == preset.rows
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.55f),
                modifier =
                  Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectPreviewSize(preset) }
                    .testTag("studio_size_${preset.name.lowercase()}"),
              ) {
                Text(
                  text = "${preset.cols}×${preset.rows}",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White,
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                )
              }
            }
          }
        } else {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White.copy(alpha = if (uiState.activeDisplayMode == DisplayMode.AOD) 0.12f else 0.20f),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                imageVector = if (uiState.activeDisplayMode == DisplayMode.AOD) Icons.Rounded.Bedtime else Icons.Rounded.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp),
              )
              Text(
                text = if (uiState.activeDisplayMode == DisplayMode.AOD) "Low-Power AOD" else "Lock Screen",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
              )
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          if (timeTravelMinutes != null) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onSetTimeTravel(false, null) },
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(13.dp))
                Text(
                  text = String.format(Locale.US, "%02d:%02d", timeTravelMinutes / 60, timeTravelMinutes % 60),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSecondary,
                )
              }
            }
          }
          if (uiState.activeDisplayMode == DisplayMode.AOD) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.primary,
              modifier =
                Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .clickable(onClick = onOpenAodExperience)
                  .testTag("studio_launch_aod_btn"),
            ) {
              Text(
                text = "Launch AOD",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
              )
            }
          } else {
            Surface(
              shape = CircleShape,
              color = Color.Black.copy(alpha = 0.55f),
              modifier =
                Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .clickable(onClick = onCycleWallpaper)
                  .testTag("studio_cycle_wallpaper_btn"),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Wallpaper, contentDescription = "Switch preview wallpaper", tint = Color.White, modifier = Modifier.size(18.dp))
              }
            }
          }
          Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.55f),
            modifier =
              Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onOpenFullPreview)
                .testTag("studio_fullscreen_preview_btn"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Rounded.Fullscreen, contentDescription = "Open full preview", tint = Color.White, modifier = Modifier.size(20.dp))
            }
          }
        }
      }

      AnimatedContent(
        targetState = uiState.activeDisplayMode,
        transitionSpec = {
          if (config.animationsEnabled) {
            (fadeIn(tween(240)) + scaleIn(initialScale = 0.94f, animationSpec = tween(240)))
              .togetherWith(fadeOut(tween(180)))
          } else {
            fadeIn(tween(0)).togetherWith(fadeOut(tween(0)))
          }
        },
        modifier = Modifier.align(Alignment.Center).padding(top = 34.dp, bottom = 10.dp, start = 16.dp, end = 16.dp),
        label = "studio_display_mode_stage",
      ) { mode ->
        val isAnalog = config.styleId.category == ClockCategory.ANALOG || config.styleId == ClockStyleId.FLOATING_ANALOG_PURE
        val aspect = when (mode) {
          DisplayMode.HOME_SCREEN -> (uiState.previewCols.toFloat() / uiState.previewRows.toFloat()).coerceIn(0.8f, 3.8f)
          DisplayMode.LOCK_SCREEN -> if (isAnalog) 1.75f else 2.25f
          DisplayMode.AOD -> if (isAnalog) 1.75f else 2.45f
        }
        val previewH = when (mode) {
          DisplayMode.HOME_SCREEN -> when (uiState.previewRows) {
            1 -> 74.dp
            2 -> 128.dp
            else -> 166.dp
          }
          DisplayMode.LOCK_SCREEN -> 122.dp
          DisplayMode.AOD -> 114.dp
        }
        Box(
          modifier = Modifier.height(previewH).aspectRatio(aspect, matchHeightConstraintsFirst = true),
          contentAlignment = Alignment.Center,
        ) {
          LiveClockCanvas(
            config = config,
            timeMillis = liveTimeMillis,
            weather = uiState.weather,
            displayMode = mode,
            timeTravelMinutesOfDay = timeTravelMinutes,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
      }
    }

    @Composable
    fun QuickFillsRow() {
      val activeFill = config.effectiveFillMode(uiState.activeDisplayMode)
      val quickFills = if (uiState.activeDisplayMode == DisplayMode.AOD) {
        listOf(BackgroundFillMode.NONE, BackgroundFillMode.SOLID)
      } else {
        listOf(BackgroundFillMode.NONE, BackgroundFillMode.GLASS, BackgroundFillMode.SOLID, BackgroundFillMode.GRADIENT)
      }
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        quickFills.forEach { fillMode ->
          val selected = activeFill == fillMode
          Box(
            modifier =
              Modifier
                .weight(1f)
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                  if (selected) MaterialTheme.colorScheme.secondaryContainer
                  else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                )
                .border(
                  width = if (selected) 1.5.dp else 1.dp,
                  color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                  shape = RoundedCornerShape(12.dp),
                )
                .clickable { onSelectFillMode(fillMode, null) }
                .testTag("card_style_${fillMode.name.lowercase()}"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = fillMode.label,
              style = MaterialTheme.typography.labelLarge,
              color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
            )
          }
        }
      }
    }

    @Composable
    fun StudioTabsRow() {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        StudioTab.entries.forEach { tab ->
          val selected = uiState.activeStudioTab == tab
          Box(
            modifier =
              Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onSelectTab(tab) }
                .padding(vertical = 9.dp)
                .testTag("studio_tab_${tab.name.lowercase()}"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = tab.label,
              style = MaterialTheme.typography.labelLarge,
              color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
            )
          }
        }
      }
    }

    @Composable
    fun StudioTabHost(mod: Modifier = Modifier) {
      Box(modifier = mod) {
        AnimatedContent(targetState = uiState.activeStudioTab, label = "studio_tab_content") { tab ->
          when (tab) {
            StudioTab.STYLE ->
              StudioStyleTabContent(
                config = config,
                weather = uiState.weather,
                keepColors = keepColorsOnSwitch,
                liveTimeMillis = liveTimeMillis,
                onToggleKeepColors = { keepColorsOnSwitch = it },
                onSelectStyle = { onSwitchStyle(it, keepColorsOnSwitch) },
                onApplyThemePack = onApplyThemePack,
                onResetDefaults = onResetDefaults,
                onOpenBuilder = onOpenBuilder,
                onUpdateConfig = onUpdateConfig,
              )
            StudioTab.COLORS ->
              StudioColorsTabContent(
                config = config,
                activeDisplayMode = uiState.activeDisplayMode,
                wallpaperSuggestions = uiState.wallpaperSuggestions,
                onApplyPalette = onApplyPalette,
                onSelectFillMode = onSelectFillMode,
                onUpdateConfig = onUpdateConfig,
              )
            StudioTab.MOTION ->
              StudioMotionTabContent(
                config = config,
                timeTravelEnabled = uiState.timeTravelEnabled,
                timeTravelMinutes = uiState.timeTravelMinutes,
                onSetTimeTravel = onSetTimeTravel,
                onUpdateConfig = onUpdateConfig,
              )
            StudioTab.LAYOUT ->
              StudioLayoutTabContent(
                config = config,
                weather = uiState.weather,
                isLoadingWeather = uiState.isLoadingWeather,
                citySearchResults = uiState.citySearchResults,
                onUpdateConfig = onUpdateConfig,
                onSearchCities = onSearchCities,
                onSelectCity = onSelectCity,
                onUseLocationWeather = onUseLocationWeather,
                onRefreshWeather = onRefreshWeather,
              )
          }
        }
      }
    }

    @Composable
    fun StickyActionBar() {
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
            onClick = { showSaveDialog = true },
            modifier = Modifier.weight(0.9f).height(48.dp).testTag("studio_save_preset_btn"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save", maxLines = 1)
          }

          val editingId = uiState.editingAppWidgetId
          Button(
            onClick = {
              if (editingId != null) onApplyToActiveWidget(editingId) else onPinWidget()
            },
            modifier = Modifier.weight(1.4f).height(48.dp).testTag("studio_pin_widget_btn"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Rounded.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (editingId != null) "Update Widget #$editingId" else "Add to Home Screen",
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    if (isTwoPane) {
      Row(modifier = Modifier.fillMaxSize()) {
        Column(
          modifier = Modifier.weight(0.95f).fillMaxSize().padding(top = 8.dp),
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          Column {
            EditingBanner()
            DisplayModeRow()
            PreviewStageCard()
            QuickFillsRow()
          }
          StickyActionBar()
        }
        Column(
          modifier = Modifier.weight(1.05f).fillMaxSize().padding(top = 8.dp),
        ) {
          StudioTabsRow()
          StudioTabHost(mod = Modifier.weight(1f).fillMaxWidth())
        }
      }
    } else {
      Column(modifier = Modifier.fillMaxSize()) {
        EditingBanner()
        DisplayModeRow()
        PreviewStageCard()
        QuickFillsRow()
        StudioTabsRow()
        StudioTabHost(mod = Modifier.weight(1f).fillMaxWidth())
        StickyActionBar()
      }
    }
  }
}
