package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val ChronoDarkColorScheme =
  darkColorScheme(
    primary = CyanElectric,
    onPrimary = ObsidianBg,
    primaryContainer = CyanContainerDark,
    onPrimaryContainer = TextPrimaryDark,
    secondary = AmberHorology,
    onSecondary = ObsidianBg,
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = RosePulse,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = ObsidianOutline,
  )

private val ChronoLightColorScheme =
  lightColorScheme(
    primary = CyanDeep,
    onPrimary = PearlSurface,
    primaryContainer = CyanContainerLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = AmberDeep,
    onSecondary = PearlSurface,
    secondaryContainer = AmberContainerLight,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = RosePulse,
    background = PearlBg,
    onBackground = TextPrimaryLight,
    surface = PearlSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = PearlSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = PearlOutline,
  )

@Composable
fun ChronoCraftTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> ChronoDarkColorScheme
      else -> ChronoLightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = ChronoTypography,
    content = content,
  )
}
