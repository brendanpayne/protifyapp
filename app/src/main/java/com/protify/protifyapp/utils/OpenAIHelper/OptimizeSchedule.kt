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
    private val model = "gpt-3.5-turbo-1106"
    private val response_format = """{ "type": "json_object" }"""

    private val client: OkHttpClient = OkHttpClient().newBuilder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val gson = GsonBuilder().create()

    private val request = okhttp3.Request.Builder()
        .url("https://api.openai.com/v1/chat/completions")
        .addHeader("Content-Type", "application/json")
        .addHeader("Authorization", "${apiKey}")
        .build()


    //Get only start time, end time, and location from FirestoreEvents. If location == "", then put homeAddress as location
    private val eventList = events.sortedBy { it.startTime } // Sort by startTime in ascending order
        .map { event -> "${event.name} goes from ${event?.startTime} to ${event?.endTime} at ${if (event?.location == "") homeAddress else event?.location}" }

    private val eventString = eventList.joinToString(" ")
    //Get the travel time, origin, and destination from the travelTime list
    private val travelTimeList = travelTime.map { travel -> "${travel?.startLocation} to ${travel?.endLocation} takes ${travel?.duration}" }
    private val travelTimeString = travelTimeList.joinToString(" ")

    private var userContent = "I need help optimizing my schedule for today. Here are my events: $eventString. Here are the times it takes to get to each location: $travelTimeString"


    fun getResponse(onComplete: (String?) -> Unit) {
        //Make a new request object
        val httpPost = Request(model,
            response_format,
            "You are a helpful assistant responsible for reducing the amount of time I have to drive in a day by optimizing my schedule. " +
                    "You are not allowed to change the length of the events, but are encouraged to rearrange events to save driving time between my events. " +
                    "Assume that when I don't have an event going on, I will drive back home to 6190 Falla Dr, Canal Winchester, OH 43110. " +
                    "You will provide the new schedule in json format. One of the objects is to be named OptimizedSchedule, where you list all of the events, their start times, " +
                    "their end times, their name, and their location. Another object should be called TimeSaved, which you will state how many minutes of driving " +
                    "was saved for that day." +
                    "The last object should be called OldSchedule, where you list all of the events, their start times, their end times, their name, and their location " +
                    "prior to the optimization. Please order them by start time.",
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