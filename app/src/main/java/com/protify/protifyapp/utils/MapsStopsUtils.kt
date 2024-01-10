package com.protify.protifyapp.utils

import DirectionsResponse
import com.google.firebase.firestore.Exclude
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset

class MapsStopsUtils(startLong: Double, startLat: Double,
    // destinationLong: Double, destinationLat: Double,
                     departTime: LocalDateTime) {

    val mapsKey = System.getenv("maps_api")
    val beginningOfTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0)
    val departTimeFix = departTime.plusDays(1).toEpochSecond(ZoneOffset.UTC) - beginningOfTime.toEpochSecond(
        ZoneOffset.UTC)
    val url = "https://maps.googleapis.com/maps/api/directions/json" +
            "?departure_time=${departTimeFix}" +
            "&destination=MainevilleOhio" +
            "origin=LancasterOhio%2C%20MA" +
            "&waypoints=via%3AWaynesvilleOhio%2CMA" +
            "&key=$mapsKey"

    private val client: OkHttpClient = OkHttpClient().newBuilder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val gson = GsonBuilder()
        .serializeNulls() // Include null values
        .setExclusionStrategies(object : ExclusionStrategy {
            override

            fun

                    shouldSkipField(f: FieldAttributes): Boolean {
                return f.getAnnotation(Exclude::class.java) != null
            }

            override fun shouldSkipClass(clazz: Class<*>): Boolean {
                return false
            }
        })
        .create()
    private fun getDuration(onComplete: (DirectionsResponse?) -> Unit) {
        val request = okhttp3.Request.Builder()
            .url(url)
            .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onComplete(null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string()
                if (response.code != 200) {
                    onComplete(null)
                    return
                }
                val directionsResponse = gson.fromJson(body, DirectionsResponse::class.java)
                onComplete(directionsResponse)
            }
        })
    }

    fun getTotalTime(onComplete: (Int) -> Unit) {
        getDuration { directionsResponse ->
            if (directionsResponse == null) {
                onComplete(0)
                return@getDuration
            }
            onComplete(directionsResponse.routes[0].legs[0].duration_in_traffic.value)
        }
    }

}
