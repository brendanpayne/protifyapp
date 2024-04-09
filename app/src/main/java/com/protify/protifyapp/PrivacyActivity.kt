package com.protify.protifyapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

class PrivacyActivity {
    @Composable
    fun BackButton(navController: NavController) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                // This is a placeholder to push the title to the center
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }

    @Composable
    fun PrivacyPage(navController: NavController) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column{
                BackButton(navController)
                Spacer(modifier = Modifier.size(50.dp))
                Text(
                    text = privacyText,
                    style = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .padding(16.dp)
                )
            }
        }
    }

    private val privacyText = """
    This app respects your privacy and is committed to protecting it. 

    We may collect location information for the purpose of improving our services. This data is anonymized and cannot be used to identify you personally.

    We do not share your location information with third parties without your explicit consent.

    By using this app, you consent to the collection and use of your location information as described in this policy.
""".trimIndent()

    @Preview
    @Composable
    fun PrivacyPagePreview() {
        Surface {
            PrivacyPage(navController = rememberNavController())
        }
    }
}

