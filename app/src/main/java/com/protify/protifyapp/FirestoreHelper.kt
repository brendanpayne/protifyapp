package com.protify.protifyapp

import OptimizedSchedule
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.protify.protifyapp.features.events.Attendee
import com.protify.protifyapp.utils.MapsDurationUtils
import com.protify.protifyapp.utils.OpenAIHelper.ParseTime
import java.time.LocalDateTime
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
                            Log.d(
                                "GoogleFirestore",
                                "User added to Firestore. Offline listener added"
                            )
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
            "homeAddress" to "",
            "use4" to false,
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

    /** This function will create an event in the Firestore database.
     * @param uid: The user's uid
     * @param event: The event to be created
     * @return A callback function that will return true if the event was created successfully, and false if it was not
     */
    fun createEvent(uid: String, event: FirestoreEvent, callback: (Boolean) -> Unit) {
        if (event.validateEvent(event).isEmpty()) {
            // Trim and lowercase the name for searching
            val nameLower = event.name.trim().lowercase()
            val FirestoreEventEntry = hashMapOf(
                "name" to event.name,
                "nameLower" to nameLower,
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
                "isOutside" to event.isOutside,
                "isOptimized" to event.isOptimized,
                "isAiSuggestion" to event.isAiSuggestion,
                "isUserAccepted" to event.isUserAccepted
            )
            db.collection("users")
                .document(uid)
                .collection("events")
                .document(event.startTime.year.toString())
                .collection(event.startTime.month.toString())
                .document()
                .set(FirestoreEventEntry)
                .addOnSuccessListener { documentReference ->
                    Log.d("GoogleFirestore", "Document added successfully")
                    callback(true)
                }
                .addOnFailureListener { e ->
                    Log.d("GoogleFirestore", "Error adding document", e)
                    callback(false)
                }
        } else {
            Log.d(
                "GoogleFirestore",
                "Event failed validation with errors: " + event.validateEvent(event)
            )
        }


    }

    fun toggleOfflineOnline(isConnected: Boolean) {
        if (!isConnected) {
            db.disableNetwork()
                .addOnCompleteListener {
                    Log.d("GoogleFirestore", "Network disabled")
                }
        } else {
            db.enableNetwork()
                .addOnSuccessListener {
                    Log.d("GoogleFirestore", "Network enabled")
                }
                .addOnFailureListener { e ->
                    Log.w("GoogleFirestore", "Error enabling network", e)
                }
        }
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

    fun getEvents(
        uid: String,
        day: String,
        month: String,
        year: String,
        callback: (List<FirestoreEvent>) -> Unit
    ) {

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
                        // Map the FirestoreEvent from the database to a FirestoreEvent object
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
                                isOutside = (document.data["isOutside"] as Boolean),
                                isOptimized = (document.data["isOptimized"] as Boolean),
                                isAiSuggestion = (document.data["isAiSuggestion"] as Boolean),
                                isUserAccepted = (document.data["isUserAccepted"] as Boolean)

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

    fun getEventsAndIds(
        uid: String,
        day: String,
        month: String,
        year: String,
        callback: (HashMap<FirestoreEvent, String>) -> Unit
    ) {
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
                            isOutside = (document.data["isOutside"] as Boolean),
                            isOptimized = (document.data["isOptimized"] as Boolean),
                            isAiSuggestion = (document.data["isAiSuggestion"] as Boolean),
                            isUserAccepted = (document.data["isUserAccepted"] as Boolean)
                        )
                        firestoreEvent.id = document.id

                        events[firestoreEvent] = document.id

                    }
                }
                callback(events)
            }
            .addOnFailureListener { exception ->
                Log.w("GoogleFirestore", "Error getting documents.", exception)
            }
    }

    fun getEvent(
        uid: String,
        day: String,
        month: String,
        year: String,
        name: String,
        callback: (String?, FirestoreEvent) -> Unit
    ) {
        val upperMonth = month.uppercase()
        //Hopefully using lowercase name will make this more accurate
        val nameLower = name.lowercase().trim()
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
                            isOutside = (document.data["isOutside"] as Boolean),
                            isOptimized = (document.data["isOptimized"] as Boolean),
                            isAiSuggestion = (document.data["isAiSuggestion"] as Boolean),
                            isUserAccepted = (document.data["isUserAccepted"] as Boolean)
                        )
                        callback(document.id, event)
                    }
                }
                //If it finds more than one document, callback null and empty event
                else {
                    callback(
                        null, FirestoreEvent(
                            name = "",
                            startTime = LocalDateTime.now(),
                            endTime = LocalDateTime.now(),
                            distance = 0,
                            nameLower = ""
                        )
                    )
                }

            }
            .addOnFailureListener { exception ->
                callback(
                    null, FirestoreEvent(
                        name = "",
                        startTime = LocalDateTime.now(),
                        endTime = LocalDateTime.now(),
                        distance = 0,
                        nameLower = ""
                    )
                )
                Log.w("GoogleFirestore", "Error getting documents.", exception)
            }
    }

    /** This function will delete an event from the Firestore database by querying the event's name.
     * @param uid: The user's uid
     * @param day: The day of the event
     * @param month: The month of the event
     * @param year: The year of the event
     * @param name: The name of the event
     * @return A callback function that will return true if the event was deleted successfully, and false if it was not
     */
    fun deleteEvent(
        uid: String,
        day: String,
        month: String,
        year: String,
        name: String,
        callback: (Boolean) -> Unit
    ) {
        getEvent(uid, day, month, year, name) { eventId, event ->
            // If the event can be found
            if (eventId != null) {
                db.collection("users")
                    .document(uid)
                    .collection("events")
                    .document(year)
                    .collection(month)
                    .document(eventId)
                    .delete()
                    .addOnSuccessListener {
                        Log.d("GoogleFirestore", "DocumentSnapshot successfully deleted!")
                        callback(true)
                    }
                    .addOnFailureListener { e ->
                        Log.w("GoogleFirestore", "Error deleting document", e)
                        callback(false)
                    }
            }
        }

    }
    /** This overloaded function will delete an event from the Firestore database by querying the event's name, isAiSuggestion, and isUserAccepted.
     * @param uid: The user's uid
     * @param day: The day of the event
     * @param month: The month of the event
     * @param year: The year of the event
     * @param FirebaseEvent: The event to be deleted
     * @return A callback function that will return true if the event was deleted successfully, and false if it was not
     */
    fun deleteEvent(
        uid: String,
        day: String,
        month: String,
        year: String,
        FirebaseEvent: FirestoreEvent,
        callback: (Boolean) -> Unit
    ) {
        // Get the uppercased month
        val upperMonth = month.uppercase()
        db.collection("users")
            .document(uid)
            .collection("events")
            .document(year)
            .collection(upperMonth)
            .whereEqualTo("startTime.dayOfMonth", day.toInt())
            .whereEqualTo("isUserAccepted", FirebaseEvent.isUserAccepted)
            .whereEqualTo("isAiSuggestion", FirebaseEvent.isAiSuggestion)
            .whereEqualTo("nameLower", FirebaseEvent.nameLower)
            .get()
            .addOnSuccessListener { result ->
                if (result.size() != -1) {
                    for (document in result) {
                        db.collection("users")
                            .document(uid)
                            .collection("events")
                            .document(year)
                            .collection(upperMonth)
                            .document(document.id)
                            .delete()
                            .addOnSuccessListener {
                                Log.d("GoogleFirestore", "DocumentSnapshot successfully deleted!")
                                callback(true)
                            }
                            .addOnFailureListener { e ->
                                Log.w("GoogleFirestore", "Error deleting document", e)
                                callback(false)
                            }
                    }
                } else {
                    Log.w("GoogleFirestore", "More than one document found")
                    callback(false)
                }
            }.addOnFailureListener {
                Log.w("GoogleFirestore", "EXCEPTION: $it")
                callback(false)
            }

    }
    @Deprecated ("success and failure listeners are not working properly... Doesn't call back")
    /** Delete an event by ID
     * @param uid: The user's uid
     * @param day: The day of the event
     * @param month: The month of the event (MONTH NAME NOT INT CONVERTED TO STRING!!)
     * @param year: The year of the event
     * @param eventId: The id of the event
     * @return A callback function that will return true if the event was deleted successfully, and false if it was not
     */
    fun deleteEventById(
        uid: String,
        month: String,
        year: String,
        eventId: String,
        callback: (Boolean) -> Unit
    ) {
        // Get the uppercased month
        val upperMonth = month.uppercase()
        db.collection("users")
            .document(uid)
            .collection("events")
            .document(year)
            .collection(upperMonth)
            .document(eventId)
            .delete()
            .addOnSuccessListener {
                Log.d("GoogleFirestore", "DocumentSnapshot successfully deleted!")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.w("GoogleFirestore", "Error deleting document", e)
                callback(false)
            }
    }

    /** This function will delete all of the events for the day generated by the AI but not accepted by the user
     * @param uid: The user's uid
     * @param day: The day of the event
     * @param month: The month of the event (MONTH NAME NOT INT CONVERTED TO STRING!!)
     * @param year: The year of the event
     * @return A callback function that will return true if and when all of the events were successfully deleted, and false if they were not
     */
    fun deleteAIGeneratedEvents(
        uid: String,
        day: String,
        month: String,
        year: String,
        callback: (Boolean) -> Unit
    ) {
        // Get the uppercased month
        val upperMonth = month.uppercase()
        db.collection("users")
            .document(uid)
            .collection("events")
            .document(year)
            .collection(upperMonth)
            .whereEqualTo("startTime.dayOfMonth", day.toInt())
            .whereEqualTo("isAiSuggestion", true)
            .whereEqualTo("isUserAccepted", false) // Delete only the ones that the user has not accepted
            .get()
            .addOnSuccessListener { result ->
                if (result.size() != -1) { // if there are results
                    var deletedDocuments = result.documents.size
                    for (document in result) {
                        db.collection("users")
                            .document(uid)
                            .collection("events")
                            .document(year)
                            .collection(upperMonth)
                            .document(document.id)
                            .delete()
                            .addOnSuccessListener {
                                Log.d("GoogleFirestore", "DocumentSnapshot successfully deleted!")
                                deletedDocuments--
                                if (deletedDocuments == 0) { // Callback After all documents are deleted
                                    callback(true)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.w("GoogleFirestore", "Error deleting document", e)
                                callback(false)
                            }
                    }
                } else {
                    Log.w("GoogleFirestore", "More than one document found")
                    callback(false)
                }
            }.addOnFailureListener {
                Log.w("GoogleFirestore", "EXCEPTION: $it")
                callback(false)
            }

    }

    //This takes a firestoreevent object and overwrites the event with the same id
    fun modifyEvent(
        uid: String,
        eventId: String,
        event: FirestoreEvent,
        callback: (Boolean) -> Unit
    ) {
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
                "isOutside" to event.isOutside,
                "isOptimized" to event.isOptimized,
                "isAiSuggestion" to event.isAiSuggestion,
                "isUserAccepted" to event.isUserAccepted
            )
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
            Log.d(
                "GoogleFirestore",
                "Event failed validation with errors: " + event.validateEvent(event)
            )
            callback(false)
        }
    }

    fun modifyEventTime(
        OptimizedEvents: OptimizedSchedule,
        uid: String,
        day: String,
        month: String,
        year: String,
        callback: (Boolean) -> Unit
    ) {
        //This will determine how many events we have to update
        val eventCount = OptimizedEvents.events.size

        fun modifySingletonEvent(iterations: Int) {
            //If we have modified all of the events, return
            if (iterations >= eventCount) {
                callback(true)
                return
            }
            getEvent(
                uid,
                day,
                month,
                year,
                OptimizedEvents.events[iterations].name
            ) { eventId, event ->
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

    fun getFreeTime(
        dayStart: LocalDateTime,
        dayEnd: LocalDateTime,
        uid: String,
        day: String,
        month: String,
        year: String,
        callback: (List<Pair<LocalDateTime, LocalDateTime>>) -> Unit
    ) {

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
                val nextEvent =
                    if (index < sortedEvents.lastIndex) sortedEvents[index + 1] else null
                //If it's the first event, check if its after your day start
                if (index == 0 && event.startTime > dayStart) {
                    freeTimes.add(Pair(dayStart, event.startTime))
                    currentFreeTimeStart = event.endTime
                }
                if (nextEvent != null) {
                    MapsDurationUtils(event.startTime).isChainedEvent(
                        event,
                        nextEvent,
                        "6190 Falla Dr, Canal Winchester, OH 43110"
                    ) { runSuccess, isChained ->
                        if (event.startTime > currentFreeTimeStart) {

                            if (nextEvent != null && isChained && runSuccess) {
                                currentFreeTimeStart = nextEvent.endTime
                            } else {
                                currentFreeTimeStart = event.endTime // Move to the end of the event
                                freeTimes.add(
                                    Pair(
                                        currentFreeTimeStart,
                                        nextEvent.startTime
                                    )
                                ) // Add free time before the event

                            }
                        } else {
                            currentFreeTimeStart = maxOf(
                                currentFreeTimeStart,
                                event.endTime
                            ) // Ensure start time is after the event
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
                        nameLower = "",
                    )
                    MapsDurationUtils(event.startTime).isChainedEvent(
                        event,
                        homeEvent,
                        "6190 Falla Dr, Canal Winchester, OH 43110"
                    ) { runSuccess, isChained ->
                        if (event.startTime > currentFreeTimeStart) {

                            if (isChained && runSuccess) {
                                currentFreeTimeStart = event.endTime
                            } else {
                                freeTimes.add(
                                    Pair(
                                        currentFreeTimeStart,
                                        event.startTime
                                    )
                                ) // Add free time before the event
                                currentFreeTimeStart = event.endTime // Move to the end of the event
                            }
                        } else {
                            currentFreeTimeStart = maxOf(
                                currentFreeTimeStart,
                                event.endTime
                            ) // Ensure start time is after the event
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

    fun getOverlappingEvents(
        uid: String,
        day: String,
        month: String,
        year: String,
        callback: (List<FirestoreEvent>) -> Unit
    ) {
        getEvents(uid, day, month, year) { events ->
            val overlappingEvents = mutableListOf<FirestoreEvent>()
            val sortedEvents = events.sortedBy { it.startTime }
            for (event in sortedEvents.withIndex()) {
                val nextEvent =
                    if (event.index < sortedEvents.lastIndex) sortedEvents[event.index + 1] else null
                if (nextEvent != null) {
                    if (event.value.startTime.isBefore(nextEvent.endTime) && event.value.endTime.isAfter(
                            nextEvent.startTime
                        )
                    ) {
                        overlappingEvents.add(event.value)
                    }
                }
            }
            callback(overlappingEvents)
        }
    }

    fun deleteUser(uid: String, callback: (Boolean) -> Unit) {
        val userDocRef = db.collection("users").document(uid)

        // Delete all documents in the 'events' subcollection
        userDocRef.collection("events").get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Begin a new batch
                    val batch = db.batch()

                    // Iterate over each document and add it to the batch to be deleted
                    for (documentSnapshot in task.result!!) {
                        batch.delete(documentSnapshot.reference)
                    }

                    // Commit the batch
                    batch.commit().addOnCompleteListener {
                        // Now delete the user document
                        userDocRef.delete()
                            .addOnSuccessListener {
                                Log.d("GoogleFirestore", "User document and all subdocuments deleted successfully")
                                callback(true)
                            }
                            .addOnFailureListener { e ->
                                Log.d("GoogleFirestore", "Error deleting user document and subdocuments", e)
                                callback(false)
                            }
                    }
                } else {
                    Log.d("GoogleFirestore", "Error getting subdocuments", task.exception)
                    callback(false)
                }
            }
    }

    /** This function will import the events from the AI generated schedule into the Firestore database
     * @param optimizedSchedule: The AI generated schedule
     * @param today: The current date of the events being optimized
     * @param uid: The user's uid
     * @param callback: A callback function that will return true if the all events were imported successfully, and false if they were not
     */
   fun importAIGeneratedEvent(
    optimizedSchedule: OptimizedSchedule,
    today: LocalDateTime,
    uid: String,
    callback: (Boolean) -> Unit
) {
    // Make sure old events and new events are different
    if (optimizedSchedule.events == optimizedSchedule.oldEvents) {
        callback(false)
        return
    }

    var importedEvents = 0

    for (optimizedEvent in optimizedSchedule.events) {
        // Get the event from the db
        getEvent(
            uid,
            today.dayOfMonth.toString(),
            today.month.toString(),
            today.year.toString(),
            optimizedEvent.name,
        ) { eventId, event ->
            // If the event is found, modify it
            if (eventId != null) {
                event.startTime = ParseTime().parseTime(optimizedEvent.startTime, today)
                event.endTime = ParseTime().parseTime(optimizedEvent.endTime, today)
                event.isAiSuggestion = true
                event.isUserAccepted = false

                // Shove the event back into the database :D
                createEvent(uid, event) { success ->
                    if (success) {
                        importedEvents++
                        if (importedEvents == optimizedSchedule.events.size) {
                            callback(true)
                        }
                    } else {
                        callback(false)
                        return@createEvent
                    }
                }

            }
            else { // Event ID was null either because there was more than one event with that name or other
                callback(false)
                return@getEvent
            }

        }
    }
}

    /** This function will get all of the AI generated events for a given day that the user has not accepted.
     * @param uid: The user's uid
     * @param day: The day of the event
     * @param month: The month of the event
     * @param year: The year of the event
     * @param callback: A callback function that will return a list of FirestoreEvents
     */
    fun getAIGeneratedEvents(
        uid: String,
        day: String,
        month: String,
        year: String,
        callback: (List<FirestoreEvent>) -> Unit
    ) {
        getEvents(uid, day, month, year) { events ->
            // Get all of the events for the day that are AI generated and not accepted by the user
            val aiGeneratedEvents = events.filter { it.isAiSuggestion && !it.isUserAccepted}
            callback(aiGeneratedEvents)

        }
    }
    /** This function will add the AI generated event to the database and set the isUserAccepted value to true, then delete the old event
     * @param uid: The user's uid
     * @param event: The event to be accepted THIS MUST BE THE AI GENERATED EVENT, NOT THE USER GENERATED EVENT
     * @param callback: A callback function that will return true if the event was accepted successfully, and false if it was not
     */
    fun acceptAIGeneratedEvent(
        uid: String,
        event: FirestoreEvent,
        callback : (Boolean) -> Unit
    )
    {
        // Store the event in the database with the isUserAccepted value set to true
        event.isUserAccepted = true
        createEvent(uid, event) { createEvent ->
            if (createEvent) {
               // If the event is successfully imported, then delete the old event AI generated event
                event.isUserAccepted = false
                deleteEvent(uid, event.startTime.dayOfMonth.toString(), event.startTime.month.toString(), event.startTime.year.toString(), event) { deleteEvent ->
                    // If the event is successfully deleted, return true
                    if (deleteEvent) {
                        event.isAiSuggestion = false
                        // Delete the old event
                        deleteEvent(uid, event.startTime.dayOfMonth.toString(), event.startTime.month.toString(), event.startTime.year.toString(), event) { deleteUserEvent ->
                            if (deleteUserEvent) {
                                callback(true)
                            } else {
                                callback(false)
                            }
                        }
                        callback(true)
                    } else {
                        callback(false)
                    }
                }
            } else {
                callback(false)
            }
        }
    }
    /** This function will remove duplicate events from the database for a given day
     * @param uid: The user's uid
     * @param day: The day of the event
     * @param month: The month of the event
     * @param year: The year of the event
     * @param callback: A callback function that will return true if the events were removed successfully, and false if there are no duplicate events
     */
    fun removeDuplicateEvents(
        uid: String,
        day: String,
        month: String,
        year: String,
        callback: (Boolean) -> Unit
    ) {
        // Get all of the events for the day
        getEvents(uid, day, month, year) { events ->

            // Get only distinct events
            val distinctEvents = events.distinct()
            // Get the events that are not distinct
            val duplicateEvents = events - distinctEvents.toSet()
            // Delete the duplicate events
            for (event in duplicateEvents) {
                db.collection("users")
                    .document(uid)
                    .collection("events")
                    .document(year)
                    .collection(month)
                    .whereEqualTo("nameLower", event.nameLower)
                    .whereEqualTo("isAiSuggestion", event.isAiSuggestion)
                    .whereEqualTo("isUserAccepted", event.isUserAccepted)
                    .whereEqualTo("isOptimized", event.isOptimized)
                    .whereEqualTo("isOutside", event.isOutside)
                    .whereEqualTo("mapsCheck", event.mapsCheck)
                    .whereEqualTo("isRaining", event.isRaining)
                    .whereEqualTo("rainCheck", event.rainCheck)
                    .whereEqualTo("startTime.dayOfMonth", event.startTime.dayOfMonth)
                    .whereEqualTo("startTime.hour", event.startTime.hour)
                    .whereEqualTo("startTime.minute", event.startTime.minute)
                    .whereEqualTo("endTime.dayOfMonth", event.endTime.dayOfMonth)
                    .whereEqualTo("endTime.hour", event.endTime.hour)
                    .whereEqualTo("endTime.minute", event.endTime.minute)
                    .get()
                    .addOnSuccessListener { result ->
                        // If more than one event is found, delete one of them
                        if (result.size() > 1) {
                            // Delete all of the events except for the first one
                            // init int to count the number of events that were successfully deleted
                            var deletedEvents = 0
                            for (i in 1 until result.size()) {
                                db.collection("users")
                                    .document(uid)
                                    .collection("events")
                                    .document(year)
                                    .collection(month)
                                    .document(result.documents[i].id)
                                    .delete()
                                    .addOnSuccessListener {
                                        Log.d(
                                            "GoogleFirestore",
                                            "DocumentSnapshot successfully deleted!"
                                        )
                                        deletedEvents++
                                        // If all of the events except one were successfully deleted, return true
                                        if (deletedEvents == result.size() - 1) {
                                            callback(true)
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w("GoogleFirestore", "Error deleting document", e)
                                        callback(false)
                                    }
                            }
                        }
                    }.addOnFailureListener {
                        Log.w("GoogleFirestore", "Nothing found")
                    }
            }

        }

    }
   /** This function will get the user's profile information from the Firestore database
     * @param uid: The user's uid
     * @return A Pair containing the user's home address and the use4 value
     */
    suspend fun getUserProfileInfo(uid: String): Pair<String, Boolean> = suspendCoroutine {continuation ->
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                        val homeAddress = document.data?.get("homeAddress") as? String ?: ""
                        val use4 = document.data?.get("use4") as? Boolean ?: false
                        continuation.resume(Pair(homeAddress, use4))
                } else {
                    continuation.resume(Pair("", false))
                }
            }
            .addOnFailureListener {
                continuation.resume(Pair("", false))
            }
    }
    /** This function will set the user's profile information in the Firestore database
     * @param uid: The user's uid
     * @param homeAddress: The user's home address
     * @param use4: The use4 value
     * @return A boolean value indicating whether the user's profile information was set successfully
     */
    suspend fun setUserProfileInfo(uid: String, homeAddress: String?, use4: Boolean?): Boolean = suspendCoroutine { continuation ->
        val userDocRef = db.collection("users").document(uid)

        userDocRef.get().addOnSuccessListener { document ->
            if (document != null) {
                val currentHomeAddress = document.data?.get("homeAddress") as? String ?: ""
                val currentUse4 = document.data?.get("use4") as? Boolean ?: false

                val updates = hashMapOf<String, Any>(
                    "homeAddress" to (homeAddress ?: currentHomeAddress),
                    "use4" to (use4 ?: currentUse4)
                )

                userDocRef.set(updates)
                    .addOnSuccessListener {
                        Log.d("Firestore", "DocumentSnapshot successfully updated!")
                        continuation.resume(true)
                    }
                    .addOnFailureListener { e ->
                        Log.w("Firestore", "Error updating document", e)
                        continuation.resume(false)
                    }
            } else {
                continuation.resume(false)
            }
        }.addOnFailureListener { e ->
            Log.w("Firestore", "Error getting document", e)
            continuation.resume(false)
        }
    }
}