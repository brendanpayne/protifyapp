package com.protify.protifyapp.features.calendar

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.protify.protifyapp.R
import com.protify.protifyapp.ui.theme.ProtifyTheme
import java.time.LocalDate

class EventBreakdown {

    data class TimeSlot(
        val startTime: String,
        val text: String,
        val height: Float = 1f,
        val endTime: String = "",
        val layer: Int = 0,
        val id: String = "",
        val isAiSuggestion: Boolean = false
    )

    private fun createListEvent(): List<Event> {
        // placeholder for preview
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
                isAiSuggestion = true
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
                startTime = "12:30"
                endTime = "13:30"
                description = "Description for Event 3"
                location = "Location for Event 3"
                attendees = listOf()
                id = "4"
                isAiSuggestion = true
            },
            Event().apply {
                title = "Event 5"
                startTime = "12:10"
                endTime = "14:00"
                description = "Description for Event 4"
                location = "Location for Event 4"
                attendees = listOf()
                id = "5"
                isAiSuggestion = false
            },
            Event().apply {
                title = "Event 6"
                startTime = "14:30"
                endTime = "15:30"
                description = "Description for Event 4"
                location = "Location for Event 4"
                attendees = listOf()
                id = "6"
                isAiSuggestion = true
            }
        )
    }

    @Composable
    fun DailySchedule(
        scale: Double = 1.0,
        eventList: List<Event> = listOf(),
        date: LocalDate,
        showTimes: Boolean = true,
        navController: NavController,
        showOptimizedEvents: MutableState<Boolean>
    ) {
        val timeSlots = createListTimeSlot(eventList)
        val scaledValue = scale.toInt()
        timeSlots.maxOfOrNull { it.height } ?: 0f
        val scrollState = rememberScrollState()
        val layers = timeSlots.map { it.layer }.distinct()
        val sortedEventList = eventList.sortedBy { convertTimeToFloat(it.startTime) } // Sort events by start time to start on the right scroll position

        val startTime = if (eventList.isNotEmpty())
            convertTimeToFloat(sortedEventList[0].startTime) else 9f // default 0900
        val startPosition = startTime * 60 * scaledValue
        LaunchedEffect(
            key1 = date
        ) {// scroll to start position when the user changes the day
            scrollState.scrollTo(startPosition.toInt() * 2) // This scrolls the screen far enough to see the first event
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            TimeGrid(scaledValue, scrollState, showTimes)
            for (layer in layers) {
                TimeSlotLayer(
                    timeSlots,
                    scaledValue,
                    layer,
                    date,
                    scrollState,
                    navController,
                    showOptimizedEvents
                )
            }
        }
    }

    private fun createListTimeSlot(events: List<Event> = createListEvent()): List<TimeSlot> {
        val timeSlots = mutableListOf<TimeSlot>()

        for (event in events) {
            val layer = 0
            val height = (convertTimeToFloat(event.endTime) - convertTimeToFloat(event.startTime))
            timeSlots.add(TimeSlot(event.startTime, event.title, height, event.endTime, layer, event.id, event.isAiSuggestion))
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
            TimeGridSlot("12:00 AM", scale, showTimes)
            Spacer(modifier = Modifier.height((60 * scale).dp))
        }
    }

    @Composable
    fun EventBreakdownCard(
        timeSlot: TimeSlot,
        scale: Int,
        date: LocalDate,
        overlappingEventsCount: Int,
        navController: NavController
    ) {
        val durationInMinutes =
            (convertTimeToFloat(timeSlot.endTime) - convertTimeToFloat(timeSlot.startTime)) * 60
        val layerColor = when (timeSlot.isAiSuggestion) {
            true -> MaterialTheme.colorScheme.secondary
            false -> MaterialTheme.colorScheme.primary
        }

        Card(
            modifier = Modifier
                .height((durationInMinutes * scale).dp)
                .width(320.dp / overlappingEventsCount)
                .padding(vertical = 1.dp)
                .clickable {
                    navController.navigate("eventDetails/${date}/${timeSlot.id}")
                },
            colors = CardDefaults.elevatedCardColors(
                containerColor = layerColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                ) {
                    if (timeSlot.text.isNotEmpty()) {
                        Text(
                            text = timeSlot.text,
                            modifier = Modifier
                                .padding(horizontal = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (timeSlot.endTime.isNotEmpty() && timeSlot.startTime.isNotEmpty()) {
                        Text(
                            text = "${timeSlot.startTime} - ${timeSlot.endTime}",
                            modifier = Modifier
                                .padding(horizontal = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (timeSlot.isAiSuggestion) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.outline_auto_awesome_24),
                        contentDescription = "AI Suggested Event",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.TopEnd)
                    )
                }
            }
        }
    }

    @Composable
    fun TimeSlotLayer(
        timeSlots: List<TimeSlot>,
        scale: Int,
        layer: Int,
        date: LocalDate,
        scrollState: ScrollState,
        navController: NavController,
        showOptimizedEvents: MutableState<Boolean>
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(1f)
                .padding(horizontal = 8.dp)
                .verticalScroll(scrollState)
                .background(Color.Transparent),
            contentAlignment = Alignment.TopEnd
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Top
            ) {
                val lastEventEndtime = timeSlots.maxOfOrNull { convertTimeToFloat(it.endTime) } ?: 24f
                val slots = if (showOptimizedEvents.value)
                    timeSlots.sortedBy { convertTimeToFloat(it.startTime) }
                else timeSlots.filter { !it.isAiSuggestion }.sortedBy { convertTimeToFloat(it.startTime) }
                val startTimeMinutes = (convertTimeToFloat(slots[0].startTime)) * 60
                Spacer(modifier = Modifier.height(((30 + startTimeMinutes) * scale).dp)) // top padding
                val displayedEvents = mutableMapOf<String, Int>()
                var count = 0

                val groupedSlots = groupOverlappingEvents(slots.toMutableList())


                for (i in groupedSlots.indices) {
                    val group = groupedSlots[i]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                    ) {
                        for (t in group) {
                            // Add each item to a column, with the start time of the first event minus the start time of the current event
                            val overlapSpacing = (convertTimeToFloat(t.startTime) - convertTimeToFloat(group.first().startTime))
                            Column {
                                Spacer(modifier = Modifier.height((overlapSpacing * 60 * scale).dp))
                                if (!displayedEvents.containsKey(t.id)) {
                                        EventBreakdownCard(
                                            t,
                                            scale,
                                            date,
                                            group.size,
                                            navController = navController
                                        )
                                        count++
                                        displayedEvents[t.id] = 1
                                    }

                            }
                        }
                    }

                    // Add a spacer after each group except the last one
                    if (i < groupedSlots.size - 1) {
                        // Sort group by end time
                        val endTimeGroupSorted = group.sortedBy { it.endTime }
                        val nextGroupStartTimeSorted = groupedSlots[i + 1].sortedBy { it.startTime }
                        val currentGroupEndTime = convertTimeToFloat(endTimeGroupSorted.last().endTime)
                        val nextGroupStartTime = convertTimeToFloat(nextGroupStartTimeSorted.first().startTime)
                        val differenceInMinutes = (nextGroupStartTime - currentGroupEndTime) * 60
                        Spacer(modifier = Modifier.height((differenceInMinutes * scale).dp))
                    }
                }
                Spacer(modifier = Modifier.height(((24 - lastEventEndtime) * 60 * scale).dp)) // bottom padding
            }
        }
    }
    private fun checkOverlapBoosted(timeSlot1: EventBreakdown.TimeSlot, timeSlot2: EventBreakdown.TimeSlot): Boolean {
        val timeSlot1Start = convertTimeToFloat(timeSlot1.startTime)
        val timeSlot1End = convertTimeToFloat(timeSlot1.endTime)
        val timeSlot2Start = convertTimeToFloat(timeSlot2.startTime)
        val timeSlot2End = convertTimeToFloat(timeSlot2.endTime)
        return (timeSlot1Start < timeSlot2End && timeSlot1End > timeSlot2Start)
    }

    fun groupOverlappingEvents(slots: MutableList<TimeSlot>): List<List<TimeSlot>> {
        val groupedSlots = mutableListOf<List<TimeSlot>>()

        while (slots.isNotEmpty()) {
            val group = mutableListOf<TimeSlot>()
            findOverlappingEvents(slots[0], slots, group)
            groupedSlots.add(group)
        }

        return groupedSlots
    }

    fun findOverlappingEvents(event: TimeSlot, slots: MutableList<TimeSlot>, group: MutableList<TimeSlot>) {
        group.add(event)
        slots.remove(event)

        val overlappingEvents = slots.filter { checkOverlapBoosted(event, it) }.toMutableList()
        for (overlappingEvent in overlappingEvents) {
            findOverlappingEvents(overlappingEvent, slots, group)
        }
    }


    @Preview
    @Composable
    fun EventBreakdownPopulatedPreview() {
        ProtifyTheme {
            Surface {
                DailySchedule(
                    scale = 1.0,
                    eventList = createListEvent(),
                    date = LocalDate.now(),
                    showTimes = true,
                    navController = NavController(LocalContext.current),
                    showOptimizedEvents = remember { mutableStateOf(false) }
                )
            }
        }
    }

    @Preview
    @Composable
    fun EventBreakdownPopulatedAIPreview() {
        ProtifyTheme {
            Surface {
                DailySchedule(
                    scale = 1.0,
                    eventList = createListEvent(),
                    date = LocalDate.now(),
                    showTimes = true,
                    navController = NavController(LocalContext.current),
                    showOptimizedEvents = remember { mutableStateOf(true) }
                )
            }
        }
    }
}