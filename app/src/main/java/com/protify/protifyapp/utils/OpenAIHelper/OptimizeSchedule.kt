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

/** Make sure an instance of this class is only used for one day. If you want to optimize a schedule for a different day, then make a new instance of this class.
 * @param day: The day of the month
 * @param month: The month of the year
 * @param year: The year
 * @param events: A list of FirestoreEvents
 * @param travelTime: A list of DrivingTime objects
 * @param homeAddress: The home address of the user
 * @param optimalEventOrder: The order of the events that the user wants to optimize the schedule to
 */
class OptimizeSchedule(day: String, month: String, year: String, events: List<FirestoreEvent>, travelTime: MutableList<DrivingTime?>, homeAddress: String, optimalEventOrder: List<FirestoreEvent>) {
    //Get openAI key
    val apiKey = APIKeys().getOpenAIKey()

    //initialize the events, travelTime, optimalEventOrder, and homeAddress
    private val events = events
    private val travelTime = travelTime
    private val homeAddress = homeAddress
    private val optimalEventOrder = optimalEventOrder

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

    //Get a list of the events that are not allowed to be rescheduled by the AI
    private val dontRescheduleEvents = events.filter { event -> event.isOptimized }

    //If an event has isOptimized == true, then do not add it to the optimalEventOrder
    private val allowedOptimalEventOrder = optimalEventOrder.filter { event -> !event.isOptimized }

    //Turn optimalEventOrder into a string
    private val optimalEventOrderString = optimalEventOrder.mapIndexed { index, event -> "${index + 1}: ${event.name}" }.joinToString(", ")

    //Turn allowedOptimalEventOrder into a string
    private val allowedOptimalEventOrderString = allowedOptimalEventOrder.mapIndexed { index, event -> "${index + 1}: ${event.name}" }.joinToString(", ")

    private val eventList = events.sortedBy { it.startTime } // Sort by startTime in ascending order
        .map { event ->
            "${event.name} goes from ${event?.startTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))} to ${event?.endTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))} at ${if (event?.location == "") homeAddress else event?.location} " +
                    if (event.isOutside) "and is outdoors." else "." // If the event is outside, then add "and is outdoors."
        }

    private val eventString = eventList.joinToString(" ")
    //Get the travel time, origin, and destination from the travelTime list
    private val travelTimeList = travelTime.map { travel -> "The distance between ${travel?.startLocation} and ${travel?.endLocation} is ${travel?.duration} " }
    private val travelTimeString = travelTimeList.joinToString(" ")

    private var userContent = "Here are my events: $eventString. Here are the times it takes to get to each location: $travelTimeString"


    /** This function is executed when it is not raining that day or there are no events that are outside.
     * @param onComplete: Returns the response from the AI
     */
    @Deprecated("This function is deprecated. Use the function with the nonRainingTimes parameter instead.")
    fun getResponse(onComplete: (String?) -> Unit) {

        if (isFullyOptimized()) {
            onComplete("FullyOptimized")
            return
        }

        var userContent = "Here are my events: $eventString. Here are the times it takes to get to each location: $travelTimeString"

        val systemContent = "Attempt to change the start times and end times of my events so they are in this order: ${optimalEventOrderString} " +
                "You can do this by changing the startTime and endTime of the events. " +
                 if (dontRescheduleEvents.isNotEmpty()) { "You may not change the start or end time of the following events: ${dontRescheduleEvents.joinToString(", ") { it.name }} " } else { "" } +
                "You will provide the optimized schedule in json format. One of the objects is to be named OptimizedEvents. " +
                "In OptimizedEvents, you will have a field called Name, StartTime, EndTime, and Location. " +
                "Another object should be called OldEvents, which will be identically formatted to Events, but will contain the original schedule. "

        //Make a new request object
        val httpPost = Request(model,
            response_format,
            systemContent,
            "${userContent}")

        makeRequest(httpPost, onComplete)
    }

