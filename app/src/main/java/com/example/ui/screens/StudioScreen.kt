package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.ClockFontId
import com.example.model.ClockPresets
import com.example.model.ClockStyleId
import com.example.model.ColorPalettePreset
import com.example.model.WeatherSnapshot
import com.example.model.WidgetConfig
import com.example.model.WidgetSizePreset
import com.example.ui.components.ColorChannelEditor
import com.example.ui.components.LiveClockCanvas
import com.example.ui.components.WallpaperBackdrop
import com.example.ui.components.rememberLiveClockMillis
import com.example.ui.viewmodel.ClockStudioUiState
import com.example.ui.viewmodel.StudioTab
import com.example.weather.GeocodingPlace
import kotlin.math.roundToInt

@Composable
fun StudioScreen(
  uiState: ClockStudioUiState,
  onSelectTab: (StudioTab) -> Unit,
  onSwitchStyle: (ClockStyleId, Boolean) -> Unit,
  onUpdateConfig: (WidgetConfig) -> Unit,
  onApplyPalette: (ColorPalettePreset) -> Unit,
  onSelectPreviewSize: (WidgetSizePreset) -> Unit,
  onCycleWallpaper: () -> Unit,
  onOpenFullPreview: () -> Unit,
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

  Column(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
  ) {
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

    // Live Interactive Preview Stage (supports Expandable sizes 2x2, 4x2, 4x3, 4x4)
    val stageHeight = if (uiState.previewRows >= 3) 244.dp else 210.dp
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp)
          .height(stageHeight)
          .clip(RoundedCornerShape(24.dp))
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
    ) {
      WallpaperBackdrop(
        wallpaper = uiState.previewWallpaper,
        customPhotoUri = uiState.customWallpaperUri,
        modifier = Modifier.fillMaxSize(),
      )

      Row(
        modifier = Modifier.fillMaxWidth().padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f),
        ) {
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

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
              Icon(
                imageVector = Icons.Rounded.Wallpaper,
                contentDescription = "Switch preview wallpaper",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
              )
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
              Icon(
                imageVector = Icons.Rounded.Fullscreen,
                contentDescription = "Open full preview",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }
      }

      val aspect = (uiState.previewCols.toFloat() / uiState.previewRows.toFloat()).coerceIn(0.8f, 3.8f)
      val previewH = when (uiState.previewRows) {
        1 -> 74.dp
        2 -> 134.dp
        else -> 174.dp
      }
      Box(
        modifier =
          Modifier
            .align(Alignment.Center)
            .padding(top = 34.dp, bottom = 10.dp, start = 16.dp, end = 16.dp)
            .height(previewH)
            .aspectRatio(aspect, matchHeightConstraintsFirst = true),
        contentAlignment = Alignment.Center,
      ) {
        LiveClockCanvas(
          config = config,
          timeMillis = liveTimeMillis,
          weather = uiState.weather,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }

    // Studio Tab Bar
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
          )
        }
      }
    }

    // Tab Content
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      AnimatedContent(targetState = uiState.activeStudioTab, label = "studio_tab_content") { tab ->
        when (tab) {
          StudioTab.STYLE ->
            StyleTabContent(
              currentStyle = config.styleId,
              weather = uiState.weather,
              keepColors = keepColorsOnSwitch,
              onToggleKeepColors = { keepColorsOnSwitch = it },
              onSelectStyle = { onSwitchStyle(it, keepColorsOnSwitch) },
              liveTimeMillis = liveTimeMillis,
            )
          StudioTab.COLORS ->
            ColorsTabContent(
              config = config,
              onApplyPalette = onApplyPalette,
              onUpdateConfig = onUpdateConfig,
            )
          StudioTab.FONTS ->
            FontsTabContent(
              config = config,
              onUpdateConfig = onUpdateConfig,
            )
          StudioTab.LAYOUT ->
            LayoutTabContent(
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

    // Sticky Bottom CTA Bar
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 4.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(
          onClick = { showSaveDialog = true },
          modifier = Modifier.weight(0.9f).height(50.dp).testTag("studio_save_preset_btn"),
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
          modifier = Modifier.weight(1.4f).height(50.dp).testTag("studio_pin_widget_btn"),
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
}

@Composable
private fun StyleTabContent(
  currentStyle: ClockStyleId,
  weather: WeatherSnapshot,
  keepColors: Boolean,
  onToggleKeepColors: (Boolean) -> Unit,
  onSelectStyle: (ClockStyleId) -> Unit,
  liveTimeMillis: Long,
) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Keep Current Colors", style = MaterialTheme.typography.titleMedium)
        Switch(
          checked = keepColors,
          onCheckedChange = onToggleKeepColors,
          modifier = Modifier.testTag("keep_colors_switch"),
        )
      }
    }

    items(ClockStyleId.entries.chunked(2)) { rowPair ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        rowPair.forEach { style ->
          val selected = style == currentStyle
          val previewCfg = remember(style) { ClockPresets.defaultConfigForStyle(style) }
          Column(
            modifier =
              Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                  if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                  else MaterialTheme.colorScheme.surface,
                )
                .border(
                  width = if (selected) 2.dp else 1.dp,
                  color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                  shape = RoundedCornerShape(16.dp),
                )
                .clickable { onSelectStyle(style) }
                .padding(10.dp)
                .testTag("studio_style_item_${style.name.lowercase()}"),
          ) {
            Box(
              modifier = Modifier.fillMaxWidth().height(78.dp),
              contentAlignment = Alignment.Center,
            ) {
              LiveClockCanvas(
                config = previewCfg,
                timeMillis = liveTimeMillis,
                weather = weather,
                modifier = Modifier.fillMaxSize(),
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = style.displayName,
              style = MaterialTheme.typography.labelLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = style.category.label,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
        if (rowPair.size == 1) Spacer(modifier = Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun ColorsTabContent(
  config: WidgetConfig,
  onApplyPalette: (ColorPalettePreset) -> Unit,
  onUpdateConfig: (WidgetConfig) -> Unit,
) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("Palettes", style = MaterialTheme.typography.titleMedium)
      }
      Spacer(modifier = Modifier.height(10.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(ClockPresets.colorPalettes, key = { it.id }) { palette ->
          Column(
            modifier =
              Modifier
                .width(96.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .clickable { onApplyPalette(palette) }
                .padding(10.dp)
                .testTag("palette_preset_${palette.id}"),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
              Box(
                modifier =
                  Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(palette.backgroundColor))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
              )
              Box(
                modifier =
                  Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(palette.accentColor))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
              )
              Box(
                modifier =
                  Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(palette.foregroundColor))
                    .border(1.dp, Color.Black.copy(alpha = 0.3f), CircleShape),
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = palette.name,
              style = MaterialTheme.typography.labelSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    item {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("Glass / Card Opacity", style = MaterialTheme.typography.titleMedium)
          Text(
            text = "${(config.backgroundAlpha * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Slider(
          value = config.backgroundAlpha,
          onValueChange = { onUpdateConfig(config.copy(backgroundAlpha = it)) },
          valueRange = 0f..1f,
          modifier = Modifier.testTag("slider_bg_opacity"),
        )
      }
    }

    item {
      ColorChannelEditor(
        title = "Accent & Hands",
        selectedColor = config.accentColor,
        onColorChange = { onUpdateConfig(config.copy(accentColor = it)) },
      )
    }

    item {
      ColorChannelEditor(
        title = "Background Card",
        selectedColor = config.backgroundColor,
        onColorChange = { onUpdateConfig(config.copy(backgroundColor = it)) },
      )
    }

    item {
      ColorChannelEditor(
        title = "Numerals & Text",
        selectedColor = config.foregroundColor,
        onColorChange = { onUpdateConfig(config.copy(foregroundColor = it)) },
      )
    }
  }
}

@Composable
private fun FontsTabContent(
  config: WidgetConfig,
  onUpdateConfig: (WidgetConfig) -> Unit,
) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("Time Scale", style = MaterialTheme.typography.titleMedium)
          Text(
            text = String.format("%.2f×", config.fontScale),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Slider(
          value = config.fontScale,
          onValueChange = { onUpdateConfig(config.copy(fontScale = it)) },
          valueRange = 0.72f..1.35f,
          modifier = Modifier.testTag("slider_font_scale"),
        )
      }
    }

    items(ClockFontId.entries.chunked(2)) { pair ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        pair.forEach { fontId ->
          val selected = config.fontId == fontId
          val fontFamily = remember(fontId) { FontFamily(Font(fontId.fontRes, FontWeight.Normal)) }
          Column(
            modifier =
              Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                  if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                  else MaterialTheme.colorScheme.surface,
                )
                .border(
                  width = if (selected) 2.dp else 1.dp,
                  color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                  shape = RoundedCornerShape(16.dp),
                )
                .clickable { onUpdateConfig(config.copy(fontId = fontId)) }
                .padding(14.dp)
                .testTag("font_option_${fontId.name.lowercase()}"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = fontId.sampleText,
                fontFamily = fontFamily,
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onSurface,
              )
              if (selected) {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = "Selected font",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = fontId.displayName,
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun LayoutTabContent(
  config: WidgetConfig,
  weather: WeatherSnapshot,
  isLoadingWeather: Boolean,
  citySearchResults: List<GeocodingPlace>,
  onUpdateConfig: (WidgetConfig) -> Unit,
  onSearchCities: (String) -> Unit,
  onSelectCity: (GeocodingPlace) -> Unit,
  onUseLocationWeather: () -> Unit,
  onRefreshWeather: () -> Unit,
) {
  val context = LocalContext.current
  var cityQuery by remember { mutableStateOf("") }
  val locationLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
      if (granted) {
        onUseLocationWeather()
      }
    }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Live Weather City & Location Card
    item {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Column {
              Text(
                text = if (weather.isLoaded) "${weather.cityName} • ${weather.displayTemp(config.useFahrenheit)}" else weather.cityName,
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text = weather.conditionText(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLoadingWeather) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
              IconButton(onClick = onRefreshWeather, modifier = Modifier.testTag("weather_refresh_btn")) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh live weather")
              }
            }
            FilledTonalButton(
              onClick = {
                val hasLoc =
                  ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                if (hasLoc) {
                  onUseLocationWeather()
                } else {
                  locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
              },
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.testTag("weather_gps_btn"),
            ) {
              Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("GPS", style = MaterialTheme.typography.labelLarge)
            }
          }
        }

        OutlinedTextField(
          value = cityQuery,
          onValueChange = {
            cityQuery = it
            onSearchCities(it)
          },
          label = { Text("Search city for live weather") },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("weather_city_search_input"),
        )

        if (citySearchResults.isNotEmpty()) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            citySearchResults.forEach { place ->
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .clickable {
                      onSelectCity(place)
                      cityQuery = ""
                    }
                    .testTag("city_result_${place.name.lowercase()}"),
              ) {
                Text(
                  text = place.displayLabel(),
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
              }
            }
          }
        }
      }
    }

    // 3D Depth Slider
    item {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("3D Depth & Shadows", style = MaterialTheme.typography.titleMedium)
          Text(
            text = "${(config.depthIntensity * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Slider(
          value = config.depthIntensity,
          onValueChange = { onUpdateConfig(config.copy(depthIntensity = it)) },
          valueRange = 0f..1f,
          modifier = Modifier.testTag("slider_depth"),
        )
      }
    }

    item {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("Corner Radius", style = MaterialTheme.typography.titleMedium)
          Text(
            text = "${config.cornerRadiusDp.roundToInt()} dp",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Slider(
          value = config.cornerRadiusDp,
          onValueChange = { onUpdateConfig(config.copy(cornerRadiusDp = it)) },
          valueRange = 0f..44f,
          modifier = Modifier.testTag("slider_corner_radius"),
        )
      }
    }

    item {
      OutlinedTextField(
        value = config.customLabel,
        onValueChange = { if (it.length <= 24) onUpdateConfig(config.copy(customLabel = it)) },
        label = { Text("Custom Tag / Motto") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("custom_label_input"),
      )
    }

    item {
      ToggleSettingCard(
        title = "Fahrenheit (°F)",
        subtitle = "Switch between °C and °F",
        checked = config.useFahrenheit,
        onCheckedChange = { onUpdateConfig(config.copy(useFahrenheit = it)) },
        tag = "toggle_fahrenheit",
      )
    }

    item {
      ToggleSettingCard(
        title = "24-Hour Format",
        subtitle = "13:00–23:59 instead of AM/PM",
        checked = config.is24Hour,
        onCheckedChange = { onUpdateConfig(config.copy(is24Hour = it)) },
        tag = "toggle_24h",
      )
    }

    item {
      ToggleSettingCard(
        title = "Seconds / Sweep Hand",
        subtitle = "Live seconds readout or analog sweep",
        checked = config.showSeconds,
        onCheckedChange = { onUpdateConfig(config.copy(showSeconds = it)) },
        tag = "toggle_seconds",
      )
    }

    item {
      ToggleSettingCard(
        title = "Date & Weekday",
        subtitle = "Show formatted date pill",
        checked = config.showDate,
        onCheckedChange = { onUpdateConfig(config.copy(showDate = it)) },
        tag = "toggle_date",
      )
    }

    item {
      ToggleSettingCard(
        title = "Card Border",
        subtitle = "Hairline outline or frame stroke",
        checked = config.showBorder,
        onCheckedChange = { onUpdateConfig(config.copy(showBorder = it)) },
        tag = "toggle_border",
      )
    }
  }
}

@Composable
private fun ToggleSettingCard(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  tag: String,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
        .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.testTag(tag))
  }
}
