package com.protify.protifyapp

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

class HomeActivity {
    @Composable
    fun HomePage(navController: NavController) {
        //Get the current user from the EmailPasswordActivity class
        val user = FirebaseLoginHelper().getCurrentUser()
        //Display the user's email
        Text(text = "Welcome ${user?.email}")
        if (user != null) {
            //Get the user's information from the database

            FirestoreHelper().userExists(user.uid, user.metadata!!.creationTimestamp)
        }
    }
}