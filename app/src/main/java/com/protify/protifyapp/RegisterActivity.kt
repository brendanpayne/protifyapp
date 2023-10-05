package com.protify.protifyapp

import android.widget.Toast
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
     val firebaseLoginHelper = FirebaseLoginHelper()
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
            Button(onClick = { firebaseLoginHelper.createAccount(email, password)
                .addOnCompleteListener {task ->
                    if(task.isSuccessful) {
                        navController.navigate("home")
                    }
                    else {
                        task.exception?.localizedMessage?.let {
                            Toast.makeText( navController.context, it, Toast.LENGTH_SHORT).show()
                        }
                    }
                }}) {
                Text("Create Account")
            }
        }
    }
    }