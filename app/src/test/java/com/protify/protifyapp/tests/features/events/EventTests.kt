package com.protify.protifyapp.tests.features.events

import com.protify.protifyapp.FirestoreEvent
import com.protify.protifyapp.FirestoreHelper
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import org.junit.Test
import java.time.LocalDateTime

class EventTests {

    @Test
    fun testEventAdding() {
        // Given
        val username = "tommy.mcreynolds@gmail.com"
        val password = "testpassword"
        val testEvent = FirestoreEvent(
            name = "Test Event",
            startTime = LocalDateTime.now(),
            endTime = LocalDateTime.now().plusHours(1),
            location = "Test Location",
            description = "Test Description",
            timeZone = "Test Timezone",
            importance = 3,
            attendees = null
        )

        // When
        addEvent(testEvent, username, password) {
            //then
            assert(it)
        }
    }

    private fun addEvent(event: FirestoreEvent, username: String, password: String, callback: (Boolean) -> Unit) {
        FirebaseLoginHelper().signInCallback(username, password) { success, errorMessage ->
            if (success) {
                val user = FirebaseLoginHelper().getCurrentUser()
                if (user != null) {
                    //If the user doesn't have a document already in the database, it will be automatically added. Failure means that there was a problem with firestore
                    //see logcat for more info
                    FirestoreHelper().userExists(user.uid, user.metadata!!.creationTimestamp) { userExists ->
                        if (userExists) {
                            FirestoreHelper().createEvent(user.uid, event)
                            callback(true)
                        } else {
                            callback(false)
                        }
                    }
                }
            }
        }
    }
}