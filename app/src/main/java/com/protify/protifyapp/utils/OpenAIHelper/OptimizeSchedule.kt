package com.protify.protifyapp.utils.OpenAIHelper

import OpenAIResponse
import com.google.gson.GsonBuilder
import com.protify.protifyapp.APIKeys
import com.protify.protifyapp.FirestoreEvent
import com.protify.protifyapp.features.GoogleMapsAPI.DrivingTime
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

class OptimizeSchedule(day: String, month: String, year: String, events: List<FirestoreEvent>, travelTime: MutableList<DrivingTime?>, homeAddress: String) {
    //Get openAI key
    val apiKey = APIKeys().getOpenAIKey()

    //Request struct
    data class Request(
        val model: String,
        val response_format: String,
        var systemContent: String,
        var userContent: String
    )
    //Model type and response format
    private val model = "gpt-3.5-turbo-1106"
    private val response_format = """{ "type": "json_object" }"""
    //Create a new okhttp client
    private val client: OkHttpClient = OkHttpClient().newBuilder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    //Create a new gson object
    private val gson = GsonBuilder().create()
    //Create a new request object
    private val request = okhttp3.Request.Builder()
        .url("https://api.openai.com/v1/chat/completions")
        .addHeader("Content-Type", "application/json")
        .addHeader("Authorization", "${apiKey}")
        .build()
    //Make a new CompressData object
    private val compressData = CompressData()
    //Create a map to store the location and its number
    private val locationToNumberMap = compressData.listLocationsToNumbers(events.map { event -> event.location!! })
    //Create a map to store the event and its number
    private val eventToNumberMap = compressData.listEventsToNumbers(events)

    //Get only start time, end time, and location from FirestoreEvents. If location == "", then put homeAddress as location
    private val eventList = events.map { event -> "Event ${compressData.eventToNumber(event)} goes from ${event?.startTime} to ${event?.endTime} at location ${compressData.locationToNumber( event?.location!! )}" }
    private val eventString = eventList.joinToString(" ")
    //Get the travel time, origin, and destination from the travelTime list
    private val travelTimeList = travelTime.map { travel -> "Location ${compressData.locationToNumber(travel?.startLocation!!)} to location ${compressData.locationToNumber(travel.endLocation)} takes ${travel?.duration}" }
    private val travelTimeString = travelTimeList.joinToString(" ")

    private var userContent = "I need help optimizing my schedule for today. Here are my events: $eventString. Here are my travel times: $travelTimeString"


    fun getResponse(onComplete: (String?) -> Unit) {
        //Make a new request object
        val httpPost = Request(model,
            response_format,
            "You are a helpful assistant responsible for reducing the amount of time I have to drive in a day by optimizing my schedule. You will provide the new schedule in json format.",
            "${userContent}")


        //Request body builder
        val requestBody = request.newBuilder()
            .post(
                """{
    "model": "${httpPost.model}",
    "response_format": ${httpPost.response_format},
    "messages": [
      {
        "role": "system",
        "content": "${httpPost.systemContent}"
      },
      {
        "role": "user",
        "content": "${httpPost.userContent}"
      }
    ]
  }""".trimIndent().toRequestBody()
            )
            .build()

        //Make the request
        client.newCall(requestBody).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                onComplete("Error")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string()
                if (response.code == 200) {
                    val openAIResponse = gson.fromJson(body, OpenAIResponse::class.java)
                    val test = openAIResponse
                    onComplete(openAIResponse.choices[0].message.content)
                }
                else {
                    onComplete("Error")
                }
            }
        })
    }

}