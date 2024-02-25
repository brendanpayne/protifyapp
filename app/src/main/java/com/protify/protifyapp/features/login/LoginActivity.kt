package com.protify.protifyapp.features.login

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

class LoginActivity {
    private var email by mutableStateOf("")
    private var password by mutableStateOf("")
    //private val firebaseLoginHelper = FirebaseLoginHelper()

    private fun updateEmail(newEmail: String) {
        email = newEmail
    }

    private fun updatePassword(newPassword: String) {
        password = newPassword
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun LoginPage(navigateToHomePage: () -> Unit) {
        val context = LocalContext.current
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Text(
                    text = "Welcome Back!",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email -> updateEmail(email) },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                )
            }
            item {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password -> updatePassword(password) },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
            }
            item {
                Button(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    onClick = {
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

    }
    fun navigateToHomePage(navController: NavController) {
        navController.navigate("home")
    }
}