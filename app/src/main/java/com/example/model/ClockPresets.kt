package com.example.model

object ClockPresets {

  val colorPalettes: List<ColorPalettePreset> = listOf(
    ColorPalettePreset(
      id = "midnight_cyan",
      name = "Midnight Cyan",
      backgroundColor = 0xFF0D1322.toInt(),
      accentColor = 0xFF38BDF8.toInt(),
      foregroundColor = 0xFFF8FAFC.toInt(),
      secondaryTextColor = 0xFF94A3B8.toInt(),
    ),
    ColorPalettePreset(
      id = "amber_noir",
      name = "Amber Noir",
      backgroundColor = 0xFF14110F.toInt(),
      accentColor = 0xFFF59E0B.toInt(),
      foregroundColor = 0xFFFEF3C7.toInt(),
      secondaryTextColor = 0xFFA8A29E.toInt(),
    ),
    ColorPalettePreset(
      id = "cyber_magenta",
      name = "Cyber Pulse",
      backgroundColor = 0xFF130B24.toInt(),
      accentColor = 0xFFF43F5E.toInt(),
      foregroundColor = 0xFFFFFFFF.toInt(),
      secondaryTextColor = 0xFFC084FC.toInt(),
    ),
    ColorPalettePreset(
      id = "swiss_red",
      name = "Swiss Alabaster",
      backgroundColor = 0xFFF8FAFC.toInt(),
      accentColor = 0xFFEF4444.toInt(),
      foregroundColor = 0xFF0F172A.toInt(),
      secondaryTextColor = 0xFF64748B.toInt(),
    ),
    ColorPalettePreset(
      id = "emerald_matrix",
      name = "Matrix Green",
      backgroundColor = 0xFF07120E.toInt(),
      accentColor = 0xFF10B981.toInt(),
      foregroundColor = 0xFFECFDF5.toInt(),
      secondaryTextColor = 0xFF6EE7B7.toInt(),
    ),
    ColorPalettePreset(
      id = "terracotta_warm",
      name = "Terracotta Cream",
      backgroundColor = 0xFFF4EAE0.toInt(),
      accentColor = 0xFFC2593F.toInt(),
      foregroundColor = 0xFF2B211C.toInt(),
      secondaryTextColor = 0xFF7D6B62.toInt(),
    ),
    ColorPalettePreset(
      id = "frosted_clear",
      name = "Clear Glass",
      backgroundColor = 0xFF1E293B.toInt(),
      accentColor = 0xFF7DD3FC.toInt(),
      foregroundColor = 0xFFFFFFFF.toInt(),
      secondaryTextColor = 0xFFCBD5E1.toInt(),
      backgroundAlpha = 0.45f,
    ),
    ColorPalettePreset(
      id = "lavender_mist",
      name = "Lavender Mist",
      backgroundColor = 0xFFEEE8FA.toInt(),
      accentColor = 0xFF6D28D9.toInt(),
      foregroundColor = 0xFF1E1B4B.toInt(),
      secondaryTextColor = 0xFF6B7280.toInt(),
    ),
    ColorPalettePreset(
      id = "sunset_gold",
      name = "Sunset Coral",
      backgroundColor = 0xFF211118.toInt(),
      accentColor = 0xFFFB7185.toInt(),
      foregroundColor = 0xFFFFF1F2.toInt(),
      secondaryTextColor = 0xFFFDA4AF.toInt(),
    ),
    ColorPalettePreset(
      id = "monochrome_ink",
      name = "Stealth Ink",
      backgroundColor = 0xFF000000.toInt(),
      accentColor = 0xFFE2E8F0.toInt(),
      foregroundColor = 0xFFFFFFFF.toInt(),
      secondaryTextColor = 0xFF71717A.toInt(),
      backgroundAlpha = 0.85f,
    ),
  )

