package com.protify.protifyapp

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

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
                    createUserDocument(uid, dateCreated)
                }
            }
    }
    private fun createUserDocument(uid: String, dateCreated: Long) {
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
                }
            }
            .addOnFailureListener { e ->
                Log.d("GoogleFirestore", "Error adding user to Firestore", e)
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
}