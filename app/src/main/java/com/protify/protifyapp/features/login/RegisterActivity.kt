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

     var email = "tommy.mcreynolds@gmail.com"
     var password = "testpassword"
     val firebaseLoginHelper = FirebaseLoginHelper()
    @Composable
    fun LandingPage(navigateToHomePage: () -> Unit) {
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
            Button(onClick = { firebaseLoginHelper.createAccount(email, password)
                .addOnCompleteListener {task ->
                    if(task.isSuccessful) {
                        Log.d("RegisterActivity", task.result?.user!!.uid)
                        navigateToHomePage()
                    }
                    else {
                        task.exception?.localizedMessage?.let {
                            Log.d("RegisterActivity", it)
                            Toast.makeText( context, it, Toast.LENGTH_SHORT).show()
                        }
                    }
                }}) {
                Text("Create Account")
            }
        }
    }
    }