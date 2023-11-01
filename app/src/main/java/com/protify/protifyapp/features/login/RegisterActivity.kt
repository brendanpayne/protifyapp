package com.protify.protifyapp.features.login

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.ViewModel

class RegisterActivity: ViewModel() {

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
    fun LandingPage(navigateToHomePage: () -> Unit) {
        val context = LocalContext.current
        Log.d("RegisterActivity", "LandingPage")
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