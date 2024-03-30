package com.protify.protifyapp.utils.OpenAIHelper

import OpenAIResponse
import OptimizedSchedule
import android.util.Log
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
    private val apiKey = APIKeys().getOpenAIKey()

    //initialize the events, travelTime, optimalEventOrder, and homeAddress
    private var events = events
    private val optimalEventOrder = optimalEventOrder
    private var homeAddress = homeAddress

    //Request struct
    data class Request(
        val model: String,
        val responseFormat: String,
        var systemContent: String,
        var userContent: String
    )
    private var model = "gpt-3.5-turbo" // Use 3.5 by default
    //GPT 4 goated. GPT 3.5 is not reorganizing the shcedule properly, but GPT 4 is
    //private val model = "gpt-4-0125-preview"
    private val responseFormat = """{ "type": "json_object" }"""

    private val client: OkHttpClient = OkHttpClient().newBuilder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS) // Had to change to 30 seconds because gpt 4 takes forever and a day
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val gson = GsonBuilder().create()

    private val request = okhttp3.Request.Builder()
        .url("https://api.openai.com/v1/chat/completions")
        .addHeader("Content-Type", "application/json")
        .addHeader("Authorization", apiKey)
        .build()

    //Get a list of the events that are not allowed to be rescheduled by the AI
    private val dontRescheduleEvents = events.filter { event -> event.isOptimized }

    //If an event has isOptimized == true, then do not add it to the optimalEventOrder
    private val allowedOptimalEventOrder = optimalEventOrder.filter { event -> !event.isOptimized }

    //Turn optimalEventOrder into a string
    private val optimalEventOrderString = optimalEventOrder.mapIndexed { index, event -> "${index + 1}: ${event.name}" }.joinToString(", ")

    //Turn allowedOptimalEventOrder into a string
    private val allowedOptimalEventOrderString = allowedOptimalEventOrder.mapIndexed { index, event -> "${index + 1}: ${event.name}" }.joinToString(", ")

    private var eventList = events.sortedBy { it.startTime } // Sort by startTime in ascending order
        .map { event ->
            "${event.name} goes from ${event.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} to ${event.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))} at ${if (event.location == "") homeAddress else event.location}. "

        }
    private var outdoorEventsString = events.filter { it.isOutside }.joinToString(", ") { it.name }

    private var eventString = eventList.joinToString(" ")
    //Get the travel time, origin, and destination from the travelTime list
    private val travelTimeList = travelTime.map { travel -> "The distance between ${travel?.startLocation} and ${travel?.endLocation} is ${travel?.duration} " }
    private val travelTimeString = travelTimeList.joinToString(" ")

    private var userContent = "Here are my events: $eventString. Here are the times it takes to get to each location: $travelTimeString"


    /** This function is only for unit testing to test baseline functionality of the AI
     */
    @Deprecated("This function is only for unit testing")
    fun getResponse(onComplete: (String?) -> Unit) {

        if (isFullyOptimized()) {
            onComplete("FullyOptimized")
            return
        }

        val userContent = "Here are my events: $eventString. Here are the times it takes to get to each location: $travelTimeString"

        val systemContent = "Attempt to change the start times and end times of my events so they are in this order: $optimalEventOrderString " +
                "You can do this by changing the startTime and endTime of the events. " +
                 if (dontRescheduleEvents.isNotEmpty()) { "You may not change the start or end time of the following events: ${dontRescheduleEvents.joinToString(", ") { it.name }} " } else { "" } +
                "You will provide the optimized schedule in json format. One of the objects is to be named OptimizedEvents. " +
                "In OptimizedEvents, you will have a field called Name, StartTime, EndTime, and Location. " +
                "Another object should be called OldEvents, which will be identically formatted to Events, but will contain the original schedule. "

        makeRequest(systemContent, userContent, onComplete)
    }

    /** This function is only when when both it is raining outside and there are outdoor events for the day.
     * @param nonRainingTimes: A list of non-raining times for the day
     * @param onComplete: Returns the response from the AI
     */
    fun getResponse(use4: Boolean = false, nonRainingTimes: List<Pair<LocalDateTime, LocalDateTime>>, onComplete: (String?) -> Unit) {
        if (isFullyOptimized()) {
            onComplete("FullyOptimized")
            return
        }

        // Init nonRainingTimes bool
        val hasRainingTimes = hasRainingTimes(nonRainingTimes)

        val systemContent: String
        if (use4) {
            systemContent = AIPrompts().prioritizeEventOrderPrompt(hasRainingTimes, nonRainingTimes.formatNonRainingTimesToString(), dontRescheduleEvents, outdoorEventsString)

        } else {
            systemContent = AIPrompts().comprehensivePromptWithOptimalEventOrder(hasRainingTimes, nonRainingTimes.formatNonRainingTimesToString(), optimalEventOrderString, dontRescheduleEvents, outdoorEventsString)
        }

        makeRequest(systemContent, userContent, onComplete)
    }
    /** This function is only ran when the user has one or more event's that aren't allowed to be rescheduled.
     * This function parses the response from the AI into the OptimizedSchedule object and calls the callback.
     * @param onComplete: The callback function that is called after the response is parsed
     */
    private fun getResponseBlockedEvents(onComplete: (String?) -> Unit) {
        if (isFullyOptimized()) {
            onComplete("FullyOptimized")
            return
        }

        val systemContent = AIPrompts().blockedEventsPrompt(allowedOptimalEventOrderString, dontRescheduleEvents)

        makeRequest(systemContent, userContent, onComplete)
    }

    /** This function is nearly identical to the getResponse function, except it doesn't tell the AI which order to put the events in
     * It also does not care about the physical distance between each location. This is more concerned with getting events that are outdoors
     * into non-raining times and ensuring non-movable events are not moved
     * @param nonRainingTimes list of non-raining times for the day
     * @param onComplete Returns the AI response
     */
    private fun getResponseNoLocationData(nonRainingTimes: List<Pair<LocalDateTime, LocalDateTime>>, onComplete: (String?) -> Unit) {
        if (isFullyOptimized()) {
            onComplete("FullyOptimized")
            return
        }

        // Init nonRainingTimes bool
        val hasRainingTimes = hasRainingTimes(nonRainingTimes)

        val userContentOverride = "Here is a list of the events I have today: $eventString"

        val systemContentOverride = AIPrompts().noLocationPrompt(dontRescheduleEvents)

        makeRequest(systemContentOverride, userContentOverride, onComplete)
    }

    private fun getResponseOverlappingEvents(onComplete: (String?) -> Unit) {
        if (isFullyOptimized()) {
            onComplete("FullyOptimized")
            return
        }
        val userContentOverride = "Here are my events: $eventString."
        val systemContentOverride = AIPrompts().overlappingEventsPrompt(dontRescheduleEvents)

        makeRequest(systemContentOverride, userContentOverride, onComplete)

    }

    /** This function should only be used for unit tests to make sure the output of the AI is parseable
     * @param callback: Returns an OptimizedSchedule object
     */
    @Deprecated("This function is only for unit testing")
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
            getResponse(false, nonRainingTimes) { response ->
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

    /** Function that is called when the user didn't set any event locations for the entire day
     *
     */
    fun makeCallNoLocation(use4: Boolean = false, nonRainingTimes: List<Pair<LocalDateTime, LocalDateTime>>, callback: (OptimizedSchedule) -> Unit) {
        if (use4) {
            model = "gpt-4-1106-preview" // This points to the latest (best?) model
        }
        // Init nonRainingTimes bool
        var hasRainingTimes = true
        // Check if the nonRainingTimes list is 0:00-00:00
        if (nonRainingTimes.size == 1 && nonRainingTimes[0].first.hour == 0 && nonRainingTimes[0].second.hour == 0 && events.any { it.isOutside }) {
            hasRainingTimes = false
        }

        // init parser
        val parse = GsonBuilder().create()

        // init has overlapping events
        var hasOverlappingEvents = false

        // Check for overlapping events
        if (events.any { event -> events.any { it != event && it.startTime.isBefore(event.endTime) && it.endTime.isAfter(event.startTime) } }) {
            hasOverlappingEvents = true
        }

        fun mainCall(iterations: Int, hasRainingTime: Boolean) {
            if (iterations > 3) {
                Log.i("OptimizeSchedule", "Main call failed after 3 attempts.")
                return
            }
            getResponseNoLocationData(nonRainingTimes) {
                if (it == "Error") {
                    mainCall(iterations + 1, hasRainingTime)
                } else {
                    val optimizedSchedule: OptimizedSchedule
                    try {
                        optimizedSchedule = parse.fromJson(it, OptimizedSchedule::class.java)
                    } catch (e: Exception) {
                        mainCall(iterations + 1, hasRainingTime)
                        return@getResponseNoLocationData
                    }
                    if (optimizedSchedule.nullCheck() && qualityCheck(optimizedSchedule)) {
                        callback(optimizedSchedule)
                    } else {
                        mainCall(iterations + 1, hasRainingTime)
                    }
                }
            }
        }

        // If there are overlapping events, then remove them before making the main call
        if (hasOverlappingEvents) {
            removeOverlappingEvents {
                if (it.isNotEmpty()) {
                    updateEvents(it) // Update the events list
                    mainCall(0, hasRainingTimes) // Make the main call
                }
            }
        } else {
            mainCall(0, hasRainingTimes) // Make the main call
        }
    }
    fun makeCallParallel(use4: Boolean = false, nonRainingTimes: List<Pair<LocalDateTime, LocalDateTime>>, callback: (OptimizedSchedule) -> Unit) {
        var isCallbackInvoked = false // This is to prevent the callback from being called multiple times
        var timesRan = 0
        if (use4) {
            model = "gpt-4-1106-preview" // This points to the latest (best?) model
        }
        // Init nonRainingTimes bool
        var hasRainingTimes = true
        // Check if the nonRainingTimes list is 0:00-00:00
        if (nonRainingTimes.size == 1 && nonRainingTimes[0].first.hour == 0 && nonRainingTimes[0].second.hour == 0 && events.any { it.isOutside }) {
            hasRainingTimes = false
        }

        // init parser
        val parse = GsonBuilder().create()

        // init has overlapping events
        var hasOverlappingEvents = false

        // Check for overlapping events
        if (events.any { event -> events.any { it != event && it.startTime.isBefore(event.endTime) && it.endTime.isAfter(event.startTime) } }) {
            hasOverlappingEvents = true
        }
        // Tries for a good response up to 3 times.
        val maxRetries = 3

        if (hasOverlappingEvents) {
            removeOverlappingEvents {
                if (it.isNotEmpty()) {
                    updateEvents(it) // Update the events list
                }
            }
        }
        fun thirdCall() {
            for (i in 0 until maxRetries) { // "Third call"
                getResponseNoLocationData(nonRainingTimes) { response ->
                    val optimizedSchedule: OptimizedSchedule
                    try {
                        optimizedSchedule = parse.fromJson(response, OptimizedSchedule::class.java)
                    } catch (e: Exception) {
                        return@getResponseNoLocationData
                    }
                    if (!isCallbackInvoked) { // If the callback hasn't been called yet
                        if (optimizedSchedule.nullCheck() && qualityCheck(optimizedSchedule)) {
                            isCallbackInvoked = true
                            callback(optimizedSchedule)
                        }
                    }
                    timesRan++
                    if (timesRan > maxRetries - 1 && !isCallbackInvoked) {
                        callback(OptimizedSchedule(emptyList(), emptyList())) // Sad face emoji here
                    }
                }
            }
        }
        fun secondCall() {
            for (i in 0 until maxRetries) { // "Second call"
                getResponseBlockedEvents { response ->
                    val optimizedSchedule: OptimizedSchedule
                    try {
                        optimizedSchedule = parse.fromJson(response, OptimizedSchedule::class.java)
                    } catch (e: Exception) {
                        return@getResponseBlockedEvents
                    }
                    if (!isCallbackInvoked) { // If the callback hasn't been called yet
                        if (optimizedSchedule.nullCheck() && qualityCheck(optimizedSchedule)) {
                            isCallbackInvoked = true
                            callback(optimizedSchedule)
                        }
                    }
                    timesRan++
                    if (timesRan > maxRetries - 1 && !isCallbackInvoked) {
                        timesRan = 0
                        thirdCall()
                    }
                }

            }

        }
        for (i in 0 until maxRetries) { // "Main call"
            getResponse(use4, nonRainingTimes) { response ->
                val optimizedSchedule: OptimizedSchedule
                try {
                    optimizedSchedule = parse.fromJson(response, OptimizedSchedule::class.java)
                } catch (e: Exception) {
                    return@getResponse
                }
                if (!isCallbackInvoked) { // If the callback hasn't been called yet
                    if (hasRainingTimes) {
                        if (optimizedSchedule.nullCheck() && qualityCheck(optimizedSchedule, nonRainingTimes)) {
                            isCallbackInvoked = true
                            callback(optimizedSchedule)
                        }
                    } else {
                        if (optimizedSchedule.nullCheck() && qualityCheck(optimizedSchedule)) {
                            isCallbackInvoked = true
                            callback(optimizedSchedule)
                        }
                    }
                }
                timesRan++
                if(timesRan > maxRetries -1 && !isCallbackInvoked) {
                    timesRan = 0
                    secondCall()
                }
            }
        }

    }

    /** This function is designed to be the main entry point into the AI call. It will start with a very strict prompt that gives it
     * a lot of information that it needs to parse through. If quality cannot be ensured, it will resort to using less factors in the prompt so it can
     * zero in on what the user needs from the AI
     * @param nonRainingTimes Needed whether or not it is raining outside
     * @param callback returns the schedule that will be handed off to firestore. In the event of an error, two empty lists will be passed to the callback
     */
    fun makeCall(use4: Boolean = false, nonRainingTimes: List<Pair<LocalDateTime, LocalDateTime>>, callback: (OptimizedSchedule) -> Unit) {

        if (use4) {
            model = "gpt-4-1106-preview" // This points to the latest (best?) model
        }

        // Init nonRainingTimes bool
        var hasRainingTimes = true
        // Check if the nonRainingTimes list is 0:00-00:00
        if (nonRainingTimes.size == 1 && nonRainingTimes[0].first.hour == 0 && nonRainingTimes[0].second.hour == 0 && events.any { it.isOutside }) {
            hasRainingTimes = false
        }

        // init parser
        val parse = GsonBuilder().create()

        // init has overlapping events
        var hasOverlappingEvents = false

        // Check for overlapping events
        if (events.any { event -> events.any { it != event && it.startTime.isBefore(event.endTime) && it.endTime.isAfter(event.startTime) } }) {
            hasOverlappingEvents = true
        }
        // Tries for a good response up to 3 times.
        val maxRetries = 3
        // Make a call to the least struct mf
        fun thirdCall(iterations: Int, hasRainingTime: Boolean) {
         if (iterations > maxRetries) {
             // If we get to here, we're down bad. Return nothing
             Log.w("OptimizeSchedule", "Third call failed after 3 attempts.")
             callback(OptimizedSchedule(emptyList(), emptyList()))
             return
         }
            getResponseNoLocationData(nonRainingTimes) {
                if (it == "Error") {
                    thirdCall(iterations + 1, hasRainingTime)
                } else {
                    // Create optimized schedule object and parse output into object
                    val optimizedSchedule: OptimizedSchedule
                    try {
                        optimizedSchedule = parse.fromJson(it, OptimizedSchedule::class.java)
                    } catch (e: Exception) {
                        thirdCall(iterations + 1, hasRainingTime)
                        return@getResponseNoLocationData
                    }
                    if (optimizedSchedule.nullCheck() && qualityCheck(optimizedSchedule)) {
                        callback(optimizedSchedule)
                        return@getResponseNoLocationData
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
                Log.i("OptimizeSchedule", "Second call failed after 3 attempts.")
                thirdCall(0, hasRainingTime)
                return
            }
            getResponseBlockedEvents {
                if (it == "Error") {
                    secondCall(iterations + 1, hasRainingTime)
                } else {
                    val optimizedSchedule: OptimizedSchedule
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
                Log.i("OptimizeSchedule", "Main call failed after 3 attempts.")
                return
            }
            getResponse(use4, nonRainingTimes) {
                if (it == "Error") {
                    mainCall(iterations + 1, hasRainingTime)
                } else {
                    val optimizedSchedule: OptimizedSchedule
                    try {
                        optimizedSchedule = parse.fromJson(it, OptimizedSchedule::class.java)
                    } catch (e: Exception) {
                        mainCall(iterations + 1, hasRainingTime)
                        return@getResponse
                    }
                    // If it has non raining times, make sure to check for that
                    if (hasRainingTime) {
                        if (optimizedSchedule.nullCheck() && qualityCheck(optimizedSchedule, nonRainingTimes)) {
                            callback(optimizedSchedule)
                        } else {
                            mainCall(iterations + 1, true)
                        }
                    } else {
                        if (optimizedSchedule.nullCheck() && qualityCheck(optimizedSchedule)) {
                            callback(optimizedSchedule)
                            return@getResponse
                        } else {
                                mainCall(iterations + 1, false)
                        }
                    }
                }
            }

        }
        // If there are overlapping events, then remove them before making the main call
        if (hasOverlappingEvents) {
            removeOverlappingEvents {
                if (it.isNotEmpty()) {
                    updateEvents(it) // Update the events list
                    mainCall(0, hasRainingTimes) // Make the main call
                }
            }
        } else {
            mainCall(0, hasRainingTimes) // Make the main call
        }


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
                    var optimizedSchedule: OptimizedSchedule = OptimizedSchedule(emptyList(), emptyList())
                    try {
                        optimizedSchedule = parse.fromJson(response, OptimizedSchedule::class.java)
                    } catch(e: Exception) {
                        retry(retries + 1)
                        return@getResponseBlockedEvents
                    }

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
            // try catch
            val optimizedSchedule: OptimizedSchedule
            try {
                optimizedSchedule = parse.fromJson(response, OptimizedSchedule::class.java)
            } catch (e: Exception) {
                callback(emptyList())
                return@getResponseOverlappingEvents
            }
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
        return false
    }

    /** This function is a helper function to check if the schedule is fully optimized before returning the object to the callback.
     * @param optimizedSchedule: The optimized schedule that is to be checked
     * @return: Returns true if the schedule is fully optimized, else false
     */
    private fun qualityCheck(optimizedSchedule: OptimizedSchedule, nonRainingTimes: List<Pair<LocalDateTime, LocalDateTime>>? = null): Boolean {
        var passedQualityCheck = true
        val today = events[0].startTime // Get the day of the events by sampling the first event
        // Check that the number of events is the same
        if (optimizedSchedule.events.size != events.size) {
            passedQualityCheck = false
        }
        // Check that the events in events has a respective optimized event in optimizedSchedule.events
        for (event in events) {
            val matchingEvent = optimizedSchedule.events.find { it.name == event.name }
            if (matchingEvent == null) {
                passedQualityCheck = false
            }
        }
        // Check that the old events start and end times don't match the new events start and end times
        var matchingStartAndEndTime = 0
        for (event in events) {
            val matchingEvent = optimizedSchedule.events.find { it.name == event.name }
            if (matchingEvent != null) {
                if (event.startTime == ParseTime().parseTime(matchingEvent.startTime, today) && event.endTime == ParseTime().parseTime(matchingEvent.endTime, today)) {
                    matchingStartAndEndTime++
                }
            }
        }
        if (matchingStartAndEndTime == events.size) {
            passedQualityCheck = false
        }
        // Check that the events and old events have the same value when you subtract the start time from the end time
        var matchingEventDuration = 0
        for (event in optimizedSchedule.events) {
            val matchingEvent = events.find { it.name == event.name }
            if (matchingEvent != null) {
                val oldMatchingEvent = optimizedSchedule.oldEvents.find { it.name == event.name }
                if (oldMatchingEvent != null) {
                    val oldDuration = ParseTime().parseTime(oldMatchingEvent.endTime, today).toLocalTime().toSecondOfDay() - ParseTime().parseTime(oldMatchingEvent.startTime, today).toLocalTime().toSecondOfDay()
                    val newDuration = ParseTime().parseTime(oldMatchingEvent.endTime, today).toLocalTime().toSecondOfDay() - ParseTime().parseTime(oldMatchingEvent.startTime, today).toLocalTime().toSecondOfDay()
                    if (oldDuration == newDuration) {
                        matchingEventDuration++
                    }
                }
            }
        }
        if (matchingEventDuration != optimizedSchedule.events.size) {
            passedQualityCheck = false
        }



        // If there are nonRainingTimes, then check that the events that have isOutside are scheduled during the non-raining times
        var isScheduledDuringNonRainingTimeCount = 0
        if (nonRainingTimes != null) {
            for (event in events) {
                val matchingEvent = optimizedSchedule.events.find { it.name == event.name } // Find event with matching name
                if (event.isOutside && matchingEvent != null) { // If the event is outside and there is a matching event
                    // Parse the start and end times of the matching event
                    val timePartsStart = matchingEvent.startTime.split(":")
                    val hourStringStart = timePartsStart[0]
                    val minuteStringStart = timePartsStart[1]
                    val timePartsEnd = matchingEvent.endTime.split(":")
                    val hourStringEnd = timePartsEnd[0]
                    val minuteStringEnd = timePartsEnd[1]
                    // Will return true if the event is scheduled during a non-raining time
                    val isScheduledDuringNonRainingTime = nonRainingTimes.any { timeRange ->
                        val startTime = LocalDateTime.of( // Create a LocalDateTime object for the start time
                            today.year,
                            today.month,
                            today.dayOfMonth,
                            hourStringStart.toInt(),
                            minuteStringStart.toInt()
                        )
                        val endTime = LocalDateTime.of( // Create a LocalDateTime object for the end time
                            today.year,
                            today.month,
                            today.dayOfMonth,
                            hourStringEnd.toInt(),
                            minuteStringEnd.toInt()
                        )
                        startTime.isAfter(timeRange.first) && endTime.isBefore(timeRange.second) // Check if the event is scheduled during a non-raining time
                    }
                    if (isScheduledDuringNonRainingTime) {
                        isScheduledDuringNonRainingTimeCount++
                    }
                }
                }
            if (isScheduledDuringNonRainingTimeCount != events.filter { it.isOutside }.size) {
                passedQualityCheck = false
            }
        }

        // Check that non-movable events are not moved
        for (event in dontRescheduleEvents) {
            val matchingEvent = optimizedSchedule.events.find { it.name == event.name }
            if (matchingEvent != null) {
                if (matchingEvent.startTime != event.startTime.format(DateTimeFormatter.ofPattern("HH:mm")) || matchingEvent.endTime != event.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))) {
                    passedQualityCheck = false
                }
            }
        }

        // Check that the start time is before the end time
        // This checks for events that go past midnight
        for (event in optimizedSchedule.events) {
            if (event.startTime >= event.endTime) {
                passedQualityCheck = false
            }
        }

        return passedQualityCheck
    }

    /** This formats the API request and makes a post to GPT API
     * @param systemContent: The system content that is to be sent to the API
     * @param userContent: The user content that is to be sent to the API
     * @param onComplete: The callback function that is called after the response is parsed
     */
    private fun makeRequest(systemContent: String, userContent: String, onComplete: (String) -> Unit) {

        // Make a request object
        val httpPost = Request(model,
            responseFormat,
            systemContent,
            userContent
        )

        val requestBody = request.newBuilder()
            .post(
                """{
    "model": "${httpPost.model}",
    "response_format": ${httpPost.responseFormat},
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
    /** This function is used to update the events list
     * @param events: The new list of events
     */

    private fun updateEvents(events: List<FirestoreEvent>) {
        this.events = events
        // Update eventList
        this.eventList = events.sortedBy { it.startTime } // Sort by startTime in ascending order
            .map { event ->
                "${event.name} goes from ${event.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} to ${event.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))} at ${if (event.location == "") homeAddress else event.location} " +
                        if (event.isOutside) "and is outdoors." else "." // If the event is outside, then add "and is outdoors."
            }
        // update eventString
        this.eventString = eventList.joinToString(" ")
    }
    /** Returns true if it is raining that day and there are outdoor events
     * @param nonRainingTimes: A list of non-raining times for the day
     * @return: Returns true if it is raining that day and there are outdoor events
     */
    private fun hasRainingTimes(nonRainingTimes: List<Pair<LocalDateTime, LocalDateTime>>): Boolean {
        return !(nonRainingTimes.size == 1 && nonRainingTimes[0].first.hour == 0 && nonRainingTimes[0].second.hour == 0)
    }
    private fun List<Pair<LocalDateTime, LocalDateTime>>.formatNonRainingTimesToString(): String {
        return joinToString(" ") { timeRange ->
            "${timeRange.first.format(DateTimeFormatter.ofPattern("HH:mm"))} and ${timeRange.second.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        }
    }


}