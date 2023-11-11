package com.protify.protifyapp

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.protify.protifyapp.features.events.Attendee
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
                    "attendees" to event.attendees,        )
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
                            attendees = attendeeList

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
}