  fun defaultConfigForStyle(styleId: ClockStyleId): WidgetConfig {
    return when (styleId) {
      ClockStyleId.BENTO_CALENDAR ->
        WidgetConfig(
          styleId = styleId,
          fontId = ClockFontId.SPACE_GROTESK,
          backgroundColor = 0xFF0D1322.toInt(),
          accentColor = 0xFF38BDF8.toInt(),
          foregroundColor = 0xFFF8FAFC.toInt(),
          secondaryTextColor = 0xFF94A3B8.toInt(),
          backgroundAlpha = 0.94f,
          cornerRadiusDp = 28f,
          depthIntensity = 0.9f,
          showWeather = true,
          customLabel = "BENTO PRO",
        )
      ClockStyleId.DEPTH_GLASS_WEATHER ->
        WidgetConfig(
          styleId = styleId,
          fontId = ClockFontId.OUTFIT,
          backgroundColor = 0xFF131B2E.toInt(),
          accentColor = 0xFFF59E0B.toInt(),
          foregroundColor = 0xFFFFFFFF.toInt(),
          secondaryTextColor = 0xFFCBD5E1.toInt(),
          backgroundAlpha = 0.65f,
          cornerRadiusDp = 30f,
          depthIntensity = 1.0f,
          showWeather = true,
          customLabel = "ATMOS",
        )
      ClockStyleId.EXPANDABLE_STACK ->
        WidgetConfig(
          styleId = styleId,
          fontId = ClockFontId.PLUS_JAKARTA_SANS,
          backgroundColor = 0xFF161326.toInt(),
          accentColor = 0xFFC084FC.toInt(),
          foregroundColor = 0xFFFAFAFA.toInt(),
          secondaryTextColor = 0xFFA5B4FC.toInt(),
          backgroundAlpha = 0.94f,
          cornerRadiusDp = 26f,
          depthIntensity = 0.95f,
          showWeather = true,
          customLabel = "STACKED",
        )
      ClockStyleId.WORLD_AGENDA_PRO ->
        WidgetConfig(
          styleId = styleId,
          fontId = ClockFontId.SPACE_GROTESK,
          backgroundColor = 0xFF0E141B.toInt(),
          accentColor = 0xFF10B981.toInt(),
          foregroundColor = 0xFFF8FAFC.toInt(),
          secondaryTextColor = 0xFF94A3B8.toInt(),
          backgroundAlpha = 0.96f,
          cornerRadiusDp = 24f,
          depthIntensity = 0.85f,
          showWeather = true,
          customLabel = "CALENDAR",
        )
      ClockStyleId.CHRONO_3D_DEPTH ->
        WidgetConfig(
          styleId = styleId,
          fontId = ClockFontId.PLUS_JAKARTA_SANS,
          backgroundColor = 0xFF0E121A.toInt(),
          accentColor = 0xFF38BDF8.toInt(),
          foregroundColor = 0xFFF8FAFC.toInt(),
          secondaryTextColor = 0xFF94A3B8.toInt(),
          backgroundAlpha = 0.96f,
          cornerRadiusDp = 32f,
          depthIntensity = 1.0f,
          showWeather = true,
          customLabel = "DEPTH 3D",
        )
      ClockStyleId.NEUMORPH_SCULPT ->
        WidgetConfig(
          styleId = styleId,
          fontId = ClockFontId.OUTFIT,
          backgroundColor = 0xFF1B2230.toInt(),
          accentColor = 0xFFF43F5E.toInt(),
          foregroundColor = 0xFFF8FAFC.toInt(),
          secondaryTextColor = 0xFF94A3B8.toInt(),
          backgroundAlpha = 1.0f,
          cornerRadiusDp = 30f,
          depthIntensity = 1.0f,
          showWeather = true,
          customLabel = "SCULPT",
        )
      ClockStyleId.CYBER_HUD ->
        WidgetConfig(
          styleId = styleId,
          fontId = ClockFontId.ORBITRON,
          backgroundColor = 0xFF0A101E.toInt(),
          accentColor = 0xFF38BDF8.toInt(),
          foregroundColor = 0xFFF8FAFC.toInt(),
          secondaryTextColor = 0xFF7DD3FC.toInt(),
          backgroundAlpha = 0.92f,
          cornerRadiusDp = 22f,
          customLabel = "CHRONO // HUD",
        )
      ClockStyleId.RETRO_FLIP ->
        WidgetConfig(
          styleId = styleId,
          fontId = ClockFontId.BEBAS_NEUE,
          backgroundColor = 0xFF16181D.toInt(),
          accentColor = 0xFFF59E0B.toInt(),
          foregroundColor = 0xFFF8FAFC.toInt(),
          secondaryTextColor = 0xFF9CA3AF.toInt(),
          backgroundAlpha = 0.96f,
          cornerRadiusDp = 26f,
          customLabel = "MECHANICAL",
        )
      ClockStyleId.GLASS_PILL ->
        WidgetConfig(
          styleId = styleId,
          fontId = ClockFontId.OUTFIT,
          backgroundColor = 0xFF1E293B.toInt(),
          accentColor = 0xFF38BDF8.toInt(),
          foregroundColor = 0xFFFFFFFF.toInt(),
          secondaryTextColor = 0xFFCBD5E1.toInt(),
          backgroundAlpha = 0.48f,
          cornerRadiusDp = 34f,
          customLabel = "STUDIO",
        )
      ClockStyleId.BRUTALIST_POSTER ->
        WidgetConfig(
          styleId = styleId,
          fontId = ClockFontId.SYNE,
          backgroundColor = 0xFFFACC15.toInt(),
          accentColor = 0xFF0F172A.toInt(),
          foregroundColor = 0xFF0F172A.toInt(),
          secondaryTextColor = 0xFF334155.toInt(),
          backgroundAlpha = 1.0f,
          cornerRadiusDp = 12f,
          customLabel = "NOW",
        )
      ClockStyleId.TERMINAL_MONO ->
        WidgetConfig(
          styleId = styleId,
          fontId = ClockFontId.JETBRAINS_MONO,
          backgroundColor = 0xFF07120E.toInt(),
          accentColor = 0xFF10B981.toInt(),
          foregroundColor = 0xFFECFDF5.toInt(),
          secondaryTextColor = 0xFF6EE7B7.toInt(),
          backgroundAlpha = 0.94f,
          cornerRadiusDp = 18f,
          is24Hour = true,
          customLabel = "SYS.CLOCK",
        )
      ClockStyleId.EDITORIAL_SERIF ->
        WidgetConfig(
          styleId = styleId,
          fontId = ClockFontId.PLAYFAIR_DISPLAY,
          backgroundColor = 0xFFF4EAE0.toInt(),
          accentColor = 0xFFC2593F.toInt(),
          foregroundColor = 0xFF2B211C.toInt(),
          secondaryTextColor = 0xFF7D6B62.toInt(),
          backgroundAlpha = 0.96f,
          cornerRadiusDp = 26f,
          customLabel = "EDITION 01",
        )
      ClockStyleId.ANALOG_SWISS ->
        WidgetConfig(
          styleId = styleId,
          fontId = ClockFontId.SPACE_GROTESK,
          backgroundColor = 0xFFF8FAFC.toInt(),
          accentColor = 0xFFEF4444.toInt(),
          foregroundColor = 0xFF0F172A.toInt(),
          secondaryTextColor = 0xFF64748B.toInt(),
          backgroundAlpha = 0.96f,
          cornerRadiusDp = 28f,
          customLabel = "ZÜRICH",
        )
      ClockStyleId.ANALOG_CHRONO ->
        WidgetConfig(
          styleId = styleId,
          fontId = ClockFontId.PLUS_JAKARTA_SANS,
          backgroundColor = 0xFF0D111A.toInt(),
          accentColor = 0xFFF59E0B.toInt(),
          foregroundColor = 0xFFF8FAFC.toInt(),
          secondaryTextColor = 0xFF38BDF8.toInt(),
          backgroundAlpha = 0.95f,
          cornerRadiusDp = 32f,
          customLabel = "CHRONO",
        )
      ClockStyleId.ANALOG_BAUHAUS ->
        WidgetConfig(
          styleId = styleId,
          fontId = ClockFontId.OUTFIT,
          backgroundColor = 0xFF18181B.toInt(),
          accentColor = 0xFFFB7185.toInt(),
          foregroundColor = 0xFFFAFAFA.toInt(),
          secondaryTextColor = 0xFFFBBF24.toInt(),
          backgroundAlpha = 0.94f,
          cornerRadiusDp = 28f,
          customLabel = "BAUHAUS",
        )
      ClockStyleId.ANALOG_ORBIT ->
        WidgetConfig(
          styleId = styleId,
          fontId = ClockFontId.SPACE_GROTESK,
          backgroundColor = 0xFF0B1120.toInt(),
          accentColor = 0xFF818CF8.toInt(),
          foregroundColor = 0xFFF8FAFC.toInt(),
          secondaryTextColor = 0xFF94A3B8.toInt(),
          backgroundAlpha = 0.80f,
          cornerRadiusDp = 36f,
          customLabel = "ORBIT",
        )
      ClockStyleId.ANALOG_ROMAN ->
        WidgetConfig(
          styleId = styleId,
          fontId = ClockFontId.PLAYFAIR_DISPLAY,
          backgroundColor = 0xFF1A1614.toInt(),
          accentColor = 0xFFD97706.toInt(),
          foregroundColor = 0xFFFEF3C7.toInt(),
          secondaryTextColor = 0xFFD6D3D1.toInt(),
          backgroundAlpha = 0.96f,
          cornerRadiusDp = 30f,
          customLabel = "GENÈVE",
        )
      ClockStyleId.WORD_MATRIX ->
        WidgetConfig(
          styleId = styleId,
          fontId = ClockFontId.SPACE_GROTESK,
          backgroundColor = 0xFF0F172A.toInt(),
          accentColor = 0xFF38BDF8.toInt(),
          foregroundColor = 0xFFF8FAFC.toInt(),
          secondaryTextColor = 0xFF334155.toInt(),
          backgroundAlpha = 0.95f,
          cornerRadiusDp = 26f,
          customLabel = "WORD MATRIX",
        )
      ClockStyleId.WORD_STATEMENT ->
        WidgetConfig(
          styleId = styleId,
          fontId = ClockFontId.PLAYFAIR_DISPLAY,
          backgroundColor = 0xFF1C1326.toInt(),
          accentColor = 0xFFF472B6.toInt(),
          foregroundColor = 0xFFFDF2F8.toInt(),
          secondaryTextColor = 0xFFC4B5FD.toInt(),
          backgroundAlpha = 0.92f,
          cornerRadiusDp = 28f,
          customLabel = "MOMENT",
        )
      ClockStyleId.WORD_DUO_BAR ->
        WidgetConfig(
          styleId = styleId,
          fontId = ClockFontId.PLUS_JAKARTA_SANS,
          backgroundColor = 0xFF111827.toInt(),
          accentColor = 0xFF34D399.toInt(),
          foregroundColor = 0xFFF9FAFB.toInt(),
          secondaryTextColor = 0xFF9CA3AF.toInt(),
          backgroundAlpha = 0.90f,
          cornerRadiusDp = 28f,
          customLabel = "DUO PRO",
        )
    }
  }
}
