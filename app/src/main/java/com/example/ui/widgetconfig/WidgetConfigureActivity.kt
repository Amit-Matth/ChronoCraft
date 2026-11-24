package com.example.ui.widgetconfig

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.WidgetConfigRepository
import com.example.model.ClockPresets
import com.example.model.ClockStyleId
import com.example.model.WallpaperOption
import com.example.model.WeatherSnapshot
import com.example.model.WidgetSizePreset
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.ChronoCraftTheme
import com.example.ui.viewmodel.ClockStudioUiState
import com.example.ui.viewmodel.StudioTab
import com.example.weather.GeocodingPlace
import com.example.weather.WeatherRepository
import com.example.widget.ClockTickScheduler
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.launch

class WidgetConfigureActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setResult(RESULT_CANCELED)
    val appWidgetId =
      intent?.extras?.getInt(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID,
      ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
      finish()
      return
    }

    val repo = WidgetConfigRepository(applicationContext)
    val weatherRepo = WeatherRepository(applicationContext)

    setContent {
      ChronoCraftTheme {
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        var config by remember { mutableStateOf(ClockPresets.defaultConfigForStyle(ClockStyleId.BENTO_CALENDAR)) }
        var weather by remember { mutableStateOf(WeatherSnapshot()) }
        var isLoadingWeather by remember { mutableStateOf(false) }
        var cityResults by remember { mutableStateOf<List<GeocodingPlace>>(emptyList()) }
        var tab by remember { mutableStateOf(StudioTab.STYLE) }
        var wallpaper by remember { mutableStateOf(WallpaperOption.AURORA) }
        var sizePreset by remember { mutableStateOf(WidgetSizePreset.CLASSIC_4X2) }

        LaunchedEffect(appWidgetId) {
          config = repo.getWidgetConfig(appWidgetId)
          weather = weatherRepo.refreshIfStale()
        }

        val uiState =
          ClockStudioUiState(
            activeConfig = config,
            activeStudioTab = tab,
            editingAppWidgetId = appWidgetId,
            previewWallpaper = wallpaper,
            previewSizePreset = sizePreset,
            previewCols = sizePreset.cols,
            previewRows = sizePreset.rows,
            weather = weather,
            isLoadingWeather = isLoadingWeather,
            citySearchResults = cityResults,
          )

        Scaffold(
          contentWindowInsets = WindowInsets.safeDrawing,
          snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
          Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            StudioScreen(
              uiState = uiState,
              onSelectTab = { tab = it },
              onSwitchStyle = { style, keepColors ->
                config = if (keepColors) {
                  config.copy(styleId = style, fontId = style.defaultFont)
                } else {
                  ClockPresets.defaultConfigForStyle(style)
                }
              },
              onUpdateConfig = { config = it },
              onApplyPalette = { palette ->
                config =
                  config.copy(
                    backgroundColor = palette.backgroundColor,
                    accentColor = palette.accentColor,
                    foregroundColor = palette.foregroundColor,
                    secondaryTextColor = palette.secondaryTextColor,
                    backgroundAlpha = palette.backgroundAlpha,
                  )
              },
              onSelectPreviewSize = { sizePreset = it },
              onCycleWallpaper = {
                val options = listOf(WallpaperOption.AURORA, WallpaperOption.TERRACOTTA, WallpaperOption.GLASS, WallpaperOption.AMOLED)
                wallpaper = options[(options.indexOf(wallpaper) + 1) % options.size]
              },
              onOpenFullPreview = {
                scope.launch {
                  snackbarHostState.showSnackbar("Tap Update Widget below to place on your home screen!")
                }
              },
              onSavePreset = { name ->
                scope.launch {
                  repo.savePreset(name, config)
                  snackbarHostState.showSnackbar("Preset \"$name\" saved")
                }
              },
              onPinWidget = {},
              onApplyToActiveWidget = { targetId ->
                scope.launch {
                  repo.saveWidgetConfig(targetId, config)
                  WidgetUpdater.updateWidget(applicationContext, targetId)
                  ClockTickScheduler.scheduleNextMinuteTick(applicationContext)
                  val resultIntent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, targetId)
                  setResult(RESULT_OK, resultIntent)
                  finish()
                }
              },
              onClearEditingWidget = { finish() },
              onSearchCities = { q ->
                scope.launch {
                  cityResults = weatherRepo.searchCities(q).getOrDefault(emptyList())
                }
              },
              onSelectCity = { place ->
                scope.launch {
                  isLoadingWeather = true
                  cityResults = emptyList()
                  weatherRepo.refreshWeatherForCity(place.name, place.latitude, place.longitude)
                    .onSuccess { weather = it }
                  isLoadingWeather = false
                }
              },
              onUseLocationWeather = {
                scope.launch {
                  isLoadingWeather = true
                  weatherRepo.refreshUsingDeviceLocation()
                    .onSuccess { weather = it }
                    .onFailure { snackbarHostState.showSnackbar(it.message ?: "Location unavailable") }
                  isLoadingWeather = false
                }
              },
              onRefreshWeather = {
                scope.launch {
                  isLoadingWeather = true
                  weatherRepo.refreshWeatherForCity(weather.cityName, weather.latitude, weather.longitude)
                    .onSuccess { weather = it }
                  isLoadingWeather = false
                }
              },
            )
          }
        }
      }
    }
  }
}
