package com.protify.protifyapp.features.weather

data class WeatherForecast(
    val type: String,
    val geometry: Geometry,
    val properties: Properties
) {
    data class Geometry(
        val type: String,
        val coordinates: List<List<List<Double>>>
    )

    data class Properties(
        val updated: String,
        val units: String,
        val forecastGenerator: String,
        val generatedAt: String,
        val updateTime: String,
        val validTimes: String,
        val elevation: Elevation,
        val periods: List<Period>
    ) {
        data class Elevation(
            val unitCode: String,
            val value: Double
        )

        data class Period(
            val number: Int,
            val name: String,
            val startTime: String,
            val endTime: String,
            val isDaytime: Boolean,
            val temperature: Int,
            val temperatureUnit: String,
            val temperatureTrend: String?,
            val probabilityOfPrecipitation: Precipitation?,
            val dewpoint: Dewpoint,
            val relativeHumidity: Humidity,
            val windSpeed: String,
            val windDirection: String,
            val icon: String,
            val shortForecast: String,
            val detailedForecast: String
        ) {
            data class Precipitation(
                val unitCode: String,
                val value: Int
            )

            data class Dewpoint(
                val unitCode: String,
                val value: Double
            )

            data class Humidity(
                val unitCode: String,
                val value: Int
            )
        }
    }
}
