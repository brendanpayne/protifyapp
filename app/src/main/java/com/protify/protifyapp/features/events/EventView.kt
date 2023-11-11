package com.protify.protifyapp.features.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.protify.protifyapp.features.calendar.CalendarUiModel
import com.protify.protifyapp.features.calendar.Event


class EventView {
    @Composable
    fun EventHeader() {
        Row{
            Text(
                text = "Events",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }

    @Composable
    fun EventList(data: CalendarUiModel, isLoadingEvents : Boolean) {
        if (isLoadingEvents) {
            Row {
                Text(
                    text = "Loading events...",
                    modifier = Modifier
                        .padding(16.dp)
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                )
            }
        } else {
            if (data.selectedDate.hasEvents) {
                LazyColumn(content = {
                    items(data.selectedDate.events.size) { event ->
                        EventItem(event = data.selectedDate.events[event])
                    }
                })
            } else {
                Row {
                    Text(
                        text = "No events for this day!",
                        modifier = Modifier
                            .padding(16.dp)
                            .weight(1f)
                            .align(Alignment.CenterVertically),
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun EventItem(event: Event) {
        Card (
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            onClick = {

            }
        ) {
            Column {
                EventItemContent(event)
            }
        }
    }

    @Composable
    private fun EventItemContent(event: Event) {
        Row {
            Text(
                text = event.title,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${event.startTime} - ${event.endTime}",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    @Composable
    fun EventCard(data: CalendarUiModel, navigateToAddEvent: () -> Unit, isLoadingEvents : Boolean) {
        ElevatedCard (
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            )
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ){
                EventHeader()
                EventList(data, isLoadingEvents)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .weight(1f, false)
                ){
                    Button(
                        onClick = {
                            navigateToAddEvent()
                        },
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Text("Add Event")
                    }
                }
            }
        }
    }
}