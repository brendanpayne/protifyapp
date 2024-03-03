package com.protify.protifyapp.features.GoogleMapsAPI

import com.google.gson.annotations.SerializedName

data class DrivingTimeMatrix(
    @SerializedName("destination_addresses")
    val destinationAddresses: List<String>,
    @SerializedName("origin_addresses")
    val originAddresses: List<String>,
    @SerializedName("rows")
    val rows: List<Row>,
    @SerializedName("status")
    val status: String
) {
    data class Row(
        @SerializedName("elements")
        val elements: List<Element>
    ) {
        data class Element(
            @SerializedName("distance")
            val distance: Distance,
            @SerializedName("duration")
            val duration: Duration,
            @SerializedName("status")
            val status: String
        ) {
            data class Distance(
                @SerializedName("text")
                val text: String,
                @SerializedName("value")
                val value: Int
            )

            data class Duration(
                @SerializedName("text")
                val text: String,
                @SerializedName("value")
                val value: Int
            )
        }
    }
}