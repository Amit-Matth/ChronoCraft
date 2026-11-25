package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AodExperienceSettings
import com.example.model.AppIconOverride
import com.example.model.AppThemeMode
import com.example.model.ClockPresets
import com.example.model.ClockStyleId
import com.example.model.ExternalIconPackInfo
import com.example.model.IconPackConfig
import com.example.model.LibraryLayoutMode
import com.example.model.LiveWallpaperTarget
import com.example.model.SavedClockPreset
import com.example.model.SuperIconWidgetConfig
import com.example.model.WallpaperOption
import com.example.model.WidgetConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.chronoDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "chrono_widget_prefs")

class WidgetConfigRepository(private val context: Context) {

  private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val configAdapter = moshi.adapter(WidgetConfig::class.java)
  private val aodSettingsAdapter = moshi.adapter(AodExperienceSettings::class.java)
  private val iconPackConfigAdapter = moshi.adapter(IconPackConfig::class.java)
  private val superIconConfigAdapter = moshi.adapter(SuperIconWidgetConfig::class.java)
  private val externalPackListType =
    Types.newParameterizedType(List::class.java, ExternalIconPackInfo::class.java)
  private val externalPackListAdapter = moshi.adapter<List<ExternalIconPackInfo>>(externalPackListType)
  private val appOverrideListType =
    Types.newParameterizedType(List::class.java, AppIconOverride::class.java)
  private val appOverrideListAdapter = moshi.adapter<List<AppIconOverride>>(appOverrideListType)
  private val presetListType =
    Types.newParameterizedType(List::class.java, SavedClockPreset::class.java)
  private val presetListAdapter = moshi.adapter<List<SavedClockPreset>>(presetListType)

  private val keyActiveDraft = stringPreferencesKey("active_studio_draft")
  private val keySavedPresets = stringPreferencesKey("saved_presets")
  private val keyPendingPinConfig = stringPreferencesKey("pending_pin_config")
  private val keyFavoriteStyles = stringPreferencesKey("favorite_styles")
  private val keyRecentStyles = stringPreferencesKey("recent_styles")
  private val keyAppThemeMode = stringPreferencesKey("app_theme_mode")
  private val keyLibraryLayoutMode = stringPreferencesKey("library_layout_mode")
  private val keyOnboardingCompleted = booleanPreferencesKey("onboarding_completed")
  private val keyLiveWallpaperTarget = stringPreferencesKey("live_wallpaper_target")
  private val keyLiveWallpaperOption = stringPreferencesKey("live_wallpaper_option")
  private val keyLiveWallpaperPhotoUri = stringPreferencesKey("live_wallpaper_photo_uri")
  private val keyAodSettings = stringPreferencesKey("aod_experience_settings")
  private val keyIconPackConfig = stringPreferencesKey("icon_pack_config")
  private val keyAppIconOverrides = stringPreferencesKey("app_icon_overrides")
  private val keyImportedPacks = stringPreferencesKey("imported_icon_packs")
  private val keySuperIconDraft = stringPreferencesKey("super_icon_draft")
  private val keyPendingSuperIcon = stringPreferencesKey("pending_super_icon")

  private fun widgetConfigKey(appWidgetId: Int) =
    stringPreferencesKey("widget_config_$appWidgetId")

  private fun superIconConfigKey(appWidgetId: Int) =
    stringPreferencesKey("super_icon_widget_$appWidgetId")

