package ru.polkatv.launcher.weather

interface WeatherApi {
    suspend fun forecast(lat: Double, lon: Double): ForecastResponse
    suspend fun search(query: String): List<GeoResultDto>
}
