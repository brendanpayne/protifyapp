package com.protify.protifyapp.tests.features.AITesting

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.protify.protifyapp.FirestoreEvent
import com.protify.protifyapp.FirestoreHelper
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import com.protify.protifyapp.utils.OpenAIHelper.GetAISchedule
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class GetAIScheduleTest {

    companion object {
        var user: FirebaseUser? = null
        var loginHelper = FirebaseLoginHelper()
        val firestoreHelper = FirestoreHelper()
        var eventsList: List<FirestoreEvent> = listOf()
        @JvmStatic
        @BeforeClass
        fun setUp() {
            var loginLatch = CountDownLatch(1)

            // Login
            loginHelper.signIn("tommy.mcreynolds@gmail.com", "testpassword")
                .addOnSuccessListener { loginLatch.countDown() }
            loginLatch.await(10, TimeUnit.SECONDS)

            user = loginHelper.getCurrentUser()

            // Mock events
            val events = listOf<FirestoreEvent>().toMutableList()
            events.add(
                FirestoreEvent(
                    name = "Gym",
                    nameLower = "gym",
                    location = "9099 Basil Western Rd, Canal Winchester, OH 43110",
                    startTime = LocalDateTime.of(2024, 3, 14, 8, 0),
                    endTime = LocalDateTime.of(2024, 3, 14, 9, 0)
                )
            )
            events.add(
                FirestoreEvent(
                    name = "Work",
                    nameLower = "work",
                    location = "1101 Beech Rd SW, New Albany, OH 43054",
                    startTime = LocalDateTime.of(2024, 3, 14, 10, 0),
                    endTime = LocalDateTime.of(2024, 3, 14, 18, 0),
                )
            )
            events.add(
                FirestoreEvent(
                    name = "Dinner",
                    nameLower = "dinner",
                    location = "729 Winchester Pike, Canal Winchester, OH 43110",
                    startTime = LocalDateTime.of(2024, 3, 14, 18, 0),
                    endTime = LocalDateTime.of(2024, 3, 14, 19, 0),
                )
            )
            events.add(
                FirestoreEvent(
                    name = "Grocery Shopping",
                    nameLower = "grocery shopping",
                    location = "8300 Meijer Dr, Canal Winchester, OH 43110",
                    startTime = LocalDateTime.of(2024, 3, 14, 18, 0),
                    endTime = LocalDateTime.of(2024, 3, 14, 19, 30),
                )
            )

            val removeEventsLatch = CountDownLatch(1)
            // Get all of the events for the day
            firestoreHelper.getEvents(
                uid = user!!.uid,
                day = "14",
                month = "March",
                year = "2024"
            ) { events ->
                eventsList = events
                removeEventsLatch.countDown()
            }
            removeEventsLatch.await(15, TimeUnit.SECONDS)
            val deleteLatch = CountDownLatch(eventsList.size)
            for (event in eventsList) {

                firestoreHelper.deleteEvent(user!!.uid, "14", "March", "2024", event) {
                    deleteLatch.countDown()
                }

            }
            removeEventsLatch.await(100, TimeUnit.SECONDS)
            assert(removeEventsLatch.count == 0L) { "Unable to remove all events"}



                val latch = CountDownLatch(4)
                // Get the schedule
                if (user != null) {
                    // Put these mfs in the database
                    for (event in events) {
                        firestoreHelper.createEvent(user!!.uid, event) {
                            if (it) {
                                latch.countDown()
                            }
                        }
                    }
                    // Wait for the events to be added.
                    latch.await(25, TimeUnit.SECONDS)
                    assert(latch.count == 0L) { "One or more of the events were not added to the database" }

                } else {
                    assert(false) { "No user found" }
                }

        }

        @JvmStatic
        @AfterClass
        fun tearDown() {
            val latch = CountDownLatch(1)
            // Get all of the events for the day
            firestoreHelper.getEvents(
                uid = user!!.uid,
                day = "14",
                month = "March",
                year = "2024"
            ) { events ->
                eventsList = events
                latch.countDown()
            }
            latch.await(15, TimeUnit.SECONDS)
            val deleteLatch = CountDownLatch(eventsList.size)
            for (event in eventsList) {

                firestoreHelper.deleteEvent(user!!.uid, "14", "March", "2024", event) {
                    deleteLatch.countDown()
                }

            }
            latch.await(100, TimeUnit.SECONDS)
            assert(latch.count == 0L) { "Unable to remove all events"}
        }
    }
    @Test
    fun testGetSchedule() {
        // init count down latch
        var latch = CountDownLatch(1)
        if (user != null) {
            GetAISchedule(user!!.uid, "6190 Falla Drive, Canal Winchester OH, 43110").getSchedule(
                LocalDateTime.of(2024, 3, 14, 0, 0) // March 14th, 2024
            ) { events, _ ->
                assert(events.isNotEmpty()) { "No events found" }
                eventsList = events
                latch.countDown()
            }

        } else {
            assert(false) { "No User Found" }
        }
        latch.await(15, TimeUnit.SECONDS)
        assert(latch.count == 0L)
    }
    @Test
    fun testGetOptimizedSchedule() {
        // init count down latch
        var latch = CountDownLatch(1)
        if (user != null) {
            GetAISchedule(user!!.uid, "6190 Falla Drive, Canal Winchester OH, 43110").getOptimizedSchedule(
                LocalDateTime.of(2024, 3, 14, 0, 0) // March 14th, 2024
            ) { success ->
                assert(success) { "Optimization failed" }
                latch.countDown()
            }

        } else {
            assert(false) { "No User Found" }
        }
        latch.await(100, TimeUnit.SECONDS)
        // Assert the new events are in the database
        val newEventsLatch = CountDownLatch(1)
        // make a list of ai events
        var aiEventList: List<FirestoreEvent> = listOf()
        firestoreHelper.getAIGeneratedEvents(
            uid = user!!.uid,
            day = "14",
            month = "March",
            year = "2024"
        ) { events ->
            assert(events.isNotEmpty()) { "No events found" }
            aiEventList = events
            newEventsLatch.countDown()
        }
        // Assert success and the events are in the database
        newEventsLatch.await(15, TimeUnit.SECONDS)
        Log.d("GetAIScheduleTest", "AI Events: ${aiEventList}")
        assert(latch.count == 0L) { "Events not found in the database" }
    }

}