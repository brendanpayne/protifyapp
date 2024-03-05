package com.protify.protifyapp.utils

import DirectionsResponse
import com.google.gson.GsonBuilder
import com.protify.protifyapp.APIKeys
import okhttp3.OkHttpClient
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset

class MapsStopsUtils(
    // destinationLong: Double, destinationLat: Double,
                     departTime: LocalDateTime) {
    private val departTime = departTime
    //val mapsKey = System.getenv("maps_api")
    val mapsKey = APIKeys().getMapsKey()
    val beginningOfTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0)
    val departTimeFix = departTime.plusDays(1).toEpochSecond(ZoneOffset.UTC) - beginningOfTime.toEpochSecond(
        ZoneOffset.UTC)
//    private val url = "https://maps.googleapis.com/maps/api/directions/json" +
//            "?departure_time=${departTimeFix}" +
//            "&destination=MainevilleOhio&" +
//            "origin=39.85%2C-82.82" +
//            "&waypoints=via%3AWaynesvilleOhio%2CMA" +
//            "&key=$mapsKey"

    private val url = "https://maps.googleapis.com/maps/api/directions/json?origin=1101%20Beech%20Rd%20SW,%20New%20Albany,%20OH%2043054&destination=6190%20Falla%20Dr,%20Canal%20Winchester,%20OH%2043110&waypoints=2700%20Brice%20Rd,%20Reynoldsburg,%20OH%2043068&key=${mapsKey}"

    private val client: OkHttpClient = OkHttpClient().newBuilder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val gson = GsonBuilder().create()
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
    private fun getDuration(startLocation: String?, endLocation: String?, waypoint: String?, onComplete: (DirectionsResponse?) -> Unit) {
        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=$startLocation&destination=$endLocation&waypoints=$waypoint&key=${mapsKey}"
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

    fun getTotalTime(startLocation: String?, endLocation: String?, waypoint: String?, onComplete: (Int) -> Unit) {
        getDuration(startLocation, endLocation, waypoint) { directionsResponse ->
            if (directionsResponse == null) {
                onComplete(0)
                return@getDuration
            }
            if (directionsResponse.routes[0].legs.count() > 1) {
                var totalTime = 0
                for (leg in directionsResponse.routes[0].legs) {
                    if (leg.duration_in_traffic != null) {
                        totalTime += leg.duration_in_traffic.value
                    }
                    else {
                        totalTime += leg.duration.value
                    }
                }
                onComplete(totalTime)
            } else {
                onComplete(directionsResponse.routes[0].legs[0].duration_in_traffic.value)
            }

        }
    }
    fun getTimeSavings(onComplete: (Int) -> Unit) {
        MapsDurationUtils(LocalDateTime.now()).getDistance { directDistance ->
            getDuration { directionsResponse ->
                if (directionsResponse == null) {
                    onComplete(0)
                    return@getDuration
                }
                // If there is more than one leg, that means there is a stop on the way home
                if (directionsResponse.routes[0].legs.count() > 1) {
                    var totalTime = 0
                    var timeAdded = 0
                    // Last leg is the leg that goes from the last stop to home
                    var lastLeg = directionsResponse.routes[0].legs.last()
                    for (leg in directionsResponse.routes[0].legs) {
                        // If the leg has traffic data, use that. Otherwise use the normal duration
                        if (leg.duration_in_traffic != null) {
                            totalTime += leg.duration_in_traffic.value
                        }
                        else {
                            totalTime += leg.duration.value
                        }
                    }
                    // DirectDistance is how long it would take to get home if I went straight there from work.
                    // Time added is giving us the amount of extra time it will take to stop at kroger on the way home
                    timeAdded = totalTime - directDistance
                    // The time added should always be greater than 0 become the direct travel should always be shorter than the travel with the stop
                    if (timeAdded > 0) {

                        if (lastLeg.duration_in_traffic != null) {
                            //Were multipying by 2 because you would otherwise have to go to the location, then back home
                            //If the time added from this transaction is less than double the value of the last leg, then it would save time
                            //Is it really worth it if you're saving less than 10 minutes? (600 seconds)
                            if (timeAdded+600 < (lastLeg.duration_in_traffic.value * 2)) {
                                //Output the amount of time you would save if you took this route
                                onComplete(lastLeg.duration_in_traffic.value * 2 - timeAdded)
                            } else {
                                //Not sure if this is the best way to handle this, but the -1 represents that it would not save time
                                onComplete(-1)
                            }
                        }
                        else {
                            if (timeAdded+600 < (lastLeg.duration.value * 2)) {
                                onComplete(lastLeg.duration.value * 2 - timeAdded)
                            } else {
                                onComplete(-1)
                            }
                        }
                    } else {
                        //Error handling needs to go here. This means that the route with the stop takes less time than the direct route
                    }
                } else {
                    // If the response only has one leg, then we don't know how long it takes to get from the last stop to home, so we need to run another api request
                }

            }

        }
    }

}
