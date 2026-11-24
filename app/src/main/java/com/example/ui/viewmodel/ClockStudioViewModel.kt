package com.example.ui.viewmodel

import android.app.Application
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.WidgetConfigRepository
import com.example.model.ActiveHomeWidget
import com.example.model.ClockCategory
import com.example.model.ClockPresets
import com.example.model.ClockStyleId
import com.example.model.ColorPalettePreset
import com.example.model.SavedClockPreset
import com.example.model.WallpaperOption
import com.example.model.WeatherSnapshot
import com.example.model.WidgetConfig
import com.example.model.WidgetSizePreset
import com.example.weather.GeocodingPlace
import com.example.weather.WeatherRepository
import com.example.widget.ChronoAnalogWidgetProvider
import com.example.widget.ChronoClockWidgetProvider
import com.example.widget.ChronoWideWidgetProvider
import com.example.widget.ClockTickScheduler
import com.example.widget.PinWidgetReceiver
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StudioTab(val label: String) {
  STYLE("Style"),
  COLORS("Colors"),
  FONTS("Fonts"),
  LAYOUT("Layout"),
}

data class ClockStudioUiState(
  val selectedCategory: ClockCategory = ClockCategory.ALL,
  val activeConfig: WidgetConfig = ClockPresets.defaultConfigForStyle(ClockStyleId.BENTO_CALENDAR),
  val activeStudioTab: StudioTab = StudioTab.STYLE,
  val editingAppWidgetId: Int? = null,
  val previewWallpaper: WallpaperOption = WallpaperOption.AURORA,
  val customWallpaperUri: String? = null,
  val previewSizePreset: WidgetSizePreset = WidgetSizePreset.CLASSIC_4X2,
  val previewCols: Int = 4,
  val previewRows: Int = 2,
  val savedPresets: List<SavedClockPreset> = emptyList(),
  val activeHomeWidgets: List<ActiveHomeWidget> = emptyList(),
  val weather: WeatherSnapshot = WeatherSnapshot(),
  val isLoadingWeather: Boolean = false,
  val citySearchResults: List<GeocodingPlace> = emptyList(),
  val isSearchingCities: Boolean = false,
  val statusMessage: String? = null,
)

class ClockStudioViewModel(application: Application) : AndroidViewModel(application) {

  private val repo = WidgetConfigRepository(application.applicationContext)
  private val weatherRepo = WeatherRepository(application.applicationContext)

