package com.protify.protifyapp.features.login

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.navigation.NavController

class LoginActivity {
    var email = "tommy.mcreynolds1@gmail.com"
    var password = "testpassword"
    val firebaseLoginHelper = FirebaseLoginHelper()
    @Composable
    fun LoginPage(navigateToHomePage: () -> Unit) {
        val context = LocalContext.current
        Column {
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") }
            )
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation()
            )
            Button(onClick = {
                FirebaseLoginHelper().signIn(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        task.result?.user?.let {
                            //navController.navigate("home")
                            navigateToHomePage()
                        }
                    } else {
                       Toast.makeText(
                           context,
                           "Login Failed",
                           Toast.LENGTH_SHORT,
                       ).show()
                        navigateToHomePage()
                    }
                }
            }) {
                Text("Login")
            }

        }

    }
    fun navigateToHomePage(navController: NavController) {
        navController.navigate("home")
    }
}