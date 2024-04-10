package com.protify.protifyapp

import FirestoreEventString
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import com.protify.protifyapp.features.events.AddEvent
import com.protify.protifyapp.features.events.Attendee
import com.protify.protifyapp.features.events.EditEvent
import com.protify.protifyapp.features.events.EventDetails
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import com.protify.protifyapp.features.login.LoginActivity
import com.protify.protifyapp.features.login.RegisterActivity
import java.time.LocalDate
import java.time.LocalDateTime
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
                        Image(
                            painter = painterResource(id = R.mipmap.protify_foreground),
                            contentDescription = "Protify Logo",
                            modifier = Modifier.width(200.dp).height(200.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            text = "Protify",
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            style = MaterialTheme.typography.headlineLarge,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "The AI-powered calendar app",
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center,
                        )

                        Button(
                            onClick = { navController.navigate("register") },
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text("New User")
                        }
                        Button(
                            onClick = { navController.navigate("login") },
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text("Existing User")
                        }
                    }
                }
                composable("register") {
                    RegisterActivity().LandingPage(navController) {
                        LoginActivity().navigateToHomePage(
                            navController = navController
                        )
                    }
                }
                composable("login") {
                    LoginActivity().LoginPage(navController) {
                        LoginActivity().navigateToHomePage(
                            navController = navController
                        )
                    }
                }

                composable("home") {
                    HomeActivity().HomePage(navController)
                }


                composable("addEvent/{date}") { backStackEntry ->
                    val date = backStackEntry.arguments?.getString("date")?: LocalDate.now().toString()
                    AddEvent().AddEventPage(navigateBack = {
                        AddEvent().navigateBack(
                            navController = navController
                        )
                    }, date = date)
                }
                composable("profile") {
                    ProfileActivity().ProfilePage(navController)
                }
                composable("about") {
                    AboutActivity().AboutPage(navController)
                }
                composable("privacyLocation") {
                    PrivacyActivity().PrivacyPage(navController)
                }
                composable("recipeGenerator") {
                    RecipeActivity().RecipePage(navController)
                }
                composable("recipeEvent/{recipeEvent}/{requiredTime}") { navBackStackEntry ->
                    val recipeEventJson = navBackStackEntry.arguments?.getString("recipeEvent")
                    val requiredTime = navBackStackEntry.arguments?.getString("requiredTime")?: "30"
                    val recipeEvent = Gson().fromJson(recipeEventJson, FirestoreEventString::class.java)
                    // Map the FirestoreEventString to FirestoreEvent
                    val event = FirestoreEvent(
                        name = recipeEvent.name,
                        nameLower = recipeEvent.nameLower,
                        startTime = LocalDateTime.now().plusMinutes(10),
                        endTime = LocalDateTime.now().plusMinutes(10).plusMinutes(requiredTime.toLong()),
                        description = recipeEvent.description,
                        attendees = recipeEvent.attendees?.map { Attendee(it.name, it.email, it.phoneNumber) }, // Map the Attendee
                        importance = 3
                   )
                    EditEvent(event, true).EditEventPage {
                        EditEvent(event).navigateBack(
                            navController = navController
                        )
                    }

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