  val appThemeModeFlow: Flow<AppThemeMode> =
    context.chronoDataStore.data.map { prefs ->
      val raw = prefs[keyAppThemeMode]
      raw?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() } ?: AppThemeMode.SYSTEM
    }

  val libraryLayoutModeFlow: Flow<LibraryLayoutMode> =
    context.chronoDataStore.data.map { prefs ->
      val raw = prefs[keyLibraryLayoutMode]
      raw?.let { runCatching { LibraryLayoutMode.valueOf(it) }.getOrNull() } ?: LibraryLayoutMode.STAGGERED
    }

  val onboardingCompletedFlow: Flow<Boolean> =
    context.chronoDataStore.data.map { prefs ->
      prefs[keyOnboardingCompleted] ?: false
    }

  val liveWallpaperTargetFlow: Flow<LiveWallpaperTarget> =
    context.chronoDataStore.data.map { prefs ->
      val raw = prefs[keyLiveWallpaperTarget]
      raw?.let { runCatching { LiveWallpaperTarget.valueOf(it) }.getOrNull() } ?: LiveWallpaperTarget.LOCK_SCREEN
    }

  val liveWallpaperOptionFlow: Flow<WallpaperOption> =
    context.chronoDataStore.data.map { prefs ->
      val raw = prefs[keyLiveWallpaperOption]
      raw?.let { runCatching { WallpaperOption.valueOf(it) }.getOrNull() } ?: WallpaperOption.AURORA
    }

  val liveWallpaperPhotoUriFlow: Flow<String?> =
    context.chronoDataStore.data.map { prefs ->
      prefs[keyLiveWallpaperPhotoUri]
    }

  val aodSettingsFlow: Flow<AodExperienceSettings> =
    context.chronoDataStore.data.map { prefs ->
      val raw = prefs[keyAodSettings]
      raw?.let { runCatching { aodSettingsAdapter.fromJson(it) }.getOrNull() } ?: AodExperienceSettings()
    }

  val iconPackConfigFlow: Flow<IconPackConfig> =
    context.chronoDataStore.data.map { prefs ->
      val raw = prefs[keyIconPackConfig]
      raw?.let { runCatching { iconPackConfigAdapter.fromJson(it) }.getOrNull() } ?: IconPackConfig()
    }

  val appIconOverridesFlow: Flow<Map<String, AppIconOverride>> =
    context.chronoDataStore.data.map { prefs ->
      val raw = prefs[keyAppIconOverrides]
      val list = raw?.let { runCatching { appOverrideListAdapter.fromJson(it) }.getOrNull() } ?: emptyList()
      list.associateBy { it.componentKey }
    }

  val importedPacksFlow: Flow<List<ExternalIconPackInfo>> =
    context.chronoDataStore.data.map { prefs ->
      val raw = prefs[keyImportedPacks]
      raw?.let { runCatching { externalPackListAdapter.fromJson(it) }.getOrNull() } ?: emptyList()
    }

  val superIconDraftFlow: Flow<SuperIconWidgetConfig> =
    context.chronoDataStore.data.map { prefs ->
      val raw = prefs[keySuperIconDraft]
      raw?.let { runCatching { superIconConfigAdapter.fromJson(it) }.getOrNull() } ?: SuperIconWidgetConfig()
    }

  suspend fun setAppThemeMode(mode: AppThemeMode) {
    context.chronoDataStore.edit { prefs ->
      prefs[keyAppThemeMode] = mode.name
    }
  }

  suspend fun setLibraryLayoutMode(mode: LibraryLayoutMode) {
    context.chronoDataStore.edit { prefs ->
      prefs[keyLibraryLayoutMode] = mode.name
    }
  }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    context.chronoDataStore.edit { prefs ->
      prefs[keyOnboardingCompleted] = completed
    }
  }

  suspend fun setLiveWallpaperTarget(target: LiveWallpaperTarget) {
    context.chronoDataStore.edit { prefs ->
      prefs[keyLiveWallpaperTarget] = target.name
    }
  }

  suspend fun setLiveWallpaperBackdrop(option: WallpaperOption, customUri: String?) {
    context.chronoDataStore.edit { prefs ->
      prefs[keyLiveWallpaperOption] = option.name
      if (customUri != null) {
        prefs[keyLiveWallpaperPhotoUri] = customUri
      }
    }
  }

  suspend fun saveAodSettings(settings: AodExperienceSettings) {
    context.chronoDataStore.edit { prefs ->
      prefs[keyAodSettings] = aodSettingsAdapter.toJson(settings)
    }
  }

  suspend fun saveIconPackConfig(config: IconPackConfig) {
    context.chronoDataStore.edit { prefs ->
      prefs[keyIconPackConfig] = iconPackConfigAdapter.toJson(config)
    }
  }

  suspend fun saveAppIconOverride(override: AppIconOverride) {
    val current = appIconOverridesFlow.first().toMutableMap()
    current[override.componentKey] = override
    context.chronoDataStore.edit { prefs ->
      prefs[keyAppIconOverrides] = appOverrideListAdapter.toJson(current.values.toList())
    }
  }

  suspend fun markAppPinned(componentKey: String) {
    val current = appIconOverridesFlow.first().toMutableMap()
    val existing = current[componentKey] ?: AppIconOverride(componentKey = componentKey)
    current[componentKey] = existing.copy(isPinnedToHome = true)
    context.chronoDataStore.edit { prefs ->
      prefs[keyAppIconOverrides] = appOverrideListAdapter.toJson(current.values.toList())
    }
  }

  suspend fun clearAppIconOverride(componentKey: String) {
    val current = appIconOverridesFlow.first().toMutableMap()
    current.remove(componentKey)
    context.chronoDataStore.edit { prefs ->
      prefs[keyAppIconOverrides] = appOverrideListAdapter.toJson(current.values.toList())
    }
  }

  suspend fun addImportedPack(pack: ExternalIconPackInfo) {
    val current = importedPacksFlow.first().filterNot { it.packId == pack.packId }
    context.chronoDataStore.edit { prefs ->
      prefs[keyImportedPacks] = externalPackListAdapter.toJson(listOf(pack) + current)
    }
  }

  suspend fun removeImportedPack(packId: String) {
    val current = importedPacksFlow.first().filterNot { it.packId == packId }
    context.chronoDataStore.edit { prefs ->
      prefs[keyImportedPacks] = externalPackListAdapter.toJson(current)
    }
  }

  suspend fun saveSuperIconDraft(config: SuperIconWidgetConfig) {
    context.chronoDataStore.edit { prefs ->
      prefs[keySuperIconDraft] = superIconConfigAdapter.toJson(config)
    }
  }

  suspend fun getSuperIconConfig(appWidgetId: Int): SuperIconWidgetConfig? {
    val prefs = context.chronoDataStore.data.first()
    val raw = prefs[superIconConfigKey(appWidgetId)] ?: return null
    return runCatching { superIconConfigAdapter.fromJson(raw) }.getOrNull()
  }

  suspend fun saveSuperIconConfig(appWidgetId: Int, config: SuperIconWidgetConfig) {
    context.chronoDataStore.edit { prefs ->
      prefs[superIconConfigKey(appWidgetId)] = superIconConfigAdapter.toJson(config)
    }
  }

  suspend fun deleteSuperIconConfig(appWidgetId: Int) {
    context.chronoDataStore.edit { prefs ->
      prefs.remove(superIconConfigKey(appWidgetId))
    }
  }

  suspend fun setPendingSuperIconConfig(config: SuperIconWidgetConfig) {
    context.chronoDataStore.edit { prefs ->
      prefs[keyPendingSuperIcon] = superIconConfigAdapter.toJson(config)
    }
  }

  suspend fun consumePendingSuperIconConfig(): SuperIconWidgetConfig? {
    val prefs = context.chronoDataStore.data.first()
    val raw = prefs[keyPendingSuperIcon] ?: return null
    context.chronoDataStore.edit { it.remove(keyPendingSuperIcon) }
    return runCatching { superIconConfigAdapter.fromJson(raw) }.getOrNull()
  }

  val favoriteStylesFlow: Flow<Set<ClockStyleId>> =
    context.chronoDataStore.data.map { prefs ->
      val raw = prefs[keyFavoriteStyles].orEmpty()
      if (raw.isBlank()) {
        setOf(ClockStyleId.WEIGHT_STACK_FLOAT, ClockStyleId.NEON_FRAME_GLOW, ClockStyleId.SOLAR_ORBIT_PLANETS)
      } else {
        raw.split(",").mapNotNull { name -> runCatching { ClockStyleId.valueOf(name) }.getOrNull() }.toSet()
      }
    }

  val recentStylesFlow: Flow<List<ClockStyleId>> =
    context.chronoDataStore.data.map { prefs ->
      val raw = prefs[keyRecentStyles].orEmpty()
      if (raw.isBlank()) {
        emptyList()
      } else {
        raw.split(",").mapNotNull { name -> runCatching { ClockStyleId.valueOf(name) }.getOrNull() }
      }
    }

  suspend fun toggleFavoriteStyle(styleId: ClockStyleId) {
    val current = favoriteStylesFlow.first()
    val updated = if (styleId in current) current - styleId else current + styleId
    context.chronoDataStore.edit { prefs ->
      prefs[keyFavoriteStyles] = updated.joinToString(",") { it.name }
    }
  }

  suspend fun recordRecentStyle(styleId: ClockStyleId) {
    val current = recentStylesFlow.first()
    val updated = (listOf(styleId) + current.filterNot { it == styleId }).take(8)
    context.chronoDataStore.edit { prefs ->
      prefs[keyRecentStyles] = updated.joinToString(",") { it.name }
    }
  }

  val activeDraftFlow: Flow<WidgetConfig> =
    context.chronoDataStore.data.map { prefs ->
      val raw = prefs[keyActiveDraft]
      if (raw != null) {
        runCatching { configAdapter.fromJson(raw) }.getOrNull()
          ?: ClockPresets.defaultConfigForStyle(ClockStyleId.CYBER_HUD)
      } else {
        ClockPresets.defaultConfigForStyle(ClockStyleId.CYBER_HUD)
      }
    }

  val savedPresetsFlow: Flow<List<SavedClockPreset>> =
    context.chronoDataStore.data.map { prefs ->
      val raw = prefs[keySavedPresets]
      if (raw != null) {
        runCatching { presetListAdapter.fromJson(raw) }.getOrNull() ?: emptyList()
      } else {
        emptyList()
      }
    }

  suspend fun saveActiveDraft(config: WidgetConfig) {
    context.chronoDataStore.edit { prefs ->
      prefs[keyActiveDraft] = configAdapter.toJson(config)
    }
  }

  suspend fun getWidgetConfig(appWidgetId: Int, fallbackStyle: ClockStyleId = ClockStyleId.CYBER_HUD): WidgetConfig {
    val prefs = context.chronoDataStore.data.first()
    val raw = prefs[widgetConfigKey(appWidgetId)]
    return if (raw != null) {
      runCatching { configAdapter.fromJson(raw) }.getOrNull()
        ?: ClockPresets.defaultConfigForStyle(fallbackStyle)
    } else {
      ClockPresets.defaultConfigForStyle(fallbackStyle)
    }
  }

  suspend fun saveWidgetConfig(appWidgetId: Int, config: WidgetConfig) {
    context.chronoDataStore.edit { prefs ->
      prefs[widgetConfigKey(appWidgetId)] = configAdapter.toJson(config)
    }
  }

  suspend fun deleteWidgetConfig(appWidgetId: Int) {
    context.chronoDataStore.edit { prefs ->
      prefs.remove(widgetConfigKey(appWidgetId))
    }
  }

  suspend fun setPendingPinConfig(config: WidgetConfig) {
    context.chronoDataStore.edit { prefs ->
      prefs[keyPendingPinConfig] = configAdapter.toJson(config)
    }
  }

  suspend fun consumePendingPinConfig(): WidgetConfig? {
    val prefs = context.chronoDataStore.data.first()
    val raw = prefs[keyPendingPinConfig] ?: return null
    context.chronoDataStore.edit { it.remove(keyPendingPinConfig) }
    return runCatching { configAdapter.fromJson(raw) }.getOrNull()
  }

  suspend fun savePreset(name: String, config: WidgetConfig) {
    val current = savedPresetsFlow.first()
    val newPreset =
      SavedClockPreset(
        id = "preset_${System.currentTimeMillis()}",
        name = name.ifBlank { config.styleId.displayName },
        config = config,
      )
    val updated = listOf(newPreset) + current
    context.chronoDataStore.edit { prefs ->
      prefs[keySavedPresets] = presetListAdapter.toJson(updated)
    }
  }

  suspend fun duplicatePreset(preset: SavedClockPreset) {
    savePreset("${preset.name} Copy", preset.config)
  }

  suspend fun deletePreset(presetId: String) {
    val current = savedPresetsFlow.first()
    val updated = current.filterNot { it.id == presetId }
    context.chronoDataStore.edit { prefs ->
      prefs[keySavedPresets] = presetListAdapter.toJson(updated)
    }
  }
}
