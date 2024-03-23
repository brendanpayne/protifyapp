package com.protify.protifyapp.features.weather

data class WeatherOverview(
    val id: String,
    val type: String,
    val geometry: Geometry,
    val properties: Properties
) {
    data class Geometry(
        val type: String,
        val coordinates: List<Double>
    )

    data class Properties(
        val id: String,
        val type: String,
        val cwa: String,
        val forecastOffice: String,
        val gridId: String,
        val gridX: Int,
        val gridY: Int,
        val forecast: String,
        val forecastHourly: String,
        val forecastGridData: String,
        val observationStations: String,
        val relativeLocation: RelativeLocation,
        val forecastZone: String,
        val county: String,
        val fireWeatherZone: String,
        val timeZone: String,
        val radarStation: String
    )

    data class RelativeLocation(
        val type: String,
        val geometry: Geometry,
        val properties: LocationProperties
    ) {
        data class LocationProperties(
            val city: String,
            val state: String,
            val distance: Distance,
            val bearing: Bearing
        )

        data class Distance(
            val unitCode: String,
            val value: Double
        )

        data class Bearing(
            val unitCode: String,
            val value: Double
        )
    }
}
