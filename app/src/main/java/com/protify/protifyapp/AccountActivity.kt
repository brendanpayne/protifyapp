package com.protify.protifyapp

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.protify.protifyapp.features.events.AddEvent
import com.protify.protifyapp.features.login.LoginActivity
import com.protify.protifyapp.features.login.RegisterActivity

class AccountActivity {
    @Composable
    fun AccountPage() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            //Greeting("Android")
            val navController = rememberNavController()
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
            }
        }
    }
}