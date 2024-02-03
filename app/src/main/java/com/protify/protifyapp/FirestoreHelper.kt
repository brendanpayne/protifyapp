package com.protify.protifyapp

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.protify.protifyapp.features.events.Attendee
import com.protify.protifyapp.utils.MapsDurationUtils
import java.time.LocalDateTime

class FirestoreHelper() {
    private val db: FirebaseFirestore = Firebase.firestore

    fun userExists(uid: String, dateCreated: Long, callback: (Boolean) -> Unit) {
        // [START get_document]
        val docRef = db.collection("users").document(uid)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    offlineListener(uid)
                    Log.d("GoogleFirestore", "User exists in Firestore. Offline listener added")
                    callback(true)
                } else {
                    createUserDocument(uid, dateCreated) { userDocumentAdded ->
                        if (userDocumentAdded) {
                            offlineListener(uid)
                            Log.d("GoogleFirestore", "User added to Firestore. Offline listener added")
                            callback(true)
                        } else {
                            callback(false)
                        }
                    }
                }
            }
    }
    private fun createUserDocument(uid: String, dateCreated: Long, callback: (Boolean) -> Unit) {
        // [START add_document]
        val user = hashMapOf(
            "dateCreated" to dateCreated,
            "uid" to uid,
        )
        db.collection("users").document(uid)
            .set(user)
            .addOnSuccessListener { documentReference ->
                userExists(uid, dateCreated) { userExists ->
                    Log.d("GoogleFirestore", "User added successfully to Firestore with uid: $uid")
                    callback(true)
                }
            }
            .addOnFailureListener { e ->
                Log.d("GoogleFirestore", "Error adding user to Firestore", e)
                callback(false)
            }
    }
    fun createEvent (uid: String, event: FirestoreEvent) {
            if (event.validateEvent(event).isEmpty()) {
                val FirestoreEventEntry = hashMapOf(
                    "name" to event.name,
                    "startTime" to event.startTime,
                    "endTime" to event.endTime,
                    "location" to event.location,
                    "description" to event.description,
                    "timeZone" to event.timeZone,
                    "importance" to event.importance,
                    "attendees" to event.attendees,
                    "rainCheck" to event.rainCheck,
                    "isRaining" to event.isRaining,
                    "mapsCheck" to event.mapsCheck,
                    "distance" to event.distance)
                db.collection("users")
                    .document(uid)
                    .collection("events")
                    .document(event.startTime.year.toString())
                    .collection(event.startTime.month.toString())
                    .document()
                    .set(FirestoreEventEntry)
                    .addOnSuccessListener { documentReference ->
                        Log.d("GoogleFirestore", "Document added successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.d("GoogleFirestore", "Error adding document", e)
                    }
            } else {
                Log.d("GoogleFirestore", "Event failed validation with errors: " + event.validateEvent(event))
            }


    }
    fun toggleOfflineOnline(isConnected: Boolean) {
        // [START disable_network]
        if (!isConnected) {
        db.disableNetwork()
            .addOnCompleteListener {
                Log.d("GoogleFirestore", "Network disabled")
                }
            }
        // [END disable_network]

        // [START enable_network]
        if(isConnected) {
            db.enableNetwork()
                .addOnSuccessListener {
                    Log.d("GoogleFirestore", "Network enabled")
                }
                .addOnFailureListener { e ->
                    Log.w("GoogleFirestore", "Error enabling network", e)
                }
        }

        // [END enable_network]
    }
    private fun offlineListener(uid: String) {
        // [START offline_listener]
        db.collection("users")
            .document(uid)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, e ->
                if (e != null) {
                    Log.w("GoogleFirestore", "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.metadata.isFromCache) {
                    Log.d("GoogleFirestore", "Data fetched from local cache")
                } else {
                    Log.d("GoogleFirestore", "Data fetched from server")
                }
            }
    }
    fun getEvents(uid: String, day: String, month: String, year: String, callback: (List<FirestoreEvent>) -> Unit) {
        val upperMonth = month.uppercase()
        db.collection("users")
            .document(uid)
            .collection("events")
            .document(year)
            .collection(upperMonth)
            .whereEqualTo("startTime.dayOfMonth", day.toInt())
            .get()
            .addOnSuccessListener { result ->
                val events = mutableListOf<FirestoreEvent>()
                for (document in result) {
                    Log.d("GoogleFirestore", "${document.id} => ${document.data}")
                    if (!result.isEmpty) {
                        var testAttendee: Attendee = Attendee(
                            name = "Test",
                            email = "Test",
                            phoneNumber = "Test"
                        )
                        val attendeeList: List<Attendee> = listOf(testAttendee)
                        var startTimeHashMap = document.data["startTime"] as HashMap<*, *>
                        var endTimeHashMap = document.data["endTime"] as HashMap<*, *>
                        val startTime = LocalDateTime.of(
                            (startTimeHashMap["year"] as Long).toInt(),
                            (startTimeHashMap["monthValue"] as Long).toInt(),
                            (startTimeHashMap["dayOfMonth"] as Long).toInt(),
                            (startTimeHashMap["hour"] as Long).toInt(),
                            (startTimeHashMap["minute"] as Long).toInt()
                        )
                        val endTime = LocalDateTime.of(
                            (endTimeHashMap["year"] as Long).toInt(),
                            (endTimeHashMap["monthValue"] as Long).toInt(),
                            (endTimeHashMap["dayOfMonth"] as Long).toInt(),
                            (endTimeHashMap["hour"] as Long).toInt(),
                            (endTimeHashMap["minute"] as Long).toInt()
                        )
                        events.add(
                            FirestoreEvent(
                                name = document.data["name"].toString(),
                                startTime = startTime as LocalDateTime,
                                endTime = endTime as LocalDateTime,
                                location = document.data["location"].toString(),
                                description = document.data["description"].toString(),
                                timeZone = document.data["timeZone"].toString(),
                                importance = (document.data["importance"] as Long).toInt(),
                                attendees = attendeeList,
                                rainCheck = (document.data["rainCheck"] as Boolean),
                                isRaining = (document.data["isRaining"] as Boolean),
                                mapsCheck = (document.data["mapsCheck"] as Boolean),
                                distance = (document.data["distance"] as Long).toInt()

                        )
                        )
                    }
                }
                callback(events)
            }
            .addOnFailureListener { exception ->
                Log.w("GoogleFirestore", "Error getting documents.", exception)
            }
    }
   // Returns a list of free time intervals for a given day (to minimize API calls)
//    fun getFreeTime(dayStart: LocalDateTime, dayEnd: LocalDateTime, uid: String, day: String, month: String, year: String, callback: (List<Pair<LocalDateTime, LocalDateTime>>) -> Unit) {
//
//        getEvents(uid, day, month, year) { events ->
//
//            var sortedEvents = events.sortedBy { it.startTime }
//            val freeTimes = mutableListOf<Pair<LocalDateTime, LocalDateTime>>()
//            var currentFreeTimeStart = dayStart
//
//            //Find free time intervals
//            for (event in sortedEvents.withIndex()) {
//                //Get the next event, or null if there are no more events
//                val nextEvent = if (event.index < sortedEvents.lastIndex) sortedEvents[event.index + 1] else null
//                if (nextEvent != null && (event.value.location != nextEvent.location)) {
//                    MapsDurationUtils(event.value.startTime).isChainedEvent(event.value, nextEvent, "6190 Falla Dr, Canal Winchester, OH 43110") { runSuccess, isChained ->
//                        if (event.value.startTime > currentFreeTimeStart) {
//                            freeTimes.add(Pair(currentFreeTimeStart, event.value.startTime)) // Add free time before the event
//                            if (nextEvent != null) {
//
//                                if (isChained && runSuccess) {
//                                    currentFreeTimeStart = nextEvent.endTime
//                                }
//                            }
//                            currentFreeTimeStart = event.value.endTime  // Move to the end of the event
//                        } else {
//                            currentFreeTimeStart = maxOf(currentFreeTimeStart, event.value.endTime) // Ensure start time is after the event
//                        }
//                        // Check for final free time interval
//                        if (currentFreeTimeStart < dayEnd) {
//                            freeTimes.add(Pair(currentFreeTimeStart, dayEnd))
//                        }
//
//                    }
//                }
//            }
//            callback(freeTimes)
//        }
//    }
    fun getFreeTime(dayStart: LocalDateTime, dayEnd: LocalDateTime, uid: String, day: String, month: String, year: String, callback: (List<Pair<LocalDateTime, LocalDateTime>>) -> Unit) {

        getEvents(uid, day, month, year) { events ->

            var sortedEvents = events.sortedBy { it.startTime }
            val freeTimes = mutableListOf<Pair<LocalDateTime, LocalDateTime>>()
            var currentFreeTimeStart = dayStart

            // Recursive function to process events sequentially
            fun processEvents(index: Int) {
                if (index >= sortedEvents.size) {
                    // All events processed, return
                    callback(freeTimes)
                    return
                }

                val event = sortedEvents[index]
                val nextEvent = if (index < sortedEvents.lastIndex) sortedEvents[index + 1] else null
                //If it's the first event, check if its after your day start
                if (index == 0 && event.startTime > dayStart) {
                    freeTimes.add(Pair(dayStart, event.startTime))
                    currentFreeTimeStart = event.endTime
                }
                if (nextEvent != null) {
                    MapsDurationUtils(event.startTime).isChainedEvent(event, nextEvent, "6190 Falla Dr, Canal Winchester, OH 43110") { runSuccess, isChained ->
                        if (event.startTime > currentFreeTimeStart) {

                            if (nextEvent != null && isChained && runSuccess) {
                                currentFreeTimeStart = nextEvent.endTime
                            } else {
                                currentFreeTimeStart = event.endTime // Move to the end of the event
                                freeTimes.add(Pair(currentFreeTimeStart, nextEvent.startTime)) // Add free time before the event

                            }
                        } else {
                            currentFreeTimeStart = maxOf(currentFreeTimeStart, event.endTime) // Ensure start time is after the event
                        }

                        // Check for final free time interval
                        if (currentFreeTimeStart > dayEnd) {
                            freeTimes.add(Pair(currentFreeTimeStart, dayEnd))
                        }

                        processEvents(index + 1) // Move to the next event after callback
                    }
                } else {

                    //Make a new firestore event with the location as home
                    var homeEvent = FirestoreEvent(
                        name = "Home",
                        startTime = event.endTime,
                        endTime = dayEnd,
                        location = "6190 Falla Dr, Canal Winchester, OH 43110",
                        description = "",
                        timeZone = "",
                        importance = 0,
                        attendees = null,
                        rainCheck = false,
                        isRaining = false,
                        mapsCheck = false,
                        distance = 0
                    )
                    MapsDurationUtils(event.startTime).isChainedEvent(event, homeEvent, "6190 Falla Dr, Canal Winchester, OH 43110") { runSuccess, isChained ->
                        if (event.startTime > currentFreeTimeStart) {

                            if (isChained && runSuccess) {
                                currentFreeTimeStart = event.endTime
                            } else {
                                freeTimes.add(Pair(currentFreeTimeStart, event.startTime)) // Add free time before the event
                                currentFreeTimeStart = event.endTime // Move to the end of the event
                            }
                        } else {
                            currentFreeTimeStart = maxOf(currentFreeTimeStart, event.endTime) // Ensure start time is after the event
                        }

                        // If the last event ends after the end of the day, don't add it
                        if (currentFreeTimeStart < dayEnd) {
                            freeTimes.add(Pair(currentFreeTimeStart, dayEnd))
                        }

                        processEvents(index + 1) // Move to the next event after callback
                    }
                }
            }

            // Initiate event processing
            processEvents(0)
        }
    }
    fun getOverlappingEvents(uid: String, day: String, month: String, year: String, callback: (List<FirestoreEvent>) -> Unit) {
        getEvents(uid, day, month, year) { events ->
            val overlappingEvents = mutableListOf<FirestoreEvent>()
            val sortedEvents = events.sortedBy { it.startTime }
            for (event in sortedEvents.withIndex()) {
                val nextEvent = if (event.index < sortedEvents.lastIndex) sortedEvents[event.index + 1] else null
                if (nextEvent != null) {
                    if (event.value.startTime.isBefore(nextEvent.endTime) && event.value.endTime.isAfter(nextEvent.startTime)) {
                        overlappingEvents.add(event.value)
                    }
                }
            }
            callback(overlappingEvents)
        }
    }
}