package com.protify.protifyapp.features.events

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.protify.protifyapp.FirestoreEvent
import com.protify.protifyapp.FirestoreHelper
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import com.protify.protifyapp.utils.EventUtils
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class EventDetails {
    @Composable
    fun EventDetailsPage(eventId: String, date: String, navController: NavHostController) {
        var event by remember { mutableStateOf<FirestoreEvent?>(null) }
        val uid = FirebaseLoginHelper().getCurrentUser()!!.uid
        val parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val parsedDateTime = parsedDate.atStartOfDay()

        LaunchedEffect(eventId, date) {
            FirestoreHelper().getEventsAndIds(
                uid,
                parsedDateTime.dayOfMonth.toString(),
                parsedDateTime.month.toString(),
                parsedDateTime.year.toString()
            ) { fetchedEvents ->
                event = fetchedEvents.keys.find { it.id == eventId }
            }
        }

        Box(Modifier.fillMaxHeight()) {
            event?.let {
                EventItem(it)
            } ?: run {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Loading event...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            FloatingActionButton(
                onClick = {
                    navController.navigate("editEvent/${date}/${eventId}")
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit event", tint = MaterialTheme.colorScheme.surface)
            }
        }
    }

    @Composable
    fun EventItem(event: FirestoreEvent) {
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
    private fun EventDetailsBody(event: FirestoreEvent) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            event::class.java.declaredFields.forEach { field ->
                field.isAccessible = true
                val value = field.get(event)
                val readableValue = EventUtils().convertToReadableValue(field.name, value)
                if (readableValue.isNotBlank()) {
                    Card(
                        modifier = Modifier.padding(8.dp),
                        content = {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = field.name.uppercase(Locale.ROOT),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = readableValue,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun EventDetailsHeader(event: FirestoreEvent) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = event.name,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}