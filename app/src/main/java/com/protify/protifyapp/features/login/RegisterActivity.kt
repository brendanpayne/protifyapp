package com.protify.protifyapp.features.login

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation

class RegisterActivity {

     //var email = "tommy.mcreynolds@gmail.com"
     //var password = "testpassword"
    var email = ""
    var password = ""
     val firebaseLoginHelper = FirebaseLoginHelper()


    @Composable
    fun LandingPage(navigateToHomePage: () -> Unit) {
        val context = LocalContext.current
        Log.d("RegisterActivity", "LandingPage")
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
                firebaseLoginHelper.createAccountCallback(email, password) { success, failure ->
                    if(success) {
                        navigateToHomePage()
                    }
                    else {
                        if (failure != null) {
                            Toast.makeText(context, failure, Toast.LENGTH_SHORT).show()
                            Log.d("RegisterActivity", failure)
                        }
                    }
                }
            }) {
                Text("Create Account")
            }
        }
    }
    }