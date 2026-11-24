package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.model.WallpaperOption
import com.example.ui.screens.HomePreviewScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SavedWidgetsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.ChronoCraftTheme
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
  SAVED("saved", "My Widgets", Icons.Rounded.Bookmarks, Icons.Outlined.Bookmarks),
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      ChronoCraftTheme {
        ChronoCraftApp()
      }
    }
  }
}

@Composable
fun ChronoCraftApp(
  viewModel: ClockStudioViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
        ) {
          composable(ChronoDestination.LIBRARY.route) {
            LibraryScreen(
              selectedCategory = uiState.selectedCategory,
              weather = uiState.weather,
              onSelectCategory = viewModel::selectCategory,
              onSelectStyle = { styleId ->
                viewModel.selectStyleFromLibrary(styleId)
                navigateTo(ChronoDestination.STUDIO.route)
              },
              onOpenPreview = { navigateTo(ChronoDestination.PREVIEW.route) },
            )
          }

          composable(ChronoDestination.STUDIO.route) {
            StudioScreen(
              uiState = uiState,
              onSelectTab = viewModel::selectStudioTab,
              onSwitchStyle = viewModel::switchStyleInStudio,
              onUpdateConfig = viewModel::updateConfig,
              onApplyPalette = viewModel::applyColorPalette,
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
              onSelectWallpaper = viewModel::setWallpaper,
              onSelectSizePreset = viewModel::setPreviewSizePreset,
              onSetGrid = viewModel::setPreviewGrid,
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
              onDeletePreset = viewModel::deletePreset,
              onOpenLibrary = { navigateTo(ChronoDestination.LIBRARY.route) },
            )
          }
        }
      }
    }
  }
}
