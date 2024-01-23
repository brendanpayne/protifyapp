package com.protify.protifyapp.utils

import DistanceResponse
import com.google.gson.GsonBuilder
import com.protify.protifyapp.APIKeys
import com.protify.protifyapp.FirestoreEvent
import okhttp3.OkHttpClient
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset

class MapsDurationUtils(startLong: Double, startLat: Double,
                        // destinationLong: Double, destinationLat: Double,
                        departTime: LocalDateTime) {
    val mapsKey = APIKeys().getMapsKey()
    val beginningOfTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0)
    val departTimeFix = departTime.plusDays(1).toEpochSecond(ZoneOffset.UTC) - beginningOfTime.toEpochSecond(ZoneOffset.UTC)
//    private val url = "https://maps.googleapis.com/maps/api/distancematrix/json" +
//            "?departure_time=${departTimeFix}" +
//            "&destinations=40.659569%2C-73.933783%7C40.729029%2C-73.851524%7C40.6860072%2C-73.6334271%7C40.598566%2C-73.7527626" +
//            "&origins=$startLong%2C$startLat" +
//            "&key=$mapsKey"
    private val url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=1101%20Beech%20Rd%20SW,%20New%20Albany,%20OH%2043054&destinations=6190%20Falla%20Dr,%20Canal%20Winchester,%20OH%2043110&key=${mapsKey}"

    private val client: OkHttpClient = OkHttpClient().newBuilder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val gson = GsonBuilder().create()

    private fun getDuration(onComplete: (DistanceResponse?) -> Unit) {
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
                val distanceResponse = gson.fromJson(body, DistanceResponse::class.java)
                onComplete(distanceResponse)
            }
        })
    }
    private fun getDuration(startLocation: String?, endLocation: String?, onComplete: (DistanceResponse?) -> Unit) {
        val url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=$startLocation&destinations=$endLocation&key=${mapsKey}"
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
                val distanceResponse = gson.fromJson(body, DistanceResponse::class.java)
                onComplete(distanceResponse)
            }
        })
    }
    private fun getDuration(homeAddress: String, startLocation: String?, onComplete: (DistanceResponse?) -> Unit) {
        val url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=$startLocation&destinations=$homeAddress&key=${mapsKey}"
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
                val distanceResponse = gson.fromJson(body, DistanceResponse::class.java)
                onComplete(distanceResponse)
            }
        })
    }
    fun getDistance(onComplete: (Int) -> Unit) {
        getDuration { distanceResponse ->
            if (distanceResponse == null) {
                onComplete(0)
                return@getDuration
            }
            onComplete(distanceResponse.rows[0].elements[0].duration.value)
        }
    }

    fun isChainedEvent(firstEvent: FirestoreEvent, secondEvent: FirestoreEvent, homeAddress: String ,onComplete: (Boolean, Boolean) -> Unit) {
        val timeGap = secondEvent.endTime.toEpochSecond(ZoneOffset.UTC) - firstEvent.endTime.toEpochSecond(ZoneOffset.UTC)


        //Get the distance between the two events
        getDuration(firstEvent.location, secondEvent.location) { eventDistanceResponse ->
            if (eventDistanceResponse == null) {
                onComplete(false, false)
                return@getDuration
            }
            val eventDistance: Int = eventDistanceResponse.rows[0].elements[0].duration.value

            //Get the distance from the first event, to home, to the second event,
            MapsStopsUtils().getTotalTime { TimeFromHome ->
                if ((TimeFromHome < timeGap - 600) || (eventDistance < timeGap - 600)) {
                    onComplete(true, true)
                }
                else {
                    onComplete(true, false)
            }
        }

        }
    }
}