package com.protify.protifyapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.protify.protifyapp.features.calendar.CalendarView
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import com.protify.protifyapp.ui.theme.ProtifyTheme
class HomeActivity {

        @Composable
        fun HomePage(navigateToAddEvent: () -> Unit) {
            val firestoreHelper = FirestoreHelper()
            val user = FirebaseLoginHelper().getCurrentUser()
            val context = LocalContext.current
            Box(modifier = Modifier.fillMaxSize()) {
                Column {
                    // TODO: Add a greeting based on the time of day and logged in user.
                    val greeting = "Good Morning, ${user?.email}!"
                    Text(
                        text = greeting,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                    CalendarView().Calendar(navigateToAddEvent)
                }

                SettingsIconButton(onClick = navigateToAddEvent)

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
        }

    @Composable
    fun SettingsIconButton(onClick: () -> Unit) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
            IconButton(onClick = onClick) {
                Icon(Icons.Filled.Menu, contentDescription = "Settings")
            }
        }
    }
        @Preview(showSystemUi = true)
        @Composable
        fun CalendarAppPreview() {
            ProtifyTheme {
                HomePage(navigateToAddEvent = {})
            }
        }

    fun navigateToAddEvent(navController: NavHostController) {
        navController.navigate("addEvent")
    }
}