package com.example.weather

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.WeatherSnapshot
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

private val Context.weatherDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "chrono_weather_prefs")

class WeatherRepository(private val context: Context) {

  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val snapshotAdapter = moshi.adapter(WeatherSnapshot::class.java)

  private val okHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(10, TimeUnit.SECONDS)
      .build()

  private val weatherService: OpenMeteoWeatherService by lazy {
    Retrofit.Builder()
      .baseUrl("https://api.open-meteo.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(OpenMeteoWeatherService::class.java)
  }

  private val geocodingService: OpenMeteoGeocodingService by lazy {
    Retrofit.Builder()
      .baseUrl("https://geocoding-api.open-meteo.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(OpenMeteoGeocodingService::class.java)
  }

  private val keyWeatherSnapshot = stringPreferencesKey("weather_snapshot_json")

  val weatherFlow: Flow<WeatherSnapshot> =
    context.weatherDataStore.data.map { prefs ->
      val raw = prefs[keyWeatherSnapshot]
      if (raw != null) {
        runCatching { snapshotAdapter.fromJson(raw) }.getOrNull() ?: WeatherSnapshot()
      } else {
        WeatherSnapshot()
      }
    }

  suspend fun getCachedWeather(): WeatherSnapshot = weatherFlow.first()

  suspend fun searchCities(query: String): Result<List<GeocodingPlace>> =
    withContext(Dispatchers.IO) {
      runCatching {
        if (query.trim().length < 2) return@runCatching emptyList()
        geocodingService.searchCity(name = query.trim()).results.orEmpty()
      }
    }

  suspend fun refreshWeatherForCity(
    cityName: String,
    latitude: Double,
    longitude: Double,
  ): Result<WeatherSnapshot> =
    withContext(Dispatchers.IO) {
      runCatching {
        val resp = weatherService.getForecast(latitude = latitude, longitude = longitude)
        val cur = resp.current ?: error("Missing current weather payload")
        val high = resp.daily?.maxTemps?.firstOrNull() ?: cur.temperature2m
        val low = resp.daily?.minTemps?.firstOrNull() ?: cur.temperature2m
        val snapshot =
          WeatherSnapshot(
            cityName = cityName,
            latitude = latitude,
            longitude = longitude,
            tempCelsius = cur.temperature2m,
            feelsLikeCelsius = cur.apparentTemperature,
            highCelsius = high,
            lowCelsius = low,
            windKmh = cur.windSpeed10m,
            weatherCode = cur.weatherCode,
            isDay = cur.isDay == 1,
            isLoaded = true,
            updatedAtMillis = System.currentTimeMillis(),
          )
        saveWeatherSnapshot(snapshot)
        snapshot
      }
    }

  suspend fun refreshIfStale(maxAgeMillis: Long = 20 * 60 * 1000L): WeatherSnapshot {
    val cached = getCachedWeather()
    val age = System.currentTimeMillis() - cached.updatedAtMillis
    if (!cached.isLoaded || age > maxAgeMillis) {
      val result = refreshWeatherForCity(cached.cityName, cached.latitude, cached.longitude)
      return result.getOrDefault(cached)
    }
    return cached
  }

  @SuppressLint("MissingPermission")
  suspend fun refreshUsingDeviceLocation(): Result<WeatherSnapshot> =
    withContext(Dispatchers.IO) {
      runCatching {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        val location =
          suspendCancellableCoroutine<android.location.Location?> { cont ->
            fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
              .addOnSuccessListener { loc ->
                if (loc != null) {
                  cont.resume(loc)
                } else {
                  fused.lastLocation
                    .addOnSuccessListener { last -> cont.resume(last) }
                    .addOnFailureListener { cont.resume(null) }
                }
              }
              .addOnFailureListener {
                fused.lastLocation
                  .addOnSuccessListener { last -> cont.resume(last) }
                  .addOnFailureListener { cont.resume(null) }
              }
          } ?: error("Location unavailable — search a city manually")

        val city = resolveCityName(location.latitude, location.longitude)
        refreshWeatherForCity(city, location.latitude, location.longitude).getOrThrow()
      }
    }

  private suspend fun resolveCityName(lat: Double, lon: Double): String {
    return runCatching {
      val geocoder = Geocoder(context, Locale.getDefault())
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { cont ->
          geocoder.getFromLocation(lat, lon, 1) { list ->
            val addr = list.firstOrNull()
            cont.resume(addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea ?: "Local Area")
          }
        }
      } else {
        @Suppress("DEPRECATION")
        val addr = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
        addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea ?: "Local Area"
      }
    }.getOrDefault("Local Area")
  }

  private suspend fun saveWeatherSnapshot(snapshot: WeatherSnapshot) {
    context.weatherDataStore.edit { prefs ->
      prefs[keyWeatherSnapshot] = snapshotAdapter.toJson(snapshot)
    }
  }
}
