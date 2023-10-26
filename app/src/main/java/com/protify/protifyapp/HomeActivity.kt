package com.protify.protifyapp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
            CalendarView().Header()
            CalendarView().CalendarContent()
        }
    }
    @Preview(showSystemUi = true)
    @Composable
    fun CalendarAppPreview() {
        HomePage(
            modifier = Modifier.padding(16.dp)
        )
    }
}