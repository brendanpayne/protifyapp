package com.protify.protifyapp

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.protify.protifyapp.features.calendar.CalendarView
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import com.protify.protifyapp.ui.theme.ProtifyTheme
import com.protify.protifyapp.utils.LocationUtils

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
                val permission = android.Manifest.permission.ACCESS_FINE_LOCATION; android.Manifest.permission.ACCESS_COARSE_LOCATION

                 val locationPermssionsContract = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        if (isGranted) {
                            LocationUtils(context).getCurrentLocation { long, lat ->
                                Toast.makeText(context, "Location: $long, $lat", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // Explain to the user that the feature is unavailable because the
                            // features requires a permission that the user has denied.
                        }
                    }
                )
                LaunchedEffect(locationPermssionsContract) {
                    locationPermssionsContract.launch(permission)
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