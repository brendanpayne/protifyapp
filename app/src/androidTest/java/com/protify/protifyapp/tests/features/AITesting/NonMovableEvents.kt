package com.protify.protifyapp.tests.features.AITesting

import FirestoreEventString
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

class NonMovableEvents {

    companion object {
        // Get the context
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

        // Set the day to February 22nd, 2024 (this is an arbitrary date for testing)
        val today = LocalDateTime.of(2024, 2, 22, 0, 0)

        // User's home address
        const val homeAddress = "762 Morning Dew Ln, Maineville, OH 45039, USA"

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

                        // Get events from February 20th, 2024 (this is an arbitrary date for testing)
                        FirestoreHelper().getEvents(
                            currentUser!!.uid,
                            "22",
                            "February",
                            "2024"
                        ) { events ->
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
                                    it.isAiSuggestion,
                                    it.isUserAccepted
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
                        assert(false) { "Login failed" }
                    }
                }
            // Wait 45 seconds for the asynchronous code to finish
            countDownLatch.await(45, java.util.concurrent.TimeUnit.SECONDS)
            // If the countDownLatch is not 0, the test will fail
            assert(countDownLatch.count == 0L)
        }
    }

    @Before
    fun `json files have valid format`() {
        // Get the json files
        val eventsFile = File(Essential.context.filesDir, "events.json")
        val drivingTimesFile = File(Essential.context.filesDir, "drivingTimes.json")
        val optimalOrderFile = File(Essential.context.filesDir, "optimalOrder.json")

        // Check that the files exist
        if (!eventsFile.exists() || !drivingTimesFile.exists() || !optimalOrderFile.exists()) {
            assert(false) { "Missing one or more json files. These are required for the tests to run." }
        }
        // Try to convert the json files to a list and assign them to variables
        try {
            val events =
                Gson().fromJson(eventsFile.readText(), Array<FirestoreEventString>::class.java)
                    .toList()
            val drivingTimes =
                Gson().fromJson(drivingTimesFile.readText(), Array<DrivingTime>::class.java)
                    .toList()
            val optimalOrder =
                Gson().fromJson(optimalOrderFile.readText(), Array<FirestoreEvent>::class.java)
                    .toList()
            // If all three have a size of at least 1, the test will pass
            assert(events.isNotEmpty() && drivingTimes.isNotEmpty() && optimalOrder.isNotEmpty())
        } catch (e: Exception) {
            // If the conversion fails, the test will fail
            assert(false) { "One or more json files are not in the correct format." }
        }
    }

    @Test
    fun `Non movable events were not moved`() {
        // init allEventsAreAtSameTime
        var allEventsAreAtSameTime = false
        // init latch
        val countDownLatch = CountDownLatch(1)
        // init gson
        val gson = Gson()
        //Get the json files
        val eventsFile = File(Essential.context.filesDir, "events.json")
        val drivingTimesFile = File(Essential.context.filesDir, "drivingTimes.json")
        val optimalOrderFile = File(Essential.context.filesDir, "optimalOrder.json")

        // Convert the json files to objects
        val eventsString: List<FirestoreEventString> = gson.fromJson(eventsFile.readText(), Array<FirestoreEventString>::class.java).toList()
        val drivingTimes: MutableList<DrivingTime?> = gson.fromJson(drivingTimesFile.readText(), Array<DrivingTime>::class.java).toMutableList()
        val optimalOrder: List<FirestoreEvent> = gson.fromJson(optimalOrderFile.readText(), Array<FirestoreEvent>::class.java).toList()

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
                it.isAiSuggestion,
                it.isUserAccepted
            )
        }
        // Get all of the events that have isOptmized set to true
        val optimizedEvents = events.filter { it.isOptimized }
        // Run the api call
        OptimizeSchedule(
            Essential.today.dayOfMonth.toString(),
            Essential.today.month.toString(),
            Essential.today.year.toString(),
            events,
            drivingTimes,
            Essential.homeAddress,
            optimalOrder
        ).parseResponseBlockedEvents { blockedEventsResponse ->
            // Get the old and new events
            val oldEvents = blockedEventsResponse.oldEvents
            val newEvents = blockedEventsResponse.events
            // In newEvents, check that all of the events that have isOptimized set to true are still at the same time
            allEventsAreAtSameTime = optimizedEvents.all { event ->
                val oldEvent = oldEvents.find { it.name.equals(event.name, ignoreCase = true) }
                assert(oldEvent != null) { "Event ${event.name} was not found in oldEvents" }
                val newEvent = newEvents.find { it.name.equals(event.name, ignoreCase = true) }
                assert(newEvent != null) { "Event ${event.name} was not found in newEvents" }
                assert(oldEvent?.startTime == newEvent?.startTime) { "Event ${event.name} was moved" }
                oldEvent != null && newEvent != null && oldEvent.startTime == newEvent.startTime

            }
                // Count down the latch
            countDownLatch.countDown()
        }
        // Wait 45 seconds for the asynchronous code to finish
        countDownLatch.await(45, java.util.concurrent.TimeUnit.SECONDS)
        if (allEventsAreAtSameTime) {
            assert(true)
        }
        else {
            assert(false) { "One or more non-movable events were moved." }
        }
    }
}