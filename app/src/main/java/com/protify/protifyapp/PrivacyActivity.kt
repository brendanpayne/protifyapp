package com.protify.protifyapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.navigation.NavController

class PrivacyActivity {
    @Composable
    fun BackButton(navController: NavController) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Go back")
            }
        }
    }

    @Composable
    fun PrivacyPage(navController: NavController) {
        Box(modifier = Modifier.fillMaxSize()) {
            BackButton(navController)
            OutlinedTextField(
                value = privacyText,
                onValueChange = {},
                modifier = Modifier.align(Alignment.Center),
                readOnly = true,
                textStyle = TextStyle(color = Color.Black),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )
        }
    }

    private val privacyText = """
    This app respects your privacy and is committed to protecting it. 

    We may collect location information for the purpose of improving our services. This data is anonymized and cannot be used to identify you personally.

    We do not share your location information with third parties without your explicit consent.

    By using this app, you consent to the collection and use of your location information as described in this policy.
""".trimIndent()
}

