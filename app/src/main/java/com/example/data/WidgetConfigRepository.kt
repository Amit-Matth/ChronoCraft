package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ClockPresets
import com.example.model.ClockStyleId
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

  private fun widgetConfigKey(appWidgetId: Int) =
    stringPreferencesKey("widget_config_$appWidgetId")

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

  suspend fun deletePreset(presetId: String) {
    val current = savedPresetsFlow.first()
    val updated = current.filterNot { it.id == presetId }
    context.chronoDataStore.edit { prefs ->
      prefs[keySavedPresets] = presetListAdapter.toJson(updated)
    }
  }
}
