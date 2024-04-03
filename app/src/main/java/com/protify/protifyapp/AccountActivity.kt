package com.protify.protifyapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.protify.protifyapp.features.calendar.CalendarUiModel
import com.protify.protifyapp.features.events.AddEvent
import com.protify.protifyapp.features.events.EditEvent
import com.protify.protifyapp.features.events.EventDetails
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import com.protify.protifyapp.features.login.LoginActivity
import com.protify.protifyapp.features.login.RegisterActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AccountActivity {
    @Composable
    fun AccountPage() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            //Greeting("Android")
            val navController = rememberNavController()
            val currentUser by remember {
                mutableStateOf(FirebaseLoginHelper().getCurrentUser())
            }
            NavHost(navController = navController, startDestination = "main") {
                composable("main") {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Protify",
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            style = MaterialTheme.typography.headlineLarge,
                            textAlign = TextAlign.Center,
                        )
                        Row (
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            Button(
                                onClick = { navController.navigate("register") },
                                modifier = Modifier
                                    .padding(16.dp)
                                    .weight(1f)
                            ) {
                                Text("New User")
                            }
                            Button(
                                onClick = { navController.navigate("login") },
                                modifier = Modifier
                                    .padding(16.dp)
                                    .weight(1f)
                            ) {
                                Text("Existing User")
                            }
                        }
                    }
                }
                composable("register") {
                    RegisterActivity().LandingPage() {
                        LoginActivity().navigateToHomePage(
                            navController = navController
                        )
                    }
                }
                composable("login") {
                    LoginActivity().LoginPage {
                        LoginActivity().navigateToHomePage(
                            navController = navController
                        )
                    }
                }

                composable("home") {
                    HomeActivity().HomePage(navController) {
                        HomeActivity().navigateToAddEvent(
                            navController = navController
                        )
                    }
                }


                composable("addEvent") {
                    AddEvent().AddEventPage {
                        AddEvent().navigateBack(
                            navController = navController
                        )
                    }
                }
                composable("profile") {
                    ProfileActivity().ProfilePage(navController)
                }

                composable("privacyLocation") {
                    PrivacyActivity().PrivacyPage(navController)
                }
                composable("eventDetails/{date}/{eventId}") { backStackEntry ->
                    val date = backStackEntry.arguments?.getString("date")
                    val eventId = backStackEntry.arguments?.getString("eventId")
                    EventDetails().EventDetailsPage(eventId = eventId!!, date = date!!, navController = navController)
                }
                composable("editEvent/{date}/{eventId}") { backStackEntry ->
                    var event = remember { mutableStateOf<FirestoreEvent?>(null) }
                    val date = backStackEntry.arguments?.getString("date")
                    val eventId = backStackEntry.arguments?.getString("eventId")
                    val parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    val parsedDateTime = parsedDate.atStartOfDay()

                    LaunchedEffect(eventId, date) {
                        FirestoreHelper().getEventsAndIds(
                            FirebaseLoginHelper().getCurrentUser()!!.uid,
                            parsedDateTime.dayOfMonth.toString(),
                            parsedDateTime.month.toString(),
                            parsedDateTime.year.toString()
                        ) { fetchedEvents ->
                            event.value = fetchedEvents.keys.find { it.id == eventId }
                        }
                    }

                    event.value?.let {
                        EditEvent(it).EditEventPage {
                            EditEvent(it).navigateBack(
                                navController = navController
                            )
                        }
                    }
                }
            }


            if (currentUser != null) {
                navController.navigate("home")
            }
        }
    }
}