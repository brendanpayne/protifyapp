package com.protify.protifyapp.tests.features.Firebase

import com.protify.protifyapp.FirestoreEvent
import com.protify.protifyapp.FirestoreHelper
import com.protify.protifyapp.features.events.Attendee
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch

class Auth {
    // init email
    val email = "unittest@google.com"
    // init password
    val password = "password"
    @Before
    fun `Create a user with a username and password and sign in`() {
        // create countdownlatch
        val latch = CountDownLatch(1)
        // Create FirebaseLoginHelper
        val firebaseLoginHelper = FirebaseLoginHelper()
        // If the user is not null, sign out
        if (firebaseLoginHelper.getCurrentUser() != null) {
            firebaseLoginHelper.signOut()
        }

        // Create a user with a username and password
        firebaseLoginHelper.createAccountCallback(email, password) { success, message ->
            // Check if the user was created successfully
            if (success) {
                FirebaseLoginHelper().signInCallback(email, password) { success, message ->
                    // Check if the user was signed in successfully
                    if (success) {
                        latch.countDown()
                    } else {
                        // Print the error message
                        assert(false) { "User sign in failed: $message" }
                    }
                }
            } else {
                // Print the error message
                assert(false) { "User creation failed: $message" }
            }
        }
        // Wait 15 seconds for async
        latch.await(15, java.util.concurrent.TimeUnit.SECONDS)
        // assert true if latch is 0
        assert(latch.count == 0L)


    }
    @Test
    fun `Create an Event`() {
        // init countDownLatch
        val latch = CountDownLatch(1)
        // Create FirebaseLoginHelper
        val firebaseLoginHelper = FirebaseLoginHelper()
        // if the user is not null, sign out
        if (firebaseLoginHelper.getCurrentUser() != null) {
            firebaseLoginHelper.signOut()
        }
        // init uid
        var uid: String = ""
        // sign in
        firebaseLoginHelper.signInCallback(email, password) { success, message ->
            // Check if the user was signed in successfully
            if (success) {
                val user = firebaseLoginHelper.getCurrentUser()
                uid = user!!.uid
                //Check if the user is not null
                if (user != null) {
                    //  Generate user document in Firestore
                    FirestoreHelper().userExists(user.uid, user.metadata!!.creationTimestamp) { exists ->
                        //Check if the user exists
                        if (exists) {
                            // Create a mock event
                            val event = FirestoreEvent(
                                name = "Unit test",
                                nameLower = "unit test",
                                description = "Unit test",
                                location = "",
                                startTime = LocalDateTime.now(),
                                endTime = LocalDateTime.now().plusHours(1),
                                attendees = listOf(Attendee(name = "Unit test", email = "", phoneNumber = "")),
                                distance = 0,
                                timeZone = "",
                                isRaining = false,
                                importance = 1,
                                isOutside = false,
                                isOptimized = false,
                                mapsCheck = false,
                                rainCheck = false)

                            // Add an event to the user's calendar
                            FirestoreHelper().createEvent(user.uid, event)
                            // Wait 10 seconds for async
                            Thread.sleep(10000)
                            // Get the events from the user's calendar
                            FirestoreHelper().getEvents(
                                user.uid,
                                LocalDateTime.now().dayOfMonth.toString(),
                                LocalDateTime.now().month.toString(),
                                LocalDateTime.now().year.toString()
                            ) { events ->
                                // Check to make sure that there is at least one event
                                if (events.isNotEmpty()) {
                                    latch.countDown()
                                } else {
                                    // Print the error message
                                    assert(false) { "Event not in database" }
                                }
                            }

                        } else {
                            //Print the error message
                            assert(false) { "Firestore setup failed" }
                        }
                    }

                } else {
                    //Print the error message
                    assert(false) { "User does not exist" }
                }
            } else {
                // Print the error message
                assert(false) { "User sign in failed: $message" }
            }
        }
        // Wait 25 seconds for async
        latch.await(25, java.util.concurrent.TimeUnit.SECONDS)
        // assert true if latch is 0
        assert(latch.count == 0L) { "Event created successfully with userid: $uid" }
    }
    @After
    fun `Delete the user`() {
        // init countDownLatch
        val latch = CountDownLatch(1)
        // Create FirebaseLoginHelper
        val firebaseLoginHelper = FirebaseLoginHelper()
        // if the user is not null, sign out
        if (firebaseLoginHelper.getCurrentUser() != null) {
            firebaseLoginHelper.signOut()
        }
        // sign in
        firebaseLoginHelper.signInCallback(email, password) { success, message ->
            // Check if the user was signed in successfully
            if (success) {
                // get the user
                val user = firebaseLoginHelper.getCurrentUser()
                // Check if the user is not null
                if (user != null) {
                    // Delete the user's document in Firestore
                    FirestoreHelper().deleteUser(user.uid) { documentDeletionSuccess ->
                        // Document deleted successfully
                        if (documentDeletionSuccess) {
                            // Delete the user
                            firebaseLoginHelper.deleteUser { userDeletionSuccess, message ->
                                // Check if the user was deleted successfully
                                if (userDeletionSuccess) {
                                    latch.countDown()
                                } else {
                                    // Print the error message
                                    assert(false) { "User deletion failed: $message" }
                                }
                            }

                        } else {
                            // Print the error message
                            assert(false) { "Document deletion failed" }
                        }
                    }

                } else {
                    // Print the error message
                    assert(false) { "User does not exist" }
                }

            } else {
                // Print the error message
                assert(false) { "User sign in failed: $message" }
            }
        }
        // Wait 15 seconds for async
        latch.await(15, java.util.concurrent.TimeUnit.SECONDS)
        // assert true if latch is 0
        assert(latch.count == 0L) {"User $email" }
    }
}