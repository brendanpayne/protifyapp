package com.protify.protifyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.protify.protifyapp.ui.theme.ProtifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProtifyTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    //Greeting("Android")
                    val loginPage = LoginActivity();

                    val navController = rememberNavController()
                   NavHost(navController = navController, startDestination = "main") {
                          composable("main") {
                            Button(onClick = {navController.navigate("login") }) {
                                 Text("Login Page")
                            }
                          }
                       composable("login") {
                           loginPage.LandingPage(navController = navController)
                   }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ProtifyTheme {
        Greeting("Android")
    }
}
@Preview(showBackground = true)
@Composable
fun LoginPage() {
    Text(text = "Success!")
}