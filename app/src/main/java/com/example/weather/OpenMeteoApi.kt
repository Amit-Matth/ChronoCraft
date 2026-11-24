package com.example.weather

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class WeatherForecastResponse(
  val current: CurrentWeatherDto? = null,
  val daily: DailyWeatherDto? = null,
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
  @Json(name = "temperature_2m") val temperature2m: Double = 0.0,
  @Json(name = "apparent_temperature") val apparentTemperature: Double = 0.0,
  @Json(name = "weather_code") val weatherCode: Int = 0,
  @Json(name = "wind_speed_10m") val windSpeed10m: Double = 0.0,
  @Json(name = "is_day") val isDay: Int = 1,
)

@JsonClass(generateAdapter = true)
data class DailyWeatherDto(
  @Json(name = "temperature_2m_max") val maxTemps: List<Double> = emptyList(),
  @Json(name = "temperature_2m_min") val minTemps: List<Double> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
  val results: List<GeocodingPlace>? = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GeocodingPlace(
  val name: String,
  val latitude: Double,
  val longitude: Double,
  val country: String? = null,
  @Json(name = "admin1") val region: String? = null,
) {
  fun displayLabel(): String {
    return listOfNotNull(name, region, country).distinct().take(2).joinToString(", ")
  }
}

interface OpenMeteoWeatherService {
  @GET("v1/forecast")
  suspend fun getForecast(
    @Query("latitude") latitude: Double,
    @Query("longitude") longitude: Double,
    @Query("current") current: String = "temperature_2m,apparent_temperature,weather_code,wind_speed_10m,is_day",
    @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min",
    @Query("forecast_days") forecastDays: Int = 1,
    @Query("timezone") timezone: String = "auto",
  ): WeatherForecastResponse
}

interface OpenMeteoGeocodingService {
  @GET("v1/search")
  suspend fun searchCity(
    @Query("name") name: String,
    @Query("count") count: Int = 6,
    @Query("language") language: String = "en",
    @Query("format") format: String = "json",
  ): GeocodingResponse
}
