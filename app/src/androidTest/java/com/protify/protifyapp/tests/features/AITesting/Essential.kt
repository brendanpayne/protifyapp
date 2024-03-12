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
// Base bones testing of AI functionality
class Essential {

// This syncs with the database and ensures that the json files are up to date
    companion object {
        // Get the context
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        // Set the day to February 20th, 2024 (this is an arbitrary date for testing)
        val today = LocalDateTime.of(2024, 2, 20, 0, 0)
        // User's home address
        const val homeAddress = "762 Morning Dew Ln, Maineville, OH 45039, USA"
        @BeforeClass
        @JvmStatic
        fun `Sync with database`() {
            // CountDownLatch object to wait for the asynchronous code to finish
            val countDownLatch = CountDownLatch(3)
            // Log in to test account
            FirebaseLoginHelper().signIn("tommy.mcreynolds@gmail.com", "testpassword").addOnCompleteListener {
                if (it.isSuccessful) {
                    // Load user data
                    var currentUser = FirebaseLoginHelper().getCurrentUser()

                    // Get events from February 20th, 2024 (this is an arbitrary date for testing)
                    FirestoreHelper().getEvents(
                        currentUser!!.uid,
                        "20",
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
                        val drivingTimesExists = File(context.filesDir, "drivingTimes.json").exists()
                        val optimalOrderExists = File(context.filesDir, "optimalOrder.json").exists()

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
                                    val jsonMatrixFile = File(context.filesDir, "drivingTimes.json")
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
                                        val optimalEventOrder = OptimizeRoute().findOptimalRoute(events, drivingTimeMatrix!!)
                                        // Overwrite the optimalOrder json file
                                        val jsonOptimalOrder = Gson().toJson(optimalEventOrder)
                                        val jsonOptimalOrderFile = File(context.filesDir, "optimalOrder.json")
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
        val eventsFile = File(context.filesDir, "events.json")
        val drivingTimesFile = File(context.filesDir, "drivingTimes.json")
        val optimalOrderFile = File(context.filesDir, "optimalOrder.json")

        // Check that the files exist
        if (!eventsFile.exists() || !drivingTimesFile.exists() || !optimalOrderFile.exists()) {
            assert(false) { "Missing one or more json files. These are required for the tests to run."}
        }
        // Try to convert the json files to a list and assign them to variables
        try {
            val events = Gson().fromJson(eventsFile.readText(), Array<FirestoreEventString>::class.java).toList()
            val drivingTimes = Gson().fromJson(drivingTimesFile.readText(), Array<DrivingTime>::class.java).toList()
            val optimalOrder = Gson().fromJson(optimalOrderFile.readText(), Array<FirestoreEvent>::class.java).toList()
            // If all three have a size of at least 1, the test will pass
            assert(events.isNotEmpty() && drivingTimes.isNotEmpty() && optimalOrder.isNotEmpty())
        } catch (e: Exception) {
            // If the conversion fails, the test will fail
            assert(false) {"One or more json files are not in the correct format."}
        }
    }
    @Test
    fun `Successful api response from openAI`() {
        // init latch
        val countDownLatch = CountDownLatch(1)
        // init gson
        val gson = Gson()
        //Get the json files
        val eventsFile = File(context.filesDir, "events.json")
        val drivingTimesFile = File(context.filesDir, "drivingTimes.json")
        val optimalOrderFile = File(context.filesDir, "optimalOrder.json")

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

        // Run the api call
        OptimizeSchedule(
            today.dayOfMonth.toString(),
            today.month.toString(),
            today.year.toString(),
            events,
            drivingTimes,
            homeAddress,
            optimalOrder
        ).getResponse { response ->
            if (response != "Error" && response != "") {
                // If the api call is successful, the test will pass
                countDownLatch.countDown()
            } else {
                // If the api call fails, the test will fail
                assert(false) { "API call failed" }
            }
        }
        // Wait 15 seconds for the asynchronous code to finish
        countDownLatch.await(15, java.util.concurrent.TimeUnit.SECONDS)
        // If the countDownLatch is not 0, the test will fail
        assert(countDownLatch.count == 0L)
    }
    @Test
    fun `AI output is able to be parsed`() {
        // init latch
        val countDownLatch = CountDownLatch(1)
        // init gson
        val gson = Gson()
        //Get the json files
        val eventsFile = File(context.filesDir, "events.json")
        val drivingTimesFile = File(context.filesDir, "drivingTimes.json")
        val optimalOrderFile = File(context.filesDir, "optimalOrder.json")

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

        // Run the api call
        OptimizeSchedule(
            today.dayOfMonth.toString(),
            today.month.toString(),
            today.year.toString(),
            events,
            drivingTimes,
            homeAddress,
            optimalOrder
        ).parseResponse { response ->
            // In the function, if it fails 5 times in a row, it will return empty lists
            if (response.events.isEmpty() || response.oldEvents.isEmpty()) {
                // If the response is empty, the test will fail
                assert(false) {"No valid response from API call."}
            } else {
                // If the response is not empty, the test will pass
                countDownLatch.countDown()
            }
        }
        // Wait 60 seconds for the asynchronous code to finish
        countDownLatch.await()
        // If the countDownLatch is not 0, the test will fail
        assert(countDownLatch.count == 0L)
    }
    @Test
    fun `Schedule is different from original`() {
        // init latch
        val countDownLatch = CountDownLatch(1)
        // init gson
        val gson = Gson()
        //Get the json files
        val eventsFile = File(context.filesDir, "events.json")
        val drivingTimesFile = File(context.filesDir, "drivingTimes.json")
        val optimalOrderFile = File(context.filesDir, "optimalOrder.json")

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

        // Run the api call
        OptimizeSchedule(
            today.dayOfMonth.toString(),
            today.month.toString(),
            today.year.toString(),
            events,
            drivingTimes,
            homeAddress,
            optimalOrder
        ).parseResponse { response ->
            // If the events are different, the test will pass
            if (response.events != response.oldEvents) {
                countDownLatch.countDown()
            } else {
                // If the events are the same, the test will fail
                assert(false) {"No events were modified"}
            }
        }
        // Wait 60 seconds for the asynchronous code to finish
        countDownLatch.await()
        // If the countDownLatch is not 0, the test will fail
        assert(countDownLatch.count == 0L)
    }
    @Test
    fun `Matrix contains all locations`() {
        // init latch
        val countDownLatch = CountDownLatch(1)
        // init gson
        val gson = Gson()
        //Get the events json file
        val eventsFile = File(context.filesDir, "events.json")
        // Convert the json file to a list
        val eventsString: List<FirestoreEventString> = gson.fromJson(eventsFile.readText(), Array<FirestoreEventString>::class.java).toList()
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

       // Get a list of locations based off of the events
        var locations = events.map { it.location }.toMutableList()

        // Test getDrivingTimes
        MapsDurationUtils(LocalDateTime.now()).getDrivingTimes(
            homeAddress,
            locations
        ) { drivingTimes ->
            // Get all of the address from the matrix and put them into a list
            val matrixAddresses = drivingTimes!!.map { it!!.startLocation } + drivingTimes.map { it!!.endLocation }
            // Remove empty strings from locations
            locations = locations.filter { it != "" }.toMutableList()
            // Remove duplicates from locations
            locations = locations.distinct().toMutableList()
            // Make sure there are as many unique values as there are locations
            if (matrixAddresses.distinct().size >= locations.size) {
                countDownLatch.countDown()
            } else {
                // If the matrix is null, the test will fail
                assert(false) {"Matrix does not contain all locations"}
            }
        }
        // Wait 25 seconds for the asynchronous code to finish
        countDownLatch.await(25, java.util.concurrent.TimeUnit.SECONDS)
        // If the countDownLatch is not 0, the test will fail
        assert(countDownLatch.count == 0L)
    }

    @Test
    fun removeOverlappingEventsTest() {

        // Create a list of events that are overlapping

        val event1 = FirestoreEvent(
            startTime = LocalDateTime.of(2024, 2, 20, 8, 0),
            endTime = LocalDateTime.of(2024, 2, 20, 9, 0),
            name = "Gaming",
            nameLower = "",
            location = "",
            description = "",
            timeZone = "",
            importance = 0,
            attendees = null,
            rainCheck = false,
            isRaining = false,
            mapsCheck = false,
            distance = 0,
            isOutside = false,
            isOptimized = false,
            isAiSuggestion = false,
            isUserAccepted = false
        )
        val event2 = FirestoreEvent(
            startTime = LocalDateTime.of(2024, 2, 20, 8, 30),
            endTime = LocalDateTime.of(2024, 2, 20, 10, 0),
            name = "Work",
            nameLower = "",
            location = "",
            description = "",
            timeZone = "",
            importance = 0,
            attendees = null,
            rainCheck = false,
            isRaining = false,
            mapsCheck = false,
            distance = 0,
            isOutside = false,
            isOptimized = false,
            isAiSuggestion = false,
            isUserAccepted = false
        )

        val event3 = FirestoreEvent(
            startTime = LocalDateTime.of(2024, 2, 20, 12, 0),
            endTime = LocalDateTime.of(2024, 2, 20, 14, 0),
            name = "Gym",
            nameLower = "",
            location = "",
            description = "",
            timeZone = "",
            importance = 0,
            attendees = null,
            rainCheck = false,
            isRaining = false,
            mapsCheck = false,
            distance = 0,
            isOutside = false,
            isOptimized = false,
            isAiSuggestion = false,
            isUserAccepted = false
        )

        // add the events to a list
        val events = mutableListOf(event1, event2, event3)

        // init countdown latch
        val countDownLatch = CountDownLatch(1)


        // Call the function to remove overlapping events, we're putting in a bunch of garbage because I wrote this class like garbage
        OptimizeSchedule("", "", "", events, mutableListOf(), "762 Morning Dew Lane, Maineville OH, 45039", mutableListOf()).removeOverlappingEvents {

            val overlapping = it.any { event1 ->
                it.any { event2 ->
                    event1 != event2 &&  // Ensure events are different
                            (event1.startTime.isBefore(event2.endTime) && event1.endTime.isAfter(event2.startTime)) // Check for overlap
                }
            }

            if (!overlapping) {
                countDownLatch.countDown()
            } else {
                assert(false) { "Events are overlapping" }
            }
        }
        // Wait 20 seconds for the asynchronous code to finish
        countDownLatch.await(20, java.util.concurrent.TimeUnit.SECONDS)
        // If the countDownLatch is not 0, the test will fail
        assert(countDownLatch.count == 0L) { "Timed out"}

    }
}