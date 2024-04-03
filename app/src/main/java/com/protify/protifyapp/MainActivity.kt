package com.protify.protifyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.FirebaseApp
import com.protify.protifyapp.ui.theme.ProtifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            val controller = rememberSystemUiController()
            controller.setStatusBarColor(
                color = MaterialTheme.colorScheme.primary,
                darkIcons = false
            )
            ProtifyTheme {
                AccountActivity().AccountPage()
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
    fun RegisterPage() {
        Text(text = "Success!")
    }
}