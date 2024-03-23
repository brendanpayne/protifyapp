
data class DirectionsResponse(
    val geocodedWaypoints: List<GeocodedWaypoint>,
    val routes: List<Route>,
    val status: String
) {
    data class GeocodedWaypoint(
        val geocoderStatus: String,
        val partialMatch: Boolean,
        val placeId: String,
        val types: List<String>
    )

    data class Bounds(
        val northeast: Location,
        val southwest: Location
    )

    data class Location(
        val lat: Double,
        val lng: Double
    )

    data class Distance(
        val text: String,
        val value: Int
    )

    data class Duration(
        val text: String,
        val value: Int
    )

    data class Step(
        val distance: Distance,
        val duration: Duration,
        val endLocation: Location,
        val htmlInstructions: String,
        val polyline: Polyline,
        val startLocation: Location,
        val travelMode: String
    )

    data class Polyline(
        val points: String
    )

    data class Leg(
        val distance: Distance,
        val duration: Duration,
        val duration_in_traffic: Duration,
        val endAddress: String,
        val endLocation: Location,
        val startAddress: String,
        val startLocation: Location,
        val steps: List<Step>
    )

    data class Route(
        val bounds: Bounds,
        val copyright: String,
        val legs: List<Leg>,
        val overviewPolyline: Polyline,
        val summary: String,
        val warnings: List<String>,
        val waypointOrder: List<Int>
    )

}
