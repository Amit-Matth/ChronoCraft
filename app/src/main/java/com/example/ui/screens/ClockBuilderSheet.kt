package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.model.BackgroundFillMode
import com.example.model.BuilderLayoutType
import com.example.model.BuilderMotif
import com.example.model.CardStyle
import com.example.model.ClockAnimationStyle
import com.example.model.ClockFontId
import com.example.model.ClockPresets
import com.example.model.ClockStyleId
import com.example.model.SecondsStyle
import com.example.model.WallpaperOption
import com.example.model.WeatherSnapshot
import com.example.model.WidgetConfig
import com.example.ui.components.LiveClockCanvas
import com.example.ui.components.WallpaperBackdrop
import com.example.ui.components.rememberLiveClockMillis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockBuilderSheet(
  initialConfig: WidgetConfig,
  weather: WeatherSnapshot,
  previewWallpaper: WallpaperOption,
  customWallpaperUri: String?,
  onDismiss: () -> Unit,
  onApplyInStudio: (WidgetConfig) -> Unit,
  onSaveCustomClock: (WidgetConfig, String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val liveMillis = rememberLiveClockMillis(1000L)
  var draft by remember(initialConfig) {
    mutableStateOf(
      if (initialConfig.styleId == ClockStyleId.CUSTOM_COMPOSER) initialConfig
      else ClockPresets.defaultConfigForStyle(ClockStyleId.CUSTOM_COMPOSER).copy(
        accentColor = initialConfig.accentColor,
        foregroundColor = initialConfig.foregroundColor,
        backgroundColor = initialConfig.backgroundColor,
        backgroundSecondaryColor = initialConfig.backgroundSecondaryColor,
      ),
    )
  }
  var presetName by remember { mutableStateOf("My Custom Clock") }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    LazyColumn(
      contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 28.dp, top = 4.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Column {
            Text("Custom Clock Builder", style = MaterialTheme.typography.titleLarge)
            Text(
              text = "Combine layout, decorative motif, typography, background & motion",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Live Modular Preview
      item {
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(172.dp)
              .clip(RoundedCornerShape(22.dp))
              .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp)),
          contentAlignment = Alignment.Center,
        ) {
          WallpaperBackdrop(
            wallpaper = previewWallpaper,
            customPhotoUri = customWallpaperUri,
            modifier = Modifier.fillMaxSize(),
          )
          LiveClockCanvas(
            config = draft,
            timeMillis = liveMillis,
            weather = weather,
            modifier = Modifier.fillMaxSize().padding(14.dp),
          )
        }
      }

      // Layout Composition Picker
      item {
        Text("1. Composition Layout", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(BuilderLayoutType.entries) { layout ->
            FilterChip(
              selected = draft.customBuilderSpec.layoutType == layout,
              onClick = {
                draft = draft.copy(customBuilderSpec = draft.customBuilderSpec.copy(layoutType = layout))
              },
              label = { Text(layout.label) },
              modifier = Modifier.testTag("builder_layout_${layout.name.lowercase()}"),
            )
          }
        }
      }

      // Decorative Motif Picker
      item {
        Text("2. Visual Motif", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(BuilderMotif.entries) { motif ->
            FilterChip(
              selected = draft.customBuilderSpec.motif == motif,
              onClick = {
                draft = draft.copy(customBuilderSpec = draft.customBuilderSpec.copy(motif = motif))
              },
              label = { Text(motif.label) },
              modifier = Modifier.testTag("builder_motif_${motif.name.lowercase()}"),
            )
          }
        }
      }

      // Background Fill Picker
      item {
        Text("3. Background Style", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        val fillModes = listOf(
          BackgroundFillMode.NONE,
          BackgroundFillMode.GLASS,
          BackgroundFillMode.SOLID,
          BackgroundFillMode.GRADIENT,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(fillModes) { fill ->
            FilterChip(
              selected = draft.effectiveFillMode() == fill,
              onClick = {
                val (card, alpha, border) = when (fill) {
                  BackgroundFillMode.NONE -> Triple(CardStyle.TRANSPARENT, 0.0f, false)
                  BackgroundFillMode.GLASS -> Triple(CardStyle.FROSTED, 0.44f, true)
                  BackgroundFillMode.GRADIENT -> Triple(CardStyle.SOLID, 0.94f, false)
                  else -> Triple(CardStyle.SOLID, 0.94f, true)
                }
                draft = draft.copy(
                  backgroundFillMode = fill,
                  cardStyle = card,
                  backgroundAlpha = alpha,
                  showBorder = border,
                )
              },
              label = { Text(fill.label) },
            )
          }
        }
      }

      // Typography Quick Picker
      item {
        Text("4. Typography", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(ClockFontId.entries) { font ->
            val selected = draft.fontId == font
            val ff = remember(font) { FontFamily(Font(font.fontRes)) }
            FilterChip(
              selected = selected,
              onClick = { draft = draft.copy(fontId = font) },
              label = { Text(font.displayName, fontFamily = ff) },
            )
          }
        }
      }

      // Animation & Seconds Accent
      item {
        Text("5. Motion & Seconds Accent", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(ClockAnimationStyle.entries) { anim ->
            FilterChip(
              selected = draft.animationStyle == anim,
              onClick = {
                draft = draft.copy(
                  animationStyle = anim,
                  animationsEnabled = anim != ClockAnimationStyle.NONE,
                )
              },
              label = { Text(anim.label) },
            )
          }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(SecondsStyle.entries) { sec ->
            FilterChip(
              selected = draft.secondsStyle == sec,
              onClick = {
                draft = draft.copy(secondsStyle = sec, showSeconds = sec != SecondsStyle.NONE)
              },
              label = { Text("Sec: ${sec.label}") },
            )
          }
        }
      }

      // Builder Switches & Name
      item {
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
              .padding(horizontal = 14.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Split Hour & Minute Colors", style = MaterialTheme.typography.bodyMedium)
          Switch(
            checked = draft.customBuilderSpec.splitDigitColors,
            onCheckedChange = {
              draft = draft.copy(customBuilderSpec = draft.customBuilderSpec.copy(splitDigitColors = it))
            },
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = presetName,
          onValueChange = { if (it.length <= 24) presetName = it },
          label = { Text("Custom Clock Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("builder_preset_name_input"),
        )
      }

      // Bottom CTAs
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = { onApplyInStudio(draft) },
            modifier = Modifier.weight(1f).height(50.dp).testTag("builder_apply_studio_btn"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Edit in Studio")
          }
          Button(
            onClick = { onSaveCustomClock(draft, presetName.trim().ifBlank { "My Custom Clock" }) },
            modifier = Modifier.weight(1.2f).height(50.dp).testTag("builder_save_apply_btn"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save & Apply")
          }
        }
      }
    }
  }
}
