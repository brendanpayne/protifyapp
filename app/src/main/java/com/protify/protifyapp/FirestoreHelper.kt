package com.protify.protifyapp

import OptimizedSchedule
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
                    "nameLower" to event.nameLower,
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
                    "distance" to event.distance,
                    "isOutside" to event.isOutside)
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
                    //Need to refactor this. I don't think creating an attendee is needed
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
                                distance = (document.data["distance"] as Long).toInt(),
                                nameLower = document.data["nameLower"].toString(),
                                isOutside = (document.data["isOutside"] as Boolean)

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
    fun getEventsAndIds(uid: String, day: String, month: String, year: String, callback: (HashMap<FirestoreEvent, String>) -> Unit) {
        val upperMonth = month.uppercase()
        //Hashmap of the events and their id

        db.collection("users")
            .document(uid)
            .collection("events")
            .document(year)
            .collection(upperMonth)
            .whereEqualTo("startTime.dayOfMonth", day.toInt())
            .get()
            .addOnSuccessListener { result ->
                val events = hashMapOf<FirestoreEvent, String>()
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
                            val firestoreEvent = FirestoreEvent(
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
                                distance = (document.data["distance"] as Long).toInt(),
                                nameLower = document.data["nameLower"].toString(),
                                isOutside = (document.data["isOutside"] as Boolean)
                            )
                        events[firestoreEvent] = document.id

                    }
                }
                callback(events)
            }
            .addOnFailureListener { exception ->
                Log.w("GoogleFirestore", "Error getting documents.", exception)
            }
    }
    fun getEvent(uid: String, day: String, month: String, year: String,name: String, callback: (String?, FirestoreEvent) -> Unit ) {
        val upperMonth = month.uppercase()
        //Hopefully using lowercase name will make this more accurate
        val nameLower= name.lowercase().trim()
        db.collection("users")
            .document(uid)
            .collection("events")
            .document(year)
            .collection(upperMonth)
            .whereEqualTo("startTime.dayOfMonth", day.toInt())
            .whereEqualTo("nameLower", nameLower)
            .get()
            .addOnSuccessListener { result ->
                if (result.size() == 1) {
                    //Return the event and the id of the event
                    for (document in result) {
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
                        val event = FirestoreEvent(
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
                            distance = (document.data["distance"] as Long).toInt(),
                            nameLower = document.data["nameLower"].toString(),
                            isOutside = (document.data["isOutside"] as Boolean)
                        )
                        callback(document.id, event)
                    }
                }
                //If it finds more than one document, callback null and empty event
                else {
                    callback(null, FirestoreEvent(
                        name = "",
                        startTime = LocalDateTime.now(),
                        endTime = LocalDateTime.now(),
                        location = "",
                        description = "",
                        timeZone = "",
                        importance = 0,
                        attendees = null,
                        rainCheck = false,
                        isRaining = false,
                        mapsCheck = false,
                        distance = 0,
                        nameLower = "",
                        isOutside = false
                    ))
                }

            }
            .addOnFailureListener { exception ->
                //Don't callback if there is an error
                Log.w("GoogleFirestore", "Error getting documents.", exception)
            }
    }
    //This takes a firestoreevent object and overwrites the event with the same id
    fun modifyEvent(uid: String, eventId: String, event: FirestoreEvent, callback: (Boolean) -> Unit) {
        if (event.validateEvent(event).isEmpty()) {
            val FirestoreEventEntry = hashMapOf(
                "name" to event.name,
                //Should be exactly same as the event name, except lowercase for searching
                "nameLower" to event.nameLower.trim().lowercase(),
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
                "distance" to event.distance,
                "isOutside" to event.isOutside)
            db.collection("users")
                .document(uid)
                .collection("events")
                .document(event.startTime.year.toString())
                .collection(event.startTime.month.toString())
                .document(eventId)
                .set(FirestoreEventEntry)
                .addOnSuccessListener { documentReference ->
                    Log.d("GoogleFirestore", "Document modified successfully")
                    callback(true)
                }
                .addOnFailureListener { e ->
                    Log.d("GoogleFirestore", "Error modifying document", e)
                    callback(false)
                }
        } else {
            Log.d("GoogleFirestore", "Event failed validation with errors: " + event.validateEvent(event))
            callback(false)
        }
    }
    fun modifyEventTime(OptimizedEvents: OptimizedSchedule, uid: String, day: String, month:String, year: String, callback: (Boolean) -> Unit) {
        //This will determine how many events we have to update
        val eventCount = OptimizedEvents.events.size

        fun modifySingletonEvent(iterations: Int) {
            //If we have modified all of the events, return
            if (iterations >= eventCount) {
                callback(true)
                return
            }
            getEvent(uid, day, month, year, OptimizedEvents.events[iterations].name) { eventId, event ->
                //If the event is found, modify it
                if (eventId != null) {
                    //This is essentially parsing the string output from the class and turning it into a localdatetime object
                    val startTimeString = OptimizedEvents.events[iterations].startTime
                    val startTimeParsed = startTimeString.split(":")
                    val startHour = startTimeParsed[0].toInt()
                    val startMinute = startTimeParsed[1].toInt()
                    val endTimeString = OptimizedEvents.events[iterations].endTime
                    val endTimeParsed = endTimeString.split(":")
                    val endHour = endTimeParsed[0].toInt()
                    val endMinute = endTimeParsed[1].toInt()
                    //Set new start and end times for events now that we have the integers associated with the start and end times
                    val startTime = LocalDateTime.of(
                        event.startTime.year,
                        event.startTime.month,
                        event.startTime.dayOfMonth,
                        startHour,
                        startMinute
                    )
                    val endTime = LocalDateTime.of(
                        event.endTime.year,
                        event.endTime.month,
                        event.endTime.dayOfMonth,
                        endHour,
                        endMinute
                    )
                    //Reassign start and end times to events
                    event.startTime = startTime
                    event.endTime = endTime

                    modifyEvent(uid, eventId, event) { success ->
                        if (success) {
                            modifySingletonEvent(iterations + 1)
                        } else {
                            callback(false)
                        }
                    }
                } else {
                    //If the event is not found, return false
                    callback(false)
                }
            }
        }
        //Initiate the recursive function
        modifySingletonEvent(0)
    }
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
                        distance = 0,
                        nameLower = "",
                        isOutside = false
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