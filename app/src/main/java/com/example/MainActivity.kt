package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.model.AppThemeMode
import com.example.model.WallpaperOption
import com.example.ui.screens.ClockBuilderSheet
import com.example.ui.screens.HomePreviewScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SavedWidgetsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.ChronoCraftTheme
import com.example.ui.viewmodel.ClockStudioUiState
import com.example.ui.viewmodel.ClockStudioViewModel

private enum class ChronoDestination(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
) {
  LIBRARY("library", "Library", Icons.Rounded.GridView, Icons.Outlined.GridView),
  STUDIO("studio", "Studio", Icons.Rounded.Tune, Icons.Outlined.Tune),
  PREVIEW("preview", "Preview", Icons.Rounded.PhoneAndroid, Icons.Outlined.PhoneAndroid),
  SAVED("saved", "My Widgets", Icons.Rounded.Bookmarks, Icons.Outlined.Bookmarks);

  companion object {
    fun indexOfRoute(route: String?): Int =
      entries.indexOfFirst { it.route == route }.takeIf { it >= 0 } ?: 0
  }
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      ChronoCraftRoot()
    }
  }
}

@Composable
fun ChronoCraftRoot(
  viewModel: ClockStudioViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val systemDark = isSystemInDarkTheme()
  val darkTheme = when (uiState.appThemeMode) {
    AppThemeMode.SYSTEM -> systemDark
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
  }

  val activity = LocalContext.current as? ComponentActivity
  LaunchedEffect(darkTheme) {
    activity?.enableEdgeToEdge(
      statusBarStyle = if (darkTheme) {
        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
      } else {
        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
      },
      navigationBarStyle = if (darkTheme) {
        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
      } else {
        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
      },
    )
  }

  ChronoCraftTheme(darkTheme = darkTheme) {
    val screenPhase = when {
      !uiState.isPrefsLoaded -> 0
      !uiState.onboardingCompleted -> 1
      else -> 2
    }

    AnimatedContent(
      targetState = screenPhase,
      transitionSpec = {
        (fadeIn(tween(340)) + scaleIn(initialScale = 0.97f, animationSpec = tween(340))) togetherWith
          fadeOut(tween(240))
      },
      label = "root_onboarding_gate",
    ) { phase ->
      when (phase) {
        0 -> Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        1 -> OnboardingScreen(
          appThemeMode = uiState.appThemeMode,
          weather = uiState.weather,
          onSelectThemeMode = viewModel::setAppThemeMode,
          onComplete = viewModel::completeOnboarding,
        )
        else -> ChronoCraftMainShell(uiState = uiState, viewModel = viewModel)
      }
    }
  }
}

