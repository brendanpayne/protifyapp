package com.protify.protifyapp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.protify.protifyapp.features.calendar.CalendarUiModel
import com.protify.protifyapp.features.calendar.CalendarView
import com.protify.protifyapp.features.events.EventView
import com.protify.protifyapp.features.login.FirebaseLoginHelper

class HomeActivity {
        @Composable
        fun HomePage(navigateToAddEvent: () -> Unit) {
            val firestoreHelper = FirestoreHelper()
            val user = FirebaseLoginHelper().getCurrentUser()
            val context = LocalContext.current
            Column(modifier = Modifier.fillMaxSize()) {
                // TODO: Add a greeting based on the time of day and logged in user.
                val greeting = "Good Morning, ${user?.email}!"
                Text(
                    text = greeting,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                CalendarView().Calendar(navigateToAddEvent)

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
        }
        @Preview(showSystemUi = true)
        @Composable
        fun CalendarAppPreview() {
            HomePage(navigateToAddEvent = {})
        }

    fun navigateToAddEvent(navController: NavHostController) {
        navController.navigate("addEvent")
    }
}