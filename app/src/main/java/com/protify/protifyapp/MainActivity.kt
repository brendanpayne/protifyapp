package com.protify.protifyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.FirebaseApp
import com.protify.protifyapp.ui.theme.ProtifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
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
}}