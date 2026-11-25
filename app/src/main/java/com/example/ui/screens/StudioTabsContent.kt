package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WbSunny
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.BackgroundFillMode
import com.example.model.CardStyle
import com.example.model.ClockAlignment
import com.example.model.ClockAnimationStyle
import com.example.model.ClockCategory
import com.example.model.ClockDateFormat
import com.example.model.ClockFontId
import com.example.model.ClockPresets
import com.example.model.ClockStyleId
import com.example.model.ClockThemePack
import com.example.model.ColorPalettePreset
import com.example.model.DisplayMode
import com.example.model.SecondsStyle
import com.example.model.WeatherSnapshot
import com.example.model.WidgetBackdropId
import com.example.model.WidgetConfig
import com.example.ui.components.ColorChannelEditor
import com.example.ui.components.LiveClockCanvas
import com.example.weather.GeocodingPlace
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun StudioStyleTabContent(
  config: WidgetConfig,
  weather: WeatherSnapshot,
  keepColors: Boolean,
  liveTimeMillis: Long,
  onToggleKeepColors: (Boolean) -> Unit,
  onSelectStyle: (ClockStyleId) -> Unit,
  onApplyThemePack: (ClockThemePack) -> Unit,
  onResetDefaults: () -> Unit,
  onOpenBuilder: () -> Unit,
  onUpdateConfig: (WidgetConfig) -> Unit,
) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onResetDefaults,
          modifier = Modifier.weight(1f).height(48.dp).testTag("studio_reset_defaults_btn"),
          shape = RoundedCornerShape(14.dp),
        ) {
          Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Reset Defaults", maxLines = 1)
        }
        FilledTonalButton(
          onClick = onOpenBuilder,
          modifier = Modifier.weight(1f).height(48.dp).testTag("studio_open_builder_btn"),
          shape = RoundedCornerShape(14.dp),
        ) {
          Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Custom Builder", maxLines = 1)
        }
      }
    }

    item {
      Text("Curated Theme Packs", style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ClockPresets.themePacks, key = { it.id }) { pack ->
          val selected = config.themePackId == pack.id
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            modifier =
              Modifier
                .clip(RoundedCornerShape(14.dp))
                .border(
                  width = if (selected) 1.5.dp else 1.dp,
                  color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                  shape = RoundedCornerShape(14.dp),
                )
                .clickable { onApplyThemePack(pack) }
                .testTag("studio_theme_pack_${pack.id}"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
              Box(
                modifier =
                  Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(pack.accentColor))
                    .border(1.dp, Color(pack.foregroundColor).copy(alpha = 0.45f), CircleShape),
              )
              Text(pack.name, style = MaterialTheme.typography.labelLarge)
            }
          }
        }
      }
    }

    item {
      StudioToggleCard(
        title = "Time-Based Adaptive Mood",
        subtitle = "Automatically shifts tones across Sunrise, Day, Sunset & Night",
        checked = config.timeBasedStyleEnabled,
        onCheckedChange = { onUpdateConfig(config.copy(timeBasedStyleEnabled = it)) },
        tag = "toggle_time_based_mood",
      )
    }

    item {
      StudioToggleCard(
        title = "Keep Current Colors When Switching",
        subtitle = "Carry active palette & fill when browsing styles below",
        checked = keepColors,
        onCheckedChange = onToggleKeepColors,
        tag = "keep_colors_switch",
      )
    }

    items(ClockStyleId.entries.chunked(2)) { rowPair ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        rowPair.forEach { style ->
          val selected = style == config.styleId
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
                .clickable {
                  if (style == ClockStyleId.CUSTOM_COMPOSER) onOpenBuilder() else onSelectStyle(style)
                }
                .padding(10.dp)
                .testTag("studio_style_item_${style.name.lowercase()}"),
          ) {
            Box(
              modifier = Modifier.fillMaxWidth().height(82.dp).clip(RoundedCornerShape(12.dp)),
              contentAlignment = Alignment.Center,
            ) {
              Image(
                painter = painterResource(id = R.drawable.img_wallpaper_aurora_1795495861674),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
              Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f)))
              LiveClockCanvas(
                config = previewCfg,
                timeMillis = liveTimeMillis,
                weather = weather,
                modifier = Modifier.fillMaxSize().padding(6.dp),
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
internal fun StudioColorsTabContent(
  config: WidgetConfig,
  activeDisplayMode: DisplayMode,
  wallpaperSuggestions: List<ColorPalettePreset>,
  onApplyPalette: (ColorPalettePreset) -> Unit,
  onSelectFillMode: (BackgroundFillMode, String?) -> Unit,
  onUpdateConfig: (WidgetConfig) -> Unit,
) {
  val activeFill = config.effectiveFillMode(activeDisplayMode)
  val activeBackdrop = when (activeDisplayMode) {
    DisplayMode.HOME_SCREEN -> config.backdropId
    DisplayMode.LOCK_SCREEN -> config.lockBackdropId
    DisplayMode.AOD -> WidgetBackdropId.NONE
  }
  val activeAlpha = when (activeDisplayMode) {
    DisplayMode.HOME_SCREEN -> config.backgroundAlpha
    DisplayMode.LOCK_SCREEN -> config.lockBackgroundAlpha
    DisplayMode.AOD -> config.aodBackgroundAlpha
  }

  val photoLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        onSelectFillMode(BackgroundFillMode.PHOTO, uri.toString())
      }
    }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Background Fill Mode Selector
    item {
      Text("${activeDisplayMode.shortLabel} Background Fill", style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.height(6.dp))
      val availableModes = if (activeDisplayMode == DisplayMode.AOD) {
        listOf(BackgroundFillMode.NONE, BackgroundFillMode.SOLID)
      } else {
        BackgroundFillMode.entries
      }
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(availableModes, key = { it.name }) { mode ->
          FilterChip(
            selected = activeFill == mode,
            onClick = {
              if (mode == BackgroundFillMode.PHOTO) {
                photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
              } else {
                onSelectFillMode(mode, null)
              }
            },
            label = { Text(mode.label) },
            leadingIcon = if (mode == BackgroundFillMode.PHOTO) {
              { Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            modifier = Modifier.testTag("fill_mode_${mode.name.lowercase()}"),
          )
        }
      }
    }

    if (activeFill == BackgroundFillMode.ARTWORK && activeDisplayMode != DisplayMode.AOD) {
      item {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Rounded.Wallpaper, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
          Text("Artwork Backdrop", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          items(WidgetBackdropId.entries.filter { it != WidgetBackdropId.NONE }, key = { it.name }) { backdrop ->
            val selected = activeBackdrop == backdrop
            Column(
              modifier =
                Modifier
                  .width(86.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                  )
                  .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(14.dp),
                  )
                  .clickable {
                    val updated = when (activeDisplayMode) {
                      DisplayMode.HOME_SCREEN ->
                        config.copy(backdropId = backdrop, backgroundFillMode = BackgroundFillMode.ARTWORK, cardStyle = CardStyle.SOLID, backgroundAlpha = 1.0f)
                      DisplayMode.LOCK_SCREEN ->
                        config.copy(lockBackdropId = backdrop, lockBackgroundFillMode = BackgroundFillMode.ARTWORK, lockCardStyle = CardStyle.SOLID, lockBackgroundAlpha = 0.92f)
                      DisplayMode.AOD -> config
                    }
                    onUpdateConfig(updated)
                  }
                  .padding(6.dp)
                  .testTag("backdrop_preset_${backdrop.name.lowercase()}"),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              val res = backdrop.drawableRes
              if (res != null) {
                Image(
                  painter = painterResource(id = res),
                  contentDescription = backdrop.displayName,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)),
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(backdrop.displayName, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }
    }

    if (activeFill == BackgroundFillMode.GRADIENT && activeDisplayMode != DisplayMode.AOD) {
      item {
        Text("Gradient Angle", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(listOf(0f, 45f, 90f, 135f, 180f)) { deg ->
            FilterChip(
              selected = config.gradientAngleDeg.roundToInt() == deg.roundToInt(),
              onClick = { onUpdateConfig(config.copy(gradientAngleDeg = deg)) },
              label = { Text("${deg.roundToInt()}°") },
            )
          }
        }
      }
      item {
        ColorChannelEditor(
          title = "Gradient Secondary Color",
          selectedColor = config.backgroundSecondaryColor,
          onColorChange = { onUpdateConfig(config.copy(backgroundSecondaryColor = it)) },
        )
      }
    }

    if (wallpaperSuggestions.isNotEmpty()) {
      item {
        Text("Matched To Preview Wallpaper", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        PaletteStrip(palettes = wallpaperSuggestions, onApplyPalette = onApplyPalette)
      }
    }

    item {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("Curated Palettes", style = MaterialTheme.typography.titleMedium)
      }
      Spacer(modifier = Modifier.height(8.dp))
      PaletteStrip(palettes = ClockPresets.colorPalettes, onApplyPalette = onApplyPalette)
    }

    if (activeFill != BackgroundFillMode.NONE) {
      item {
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
              .padding(14.dp),
        ) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${activeDisplayMode.shortLabel} Background Opacity", style = MaterialTheme.typography.titleMedium)
            Text("${(activeAlpha * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          }
          val maxAlpha = if (activeDisplayMode == DisplayMode.AOD) 0.28f else 1f
          Slider(
            value = activeAlpha.coerceIn(0f, maxAlpha),
            onValueChange = { alpha ->
              val updated = when (activeDisplayMode) {
                DisplayMode.HOME_SCREEN -> config.copy(backgroundAlpha = alpha)
                DisplayMode.LOCK_SCREEN -> config.copy(lockBackgroundAlpha = alpha)
                DisplayMode.AOD -> config.copy(aodBackgroundAlpha = alpha)
              }
              onUpdateConfig(updated)
            },
            valueRange = 0.04f..maxAlpha,
            modifier = Modifier.testTag("slider_bg_opacity"),
          )
        }
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
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Digit Color Overrides", style = MaterialTheme.typography.titleMedium)
        if (config.hourColor != null || config.minuteColor != null) {
          TextButton(onClick = { onUpdateConfig(config.copy(hourColor = null, minuteColor = null, secondsColor = null)) }) {
            Text("Reset Split Colors")
          }
        }
      }
    }

    item {
      ColorChannelEditor(
        title = "Hour Numerals",
        selectedColor = config.hourColor ?: config.foregroundColor,
        onColorChange = { onUpdateConfig(config.copy(foregroundColor = it, hourColor = it)) },
      )
    }

    item {
      ColorChannelEditor(
        title = "Minute Numerals",
        selectedColor = config.minuteColor ?: config.foregroundColor,
        onColorChange = { onUpdateConfig(config.copy(minuteColor = it)) },
      )
    }

    if (config.showSeconds) {
      item {
        ColorChannelEditor(
          title = "Seconds Accent",
          selectedColor = config.secondsColor ?: config.accentColor,
          onColorChange = { onUpdateConfig(config.copy(secondsColor = it)) },
        )
      }
    }

    if (activeFill != BackgroundFillMode.NONE) {
      item {
        ColorChannelEditor(
          title = "Background Surface",
          selectedColor = config.backgroundColor,
          onColorChange = { onUpdateConfig(config.copy(backgroundColor = it)) },
        )
      }
    }

    if (config.showBorder && activeFill != BackgroundFillMode.NONE) {
      item {
        ColorChannelEditor(
          title = "Border Outline",
          selectedColor = config.borderColor,
          onColorChange = { onUpdateConfig(config.copy(borderColor = it)) },
        )
      }
    }
  }
}

@Composable
private fun PaletteStrip(
  palettes: List<ColorPalettePreset>,
  onApplyPalette: (ColorPalettePreset) -> Unit,
) {
  LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    items(palettes, key = { it.id }) { palette ->
      Column(
        modifier =
          Modifier
            .width(102.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable { onApplyPalette(palette) }
            .padding(10.dp)
            .testTag("palette_preset_${palette.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
          Box(Modifier.size(24.dp).clip(CircleShape).background(Color(palette.backgroundColor)).border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape))
          Box(Modifier.size(24.dp).clip(CircleShape).background(Color(palette.accentColor)).border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape))
          Box(Modifier.size(24.dp).clip(CircleShape).background(Color(palette.foregroundColor)).border(1.dp, Color.Black.copy(alpha = 0.3f), CircleShape))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(palette.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }
  }
}

@Composable
internal fun StudioMotionTabContent(
  config: WidgetConfig,
  timeTravelEnabled: Boolean,
  timeTravelMinutes: Int,
  onSetTimeTravel: (Boolean, Int?) -> Unit,
  onUpdateConfig: (WidgetConfig) -> Unit,
) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      StudioToggleCard(
        title = "Motion & Ambient Effects",
        subtitle = "Smooth animation on Home & Lock Screen (disabled on AOD to save battery)",
        checked = config.animationsEnabled,
        onCheckedChange = { onUpdateConfig(config.copy(animationsEnabled = it)) },
        tag = "toggle_animations",
      )
    }

    item {
      Text("Animation Style", style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.height(8.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ClockAnimationStyle.entries, key = { it.name }) { style ->
          val selected = config.animationStyle == style
          FilterChip(
            selected = selected,
            onClick = {
              onUpdateConfig(
                config.copy(
                  animationStyle = style,
                  animationsEnabled = style != ClockAnimationStyle.NONE,
                ),
              )
            },
            label = { Text(style.label) },
            modifier = Modifier.testTag("anim_style_${style.name.lowercase()}"),
          )
        }
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = config.animationStyle.subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    item {
      Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)).padding(14.dp),
      ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Animation Speed", style = MaterialTheme.typography.titleMedium)
          Text(String.format(Locale.US, "%.2f×", config.animationSpeed), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
          value = config.animationSpeed,
          onValueChange = { onUpdateConfig(config.copy(animationSpeed = it)) },
          valueRange = 0.5f..2.0f,
          modifier = Modifier.testTag("slider_anim_speed"),
        )
      }
    }

    item {
      Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)).padding(14.dp),
      ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Glow & Aura Intensity", style = MaterialTheme.typography.titleMedium)
          Text("${(config.glowIntensity * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
          value = config.glowIntensity,
          onValueChange = { onUpdateConfig(config.copy(glowIntensity = it)) },
          valueRange = 0f..1f,
          modifier = Modifier.testTag("slider_glow_intensity"),
        )
      }
    }

    item {
      StudioToggleCard(
        title = "Decorative Elements",
        subtitle = "Stars, orbits, clouds, horizon lines & framing accents",
        checked = config.showDecorations,
        onCheckedChange = { onUpdateConfig(config.copy(showDecorations = it)) },
        tag = "toggle_decorations",
      )
    }

    // Time Travel Preview Tool
    item {
      val hh = timeTravelMinutes / 60
      val mm = timeTravelMinutes % 60
      val timeLabel = String.format(Locale.US, "%02d:%02d", hh, mm)
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
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
              Text("Time Travel Preview", style = MaterialTheme.typography.titleMedium)
              Text(
                text = if (timeTravelEnabled) "Simulating $timeLabel" else "Showing live system time",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Switch(
            checked = timeTravelEnabled,
            onCheckedChange = { onSetTimeTravel(it, null) },
            modifier = Modifier.testTag("toggle_time_travel"),
          )
        }
        Slider(
          value = timeTravelMinutes.toFloat(),
          onValueChange = { onSetTimeTravel(true, it.roundToInt()) },
          valueRange = 0f..1439f,
          modifier = Modifier.testTag("slider_time_travel"),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          val presets = listOf("Dawn 06:30" to 390, "Noon 12:00" to 720, "Sunset 18:15" to 1095, "Night 23:00" to 1380)
          items(presets) { (label, mins) ->
            FilterChip(
              selected = timeTravelEnabled && kotlin.math.abs(timeTravelMinutes - mins) < 12,
              onClick = { onSetTimeTravel(true, mins) },
              label = { Text(label) },
            )
          }
        }
      }
    }
  }
}

