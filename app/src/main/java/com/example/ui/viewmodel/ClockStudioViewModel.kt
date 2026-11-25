package com.example.ui.viewmodel

import android.app.Application
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.WidgetConfigRepository
import com.example.model.ActiveHomeWidget
import com.example.model.AodExperienceSettings
import com.example.model.AppIconOverride
import com.example.model.AppThemeMode
import com.example.model.BackgroundFillMode
import com.example.model.CardStyle
import com.example.model.ClockCategory
import com.example.model.ClockPresets
import com.example.model.ClockStyleId
import com.example.model.ClockThemePack
import com.example.model.ColorPalettePreset
import com.example.model.CustomClockSpec
import com.example.model.DisplayMode
import com.example.model.ExternalIconPackInfo
import com.example.model.IconPackConfig
import com.example.model.IconPackPreset
import com.example.model.LaunchableAppInfo
import com.example.model.LibraryLayoutMode
import com.example.model.LiveWallpaperTarget
import com.example.model.SavedClockPreset
import com.example.model.SuperIconPresets
import com.example.model.SuperIconWidgetConfig
import com.example.model.WallpaperOption
import com.example.model.WeatherSnapshot
import com.example.model.WidgetBackdropId
import com.example.model.WidgetConfig
import com.example.model.WidgetSizePreset
import com.example.weather.GeocodingPlace
import com.example.weather.WeatherRepository
import com.example.widget.ChronoAnalogWidgetProvider
import com.example.widget.ChronoClockWidgetProvider
import com.example.widget.ChronoSuperIconWidgetProvider
import com.example.widget.ChronoWideWidgetProvider
import com.example.widget.ClockTickScheduler
import com.example.widget.IconPackImporter
import com.example.widget.IconPackRenderer
import com.example.widget.PinSuperIconReceiver
import com.example.widget.PinWidgetReceiver
import com.example.widget.WallpaperColorExtractor
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class StudioTab(val label: String) {
  STYLE("Style"),
  COLORS("Colors"),
  MOTION("Motion"),
  LAYOUT("Layout"),
}

