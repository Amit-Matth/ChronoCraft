package com.example.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val quickColorSwatches = listOf(
  0xFF0A0D14.toInt(),
  0xFF0F172A.toInt(),
  0xFF18181B.toInt(),
  0xFF130B24.toInt(),
  0xFF07120E.toInt(),
  0xFFF8FAFC.toInt(),
  0xFFF4EAE0.toInt(),
  0xFFFACC15.toInt(),
  0xFF38BDF8.toInt(),
  0xFFF59E0B.toInt(),
  0xFFF43F5E.toInt(),
  0xFF10B981.toInt(),
  0xFF818CF8.toInt(),
  0xFFC2593F.toInt(),
)

@Composable
fun ColorChannelEditor(
  title: String,
  selectedColor: Int,
  onColorChange: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val hsv = FloatArray(3)
  AndroidColor.colorToHSV(selectedColor, hsv)

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
        .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = String.format("#%06X", 0xFFFFFF and selectedColor),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
          modifier =
            Modifier
              .size(26.dp)
              .clip(CircleShape)
              .background(Color(selectedColor))
              .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
      }
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      items(quickColorSwatches) { swatch ->
        val isSelected = (swatch and 0xFFFFFF) == (selectedColor and 0xFFFFFF)
        Box(
          modifier =
            Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(Color(swatch))
              .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
              )
              .clickable { onColorChange(swatch) }
              .testTag("swatch_${title.lowercase().replace(" ", "_")}_${swatch}"),
          contentAlignment = Alignment.Center,
        ) {
          if (isSelected) {
            val lum =
              0.299 * AndroidColor.red(swatch) +
                0.587 * AndroidColor.green(swatch) +
                0.114 * AndroidColor.blue(swatch)
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Selected color",
              tint = if (lum > 150) Color.Black else Color.White,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }
    }

    // Fine-tune Hue & Brightness sliders
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "Hue",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "${hsv[0].roundToInt()}°",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Slider(
        value = hsv[0],
        onValueChange = { newHue ->
          val next = FloatArray(3).apply {
            this[0] = newHue
            this[1] = if (hsv[1] < 0.15f) 0.78f else hsv[1]
            this[2] = if (hsv[2] < 0.20f) 0.92f else hsv[2]
          }
          onColorChange(AndroidColor.HSVToColor(next))
        },
        valueRange = 0f..360f,
        colors = SliderDefaults.colors(
          thumbColor = Color(selectedColor),
          activeTrackColor = Color(selectedColor),
        ),
        modifier = Modifier.height(28.dp),
      )

      Spacer(modifier = Modifier.height(2.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "Brightness",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "${(hsv[2] * 100).roundToInt()}%",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Slider(
        value = hsv[2],
        onValueChange = { newVal ->
          val next = FloatArray(3).apply {
            this[0] = hsv[0]
            this[1] = hsv[1]
            this[2] = newVal
          }
          onColorChange(AndroidColor.HSVToColor(next))
        },
        valueRange = 0.04f..1.0f,
        modifier = Modifier.height(28.dp),
      )
    }
  }
}
