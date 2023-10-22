package com.protify.protifyapp

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import java.time.LocalDateTime

class HomeActivity {
    @Composable
    fun HomePage(navController: NavController) {
        //Get the current user from the EmailPasswordActivity class
        var firestoreHelper = FirestoreHelper()
        val user = FirebaseLoginHelper().getCurrentUser()
        //Display the user's email
        Text(text = "Welcome ${user?.email}")

        var networkManager = NetworkManager(navController.context)

        var isConnected by remember { mutableStateOf(false) }
        LaunchedEffect(networkManager) {
            networkManager.startListening()
        }
        LaunchedEffect(isConnected) {
            networkManager.setNetworkChangeListener {
                if(it) {
                    firestoreHelper.toggleOfflineOnline(true)
                } else {
                    firestoreHelper.toggleOfflineOnline(false)
                }

            }
        }
                    FirestoreHelper().userExists(user!!.uid, user.metadata!!.creationTimestamp) {userExists ->
                        if (userExists) {
                            FirestoreHelper().createEvent(user.uid, FirestoreEvent(
                                name = "Test Event",
                                startTime = LocalDateTime.now(),
                                endTime = LocalDateTime.now(),
                                location = "Test Location",
                                description = "Test Description",
                                timeZone = "Test Timezone",
                                importance = 1,
                                attendees = null
                            ))

                        }
                    }
            }
    }