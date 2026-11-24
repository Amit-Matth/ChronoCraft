package com.example.model

import androidx.annotation.FontRes
import com.example.R
import com.squareup.moshi.JsonClass

enum class ClockCategory(val label: String) {
  ALL("All"),
  DASHBOARD("Weather & Cal"),
  DIGITAL("Digital"),
  ANALOG("Analog"),
  WORD("Word & Duo"),
}

enum class ClockStyleId(
  val displayName: String,
  val subtitle: String,
  val category: ClockCategory,
  val defaultFont: ClockFontId,
) {
  // New Advanced Depth, Expandable, Calendar & Weather Styles
  BENTO_CALENDAR(
    displayName = "Bento Cal & Weather",
    subtitle = "Expandable depth tiles + month calendar",
    category = ClockCategory.DASHBOARD,
    defaultFont = ClockFontId.SPACE_GROTESK,
  ),
  DEPTH_GLASS_WEATHER(
    displayName = "Aurora Depth",
    subtitle = "3D glass depth + live weather & forecast",
    category = ClockCategory.DASHBOARD,
    defaultFont = ClockFontId.OUTFIT,
  ),
  EXPANDABLE_STACK(
    displayName = "Dynamic Stack",
    subtitle = "Layered floating cards that expand",
    category = ClockCategory.DASHBOARD,
    defaultFont = ClockFontId.PLUS_JAKARTA_SANS,
  ),
  WORLD_AGENDA_PRO(
    displayName = "Calendar Matrix",
    subtitle = "Full month calendar + live weather",
    category = ClockCategory.DASHBOARD,
    defaultFont = ClockFontId.SPACE_GROTESK,
  ),
  CHRONO_3D_DEPTH(
    displayName = "3D Recessed Dial",
    subtitle = "Sculpted bezel depth + weather window",
    category = ClockCategory.ANALOG,
    defaultFont = ClockFontId.PLUS_JAKARTA_SANS,
  ),
  NEUMORPH_SCULPT(
    displayName = "Soft Depth 3D",
    subtitle = "Tactile beveled housing + weather",
    category = ClockCategory.DIGITAL,
    defaultFont = ClockFontId.OUTFIT,
  ),

  // Core Digital Styles
  CYBER_HUD(
    displayName = "Neo Cyber HUD",
    subtitle = "Segmented brackets & telemetry",
    category = ClockCategory.DIGITAL,
    defaultFont = ClockFontId.ORBITRON,
  ),
  RETRO_FLIP(
    displayName = "Retro Flip Card",
    subtitle = "Mechanical split-flap tiles",
    category = ClockCategory.DIGITAL,
    defaultFont = ClockFontId.BEBAS_NEUE,
  ),
  GLASS_PILL(
    displayName = "Frosted Glass",
    subtitle = "Translucent card with date pill",
    category = ClockCategory.DIGITAL,
    defaultFont = ClockFontId.OUTFIT,
  ),
  BRUTALIST_POSTER(
    displayName = "Brutalist Poster",
    subtitle = "Bold stacked ink & offset frame",
    category = ClockCategory.DIGITAL,
    defaultFont = ClockFontId.SYNE,
  ),
  TERMINAL_MONO(
    displayName = "Terminal Matrix",
    subtitle = "Monospace dev readout & progress",
    category = ClockCategory.DIGITAL,
    defaultFont = ClockFontId.JETBRAINS_MONO,
  ),
  EDITORIAL_SERIF(
    displayName = "Editorial Luxe",
    subtitle = "Magazine serif & hairline rules",
    category = ClockCategory.DIGITAL,
    defaultFont = ClockFontId.PLAYFAIR_DISPLAY,
  ),

  // Analog Styles
  ANALOG_SWISS(
    displayName = "Swiss Railway",
    subtitle = "Iconic baton ticks & bold sweep",
    category = ClockCategory.ANALOG,
    defaultFont = ClockFontId.SPACE_GROTESK,
  ),
  ANALOG_CHRONO(
    displayName = "Obsidian Chrono",
    subtitle = "Concentric dial & luminous hands",
    category = ClockCategory.ANALOG,
    defaultFont = ClockFontId.PLUS_JAKARTA_SANS,
  ),
  ANALOG_BAUHAUS(
    displayName = "Bauhaus Geo",
    subtitle = "Architectural arcs & dot markers",
    category = ClockCategory.ANALOG,
    defaultFont = ClockFontId.OUTFIT,
  ),
  ANALOG_ORBIT(
    displayName = "Minimal Orbit",
    subtitle = "Floating perimeter planet hand",
    category = ClockCategory.ANALOG,
    defaultFont = ClockFontId.SPACE_GROTESK,
  ),
  ANALOG_ROMAN(
    displayName = "Classic Roman",
    subtitle = "Dress dial with Roman numerals",
    category = ClockCategory.ANALOG,
    defaultFont = ClockFontId.PLAYFAIR_DISPLAY,
  ),

  // Word & Duo Styles
  WORD_MATRIX(
    displayName = "Word Matrix",
    subtitle = "Illuminated typographic letter grid",
    category = ClockCategory.WORD,
    defaultFont = ClockFontId.SPACE_GROTESK,
  ),
  WORD_STATEMENT(
    displayName = "Poetic Time",
    subtitle = "Full-sentence editorial statement",
    category = ClockCategory.WORD,
    defaultFont = ClockFontId.PLAYFAIR_DISPLAY,
  ),
  WORD_DUO_BAR(
    displayName = "Hybrid Duo",
    subtitle = "Analog dial paired with digital bar",
    category = ClockCategory.WORD,
    defaultFont = ClockFontId.PLUS_JAKARTA_SANS,
  ),
}

