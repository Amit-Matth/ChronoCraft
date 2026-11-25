package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ShieldMoon
import androidx.compose.material.icons.rounded.Splitscreen
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AodExperienceSettings
import com.example.model.ClockCategory
import com.example.model.ClockStyleId
import com.example.model.DisplayMode
import com.example.model.SecondsStyle
import com.example.model.WeatherSnapshot
import com.example.model.WidgetConfig
import com.example.ui.components.LiveClockCanvas
import com.example.ui.components.rememberLiveClockMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun AodExperienceScreen(
  config: WidgetConfig,
  weather: WeatherSnapshot,
  aodSettings: AodExperienceSettings,
  onUpdateAodSettings: (AodExperienceSettings) -> Unit,
  onSwitchClockStyle: (ClockStyleId) -> Unit,
  onExit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onExit)

  val context = LocalContext.current
  val liveMillis = rememberLiveClockMillis(if (aodSettings.showSecondsPulse) 1000L else 5000L)
  var hudVisible by remember { mutableStateOf(true) }
  var hudInteractionTick by remember { mutableIntStateOf(0) }

  // Keep screen awake cleanly while AOD StandBy screen is open
  DisposableEffect(context) {
    val window = (context as? Activity)?.window
    val prevFlags = window?.attributes?.flags ?: 0
    window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    onDispose {
      if ((prevFlags and android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) == 0) {
        window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
    }
  }

  // Auto-hide controls after 5s of idle for true ambient OLED look
  LaunchedEffect(hudVisible, hudInteractionTick) {
    if (hudVisible) {
      delay(5000L)
      hudVisible = false
    }
  }

  val batteryInfo = remember(liveMillis / 15_000L) {
    runCatching {
      val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
      val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
      val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
      val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
      val pct = if (level >= 0 && scale > 0) ((level * 100) / scale).coerceIn(0, 100) else 85
      val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
      pct to charging
    }.getOrDefault(85 to false)
  }

  val minuteStep = ((liveMillis / 60_000L) % 6L).toInt()
  val shiftX by animateDpAsState(
    targetValue = if (aodSettings.burnInProtection) (((minuteStep % 3) - 1) * 8).dp else 0.dp,
    animationSpec = tween(900),
    label = "aod_shift_x",
  )
  val shiftY by animateDpAsState(
    targetValue = if (aodSettings.burnInProtection) (((minuteStep / 3) - 1) * 10).dp else 0.dp,
    animationSpec = tween(900),
    label = "aod_shift_y",
  )

  val aodClockConfig = remember(config, aodSettings.showSecondsPulse) {
    if (aodSettings.showSecondsPulse) {
      config.copy(secondsStyle = if (config.secondsStyle == SecondsStyle.NONE) SecondsStyle.DIGITS else config.secondsStyle)
    } else {
      config
    }
  }

  val aodRecommendedStyles = remember {
    listOf(
      ClockStyleId.WEIGHT_STACK_FLOAT,
      ClockStyleId.NOTHING_DOT_FLOAT,
      ClockStyleId.FLOATING_ANALOG_PURE,
      ClockStyleId.NEON_FRAME_GLOW,
      ClockStyleId.ANALOG_CHRONO,
      ClockStyleId.HYPER_PILL_FLOAT,
      ClockStyleId.SOLAR_ORBIT_PLANETS,
      ClockStyleId.ANALOG_SWISS,
    )
  }

  val dateText = remember(liveMillis / 60_000L) {
    SimpleDateFormat("EEEE • MMM d", Locale.getDefault()).format(Date(liveMillis))
  }

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color.Black)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
        ) {
          hudVisible = !hudVisible
          hudInteractionTick++
        }
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .testTag("aod_experience_root"),
  ) {
    val isLandscapeSplit = maxWidth >= 540.dp && aodSettings.standbySplitLandscape

    // Ambient OLED Clock Stage
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .alpha(aodSettings.dimLevel.coerceIn(0.25f, 1.0f))
          .padding(horizontal = 24.dp, vertical = 20.dp),
      contentAlignment = Alignment.Center,
    ) {
      if (isLandscapeSplit) {
        Row(
          modifier =
            Modifier
              .fillMaxSize()
              .widthIn(max = 980.dp)
              .offset(x = shiftX, y = shiftY),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
          val isAnalog = config.styleId.category == ClockCategory.ANALOG ||
            config.styleId == ClockStyleId.FLOATING_ANALOG_PURE ||
            config.styleId == ClockStyleId.SOLAR_ORBIT_PLANETS
          Box(
            modifier =
              Modifier
                .weight(1.25f)
                .height(if (isAnalog) 260.dp else 210.dp),
            contentAlignment = Alignment.Center,
          ) {
            LiveClockCanvas(
              config = aodClockConfig,
              timeMillis = liveMillis,
              weather = weather,
              displayMode = DisplayMode.AOD,
              modifier = Modifier.fillMaxSize(),
            )
          }

          if (aodSettings.showBatteryAndWeather) {
            Column(
              modifier =
                Modifier
                  .weight(0.85f)
                  .clip(RoundedCornerShape(24.dp))
                  .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
                  .background(Color.White.copy(alpha = 0.04f))
                  .padding(20.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Text(
                text = "STANDBY AOD",
                style = MaterialTheme.typography.labelSmall,
                color = Color(config.accentColor).copy(alpha = 0.85f),
              )
              Text(
                text = dateText,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.90f),
                fontWeight = FontWeight.Medium,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.WbSunny,
                  contentDescription = null,
                  tint = Color(0xFFFBBF24).copy(alpha = 0.88f),
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = if (weather.isLoaded) "${weather.displayTemp(false)} • ${weather.cityName}" else weather.cityName,
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color.White.copy(alpha = 0.78f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = if (batteryInfo.second) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull,
                  contentDescription = null,
                  tint = if (batteryInfo.second) Color(0xFF4ADE80) else Color.White.copy(alpha = 0.72f),
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = if (batteryInfo.second) "${batteryInfo.first}% • Charging" else "${batteryInfo.first}% Battery",
                  style = MaterialTheme.typography.labelLarge,
                  color = Color.White.copy(alpha = 0.82f),
                )
              }
            }
          }
        }
      } else {
        Column(
          modifier = Modifier.fillMaxSize().offset(x = shiftX, y = shiftY),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = dateText.uppercase(),
              style = MaterialTheme.typography.labelMedium,
              color = Color.White.copy(alpha = 0.56f),
            )
            if (aodSettings.burnInProtection) {
              Text(
                text = "Burn-In Guard Active • Tap for controls",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.28f),
              )
            }
          }

          val isAnalog = config.styleId.category == ClockCategory.ANALOG ||
            config.styleId == ClockStyleId.FLOATING_ANALOG_PURE ||
            config.styleId == ClockStyleId.SOLAR_ORBIT_PLANETS
          Box(
            modifier =
              Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .height(if (isAnalog) 250.dp else 190.dp),
            contentAlignment = Alignment.Center,
          ) {
            LiveClockCanvas(
              config = aodClockConfig,
              timeMillis = liveMillis,
              weather = weather,
              displayMode = DisplayMode.AOD,
              modifier = Modifier.fillMaxSize(),
            )
          }

          if (aodSettings.showBatteryAndWeather) {
            Row(
              modifier =
                Modifier
                  .clip(RoundedCornerShape(18.dp))
                  .background(Color.White.copy(alpha = 0.06f))
                  .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                  .padding(horizontal = 16.dp, vertical = 9.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Icon(
                  imageVector = if (batteryInfo.second) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull,
                  contentDescription = null,
                  tint = if (batteryInfo.second) Color(0xFF4ADE80) else Color.White.copy(alpha = 0.72f),
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = if (batteryInfo.second) "${batteryInfo.first}% Charging" else "${batteryInfo.first}%",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White.copy(alpha = 0.78f),
                )
              }
              if (weather.isLoaded) {
                Text("•", color = Color.White.copy(alpha = 0.35f))
                Text(
                  text = "${weather.displayTemp(false)} ${weather.cityName}",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White.copy(alpha = 0.72f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          } else {
            Spacer(modifier = Modifier.height(8.dp))
          }
        }
      }
    }

    // Floating Auto-Hiding HUD Controls
    AnimatedVisibility(
      visible = hudVisible,
      enter = fadeIn(tween(200)),
      exit = fadeOut(tween(260)),
      modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
    ) {
      IconButton(
        onClick = onExit,
        modifier =
          Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .testTag("aod_exit_btn"),
      ) {
        Icon(Icons.Rounded.Close, contentDescription = "Exit AOD experience", tint = Color.White)
      }
    }

    AnimatedVisibility(
      visible = hudVisible,
      enter = fadeIn(tween(220)) + slideInVertically(initialOffsetY = { it / 2 }),
      exit = fadeOut(tween(220)) + slideOutVertically(targetOffsetY = { it / 2 }),
      modifier = Modifier.align(Alignment.BottomCenter).padding(14.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF101522).copy(alpha = 0.94f),
        modifier =
          Modifier
            .fillMaxWidth()
            .widthIn(max = 620.dp)
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(26.dp))
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null,
            ) { hudInteractionTick++ },
      ) {
        Column(
          modifier = Modifier.padding(14.dp).verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          // Quick Clock Style Switcher
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(aodRecommendedStyles, key = { it.name }) { styleId ->
              val selected = config.styleId == styleId
              FilterChip(
                selected = selected,
                onClick = {
                  hudInteractionTick++
                  onSwitchClockStyle(styleId)
                },
                label = { Text(styleId.displayName, maxLines = 1) },
                colors = FilterChipDefaults.filterChipColors(
                  containerColor = Color.White.copy(alpha = 0.06f),
                  labelColor = Color.White.copy(alpha = 0.85f),
                  selectedContainerColor = Color(0xFF38BDF8),
                  selectedLabelColor = Color(0xFF07111E),
                ),
                modifier = Modifier.testTag("aod_style_chip_${styleId.name.lowercase()}"),
              )
            }
          }

          // OLED Brightness Dimmer Slider
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              Icons.Rounded.BrightnessMedium,
              contentDescription = "AOD dimmer",
              tint = Color.White.copy(alpha = 0.8f),
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = "Dimmer ${(aodSettings.dimLevel * 100).toInt()}%",
              style = MaterialTheme.typography.labelMedium,
              color = Color.White,
            )
            Slider(
              value = aodSettings.dimLevel,
              onValueChange = {
                hudInteractionTick++
                onUpdateAodSettings(aodSettings.copy(dimLevel = it.coerceIn(0.25f, 1.0f)))
              },
              valueRange = 0.25f..1.0f,
              colors = SliderDefaults.colors(
                thumbColor = Color(0xFF38BDF8),
                activeTrackColor = Color(0xFF38BDF8),
              ),
              modifier = Modifier.weight(1f).height(28.dp).testTag("aod_dimmer_slider"),
            )
          }

          // Feature Toggles + Screen Saver Link
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
              FilterChip(
                selected = aodSettings.burnInProtection,
                onClick = {
                  hudInteractionTick++
                  onUpdateAodSettings(aodSettings.copy(burnInProtection = !aodSettings.burnInProtection))
                },
                leadingIcon = { Icon(Icons.Rounded.ShieldMoon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Burn-In Shift") },
              )
            }
            item {
              FilterChip(
                selected = aodSettings.standbySplitLandscape,
                onClick = {
                  hudInteractionTick++
                  onUpdateAodSettings(aodSettings.copy(standbySplitLandscape = !aodSettings.standbySplitLandscape))
                },
                leadingIcon = { Icon(Icons.Rounded.Splitscreen, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("StandBy Split") },
              )
            }
            item {
              FilterChip(
                selected = aodSettings.showBatteryAndWeather,
                onClick = {
                  hudInteractionTick++
                  onUpdateAodSettings(aodSettings.copy(showBatteryAndWeather = !aodSettings.showBatteryAndWeather))
                },
                label = { Text("Info Pill") },
              )
            }
            item {
              FilterChip(
                selected = aodSettings.showSecondsPulse,
                onClick = {
                  hudInteractionTick++
                  onUpdateAodSettings(aodSettings.copy(showSecondsPulse = !aodSettings.showSecondsPulse))
                },
                label = { Text("1s Pulse") },
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = {
                hudInteractionTick++
                val dreamIntent = Intent(Settings.ACTION_DREAM_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val fallback = Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(dreamIntent) }
                  .onFailure { runCatching { context.startActivity(fallback) } }
              },
              modifier = Modifier.weight(1f).height(48.dp).testTag("aod_system_dream_btn"),
              shape = RoundedCornerShape(14.dp),
            ) {
              Icon(Icons.Rounded.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
              Spacer(modifier = Modifier.width(6.dp))
              Text("System Screen Saver", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FilledTonalButton(
              onClick = onExit,
              modifier = Modifier.height(48.dp).testTag("aod_done_btn"),
              shape = RoundedCornerShape(14.dp),
            ) {
              Text("Exit")
            }
          }
        }
      }
    }
  }
}
