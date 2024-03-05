package com.protify.protifyapp.features.GoogleMapsAPI

data class GeocodedResponse(
    val results: List<Result>,
    val status: String)
{
    data class Result(
        val addressComponents: List<AddressComponent>,
        val formattedAddress: String,
        val geometry: Geometry,
        val placeId: String,
        val plusCode: PlusCode,
        val types: List<String>
    )
    {
        data class AddressComponent(
            val longName: String,
            val shortName: String,
            val types: List<String>
        )
        data class Geometry(
            val location: Location,
            val locationType: String,
            val viewport: Viewport
        )
        {
            data class Location(
                val lat: Double,
                val lng: Double
            )
            data class Viewport(
                val northeast: Northeast,
                val southwest: Southwest
            )
            {
                data class Northeast(
                    val lat: Double,
                    val lng: Double
                )
                data class Southwest(
                    val lat: Double,
                    val lng: Double
                )
            }
        }
        data class PlusCode(
            val compoundCode: String,
            val globalCode: String
        )
    }
}