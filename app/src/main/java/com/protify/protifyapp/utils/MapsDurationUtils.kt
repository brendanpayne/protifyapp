package com.protify.protifyapp.utils

import DistanceResponse
import com.google.gson.GsonBuilder
import com.protify.protifyapp.APIKeys
import com.protify.protifyapp.FirestoreEvent
import com.protify.protifyapp.features.GoogleMapsAPI.DrivingTime
import com.protify.protifyapp.features.GoogleMapsAPI.DrivingTimeMatrix
import com.protify.protifyapp.features.GoogleMapsAPI.GeocodedResponse
import okhttp3.OkHttpClient
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset

class MapsDurationUtils(departTime: LocalDateTime) {
    val mapsKey = APIKeys().getMapsKey()
    val beginningOfTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0)
    val departTimeFix = departTime.plusMinutes(10).toEpochSecond(ZoneOffset.UTC) - beginningOfTime.toEpochSecond(ZoneOffset.UTC)
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
        var startLocationFormatted = startLocation?.replace(" ", "%20")
        var endLocationFormatted = endLocation?.replace(" ", "%20")
        val testurl = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=$startLocationFormatted&destinations=$endLocationFormatted&key=$mapsKey"
        val url =   "https://maps.googleapis.com/maps/api/distancematrix/json?origins=1091%20Hill%20Rd%20N,%20Pickerington,%20OH%2043147,%20USA&destinations=6591%20Winchester%20Blvd,%20Canal%20Winchester,%20OH%2043110,%20USA&key=${mapsKey}"
        val request = okhttp3.Request.Builder()
            .url(testurl)
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
     fun getMatrix(homeAddress: String?, endLocations: List<String?>, onComplete: (DrivingTimeMatrix?) -> Unit) {

         //get unique locations
         var endLocations = endLocations.toMutableList()
        //If the home address isn't in the locations, add it
        if (!endLocations.contains(homeAddress)) {
            endLocations.add(homeAddress)
        }
         // If there are any empty strings, remove them
         endLocations = endLocations.filter { it != "" }.toMutableList()
         // Get only distinct
         endLocations = endLocations.distinct().toMutableList()



        var startLocationsFormatted = homeAddress?.replace(" ", "%20")
        var endLocationsFormatted = endLocations.map { it?.replace(" ", "%20") }
        val url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=${endLocationsFormatted.joinToString("%7C")}&destinations=${endLocationsFormatted.joinToString("%7C")}&key=${mapsKey}"
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
                //Map to DrivingTimeMatrix class
                val distanceResponse = gson.fromJson(body, DrivingTimeMatrix::class.java)
                val jsonMatrix = gson.toJson(distanceResponse)
                onComplete(distanceResponse)
            }
        })

    }
    fun getDrivingTimes(homeAddress: String?, endLocations: List<String?>, onComplete: (List<DrivingTime?>) -> Unit) {

        var drivingTimes = mutableListOf<DrivingTime?>()

        var endLocations = endLocations.toMutableList()
        getMatrix(homeAddress, endLocations) { matrix ->
            if (matrix == null) {
                onComplete(emptyList())
                return@getMatrix
            }
            for (i in matrix.rows.indices) {
                //j is representing the destination
                for (j in matrix.rows[i].elements.indices) {
                    //Make sure origin and destination are not the same
                    if (matrix.originAddresses[i] != matrix.destinationAddresses[j]) {
                        //This is the driving time in seconds
                        val drivingTime =
                            matrix.rows[i].elements[j].duration.value
                        //This is the driving time in text
                        val drivingTimeText =
                            matrix.rows[i].elements[j].duration.text
                        //Add the driving time to the list
                        drivingTimes.add(
                            DrivingTime(
                                matrix.originAddresses[i],
                                matrix.destinationAddresses[j],
                                drivingTimeText
                            )
                        )
                    }
                }
            }
            onComplete(removeDuplicateDrivingTimes(drivingTimes))
        }
    }
    private fun removeDuplicateDrivingTimes(drivingTimes: List<DrivingTime?>): List<DrivingTime?> {
        val uniqueDrivingTimes = mutableListOf<DrivingTime?>()
        for (drivingTime in drivingTimes) {
            if (uniqueDrivingTimes.none { it?.startLocation == drivingTime?.endLocation && it?.endLocation == drivingTime?.startLocation }) {
                uniqueDrivingTimes.add(drivingTime)
            }
        }
        return uniqueDrivingTimes
    }
