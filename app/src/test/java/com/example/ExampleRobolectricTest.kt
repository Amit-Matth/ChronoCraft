package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.ClockPresets
import com.example.model.ClockStyleId
import com.example.widget.ClockCanvasRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ChronoCraft", appName)
  }

  @Test
  fun `all 14 clock styles render across any widget size within memory cap`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sizes = listOf(
      140 to 70,   // 2x1 pill
      170 to 170,  // 2x2 square
      320 to 80,   // 4x1 wide banner
      320 to 160,  // 4x2 classic
      360 to 360,  // 4x4 hero
    )
    for (style in ClockStyleId.entries) {
      val config = ClockPresets.defaultConfigForStyle(style)
      for ((wDp, hDp) in sizes) {
        val bitmap = ClockCanvasRenderer.renderToBitmap(
          context = context,
          config = config,
          widthDp = wDp,
          heightDp = hDp,
          density = 2.0f,
          timeMillis = 1763959762000L,
        )
        assertTrue("Bitmap width must be > 0 for $style", bitmap.width > 0)
        assertTrue("Bitmap height must be > 0 for $style", bitmap.height > 0)
        assertTrue(
          "Bitmap memory cap exceeded for $style at ${wDp}x$hDp",
          bitmap.allocationByteCount <= 2_300_000,
        )
        bitmap.recycle()
      }
    }
  }
}
