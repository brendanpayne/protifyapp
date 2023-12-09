package com.protify.protifyapp.utils

import com.google.gson.GsonBuilder
import com.protify.protifyapp.features.weather.WeatherForecast
import com.protify.protifyapp.features.weather.WeatherOverview
import okhttp3.OkHttpClient
import java.io.IOException

class WeatherUtils(val longitude: Double, val latitude: Double) {

    private val url: String = "https://api.weather.gov/points/${latitude},${longitude}"
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
                .url(overview.properties.forecast)
                .build()
            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    onComplete(null)
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    val body = response.body?.string()
                    val forecast = gson.fromJson(body, WeatherForecast::class.java)
                    onComplete(forecast)
                }
            })
        }
    }
}
