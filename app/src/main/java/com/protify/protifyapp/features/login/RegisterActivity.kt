package com.protify.protifyapp.features.login

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController

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
    fun LandingPage(navController: NavController, navigateToHomePage: () -> Unit) {
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
                    text = "Create an Account",
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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
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
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = {
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
            item {
                Row {
                    Text(
                        text = "Already have an account? ",
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Login",
                        modifier = Modifier.clickable { navController.navigate("login") },
                        color = Color.Blue,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
