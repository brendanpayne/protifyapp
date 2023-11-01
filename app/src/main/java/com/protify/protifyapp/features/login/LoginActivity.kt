package com.protify.protifyapp.features.login

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.*
import androidx.navigation.NavController

class LoginActivity {
    private var email by mutableStateOf("")
    private var password by mutableStateOf("")
    private val firebaseLoginHelper = FirebaseLoginHelper()

    private fun updateEmail(newEmail: String) {
        email = newEmail
    }

    private fun updatePassword(newPassword: String) {
        password = newPassword
    }

    @Composable
    fun LoginPage(navigateToHomePage: () -> Unit) {
        val context = LocalContext.current
        Column {
            TextField(
                value = email,
                onValueChange = { email -> updateEmail(email) },
                label = { Text("Email") }
            )
            TextField(
                value = password,
                onValueChange = { password -> updatePassword(password) },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
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