@Composable
private fun ChronoCraftMainShell(
  uiState: ClockStudioUiState,
  viewModel: ClockStudioViewModel,
) {
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: ChronoDestination.LIBRARY.route
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiState.statusMessage) {
    val msg = uiState.statusMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusMessage()
    }
  }

  fun navigateTo(route: String) {
    if (route == ChronoDestination.SAVED.route) {
      viewModel.refreshActiveWidgets()
    }
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  if (uiState.showBuilderSheet) {
    ClockBuilderSheet(
      initialConfig = uiState.activeConfig,
      weather = uiState.weather,
      previewWallpaper = uiState.previewWallpaper,
      customWallpaperUri = uiState.customWallpaperUri,
      onDismiss = viewModel::closeBuilder,
      onApplyInStudio = { built ->
        viewModel.applyCustomBuilderConfig(built, saveAsPresetName = null)
        navigateTo(ChronoDestination.STUDIO.route)
      },
      onSaveCustomClock = { built, name ->
        viewModel.applyCustomBuilderConfig(built, saveAsPresetName = name)
        navigateTo(ChronoDestination.STUDIO.route)
      },
    )
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 640.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (!useRail) {
          NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            ChronoDestination.entries.forEach { dest ->
              val selected = currentRoute == dest.route
              NavigationBarItem(
                selected = selected,
                onClick = { navigateTo(dest.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                    contentDescription = dest.label,
                  )
                },
                label = { Text(dest.label, maxLines = 1) },
                modifier = Modifier.testTag("nav_${dest.route}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useRail) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxHeight(),
          ) {
            ChronoDestination.entries.forEach { dest ->
              val selected = currentRoute == dest.route
              NavigationRailItem(
                selected = selected,
                onClick = { navigateTo(dest.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                    contentDescription = dest.label,
                  )
                },
                label = { Text(dest.label, maxLines = 1) },
                modifier = Modifier.testTag("nav_rail_${dest.route}"),
              )
            }
          }
        }

        NavHost(
          navController = navController,
          startDestination = ChronoDestination.LIBRARY.route,
          modifier = Modifier.weight(1f).fillMaxHeight(),
          enterTransition = {
            val fromIndex = ChronoDestination.indexOfRoute(initialState.destination.route)
            val toIndex = ChronoDestination.indexOfRoute(targetState.destination.route)
            val forward = toIndex >= fromIndex
            slideInHorizontally(
              animationSpec = tween(290, easing = FastOutSlowInEasing),
              initialOffsetX = { fullWidth -> if (forward) fullWidth / 5 else -fullWidth / 5 },
            ) + fadeIn(tween(260)) + scaleIn(initialScale = 0.97f, animationSpec = tween(290))
          },
          exitTransition = {
            val fromIndex = ChronoDestination.indexOfRoute(initialState.destination.route)
            val toIndex = ChronoDestination.indexOfRoute(targetState.destination.route)
            val forward = toIndex >= fromIndex
            slideOutHorizontally(
              animationSpec = tween(250, easing = FastOutSlowInEasing),
              targetOffsetX = { fullWidth -> if (forward) -fullWidth / 8 else fullWidth / 8 },
            ) + fadeOut(tween(200))
          },
          popEnterTransition = {
            val fromIndex = ChronoDestination.indexOfRoute(initialState.destination.route)
            val toIndex = ChronoDestination.indexOfRoute(targetState.destination.route)
            val forward = toIndex >= fromIndex
            slideInHorizontally(
              animationSpec = tween(290, easing = FastOutSlowInEasing),
              initialOffsetX = { fullWidth -> if (forward) fullWidth / 5 else -fullWidth / 5 },
            ) + fadeIn(tween(260)) + scaleIn(initialScale = 0.97f, animationSpec = tween(290))
          },
          popExitTransition = {
            val fromIndex = ChronoDestination.indexOfRoute(initialState.destination.route)
            val toIndex = ChronoDestination.indexOfRoute(targetState.destination.route)
            val forward = toIndex >= fromIndex
            slideOutHorizontally(
              animationSpec = tween(250, easing = FastOutSlowInEasing),
              targetOffsetX = { fullWidth -> if (forward) -fullWidth / 8 else fullWidth / 8 },
            ) + fadeOut(tween(200))
          },
        ) {
          composable(ChronoDestination.LIBRARY.route) {
            LibraryScreen(
              selectedCategory = uiState.selectedCategory,
              weather = uiState.weather,
              appThemeMode = uiState.appThemeMode,
              layoutMode = uiState.libraryLayoutMode,
              activeStyleId = uiState.activeConfig.styleId,
              searchQuery = uiState.searchQuery,
              showFavoritesOnly = uiState.showFavoritesOnly,
              favoriteStyles = uiState.favoriteStyles,
              recentStyles = uiState.recentStyles,
              onCycleThemeMode = viewModel::cycleAppThemeMode,
              onSelectLayoutMode = viewModel::setLibraryLayoutMode,
              onSearchQueryChange = viewModel::setSearchQuery,
              onToggleFavoritesOnly = viewModel::toggleShowFavoritesOnly,
              onToggleFavoriteStyle = viewModel::toggleFavorite,
              onSelectCategory = viewModel::selectCategory,
              onSelectStyle = { styleId ->
                viewModel.selectStyleFromLibrary(styleId)
                navigateTo(ChronoDestination.STUDIO.route)
              },
              onPickRandomStyle = {
                viewModel.pickRandomStyle()
                navigateTo(ChronoDestination.STUDIO.route)
              },
              onOpenBuilder = viewModel::openBuilder,
              onApplyThemePack = { pack ->
                viewModel.applyThemePack(pack)
                navigateTo(ChronoDestination.STUDIO.route)
              },
              onOpenPreview = { navigateTo(ChronoDestination.PREVIEW.route) },
            )
          }

          composable(ChronoDestination.STUDIO.route) {
            StudioScreen(
              uiState = uiState,
              onSelectTab = viewModel::selectStudioTab,
              onSelectDisplayMode = viewModel::selectDisplayMode,
              onSwitchStyle = viewModel::switchStyleInStudio,
              onUpdateConfig = viewModel::updateConfig,
              onSelectCardStyle = viewModel::setCardStyle,
              onSelectFillMode = viewModel::setBackgroundFillMode,
              onApplyPalette = viewModel::applyColorPalette,
              onApplyThemePack = viewModel::applyThemePack,
              onResetDefaults = viewModel::resetToOriginalDefaults,
              onOpenBuilder = viewModel::openBuilder,
              onSetTimeTravel = viewModel::setTimeTravel,
              onSelectPreviewSize = viewModel::setPreviewSizePreset,
              onCycleWallpaper = {
                val cycle = listOf(
                  WallpaperOption.AURORA,
                  WallpaperOption.TERRACOTTA,
                  WallpaperOption.GLASS,
                  WallpaperOption.AMOLED,
                )
                val next = cycle[(cycle.indexOf(uiState.previewWallpaper) + 1) % cycle.size]
                viewModel.setWallpaper(next)
              },
              onOpenFullPreview = { navigateTo(ChronoDestination.PREVIEW.route) },
              onSavePreset = viewModel::saveCurrentAsPreset,
              onPinWidget = viewModel::requestPinCurrentWidget,
              onApplyToActiveWidget = viewModel::applyConfigToHomeWidget,
              onClearEditingWidget = viewModel::clearEditingHomeWidget,
              onSearchCities = viewModel::searchCities,
              onSelectCity = viewModel::selectCity,
              onUseLocationWeather = viewModel::useDeviceLocationWeather,
              onRefreshWeather = viewModel::forceRefreshWeather,
            )
          }

          composable(ChronoDestination.PREVIEW.route) {
            HomePreviewScreen(
              uiState = uiState,
              onSelectDisplayMode = viewModel::selectDisplayMode,
              onSelectWallpaper = viewModel::setWallpaper,
              onSelectSizePreset = viewModel::setPreviewSizePreset,
              onSetGrid = viewModel::setPreviewGrid,
              onSetTimeTravel = viewModel::setTimeTravel,
              onOpenStudio = { navigateTo(ChronoDestination.STUDIO.route) },
              onPinWidget = viewModel::requestPinCurrentWidget,
            )
          }

          composable(ChronoDestination.SAVED.route) {
            SavedWidgetsScreen(
              uiState = uiState,
              onRefreshActiveWidgets = viewModel::refreshActiveWidgets,
              onEditActiveWidget = { widget ->
                viewModel.startEditingHomeWidget(widget)
                navigateTo(ChronoDestination.STUDIO.route)
              },
              onLoadPreset = { preset ->
                viewModel.loadPresetIntoStudio(preset)
                navigateTo(ChronoDestination.STUDIO.route)
              },
              onDuplicatePreset = viewModel::duplicateSavedPreset,
              onDeletePreset = viewModel::deletePreset,
              onOpenBuilder = viewModel::openBuilder,
              onOpenLibrary = { navigateTo(ChronoDestination.LIBRARY.route) },
              onSelectThemeMode = viewModel::setAppThemeMode,
              onReplayOnboarding = viewModel::replayOnboarding,
            )
          }
        }
      }
    }
  }
}
