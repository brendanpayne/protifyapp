package com.protify.protifyapp.features.calendar

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.protify.protifyapp.FirestoreHelper
import com.protify.protifyapp.ui.theme.ProtifyTheme

class EventBreakdown {

    data class TimeSlot(
        val startTime: String,
        val text: String,
        val color: Color,
        val height: Float = 1f,
        val endTime: String = "",
        val layer: Int = 0,
        val id: String = ""
    )

    private fun createListEvent(): List<Event> {
        // placeholder until event logic is implemented
        return listOf(
            Event().apply {
                title = "Event 1"
                startTime = "07:00"
                endTime = "08:30"
                description = "Description for Event 1"
                location = "Location for Event 1"
                attendees = listOf()
                id = "1"
            },
            Event().apply {
                title = "Event 2"
                startTime = "07:00"
                endTime = "10:00"
                description = "Description for Event 2"
                location = "Location for Event 2"
                attendees = listOf()
                id = "2"
            },
            Event().apply {
                title = "Event 3"
                startTime = "12:00"
                endTime = "13:00"
                description = "Description for Event 3"
                location = "Location for Event 3"
                attendees = listOf()
                id = "3"
            },
            Event().apply {
                title = "Event 4"
                startTime = "14:00"
                endTime = "15:30"
                description = "Description for Event 4"
                location = "Location for Event 4"
                attendees = listOf()
                id = "4"
            }
        )
    }

    @Composable
    fun DailySchedule(
        scale: Double = 1.0,
        eventList: List<Event> = listOf(),
        uid: String = "",
        day: String = "",
        month: String = "",
        year: String = "",
        showTimes: Boolean = true
    ) {
        val context: Context = LocalContext.current
        val timeSlots = if (eventList.isEmpty()) createListTimeSlot() else createListTimeSlot(eventList)
        val scaledValue = scale.toInt()
        timeSlots.maxOfOrNull { it.height } ?: 0f
        val scrollState = rememberScrollState()
        val layers = timeSlots.map { it.layer }.distinct()

        val startTime = if (eventList.isNotEmpty())
            convertTimeToFloat(eventList[0].startTime) else 9f // default 0900
        val startPosition = startTime * 60 * scaledValue
        LaunchedEffect(key1 = true) {
            scrollState.scrollTo(startPosition.toInt())
        }

        Box(modifier = Modifier.fillMaxSize()) {
            TimeGrid(scaledValue, scrollState, showTimes)
            for (layer in layers) {
                TimeSlotLayer(timeSlots, scaledValue, layer, context, uid, day, month, year, scrollState)
            }
        }
    }

    private fun createListTimeSlot(events: List<Event> = createListEvent()): List<TimeSlot> {
        val timeSlots = mutableListOf<TimeSlot>()

        for (event in events) {
//            val layer = calculateLayer(event, events)
//            val layer = events.indexOf(event)
            val layer = 0
            val height = (convertTimeToFloat(event.endTime) - convertTimeToFloat(event.startTime))
            timeSlots.add(TimeSlot(event.startTime, event.title, Color.LightGray, height, event.endTime, layer, event.id))
        }

        return timeSlots
    }

    private fun convertTimeToFloat(time: String): Float {
        if (time.isBlank()) {
            return 0f // prevent crash if time is empty
        }
        val timeArray = time.split(":")
        var hour = timeArray[0].toFloat()
        val minute = timeArray[1].substring(0, 2).toFloat()
        val amPm = timeArray[1].substring(2).trim()

        if (amPm == "PM" && hour < 12) {
            hour += 12
        } else if (amPm == "AM" && hour == 12f) {
            hour = 0f
        }
        return (hour * 60 + minute) / 60
    }

    private fun calculateLayer(event: Event, events: List<Event>): Int {
        val sortedEvents = events.sortedByDescending {
            convertTimeToFloat(it.endTime) - convertTimeToFloat(it.startTime)
        }
        val layers = mutableListOf<MutableList<Event>>()

        for (e in sortedEvents) {
            val layer = layers.indexOfFirst { layer -> layer.none { checkOverlap(e, it) } }

            if (layer == -1) {
                layers.add(mutableListOf(e))
            } else {
                layers[layer].add(e)
            }
        }

        return layers.indexOfFirst { layer -> layer.contains(event) }
    }

    private fun checkOverlap(event1: Event, event2: Event): Boolean {
        val event1Start = convertTimeToFloat(event1.startTime)
        val event1End = convertTimeToFloat(event1.endTime)
        val event2Start = convertTimeToFloat(event2.startTime)
        val event2End = convertTimeToFloat(event2.endTime)
        return (event1Start < event2End && event1End > event2Start)
    }

