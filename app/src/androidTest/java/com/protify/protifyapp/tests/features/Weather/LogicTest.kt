package com.protify.protifyapp.tests.features.Weather

import com.protify.protifyapp.utils.WeatherUtils
import org.junit.Test
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch

class LogicTest {
    @Test
    fun `getNonRainingTimes is Valid`() {

        // init countDownLatch
        val countDownLatch = CountDownLatch(1)

        // Create a WeatherUtils object
        val weatherUtils = WeatherUtils(39.8707369, -82.8281)
        // Call the getRainForecast function
        weatherUtils.getNonRainingTimes(LocalDateTime.now()) {nonRainingTimes ->
            // Make sure that the list includes every hour for one week
            if (nonRainingTimes.size < 13) {
                for (time in nonRainingTimes) {
                    // Make sure the date is for today
                    if (time.first.dayOfMonth != LocalDateTime.now().dayOfMonth && time.second.dayOfMonth != LocalDateTime.now().dayOfMonth) {
                        assert(false) {"The date is not for today"}
                    }
                }
                // Signal that the callback has been called
                countDownLatch.countDown()
            } else {
                // fail
                assert(false) {"Result is impossibly large"}
            }

        }
        // Wait for the callback to be called. We wait for 15 seconds to avoid an infinite loop
        countDownLatch.await(15, java.util.concurrent.TimeUnit.SECONDS)
        // Check that the countdown latch is 0
        assert(countDownLatch.count == 0L)

    }
    @Test
    fun `getNonRainingTimes can fail safely`() {
        // Create a WeatherUtils object
        val weatherUtils = WeatherUtils(0.0, 0.0)
        // Create a CountDownLatch object (essential for testing asynchronous code)
        val countDownLatch = CountDownLatch(1)
        // Call the getNonRainingTimes function
        weatherUtils.getNonRainingTimes(LocalDateTime.now()){ nonRainingTimes ->
            if (nonRainingTimes.isNotEmpty()) {
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