  private val _uiState = MutableStateFlow(ClockStudioUiState())
  val uiState: StateFlow<ClockStudioUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      repo.activeDraftFlow.collect { draft ->
        if (_uiState.value.editingAppWidgetId == null) {
          _uiState.update { it.copy(activeConfig = draft) }
        }
      }
    }
    viewModelScope.launch {
      repo.savedPresetsFlow.collect { presets ->
        _uiState.update { it.copy(savedPresets = presets) }
      }
    }
    viewModelScope.launch {
      weatherRepo.weatherFlow.collect { snapshot ->
        _uiState.update { it.copy(weather = snapshot) }
      }
    }
    refreshWeatherIfNeeded()
    refreshActiveWidgets()
  }

  fun refreshWeatherIfNeeded() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingWeather = true) }
      weatherRepo.refreshIfStale()
      _uiState.update { it.copy(isLoadingWeather = false) }
    }
  }

  fun forceRefreshWeather() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingWeather = true) }
      val cur = _uiState.value.weather
      val result = weatherRepo.refreshWeatherForCity(cur.cityName, cur.latitude, cur.longitude)
      _uiState.update { it.copy(isLoadingWeather = false) }
      if (result.isSuccess) {
        WidgetUpdater.updateAllWidgets(getApplication())
        _uiState.update { it.copy(statusMessage = "Weather updated for ${cur.cityName}") }
      } else {
        _uiState.update { it.copy(statusMessage = "Unable to reach weather service") }
      }
    }
  }

  fun searchCities(query: String) {
    if (query.trim().length < 2) {
      _uiState.update { it.copy(citySearchResults = emptyList(), isSearchingCities = false) }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isSearchingCities = true) }
      val res = weatherRepo.searchCities(query)
      _uiState.update {
        it.copy(
          citySearchResults = res.getOrDefault(emptyList()),
          isSearchingCities = false,
        )
      }
    }
  }

  fun selectCity(place: GeocodingPlace) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingWeather = true, citySearchResults = emptyList()) }
      val res = weatherRepo.refreshWeatherForCity(place.name, place.latitude, place.longitude)
      _uiState.update { it.copy(isLoadingWeather = false) }
      if (res.isSuccess) {
        WidgetUpdater.updateAllWidgets(getApplication())
        _uiState.update { it.copy(statusMessage = "Weather set to ${place.displayLabel()}") }
      } else {
        _uiState.update { it.copy(statusMessage = "Could not load weather for ${place.name}") }
      }
    }
  }

  fun useDeviceLocationWeather() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingWeather = true) }
      val res = weatherRepo.refreshUsingDeviceLocation()
      _uiState.update { it.copy(isLoadingWeather = false) }
      res.onSuccess { snap ->
        WidgetUpdater.updateAllWidgets(getApplication())
        _uiState.update { it.copy(statusMessage = "Weather synced to ${snap.cityName}") }
      }.onFailure { err ->
        _uiState.update { it.copy(statusMessage = err.message ?: "Location unavailable") }
      }
    }
  }

  fun refreshActiveWidgets() {
    viewModelScope.launch {
      val list = WidgetUpdater.loadActiveWidgets(getApplication(), repo)
      _uiState.update { it.copy(activeHomeWidgets = list) }
    }
  }

  fun selectCategory(category: ClockCategory) {
    _uiState.update { it.copy(selectedCategory = category) }
  }

  fun selectStudioTab(tab: StudioTab) {
    _uiState.update { it.copy(activeStudioTab = tab) }
  }

  fun selectStyleFromLibrary(styleId: ClockStyleId) {
    val newConfig = ClockPresets.defaultConfigForStyle(styleId)
    val defaultSize = when {
      styleId == ClockStyleId.WORLD_AGENDA_PRO || styleId == ClockStyleId.BENTO_CALENDAR ->
        WidgetSizePreset.EXPANDED_4X3
      styleId.category == ClockCategory.ANALOG -> WidgetSizePreset.SQUARE_2X2
      else -> WidgetSizePreset.CLASSIC_4X2
    }
    _uiState.update {
      it.copy(
        editingAppWidgetId = null,
        activeConfig = newConfig,
        previewSizePreset = defaultSize,
        previewCols = defaultSize.cols,
        previewRows = defaultSize.rows,
      )
    }
    viewModelScope.launch { repo.saveActiveDraft(newConfig) }
  }

  fun switchStyleInStudio(styleId: ClockStyleId, keepColors: Boolean = true) {
    val cur = _uiState.value.activeConfig
    val updated = if (keepColors) {
      cur.copy(styleId = styleId, fontId = styleId.defaultFont)
    } else {
      ClockPresets.defaultConfigForStyle(styleId)
    }
    updateConfig(updated)
  }

  fun applyColorPalette(palette: ColorPalettePreset) {
    updateConfig(
      _uiState.value.activeConfig.copy(
        backgroundColor = palette.backgroundColor,
        accentColor = palette.accentColor,
        foregroundColor = palette.foregroundColor,
        secondaryTextColor = palette.secondaryTextColor,
        backgroundAlpha = palette.backgroundAlpha,
      ),
    )
  }

  fun updateConfig(newConfig: WidgetConfig) {
    _uiState.update { it.copy(activeConfig = newConfig) }
    viewModelScope.launch {
      val targetWidgetId = _uiState.value.editingAppWidgetId
      if (targetWidgetId != null) {
        repo.saveWidgetConfig(targetWidgetId, newConfig)
        WidgetUpdater.updateWidget(getApplication(), targetWidgetId)
      } else {
        repo.saveActiveDraft(newConfig)
      }
    }
  }

  fun loadPresetIntoStudio(preset: SavedClockPreset) {
    _uiState.update { it.copy(editingAppWidgetId = null, activeConfig = preset.config) }
    viewModelScope.launch { repo.saveActiveDraft(preset.config) }
  }

  fun startEditingHomeWidget(widget: ActiveHomeWidget) {
    _uiState.update {
      it.copy(
        editingAppWidgetId = widget.appWidgetId,
        activeConfig = widget.config,
      )
    }
  }

  fun clearEditingHomeWidget() {
    _uiState.update { it.copy(editingAppWidgetId = null) }
  }

  fun setWallpaper(option: WallpaperOption, customUri: String? = null) {
    _uiState.update {
      it.copy(
        previewWallpaper = option,
        customWallpaperUri = customUri ?: it.customWallpaperUri,
      )
    }
  }

  fun setPreviewSizePreset(preset: WidgetSizePreset) {
    _uiState.update {
      it.copy(
        previewSizePreset = preset,
        previewCols = preset.cols,
        previewRows = preset.rows,
      )
    }
  }

  fun setPreviewGrid(cols: Int, rows: Int) {
    _uiState.update {
      it.copy(
        previewCols = cols.coerceIn(2, 4),
        previewRows = rows.coerceIn(1, 4),
      )
    }
  }

  fun saveCurrentAsPreset(name: String) {
    viewModelScope.launch {
      repo.savePreset(name, _uiState.value.activeConfig)
      _uiState.update { it.copy(statusMessage = "Saved preset \"$name\"") }
    }
  }

  fun deletePreset(presetId: String) {
    viewModelScope.launch {
      repo.deletePreset(presetId)
      _uiState.update { it.copy(statusMessage = "Preset removed") }
    }
  }

  fun applyConfigToHomeWidget(appWidgetId: Int) {
    viewModelScope.launch {
      val cfg = _uiState.value.activeConfig
      repo.saveWidgetConfig(appWidgetId, cfg)
      WidgetUpdater.updateWidget(getApplication(), appWidgetId)
      ClockTickScheduler.scheduleNextMinuteTick(getApplication())
      refreshActiveWidgets()
      _uiState.update { it.copy(statusMessage = "Home screen widget #$appWidgetId updated") }
    }
  }

  fun requestPinCurrentWidget() {
    val context = getApplication<Application>().applicationContext
    val mgr = AppWidgetManager.getInstance(context)
    val cfg = _uiState.value.activeConfig
    val cols = _uiState.value.previewCols
    val rows = _uiState.value.previewRows

    val providerClass = when {
      rows == 1 -> ChronoWideWidgetProvider::class.java
      cfg.styleId.category == ClockCategory.ANALOG && cols <= 2 && rows <= 2 -> ChronoAnalogWidgetProvider::class.java
      else -> ChronoClockWidgetProvider::class.java
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mgr.isRequestPinAppWidgetSupported) {
      viewModelScope.launch {
        repo.setPendingPinConfig(cfg)
        val callbackIntent = Intent(context, PinWidgetReceiver::class.java)
        val successCallback = PendingIntent.getBroadcast(
          context,
          System.currentTimeMillis().toInt(),
          callbackIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val provider = ComponentName(context, providerClass)
        val ok = mgr.requestPinAppWidget(provider, null, successCallback)
        if (!ok) {
          _uiState.update {
            it.copy(statusMessage = "Long-press home screen → Widgets → ChronoCraft")
          }
        }
      }
    } else {
      _uiState.update {
        it.copy(statusMessage = "Long-press home screen → Widgets → ChronoCraft")
      }
    }
  }

  fun clearStatusMessage() {
    _uiState.update { it.copy(statusMessage = null) }
  }
}
