package com.protify.protifyapp.features.login

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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.protify.protifyapp.FirestoreHelper
import java.time.LocalDateTime
import java.time.ZoneOffset

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
    fun LoginPage(navController: NavController,navigateToHomePage: () -> Unit) {
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
                        FirebaseLoginHelper().signIn(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Sign in success, update UI with the signed-in user's information
                                    task.result?.user?.let {
                                        //navController.navigate("home")
                                        val user = FirebaseLoginHelper().getCurrentUser()
                                        if (user != null) {
                                            FirestoreHelper().userExists(user.uid, LocalDateTime.now().toInstant(
                                                ZoneOffset.UTC).epochSecond) { exists ->
                                                if (exists) {
                                                    navigateToHomePage()
                                                } else {
                                                    Toast.makeText(context, "Error making user document", Toast.LENGTH_SHORT).show()
                                                }
                                            }

                                        } else {
                                            Toast.makeText(context, "Null user", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Login Failed",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                    }) {
                    Text("Login")
                }
            }
            item {
                Row {
                    Text(
                        text = "Don't have an account? ",
                        modifier = Modifier.clickable { navController.navigate("register") },
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Create an account",
                        modifier = Modifier.clickable { navController.navigate("register") },
                        color = Color.Blue,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }


    }
    fun navigateToHomePage(navController: NavController) {
        navController.navigate("home")
    }
}