@Composable
internal fun StudioLayoutTabContent(
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
  val isAnalog = config.styleId.category == ClockCategory.ANALOG || config.styleId == ClockStyleId.FLOATING_ANALOG_PURE
  val locationLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) onUseLocationWeather()
    }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Typography Scale & Font Selector
    item {
      Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)).padding(14.dp),
      ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Time Scale", style = MaterialTheme.typography.titleMedium)
          Text(String.format(Locale.US, "%.2f×", config.fontScale), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
          value = config.fontScale,
          onValueChange = { onUpdateConfig(config.copy(fontScale = it)) },
          valueRange = 0.72f..1.35f,
          modifier = Modifier.testTag("slider_font_scale"),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text("Typography (${config.fontId.displayName})", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(ClockFontId.entries, key = { it.name }) { fontId ->
            val selected = config.fontId == fontId
            val fontFamily = remember(fontId) { FontFamily(Font(fontId.fontRes, FontWeight.Normal)) }
            Column(
              modifier =
                Modifier
                  .width(106.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surface)
                  .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(14.dp),
                  )
                  .clickable { onUpdateConfig(config.copy(fontId = fontId)) }
                  .padding(10.dp)
                  .testTag("font_option_${fontId.name.lowercase()}"),
            ) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = fontId.sampleText, fontFamily = fontFamily, fontSize = 22.sp)
                if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selected font", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
              }
              Spacer(modifier = Modifier.height(3.dp))
              Text(fontId.displayName, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }
    }

    if (!isAnalog) {
      item {
        Text("Text Alignment", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ClockAlignment.entries.forEach { align ->
            FilterChip(
              selected = config.alignment == align,
              onClick = { onUpdateConfig(config.copy(alignment = align)) },
              label = { Text(align.label) },
              modifier = Modifier.weight(1f).testTag("align_${align.name.lowercase()}"),
            )
          }
        }
      }
    }

    item {
      Text("Seconds Display Style", style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(SecondsStyle.entries, key = { it.name }) { secStyle ->
          FilterChip(
            selected = if (!config.showSeconds) secStyle == SecondsStyle.NONE else config.secondsStyle == secStyle,
            onClick = {
              onUpdateConfig(config.copy(secondsStyle = secStyle, showSeconds = secStyle != SecondsStyle.NONE))
            },
            label = { Text(secStyle.label) },
            modifier = Modifier.testTag("seconds_style_${secStyle.name.lowercase()}"),
          )
        }
      }
    }

    item {
      Text("Date Format", style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ClockDateFormat.entries, key = { it.name }) { fmt ->
          FilterChip(
            selected = config.dateFormat == fmt,
            onClick = { onUpdateConfig(config.copy(dateFormat = fmt, showDate = true)) },
            label = { Text(fmt.label) },
            modifier = Modifier.testTag("date_fmt_${fmt.name.lowercase()}"),
          )
        }
      }
    }

    // World Clock Time Zone Picker
    item {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Rounded.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text("World Clock Time Zone", style = MaterialTheme.typography.titleMedium)
      }
      Spacer(modifier = Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ClockPresets.worldTimeZones, key = { it.shortCode }) { tz ->
          FilterChip(
            selected = config.timeZoneId == tz.id,
            onClick = { onUpdateConfig(config.copy(timeZoneId = tz.id)) },
            label = { Text("${tz.shortCode} • ${tz.label.substringBefore(" (")}") },
            modifier = Modifier.testTag("tz_${tz.shortCode.lowercase()}"),
          )
        }
      }
    }

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
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Column {
              Text(
                text = if (weather.isLoaded) "${weather.cityName} • ${weather.displayTemp(config.useFahrenheit)}" else weather.cityName,
                style = MaterialTheme.typography.titleMedium,
              )
              Text(weather.conditionText(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
                val hasLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (hasLoc) onUseLocationWeather() else locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
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
          placeholder = { Text("Search city (e.g. Tokyo, London, Mumbai)") },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("weather_city_search"),
        )
        citySearchResults.forEach { place ->
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable {
                  cityQuery = ""
                  onSelectCity(place)
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(place.displayLabel(), style = MaterialTheme.typography.bodyMedium)
            Text("Select", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }

    item {
      Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)).padding(14.dp),
      ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Corner Roundness", style = MaterialTheme.typography.titleMedium)
          Text("${config.cornerRadiusDp.roundToInt()} dp", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
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
      StudioToggleCard("24-Hour Format", "13:00–23:59 instead of AM/PM", config.is24Hour, { onUpdateConfig(config.copy(is24Hour = it)) }, "toggle_24h")
    }
    item {
      StudioToggleCard("Date & Weekday", "Show formatted date label", config.showDate, { onUpdateConfig(config.copy(showDate = it)) }, "toggle_date")
    }
    item {
      StudioToggleCard("Weather Readout", "Show live temperature & sky state", config.showWeather, { onUpdateConfig(config.copy(showWeather = it)) }, "toggle_weather")
    }
    item {
      StudioToggleCard("Fahrenheit (°F)", "Switch between °C and °F", config.useFahrenheit, { onUpdateConfig(config.copy(useFahrenheit = it)) }, "toggle_fahrenheit")
    }
    item {
      StudioToggleCard("3D Depth Shadow", "Drop shadow behind numerals & hands", config.showShadow, { onUpdateConfig(config.copy(showShadow = it)) }, "toggle_shadow")
    }
    item {
      StudioToggleCard("Card Border", "Hairline outline around background card", config.showBorder, { onUpdateConfig(config.copy(showBorder = it)) }, "toggle_border")
    }

    item {
      Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)).padding(14.dp),
      ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("AOD Brightness", style = MaterialTheme.typography.titleMedium)
          Text("${(config.aodLowPowerDim * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
          value = config.aodLowPowerDim,
          onValueChange = { onUpdateConfig(config.copy(aodLowPowerDim = it)) },
          valueRange = 0.45f..0.95f,
          modifier = Modifier.testTag("slider_aod_dim"),
        )
      }
    }

    item {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Text("Ambient Screen Saver (AOD)", style = MaterialTheme.typography.titleMedium)
        }
        Text(
          text = "Android restricts custom widgets inside system AOD. Set ChronoCraft as your Screen Saver for real ambient OLED clock display while charging or docked.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(
          onClick = {
            val dreamIntent = Intent(Settings.ACTION_DREAM_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val fallback = Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(dreamIntent) }.onFailure { runCatching { context.startActivity(fallback) } }
          },
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_dream_settings_btn"),
        ) {
          Text("Open Screen Saver Settings")
        }
      }
    }
  }
}

@Composable
private fun StudioToggleCard(
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
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.testTag(tag))
  }
}
