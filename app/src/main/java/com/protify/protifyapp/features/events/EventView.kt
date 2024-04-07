package com.protify.protifyapp.features.events

import android.widget.Toast
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.protify.protifyapp.features.calendar.CalendarUiModel
import com.protify.protifyapp.features.calendar.Event
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter


class EventView (private val navController: NavController) {
    @Composable
    fun EventHeader() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            content = {
                Text(
                    text = "Events",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = {
                        navController.navigate("addEvent")
                    },
                    modifier = Modifier.padding(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Event",
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        )
    }

    @Composable
    fun LoadingText() {
        val scope = rememberCoroutineScope()
        val periodCount = remember { mutableIntStateOf(0) }

        LaunchedEffect(key1 = periodCount) {
            scope.launch {
                while (true) {
                    delay(1000L)
                    periodCount.intValue = (periodCount.intValue + 1) % 3
                }
            }
        }

        val animatedPeriodCount by animateIntAsState(targetValue = periodCount.intValue, label = "")

        Text(
            text = "Loading events." + ".".repeat(animatedPeriodCount),
            modifier = Modifier
                .padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }

    @Composable
    fun EventList(data: CalendarUiModel, isLoadingEvents: Boolean) {
        if (isLoadingEvents) {
            Row (
                modifier = Modifier
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ){
                LoadingText()
            }
        } else {
            if (data.selectedDate.hasEvents) {
                LazyColumn(content = {
                    items(data.selectedDate.events.size) { event ->
                        EventItem(
                            event = data.selectedDate.events[event],
                            date = data.selectedDate.date.toString()
                        )
                    }
                })
            } else {
                Row (
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ){
                    Text(
                        text = "No events for this day!",
                        modifier = Modifier
                            .padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun EventItem(event: Event, date: String) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            onClick = {
                if (event.id != "") {
                    navController.navigate("eventDetails/${date}/${event.id}")
                } else {
                    Toast.makeText(
                        navController.context,
                        "Event ID not found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
    fun EventCard(data: CalendarUiModel, navigateToAddEvent: () -> Unit, isLoadingEvents: Boolean) {
        ElevatedCard(
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
            ) {
                EventHeader()
                EventList(data, isLoadingEvents)
            }
        }
    }
}