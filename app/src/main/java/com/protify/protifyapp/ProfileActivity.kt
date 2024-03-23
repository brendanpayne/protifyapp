package com.protify.protifyapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity {
    val user = FirebaseAuth.getInstance().currentUser
    val name = user?.displayName.takeIf { it?.isNotEmpty() == true } ?: "Unknown"
    val email = user?.email ?: "Unknown"

    @Composable
    fun BackButton(navController: NavController) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Go back")
        }
    }

    @Composable
    fun ProfilePage(navController: NavController) {
        var displayName by remember { mutableStateOf(name) }
        var homeAddress by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(navController)
                Spacer(modifier = Modifier.width(16.dp)) 
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.profile_picture),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(16.dp)
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { newDisplayName -> displayName = newDisplayName },
                    label = { Text("Display Name") },
                    modifier = Modifier.padding(16.dp)
                )
                OutlinedTextField(
                    value = homeAddress,
                    onValueChange = { newHomeAddress -> homeAddress = newHomeAddress },
                    label = { Text("Home Address") },
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    text = email,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}