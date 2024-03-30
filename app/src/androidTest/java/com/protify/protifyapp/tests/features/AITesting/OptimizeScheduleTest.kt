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
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch

class OptimizeScheduleTest {

    companion object {
        // Get the context
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

        // Set the day to February 20th, 2024 (this is an arbitrary date for testing)
        val today = LocalDateTime.of(2024, 2, 20, 0, 0)

        // User's home address
        const val homeAddress = "6190 Falla Drive, Canal Winchester, OH 43110"

        // Make a mock nonRainingTimes list so we don't really have to make a call to the weather API
        val nonRainingTimes: List<Pair<LocalDateTime, LocalDateTime>> = listOf(
            Pair(
                LocalDateTime.of(2024, 2, 24, 6, 0), // 6:00 AM
                LocalDateTime.of(2024, 2, 24, 16, 59) // 4:59 PM

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
    @Test
    fun testMakeCallFour() {
        // init latch
        val countDownLatch = CountDownLatch(1)
        // init gson
        val gson = Gson()
        //Get the json files
        val eventsFile = File(StrictRain.context.filesDir, "events.json")
        val drivingTimesFile = File(StrictRain.context.filesDir, "drivingTimes.json")
        val optimalOrderFile = File(StrictRain.context.filesDir, "optimalOrder.json")

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

        // Make the call
        OptimizeSchedule(
            homeAddress = homeAddress,
            year = "2024",
            month = "February",
            day = "24",
            travelTime = drivingTimes,
            optimalEventOrder = optimalOrder,
            events = events
        ).makeCall(true, nonRainingTimes) { optimizedEvents -> // Use 4.0 for this test
            // If the events were not able to be stored, then the optimization failed
            if (optimizedEvents.events.isEmpty() || optimizedEvents.oldEvents.isEmpty()) {
                assert(false) { "The optimization failed" }
            }
            // Signal that the optimization was successful
            countDownLatch.countDown()
        }
        // Wait 3 minutes for the asynchronous code to finish
        countDownLatch.await(180, java.util.concurrent.TimeUnit.SECONDS)
        // If the countDownLatch is not 0, the test will fail
        assert(countDownLatch.count == 0L) {"No response"}
    }
    @Test
    fun testMakeCallParallel() {
        // init latch
        val countDownLatch = CountDownLatch(1)
        // init gson
        val gson = Gson()
        //Get the json files
        val eventsFile = File(StrictRain.context.filesDir, "events.json")
        val drivingTimesFile = File(StrictRain.context.filesDir, "drivingTimes.json")
        val optimalOrderFile = File(StrictRain.context.filesDir, "optimalOrder.json")

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

        // Make the call
        OptimizeSchedule(
            homeAddress = homeAddress,
            year = "2024",
            month = "February",
            day = "24",
            travelTime = drivingTimes,
            optimalEventOrder = optimalOrder,
            events = events
        ).makeCallParallel(false, nonRainingTimes) { optimizedEvents -> // Use 3.5 for this test
            // If the events were not able to be stored, then the optimization failed
            if (optimizedEvents.events.isEmpty() || optimizedEvents.oldEvents.isEmpty()) {
                assert(false) { "The optimization failed" }
            }
            // Signal that the optimization was successful
            countDownLatch.countDown()
        }
        countDownLatch.await(120, java.util.concurrent.TimeUnit.SECONDS)
        // If the countDownLatch is not successful, the test will fail
        assert(countDownLatch.count == 0L) {"Not fast enough"}
    }
    @Test
    fun testMakeCallParallelFour() {
        // init latch
        val countDownLatch = CountDownLatch(1)
        // init gson
        val gson = Gson()
        //Get the json files
        val eventsFile = File(StrictRain.context.filesDir, "events.json")
        val drivingTimesFile = File(StrictRain.context.filesDir, "drivingTimes.json")
        val optimalOrderFile = File(StrictRain.context.filesDir, "optimalOrder.json")

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

        // Make the call
        OptimizeSchedule(
            homeAddress = homeAddress,
            year = "2024",
            month = "February",
            day = "24",
            travelTime = drivingTimes,
            optimalEventOrder = optimalOrder,
            events = events
        ).makeCallParallel(true, nonRainingTimes) { optimizedEvents -> // Use 4.0 for this test
            // If the events were not able to be stored, then the optimization failed
            if (optimizedEvents.events.isEmpty() || optimizedEvents.oldEvents.isEmpty()) {
                assert(false) { "The optimization failed" }
            }
            // Signal that the optimization was successful
            countDownLatch.countDown()
        }
        countDownLatch.await(120, java.util.concurrent.TimeUnit.SECONDS)
        // If the countDownLatch is not successful, the test will fail
        assert(countDownLatch.count == 0L) {"Not fast enough"}
    }
}