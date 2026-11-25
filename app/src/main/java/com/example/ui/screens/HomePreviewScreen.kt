package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ClockCategory
import com.example.model.ClockStyleId
import com.example.model.DisplayMode
import com.example.model.LiveWallpaperTarget
import com.example.model.WallpaperOption
import com.example.model.WidgetSizePreset
import com.example.ui.components.LiveClockCanvas
import com.example.ui.components.WallpaperBackdrop
import com.example.ui.components.rememberLiveClockMillis
import com.example.ui.viewmodel.ClockStudioUiState
import com.example.widget.ChronoLiveWallpaperService
import com.example.widget.ClockArtStyles
import java.util.Locale

@Composable
fun HomePreviewScreen(
  uiState: ClockStudioUiState,
  onSelectDisplayMode: (DisplayMode) -> Unit = {},
  onSelectWallpaper: (WallpaperOption, String?) -> Unit,
  onSelectLiveWallpaperTarget: (LiveWallpaperTarget) -> Unit = {},
  onSelectSizePreset: (WidgetSizePreset) -> Unit,
  onSetGrid: (Int, Int) -> Unit,
  onSetTimeTravel: (Boolean, Int?) -> Unit = { _, _ -> },
  onOpenAodExperience: () -> Unit = {},
  onOpenIconPackStudio: () -> Unit = {},
  onOpenStudio: () -> Unit,
  onPinWidget: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val liveTimeMillis = rememberLiveClockMillis(1000L)
  val timeTravelMinutes = if (uiState.timeTravelEnabled) uiState.timeTravelMinutes else null
  var showGridOverlay by remember { mutableStateOf(true) }
  val activeMode = uiState.activeDisplayMode
  val config = uiState.activeConfig

  val photoPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
      if (uri != null) {
        onSelectWallpaper(WallpaperOption.CUSTOM_PHOTO, uri.toString())
      }
    }

  BoxWithConstraints(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
  ) {
    val isTwoPane = maxWidth >= 700.dp

    @Composable
    fun DisplayModeSwitcher(mod: Modifier = Modifier) {
      Row(
        modifier =
          mod
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        DisplayMode.entries.forEach { mode ->
          val selected = activeMode == mode
          val icon = when (mode) {
            DisplayMode.HOME_SCREEN -> Icons.Rounded.Home
            DisplayMode.LOCK_SCREEN -> Icons.Rounded.Lock
            DisplayMode.AOD -> Icons.Rounded.Bedtime
          }
          Row(
            modifier =
              Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onSelectDisplayMode(mode) }
                .testTag("preview_display_mode_${mode.name.lowercase()}"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = icon,
              contentDescription = mode.label,
              tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
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
    fun PhoneStageBox(mod: Modifier = Modifier) {
      BoxWithConstraints(
        modifier = mod,
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
        if (activeMode == DisplayMode.AOD) {
          Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        } else {
          WallpaperBackdrop(
            wallpaper = uiState.previewWallpaper,
            customPhotoUri = uiState.customWallpaperUri,
            modifier = Modifier.fillMaxSize(),
          )
          if (activeMode == DisplayMode.LOCK_SCREEN) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)))
          }
        }

        AnimatedContent(
          targetState = activeMode,
          transitionSpec = {
            if (config.animationsEnabled) {
              (fadeIn(tween(260)) + scaleIn(initialScale = 0.96f, animationSpec = tween(260)))
                .togetherWith(fadeOut(tween(200)))
            } else {
              fadeIn(tween(0)).togetherWith(fadeOut(tween(0)))
            }
          },
          modifier = Modifier.fillMaxSize(),
          label = "phone_display_preview",
        ) { mode ->
          when (mode) {
            DisplayMode.HOME_SCREEN ->
              Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "${uiState.previewCols}×${uiState.previewRows} HOME GRID",
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
                                RoundedCornerShape((config.cornerRadiusDp * 0.65f).dp),
                              )
                            } else {
                              Modifier
                            },
                          )
                          .padding(3.dp),
                    ) {
                      LiveClockCanvas(
                        config = config,
                        timeMillis = liveTimeMillis,
                        weather = uiState.weather,
                        displayMode = DisplayMode.HOME_SCREEN,
                        timeTravelMinutesOfDay = timeTravelMinutes,
                        modifier = Modifier.fillMaxSize(),
                      )
                    }

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
                    text = config.styleId.displayName,
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

            DisplayMode.LOCK_SCREEN ->
              Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "LOCK SCREEN",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                  )
                  Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Rounded.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Icon(Icons.Rounded.BatteryFull, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                  }
                }

                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Lock Screen icon",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(20.dp),
                  )
                  val isAnalog = config.styleId.category == ClockCategory.ANALOG || config.styleId == ClockStyleId.FLOATING_ANALOG_PURE
                  val lockH = if (isAnalog) 160.dp else 132.dp
                  Box(
                    modifier = Modifier.fillMaxWidth().height(lockH),
                    contentAlignment = Alignment.Center,
                  ) {
                    LiveClockCanvas(
                      config = config,
                      timeMillis = liveTimeMillis,
                      weather = uiState.weather,
                      displayMode = DisplayMode.LOCK_SCREEN,
                      timeTravelMinutesOfDay = timeTravelMinutes,
                      modifier = Modifier.fillMaxSize(),
                    )
                  }
                  Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.34f),
                    modifier = Modifier.fillMaxWidth(0.88f),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      Icon(Icons.Rounded.NotificationsNone, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                      Text(
                        text = "2 notifications",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.92f),
                      )
                    }
                  }
                }

                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(
                    text = "Swipe up to unlock",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f),
                  )
                  Box(
                    modifier =
                      Modifier
                        .width(96.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.65f)),
                  )
                }
              }

            DisplayMode.AOD -> {
              // Subtle burn-in shift across minutes
              val minuteIdx = ((liveTimeMillis / 60_000L) % 6L).toInt()
              val shiftX = (((minuteIdx % 3) - 1) * 4).dp
              val shiftY = (((minuteIdx / 3) - 1) * 6).dp
              val batteryPct = remember(liveTimeMillis / 30_000L) { ClockArtStyles.getBatteryPercent(context) }

              Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "ALWAYS-ON DISPLAY",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.36f),
                )

                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(14.dp),
                  modifier = Modifier.fillMaxWidth().offset(x = shiftX, y = shiftY),
                ) {
                  Box(
                    modifier = Modifier.fillMaxWidth().height(130.dp),
                    contentAlignment = Alignment.Center,
                  ) {
                    LiveClockCanvas(
                      config = config,
                      timeMillis = liveTimeMillis,
                      weather = uiState.weather,
                      displayMode = DisplayMode.AOD,
                      timeTravelMinutesOfDay = timeTravelMinutes,
                      modifier = Modifier.fillMaxSize(),
                    )
                  }
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(
                      Icons.Rounded.NotificationsNone,
                      contentDescription = "Notifications badge",
                      tint = Color.White.copy(alpha = 0.45f),
                      modifier = Modifier.size(16.dp),
                    )
                    Icon(
                      Icons.Rounded.MailOutline,
                      contentDescription = "Messages badge",
                      tint = Color.White.copy(alpha = 0.45f),
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    Icons.Rounded.BatteryChargingFull,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.50f),
                    modifier = Modifier.size(15.dp),
                  )
                  Text(
                    text = "$batteryPct%  •  Low-Power OLED",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.50f),
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
    fun PreviewControlDeck(mod: Modifier = Modifier) {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shape = if (isTwoPane) RoundedCornerShape(24.dp) else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = mod,
      ) {
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          when (activeMode) {
            DisplayMode.HOME_SCREEN -> {
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

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                FilledTonalButton(
                  onClick = {
                    onSelectLiveWallpaperTarget(LiveWallpaperTarget.HOME_SCREEN)
                    ChronoLiveWallpaperService.launchWallpaperPicker(context)
                  },
                  modifier = Modifier.weight(1f).height(46.dp).testTag("preview_home_wallpaper_btn"),
                  shape = RoundedCornerShape(14.dp),
                ) {
                  Icon(Icons.Rounded.Wallpaper, contentDescription = null, modifier = Modifier.size(17.dp))
                  Spacer(modifier = Modifier.width(5.dp))
                  Text("Live Wallpaper", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FilledTonalButton(
                  onClick = onOpenIconPackStudio,
                  modifier = Modifier.weight(1f).height(46.dp).testTag("preview_open_icons_btn"),
                  shape = RoundedCornerShape(14.dp),
                ) {
                  Icon(Icons.Rounded.Apps, contentDescription = null, modifier = Modifier.size(17.dp))
                  Spacer(modifier = Modifier.width(5.dp))
                  Text("Icon Studio", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
              }
            }

            DisplayMode.LOCK_SCREEN -> {
              Text(
                text = "Apply ${config.styleId.displayName} as a Live Lock Screen Wallpaper, or add it as a keyguard widget on Samsung LockStar / Android 15+ devices.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LiveWallpaperTarget.entries) { target ->
                  FilterChip(
                    selected = uiState.liveWallpaperTarget == target,
                    onClick = { onSelectLiveWallpaperTarget(target) },
                    label = { Text(target.label) },
                    modifier = Modifier.testTag("lock_target_${target.name.lowercase()}"),
                  )
                }
              }
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
                    modifier = Modifier.testTag("wallpaper_chip_${option.name.lowercase()}"),
                  )
                }
              }
              FilledTonalButton(
                onClick = { ChronoLiveWallpaperService.launchWallpaperPicker(context) },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("preview_set_lock_wallpaper_btn"),
                shape = RoundedCornerShape(14.dp),
              ) {
                Icon(Icons.Rounded.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Set Lock / Home Live Wallpaper")
              }
            }

            DisplayMode.AOD -> {
              Text(
                text = "Launch ChronoCraft StandBy AOD (OLED true-black with burn-in pixel shift & dimmer) or enable the system charging Screen Saver.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Button(
                  onClick = onOpenAodExperience,
                  modifier = Modifier.weight(1.1f).height(48.dp).testTag("preview_launch_aod_btn"),
                  shape = RoundedCornerShape(14.dp),
                ) {
                  Icon(Icons.Rounded.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Launch AOD", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FilledTonalButton(
                  onClick = {
                    val dreamIntent = Intent(Settings.ACTION_DREAM_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val fallback = Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(dreamIntent) }
                      .onFailure { runCatching { context.startActivity(fallback) } }
                  },
                  modifier = Modifier.weight(1f).height(48.dp).testTag("preview_open_dream_settings_btn"),
                  shape = RoundedCornerShape(14.dp),
                ) {
                  Text("Screen Saver", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            FilterChip(
              selected = uiState.timeTravelEnabled,
              onClick = { onSetTimeTravel(!uiState.timeTravelEnabled, null) },
              label = {
                Text(
                  if (uiState.timeTravelEnabled) {
                    "Time Travel ${String.format(Locale.US, "%02d:%02d", uiState.timeTravelMinutes / 60, uiState.timeTravelMinutes % 60)}"
                  } else {
                    "Time Travel"
                  },
                )
              },
              leadingIcon = { Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) },
              modifier = Modifier.testTag("preview_time_travel_chip"),
            )
            if (uiState.timeTravelEnabled) {
              Slider(
                value = uiState.timeTravelMinutes.toFloat(),
                onValueChange = { onSetTimeTravel(true, it.toInt()) },
                valueRange = 0f..1439f,
                modifier = Modifier.weight(1f).height(28.dp),
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
              Text("Edit Style", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
              onClick = onPinWidget,
              modifier = Modifier.weight(1.3f).height(48.dp).testTag("preview_pin_btn"),
              shape = RoundedCornerShape(14.dp),
            ) {
              Icon(Icons.Rounded.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Pin to Home", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }
    }

    if (isTwoPane) {
      Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Column(
          modifier = Modifier.weight(1f).fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          DisplayModeSwitcher()
          PhoneStageBox(mod = Modifier.weight(1f).fillMaxWidth())
        }
        PreviewControlDeck(mod = Modifier.weight(1f).fillMaxSize())
      }
    } else {
      Column(modifier = Modifier.fillMaxSize()) {
        DisplayModeSwitcher(mod = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        PhoneStageBox(
          mod =
            Modifier
              .weight(1f)
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 4.dp),
        )
        PreviewControlDeck(
          mod =
            Modifier
              .fillMaxWidth()
              .weight(0.78f, fill = false),
        )
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
