package com.example.model

import androidx.annotation.DrawableRes
import androidx.annotation.FontRes
import com.example.R
import com.squareup.moshi.JsonClass

enum class CardStyle(val label: String) {
  TRANSPARENT("No Card"),
  FROSTED("Glass"),
  SOLID("Card"),
}

enum class DisplayMode(val label: String, val shortLabel: String) {
  HOME_SCREEN("Home Screen", "Home"),
  LOCK_SCREEN("Lock Screen", "Lock"),
  AOD("Always-On Display", "AOD"),
}

enum class AppThemeMode(val label: String) {
  SYSTEM("Auto"),
  LIGHT("Light"),
  DARK("Dark"),
}

enum class LibraryLayoutMode(val label: String) {
  STAGGERED("Staggered"),
  DENSE("Dense"),
  LIST("List"),
}

enum class BackgroundFillMode(val label: String) {
  NONE("None"),
  SOLID("Solid"),
  GRADIENT("Gradient"),
  GLASS("Glass"),
  ARTWORK("Art"),
  PHOTO("Photo"),
}

enum class ClockAnimationStyle(val label: String, val subtitle: String) {
  NONE("Static", "Zero motion, ultra battery-friendly"),
  GLOW_PULSE("Glow Pulse", "Soft breathing ambient halo"),
  BREATHE("Breathing", "Gentle rhythmic scale & luminance"),
  ORBIT_SPIN("Orbit", "Revolving celestial satellites"),
  WAVE_DRIFT("Fluid Wave", "Undulating liquid shimmer"),
  PARTICLE_FLOAT("Starlight", "Drifting micro-star particles"),
  BOUNCE_TICK("Kinetic Tick", "Crisp elastic second accent"),
  GRADIENT_SHIFT("Chroma Shift", "Flowing aurora gradient sweep"),
  RING_SWEEP("Ring Sweep", "Continuous luminous perimeter arc"),
}

enum class SecondsStyle(val label: String) {
  NONE("Hidden"),
  DIGITS("Digits"),
  RING("Progress Ring"),
  DOT_ORBIT("Orbit Dot"),
  PROGRESS_BAR("Linear Bar"),
}

enum class ClockDateFormat(val label: String, val pattern: String) {
  SHORT("Mon, Nov 24", "EEE, MMM d"),
  FULL("Monday • Nov 24", "EEEE • MMM d"),
  NUMERIC("11 / 24", "MM / dd"),
  MONTH_DAY("NOV 24", "MMM d"),
}

enum class ClockAlignment(val label: String) {
  START("Left"),
  CENTER("Center"),
  END("Right"),
}

enum class BuilderLayoutType(val label: String) {
  CENTERED_HERO("Centered Hero"),
  STACKED_DUO("Stacked Duo"),
  SPLIT_SPINE("Split Spine"),
  RING_DIGITAL("Orbit Ring"),
  ANALOG_HYBRID("Analog Duo"),
}

enum class BuilderMotif(val label: String) {
  NONE("Clean"),
  STARS("Stars"),
  ORBIT("Orbit Rings"),
  HORIZON("Horizon Line"),
  DOODLE("Sketch"),
  CORNER_HUD("HUD Brackets"),
}

@JsonClass(generateAdapter = true)
data class CustomClockSpec(
  val layoutType: BuilderLayoutType = BuilderLayoutType.CENTERED_HERO,
  val motif: BuilderMotif = BuilderMotif.ORBIT,
  val showRing: Boolean = true,
  val splitDigitColors: Boolean = true,
  val titleTag: String = "CUSTOM",
)

enum class WidgetBackdropId(
  val displayName: String,
  @DrawableRes val drawableRes: Int?,
) {
  NONE("None", null),
  SUNSET_SCENIC("Sunset", R.drawable.img_bg_sunset_scenic_1795567007696),
  PIXEL_SPACE("Pixel Space", R.drawable.img_bg_pixel_space_1795567008130),
  DREAMY_NIGHT("Dreamy Night", R.drawable.img_bg_dreamy_clouds_1795567008268),
  PLANET_GLOW("Planet Glow", R.drawable.img_bg_planet_glow_1795567009836),
  KAWAII_KITTY("Kawaii Cat", R.drawable.img_bg_kawaii_cat_1795567009210),
  AURORA_ART("Aurora", R.drawable.img_wallpaper_aurora_1795495861674),
  BAUHAUS_ART("Warm Sand", R.drawable.img_wallpaper_terracotta_1795495862707),
}

