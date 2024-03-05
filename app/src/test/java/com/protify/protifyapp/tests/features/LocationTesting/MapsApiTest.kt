package com.protify.protifyapp.tests.features.LocationTesting

import com.protify.protifyapp.features.GoogleMapsAPI.DrivingTime
import com.protify.protifyapp.utils.MapsDurationUtils
import org.junit.Test
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch

class MapsApiTest {

    // Test the getDrivingTimes function
    @Test
    fun testGetDrivingTimes() {
        //Home address
        val homeAddress = "762 Morning Dew Ln, Maineville, OH 45039, USA"
        // Create a list of events
        val locations = listOf(
            "9769 Montgomery Rd, Montgomery, OH 45242, USA",
            "6674 Winchester Blvd, Canal Winchester, OH 43110, USA",
            "6591 Winchester Blvd, Canal Winchester, OH 43110, USA"
        )
        // Create a MapsDurationUtils object
        val mapsDurationUtils = MapsDurationUtils(LocalDateTime.now())
        // Create a CountDownLatch object (essential for testing asynchronous code)
        val countDownLatch = CountDownLatch(1)
        // Create a variable to store the matrix result
        var maxtrixResult: List<DrivingTime?>? = null
        mapsDurationUtils.getDrivingTimes(homeAddress, locations) {matrix ->
            maxtrixResult = matrix
            // Signal that the callback has been called
            countDownLatch.countDown()
        }
        // Wait for the callback to be called. We wait for 15 seconds to avoid an infinite loop
        countDownLatch.await(15, java.util.concurrent.TimeUnit.SECONDS)

        // Check that the matrix result is not null (a failed result will return an empty list)
        assert(maxtrixResult!!.isNotEmpty())
    }

    @Test
    fun testGeocoding() {
        // List of addresses
        val addresses = listOf(
            "762 Morning Dew Ln, Maineville, OH 45039, USA",
            "6190 Falla Dr, Canal Winchester, OH 43110, USA",
            "9769 Montgomery Rd, Cincinnati, OH 45242, USA"
        )
        // Generate a random index
        val randomIndex = (addresses.indices).random()

        // Select a random address
        val randomAddress = addresses[randomIndex]

        // Create a MapsDurationUtils object
        val mapsDurationUtils = MapsDurationUtils(LocalDateTime.now())
        // Create a CountDownLatch object (essential for testing asynchronous code)
        val countDownLatch = CountDownLatch(1)
        // Create variables to store the geocoding result
        var lat: Double? = null
        var long: Double? = null
        mapsDurationUtils.geocode(randomAddress) { latitude, longitude ->
            lat = latitude
            long = longitude
            // Signal that the callback has been called
            countDownLatch.countDown()
        }
        // Wait for the callback to be called. We wait for 15 seconds to avoid an infinite loop
        countDownLatch.await(15, java.util.concurrent.TimeUnit.SECONDS)

        // Check that the latitude and longitude are not null
        assert(lat != 0.0)
        assert(long != 0.0)
    }


}