    /** This function is only when when both it is raining outside and there are outdoor events for the day.
     * @param nonRainingTimes: A list of non-raining times for the day
     * @param onComplete: Returns the response from the AI
     */
    fun getResponse(nonRainingTimes: List<Pair<LocalDateTime, LocalDateTime>>, onComplete: (String?) -> Unit) {
        if (isFullyOptimized()) {
            onComplete("FullyOptimized")
            return
        }

        // Init nonRainingTimes bool
        var hasRainingTimes = true
        // Check if the nonRainingTimes list is 0:00-00:00
        if (nonRainingTimes.size == 1 && nonRainingTimes[0].first.hour == 0 && nonRainingTimes[0].second.hour == 0) {
            hasRainingTimes = false
        }

        //Turn the list of non-raining times into a string
        val nonRainingTimesString = nonRainingTimes.joinToString(" ") { (start, end) -> "${start.format(DateTimeFormatter.ofPattern("HH:mm"))} and ${end.format(DateTimeFormatter.ofPattern("HH:mm"))} " }



        val systemContent = (if (hasRainingTimes) "Prioritize scheduling events that are outdoors between ${nonRainingTimesString}. "  else "") +
                "Attempt to change the start times and end times of my events so they are in this order: ${optimalEventOrderString} " +
                "You can do this by changing the startTime and endTime of the events. " +
                //If there are events that aren't allowed to be rescheduled, list them here.
                if (dontRescheduleEvents.isNotEmpty()) { "You may not reschedule the following events: ${dontRescheduleEvents.joinToString(", ") { it.name }} " } else { "" } +
                "You will provide the optimized schedule in json format. One of the objects is to be named OptimizedEvents. " +
                "In OptimizedEvents, you will have a field called Name, StartTime, EndTime, and Location. " +
                "Another object should be called OldEvents, which will be identically formatted to Events, but will contain the original schedule. "

        //Make a new request object
        val httpPost = Request(model,
            response_format,
            systemContent,
            "${userContent}")

        makeRequest(httpPost, onComplete)
    }
    /** This function is only ran when the user has one or more event's that aren't allowed to be rescheduled.
     * This function parses the response from the AI into the OptimizedSchedule object and calls the callback.
     * @param onComplete: The callback function that is called after the response is parsed
     */
    fun getResponseBlockedEvents(onComplete: (String?) -> Unit) {
        if (isFullyOptimized()) {
            onComplete("FullyOptimized")
            return
        }

        val systemContent = "Attempt to change the start times and end times of my events so they are in this order: ${allowedOptimalEventOrderString} " +
                "You can do this by changing the startTime and endTime of the events. " +
                if (dontRescheduleEvents.isNotEmpty()) { "You may not change the start or end time of the following events: ${dontRescheduleEvents.joinToString(", ") { it.name }} " } else { "" } +
                "You will provide the optimized schedule in json format. One of the objects is to be named OptimizedEvents. " +
                "In OptimizedEvents, you will have a field called Name, StartTime, EndTime, and Location. " +
                "Another object should be called OldEvents, which will be identically formatted to Events, but will contain the original schedule. "

        //Make a new request object
        val httpPost = Request(model,
            response_format,
            systemContent,
            "${userContent}")

        makeRequest(httpPost, onComplete)
    }

    fun getResponseOverlappingEvents(onComplete: (String?) -> Unit) {
        if (isFullyOptimized()) {
            onComplete("FullyOptimized")
            return
        }
        var userContentOverride: String = "Here are my events: $eventString."
        val systemContentOverride = "Remove any events that have overlapping times" +
                "You can do this by changing the startTime and endTime of the events. " +
                if (dontRescheduleEvents.isNotEmpty()) { "You may not change the start or end time of the following events: ${dontRescheduleEvents.joinToString(", ") { it.name }} " } else { "" } +
                "You will provide the optimized schedule in json format. One of the objects is to be named OptimizedEvents. " +
                "In OptimizedEvents, you will have a field called Name, StartTime, EndTime, and Location. " +
                "Another object should be called OldEvents, which will be identically formatted to Events, but will contain the original schedule. "

        val httpPost = Request(model,
            response_format,
            systemContentOverride,
            "${userContentOverride}")

        makeRequest(httpPost, onComplete)

    }

