package com.protify.protifyapp

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
class HomeActivity {
    @Composable
    fun HomePage( navController: NavController) {
        //Get the current user from the EmailPasswordActivity class
        val user = EmailPasswordActivity().getCurrentUser()
        //Display the user's email
        Text(text = "Welcome ${user?.email}")
        }
    }