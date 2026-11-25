package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewQuilt
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppThemeMode
import com.example.model.ClockCategory
import com.example.model.ClockPresets
import com.example.model.ClockStyleId
import com.example.model.ClockThemePack
import com.example.model.LibraryLayoutMode
import com.example.model.WeatherSnapshot
import com.example.ui.components.LiveClockCanvas
import com.example.ui.components.rememberLiveClockMillis

@Composable
fun LibraryScreen(
  selectedCategory: ClockCategory,
  weather: WeatherSnapshot,
  appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
  layoutMode: LibraryLayoutMode = LibraryLayoutMode.STAGGERED,
  activeStyleId: ClockStyleId = ClockStyleId.WEIGHT_STACK_FLOAT,
  searchQuery: String = "",
  showFavoritesOnly: Boolean = false,
  favoriteStyles: Set<ClockStyleId> = emptySet(),
  recentStyles: List<ClockStyleId> = emptyList(),
  onCycleThemeMode: () -> Unit = {},
  onSelectLayoutMode: (LibraryLayoutMode) -> Unit = {},
  onSearchQueryChange: (String) -> Unit = {},
  onToggleFavoritesOnly: () -> Unit = {},
  onToggleFavoriteStyle: (ClockStyleId) -> Unit = {},
  onSelectCategory: (ClockCategory) -> Unit,
  onSelectStyle: (ClockStyleId) -> Unit,
  onPickRandomStyle: () -> Unit = {},
  onOpenBuilder: () -> Unit = {},
  onApplyThemePack: (ClockThemePack) -> Unit = {},
  onOpenPreview: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val liveTimeMillis = rememberLiveClockMillis(1000L)
  val query = searchQuery.trim().lowercase()
  val styles = remember(selectedCategory, query, showFavoritesOnly, favoriteStyles) {
    ClockStyleId.entries.filter { style ->
      val catOk = selectedCategory == ClockCategory.ALL || style.category == selectedCategory
      val favOk = !showFavoritesOnly || style in favoriteStyles
      val queryOk =
        query.isEmpty() ||
          style.displayName.lowercase().contains(query) ||
          style.subtitle.lowercase().contains(query) ||
          style.category.label.lowercase().contains(query)
      catOk && favOk && queryOk
    }
  }

  Column(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
  ) {
    // Editorial Header + Theme Toggle + Quick Actions
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Image(
            painter = painterResource(id = R.drawable.chrono_app_logo_1795569986651),
            contentDescription = "ChronoCraft logo",
            contentScale = ContentScale.Crop,
            modifier =
              Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
          )
          Column {
            Text(
              text = "ChronoCraft",
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onBackground,
            )
            val subline = buildString {
              append("${ClockStyleId.entries.size} clocks")
              if (weather.isLoaded) append(" • ${weather.displayTemp(false)}")
            }
            Text(
              text = subline,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          val themeIcon = when (appThemeMode) {
            AppThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
            AppThemeMode.LIGHT -> Icons.Rounded.LightMode
            AppThemeMode.DARK -> Icons.Rounded.DarkMode
          }
          IconButton(
            onClick = onCycleThemeMode,
            modifier =
              Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("library_theme_toggle_btn"),
          ) {
            Icon(
              imageVector = themeIcon,
              contentDescription = "Theme mode: ${appThemeMode.label}",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          IconButton(
            onClick = onPickRandomStyle,
            modifier =
              Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("library_random_btn"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Shuffle,
              contentDescription = "Surprise me with a random clock style",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          FilledTonalButton(
            onClick = onOpenBuilder,
            modifier = Modifier.height(48.dp).testTag("library_open_builder_btn"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
          ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text("Build", style = MaterialTheme.typography.labelLarge)
          }

          IconButton(
            onClick = onOpenPreview,
            modifier =
              Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .testTag("library_open_preview_btn"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Visibility,
              contentDescription = "Open full preview",
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }

      // Search Input
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Search dial, minimal, neon, orbit...") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchQueryChange("") }) {
              Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("library_search_input"),
      )

      // Favorites + Category Filter Chips
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        item(key = "favorites_chip") {
          FilterChip(
            selected = showFavoritesOnly,
            onClick = onToggleFavoritesOnly,
            label = { Text("Favorites (${favoriteStyles.size})") },
            leadingIcon = {
              Icon(
                imageVector = if (showFavoritesOnly) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            modifier = Modifier.testTag("filter_chip_favorites"),
          )
        }
        items(ClockCategory.entries, key = { it.name }) { category ->
          val selected = !showFavoritesOnly && category == selectedCategory
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

      // Dense / Staggered / List Layout Switcher Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${styles.size} styles • ${appThemeMode.label} theme",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
          modifier =
            Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
              .padding(3.dp),
          horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          LibraryLayoutMode.entries.forEach { mode ->
            val selected = layoutMode == mode
            val icon = when (mode) {
              LibraryLayoutMode.STAGGERED -> Icons.Rounded.ViewQuilt
              LibraryLayoutMode.DENSE -> Icons.Rounded.GridView
              LibraryLayoutMode.LIST -> Icons.Rounded.ViewAgenda
            }
            Row(
              modifier =
                Modifier
                  .height(34.dp)
                  .clip(RoundedCornerShape(9.dp))
                  .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                  .clickable { onSelectLayoutMode(mode) }
                  .padding(horizontal = 9.dp)
                  .testTag("layout_mode_${mode.name.lowercase()}"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = icon,
                contentDescription = mode.label,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = mode.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }

    val showRecents = recentStyles.isNotEmpty() && query.isEmpty() && !showFavoritesOnly && selectedCategory == ClockCategory.ALL
    val showThemes = query.isEmpty() && !showFavoritesOnly && selectedCategory == ClockCategory.ALL

    // Animated switch between Staggered Masonry, Dense Grid, and Sleek List
    AnimatedContent(
      targetState = layoutMode,
      transitionSpec = { fadeIn(tween(220)).togetherWith(fadeOut(tween(160))) },
      modifier = Modifier.weight(1f).fillMaxWidth(),
      label = "library_layout_mode",
    ) { activeLayout ->
      if (activeLayout == LibraryLayoutMode.LIST) {
        LazyColumn(
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 4.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize(),
        ) {
          if (showRecents) {
            item(key = "list_recents") {
              RecentStylesStrip(recentStyles = recentStyles, onSelectStyle = onSelectStyle)
            }
          }
          if (showThemes) {
            item(key = "list_themes") {
              ThemePacksStrip(onApplyThemePack = onApplyThemePack)
            }
          }
          if (styles.isEmpty()) {
            item(key = "list_empty") {
              EmptyFilterCard(onReset = {
                onSearchQueryChange("")
                onSelectCategory(ClockCategory.ALL)
              })
            }
          } else {
            items(styles, key = { it.name }) { styleId ->
              LibraryListCard(
                styleId = styleId,
                weather = weather,
                liveTimeMillis = liveTimeMillis,
                isActive = styleId == activeStyleId,
                isFavorite = styleId in favoriteStyles,
                onClick = {
                  if (styleId == ClockStyleId.CUSTOM_COMPOSER) onOpenBuilder() else onSelectStyle(styleId)
                },
                onToggleFavorite = { onToggleFavoriteStyle(styleId) },
              )
            }
          }
        }
      } else {
        val isDense = activeLayout == LibraryLayoutMode.DENSE
        val minCell = if (isDense) 108.dp else 160.dp
        val gap = if (isDense) 8.dp else 12.dp

        LazyVerticalStaggeredGrid(
          columns = StaggeredGridCells.Adaptive(minSize = minCell),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(gap),
          verticalItemSpacing = gap,
          modifier = Modifier.fillMaxSize(),
        ) {
          if (showRecents) {
            item(span = StaggeredGridItemSpan.FullLine, key = "recent_strip") {
              RecentStylesStrip(recentStyles = recentStyles, onSelectStyle = onSelectStyle)
            }
          }
          if (showThemes) {
            item(span = StaggeredGridItemSpan.FullLine, key = "theme_packs_strip") {
              ThemePacksStrip(onApplyThemePack = onApplyThemePack)
            }
          }
          if (styles.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine, key = "empty_filter_state") {
              EmptyFilterCard(onReset = {
                onSearchQueryChange("")
                onSelectCategory(ClockCategory.ALL)
              })
            }
          } else {
            items(styles, key = { it.name }) { styleId ->
              LibraryStaggeredCard(
                styleId = styleId,
                weather = weather,
                liveTimeMillis = liveTimeMillis,
                isDense = isDense,
                isActive = styleId == activeStyleId,
                isFavorite = styleId in favoriteStyles,
                onClick = {
                  if (styleId == ClockStyleId.CUSTOM_COMPOSER) onOpenBuilder() else onSelectStyle(styleId)
                },
                onToggleFavorite = { onToggleFavoriteStyle(styleId) },
              )
            }
          }
        }
      }
    }
  }
}

private fun staggeredAspectRatioFor(styleId: ClockStyleId, isDense: Boolean): Float {
  if (isDense) {
    return when {
      styleId.category == ClockCategory.ANALOG || styleId == ClockStyleId.FLOATING_ANALOG_PURE -> 1.0f
      styleId == ClockStyleId.BENTO_CALENDAR || styleId == ClockStyleId.WORLD_AGENDA_PRO -> 0.92f
      else -> 1.08f
    }
  }
  return when (styleId) {
    ClockStyleId.WEIGHT_STACK_FLOAT,
    ClockStyleId.VERTICAL_SPINE_FLOAT,
    ClockStyleId.HAUTE_DIDONE_FLOAT,
    ClockStyleId.HANDWRITTEN_SCRIPT,
    -> 0.84f
    ClockStyleId.WORLD_AGENDA_PRO,
    ClockStyleId.BENTO_CALENDAR,
    ClockStyleId.EXPANDABLE_STACK,
    -> 0.90f
    ClockStyleId.FLOATING_ANALOG_PURE,
    ClockStyleId.ANALOG_SWISS,
    ClockStyleId.ANALOG_CHRONO,
    ClockStyleId.ANALOG_BAUHAUS,
    ClockStyleId.ANALOG_ORBIT,
    ClockStyleId.ANALOG_ROMAN,
    ClockStyleId.CHRONO_3D_DEPTH,
    ClockStyleId.SOLAR_ORBIT_PLANETS,
    ClockStyleId.CIRCULAR_BATTERY_RING,
    -> 0.98f
    ClockStyleId.SUNSET_SCENIC_ART,
    ClockStyleId.DREAMY_NIGHT_ART,
    ClockStyleId.KAWAII_KITTY_PEEK,
    ClockStyleId.PIXEL_SPACE_RETRO,
    ClockStyleId.PLANET_GLOW_ART,
    ClockStyleId.NEON_FRAME_GLOW,
    ClockStyleId.STARFIELD_CONSTELLATION,
    -> 1.06f
    ClockStyleId.HYPER_PILL_FLOAT,
    ClockStyleId.CYBER_HUD,
    ClockStyleId.TERMINAL_MONO,
    ClockStyleId.RETRO_FLIP,
    ClockStyleId.GLASS_PILL,
    -> 1.38f
    else -> 1.18f
  }
}

@Composable
private fun LibraryStaggeredCard(
  styleId: ClockStyleId,
  weather: WeatherSnapshot,
  liveTimeMillis: Long,
  isDense: Boolean,
  isActive: Boolean,
  isFavorite: Boolean,
  onClick: () -> Unit,
  onToggleFavorite: () -> Unit,
) {
  val config = remember(styleId) { ClockPresets.defaultConfigForStyle(styleId) }
  val aspect = remember(styleId, isDense) { staggeredAspectRatioFor(styleId, isDense) }
  val isDial = styleId.category == ClockCategory.ANALOG ||
    styleId == ClockStyleId.FLOATING_ANALOG_PURE ||
    styleId == ClockStyleId.SOLAR_ORBIT_PLANETS ||
    styleId == ClockStyleId.CIRCULAR_BATTERY_RING

  Surface(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(if (isDense) 16.dp else 22.dp))
        .border(
          width = if (isActive) 2.dp else 1.dp,
          color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
          shape = RoundedCornerShape(if (isDense) 16.dp else 22.dp),
        )
        .clickable(onClick = onClick)
        .testTag("library_style_card_${styleId.name.lowercase()}"),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
  ) {
    Column(modifier = Modifier.padding(if (isDense) 7.dp else 10.dp)) {
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .clip(RoundedCornerShape(if (isDense) 12.dp else 16.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_wallpaper_aurora_1795495861674),
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
        if (isDial && !isDense) {
          Box(
            modifier =
              Modifier
                .fillMaxSize(0.88f)
                .clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
          )
        }
        LiveClockCanvas(
          config = config,
          timeMillis = liveTimeMillis,
          weather = weather,
          modifier = Modifier.fillMaxSize().padding(if (isDense) 6.dp else 10.dp),
        )

        if (isActive) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
          ) {
            Text(
              text = "ACTIVE",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(if (isDense) 5.dp else 8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = styleId.displayName,
            style = if (isDense) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (!isDense) {
            Text(
              text = styleId.category.label.uppercase(),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        if (!isDense) {
          IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(48.dp).testTag("favorite_btn_${styleId.name.lowercase()}"),
          ) {
            Icon(
              imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
              contentDescription = if (isFavorite) "Remove ${styleId.displayName} from favorites" else "Add ${styleId.displayName} to favorites",
              tint = if (isFavorite) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LibraryListCard(
  styleId: ClockStyleId,
  weather: WeatherSnapshot,
  liveTimeMillis: Long,
  isActive: Boolean,
  isFavorite: Boolean,
  onClick: () -> Unit,
  onToggleFavorite: () -> Unit,
) {
  val config = remember(styleId) { ClockPresets.defaultConfigForStyle(styleId) }
  Surface(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .border(
          width = if (isActive) 2.dp else 1.dp,
          color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
          shape = RoundedCornerShape(20.dp),
        )
        .clickable(onClick = onClick)
        .testTag("library_style_card_${styleId.name.lowercase()}"),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
  ) {
    Row(
      modifier = Modifier.padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier = Modifier.width(124.dp).height(84.dp).clip(RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_wallpaper_aurora_1795495861674),
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.24f)))
        LiveClockCanvas(
          config = config,
          timeMillis = liveTimeMillis,
          weather = weather,
          modifier = Modifier.fillMaxSize().padding(6.dp),
        )
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = styleId.displayName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (isActive) {
            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary) {
              Text(
                text = "ACTIVE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
              )
            }
          }
        }
        Text(
          text = styleId.subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = styleId.category.label.uppercase(),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
        )
      }
      IconButton(
        onClick = onToggleFavorite,
        modifier = Modifier.size(48.dp).testTag("favorite_btn_${styleId.name.lowercase()}"),
      ) {
        Icon(
          imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
          contentDescription = if (isFavorite) "Remove ${styleId.displayName} from favorites" else "Add ${styleId.displayName} to favorites",
          tint = if (isFavorite) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun RecentStylesStrip(
  recentStyles: List<ClockStyleId>,
  onSelectStyle: (ClockStyleId) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = "RECENTLY VIEWED",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.primary,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(recentStyles, key = { "recent_${it.name}" }) { styleId ->
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
          modifier =
            Modifier
              .clip(RoundedCornerShape(14.dp))
              .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
              .clickable { onSelectStyle(styleId) }
              .testTag("recent_style_${styleId.name.lowercase()}"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(styleId.displayName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(styleId.category.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }
  }
}

@Composable
private fun ThemePacksStrip(
  onApplyThemePack: (ClockThemePack) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = "CURATED THEME PACKS",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.secondary,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(ClockPresets.themePacks, key = { it.id }) { pack ->
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
          modifier =
            Modifier
              .clip(RoundedCornerShape(14.dp))
              .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
              .clickable { onApplyThemePack(pack) }
              .testTag("library_theme_pack_${pack.id}"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier =
                Modifier
                  .size(16.dp)
                  .clip(CircleShape)
                  .background(Color(pack.accentColor))
                  .border(1.dp, Color(pack.foregroundColor).copy(alpha = 0.5f), CircleShape),
            )
            Text(pack.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyFilterCard(onReset: () -> Unit) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text("No matching clock styles", style = MaterialTheme.typography.titleMedium)
      Text(
        text = "Try a different keyword or clear the Favorites/category filter.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      OutlinedButton(onClick = onReset) {
        Text("Show All Styles")
      }
    }
  }
}
