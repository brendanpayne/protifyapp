package com.protify.protifyapp.tests.features.Firebase

import com.protify.protifyapp.FirestoreEvent
import com.protify.protifyapp.FirestoreHelper
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch

class AcceptAIEvent {

    companion object {

        val today = LocalDateTime.of(2024, 1, 22, 0, 0) // January 22, 2024
        var user = FirebaseLoginHelper().getCurrentUser()
        @JvmStatic
        @BeforeClass
        fun addEvents() {
            if (user != null) {
                if (user!!.email == "tommy.mcreynolds@gmail.com") {
                } else {
                    FirebaseLoginHelper().signOut()
                    FirebaseLoginHelper().signIn("tommy.mcreynolds@gmail.com", "testpassword").addOnSuccessListener { user = FirebaseLoginHelper().getCurrentUser()}

                }
            } else {
                FirebaseLoginHelper().signIn("tommy.mcreynolds@gmail.com", "testpassword").addOnSuccessListener { user = FirebaseLoginHelper().getCurrentUser()}
            }
            // events count down latch
            val eventsCountDownLatch = CountDownLatch(3)
            val aiEventsCountDownLatch = CountDownLatch(3)
            // List of events
            val events = listOf(
                FirestoreEvent(
                    name = "Gym",
                    nameLower = "gym",
                    startTime = today.withHour(8),
                    endTime = today.withHour(9),
                ),
                FirestoreEvent(
                    name = "Meeting",
                    nameLower = "meeting",
                    startTime = today.withHour(10),
                    endTime = today.withHour(11),
                ),
                FirestoreEvent(
                    name = "Lunch",
                    nameLower = "lunch",
                    startTime = today.withHour(12),
                    endTime = today.withHour(13),
                ),
            )
            val AIGeneratedEvent = listOf( // Simulating AI generated event
                FirestoreEvent(
                    name = "Gym",
                    nameLower = "gym",
                    startTime = today.withHour(10),
                    endTime = today.withHour(11),
                    isAiSuggestion = true,
                ),
                FirestoreEvent(
                    name = "Meeting",
                    nameLower = "meeting",
                    startTime = today.withHour(12),
                    endTime = today.withHour(13),
                    isAiSuggestion = true,
                ),
                FirestoreEvent(
                    name = "Lunch",
                    nameLower = "lunch",
                    startTime = today.withHour(14),
                    endTime = today.withHour(15),
                    isAiSuggestion = true,
                ),
            )
            // Delete event count down latch job
            val deleteEventCountDownLatchJob = CountDownLatch(1)
            // First delete all events
            FirestoreHelper().getEventsAndIds(user!!.uid, today.dayOfMonth.toString(), today.month.toString(), today.year.toString()) {
                val deleteEventCountDownLatch = CountDownLatch(it.keys.size)
                if (it.keys.size > 0) {
                    for (event in it) {
                        FirestoreHelper().deleteEventById(user!!.uid, today.month.toString(), today.year.toString(), event.value) {deletedEventSuccess ->
                            if (deletedEventSuccess) {
                                deleteEventCountDownLatch.countDown()
                            }
                        }
                    }
                    deleteEventCountDownLatch.await(30, java.util.concurrent.TimeUnit.SECONDS)
                    if (deleteEventCountDownLatch.count == 0L) {
                        deleteEventCountDownLatchJob.countDown()
                    } else {
                        assert(false) {"Events not deleted"}
                    }
                }
                deleteEventCountDownLatchJob.countDown()
            }
            deleteEventCountDownLatchJob.await(45, java.util.concurrent.TimeUnit.SECONDS)
            assert(deleteEventCountDownLatchJob.count == 0L) {"Events not deleted"}
            runBlocking { delay(10000) } // Wait 10 seconds for firestore to process the events
            // Put all of the events into firestore
            for (event in events) {
                FirestoreHelper().createEvent(user!!.uid, event) {
                    if (it) {
                        eventsCountDownLatch.countDown()
                    } else {
                        assert(false) {"Event not created"}
                    }
                }
            }
            // Put all of the AI generated events into firestore
            for (event in AIGeneratedEvent) {
                FirestoreHelper().createEvent(user!!.uid, event) {
                    if (it) {
                        aiEventsCountDownLatch.countDown()
                    } else {
                        assert(false) {"AI generated event not created"}
                    }
                }
            }
            // Wait for all events to be put into firestore
            eventsCountDownLatch.await(45, java.util.concurrent.TimeUnit.SECONDS)
            aiEventsCountDownLatch.await(45, java.util.concurrent.TimeUnit.SECONDS)
            // wait 10 seconds for firestore to process the events
            runBlocking { delay(10000) }
            assert(eventsCountDownLatch.count == 0L) {"Events not created"}
            assert(aiEventsCountDownLatch.count == 0L) {"AI generated events not created"}

        }
    }
    @Test
    fun acceptAIEvent() {
        // Count down latch for fetching events
        val eventsCountDownLatch = CountDownLatch(1)

        // make an empty event list
        val events = mutableListOf<FirestoreEvent>()
        // Get the events from Firestore
        FirestoreHelper().getEvents(user!!.uid, today.dayOfMonth.toString(), today.month.toString(), today.year.toString()) {
            events.addAll(it) // Add the events to the list
            eventsCountDownLatch.countDown() // Count down the latch
        }
        // Wait for the events to be fetched
        eventsCountDownLatch.await(30, java.util.concurrent.TimeUnit.SECONDS)
        assert (eventsCountDownLatch.count == 0L) {"Events not fetched"}
        // Get all of the AI generated events
        val aiEvents = events.filter { it.isAiSuggestion }

        // Count down latch for accepting AI generated events
        val acceptAIGeneratedEventsCountDownLatch = CountDownLatch(aiEvents.size)
        // Accept all of the AI generated events
        for (aiEvent in aiEvents) {
            FirestoreHelper().acceptAIGeneratedEvent(user!!.uid, aiEvent) {
                acceptAIGeneratedEventsCountDownLatch.countDown()
            }
        }
        // Wait for all AI generated events to be accepted
        acceptAIGeneratedEventsCountDownLatch.await(30, java.util.concurrent.TimeUnit.SECONDS)
        runBlocking { delay(10000) } // Wait 10 seconds for firestore to process the events
        // Count down latch for fetching events
        val acceptedEventsCountDownLatch = CountDownLatch(1)
        // accepted events list
        var acceptedEvents = mutableListOf<FirestoreEvent>()
        FirestoreHelper().getEvents(user!!.uid, today.dayOfMonth.toString(), today.month.toString(), today.year.toString()) {
            acceptedEvents = it.filter { it.isAiSuggestion && it.isUserAccepted }.toMutableList()
            acceptedEventsCountDownLatch.countDown()
        }
        // Wait for the accepted events to be fetched
        acceptedEventsCountDownLatch.await(45, java.util.concurrent.TimeUnit.SECONDS)
        assert (acceptedEventsCountDownLatch.count == 0L) {"Accepted events not fetched"}
        assert(acceptedEvents.size == aiEvents.size) {"Not all AI generated events accepted"}
    }
}