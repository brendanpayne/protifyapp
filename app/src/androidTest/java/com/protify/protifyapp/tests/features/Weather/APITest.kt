package com.protify.protifyapp.tests.features.WeatherTesting

import com.protify.protifyapp.utils.WeatherUtils
import org.junit.Test
import java.util.concurrent.CountDownLatch

class APITest {
    // Test the getWeather function
    @Test
    fun `The response is valid`() {
        // Create a WeatherUtils object
        val weatherUtils = WeatherUtils(39.8707369, -82.8281)
        // Create a CountDownLatch object (essential for testing asynchronous code)
        val countDownLatch = CountDownLatch(1)
        // Call the getWeather function
        weatherUtils.getRainForecast{ weather ->
            if (weather != null) {
                if (weather.isNotEmpty()) {
                    // Signal that the callback has been called
                    countDownLatch.countDown()
                } else {
                    // fail
                    assert(false) {"One or more fields is empty"}
                }

            } else {
                // fail
                assert(false) {"No output"}
            }
        }
        // Wait for the callback to be called. We wait for 15 seconds to avoid an infinite loop
        countDownLatch.await(15, java.util.concurrent.TimeUnit.SECONDS)
        // Check that the countdown latch is 0
        assert(countDownLatch.count == 0L)
    }
    @Test
    fun `The api call can fail safely`() {
        // Create a WeatherUtils object
        val weatherUtils = WeatherUtils(0.0, 0.0)
        // Create a CountDownLatch object (essential for testing asynchronous code)
        val countDownLatch = CountDownLatch(1)
        // Call the getWeather function
        weatherUtils.getRainForecast{ weather ->
            if (weather != null) {
                assert(false) {"The api call should fail"}

            } else {
                // Signal that the callback has been called
                countDownLatch.countDown()
            }
        }
        // Wait for the callback to be called. We wait for 15 seconds to avoid an infinite loop
        countDownLatch.await(15, java.util.concurrent.TimeUnit.SECONDS)
        // Check that the countdown latch is 0
        assert(countDownLatch.count == 0L)
    }
}