package com.protify.protifyapp

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.navigation.NavController

class RegisterActivity {

     var email = "tommy.mcreynolds@gmail.com"
     var password = "testpassword"
     val emailPasswordActivity = EmailPasswordActivity()
    @Composable
    fun LandingPage(navController : NavController) {
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
            Button(onClick = { EmailPasswordActivity().createAccount(email, password) }) {
                Text("Create Account")
            }
        }
    }
    }