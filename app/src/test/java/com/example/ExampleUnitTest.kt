package com.example

import com.example.model.ClockCategory
import com.example.model.ClockPresets
import com.example.model.ClockStyleId
import com.example.widget.ClockCanvasRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun allClockStylesHaveValidDefaultConfigs() {
    assertEquals(20, ClockStyleId.entries.size)
    assertEquals(10, ClockPresets.colorPalettes.size)
    for (style in ClockStyleId.entries) {
      val cfg = ClockPresets.defaultConfigForStyle(style)
      assertEquals(style, cfg.styleId)
      assertTrue(cfg.backgroundAlpha in 0f..1f)
      assertTrue(cfg.cornerRadiusDp in 0f..44f)
      assertTrue(style.category != ClockCategory.ALL)
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
}
