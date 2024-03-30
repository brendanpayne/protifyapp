package com.protify.protifyapp.utils

import com.google.gson.GsonBuilder
import com.protify.protifyapp.features.weather.RainForecast
import com.protify.protifyapp.features.weather.WeatherForecast
import com.protify.protifyapp.features.weather.WeatherOverview
import okhttp3.OkHttpClient
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class WeatherUtils(val longitude: Double, val latitude: Double) {

    private val url: String = "https://api.weather.gov/points/${longitude},${latitude}"
    private val client: OkHttpClient = OkHttpClient().newBuilder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val gson = GsonBuilder().create()

    private fun getGridValues(onComplete: (WeatherOverview?) -> Unit) {
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
                val WeatherOverview = gson.fromJson(body, WeatherOverview::class.java)
                onComplete(WeatherOverview)
            }
        })
    }
    fun getForecast(onComplete: (WeatherForecast?) -> Unit) {
        getGridValues { overview ->
            if (overview == null) {
                onComplete(null)
                return@getGridValues
            }
            val request = okhttp3.Request.Builder()
                .url(overview.properties.forecastHourly)
                .build()
            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    onComplete(null)
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    if (response.code != 200) {
                        onComplete(null)
                        return
                    }
                    val body = response.body?.string()
                    val forecast = gson.fromJson(body, WeatherForecast::class.java)
                    for (period in forecast.properties.periods) {
                        period.startTimeLocalDateTime = java.time.LocalDateTime.parse(period.startTime, java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        period.endTimeLocalDateTime = java.time.LocalDateTime.parse(period.endTime, java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    }
                    onComplete(forecast)
                }
            })
        }
    }

    fun getRainForecast(oncomplete: (List<RainForecast>?) -> Unit) {
        getForecast { forecast ->
            if (forecast == null) {
                oncomplete(null)
                return@getForecast
            }
            val rainForecast = forecast.properties.periods
                .map {
                    RainForecast(
                        it.startTimeLocalDateTime,
                        it.probabilityOfPrecipitation!!.value,
                        false,
                        ""
                    )
                }
            for (forecasts in rainForecast) {
                forecasts.stringTime = forecasts.time.format(DateTimeFormatter.ISO_DATE_TIME)
                if (forecasts.probability > 50) {
                    forecasts.isRaining = true
                }
            }
            val jsonRainForecast = gson.toJson(rainForecast)
            oncomplete(rainForecast)
        }
    }
    fun getRainForecastList(locationsList: HashMap<Double, Double>, oncomplete: (List<RainForecast>) -> Unit) {
        val rainForecast = mutableListOf<RainForecast>()
        val locationsList = locationsList.size
        fun getRainForecastOne(iteration: Int) {
            getRainForecast { forecast ->
                if (forecast == null) {
                    if (iteration < locationsList) {
                        getRainForecastOne(iteration + 1)
                    } else {
                        oncomplete(rainForecast)
                    }
                    return@getRainForecast
                }
            }
        }
        //Init
        getRainForecastOne(0)
    }

    fun rainToday(today: LocalDateTime, onComplete: (Boolean) -> Unit) {

        if (today.isAfter(LocalDateTime.now().plusDays(6))) {
            onComplete(false)
            return
        }
        getRainForecast { rainForecast ->
            if (rainForecast == null) {
                onComplete(false)
                return@getRainForecast
            }
            val isRaining = rainForecast.any { it.time.dayOfYear == today.dayOfYear && it.isRaining }
            onComplete(isRaining)
        }
    }

    /** This function only gets the non raining times on a day by dy basis, as implied by the time parameter
     * @return Returns empty list if there is no rain forecast and if it's not raining for the day, it will return 1 pair of midnight to midnight
     */
    fun getNonRainingTimes(time: LocalDateTime, onComplete: (List<Pair<LocalDateTime, LocalDateTime>>) -> Unit) {
        val today = time.withHour(0).withMinute(0).withSecond(0).withNano(0)
        getRainForecast { rainForecast ->
            if (rainForecast == null) {
                onComplete(listOf())
                return@getRainForecast
            }

            val dayForecast = rainForecast.filter { it.time.dayOfMonth == today.dayOfMonth }

            val nonRainingTimes = mutableListOf<Pair<LocalDateTime, LocalDateTime>>()
            var currentFreeTimeStart = today.withHour(0).withMinute(0).withSecond(0).withNano(0) // Start at midnight

            for (index in dayForecast.indices) {
                val forecast = dayForecast[index]
                val nextForecast = dayForecast.getOrNull(index + 1)

                if (!forecast.isRaining) {
                    if (currentFreeTimeStart > forecast.time) {
                        currentFreeTimeStart = forecast.time // Adjust start time if non-raining period began earlier
                    }

                    if (nextForecast == null || nextForecast.isRaining) {
                        // End of non-raining period
                        nonRainingTimes.add(Pair(
                            if (currentFreeTimeStart.minute == 1) {
                                currentFreeTimeStart.minusMinutes(1)
                            } else {
                                currentFreeTimeStart
                            }, forecast.time))
                        currentFreeTimeStart = forecast.time.plusMinutes(1) // Start one minute after the non-raining period
                    }
                } else {
                    currentFreeTimeStart = forecast.time.plusMinutes(1) // Start one minute after the rainy forecast
                }
            }

            // Handle last non-raining period if it extends to midnight
            val lastForecast = dayForecast.lastOrNull()
            if (lastForecast != null && !lastForecast.isRaining && currentFreeTimeStart < today.plusDays(1).withHour(0)) {
                //nonRainingTimes.add(Pair(currentFreeTimeStart, today.plusDays(1).withHour(0)))
                // Modify the existing pair to end at midnight
                nonRainingTimes[nonRainingTimes.size - 1] = Pair(nonRainingTimes.last().first, today.plusDays(1).withHour(0))
            }

            onComplete(nonRainingTimes)
        }
    }

}
