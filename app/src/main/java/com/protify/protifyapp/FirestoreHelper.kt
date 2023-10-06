package com.protify.protifyapp

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class FirestoreHelper {
    private val db: FirebaseFirestore = Firebase.firestore



    @OptIn(ExperimentalTime::class)
    fun userExists(uid: String, dateCreated: Long) {
        // [START get_document]
        val docRef = db.collection("users").document(uid)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    //Add function here
                } else {
                    val seconds = dateCreated.seconds.inWholeSeconds
                    val nanoseconds = dateCreated.nanoseconds.inWholeNanoseconds.toInt()
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
                //Add function here
            }
            .addOnFailureListener { e ->
            }
    }
}