    @Composable
    fun TimeGridSlot(time: String, scale: Int, showTimes: Boolean) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height((60 * scale).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showTimes) {
                Text(
                    text = time,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Red),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Divider(color = Color.Gray, thickness = 1.dp)
        }
    }

    @Composable
    fun TimeGrid(
        scale: Int,
        scrollState: ScrollState = rememberScrollState(),
        showTimes: Boolean
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
        ) {
            TimeGridSlot("12:00 AM", scale, showTimes)
            for (i in 1..11) {
                TimeGridSlot("$i:00 AM", scale, showTimes)
            }
            TimeGridSlot("12:00 PM", scale, showTimes)
            for (i in 1..11) {
                TimeGridSlot("$i:00 PM", scale, showTimes)
            }
        }
    }

    @Composable
    fun EventBreakdownCard(
        timeSlot: TimeSlot,
        scale: Int,
        layerColor: Color,
        context: Context,
        uid: String,
        day: String,
        month: String,
        year: String,
        overlappingEventsCount: Int
    ) {
        val durationInMinutes =
            (convertTimeToFloat(timeSlot.endTime) - convertTimeToFloat(timeSlot.startTime)) * 60

        Card(
            modifier = Modifier
                .fillMaxWidth(1f / overlappingEventsCount)
                .height((durationInMinutes * scale).dp)
                .padding(vertical = 1.dp)
                .clickable {
                    FirestoreHelper().getEvent(
                        uid,
                        day,
                        month,
                        year,
                        timeSlot.text
                    ) { id, _ ->
                        if (id == null) {
                            Toast
                                .makeText(
                                    context,
                                    "Event not found",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    }
                },
            colors = CardDefaults.elevatedCardColors(
                containerColor = layerColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (timeSlot.text.isNotEmpty()) {
                    Text(
                        text = timeSlot.text,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .fillMaxWidth(1f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
//                    Text(
//                        text = ("${timeSlot.startTime} - ${timeSlot.endTime}"),
//                        modifier = Modifier.padding(end = 4.dp),
//                        style = MaterialTheme.typography.bodyMedium
//                    )
                }
            }
        }
    }

    @Composable
    fun TimeSlotLayer(
        timeSlots: List<TimeSlot>,
        scale: Int,
        layer: Int,
        context: Context,
        uid: String,
        day: String,
        month: String,
        year: String,
        scrollState: ScrollState
    ) {
        val layerColor = when (layer) {
            0 -> MaterialTheme.colorScheme.primary
            1 -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.error
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(1f)
                .padding(horizontal = 8.dp)
                .verticalScroll(scrollState)
                .background(Color.Transparent),
            contentAlignment = Alignment.TopEnd
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalAlignment = Alignment.End
            ) {
                val startTimeMinutes = (convertTimeToFloat(timeSlots[0].startTime)) * 60
                Spacer(modifier = Modifier.height(((30 + startTimeMinutes) * scale).dp)) // top padding
                val displayedEvents = mutableMapOf<String, Int>()
                // sort time slots by start time
                val timeSlots = timeSlots.sortedBy { convertTimeToFloat(it.startTime) }
                for (i in timeSlots.indices) {
                    val timeSlot = timeSlots[i]

                    val nextEventStartTime = if (i < timeSlots.size - 1) convertTimeToFloat(timeSlots[i + 1].startTime) else 24f
                    val currentEventEndTime = convertTimeToFloat(timeSlot.endTime)
                    val differenceInMinutes = (nextEventStartTime - currentEventEndTime) * 60

                    val overlappingEvents = timeSlots.filter{ checkOverlap(
                        Event().apply { startTime = it.startTime; endTime = it.endTime },
                        Event().apply { startTime = timeSlot.startTime; endTime = timeSlot.endTime }
                    )}

                    if (overlappingEvents.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            for (timeSlot in overlappingEvents) {
                                val count = displayedEvents.getOrDefault(timeSlot.id, 0)
                                if (count < overlappingEvents.size && !displayedEvents.containsKey(timeSlot.id)) {
                                    EventBreakdownCard(timeSlot, scale, layerColor, context, uid, day, month, year, overlappingEvents.size)
                                    displayedEvents[timeSlot.id] = count + 1
                                }
                            }
                        }
                    } else if (!displayedEvents.containsKey(timeSlot.id) || displayedEvents[timeSlot.id]!! < 1)
                    {
                        EventBreakdownCard(timeSlot, scale, layerColor, context, uid, day, month, year, 0)
                        displayedEvents[timeSlot.id] = 1
                    }

                    Spacer(modifier = Modifier.height((differenceInMinutes * scale).dp))
                }
                Spacer(modifier = Modifier.height((60 * scale).dp))
            }
        }
    }

    @Preview
    @Composable
    fun EventBreakdownPopulatedPreview() {
        ProtifyTheme {
            Surface {
                DailySchedule(scale = 1.0, eventList = createListEvent())
            }
        }
    }
}