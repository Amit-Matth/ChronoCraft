package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppThemeMode
import com.example.model.ClockPresets
import com.example.model.ClockStyleId
import com.example.model.DisplayMode
import com.example.model.WeatherSnapshot
import com.example.ui.components.LiveClockCanvas
import com.example.ui.components.rememberLiveClockMillis
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
  appThemeMode: AppThemeMode,
  weather: WeatherSnapshot,
  onSelectThemeMode: (AppThemeMode) -> Unit,
  onComplete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val pagerState = rememberPagerState(pageCount = { 3 })
  val scope = rememberCoroutineScope()
  val liveMillis = rememberLiveClockMillis(1000L)

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              MaterialTheme.colorScheme.background,
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
              MaterialTheme.colorScheme.background,
            ),
          ),
        )
        .windowInsetsPadding(WindowInsets.safeDrawing),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier.fillMaxSize().widthIn(max = 560.dp).padding(horizontal = 20.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top Brand + Skip Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Image(
            painter = painterResource(id = R.drawable.chrono_app_logo_1795569986651),
            contentDescription = "ChronoCraft logo",
            contentScale = ContentScale.Crop,
            modifier =
              Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
          )
          Text(
            text = "ChronoCraft",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }

        TextButton(
          onClick = onComplete,
          modifier = Modifier.height(48.dp).testTag("onboarding_skip_btn"),
        ) {
          Text("Skip", style = MaterialTheme.typography.labelLarge)
        }
      }

      // Interactive Pager Showcase
      HorizontalPager(
        state = pagerState,
        modifier = Modifier.weight(1f).fillMaxWidth(),
      ) { page ->
        when (page) {
          0 -> OnboardingHeroPage(liveMillis = liveMillis, weather = weather)
          1 -> OnboardingAdaptivePage(liveMillis = liveMillis, weather = weather)
          else ->
            OnboardingThemePage(
              liveMillis = liveMillis,
              weather = weather,
              appThemeMode = appThemeMode,
              onSelectThemeMode = onSelectThemeMode,
            )
        }
      }

      // Bottom Page Dots + Next / Get Started CTA
      Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          repeat(3) { idx ->
            val selected = pagerState.currentPage == idx
            val width by animateDpAsState(
              targetValue = if (selected) 26.dp else 8.dp,
              animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
              label = "onboarding_dot_width",
            )
            Box(
              modifier =
                Modifier
                  .height(8.dp)
                  .width(width)
                  .clip(CircleShape)
                  .background(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                  ),
            )
          }
        }

        val isLast = pagerState.currentPage == 2
        Button(
          onClick = {
            if (isLast) {
              onComplete()
            } else {
              scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            }
          },
          shape = RoundedCornerShape(16.dp),
          contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
          modifier = Modifier.fillMaxWidth().height(54.dp).testTag("onboarding_primary_btn"),
        ) {
          if (isLast) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Crafting", style = MaterialTheme.typography.titleMedium)
          } else {
            Text("Next", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun OnboardingHeroPage(
  liveMillis: Long,
  weather: WeatherSnapshot,
) {
  val analogCfg = remember { ClockPresets.defaultConfigForStyle(ClockStyleId.FLOATING_ANALOG_PURE) }
  val digitalCfg = remember { ClockPresets.defaultConfigForStyle(ClockStyleId.WEIGHT_STACK_FLOAT) }

  Column(
    modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(30.dp),
      color = Color(0xFF0A0F1D),
      tonalElevation = 6.dp,
      modifier =
        Modifier
          .fillMaxWidth()
          .height(272.dp)
          .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(30.dp)),
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        Image(
          painter = painterResource(id = R.drawable.img_wallpaper_aurora_1795495861674),
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)))
        Row(
          modifier = Modifier.fillMaxSize().padding(18.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(modifier = Modifier.weight(1f).height(170.dp), contentAlignment = Alignment.Center) {
            LiveClockCanvas(
              config = analogCfg,
              timeMillis = liveMillis,
              weather = weather,
              modifier = Modifier.fillMaxSize(),
            )
          }
          Box(modifier = Modifier.weight(1f).height(170.dp), contentAlignment = Alignment.Center) {
            LiveClockCanvas(
              config = digitalCfg,
              timeMillis = liveMillis,
              weather = weather,
              modifier = Modifier.fillMaxSize(),
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = "Timeless Clocks,\nBuilt Your Way",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text = "Explore 46 handcrafted analog dials, frameless floating typography, neon frames, weather dashboards, and custom modular designs.",
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun OnboardingAdaptivePage(
  liveMillis: Long,
  weather: WeatherSnapshot,
) {
  var previewMode by remember { mutableStateOf(DisplayMode.HOME_SCREEN) }
  val orbitCfg = remember { ClockPresets.defaultConfigForStyle(ClockStyleId.SOLAR_ORBIT_PLANETS) }

  Column(
    modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(30.dp),
      color = Color.Black,
      tonalElevation = 6.dp,
      modifier =
        Modifier
          .fillMaxWidth()
          .height(272.dp)
          .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(30.dp)),
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(Color.White.copy(alpha = 0.10f))
              .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          DisplayMode.entries.forEach { mode ->
            val selected = previewMode == mode
            val icon = when (mode) {
              DisplayMode.HOME_SCREEN -> Icons.Rounded.Home
              DisplayMode.LOCK_SCREEN -> Icons.Rounded.Lock
              DisplayMode.AOD -> Icons.Rounded.Bedtime
            }
            Row(
              modifier =
                Modifier
                  .weight(1f)
                  .height(38.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                  .clickable { previewMode = mode },
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = icon,
                contentDescription = mode.label,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White,
                modifier = Modifier.size(15.dp),
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = mode.shortLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White,
              )
            }
          }
        }

        AnimatedContent(
          targetState = previewMode,
          transitionSpec = {
            (fadeIn(tween(240)) + scaleIn(initialScale = 0.93f, animationSpec = tween(240)))
              .togetherWith(fadeOut(tween(160)))
          },
          modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
          label = "onboarding_display_mode",
        ) { mode ->
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LiveClockCanvas(
              config = orbitCfg,
              timeMillis = liveMillis,
              weather = weather,
              displayMode = mode,
              modifier = Modifier.fillMaxSize(),
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = "One Design Across\nHome, Lock & AOD",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text = "Tap Home, Lock, or AOD above to watch the same identity transform from rich layered widgets into battery-conscious ambient mode.",
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun OnboardingThemePage(
  liveMillis: Long,
  weather: WeatherSnapshot,
  appThemeMode: AppThemeMode,
  onSelectThemeMode: (AppThemeMode) -> Unit,
) {
  val liquidCfg = remember { ClockPresets.defaultConfigForStyle(ClockStyleId.LIQUID_GLASS_PRISM) }

  Column(
    modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(30.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 4.dp,
      modifier =
        Modifier
          .fillMaxWidth()
          .height(272.dp)
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(30.dp)),
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .weight(1f)
              .clip(RoundedCornerShape(20.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(
              id = if (appThemeMode == AppThemeMode.LIGHT) R.drawable.img_wallpaper_glass_1795495862804
              else R.drawable.img_wallpaper_aurora_1795495861674,
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          LiveClockCanvas(
            config = liquidCfg,
            timeMillis = liveMillis,
            weather = weather,
            modifier = Modifier.fillMaxSize().padding(14.dp),
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          AppThemeMode.entries.forEach { mode ->
            val icon = when (mode) {
              AppThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
              AppThemeMode.LIGHT -> Icons.Rounded.LightMode
              AppThemeMode.DARK -> Icons.Rounded.DarkMode
            }
            FilterChip(
              selected = appThemeMode == mode,
              onClick = { onSelectThemeMode(mode) },
              label = { Text(mode.label) },
              leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
              modifier = Modifier.weight(1f).testTag("onboarding_theme_${mode.name.lowercase()}"),
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = "Choose Your Studio\nLight or Dark Look",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text = "ChronoCraft adapts effortlessly to Light Mode, Dark Mode, and Android themed icons while keeping your widgets sharp.",
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
