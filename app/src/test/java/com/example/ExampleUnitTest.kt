package com.example

import com.example.model.BackgroundFillMode
import com.example.model.ClockCategory
import com.example.model.ClockPresets
import com.example.model.ClockStyleId
import com.example.model.DisplayMode
import com.example.widget.ClockCanvasRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun allClockStylesHaveValidDefaultConfigs() {
    assertEquals(46, ClockStyleId.entries.size)
    assertEquals(17, com.example.model.ClockFontId.entries.size)
    assertEquals(8, com.example.model.WidgetBackdropId.entries.size)
    assertEquals(10, ClockPresets.colorPalettes.size)
    assertEquals(15, ClockPresets.themePacks.size)
    assertEquals(10, ClockPresets.worldTimeZones.size)
    for (style in ClockStyleId.entries) {
      val cfg = ClockPresets.defaultConfigForStyle(style)
      assertEquals(style, cfg.styleId)
      assertTrue(cfg.backgroundAlpha in 0f..1f)
      assertTrue(cfg.cornerRadiusDp in 0f..44f)
      assertTrue(style.category != ClockCategory.ALL)
      if (style.category == ClockCategory.MINIMAL_FLOAT) {
        assertEquals(com.example.model.CardStyle.TRANSPARENT, cfg.cardStyle)
        assertEquals(0.0f, cfg.backgroundAlpha, 0.001f)
        assertEquals(BackgroundFillMode.NONE, cfg.effectiveFillMode(DisplayMode.HOME_SCREEN))
      }
    }
  }

  @Test
  fun safeBitmapSizeAlwaysStaysWithinMemoryCap() {
    val testCases = listOf(
      Triple(110, 50, 3.0f),
      Triple(170, 170, 3.0f),
      Triple(320, 88, 3.0f),
      Triple(340, 180, 3.5f),
      Triple(600, 600, 4.0f),
    )
    for ((wDp, hDp, density) in testCases) {
      val (wPx, hPx) = ClockCanvasRenderer.computeSafeBitmapSize(wDp, hDp, density)
      assertTrue(wPx >= 120)
      assertTrue(hPx >= 90)
      val bytes = wPx.toLong() * hPx.toLong() * 4L
      assertTrue("Bytes $bytes exceeded 2.25 MB cap", bytes <= 2_250_000L)
    }
  }

  @Test
  fun unifiedDisplayDefaultsAndThemePacksAreValid() {
    assertEquals(3, DisplayMode.entries.size)
    val cfg = ClockPresets.defaultConfigForStyle(ClockStyleId.NEON_FRAME_GLOW)
    assertTrue(cfg.animationsEnabled)
    assertTrue(cfg.aodLowPowerDim in 0.45f..0.95f)
    assertEquals(com.example.model.CardStyle.TRANSPARENT, cfg.aodCardStyle)
    assertEquals(0.0f, cfg.aodBackgroundAlpha, 0.001f)
    assertEquals(BackgroundFillMode.NONE, cfg.effectiveFillMode(DisplayMode.AOD))
    assertEquals(com.example.model.CardStyle.FROSTED, cfg.lockCardStyle)
    assertTrue(cfg.lockBackgroundAlpha in 0f..1f)

    for (pack in ClockPresets.themePacks) {
      val themed = ClockPresets.applyThemePack(cfg, pack)
      assertEquals(pack.id, themed.themePackId)
      assertEquals(pack.fillMode, themed.effectiveFillMode(DisplayMode.HOME_SCREEN))
      assertEquals(pack.fontId, themed.fontId)
    }
  }

  @Test
  fun appThemeAndLibraryLayoutModesAreDefined() {
    assertEquals(3, com.example.model.AppThemeMode.entries.size)
    assertEquals(3, com.example.model.LibraryLayoutMode.entries.size)
  }
}