enum class ClockCategory(val label: String) {
  ALL("All"),
  MINIMAL_FLOAT("No Card"),
  ART_BACKDROP("Art & Glow"),
  NATURE_SKY("Nature & Sky"),
  DASHBOARD("Weather & Cal"),
  DIGITAL("Digital"),
  ANALOG("Analog"),
  WORD("Word & Duo"),
  EXPERIMENTAL("Experimental"),
}

enum class ClockStyleId(
  val displayName: String,
  val subtitle: String,
  val category: ClockCategory,
  val defaultFont: ClockFontId,
) {
  // Ultra-Pro Floating / No-Card Styles (Default Transparent)
  WEIGHT_STACK_FLOAT(
    displayName = "Stacked Contrast",
    subtitle = "Floating bold-light editorial digits",
    category = ClockCategory.MINIMAL_FLOAT,
    defaultFont = ClockFontId.JOST,
  ),
  NOTHING_DOT_FLOAT(
    displayName = "Dot Matrix Float",
    subtitle = "LED matrix numerals + accent dot",
    category = ClockCategory.MINIMAL_FLOAT,
    defaultFont = ClockFontId.SHARE_TECH_MONO,
  ),
  HAUTE_DIDONE_FLOAT(
    displayName = "Vogue Hairline",
    subtitle = "High-fashion Didone serif floating",
    category = ClockCategory.MINIMAL_FLOAT,
    defaultFont = ClockFontId.BODONI_MODA,
  ),
  HYPER_PILL_FLOAT(
    displayName = "Minimal Horizon",
    subtitle = "Split floating time, date & weather",
    category = ClockCategory.MINIMAL_FLOAT,
    defaultFont = ClockFontId.MICHROMA,
  ),
  FLOATING_ANALOG_PURE(
    displayName = "Frameless Dial",
    subtitle = "Pure floating watch hands on wallpaper",
    category = ClockCategory.MINIMAL_FLOAT,
    defaultFont = ClockFontId.PRATA,
  ),
  VERTICAL_SPINE_FLOAT(
    displayName = "Editorial Spine",
    subtitle = "Asymmetric spine + week & weather",
    category = ClockCategory.MINIMAL_FLOAT,
    defaultFont = ClockFontId.CORMORANT_GARAMOND,
  ),

  // Advanced Depth, Expandable, Calendar & Weather Styles
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
  COUNTDOWN_PROGRESS(
    displayName = "Day Progress",
    subtitle = "Day completion bar, remaining hours & rings",
    category = ClockCategory.DASHBOARD,
    defaultFont = ClockFontId.JETBRAINS_MONO,
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

  // Signature Art, Neon, Handwritten, Pixel & Progress Ring Styles
  NEON_FRAME_GLOW(
    displayName = "Neon Frame",
    subtitle = "Glowing cyan-magenta border & gradient digits",
    category = ClockCategory.ART_BACKDROP,
    defaultFont = ClockFontId.OUTFIT,
  ),
  CIRCULAR_BATTERY_RING(
    displayName = "Power Ring",
    subtitle = "Glowing progress arc + live battery",
    category = ClockCategory.ART_BACKDROP,
    defaultFont = ClockFontId.PLUS_JAKARTA_SANS,
  ),
  LIQUID_GLASS_PRISM(
    displayName = "Liquid Glass",
    subtitle = "Refractive water-drop highlights & specular rim",
    category = ClockCategory.ART_BACKDROP,
    defaultFont = ClockFontId.OUTFIT,
  ),
  HANDWRITTEN_SCRIPT(
    displayName = "Handwritten",
    subtitle = "Expressive brush lettering + flourish",
    category = ClockCategory.MINIMAL_FLOAT,
    defaultFont = ClockFontId.PERMANENT_MARKER,
  ),
  PIXEL_SPACE_RETRO(
    displayName = "Pixel Space",
    subtitle = "Retro 16-bit astronaut backdrop + pixel digits",
    category = ClockCategory.ART_BACKDROP,
    defaultFont = ClockFontId.SILKSCREEN,
  ),
  SUNSET_SCENIC_ART(
    displayName = "Sunset Scenic",
    subtitle = "Alpine sunset art + serif typography",
    category = ClockCategory.ART_BACKDROP,
    defaultFont = ClockFontId.BODONI_MODA,
  ),
  DREAMY_NIGHT_ART(
    displayName = "Dreamy Night",
    subtitle = "Crescent moon clouds + luminous clock",
    category = ClockCategory.ART_BACKDROP,
    defaultFont = ClockFontId.JOST,
  ),
  KAWAII_KITTY_PEEK(
    displayName = "Kawaii Kitty",
    subtitle = "Cute peeking cat card + warm ink digits",
    category = ClockCategory.ART_BACKDROP,
    defaultFont = ClockFontId.OUTFIT,
  ),
  PLANET_GLOW_ART(
    displayName = "Planet Glow",
    subtitle = "Cosmic horizon glow + sleek pill readout",
    category = ClockCategory.ART_BACKDROP,
    defaultFont = ClockFontId.PLUS_JAKARTA_SANS,
  ),

  // Nature, Celestial & Sky Collection
  SOLAR_ORBIT_PLANETS(
    displayName = "Solar Orbit",
    subtitle = "Concentric planetary rings & revolving moons",
    category = ClockCategory.NATURE_SKY,
    defaultFont = ClockFontId.SPACE_GROTESK,
  ),
  STARFIELD_CONSTELLATION(
    displayName = "Constellation",
    subtitle = "Star-map geometry, lunar phase & starlight",
    category = ClockCategory.NATURE_SKY,
    defaultFont = ClockFontId.CORMORANT_GARAMOND,
  ),
  CLOUD_DREAMER(
    displayName = "Cloud Drift",
    subtitle = "Layered drifting vector clouds & soft glow",
    category = ClockCategory.NATURE_SKY,
    defaultFont = ClockFontId.JOST,
  ),
  NATURE_BOTANICAL(
    displayName = "Botanical Dawn",
    subtitle = "Alpine ridge silhouette, sun arc & editorial date",
    category = ClockCategory.NATURE_SKY,
    defaultFont = ClockFontId.PLAYFAIR_DISPLAY,
  ),
  MOOD_DAYLIGHT(
    displayName = "Solar Shift",
    subtitle = "Time-adaptive Dawn, Noon, Sunset & Night sky",
    category = ClockCategory.NATURE_SKY,
    defaultFont = ClockFontId.PRATA,
  ),

  // Experimental, Fluid & Custom Composer Styles
  AURORA_RIBBON_FLOW(
    displayName = "Fluid Gradient",
    subtitle = "Organic flowing aurora ribbons & soft depth",
    category = ClockCategory.EXPERIMENTAL,
    defaultFont = ClockFontId.PLUS_JAKARTA_SANS,
  ),
  DOODLE_SKETCH_NOTE(
    displayName = "Doodle Sketch",
    subtitle = "Hand-drawn ink frame, squiggles & marker notes",
    category = ClockCategory.EXPERIMENTAL,
    defaultFont = ClockFontId.CAVEAT,
  ),
  LIQUID_CHROME_WAVE(
    displayName = "Liquid Chrome",
    subtitle = "Sculptural fluid wave form & metallic digits",
    category = ClockCategory.EXPERIMENTAL,
    defaultFont = ClockFontId.SYNE,
  ),
  EXPERIMENTAL_KINETIC(
    displayName = "Kinetic Split",
    subtitle = "Diagonal slash composition & staggered motion",
    category = ClockCategory.EXPERIMENTAL,
    defaultFont = ClockFontId.MICHROMA,
  ),
  CUSTOM_COMPOSER(
    displayName = "Custom Builder",
    subtitle = "Modular user-designed clock composition",
    category = ClockCategory.EXPERIMENTAL,
    defaultFont = ClockFontId.SPACE_GROTESK,
  ),
}

enum class ClockFontId(
  val displayName: String,
  @FontRes val fontRes: Int,
  val sampleText: String,
) {
  PERMANENT_MARKER("Marker Brush", R.font.permanent_marker, "9:41"),
  CAVEAT("Caveat Script", R.font.caveat, "9:41"),
  SILKSCREEN("Pixel 8-Bit", R.font.silkscreen, "9:41"),
  BODONI_MODA("Bodoni Moda", R.font.bodoni_moda, "10:42"),
  PRATA("Prata Luxe", R.font.prata, "10:42"),
  CORMORANT_GARAMOND("Cormorant Serif", R.font.cormorant_garamond, "10:42"),
  JOST("Jost Bauhaus", R.font.jost, "10:42"),
  MICHROMA("Michroma Wide", R.font.michroma, "10:42"),
  SHARE_TECH_MONO("Share Tech Mono", R.font.share_tech_mono, "10:42"),
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
  val styleId: ClockStyleId = ClockStyleId.WEIGHT_STACK_FLOAT,
  val fontId: ClockFontId = ClockFontId.JOST,
  val cardStyle: CardStyle = CardStyle.TRANSPARENT,
  val backdropId: WidgetBackdropId = WidgetBackdropId.NONE,
  val backgroundColor: Int = 0xFF0F172A.toInt(),
  val accentColor: Int = 0xFF38BDF8.toInt(),
  val foregroundColor: Int = 0xFFF8FAFC.toInt(),
  val secondaryTextColor: Int = 0xFFE2E8F0.toInt(),
  val backgroundAlpha: Float = 0.0f,
  val cornerRadiusDp: Float = 26f,
  val fontScale: Float = 1.0f,
  val depthIntensity: Float = 0.85f,
  val is24Hour: Boolean = false,
  val showSeconds: Boolean = true,
  val showDate: Boolean = true,
  val showWeather: Boolean = true,
  val useFahrenheit: Boolean = false,
  val showBorder: Boolean = false,
  val customLabel: String = "LOCAL TIME",
  // Unified Multi-Display Settings (Home / Lock Screen / AOD)
  val lockCardStyle: CardStyle = CardStyle.FROSTED,
  val lockBackgroundAlpha: Float = 0.24f,
  val lockBackdropId: WidgetBackdropId = WidgetBackdropId.NONE,
  val aodCardStyle: CardStyle = CardStyle.TRANSPARENT,
  val aodBackgroundAlpha: Float = 0.0f,
  val aodLowPowerDim: Float = 0.78f,
  val animationsEnabled: Boolean = true,
  // Background Freedom, Motion, Typography, Time-Based & Custom Builder extensions
  val backgroundFillMode: BackgroundFillMode? = null,
  val backgroundSecondaryColor: Int = 0xFF1E1B4B.toInt(),
  val gradientAngleDeg: Float = 135f,
  val borderColor: Int = 0xFFFFFFFF.toInt(),
  val showShadow: Boolean = true,
  val customPhotoUri: String? = null,
  val lockBackgroundFillMode: BackgroundFillMode? = null,
  val lockCustomPhotoUri: String? = null,
  val hourColor: Int? = null,
  val minuteColor: Int? = null,
  val secondsColor: Int? = null,
  val animationStyle: ClockAnimationStyle = ClockAnimationStyle.GLOW_PULSE,
  val animationSpeed: Float = 1.0f,
  val glowIntensity: Float = 0.65f,
  val showDecorations: Boolean = true,
  val secondsStyle: SecondsStyle = SecondsStyle.DIGITS,
  val dateFormat: ClockDateFormat = ClockDateFormat.SHORT,
  val alignment: ClockAlignment = ClockAlignment.CENTER,
  val timeBasedStyleEnabled: Boolean = false,
  val timeZoneId: String? = null,
  val themePackId: String? = null,
  val customBuilderSpec: CustomClockSpec = CustomClockSpec(),
) {
  fun effectiveFillMode(mode: DisplayMode = DisplayMode.HOME_SCREEN): BackgroundFillMode {
    return when (mode) {
      DisplayMode.HOME_SCREEN -> {
        backgroundFillMode ?: when {
          cardStyle == CardStyle.TRANSPARENT || backgroundAlpha <= 0.02f -> BackgroundFillMode.NONE
          backdropId != WidgetBackdropId.NONE -> BackgroundFillMode.ARTWORK
          cardStyle == CardStyle.FROSTED -> BackgroundFillMode.GLASS
          else -> BackgroundFillMode.SOLID
        }
      }
      DisplayMode.LOCK_SCREEN -> {
        lockBackgroundFillMode ?: when {
          lockCardStyle == CardStyle.TRANSPARENT || lockBackgroundAlpha <= 0.02f -> BackgroundFillMode.NONE
          lockBackdropId != WidgetBackdropId.NONE -> BackgroundFillMode.ARTWORK
          lockCardStyle == CardStyle.FROSTED -> BackgroundFillMode.GLASS
          else -> BackgroundFillMode.SOLID
        }
      }
      DisplayMode.AOD -> {
        if (aodCardStyle == CardStyle.TRANSPARENT || aodBackgroundAlpha <= 0.02f) {
          BackgroundFillMode.NONE
        } else {
          BackgroundFillMode.SOLID
        }
      }
    }
  }
}

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

data class ClockThemePack(
  val id: String,
  val name: String,
  val subtitle: String,
  val fontId: ClockFontId,
  val fillMode: BackgroundFillMode,
  val backgroundColor: Int,
  val backgroundSecondaryColor: Int,
  val accentColor: Int,
  val foregroundColor: Int,
  val secondaryTextColor: Int,
  val borderColor: Int,
  val backgroundAlpha: Float,
  val showBorder: Boolean,
  val animationStyle: ClockAnimationStyle,
  val glowIntensity: Float,
)

data class WorldTimeZoneOption(
  val id: String?,
  val label: String,
  val shortCode: String,
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

enum class LiveWallpaperTarget(
  val label: String,
  val displayMode: DisplayMode,
) {
  LOCK_SCREEN("Lock Clock", DisplayMode.LOCK_SCREEN),
  HOME_SCREEN("Home Clock", DisplayMode.HOME_SCREEN),
  BACKDROP_ONLY("Subtle Wallpaper", DisplayMode.LOCK_SCREEN),
}

@JsonClass(generateAdapter = true)
data class AodExperienceSettings(
  val dimLevel: Float = 0.82f,
  val burnInProtection: Boolean = true,
  val standbySplitLandscape: Boolean = true,
  val showBatteryAndWeather: Boolean = true,
  val showSecondsPulse: Boolean = false,
)

enum class IconShapeStyle(val label: String) {
  SQUIRCLE("Squircle"),
  CIRCLE("Circle"),
  ROUNDED_SQUARE("Rounded"),
  TEARDROP("Teardrop"),
  HEXAGON("Hexagon"),
  SOFT_CUT("Gem Cut"),
}

enum class IconFinishStyle(val label: String, val subtitle: String) {
  ORIGINAL_FRAMED("Framed", "Full-color app icon inside a styled tile"),
  GLASS_FROST("Frosted Glass", "Translucent glass frame with specular highlight"),
  NEON_GLOW("Neon Edge", "Luminous neon stroke on dark obsidian"),
  MONOCHROME_INK("Monochrome", "Unified clean single-tone tinted icons"),
  DUOTONE_GRADIENT("Gradient Luxe", "Vivid dual-tone gradient backdrop"),
  PASTEL_CLAY("Soft Clay", "Gentle matte pastel aesthetic"),
  OLED_OUTLINE("OLED Line", "Ultra-dark tile with crisp accent border"),
}

enum class IconBadgeStyle(val label: String) {
  NONE("None"),
  DOT("Accent Dot"),
  SPARK("Corner Spark"),
  RING("Inner Ring"),
}

@JsonClass(generateAdapter = true)
data class IconPackConfig(
  val packName: String = "Obsidian Neon",
  val shapeStyle: IconShapeStyle = IconShapeStyle.SQUIRCLE,
  val finishStyle: IconFinishStyle = IconFinishStyle.NEON_GLOW,
  val backgroundColor: Int = 0xFF0F172A.toInt(),
  val secondaryColor: Int = 0xFF1E293B.toInt(),
  val iconTintColor: Int = 0xFF38BDF8.toInt(),
  val borderColor: Int = 0xFF38BDF8.toInt(),
  val iconScale: Float = 0.62f,
  val cornerRadiusRatio: Float = 0.30f,
  val borderWidthDp: Float = 1.6f,
  val showDropShadow: Boolean = true,
  val showGlowHalo: Boolean = true,
  val monochromeGlyph: Boolean = false,
  val useDynamicAppColor: Boolean = false,
  val showHomeLabels: Boolean = true,
  val badgeStyle: IconBadgeStyle = IconBadgeStyle.NONE,
  val activeExternalPackId: String? = null,
)

@JsonClass(generateAdapter = true)
data class AppIconOverride(
  val componentKey: String,
  val customLabel: String? = null,
  val customPhotoUri: String? = null,
  val customTintColor: Int? = null,
  val isPinnedToHome: Boolean = false,
  val externalDrawableName: String? = null,
  val externalImagePath: String? = null,
)

enum class IconPackSourceType(val label: String) {
  INSTALLED_APK("Installed Pack"),
  IMPORTED_ZIP("Imported ZIP"),
}

@JsonClass(generateAdapter = true)
data class ExternalIconPackInfo(
  val packId: String,
  val name: String,
  val sourceType: IconPackSourceType,
  val packageName: String? = null,
  val folderPath: String? = null,
  val iconCount: Int = 0,
)

enum class SuperIconLayoutStyle(val label: String) {
  COLOROS_SPLIT("ColorOS 2×2"),
  PILL_BAR("Pill Strip 4×1"),
  DECK_GRID("Deck Card 4×2"),
}

enum class ShortcutActionType(val label: String) {
  CAMERA_PHOTO("Photo Mode"),
  CAMERA_VIDEO("Video Mode"),
  CAMERA_SELFIE("Selfie Cam"),
  LAUNCH_APP("Open App"),
  OPEN_URL("Web Link"),
  WEB_SEARCH("Web Search"),
  SETTINGS_WIFI("Wi-Fi"),
  SETTINGS_BLUETOOTH("Bluetooth"),
  SET_ALARM("New Alarm"),
  SET_TIMER("Timer"),
}

@JsonClass(generateAdapter = true)
data class QuickShortcutAction(
  val id: String,
  val label: String,
  val actionType: ShortcutActionType,
  val targetPackage: String? = null,
  val targetActivity: String? = null,
  val targetUriOrQuery: String? = null,
)

@JsonClass(generateAdapter = true)
data class SuperIconWidgetConfig(
  val widgetId: Int = 0,
  val primaryPackage: String = "",
  val primaryActivity: String = "",
  val primaryLabel: String = "Camera",
  val layoutStyle: SuperIconLayoutStyle = SuperIconLayoutStyle.COLOROS_SPLIT,
  val backgroundColor: Int = 0xFF0F172A.toInt(),
  val accentColor: Int = 0xFF38BDF8.toInt(),
  val textColor: Int = 0xFFF8FAFC.toInt(),
  val cardAlpha: Float = 0.88f,
  val cornerRadiusDp: Float = 26f,
  val shortcuts: List<QuickShortcutAction> = emptyList(),
)

object SuperIconPresets {
  fun suggestedShortcutsForApp(
    app: LaunchableAppInfo?,
    allApps: List<LaunchableAppInfo>,
  ): List<QuickShortcutAction> {
    val pkg = app?.packageName?.lowercase().orEmpty()
    val lbl = app?.label?.lowercase().orEmpty()
    return when {
      "camera" in pkg || "camera" in lbl -> listOf(
        QuickShortcutAction("cam_photo", "Photo", ShortcutActionType.CAMERA_PHOTO),
        QuickShortcutAction("cam_video", "Video", ShortcutActionType.CAMERA_VIDEO),
        QuickShortcutAction("cam_selfie", "Selfie", ShortcutActionType.CAMERA_SELFIE),
      )
      "chrome" in pkg || "browser" in lbl || "search" in lbl -> listOf(
        QuickShortcutAction("br_search", "Search", ShortcutActionType.WEB_SEARCH),
        QuickShortcutAction("br_yt", "YouTube", ShortcutActionType.OPEN_URL, targetUriOrQuery = "https://m.youtube.com"),
        QuickShortcutAction("br_news", "News", ShortcutActionType.OPEN_URL, targetUriOrQuery = "https://news.google.com"),
      )
      "clock" in pkg || "deskclock" in pkg || "alarm" in lbl -> listOf(
        QuickShortcutAction("clk_alarm", "Alarm", ShortcutActionType.SET_ALARM),
        QuickShortcutAction("clk_timer", "Timer", ShortcutActionType.SET_TIMER),
        QuickShortcutAction("clk_wifi", "Wi-Fi", ShortcutActionType.SETTINGS_WIFI),
      )
      "settings" in pkg || "settings" in lbl -> listOf(
        QuickShortcutAction("set_wifi", "Wi-Fi", ShortcutActionType.SETTINGS_WIFI),
        QuickShortcutAction("set_bt", "Bluetooth", ShortcutActionType.SETTINGS_BLUETOOTH),
        QuickShortcutAction("set_alarm", "Alarm", ShortcutActionType.SET_ALARM),
      )
      else -> {
        val extras = allApps.filter { it.packageName != app?.packageName }.take(3)
        if (extras.isNotEmpty()) {
          extras.mapIndexed { idx, other ->
            QuickShortcutAction(
              id = "app_sc_$idx",
              label = other.label.take(10),
              actionType = ShortcutActionType.LAUNCH_APP,
              targetPackage = other.packageName,
              targetActivity = other.activityName,
            )
          }
        } else {
          listOf(
            QuickShortcutAction("def_photo", "Photo", ShortcutActionType.CAMERA_PHOTO),
            QuickShortcutAction("def_video", "Video", ShortcutActionType.CAMERA_VIDEO),
            QuickShortcutAction("def_search", "Search", ShortcutActionType.WEB_SEARCH),
          )
        }
      }
    }
  }
}

data class IconPackPreset(
  val id: String,
  val name: String,
  val subtitle: String,
  val config: IconPackConfig,
)

data class LaunchableAppInfo(
  val packageName: String,
  val activityName: String,
  val label: String,
  val componentKey: String,
  val dominantColor: Int,
)

object IconPackPresets {
  val presets: List<IconPackPreset> = listOf(
    IconPackPreset(
      id = "obsidian_neon",
      name = "Obsidian Neon",
      subtitle = "Deep slate + electric cyan rim",
      config = IconPackConfig(
        packName = "Obsidian Neon",
        shapeStyle = IconShapeStyle.SQUIRCLE,
        finishStyle = IconFinishStyle.NEON_GLOW,
        backgroundColor = 0xFF0B1120.toInt(),
        secondaryColor = 0xFF1E1B4B.toInt(),
        iconTintColor = 0xFF38BDF8.toInt(),
        borderColor = 0xFF22D3EE.toInt(),
        borderWidthDp = 1.8f,
        showGlowHalo = true,
        monochromeGlyph = true,
      ),
    ),
    IconPackPreset(
      id = "frosted_pearl",
      name = "Frosted Glass",
      subtitle = "Translucent air glass + crisp white",
      config = IconPackConfig(
        packName = "Frosted Glass",
        shapeStyle = IconShapeStyle.ROUNDED_SQUARE,
        finishStyle = IconFinishStyle.GLASS_FROST,
        backgroundColor = 0xFFFFFFFF.toInt(),
        secondaryColor = 0xFFE2E8F0.toInt(),
        iconTintColor = 0xFFFFFFFF.toInt(),
        borderColor = 0xFFFFFFFF.toInt(),
        borderWidthDp = 1.4f,
        showGlowHalo = false,
        monochromeGlyph = false,
      ),
    ),
    IconPackPreset(
      id = "pure_mono",
      name = "OLED Mono",
      subtitle = "Pitch black + silver minimalist glyph",
      config = IconPackConfig(
        packName = "OLED Mono",
        shapeStyle = IconShapeStyle.CIRCLE,
        finishStyle = IconFinishStyle.OLED_OUTLINE,
        backgroundColor = 0xFF050507.toInt(),
        secondaryColor = 0xFF141418.toInt(),
        iconTintColor = 0xFFF8FAFC.toInt(),
        borderColor = 0xFF475569.toInt(),
        borderWidthDp = 1.2f,
        showGlowHalo = false,
        monochromeGlyph = true,
      ),
    ),
    IconPackPreset(
      id = "aurora_bloom",
      name = "Aurora Luxe",
      subtitle = "Violet-indigo gradient + soft glow",
      config = IconPackConfig(
        packName = "Aurora Luxe",
        shapeStyle = IconShapeStyle.SQUIRCLE,
        finishStyle = IconFinishStyle.DUOTONE_GRADIENT,
        backgroundColor = 0xFF312E81.toInt(),
        secondaryColor = 0xFFEC4899.toInt(),
        iconTintColor = 0xFFFDF2F8.toInt(),
        borderColor = 0xFFF472B6.toInt(),
        borderWidthDp = 1.2f,
        showGlowHalo = true,
        monochromeGlyph = true,
      ),
    ),
    IconPackPreset(
      id = "bauhaus_sand",
      name = "Warm Terracotta",
      subtitle = "Editorial clay beige + espresso ink",
      config = IconPackConfig(
        packName = "Warm Terracotta",
        shapeStyle = IconShapeStyle.TEARDROP,
        finishStyle = IconFinishStyle.PASTEL_CLAY,
        backgroundColor = 0xFFF5E6D3.toInt(),
        secondaryColor = 0xFFE7CBB0.toInt(),
        iconTintColor = 0xFF7C2D12.toInt(),
        borderColor = 0xFFC2410C.toInt(),
        borderWidthDp = 1.0f,
        showGlowHalo = false,
        monochromeGlyph = true,
      ),
    ),
    IconPackPreset(
      id = "cyber_mint",
      name = "Cyber Matrix",
      subtitle = "Hexagonal tech grid + emerald glow",
      config = IconPackConfig(
        packName = "Cyber Matrix",
        shapeStyle = IconShapeStyle.HEXAGON,
        finishStyle = IconFinishStyle.NEON_GLOW,
        backgroundColor = 0xFF022C22.toInt(),
        secondaryColor = 0xFF064E3B.toInt(),
        iconTintColor = 0xFF34D399.toInt(),
        borderColor = 0xFF10B981.toInt(),
        borderWidthDp = 1.8f,
        showGlowHalo = true,
        monochromeGlyph = true,
        badgeStyle = IconBadgeStyle.DOT,
      ),
    ),
    IconPackPreset(
      id = "adaptive_vivid",
      name = "Dynamic Tint",
      subtitle = "Pulls accent glow from each app icon",
      config = IconPackConfig(
        packName = "Dynamic Tint",
        shapeStyle = IconShapeStyle.SQUIRCLE,
        finishStyle = IconFinishStyle.ORIGINAL_FRAMED,
        backgroundColor = 0xFF0F172A.toInt(),
        secondaryColor = 0xFF1E293B.toInt(),
        iconTintColor = 0xFFF8FAFC.toInt(),
        borderColor = 0xFF64748B.toInt(),
        borderWidthDp = 1.2f,
        showGlowHalo = true,
        monochromeGlyph = false,
        useDynamicAppColor = true,
      ),
    ),
    IconPackPreset(
      id = "gem_ivory",
      name = "Ivory Gem",
      subtitle = "Clean paper card + cobalt trim",
      config = IconPackConfig(
        packName = "Ivory Gem",
        shapeStyle = IconShapeStyle.SOFT_CUT,
        finishStyle = IconFinishStyle.MONOCHROME_INK,
        backgroundColor = 0xFFF8FAFC.toInt(),
        secondaryColor = 0xFFE2E8F0.toInt(),
        iconTintColor = 0xFF1D4ED8.toInt(),
        borderColor = 0xFF93C5FD.toInt(),
        borderWidthDp = 1.4f,
        showGlowHalo = false,
        monochromeGlyph = true,
      ),
    ),
  )
}

