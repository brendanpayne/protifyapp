package com.protify.protifyapp

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.UUID

class FirestoreHelper {
    private val db: FirebaseFirestore = Firebase.firestore

    fun userExists(uid: String, dateCreated: Long, callback: (Boolean) -> Unit) {
        // [START get_document]
        val docRef = db.collection("users").document(uid)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
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
            }
            .addOnFailureListener { e ->
            }
    }
    fun addEvent (uid: String, event: FirestoreEvent) {
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
                .document(event.startTime.dayOfMonth.toString())
                .collection("events")
                .document(UUID.randomUUID().toString())
                .set(FirestoreEventEntry)
                .addOnSuccessListener { documentReference ->
                    Log.d("GoogleFirestore", "DocumentSnapshot added successfully")
                }
                .addOnFailureListener { e ->
                    Log.d("GoogleFirestore", "Error adding document", e)
                }

    }
    fun toggleOfflineOnline(isConnected: Boolean, callback: (Boolean) -> Unit) {
        // [START disable_network]
        db.disableNetwork()
            .addOnCompleteListener {
                Log.d("GoogleFirestore", "Network disabled")
                if (!isConnected) {
                    offlineListener("test")
                    callback(true)
                }
            }
        // [END disable_network]

        // [START enable_network]
        if(isConnected) {
            db.enableNetwork()
                .addOnSuccessListener {
                    Log.d("GoogleFirestore", "Network enabled")
                    callback(true)
                }
                .addOnFailureListener { e ->
                    Log.w("GoogleFirestore", "Error enabling network", e)
                    callback(true)
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