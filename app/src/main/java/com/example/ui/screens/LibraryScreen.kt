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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ClockCategory
import com.example.model.ClockPresets
import com.example.model.ClockStyleId
import com.example.model.WeatherSnapshot
import com.example.ui.components.LiveClockCanvas
import com.example.ui.components.rememberLiveClockMillis

@Composable
fun LibraryScreen(
  selectedCategory: ClockCategory,
  weather: WeatherSnapshot,
  onSelectCategory: (ClockCategory) -> Unit,
  onSelectStyle: (ClockStyleId) -> Unit,
  onOpenPreview: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val liveTimeMillis = rememberLiveClockMillis(1000L)
  val styles = remember(selectedCategory) {
    if (selectedCategory == ClockCategory.ALL) {
      ClockStyleId.entries
    } else {
      ClockStyleId.entries.filter { it.category == selectedCategory }
    }
  }

  Column(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
  ) {
    // Sleek Minimal Header
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "ChronoCraft",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
          )
          if (weather.isLoaded) {
            Text(
              text = "${weather.cityName} • ${weather.displayTemp(false)}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        FilledTonalButton(
          onClick = onOpenPreview,
          modifier = Modifier.testTag("library_open_preview_btn"),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Visibility,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Preview", style = MaterialTheme.typography.labelLarge)
        }
      }

      // Category Filter Chips
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        items(ClockCategory.entries) { category ->
          val selected = category == selectedCategory
          FilterChip(
            selected = selected,
            onClick = { onSelectCategory(category) },
            label = { Text(category.label) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.testTag("filter_chip_${category.name.lowercase()}"),
          )
        }
      }
    }

    // Adaptive Grid of Live Ticking Widget Cards
    LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = 164.dp),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.fillMaxSize(),
    ) {
      items(styles, key = { it.name }) { styleId ->
        val config = remember(styleId) { ClockPresets.defaultConfigForStyle(styleId) }
        Surface(
          modifier =
            Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(22.dp))
              .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                shape = RoundedCornerShape(22.dp),
              )
              .clickable { onSelectStyle(styleId) }
              .testTag("library_style_card_${styleId.name.lowercase()}"),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Box(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .aspectRatio(
                    when {
                      styleId == ClockStyleId.WORLD_AGENDA_PRO || styleId == ClockStyleId.BENTO_CALENDAR -> 1.0f
                      styleId.category == ClockCategory.ANALOG -> 1.05f
                      else -> 1.32f
                    },
                  )
                  .clip(RoundedCornerShape(16.dp))
                  .background(
                    Brush.linearGradient(
                      colors = listOf(Color(0xFF151C2C), Color(0xFF0E131F)),
                    ),
                  )
                  .padding(10.dp),
              contentAlignment = Alignment.Center,
            ) {
              LiveClockCanvas(
                config = config,
                timeMillis = liveTimeMillis,
                weather = weather,
                modifier = Modifier.fillMaxSize(),
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = styleId.displayName,
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = styleId.category.label.uppercase(),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              Box(
                modifier =
                  Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Tune,
                  contentDescription = "Customize ${styleId.displayName}",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}