//    private fun getDuration(homeAddress: String, startLocation: String?, onComplete: (DistanceResponse?) -> Unit) {
//        val url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=$startLocation&destinations=$homeAddress&key=${mapsKey}"
//        val request = okhttp3.Request.Builder()
//            .url(url)
//            .build()
//        client.newCall(request).enqueue(object : okhttp3.Callback {
//            override fun onFailure(call: okhttp3.Call, e: IOException) {
//                onComplete(null)
//            }
//
//            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
//                val body = response.body?.string()
//                if (response.code != 200) {
//                    onComplete(null)
//                    return
//                }
//                val distanceResponse = gson.fromJson(body, DistanceResponse::class.java)
//                onComplete(distanceResponse)
//            }
//        })
//    }
    fun getDistance(onComplete: (Int) -> Unit) {
        getDuration { distanceResponse ->
            if (distanceResponse == null) {
                onComplete(0)
                return@getDuration
            }
            onComplete(distanceResponse.rows[0].elements[0].duration.value)
        }
    }
    fun getDistance(startLocation: String?, endLocation: String?, onComplete: (Int) -> Unit) {
        getDuration(startLocation, endLocation) { distanceResponse ->
            if (distanceResponse == null) {
                onComplete(0)
                return@getDuration
            }
            onComplete(distanceResponse.rows[0].elements[0].duration.value)
        }
    }

    fun isChainedEvent(firstEvent: FirestoreEvent, secondEvent: FirestoreEvent, homeAddress: String ,onComplete: (Boolean, Boolean) -> Unit) {
        val timeGap = secondEvent.startTime.toEpochSecond(ZoneOffset.UTC) - firstEvent.endTime.toEpochSecond(ZoneOffset.UTC)
        var homeAddress = "6190 Falla Dr, Canal Winchester, OH 43110"
        if (firstEvent.location == "") {
            firstEvent.location = homeAddress
        }
        if (secondEvent.location == "") {
            secondEvent.location = homeAddress
        }
        //Get the distance between the two events
        getDuration(firstEvent.location, secondEvent.location) { eventDistanceResponse ->
            if (eventDistanceResponse == null) {
                onComplete(false, false)
                return@getDuration
            }
            val eventDistance: Int = eventDistanceResponse.rows[0].elements[0].duration.value

            //Get the distance from the first event, to home, to the second event,
            MapsStopsUtils(LocalDateTime.now()).getTotalTime(firstEvent.location, secondEvent.location, homeAddress) { TimeFromHome ->
                if ((TimeFromHome > timeGap - 600) || (eventDistance > timeGap - 600)) {
                    onComplete(true, true)
                }
                else {
                    onComplete(true, false)
            }
        }

        }
    }
    fun geocode(address: String, onComplete: (Double, Double) -> Unit) {
        val url = "https://maps.googleapis.com/maps/api/geocode/json?address=$address&key=${mapsKey}"
        val request = okhttp3.Request.Builder()
            .url(url)
            .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onComplete(0.0,0.0)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string()
                if (response.code != 200) {
                    onComplete(0.0,0.0)
                    return
                }
                try {
                    val geocodeResponse = gson.fromJson(body, GeocodedResponse::class.java)
                    onComplete(geocodeResponse.results[0].geometry.location.lat, geocodeResponse.results[0].geometry.location.lng)
                } catch (e: Exception) {
                    onComplete(0.0,0.0)
                }
            }
        })
    }
    fun geocodeList(addresses: List<String>, onComplete: (List<HashMap<String, HashMap<Double, Double>>>) -> Unit) {
        val geocodedList = mutableListOf<HashMap<String, HashMap<Double, Double>>>()

        //use recursion to get one at a time
        fun geocodeOne(iteration: Int) {
            if (iteration == addresses.size) {
                onComplete(geocodedList)
                return
            }
            geocode(addresses[iteration]) { lat, lng ->
                val geocodedAddress = HashMap<String, HashMap<Double, Double>>()
                val location = HashMap<Double, Double>()
                location[lat] = lng
                geocodedAddress[addresses[iteration]] = location
                geocodedList.add(geocodedAddress)
                geocodeOne(iteration + 1)
            }
        }
        //Init
        geocodeOne(0)
    }
}