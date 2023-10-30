package com.protify.protifyapp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.protify.protifyapp.features.calendar.CalendarView

class HomeActivity {
    @Composable
    fun HomePage(modifier: Modifier = Modifier, navController: NavController? = null) {
        Column(modifier = modifier.fillMaxSize()) {
            // TODO: Add a greeting based on the time of day and logged in user.
            val greeting = "Good Morning, Tom"
            Text(
                text = greeting,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge
            )
            CalendarView().Calendar()
        }
    }
    @Preview(showSystemUi = true)
    @Composable
    fun CalendarAppPreview() {
        HomePage()
    }
}