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

    /** This function is nearly identical to the getResponse function, except it doesn't tell the AI which order to put the events in
     * It also does not care about the physical distance between each location. This is more concerned with getting events that are outdoors
     * into non-raining times and ensuring non-movable events are not moved
     * @param nonRainingTimes list of non-raining times for the day
     * @param onComplete Returns the AI response
     */
    fun getResponseNoLocationData(nonRainingTimes: List<Pair<LocalDateTime, LocalDateTime>>, onComplete: (String?) -> Unit) {
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

        val userContentOverride = "Here is a list of the events I have today: $eventString"

        val systemContentOverride =   (if (hasRainingTimes) "Prioritize scheduling events that are outdoors between ${nonRainingTimesString}. "  else "") +
            if (dontRescheduleEvents.isNotEmpty()) { "You may not change the start or end time of the following events: ${dontRescheduleEvents.joinToString(", ") { it.name }} " } else { "" } +
                "You will provide the optimized schedule in json format. One of the objects is to be named OptimizedEvents. " +
                "In OptimizedEvents, you will have a field called Name, StartTime, EndTime, and Location. " +
                "Another object should be called OldEvents, which will be identically formatted to Events, but will contain the original schedule. "

        // Build the request object
        val httpPost = Request(model,
            response_format,
            systemContentOverride,
            "${userContentOverride}")

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

    /** This function is designed to be the main entry point into the AI call. It will start with a very strict prompt that gives it
     * a lot of information that it needs to parse through. If quality cannot be ensured, it will resort to using less factors in the prompt so it can
     * zero in on what the user needs from the AI
     * @param nonRainingTimes Needed whether or not it is raining outside
     * @param callback returns the schedule that will be handed off to firestore. In the event of an error, two empty lists will be passed to the callback
     */
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

        // init parser
        val parse = GsonBuilder().create()

        // init has overlapping events
        var hasOverlappingEvents: Boolean = false

        // Check for overlapping events
        if (events.any { event -> events.any { it != event && it.startTime.isBefore(event.endTime) && it.endTime.isAfter(event.startTime) } }) {
            hasOverlappingEvents = true
        }

        // Try each call 3 times
        var maxRetries = 3
        // Make a call to the least struct mf
        fun thirdCall(iterations: Int, hasRainingTime: Boolean) {
         if (iterations > maxRetries) {
             callback(OptimizedSchedule(emptyList(), emptyList()))
             return
         }
            getResponseNoLocationData(nonRainingTimes) {
                if (it == "Error") {
                    thirdCall(iterations + 1, hasRainingTime)
                } else {
                    // Create optimized schedule object and parse output into object
                    var optimizedSchedule: OptimizedSchedule
                    try {
                        optimizedSchedule = parse.fromJson(it, OptimizedSchedule::class.java)
                    } catch (e: Exception) {
                        thirdCall(iterations + 1, hasRainingTime)
                        return@getResponseNoLocationData
                    }
                    // At this point, we're down bad so we're not going to quality check it. If we got output, we giving it to the user
                    if (optimizedSchedule.nullCheck()) {
                        callback(optimizedSchedule)
                    } else {
                        thirdCall(iterations + 1, hasRainingTime)
                    }
                }
            }
        }

        /** Second call is a less comprehensive call that will be made to the AI. It will still go through the same
         * quality check as the main call, but will have less variables to work with.
         * @param iterations: The number of times the function has been called
         * @param hasRainingTime: A boolean that is true if there are non-raining times for the day
         */
        fun secondCall(iterations: Int, hasRainingTime: Boolean) {
            if (iterations > maxRetries) {
                callback(OptimizedSchedule(emptyList(), emptyList()))
                //thirdCall(0, hasRainingTime)
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

        /** Main call is the first call that will be made to the AI. This response is the most compresensive and will
         * use all of the variables given
         * @param iterations: The number of times the function has been called
         * @param hasRainingTime: A boolean that is true if there are non-raining times for the day
         */
        fun mainCall(iterations: Int, hasRainingTime: Boolean) {
            if (iterations > maxRetries) {
                secondCall(0, hasRainingTime)
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
                    // If it has non rainging times, make sure to check for that
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
        // It's called we do a little recursion
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

    /** This function is used to output a new list of firestore events so you can call other
     * instance of this class with the new output. Once overlapping events
     * are removed, we should have a better chance of the AI giving accurate responses
     * @param callback: Returns a new list of firestore events that have been optimized
     */
    fun removeOverlappingEvents(callback: (List<FirestoreEvent>) -> Unit) {
        getResponseOverlappingEvents { response ->
            val parse = GsonBuilder().create()
            val optimizedSchedule = parse.fromJson(response, OptimizedSchedule::class.java)
            if (optimizedSchedule.nullCheck()) {
                // map the start time and end times of the response.events to the events list
                val newEvents: List<FirestoreEvent> = events.map { event ->
                    val matchingEvent = optimizedSchedule.events.find { it.name == event.name }
                    if (matchingEvent != null) {
                        val timePartsStart = matchingEvent.startTime.split(":")
                        val hourStringStart = timePartsStart[0]
                        val minuteStringStart = timePartsStart[1]
                        val timePartsEnd = matchingEvent.endTime.split(":")
                        val hourStringEnd = timePartsEnd[0]
                        val minuteStringEnd = timePartsEnd[1]
                        // build start time and end time
                        val startTime = LocalDateTime.of(
                            event.startTime.year,
                            event.startTime.month,
                            event.startTime.dayOfMonth,
                            hourStringStart.toInt(),
                            minuteStringStart.toInt()
                        )
                        val endTime = LocalDateTime.of(
                            event.endTime.year,
                            event.endTime.month,
                            event.endTime.dayOfMonth,
                            hourStringEnd.toInt(),
                            minuteStringEnd.toInt()
                        )

                        // Make a new firestore event except with new start times and end times
                        FirestoreEvent(
                            event.name,
                            event.nameLower,
                            startTime,
                            endTime,
                            event.location,
                            event.description,
                            event.timeZone,
                            event.importance,
                            event.attendees,
                            event.rainCheck,
                            event.isRaining,
                            event.mapsCheck,
                            event.distance,
                            event.isOutside,
                            event.isOptimized,
                            event.isAiSuggestion,
                            event.isUserAccepted
                        )
                    } else {
                        event
                    }

                }
                callback(newEvents)
            } else {
                callback(emptyList())
            }

        }
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

    /** This formats the API request and makes a post to GPT API
     * @param httpPost Request object
     * @param onComplete Callback returns response in the form of a string, and if there was a failure, will return "Error"
     */
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