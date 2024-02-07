package com.protify.protifyapp

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.protify.protifyapp.features.GoogleMapsAPI.DrivingTime
import com.protify.protifyapp.features.events.AddEvent
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import com.protify.protifyapp.features.login.LoginActivity
import com.protify.protifyapp.features.login.RegisterActivity
import com.protify.protifyapp.utils.MapsDurationUtils
import com.protify.protifyapp.utils.OpenAIHelper.OptimizeSchedule
import java.time.LocalDateTime

class AccountActivity {
    @Composable
    fun AccountPage() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            //Greeting("Android")
            val navController = rememberNavController()
            val currentUser by remember {
                mutableStateOf(FirebaseLoginHelper().getCurrentUser())
            }
            NavHost(navController = navController, startDestination = "main") {
                composable("main") {
                    Row {
                        Button(onClick = { navController.navigate("register") }) {
                            Text("New User")
                        }
                        Button(onClick = { navController.navigate("login") }) {
                            Text("Existing User")
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
                    HomeActivity().HomePage {
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
                /*
                composable("eventDetails/{date}/{eventId}") { backStackEntry ->
                    val date = backStackEntry.arguments?.getString("date")
                    val eventId = backStackEntry.arguments?.getString("eventId")
                    EventDetails().EventDetailsPage(
                        date = date!!,
                        eventId = eventId!!
                    )
                }
                 */
            }
            //Open AI Testing
//            AvoidRain("Garbage").getResponse {
//                println(it)
//            }
            if (currentUser != null) {
                val today = LocalDateTime.of(2024, 2, 3, 0, 0)
//                val dayStart = LocalDateTime.of(today.year, today.month, today.dayOfMonth, 7, 0)
//                val dayEnd = LocalDateTime.of(today.year, today.month, today.dayOfMonth, 22, 0)
//                FirestoreHelper().getFreeTime(dayStart, dayEnd, currentUser!!.uid, today.dayOfMonth.toString(), today.month.toString(), today.year.toString()) { freeTimes ->
//                    println(freeTimes)
//
                val homeAddress = "6190 Falla Dr, Canal Winchester, OH 43110" // pull up
                //get all of the events for the day
                FirestoreHelper().getEvents(currentUser!!.uid, today.dayOfMonth.toString(), today.month.toString(), today.year.toString()) { events ->
                    //map locations from the events
                    val locations = mutableListOf<String?>()
                    //If the location is empty, use home address
                    events.mapTo(locations) {
                        if (it.location == "") {
                            "6190 Falla Dr, Canal Winchester, OH 43110"
                        } else {
                            it.location
                        }
                    }

                    //Get the distance between all of the events (including home)
                    MapsDurationUtils(LocalDateTime.now()).getMaxtrix(homeAddress, locations) { matrix ->
                        if (matrix != null) {
                            var drivingTimes = mutableListOf<DrivingTime?>()
                            //i is representing the origin
                            for (i in matrix.rows.indices) {
                                //j is representing the destination
                                for (j in matrix.rows[i].elements.indices) {
                                    //Make sure origin and destination are not the same
                                    if (matrix.originAddresses[i] != matrix.destinationAddresses[j]) {
                                        //This is the driving time in seconds
                                        val drivingTime = matrix.rows[i].elements[j].duration.value
                                        //This is the driving time in text
                                        val drivingTimeText = matrix.rows[i].elements[j].duration.text
                                        //Add the driving time to the list
                                        drivingTimes.add(DrivingTime(matrix.originAddresses[i], matrix.destinationAddresses[j], drivingTimeText))
                                    }
                                }
                            }
                            //Put all of this into the OptimizeSchedule class
                            OptimizeSchedule(today.dayOfMonth.toString(), today.month.toString(), today.year.toString(), events, drivingTimes, homeAddress).getResponse {
                                println(it)
                            }

                        }

                    }
                }


                navController.navigate("home")
            }
        }
    }
}