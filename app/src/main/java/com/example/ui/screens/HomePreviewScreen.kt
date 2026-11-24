package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.WallpaperOption
import com.example.model.WidgetSizePreset
import com.example.ui.components.LiveClockCanvas
import com.example.ui.components.WallpaperBackdrop
import com.example.ui.components.rememberLiveClockMillis
import com.example.ui.viewmodel.ClockStudioUiState

@Composable
fun HomePreviewScreen(
  uiState: ClockStudioUiState,
  onSelectWallpaper: (WallpaperOption, String?) -> Unit,
  onSelectSizePreset: (WidgetSizePreset) -> Unit,
  onSetGrid: (Int, Int) -> Unit,
  onOpenStudio: () -> Unit,
  onPinWidget: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val liveTimeMillis = rememberLiveClockMillis(1000L)
  var showGridOverlay by remember { mutableStateOf(true) }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
      if (uri != null) {
        onSelectWallpaper(WallpaperOption.CUSTOM_PHOTO, uri.toString())
      }
    }

  Column(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
  ) {
    // Interactive Phone Home Screen Mockup Stage
    BoxWithConstraints(
      modifier =
        Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
      contentAlignment = Alignment.Center,
    ) {
      val maxPreviewW = (maxWidth - 8.dp).coerceAtMost(380.dp)
      Box(
        modifier =
          Modifier
            .width(maxPreviewW)
            .fillMaxSize()
            .clip(RoundedCornerShape(30.dp))
            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(30.dp)),
      ) {
        WallpaperBackdrop(
          wallpaper = uiState.previewWallpaper,
          customPhotoUri = uiState.customWallpaperUri,
          modifier = Modifier.fillMaxSize(),
        )

        Column(
          modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          // Simulated Android Status Bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "${uiState.previewCols}×${uiState.previewRows} GRID",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.85f),
            )
            Row(
              horizontalArrangement = Arrangement.spacedBy(5.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Rounded.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
              Icon(Icons.Rounded.SignalCellularAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
              Icon(Icons.Rounded.BatteryFull, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
          }

          // Home Screen Widget Placement Zone
          BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.TopCenter,
          ) {
            val cellW = maxWidth / 4f
            val cellH = (maxHeight / 4.4f).coerceAtMost(84.dp)
            val widgetW = cellW * uiState.previewCols
            val widgetH = cellH * uiState.previewRows

            Column(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.SpaceBetween,
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              // Resizable Widget with optional resize-handle outline
              Box(
                modifier =
                  Modifier
                    .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
                    .width(widgetW)
                    .height(widgetH)
                    .then(
                      if (showGridOverlay) {
                        Modifier.border(
                          1.5.dp,
                          MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                          RoundedCornerShape((uiState.activeConfig.cornerRadiusDp * 0.65f).dp),
                        )
                      } else {
                        Modifier
                      },
                    )
                    .padding(3.dp),
              ) {
                LiveClockCanvas(
                  config = uiState.activeConfig,
                  timeMillis = liveTimeMillis,
                  weather = uiState.weather,
                  modifier = Modifier.fillMaxSize(),
                )
              }

              // Mock Home Screen App Icons below widget (when rows < 4)
              if (uiState.previewRows <= 3) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                  horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                  MockAppIcon("Photos", Icons.Rounded.PhotoLibrary, Color(0xFF38BDF8))
                  MockAppIcon("Music", Icons.Rounded.MusicNote, Color(0xFFF43F5E))
                  MockAppIcon("Camera", Icons.Rounded.CameraAlt, Color(0xFFF59E0B))
                  MockAppIcon("Files", Icons.Rounded.Folder, Color(0xFF10B981))
                }
              }
            }
          }

          // Frosted Bottom App Dock
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = uiState.activeConfig.styleId.displayName,
              style = MaterialTheme.typography.labelLarge,
              color = Color.White,
            )
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color.White.copy(alpha = 0.16f),
              modifier =
                Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { showGridOverlay = !showGridOverlay }
                  .testTag("preview_toggle_guides_btn"),
            ) {
              Text(
                text = if (showGridOverlay) "Guides: ON" else "Guides: OFF",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
        }
      }
    }

    // Preview Control Deck
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 4.dp,
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Quick Size Presets
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(WidgetSizePreset.entries) { preset ->
            val selected = uiState.previewCols == preset.cols && uiState.previewRows == preset.rows
            FilterChip(
              selected = selected,
              onClick = { onSelectSizePreset(preset) },
              label = { Text(preset.label) },
              modifier = Modifier.testTag("preview_size_chip_${preset.name.lowercase()}"),
            )
          }
        }

        // Custom Freeform Width / Height Sliders
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Width: ${uiState.previewCols} cells",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
              value = uiState.previewCols.toFloat(),
              onValueChange = { onSetGrid(it.toInt(), uiState.previewRows) },
              valueRange = 2f..4f,
              steps = 1,
              modifier = Modifier.height(28.dp).testTag("preview_width_slider"),
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Height: ${uiState.previewRows} cells",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
              value = uiState.previewRows.toFloat(),
              onValueChange = { onSetGrid(uiState.previewCols, it.toInt()) },
              valueRange = 1f..4f,
              steps = 2,
              modifier = Modifier.height(28.dp).testTag("preview_height_slider"),
            )
          }
        }

        // Wallpaper Picker Row + Photo Picker
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(WallpaperOption.entries) { option ->
            val selected = uiState.previewWallpaper == option
            FilterChip(
              selected = selected,
              onClick = {
                if (option == WallpaperOption.CUSTOM_PHOTO) {
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                  )
                } else {
                  onSelectWallpaper(option, null)
                }
              },
              label = { Text(option.label) },
              leadingIcon = if (option == WallpaperOption.CUSTOM_PHOTO) {
                { Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp)) }
              } else null,
              modifier = Modifier.testTag("wallpaper_chip_${option.name.lowercase()}"),
            )
          }
        }

        // Bottom CTAs
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = onOpenStudio,
            modifier = Modifier.weight(1f).height(48.dp).testTag("preview_edit_studio_btn"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Edit Style")
          }
          Button(
            onClick = onPinWidget,
            modifier = Modifier.weight(1.3f).height(48.dp).testTag("preview_pin_btn"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Rounded.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Pin to Home")
          }
        }
      }
    }
  }
}

@Composable
private fun MockAppIcon(
  label: String,
  icon: ImageVector,
  tint: Color,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Box(
      modifier =
        Modifier
          .size(44.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(tint.copy(alpha = 0.22f))
          .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = Color.White.copy(alpha = 0.85f),
    )
  }
}