    /** This function is only ran when either it is not raining outside or there are no outdoor events for the day.
     * This function parses the response from the AI into the OptimizedSchedule object and calls the callback.
     * @param callback: The callback function that is called after the response is parsed
     */
    @Deprecated("This function is deprecated. Use the function with the nonRainingTimes parameter instead.")
    fun parseResponse(callback: (OptimizedSchedule) -> Unit) {
        val parse = GsonBuilder().create()
        //After 5 attempts, give up
        val maxRetries = 5

        fun retry(retries: Int) {
            if (retries > maxRetries) {
                callback(OptimizedSchedule(emptyList(), emptyList()))
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

    /** This function is only ran when either it is raining outside for the day and there are outdoor events for the day.
     * This function parses the response from the AI into the OptimizedSchedule object and calls the callback.
     * @param nonRainingTimes: A list of non-raining times for the day
     * @param callback: The callback function that is called after the response is parsed
     */
    @Deprecated ("This function does not do any error checking and is meant for unit testing only.")
    fun parseResponse(nonRainingTimes: List<Pair<LocalDateTime, LocalDateTime>>, callback: (OptimizedSchedule) -> Unit) {
        val parse = GsonBuilder().create()
        //After 5 attempts, give up
        val maxRetries = 5

        fun retry(retries: Int) {
            if (retries > maxRetries) {
                callback(OptimizedSchedule(emptyList(), emptyList()))
                return
            }
            getResponse(nonRainingTimes) { response ->
                if (response == "Error") {
                    retry(retries + 1)
                }
                //If the response is not an error, then parse the response
                else {
                    val optimizedSchedule = parse.fromJson(response, OptimizedSchedule::class.java)
                    //If the schedule is not null and the optimizedSchedule is different from the original schedule, then call the callback, else retry
                    if (optimizedSchedule.nullCheck() && optimizedSchedule.events != optimizedSchedule.oldEvents) {
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

    fun makeCall(nonRainingTimes: List<Pair<LocalDateTime, LocalDateTime>>, callback: (OptimizedSchedule) -> Unit) {

        // init hasOptimizedEvents bool
        var hasOptimizedEvents = false
        // Check if there are any events that are not allowed to be rescheduled
        if(events.any { it.isOptimized }) {
            hasOptimizedEvents = true
        }
        // Init nonRainingTimes bool
        var hasRainingTimes = true
        // Check if the nonRainingTimes list is 0:00-00:00
        if (nonRainingTimes.size == 1 && nonRainingTimes[0].first.hour == 0 && nonRainingTimes[0].second.hour == 0) {
            hasRainingTimes = false
        }

        val parse = GsonBuilder().create()

        // Try each call 3 times
        var maxRetries = 3

        fun secondCall(iterations: Int, hasRainingTime: Boolean) {
            if (iterations > maxRetries) {
                callback(OptimizedSchedule(emptyList(), emptyList()))
                return
            }
            getResponseBlockedEvents {
                if (it == "Error") {
                    secondCall(iterations + 1, hasRainingTime)
                } else {
                    var optimizedSchedule: OptimizedSchedule
                    try {
                        optimizedSchedule = parse.fromJson(it, OptimizedSchedule::class.java)
                    } catch (e: Exception) {
                        secondCall(iterations + 1, hasRainingTime)
                        return@getResponseBlockedEvents
                    }
                    if (optimizedSchedule.nullCheck() && qualityCheck(optimizedSchedule)) {
                        callback(optimizedSchedule)
                    } else {
                        secondCall(iterations + 1, hasRainingTime)
                    }
                }
            }
        }

        fun mainCall(iterations: Int, hasRainingTime: Boolean) {
            if (iterations > maxRetries) {
                maxRetries = 0
                secondCall(maxRetries, hasRainingTime)
                return
            }
            getResponse(nonRainingTimes) {
                if (it == "Error") {
                    mainCall(iterations + 1, hasRainingTime)
                } else {
                    var optimizedSchedule: OptimizedSchedule
                    try {
                        optimizedSchedule = parse.fromJson(it, OptimizedSchedule::class.java)
                    } catch (e: Exception) {
                        mainCall(iterations + 1, hasRainingTime)
                        return@getResponse
                    }
                    if (hasRainingTime) {
                        if (optimizedSchedule.nullCheck() && qualityCheck(optimizedSchedule, nonRainingTimes)) {
                            callback(optimizedSchedule)
                        } else {
                            mainCall(iterations + 1, hasRainingTime)
                        }
                    } else {
                        if (optimizedSchedule.nullCheck() && qualityCheck(optimizedSchedule)) {
                            callback(optimizedSchedule)
                        } else {
                            if (hasOptimizedEvents && iterations > 2) {
                                mainCall(iterations, hasRainingTime)
                            } else {
                                mainCall(iterations + 1, hasRainingTime)
                            }
                        }
                    }
                }
            }

        }

        mainCall(0, hasRainingTimes)

    }
    /** This function is to be used when the user has one or more events that are not allowed to be rescheduled.
     * This function parses the response from the AI into the OptimizedSchedule object and calls the callback.
     * @param callback: The callback function that is called after the response is parsed
     */
    fun parseResponseBlockedEvents(callback: (OptimizedSchedule) -> Unit) {
        val parse = GsonBuilder().create()
        //After 5 attempts, give up
        val maxRetries = 5

        fun retry(retries: Int) {
            if (retries > maxRetries) {
                callback(OptimizedSchedule(emptyList(), emptyList()))
                return
            }
            getResponseBlockedEvents { response ->
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
    /** This function is a helper function to check if the schedule is fully optimized before returning the object to the callback.
     * @return: Returns true if the schedule is fully optimized, else false
     */
    private fun isFullyOptimized(): Boolean {
        //If there are no events, then it's fully optimized
        if (events.isEmpty()) {
            return true
        }
        //If there are no events that are not allowed to be rescheduled, then it's fully optimized
        if (dontRescheduleEvents.size >= events.size) {
            return true
        }
        //If the optimalEventOrder is in the same order as the events by start time, then it's fully optimized
        if (events.sortedBy { it.startTime } == optimalEventOrder) {
            return true
        }
        return false
    }

    /** This function is a helper function to check if the schedule is fully optimized before returning the object to the callback.
     * @param optimizedSchedule: The optimized schedule that is to be checked
     * @return: Returns true if the schedule is fully optimized, else false
     */
    private fun qualityCheck(optimizedSchedule: OptimizedSchedule, nonRainingTimes: List<Pair<LocalDateTime, LocalDateTime>>? = null): Boolean {
        var passedQaulityCheck = true
        // Check that the number of events is the same
        if (optimizedSchedule.events.size != events.size) {
            passedQaulityCheck = false
        }
        // Check that the events all have the same name
        if (optimizedSchedule.events.map { it.name.lowercase() } != events.map { it.name.lowercase() }) {
            passedQaulityCheck = false
        }
        // Check that the old events and the new events don't all have the same start and end times
        if (optimizedSchedule.events.map { it.startTime } == optimizedSchedule.oldEvents.map { it.startTime } && optimizedSchedule.events.map { it.endTime } == optimizedSchedule.oldEvents.map { it.endTime }) {
            passedQaulityCheck = false
        }
        // If there are nonRainingTimes, then check that the events that have isOutside are scheduled during the non-raining times
        if (nonRainingTimes != null) {
            for (event in events) {
                val matchingEvent = optimizedSchedule.events.find { it.name == event.name }
                if (event.isOutside && matchingEvent != null) {
                    // Parse the start and end times of the matching event
                    val timePartsStart = matchingEvent.startTime.split(":")
                    val hourStringStart = timePartsStart[0]
                    val minuteStringStart = timePartsStart[1]
                    val timePartsEnd = matchingEvent.endTime.split(":")
                    val hourStringEnd = timePartsEnd[0]
                    val minuteStringEnd = timePartsEnd[1]
                    // Will return true if the event is scheduled during a non-raining time
                     passedQaulityCheck = nonRainingTimes.any {
                        val startHour = it.first.hour
                        val startMinute = it.first.minute
                        val endHour = it.second.hour
                        val endMinute = it.second.minute
                        val eventStartHour = hourStringStart.toInt()
                        val eventStartMinute = minuteStringStart.toInt()
                        val eventEndHour = hourStringEnd.toInt()
                        val eventEndMinute = minuteStringEnd.toInt()
                        return@any eventStartHour >= startHour && eventStartMinute >= startMinute && eventEndHour <= endHour && eventEndMinute <= endMinute
                    }


                }
                }
        }

        // Check that non-movable events are not moved
        for (event in dontRescheduleEvents) {
            val matchingEvent = optimizedSchedule.events.find { it.name == event.name }
            if (matchingEvent != null) {
                if (matchingEvent.startTime != event.startTime.format(DateTimeFormatter.ofPattern("HH:mm")) || matchingEvent.endTime != event.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))) {
                    passedQaulityCheck = false
                }
            }
        }

        return passedQaulityCheck
    }
    private fun makeRequest(httpPost: Request, onComplete: (String) -> Unit) {

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

}