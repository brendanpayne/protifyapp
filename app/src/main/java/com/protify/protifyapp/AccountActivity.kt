package com.protify.protifyapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.protify.protifyapp.features.events.AddEvent
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import com.protify.protifyapp.features.login.LoginActivity
import com.protify.protifyapp.features.login.RegisterActivity

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
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Logo",
                            modifier = Modifier.padding(16.dp)
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
                composable("recipeGenerator") {
                    RecipeActivity().RecipePage(navController)
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


            if (currentUser != null) {
                navController.navigate("home")
            }
        }
    }
}