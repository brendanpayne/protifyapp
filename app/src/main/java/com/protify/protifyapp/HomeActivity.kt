package com.protify.protifyapp

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import java.time.LocalDateTime

class HomeActivity {
    @Composable
    fun HomePage(navigateToAddEvent: () -> Unit) {
        //Get the current user from the EmailPasswordActivity class
        val firestoreHelper = FirestoreHelper()
        val user = FirebaseLoginHelper().getCurrentUser()
        val context = LocalContext.current
        //Display the user's email
        Log.d("HomeActivity", "User email: ${user?.email}")
        Column {
            Text(
                text = "Good Morning, ${user?.email}!",
                modifier = androidx.compose.ui.Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium
            )
            Button(onClick = navigateToAddEvent) {
                Text("Add Event")
            }
        }

        val networkManager = NetworkManager(context)

        val isConnected by remember { mutableStateOf(false) }
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
    fun navigateToAddEvent(navController: NavController) {
        navController.navigate("addEvent")
    }
}