data class ClockStudioUiState(
  val appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
  val libraryLayoutMode: LibraryLayoutMode = LibraryLayoutMode.STAGGERED,
  val isPrefsLoaded: Boolean = false,
  val onboardingCompleted: Boolean = true,
  val selectedCategory: ClockCategory = ClockCategory.ALL,
  val searchQuery: String = "",
  val showFavoritesOnly: Boolean = false,
  val favoriteStyles: Set<ClockStyleId> = emptySet(),
  val recentStyles: List<ClockStyleId> = emptyList(),
  val activeConfig: WidgetConfig = ClockPresets.defaultConfigForStyle(ClockStyleId.WEIGHT_STACK_FLOAT),
  val activeStudioTab: StudioTab = StudioTab.STYLE,
  val activeDisplayMode: DisplayMode = DisplayMode.HOME_SCREEN,
  val editingAppWidgetId: Int? = null,
  val previewWallpaper: WallpaperOption = WallpaperOption.AURORA,
  val customWallpaperUri: String? = null,
  val wallpaperSuggestions: List<ColorPalettePreset> = emptyList(),
  val previewSizePreset: WidgetSizePreset = WidgetSizePreset.CLASSIC_4X2,
  val previewCols: Int = 4,
  val previewRows: Int = 2,
  val timeTravelEnabled: Boolean = false,
  val timeTravelMinutes: Int = 581,
  val showBuilderSheet: Boolean = false,
  val showAodExperience: Boolean = false,
  val aodSettings: AodExperienceSettings = AodExperienceSettings(),
  val liveWallpaperTarget: LiveWallpaperTarget = LiveWallpaperTarget.LOCK_SCREEN,
  val iconPackConfig: IconPackConfig = IconPackConfig(),
  val appIconOverrides: Map<String, AppIconOverride> = emptyMap(),
  val installedApps: List<LaunchableAppInfo> = emptyList(),
  val installedApkIconPacks: List<ExternalIconPackInfo> = emptyList(),
  val importedIconPacks: List<ExternalIconPackInfo> = emptyList(),
  val superIconConfig: SuperIconWidgetConfig = SuperIconWidgetConfig(),
  val isLoadingApps: Boolean = false,
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
      repo.appThemeModeFlow.collect { mode ->
        _uiState.update { it.copy(appThemeMode = mode) }
      }
    }
    viewModelScope.launch {
      repo.libraryLayoutModeFlow.collect { layout ->
        _uiState.update { it.copy(libraryLayoutMode = layout) }
      }
    }
    viewModelScope.launch {
      repo.onboardingCompletedFlow.collect { completed ->
        _uiState.update { it.copy(onboardingCompleted = completed, isPrefsLoaded = true) }
      }
    }
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
      repo.favoriteStylesFlow.collect { favorites ->
        _uiState.update { it.copy(favoriteStyles = favorites) }
      }
    }
    viewModelScope.launch {
      repo.recentStylesFlow.collect { recents ->
        _uiState.update { it.copy(recentStyles = recents) }
      }
    }
    viewModelScope.launch {
      weatherRepo.weatherFlow.collect { snapshot ->
        _uiState.update { it.copy(weather = snapshot) }
      }
    }
    viewModelScope.launch {
      repo.aodSettingsFlow.collect { aod ->
        _uiState.update { it.copy(aodSettings = aod) }
      }
    }
    viewModelScope.launch {
      repo.liveWallpaperTargetFlow.collect { target ->
        _uiState.update { it.copy(liveWallpaperTarget = target) }
      }
    }
    viewModelScope.launch {
      repo.liveWallpaperOptionFlow.collect { option ->
        _uiState.update { it.copy(previewWallpaper = option) }
      }
    }
    viewModelScope.launch {
      repo.liveWallpaperPhotoUriFlow.collect { uri ->
        if (uri != null) {
          _uiState.update { it.copy(customWallpaperUri = uri) }
        }
      }
    }
    viewModelScope.launch {
      repo.iconPackConfigFlow.collect { cfg ->
        _uiState.update { it.copy(iconPackConfig = cfg) }
      }
    }
    viewModelScope.launch {
      repo.appIconOverridesFlow.collect { overrides ->
        _uiState.update { it.copy(appIconOverrides = overrides) }
      }
    }
    viewModelScope.launch {
      repo.importedPacksFlow.collect { imported ->
        _uiState.update { it.copy(importedIconPacks = imported) }
      }
    }
    viewModelScope.launch {
      repo.superIconDraftFlow.collect { superCfg ->
        _uiState.update { it.copy(superIconConfig = superCfg) }
      }
    }
    loadInstalledApps()
    refreshWeatherIfNeeded()
    refreshActiveWidgets()
    extractWallpaperSuggestions(WallpaperOption.AURORA, null)
  }

  fun loadInstalledApps() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingApps = true) }
      val (apps, apkPacks) = withContext(Dispatchers.IO) {
        IconPackRenderer.loadLaunchableApps(getApplication()) to
          IconPackImporter.discoverInstalledIconPacks(getApplication())
      }
      _uiState.update { state ->
        val curSuper = state.superIconConfig
        val initializedSuper = if (curSuper.primaryPackage.isBlank() && apps.isNotEmpty()) {
          val cameraApp = apps.firstOrNull { "camera" in it.packageName.lowercase() || "camera" in it.label.lowercase() }
            ?: apps.first()
          curSuper.copy(
            primaryPackage = cameraApp.packageName,
            primaryActivity = cameraApp.activityName,
            primaryLabel = cameraApp.label,
            shortcuts = SuperIconPresets.suggestedShortcutsForApp(cameraApp, apps),
          )
        } else {
          curSuper
        }
        state.copy(
          installedApps = apps,
          installedApkIconPacks = apkPacks,
          superIconConfig = initializedSuper,
          isLoadingApps = false,
        )
      }
    }
  }

  fun openAodExperience() {
    _uiState.update { it.copy(showAodExperience = true) }
  }

  fun closeAodExperience() {
    _uiState.update { it.copy(showAodExperience = false) }
  }

  fun updateAodSettings(settings: AodExperienceSettings) {
    _uiState.update { it.copy(aodSettings = settings) }
    viewModelScope.launch { repo.saveAodSettings(settings) }
  }

  fun setLiveWallpaperTarget(target: LiveWallpaperTarget) {
    _uiState.update { it.copy(liveWallpaperTarget = target) }
    viewModelScope.launch { repo.setLiveWallpaperTarget(target) }
  }

  fun updateIconPackConfig(config: IconPackConfig) {
    _uiState.update { it.copy(iconPackConfig = config) }
    viewModelScope.launch { repo.saveIconPackConfig(config) }
  }

  fun applyIconPackPreset(preset: IconPackPreset) {
    updateIconPackConfig(preset.config)
    _uiState.update { it.copy(statusMessage = "Applied ${preset.name} icon pack") }
  }

  fun syncIconPackWithActiveClock() {
    val clock = _uiState.value.activeConfig
    val synced = _uiState.value.iconPackConfig.copy(
      packName = "${clock.styleId.displayName} Icons",
      backgroundColor = clock.backgroundColor,
      secondaryColor = clock.backgroundSecondaryColor,
      iconTintColor = clock.accentColor,
      borderColor = clock.accentColor,
    )
    updateIconPackConfig(synced)
    _uiState.update { it.copy(statusMessage = "Matched icons to ${clock.styleId.displayName}") }
  }

  fun saveAppIconOverride(override: AppIconOverride) {
    viewModelScope.launch {
      repo.saveAppIconOverride(override)
      _uiState.update { it.copy(statusMessage = "Updated custom icon") }
    }
  }

  fun clearAppIconOverride(componentKey: String) {
    viewModelScope.launch {
      repo.clearAppIconOverride(componentKey)
      _uiState.update { it.copy(statusMessage = "Reset icon customization") }
    }
  }

  fun selectExternalIconPack(packId: String?) {
    val cur = _uiState.value.iconPackConfig
    updateIconPackConfig(cur.copy(activeExternalPackId = packId))
    val packName = (_uiState.value.installedApkIconPacks + _uiState.value.importedIconPacks)
      .firstOrNull { it.packId == packId }?.name
    _uiState.update {
      it.copy(
        statusMessage = if (packName != null) {
          "Using $packName icons"
        } else {
          "Switched back to ChronoCraft styled icons"
        },
      )
    }
  }

  fun importZipIconPack(uri: Uri) {
    viewModelScope.launch {
      val res = withContext(Dispatchers.IO) {
        IconPackImporter.importZipIconPack(getApplication(), uri)
      }
      res.onSuccess { pack ->
        repo.addImportedPack(pack)
        val cur = _uiState.value.iconPackConfig
        updateIconPackConfig(cur.copy(activeExternalPackId = pack.packId))
        _uiState.update { it.copy(statusMessage = "Imported \"${pack.name}\" (${pack.iconCount} icons)") }
      }.onFailure { err ->
        _uiState.update { it.copy(statusMessage = err.message ?: "Could not import icon pack ZIP") }
      }
    }
  }

  fun removeImportedPack(packId: String) {
    viewModelScope.launch {
      repo.removeImportedPack(packId)
      val cur = _uiState.value.iconPackConfig
      if (cur.activeExternalPackId == packId) {
        updateIconPackConfig(cur.copy(activeExternalPackId = null))
      }
      _uiState.update { it.copy(statusMessage = "Removed imported icon pack") }
    }
  }

  fun updateSuperIconConfig(config: SuperIconWidgetConfig) {
    _uiState.update { it.copy(superIconConfig = config) }
    viewModelScope.launch { repo.saveSuperIconDraft(config) }
  }

  fun selectSuperIconApp(app: LaunchableAppInfo) {
    val suggested = SuperIconPresets.suggestedShortcutsForApp(app, _uiState.value.installedApps)
    val updated = _uiState.value.superIconConfig.copy(
      primaryPackage = app.packageName,
      primaryActivity = app.activityName,
      primaryLabel = app.label,
      shortcuts = suggested,
    )
    updateSuperIconConfig(updated)
  }

  fun requestPinSuperIconWidget() {
    val context = getApplication<Application>().applicationContext
    val mgr = AppWidgetManager.getInstance(context)
    val cfg = _uiState.value.superIconConfig
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mgr.isRequestPinAppWidgetSupported) {
      viewModelScope.launch {
        repo.setPendingSuperIconConfig(cfg)
        val callbackIntent = Intent(context, PinSuperIconReceiver::class.java)
        val successCallback = PendingIntent.getBroadcast(
          context,
          (System.currentTimeMillis() and 0x7FFFFFFFL).toInt(),
          callbackIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val provider = ComponentName(context, ChronoSuperIconWidgetProvider::class.java)
        val ok = mgr.requestPinAppWidget(provider, null, successCallback)
        if (!ok) {
          _uiState.update { it.copy(statusMessage = "Long-press home screen → Widgets → ChronoCraft Super Icon") }
        }
      }
    } else {
      _uiState.update { it.copy(statusMessage = "Long-press home screen → Widgets → ChronoCraft Super Icon") }
    }
  }

  fun pinAppIconToHome(app: LaunchableAppInfo): Boolean {
    val ctx = getApplication<Application>().applicationContext
    val cfg = _uiState.value.iconPackConfig
    val ov = _uiState.value.appIconOverrides[app.componentKey]
    val extPack = (_uiState.value.installedApkIconPacks + _uiState.value.importedIconPacks)
      .firstOrNull { it.packId == cfg.activeExternalPackId }
    val ok = IconPackRenderer.pinThemedShortcutToHome(ctx, app, cfg, ov, extPack)
    if (ok) {
      viewModelScope.launch { repo.markAppPinned(app.componentKey) }
      val name = ov?.customLabel?.takeIf { it.isNotBlank() } ?: app.label
      _uiState.update { it.copy(statusMessage = "Adding \"$name\" icon to Home Screen") }
    } else {
      _uiState.update { it.copy(statusMessage = "Your home launcher doesn't support direct icon pinning") }
    }
    return ok
  }

  fun setAppThemeMode(mode: AppThemeMode) {
    _uiState.update { it.copy(appThemeMode = mode) }
    viewModelScope.launch { repo.setAppThemeMode(mode) }
  }

  fun cycleAppThemeMode() {
    val next = when (_uiState.value.appThemeMode) {
      AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
      AppThemeMode.LIGHT -> AppThemeMode.DARK
      AppThemeMode.DARK -> AppThemeMode.SYSTEM
    }
    setAppThemeMode(next)
  }

  fun setLibraryLayoutMode(mode: LibraryLayoutMode) {
    _uiState.update { it.copy(libraryLayoutMode = mode) }
    viewModelScope.launch { repo.setLibraryLayoutMode(mode) }
  }

  fun completeOnboarding() {
    _uiState.update { it.copy(onboardingCompleted = true) }
    viewModelScope.launch { repo.setOnboardingCompleted(true) }
  }

  fun replayOnboarding() {
    _uiState.update { it.copy(onboardingCompleted = false) }
  }

  private fun extractWallpaperSuggestions(wallpaper: WallpaperOption, customUri: String?) {
    viewModelScope.launch {
      val suggestions = withContext(Dispatchers.Default) {
        WallpaperColorExtractor.extractSuggestions(getApplication(), wallpaper, customUri)
      }
      _uiState.update { it.copy(wallpaperSuggestions = suggestions) }
    }
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
    _uiState.update { it.copy(selectedCategory = category, showFavoritesOnly = false) }
  }

  fun setSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
  }

  fun toggleShowFavoritesOnly() {
    _uiState.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
  }

  fun toggleFavorite(styleId: ClockStyleId) {
    viewModelScope.launch { repo.toggleFavoriteStyle(styleId) }
  }

  fun selectStudioTab(tab: StudioTab) {
    _uiState.update { it.copy(activeStudioTab = tab) }
  }

  fun selectDisplayMode(mode: DisplayMode) {
    _uiState.update { it.copy(activeDisplayMode = mode) }
  }

  fun selectStyleFromLibrary(styleId: ClockStyleId) {
    val newConfig = ClockPresets.defaultConfigForStyle(styleId)
    val defaultSize = when {
      styleId == ClockStyleId.WORLD_AGENDA_PRO || styleId == ClockStyleId.BENTO_CALENDAR ->
        WidgetSizePreset.EXPANDED_4X3
      styleId.category == ClockCategory.ANALOG ||
        styleId.category == ClockCategory.ART_BACKDROP ||
        styleId == ClockStyleId.FLOATING_ANALOG_PURE ||
        styleId == ClockStyleId.SOLAR_ORBIT_PLANETS ->
        WidgetSizePreset.SQUARE_2X2
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
    viewModelScope.launch {
      repo.saveActiveDraft(newConfig)
      repo.recordRecentStyle(styleId)
    }
  }

  fun pickRandomStyle(): ClockStyleId {
    val current = _uiState.value.activeConfig.styleId
    val pool = ClockStyleId.entries.filter { it != current && it != ClockStyleId.CUSTOM_COMPOSER }
    val chosen = pool.random()
    selectStyleFromLibrary(chosen)
    _uiState.update { it.copy(statusMessage = "Loaded \"${chosen.displayName}\"") }
    return chosen
  }

  fun switchStyleInStudio(styleId: ClockStyleId, keepColors: Boolean = true) {
    val cur = _uiState.value.activeConfig
    val targetDefault = ClockPresets.defaultConfigForStyle(styleId)
    val updated = if (keepColors) {
      cur.copy(
        styleId = styleId,
        fontId = styleId.defaultFont,
        backdropId = if (targetDefault.backdropId != WidgetBackdropId.NONE) targetDefault.backdropId else cur.backdropId,
        backgroundFillMode = if (targetDefault.backdropId != WidgetBackdropId.NONE) BackgroundFillMode.ARTWORK else cur.backgroundFillMode,
        cardStyle = if (targetDefault.backdropId != WidgetBackdropId.NONE) CardStyle.SOLID else cur.cardStyle,
        backgroundAlpha = if (targetDefault.backdropId != WidgetBackdropId.NONE) 1.0f else cur.backgroundAlpha,
      )
    } else {
      targetDefault
    }
    updateConfig(updated)
    viewModelScope.launch { repo.recordRecentStyle(styleId) }
  }

  fun resetToOriginalDefaults() {
    val curStyle = _uiState.value.activeConfig.styleId
    val reset = ClockPresets.defaultConfigForStyle(curStyle)
    updateConfig(reset)
    _uiState.update { it.copy(statusMessage = "Reset ${curStyle.displayName} to defaults") }
  }

  fun applyThemePack(pack: ClockThemePack) {
    val updated = ClockPresets.applyThemePack(_uiState.value.activeConfig, pack)
    updateConfig(updated)
    _uiState.update { it.copy(statusMessage = "Applied ${pack.name} theme") }
  }

  fun setCardStyle(cardStyle: CardStyle) {
    val mode = when (cardStyle) {
      CardStyle.TRANSPARENT -> BackgroundFillMode.NONE
      CardStyle.FROSTED -> BackgroundFillMode.GLASS
      CardStyle.SOLID -> BackgroundFillMode.SOLID
    }
    setBackgroundFillMode(mode)
  }

  fun setBackgroundFillMode(fillMode: BackgroundFillMode, photoUri: String? = null) {
    val state = _uiState.value
    val cur = state.activeConfig
    val updated = when (state.activeDisplayMode) {
      DisplayMode.HOME_SCREEN -> {
        val (card, alpha, border) = when (fillMode) {
          BackgroundFillMode.NONE -> Triple(CardStyle.TRANSPARENT, 0.0f, false)
          BackgroundFillMode.GLASS -> Triple(CardStyle.FROSTED, 0.44f, true)
          BackgroundFillMode.SOLID -> Triple(CardStyle.SOLID, 0.94f, true)
          BackgroundFillMode.GRADIENT -> Triple(CardStyle.SOLID, 0.94f, false)
          BackgroundFillMode.ARTWORK -> Triple(CardStyle.SOLID, 1.0f, true)
          BackgroundFillMode.PHOTO -> Triple(CardStyle.SOLID, 0.96f, true)
        }
        val nextBackdrop = if (fillMode == BackgroundFillMode.ARTWORK && cur.backdropId == WidgetBackdropId.NONE) {
          WidgetBackdropId.AURORA_ART
        } else if (fillMode != BackgroundFillMode.ARTWORK) {
          WidgetBackdropId.NONE
        } else {
          cur.backdropId
        }
        cur.copy(
          backgroundFillMode = fillMode,
          cardStyle = card,
          backdropId = nextBackdrop,
          customPhotoUri = photoUri ?: cur.customPhotoUri,
          backgroundAlpha = alpha,
          showBorder = border,
        )
      }
      DisplayMode.LOCK_SCREEN -> {
        val (card, alpha) = when (fillMode) {
          BackgroundFillMode.NONE -> CardStyle.TRANSPARENT to 0.0f
          BackgroundFillMode.GLASS -> CardStyle.FROSTED to 0.30f
          BackgroundFillMode.SOLID, BackgroundFillMode.GRADIENT -> CardStyle.SOLID to 0.86f
          BackgroundFillMode.ARTWORK, BackgroundFillMode.PHOTO -> CardStyle.SOLID to 0.92f
        }
        val nextLockBackdrop = if (fillMode == BackgroundFillMode.ARTWORK && cur.lockBackdropId == WidgetBackdropId.NONE) {
          WidgetBackdropId.AURORA_ART
        } else if (fillMode != BackgroundFillMode.ARTWORK) {
          WidgetBackdropId.NONE
        } else {
          cur.lockBackdropId
        }
        cur.copy(
          lockBackgroundFillMode = fillMode,
          lockCardStyle = card,
          lockBackdropId = nextLockBackdrop,
          lockCustomPhotoUri = photoUri ?: cur.lockCustomPhotoUri,
          lockBackgroundAlpha = alpha,
        )
      }
      DisplayMode.AOD -> {
        val (card, alpha) = when (fillMode) {
          BackgroundFillMode.NONE -> CardStyle.TRANSPARENT to 0.0f
          else -> CardStyle.FROSTED to 0.16f
        }
        cur.copy(
          aodCardStyle = card,
          aodBackgroundAlpha = alpha,
        )
      }
    }
    updateConfig(updated)
  }

  fun applyColorPalette(palette: ColorPalettePreset) {
    val targetCardStyle = when {
      palette.backgroundAlpha <= 0.02f -> CardStyle.TRANSPARENT
      palette.backgroundAlpha < 0.78f -> CardStyle.FROSTED
      else -> CardStyle.SOLID
    }
    val targetFill = when (targetCardStyle) {
      CardStyle.TRANSPARENT -> BackgroundFillMode.NONE
      CardStyle.FROSTED -> BackgroundFillMode.GLASS
      CardStyle.SOLID -> BackgroundFillMode.SOLID
    }
    updateConfig(
      _uiState.value.activeConfig.copy(
        backgroundFillMode = targetFill,
        cardStyle = targetCardStyle,
        backgroundColor = palette.backgroundColor,
        accentColor = palette.accentColor,
        foregroundColor = palette.foregroundColor,
        secondaryTextColor = palette.secondaryTextColor,
        backgroundAlpha = palette.backgroundAlpha,
        hourColor = null,
        minuteColor = null,
        secondsColor = null,
        showBorder = targetCardStyle != CardStyle.TRANSPARENT,
      ),
    )
  }

  fun setTimeTravel(enabled: Boolean, minutesOfDay: Int? = null) {
    _uiState.update {
      it.copy(
        timeTravelEnabled = enabled,
        timeTravelMinutes = minutesOfDay?.coerceIn(0, 1439) ?: it.timeTravelMinutes,
      )
    }
  }

  fun openBuilder() {
    _uiState.update { it.copy(showBuilderSheet = true) }
  }

  fun closeBuilder() {
    _uiState.update { it.copy(showBuilderSheet = false) }
  }

  fun applyCustomBuilderConfig(builtConfig: WidgetConfig, saveAsPresetName: String? = null) {
    val finalCfg = builtConfig.copy(styleId = ClockStyleId.CUSTOM_COMPOSER)
    _uiState.update { it.copy(showBuilderSheet = false, editingAppWidgetId = null) }
    updateConfig(finalCfg)
    if (!saveAsPresetName.isNullOrBlank()) {
      viewModelScope.launch {
        repo.savePreset(saveAsPresetName, finalCfg)
        _uiState.update { it.copy(statusMessage = "Saved \"$saveAsPresetName\"") }
      }
    }
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
    viewModelScope.launch {
      repo.saveActiveDraft(preset.config)
      repo.recordRecentStyle(preset.config.styleId)
    }
  }

  fun duplicateSavedPreset(preset: SavedClockPreset) {
    viewModelScope.launch {
      repo.duplicatePreset(preset)
      _uiState.update { it.copy(statusMessage = "Duplicated \"${preset.name}\"") }
    }
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
    val resolvedCustomUri = customUri ?: _uiState.value.customWallpaperUri
    _uiState.update {
      it.copy(
        previewWallpaper = option,
        customWallpaperUri = resolvedCustomUri,
      )
    }
    viewModelScope.launch {
      repo.setLiveWallpaperBackdrop(option, resolvedCustomUri)
    }
    extractWallpaperSuggestions(option, resolvedCustomUri)
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
