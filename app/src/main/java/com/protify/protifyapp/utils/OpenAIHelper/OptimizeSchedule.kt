package com.protify.protifyapp.utils.OpenAIHelper

import OpenAIResponse
import OptimizedSchedule
import com.google.gson.GsonBuilder
import com.protify.protifyapp.APIKeys
import com.protify.protifyapp.FirestoreEvent
import com.protify.protifyapp.features.GoogleMapsAPI.DrivingTime
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class OptimizeSchedule(day: String, month: String, year: String, events: List<FirestoreEvent>, travelTime: MutableList<DrivingTime?>, homeAddress: String) {
    //Get openAI key
    val apiKey = APIKeys().getOpenAIKey()

    //initialize the events, travelTime, and homeAddress
    private val events = events
    private val travelTime = travelTime
    private val homeAddress = homeAddress

    //Request struct
    data class Request(
        val model: String,
        val response_format: String,
        var systemContent: String,
        var userContent: String
    )
    private val model = "gpt-3.5-turbo-0125"
    //GPT 4 goated. GPT 3.5 is not reorganizing the shcedule properly, but GPT 4 is
    //private val model = "gpt-4-0125-preview"
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





    private fun getResponse(onComplete: (String?) -> Unit) {

        //Get only start time, end time, and location from FirestoreEvents. If location == "", then put homeAddress as location
        val eventList = events.sortedBy { it.startTime } // Sort by startTime in ascending order
            .map { event -> "${event.name} goes from ${event?.startTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))} to ${event?.endTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))} at ${if (event?.location == "") homeAddress else event?.location}" }

        val eventString = eventList.joinToString(" ")
        //Get the travel time, origin, and destination from the travelTime list
        val travelTimeList = travelTime.map { travel -> "The distance between ${travel?.startLocation} and ${travel?.endLocation} is ${travel?.duration}" }
        val travelTimeString = travelTimeList.joinToString(" ")

        var userContent = "I need help optimizing my schedule for today. Here are my events: $eventString. Here are the times it takes to get to each location: $travelTimeString"

        //Make a new request object
        val httpPost = Request(model,
            response_format,
            "You are responsible for saving me the time it takes to drive to these events by changing the time of the events. " +
                    "You will provide the optimized schedule in json format. One of the objects is to be named Events. " +
                    "In Events, you will have a field called Name, StartTime, EndTime, and Location. " +
                    "Another object should be called TimeSaved, which you will state how many minutes of driving in the form of an integer, in minutes",
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
                    onComplete(openAIResponse.choices[0].message.content)
                }
                else {
                    onComplete("Error")
                }
            }
        })
    }

    //Get response with weather data
    private fun getResponse(nonRainingTimes: List<Pair<LocalDateTime, LocalDateTime>>, onComplete: (String?) -> Unit) {

        //Turn the list of non-raining times into a string
        val nonRainingTimesString = nonRainingTimes.joinToString(" ") { (start, end) -> "${start.format(DateTimeFormatter.ofPattern("HH:mm"))} to ${end.format(DateTimeFormatter.ofPattern("HH:mm"))} " }

        //Get only start time, end time, and location from FirestoreEvents. If location == "", then put homeAddress as location
        val eventList = events.sortedBy { it.startTime } // Sort by startTime in ascending order
            .map { event ->
                "${event.name} goes from ${event?.startTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))} to ${event?.endTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))} at ${if (event?.location == "") homeAddress else event?.location} " +
                        if (event.isOutside) "and is outdoors." else "."
            }

        val eventString = eventList.joinToString(" ")
        //Get the travel time, origin, and destination from the travelTime list
        val travelTimeList = travelTime.map { travel -> "The distance between ${travel?.startLocation} and ${travel?.endLocation} is ${travel?.duration} " }
        val travelTimeString = travelTimeList.joinToString(" ")

        var userContent = "I need help optimizing my schedule for today. Here are my events: $eventString. Here are the times it takes to get to each location: $travelTimeString"

        //Make a new request object
        val httpPost = Request(model,
            response_format,
            "You are responsible for saving me the time it takes to drive to these events by changing the time of the events. " +
                    "If an event says it's outdoors, you may only schedule it within the following times: $nonRainingTimesString" +
                    "You will provide the optimized schedule in json format. One of the objects is to be named Events. " +
                    "In Events, you will have a field called Name, StartTime, EndTime, and Location. " +
                    "Another object should be called TimeSaved, which you will state how many minutes of driving in the form of an integer, in minutes",
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
                    onComplete(openAIResponse.choices[0].message.content)
                }
                else {
                    onComplete("Error")
                }
            }
        })
    }
    //Turn the response into OptimizedSchedule object (great naming scheme I know)
    fun parseResponse(callback: (OptimizedSchedule) -> Unit) {
        val parse = GsonBuilder().create()
        //After 5 attempts, give up
        val maxRetries = 5

        fun retry(retries: Int) {
            if (retries > maxRetries) {
                callback(OptimizedSchedule(emptyList(), -1))
                return
            }
            getResponse { response ->
                if (response == "Error") {
                    retry(retries + 1)
                }
                //If the response is not an error, then parse the response
                else {
                    val optimizedSchedule = parse.fromJson(response, OptimizedSchedule::class.java)
                    //If the schedule is not null, then call the callback, else retry
                    if (optimizedSchedule.nullCheck()) {
                        callback(optimizedSchedule)
                    }
                    else {
                        retry(retries + 1)
                    }
                }
            }
        }
        //It's called we do a little recursion
        retry(0)
    }
    //This is the same as the above function, but with the addition of the nonRainingTimes parameter
    fun parseResponse(nonRainingTimes: List<Pair<LocalDateTime, LocalDateTime>>, callback: (OptimizedSchedule) -> Unit) {
        val parse = GsonBuilder().create()
        //After 5 attempts, give up
        val maxRetries = 5

        fun retry(retries: Int) {
            if (retries > maxRetries) {
                callback(OptimizedSchedule(emptyList(), -1))
                return
            }
            getResponse(nonRainingTimes) { response ->
                if (response == "Error") {
                    retry(retries + 1)
                }
                //If the response is not an error, then parse the response
                else {
                    val optimizedSchedule = parse.fromJson(response, OptimizedSchedule::class.java)
                    //If the schedule is not null, then call the callback, else retry
                    if (optimizedSchedule.nullCheck()) {
                        callback(optimizedSchedule)
                    }
                    else {
                        retry(retries + 1)
                    }
                }
            }
        }
        //It's called we do a little recursion
        retry(0)
    }
    fun updateFirebase() {
        TODO()
    }
}