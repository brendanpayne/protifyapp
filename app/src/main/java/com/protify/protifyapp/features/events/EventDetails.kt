package com.protify.protifyapp.features.events

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.protify.protifyapp.features.calendar.CalendarUiModel
import com.protify.protifyapp.features.calendar.Event

class EventDetails {
    @Composable
    fun EventDetailsPage(event: Event) {
        LazyColumn(content = {
            item {
                EventDetailsHeader(event)
            }
            item {
                EventDetailsBody(event)
            }
        })
    }

    @Composable
    private fun EventDetailsBody(event: Event) {
        Text(
            text = event.description,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }

    @Composable
    private fun EventDetailsHeader(event: Event) {
        Text(
            text = event.title,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium
        )
    }
}