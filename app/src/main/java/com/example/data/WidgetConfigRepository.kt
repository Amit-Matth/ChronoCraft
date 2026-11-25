package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppThemeMode
import com.example.model.ClockPresets
import com.example.model.ClockStyleId
import com.example.model.LibraryLayoutMode
import com.example.model.SavedClockPreset
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

  private fun widgetConfigKey(appWidgetId: Int) =
    stringPreferencesKey("widget_config_$appWidgetId")

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
