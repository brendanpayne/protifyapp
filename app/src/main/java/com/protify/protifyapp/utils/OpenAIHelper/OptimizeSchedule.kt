package com.protify.protifyapp.utils.OpenAIHelper

import OpenAIResponse
import OptimizedSchedule
import com.google.gson.GsonBuilder
import com.protify.protifyapp.APIKeys
import com.protify.protifyapp.FirestoreEvent
import com.protify.protifyapp.features.GoogleMapsAPI.DrivingTime
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.format.DateTimeFormatter

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
        .map { event -> "${event.name} goes from ${event?.startTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))} to ${event?.endTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))} at ${if (event?.location == "") homeAddress else event?.location}" }

    private val eventString = eventList.joinToString(" ")
    //Get the travel time, origin, and destination from the travelTime list
    private val travelTimeList = travelTime.map { travel -> "${travel?.startLocation} to ${travel?.endLocation} takes ${travel?.duration}" }
    private val travelTimeString = travelTimeList.joinToString(" ")

    private var userContent = "I need help optimizing my schedule for today. Here are my events: $eventString. Here are the times it takes to get to each location: $travelTimeString"
    //Initialize the home address
    private val homeAddress = homeAddress

    private fun getResponse(onComplete: (String?) -> Unit) {
        //Make a new request object
        val httpPost = Request(model,
            response_format,
            "You are a helpful assistant responsible for reducing the amount of time I have to drive in a day by optimizing my schedule. " +
                    "You are not allowed to change the length of the events, but are encouraged to rearrange events to save driving time between my events. " +
                    "Assume that when I don't have an event going on, I will drive back home to ${homeAddress}. " +
                    "You will provide the new schedule in json format. One of the objects is to be named Events. " +
                    "In Events, you will have a field Called Name, StartTime, EndTime, and Location. " +
                    "Another object should be called TimeSaved, which you will state how many minutes of driving in the form of an integer, in minutes. " +
                    "If you are not able to optimize the schedule, please set all TimeSaved to -1.",
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
    fun updateFirebase() {
        TODO()
    }
}