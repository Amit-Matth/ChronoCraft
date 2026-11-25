package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ActiveHomeWidget
import com.example.model.AppThemeMode
import com.example.model.SavedClockPreset
import com.example.ui.components.LiveClockCanvas
import com.example.ui.components.rememberLiveClockMillis
import com.example.ui.viewmodel.ClockStudioUiState
import com.example.widget.ChronoLiveWallpaperService

@Composable
fun SavedWidgetsScreen(
  uiState: ClockStudioUiState,
  onRefreshActiveWidgets: () -> Unit,
  onEditActiveWidget: (ActiveHomeWidget) -> Unit,
  onLoadPreset: (SavedClockPreset) -> Unit,
  onDuplicatePreset: (SavedClockPreset) -> Unit = {},
  onDeletePreset: (String) -> Unit,
  onOpenBuilder: () -> Unit = {},
  onOpenLibrary: () -> Unit,
  onSelectThemeMode: (AppThemeMode) -> Unit = {},
  onReplayOnboarding: () -> Unit = {},
  onOpenAodExperience: () -> Unit = {},
  onOpenIconPackStudio: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val liveTimeMillis = rememberLiveClockMillis(1000L)

  Box(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 880.dp),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.weight(1f),
        ) {
          Image(
            painter = painterResource(id = R.drawable.chrono_app_logo_1795569986651),
            contentDescription = "ChronoCraft logo",
            contentScale = ContentScale.Crop,
            modifier =
              Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
          )
          Column {
            Text(
              text = "My Widgets",
              style = MaterialTheme.typography.displaySmall,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
              text = "Active instances, presets & app theme",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        IconButton(
          onClick = onRefreshActiveWidgets,
          modifier = Modifier.size(48.dp).testTag("refresh_active_widgets_btn"),
        ) {
          Icon(Icons.Rounded.Refresh, contentDescription = "Refresh active home screen widgets")
        }
      }
    }

    item {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier =
          Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "APP APPEARANCE",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            OutlinedButton(
              onClick = onReplayOnboarding,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
              modifier = Modifier.testTag("saved_replay_onboarding_btn"),
            ) {
              Icon(
                imageVector = Icons.Rounded.PlayCircleOutline,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text("App Tour")
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            listOf(
              Triple(AppThemeMode.SYSTEM, "Auto", Icons.Rounded.BrightnessAuto),
              Triple(AppThemeMode.LIGHT, "Light", Icons.Rounded.LightMode),
              Triple(AppThemeMode.DARK, "Dark", Icons.Rounded.DarkMode),
            ).forEach { (mode, label, icon) ->
              FilterChip(
                selected = uiState.appThemeMode == mode,
                onClick = { onSelectThemeMode(mode) },
                leadingIcon = {
                  Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                label = { Text(label, maxLines = 1) },
                modifier = Modifier.weight(1f).testTag("saved_theme_${mode.name.lowercase()}"),
              )
            }
          }
        }
      }
    }

    item {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier =
          Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "DISPLAYS, LOCK SCREEN & HOME ICONS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilledTonalButton(
              onClick = onOpenAodExperience,
              modifier = Modifier.weight(1f).height(46.dp).testTag("saved_open_aod_btn"),
              shape = RoundedCornerShape(14.dp),
            ) {
              Icon(Icons.Rounded.Bedtime, contentDescription = null, modifier = Modifier.size(17.dp))
              Spacer(modifier = Modifier.width(5.dp))
              Text("StandBy AOD", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FilledTonalButton(
              onClick = { ChronoLiveWallpaperService.launchWallpaperPicker(context) },
              modifier = Modifier.weight(1f).height(46.dp).testTag("saved_open_wallpaper_btn"),
              shape = RoundedCornerShape(14.dp),
            ) {
              Icon(Icons.Rounded.Wallpaper, contentDescription = null, modifier = Modifier.size(17.dp))
              Spacer(modifier = Modifier.width(5.dp))
              Text("Lock/Home WP", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
          OutlinedButton(
            onClick = onOpenIconPackStudio,
            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("saved_open_icons_btn"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Rounded.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Icon Pack Studio (${uiState.iconPackConfig.packName})")
          }
        }
      }
    }

    // Section 1: Active Home Screen Widgets
    item {
      Text(
        text = "ACTIVE ON HOME SCREEN (${uiState.activeHomeWidgets.size})",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
      )
    }

    if (uiState.activeHomeWidgets.isEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
          modifier =
            Modifier
              .fillMaxWidth()
              .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Widgets,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(36.dp),
            )
            Text("No Pinned Widgets Yet", style = MaterialTheme.typography.titleLarge)
            Text(
              text = "Tap \"Add to Home Screen\" in the Studio or long-press your phone home screen → Widgets → ChronoCraft. Placed widgets can be resized to any dimension and edited live here!",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
              onClick = onOpenLibrary,
              modifier = Modifier.testTag("saved_explore_btn"),
            ) {
              Icon(Icons.Rounded.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Browse Clock Styles")
            }
          }
        }
      }
    } else {
      items(uiState.activeHomeWidgets, key = { it.appWidgetId }) { widget ->
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
          modifier =
            Modifier
              .fillMaxWidth()
              .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Box(
              modifier = Modifier.width(120.dp).height(80.dp),
              contentAlignment = Alignment.Center,
            ) {
              LiveClockCanvas(
                config = widget.config,
                timeMillis = liveTimeMillis,
                weather = uiState.weather,
                modifier = Modifier.fillMaxSize(),
              )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = widget.config.styleId.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "Widget #${widget.appWidgetId} • ${widget.providerLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(2.dp))
              FilledTonalButton(
                onClick = { onEditActiveWidget(widget) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("edit_active_widget_${widget.appWidgetId}"),
              ) {
                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Customize Live")
              }
            }
          }
        }
      }
    }

    // Section 2: Saved Custom Presets
    item {
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "SAVED PRESETS (${uiState.savedPresets.size})",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.secondary,
        )
        OutlinedButton(
          onClick = onOpenBuilder,
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.testTag("saved_create_custom_btn"),
        ) {
          Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(5.dp))
          Text("Create Custom")
        }
      }
    }

    if (uiState.savedPresets.isEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Customize any clock in the Studio and tap Save to build your personal widget collection.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
          )
        }
      }
    } else {
      items(uiState.savedPresets, key = { it.id }) { preset ->
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
          modifier =
            Modifier
              .fillMaxWidth()
              .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier = Modifier.width(114.dp).height(76.dp),
              contentAlignment = Alignment.Center,
            ) {
              LiveClockCanvas(
                config = preset.config,
                timeMillis = liveTimeMillis,
                weather = uiState.weather,
                modifier = Modifier.fillMaxSize(),
              )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = preset.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "${preset.config.styleId.displayName} • ${preset.config.fontId.displayName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                FilledTonalButton(
                  onClick = { onLoadPreset(preset) },
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier.testTag("load_preset_${preset.id}"),
                ) {
                  Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(5.dp))
                  Text("Open")
                }
                IconButton(
                  onClick = { onDuplicatePreset(preset) },
                  modifier = Modifier.size(48.dp).testTag("duplicate_preset_${preset.id}"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "Duplicate preset ${preset.name}",
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
                IconButton(
                  onClick = { onDeletePreset(preset.id) },
                  modifier = Modifier.size(48.dp).testTag("delete_preset_${preset.id}"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete preset ${preset.name}",
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }
          }
        }
      }
    }
    }
  }
}
