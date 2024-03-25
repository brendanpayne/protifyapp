package com.protify.protifyapp.features.events

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.protify.protifyapp.FirestoreHelper
import com.protify.protifyapp.HomeActivity
import com.protify.protifyapp.features.calendar.CalendarUiModel
import com.protify.protifyapp.features.calendar.Event
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import com.protify.protifyapp.utils.OpenAIHelper.ParseTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime


class EventView {
    val user = FirebaseLoginHelper().getCurrentUser()
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun EventHeader(context: Context, today: LocalDateTime) {
        var isLoading by remember { mutableStateOf(false) }
        val user = FirebaseLoginHelper().getCurrentUser()

        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Events",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (today.withHour(0).withMinute(0).withSecond(0) >= LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).plusHours(22)) {
                IconButton(
                    onClick = {
                        CoroutineScope(Dispatchers.Main).launch {
                            val homeAddress = FirestoreHelper().getUserHomeAddress(user!!.uid)
                            if (homeAddress == "" || homeAddress == "No home address found") {
                                Toast.makeText(context, "Please set your home address in the settings", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            if (isLoading) {
                                Toast.makeText(context, "Optimization already in progress", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            isLoading = true
                            Toast.makeText(context, "Optimizing schedule...", Toast.LENGTH_SHORT).show()
                            val success = HomeActivity().optimizeScheduleForToday(user!!.uid, today)
                            if (success) {
                                Toast.makeText(context, "Schedule optimized!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Schedule is already optimized!", Toast.LENGTH_SHORT).show()
                            }
                            isLoading = false
                        }

                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    OptimizingIcon(isLoading = isLoading)
                }
            }
        }
    }

    @Composable
    fun EventList(data: CalendarUiModel, isLoadingEvents: Boolean, context: Context) {
        var selectedDate = data.selectedDate.date.atStartOfDay()
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
                    // Sort events by start time
                    val sortedEvents = data.selectedDate.events.sortedBy { ParseTime().parseAMPMTime(it.startTime, data.selectedDate.date.atStartOfDay()) }
                    items(sortedEvents.size) { event ->
                        EventItem(event = sortedEvents[event], context = context, selectedDate = selectedDate, isAiSuggested = sortedEvents[event].isAiSuggestion)
                    }
                })
            } else {
                Row {
                    Text(
                        text = "No events for this day!",
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .weight(1f)
                            .align(Alignment.CenterVertically),
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun EventItem(event: Event, context: Context, selectedDate: LocalDateTime, isAiSuggested: Boolean) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            onClick = { /* Handle click event */ },
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(
                contentColor = if (isAiSuggested) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
            )
        ) {
            EventItemContent(event, context, selectedDate, isAiSuggested)
        }
    }

    @Composable
    private fun EventItemContent(event: Event, context: Context, selectedDate: LocalDateTime, isAiSuggested: Boolean) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isAiSuggested) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${event.startTime} - ${event.endTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.medium)
                )
            }
            IconButton(
                onClick = {
                    FirestoreHelper().deleteEventById(user!!.uid, selectedDate.month.toString(), selectedDate.year.toString(), event.id) {
                        if (it) {
                            Toast.makeText(context, "Event deleted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to delete event", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Event",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    @Composable
    fun EventCard(data: CalendarUiModel, navigateToAddEvent: () -> Unit, isLoadingEvents: Boolean, context: Context) {
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
                EventHeader(context, data.selectedDate.date.atStartOfDay())
                EventList(data, isLoadingEvents, context)
                /*  Row(
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
            */

            }
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun OptimizingIcon(isLoading: Boolean) {
        val infiniteRotation by animateFloatAsState(
            targetValue = if (isLoading) 360f else 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "",
        )

        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Loading",
            modifier = Modifier
                .padding(4.dp)
                .size(30.dp)
                .rotate(if (isLoading) infiniteRotation else 0f), // Use the animated value directly
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
    @Composable
    fun DeleteEventButton(onClick : () -> Unit, modifier: Modifier = Modifier) {
        Button(
            onClick = onClick,
            modifier = modifier,
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Event",
                modifier = Modifier.size(20.dp)
            )
        }

    }


}