enum class ClockFontId(
  val displayName: String,
  @FontRes val fontRes: Int,
  val sampleText: String,
) {
  SPACE_GROTESK("Space Grotesk", R.font.space_grotesk, "10:42"),
  PLUS_JAKARTA_SANS("Jakarta Sans", R.font.plus_jakarta_sans, "10:42"),
  JETBRAINS_MONO("JetBrains Mono", R.font.jetbrains_mono, "10:42"),
  SYNE("Syne Display", R.font.syne, "10:42"),
  PLAYFAIR_DISPLAY("Playfair Serif", R.font.playfair_display, "10:42"),
  BEBAS_NEUE("Bebas Neue", R.font.bebas_neue, "10:42"),
  ORBITRON("Orbitron HUD", R.font.orbitron, "10:42"),
  OUTFIT("Outfit Round", R.font.outfit, "10:42"),
}

@JsonClass(generateAdapter = true)
data class WeatherSnapshot(
  val cityName: String = "San Francisco",
  val latitude: Double = 37.7749,
  val longitude: Double = -122.4194,
  val tempCelsius: Double = 0.0,
  val feelsLikeCelsius: Double = 0.0,
  val highCelsius: Double = 0.0,
  val lowCelsius: Double = 0.0,
  val windKmh: Double = 0.0,
  val weatherCode: Int = 0,
  val isDay: Boolean = true,
  val isLoaded: Boolean = false,
  val updatedAtMillis: Long = 0L,
) {
  fun displayTemp(useFahrenheit: Boolean): String {
    if (!isLoaded) return "--°"
    val value = if (useFahrenheit) (tempCelsius * 9.0 / 5.0 + 32.0) else tempCelsius
    return "${kotlin.math.round(value).toInt()}°"
  }

  fun displayHighLow(useFahrenheit: Boolean): String {
    if (!isLoaded) return "H --°  L --°"
    val h = if (useFahrenheit) (highCelsius * 9.0 / 5.0 + 32.0) else highCelsius
    val l = if (useFahrenheit) (lowCelsius * 9.0 / 5.0 + 32.0) else lowCelsius
    return "H ${kotlin.math.round(h).toInt()}°  L ${kotlin.math.round(l).toInt()}°"
  }

  fun conditionText(): String {
    if (!isLoaded) return "Weather offline"
    return when (weatherCode) {
      0 -> if (isDay) "Clear Sky" else "Clear Night"
      1, 2 -> "Partly Cloudy"
      3 -> "Overcast"
      45, 48 -> "Foggy"
      51, 53, 55, 56, 57 -> "Drizzle"
      61, 63, 65, 66, 67, 80, 81, 82 -> "Rain"
      71, 73, 75, 77, 85, 86 -> "Snow"
      95, 96, 99 -> "Thunderstorm"
      else -> "Fair"
    }
  }
}

@JsonClass(generateAdapter = true)
data class WidgetConfig(
  val styleId: ClockStyleId = ClockStyleId.BENTO_CALENDAR,
  val fontId: ClockFontId = ClockFontId.SPACE_GROTESK,
  val backgroundColor: Int = 0xFF0F172A.toInt(),
  val accentColor: Int = 0xFF38BDF8.toInt(),
  val foregroundColor: Int = 0xFFF8FAFC.toInt(),
  val secondaryTextColor: Int = 0xFF94A3B8.toInt(),
  val backgroundAlpha: Float = 0.92f,
  val cornerRadiusDp: Float = 26f,
  val fontScale: Float = 1.0f,
  val depthIntensity: Float = 0.85f,
  val is24Hour: Boolean = false,
  val showSeconds: Boolean = true,
  val showDate: Boolean = true,
  val showWeather: Boolean = true,
  val useFahrenheit: Boolean = false,
  val showBorder: Boolean = true,
  val customLabel: String = "LOCAL TIME",
)

@JsonClass(generateAdapter = true)
data class SavedClockPreset(
  val id: String,
  val name: String,
  val config: WidgetConfig,
  val createdAtMillis: Long = System.currentTimeMillis(),
)

data class ColorPalettePreset(
  val id: String,
  val name: String,
  val backgroundColor: Int,
  val accentColor: Int,
  val foregroundColor: Int,
  val secondaryTextColor: Int,
  val backgroundAlpha: Float = 0.92f,
)

enum class WallpaperOption(val label: String) {
  AURORA("Obsidian Aurora"),
  TERRACOTTA("Bauhaus Sand"),
  GLASS("Frosted Pearl"),
  AMOLED("Pure AMOLED"),
  CUSTOM_PHOTO("My Photo"),
}

enum class WidgetSizePreset(
  val label: String,
  val cols: Int,
  val rows: Int,
) {
  PILL_2X1("2×1 Pill", 2, 1),
  SQUARE_2X2("2×2 Square", 2, 2),
  STRIP_4X1("4×1 Strip", 4, 1),
  CLASSIC_4X2("4×2 Classic", 4, 2),
  EXPANDED_4X3("4×3 Expanded", 4, 3),
  HERO_4X4("4×4 Hero", 4, 4),
}

data class ActiveHomeWidget(
  val appWidgetId: Int,
  val providerLabel: String,
  val config: WidgetConfig,
)
