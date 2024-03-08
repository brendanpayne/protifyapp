package com.protify.protifyapp.tests.features.AITesting

import FirestoreEventString
import OptimizedSchedule
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.protify.protifyapp.FirestoreEvent
import com.protify.protifyapp.FirestoreHelper
import com.protify.protifyapp.features.GoogleMapsAPI.DrivingTime
import com.protify.protifyapp.features.GoogleMapsAPI.OptimizeRoute
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import com.protify.protifyapp.utils.MapsDurationUtils
import com.protify.protifyapp.utils.OpenAIHelper.OptimizeSchedule
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch

// This class makes sure that the output of the AI makes sense when it's raining outside and there are outdoor events
class StrictRain {

    // This syncs with the database and ensures that the json files are up to date
    companion object {
        // Get the context
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

        // Set the day to February 20th, 2024 (this is an arbitrary date for testing)
        val today = LocalDateTime.of(2024, 2, 20, 0, 0)

        // User's home address
        const val homeAddress = "6190 Falla Drive, Canal Winchester, OH 43110"

        // Make a mock nonRainingTimes list
        val nonRainingTimes: List<Pair<LocalDateTime, LocalDateTime>> = listOf(
            Pair(
                LocalDateTime.of(2024, 2, 24, 6, 0),
                LocalDateTime.of(2024, 2, 24, 16, 59)

            )
        )
        @BeforeClass
        @JvmStatic
        fun `Sync with database`() {
            // CountDownLatch object to wait for the asynchronous code to finish
            val countDownLatch = CountDownLatch(3)
            // Log in to test account
            FirebaseLoginHelper().signIn("tommy.mcreynolds@gmail.com", "testpassword")
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        // Load user data
                        var currentUser = FirebaseLoginHelper().getCurrentUser()

                        // Get events from February 24th, 2024 (this is an arbitrary date for testing AT LEAST ONE OUTDOOR EVENT IS REQUIRED FOR TESTING)
                        FirestoreHelper().getEvents(
                            currentUser!!.uid,
                            "24",
                            "February",
                            "2024"
                        ) { events ->
                            // If all of the events aren't outside, the test will fail
                            if (events.all { !it.isOutside }) {
                                assert(false) {"At least one event must be outside on February 24th, 2024"}
                            }

                            // Convert FirestoreEvent to FirestoreEventString
                            val eventsString = events.map {
                                FirestoreEventString(
                                    it.name,
                                    it.nameLower,
                                    it.startTime.toString(),
                                    it.endTime.toString(),
                                    it.location,
                                    it.description,
                                    it.timeZone,
                                    it.importance,
                                    it.attendees,
                                    it.rainCheck,
                                    it.isRaining,
                                    it.mapsCheck,
                                    it.distance,
                                    it.isOutside,
                                    it.isOptimized,
                                    isUserAccepted = false,
                                    isAiSuggestion = false
                                )
                            }
                            // Convert events to json
                            val jsonEvents = Gson().toJson(eventsString)
                            // Get the events json file
                            val jsonEventsFile = File(context.filesDir, "events.json")
                            // If the file doesn't exist, create it
                            if (!jsonEventsFile.exists()) {
                                jsonEventsFile.createNewFile()
                            }
                            // Get the events json file and convert it to a string
                            val jsonEventsFileString = jsonEventsFile.readText()
                            // Signal that events have been fetched
                            countDownLatch.countDown()

                            // Make sure drivingTimes.json and optimalOrder.json exist
                            val drivingTimesExists =
                                File(context.filesDir, "drivingTimes.json").exists()
                            val optimalOrderExists =
                                File(context.filesDir, "optimalOrder.json").exists()

                            // Compare the two JSON strings
                            if (jsonEvents == jsonEventsFileString && drivingTimesExists && optimalOrderExists) {
                                // If the database is already up to date, continue with the tests
                                // Set the countDownLatch to 0
                                repeat((countDownLatch.count).toInt()) {
                                    countDownLatch.countDown()
                                }
                            } else {
                                // Overwrite the events json file
                                jsonEventsFile.writeText(jsonEvents)

                                // Pull the locations from the events and map it to a list
                                val locations = events.map { it.location }.toMutableList()

                                // Get the driving times for the locations
                                MapsDurationUtils(LocalDateTime.now()).getDrivingTimes(
                                    homeAddress,
                                    locations
                                ) { matrix ->
                                    if (matrix != null) {
                                        val jsonMatrix = Gson().toJson(matrix)
                                        val jsonMatrixFile =
                                            File(context.filesDir, "drivingTimes.json")
                                        // If it doesn't exist, create it
                                        if (!jsonMatrixFile.exists()) {
                                            jsonMatrixFile.createNewFile()
                                        }
                                        // Overwrite the divingTimes json file
                                        jsonMatrixFile.writeText(jsonMatrix)
                                        // Signal that the driving times have been fetched
                                        countDownLatch.countDown()

                                        // Get the distance matrix
                                        MapsDurationUtils(LocalDateTime.now()).getMatrix(
                                            homeAddress,
                                            locations
                                        ) { drivingTimeMatrix ->
                                            // Get the optimal order of events
                                            val optimalEventOrder =
                                                OptimizeRoute().findOptimalRoute(
                                                    events,
                                                    drivingTimeMatrix!!
                                                )
                                            // Overwrite the optimalOrder json file
                                            val jsonOptimalOrder = Gson().toJson(optimalEventOrder)
                                            val jsonOptimalOrderFile =
                                                File(context.filesDir, "optimalOrder.json")
                                            // If it doesn't exist, create it
                                            if (!jsonOptimalOrderFile.exists()) {
                                                jsonOptimalOrderFile.createNewFile()
                                            }
                                            jsonOptimalOrderFile.writeText(jsonOptimalOrder)
                                            // Signal that the optimal order has been fetched
                                            countDownLatch.countDown()
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // If the login fails, the test will fail
                        assert(false) {"Login failed"}
                    }
                }
            // Wait 45 seconds for the asynchronous code to finish
            countDownLatch.await(45, java.util.concurrent.TimeUnit.SECONDS)
            // If the countDownLatch is not 0, the test will fail
            assert(countDownLatch.count == 0L)
        }
    }

    @Before
    fun `Get AI response`() {
        // init latch
        val countDownLatch = CountDownLatch(1)
        // init gson
        val gson = Gson()
        //Get the json files
        val eventsFile = File(context.filesDir, "events.json")
        val drivingTimesFile = File(context.filesDir, "drivingTimes.json")
        val optimalOrderFile = File(context.filesDir, "optimalOrder.json")

        // Convert the json files to objects
        val eventsString: List<FirestoreEventString> =
            gson.fromJson(eventsFile.readText(), Array<FirestoreEventString>::class.java).toList()
        val drivingTimes: MutableList<DrivingTime?> =
            gson.fromJson(drivingTimesFile.readText(), Array<DrivingTime>::class.java)
                .toMutableList()
        val optimalOrder: List<FirestoreEvent> =
            gson.fromJson(optimalOrderFile.readText(), Array<FirestoreEvent>::class.java).toList()

        // Convert List<FirestoreEventString> to List<FirestoreEvent>
        val events = eventsString.map {
            FirestoreEvent(
                it.name,
                it.nameLower,
                LocalDateTime.parse(it.startTime),
                LocalDateTime.parse(it.endTime),
                it.location,
                it.description,
                it.timeZone,
                it.importance,
                it.attendees,
                it.rainCheck,
                it.isRaining,
                it.mapsCheck,
                it.distance,
                it.isOutside,
                it.isOptimized,
                isUserAccepted = false,
                isAiSuggestion = false
            )
        }
        // Run the api call
        OptimizeSchedule(
            Essential.today.dayOfMonth.toString(),
            Essential.today.month.toString(),
            Essential.today.year.toString(),
            events,
            drivingTimes,
            Essential.homeAddress,
            optimalOrder
        ).getResponse(nonRainingTimes) { response ->

            // Parse response into OptimizedSchedule object
            var optimizedSchedule: OptimizedSchedule = OptimizedSchedule(events = emptyList(), oldEvents = emptyList()) // Create empty list
            try {
                optimizedSchedule = gson.fromJson(response, OptimizedSchedule::class.java)
            } catch (e: Exception) {
                assert(false) {"The response is not in the correct format"}
            }

            // In the function, if it fails 5 times in a row, it will return empty lists
            if (optimizedSchedule.events.isEmpty() || optimizedSchedule.oldEvents.isEmpty()) {
                // If the response is empty, the test will fail
                assert(false) {"The response is empty likely due to incorrect formatting of the input data or an error in the API call."}
            } else {
                // Write the response to a file
                val responseFile = File(context.filesDir, "aiResponse.json")
                // Check if the file exists
                if (!responseFile.exists()) {
                    responseFile.createNewFile()
                }
                // Write the response to the file
                responseFile.writeText(gson.toJson(optimizedSchedule))
                // If the response is not empty, the test will pass
                countDownLatch.countDown()
            }
        }
        // Wait 25 seconds for the asynchronous code to finish
        countDownLatch.await(85, java.util.concurrent.TimeUnit.SECONDS)
        // If the countDownLatch is not 0, the test will fail
        assert(countDownLatch.count == 0L) {"No response was received from the AI"}

    }

    @Test
    fun `The response makes sense`() {
        // init gson
        val gson = Gson()
        // Get the response file
        val responseFile = File(context.filesDir, "aiResponse.json")
        // Convert the response file to an OptimizedSchedule object
        val response = gson.fromJson(responseFile.readText(), OptimizedSchedule::class.java)

        // Get the events file
        val eventsFile = File(context.filesDir, "events.json")
        // Convert the events file to a list of FirestoreEventString objects
        val eventsString: List<FirestoreEventString> =
            gson.fromJson(eventsFile.readText(), Array<FirestoreEventString>::class.java).toList()

        // Get all of the events that are outside
        val outdoorEvents = eventsString.filter { it.isOutside }

        // Test cases

        // If the old schedule is the same as the new schedule, the test will fail
        if (response.events == response.oldEvents) {
            assert(false) {"The old schedule is the same as the new schedule"}
        }
        // If the old schedule or new schedule is empty, the test will fail
        if (response.events.isEmpty() || response.oldEvents.isEmpty()) {
            assert(false) {"The old schedule or new schedule is empty"}
        }

        // Match the events in the response to the events in the events file by name with case insensitivity
        // If the ai didn't return all of the events, the test will fail
        eventsString.forEach { event ->
            if (response.events.none { it.name.equals(event.name, true) }) {
                assert(false) {"One or more events are missing from the response"}
            }
        }
        var outdoorEventDuringNonRainingTimeCount = 0
        // For all of the events in the eventsString that are outside, each event with a matching name must be scheduled during a nonRainingTime
        outdoorEvents.forEach { event ->
            // Find the outdoor event in the response
            val matchingEvent = response.events.find { it.name.equals(event.name, true) }
            if (matchingEvent != null) {
                // Parse the string time of the event to a LocalDateTime object
                val eventLocalDateTimeStartTime = LocalDateTime.parse(event.startTime)
                val eventLocalDateTimeEndTime = LocalDateTime.parse(event.endTime)

                // Break the startTime and endTime into separate variables from mathcingEvent
                val timePartsStart = matchingEvent.startTime.split(":")
                val hourStringStart = timePartsStart[0]
                val minuteStringStart = timePartsStart[1]
                val timePartsEnd = matchingEvent.endTime.split(":")
                val hourStringEnd = timePartsEnd[0]
                val minuteStringEnd = timePartsEnd[1]

                // Use the matching event to get the start and end times
                val startTime = LocalDateTime.of(
                    eventLocalDateTimeStartTime.year,
                    eventLocalDateTimeStartTime.month,
                    eventLocalDateTimeStartTime.dayOfMonth,
                    hourStringStart.toInt(),
                    minuteStringStart.toInt()

                )
                val endTime = LocalDateTime.of(
                    eventLocalDateTimeEndTime.year,
                    eventLocalDateTimeEndTime.month,
                    eventLocalDateTimeEndTime.dayOfMonth,
                    hourStringEnd.toInt(),
                    minuteStringEnd.toInt()
                )
                // If the event is scheduled during a nonRainingTime, increment the count
                if (nonRainingTimes.any { nonRainingTime ->
                        val graceStartTime = nonRainingTime.first.minusMinutes(15)
                        val graceEndTime = nonRainingTime.second.plusMinutes(15)
                        startTime.isAfter(graceStartTime) && endTime.isBefore(graceEndTime)
                    }) {
                    outdoorEventDuringNonRainingTimeCount++
                }
            }
        }
        // If there is only one outdoor event, and it is not scheduled during a nonRainingTime, the test will fail
        if (outdoorEvents.size == 1) {
            if (outdoorEventDuringNonRainingTimeCount != 1) {
                assert(false) {"One or more outdoor events are scheduled during a time when it is raining"}

            }
            // If there are two or more outdoor events, and none of them are scheduled during a nonRainingTime, the test will fail
        } else {
            if (outdoorEventDuringNonRainingTimeCount < 1) {
                assert(false) {"Two or more outdoor events are scheduled during a time when it is raining"}
            }

        }
        // Make sure the event durations have not been changed
        response.events.forEach { event ->
            // Match the event to the event in the eventsString by name with case insensitivity
            val matchingEvent = eventsString.find { it.name.equals(event.name, true) }
            if (matchingEvent != null) {
                // Parse the string time of the event to a LocalDateTime object
                val eventLocalDateTimeStartTime = LocalDateTime.parse(matchingEvent.startTime)
                val eventLocalDateTimeEndTime = LocalDateTime.parse(matchingEvent.endTime)
                // Get the duration of the event
                val eventDuration = eventLocalDateTimeStartTime.until(eventLocalDateTimeEndTime, java.time.temporal.ChronoUnit.MINUTES)
                // Break the startTime and endTime into separate variables from event
                val timePartsStart = event.startTime.split(":")
                val hourStringStart = timePartsStart[0]
                val minuteStringStart = timePartsStart[1]
                val timePartsEnd = event.endTime.split(":")
                val hourStringEnd = timePartsEnd[0]
                val minuteStringEnd = timePartsEnd[1]
                // Get the duration of the event from the response
                val startTimeResponse = eventLocalDateTimeStartTime.withHour(hourStringStart.toInt()).withMinute(minuteStringStart.toInt())
                val endTimeResponse = eventLocalDateTimeEndTime.withHour(hourStringEnd.toInt()).withMinute(minuteStringEnd.toInt())
                val eventDurationResponse = startTimeResponse.until(endTimeResponse, java.time.temporal.ChronoUnit.MINUTES)

                if (eventDuration != eventDurationResponse) {
                    assert(false) {"The duration of one or more events has been changed"}
                }
            }
        }
        // If you made it this far, the test will pass
        assert(true)
    }
}