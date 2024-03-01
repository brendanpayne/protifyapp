package com.protify.protifyapp.features.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex

class EventBreakdown {

    @Composable
    @Preview
    fun DailySchedulePreview() {
        DailySchedule()
    }
    // This will, at some point, take a list of events and display them on the calendar
    @Composable
    fun DailySchedule() {
        // Create a list of time slots
        val timeSlots = createListTimeSlot()
        // This is the scale for the time slots
        val scale = 550
        // this is for scaling the time slots
        val maxValue = timeSlots.maxOfOrNull { it.height } ?: 0f

        // Make a new scroll state. Since both of the columns will share this state, they will scroll together.
        var scrollState = rememberScrollState()

        Column(
                modifier = Modifier
                        .verticalScroll(scrollState)
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
        ) {

            timeSlots.forEach { timeSlot ->
                Row(
                        modifier = Modifier
                                // Take up 90% of the width on the right side, or if it's overlapping, take up 80%
                                .fillMaxWidth(if (timeSlot.color == Color.Red) 0.8f else 0.9f)
                                // Push overlapping events on x axis
                                .offset(x = if (timeSlot.color == Color.Red) 20.dp else 0.dp)
                                // Slightly round corners to make it look cleaner
                                .clip(RoundedCornerShape(12.dp))
                                // Align the time slots to the right
                                .align((Alignment.End))
                                // This adjusts the scale
                                .height(timeSlot.height / maxValue * scale.dp)
                                // Whatever color the time slot is assigned
                                .background(timeSlot.color)
                                // Put a border around each time slot
                                .border(1.dp, Color.Black)
                                // Put this on the top layer
                                .zIndex(if (timeSlot.color == Color.Red) 2f else 1f)
                                .padding(start = 4.dp, end = 8.dp)
                ) {
                    // If the time slot has text, display it. This is needed because the time between
                    // events is a transparent time slot, and you don't want anything displayed there.
                    if (timeSlot.text.isNotEmpty()) {
                        Text(
                                text = timeSlot.text,
                                modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        // This adjusts the scale of the start and end time text
                                        .fillMaxWidth(0.2f)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f)) // This puts the time slot text on the right
                    if (timeSlot.text.isNotEmpty()) {
                        Text(
                                text = ("${timeSlot.startTime} - ${timeSlot.endTime}"),
                                modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))

            }
        }
        // This is the time scale on the left side with the same scroll state as the time slots
        Column(
                modifier = Modifier
                        .fillMaxWidth(0.1f)
                        .background(Color.LightGray)
                        .border(1.dp, Color.Black)
                        // Put this on the top layer
                        .zIndex(0f)
                        // Lock scrolling with events list
                        .verticalScroll(scrollState),
        ) {
            for (i in 0..24) {
                val time = if (i < 10) "0$i:00" else "$i:00"

                val height = if (i < 24) {
                    val nextTime = if (i + 1 < 10) "0${i + 1}:00" else "${i + 1}:00"
                    (convertTimeToFloat(nextTime) - convertTimeToFloat(time)) / maxValue * scale.dp
                } else {
                    (convertTimeToFloat(time)) / maxValue * scale.dp
                }
                Row(
                        modifier = Modifier
                                .height(height)
                                .fillMaxWidth()
                                .background(Color.Transparent)
                ) {
                    Text(
                            text = time,
                            modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
        // Underlay time scale with dividers
        Column(
                modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        // Put this on the bottom layer
                        .zIndex(-99f)
        ) {
            for (i in 0..24) {
                val time = if (i < 10) "0$i:00" else "$i:00"

                val height = if (i < 24) {
                    val nextTime = if (i + 1 < 10) "0${i + 1}:00" else "${i + 1}:00"
                    (convertTimeToFloat(nextTime) - convertTimeToFloat(time)) / maxValue * scale.dp
                } else {
                    (convertTimeToFloat(time)) / maxValue * scale.dp
                }
                Row(
                        modifier = Modifier
                                .height(height)
                                .fillMaxWidth()
                                .background(Color.Transparent)
                ) {
                    // Make a divider
                    Divider(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.Black))
                }
            }


        }
    }

    data class TimeSlot(val startTime: String, val text: String, val color: Color, val height: Float = 1f, val endTime: String = "")

    /** Create a mock list of events for testing
     * @return List<Event>
     */
    fun createListEvent(): List<Event> {
        val event1 = Event()
        event1.title = "Breakfast"
        event1.description = "Eggs and bacon"
        event1.location = "Home"
        event1.startTime = "06:00"
        event1.endTime = "06:15"

        val event2 = Event()
        event2.title = "Work"
        event2.description = "Coding"
        event2.location = "Home"
        event2.startTime = "08:00"
        event2.endTime = "16:00"

        val event3 = Event()
        event3.title = "Standup"
        event3.description = "Daily standup meeting"
        event3.location = "Home"
        event3.startTime = "19:00"
        event3.endTime = "20:00"

        val event4 = Event()
        event4.title = "Planet Fitness"
        event4.description = "Workout"
        event4.location = "Gym"
        event4.startTime = "19:30"
        event4.endTime = "22:00"

        return listOf(event1, event2, event3, event4)
    }

    /** Converts a list of events into a list of time slots
     * This essentially stacks all of the events into a column by putting transparent time slots between events
     * This is designed to be exclusively used for the DailySchedule composable
     * @return List<TimeSlot>
     */
    fun createListTimeSlot(): List<TimeSlot> {
        // Create a list of time slots
        val timeSlots = mutableListOf<TimeSlot>()
        // Generate events
        val events = createListEvent()

        // sort events by start time
        val eventByStart = events.sortedBy { it.startTime }

        // Calculate the time from midnight to the first event and add transparent time slot
        val firstEvent = eventByStart.first()
        timeSlots.add(TimeSlot("00:00", "", Color.Transparent, convertTimeToFloat(firstEvent.startTime)))

        // Calculate the size of each event, and make a time slot for empty time between events
        for (i in eventByStart.indices) {
            // Add the event time slot
            val event = eventByStart[i]
            // Check for overlapping events
            var overlap = false
            // Check for overlapping events
            for (j in i + 1 until eventByStart.size) {
                if (checkOverlap(event, eventByStart[j])) {
                    overlap = true
                    break
                }
            }
            // If there is a next event, calculate the time between the two events
            val nextEvent = if (i < eventByStart.size - 1) eventByStart[i + 1] else null
            // Calculate the height of the event time slot
            val height = (convertTimeToFloat(event.endTime) - convertTimeToFloat(event.startTime))
            // Color the box red if it's overlapping, else make it light gray
            timeSlots.add(TimeSlot(event.startTime, event.title, if (overlap) Color.Red else Color.LightGray, height, event.endTime))
            // If there is a next event, calculate the time between the two events
            if (nextEvent != null) {
                val timeBetweenEvents = convertTimeToFloat(nextEvent.startTime) - convertTimeToFloat(event.endTime)
            // If there is time between the two events, add a transparent time slot
                if (timeBetweenEvents > 0) {
                timeSlots.add(TimeSlot(event.endTime, "", Color.Transparent, timeBetweenEvents))
            }
            }
        }
        // Calculate the time from the last event to midnight and add transparent time slot
        val lastEvent = eventByStart.last()
        timeSlots.add(TimeSlot(lastEvent.endTime, "", Color.Transparent, convertTimeToFloat("24:00") - convertTimeToFloat(lastEvent.endTime)))
        return timeSlots
    }
    /** Helper function to convert the the startTime from event into a float
     * @param time String
     * @return Float (time in hours)
     */
    fun convertTimeToFloat(time: String): Float {
        val timeArray = time.split(":")
        val hour = timeArray[0].toFloat() * 60
        val minute = timeArray[1].toFloat()
        return (hour + minute) / 60
    }
    /** Helper function to check if two events overlap
     * @param event1 Event
     * @param event2 Event
     * @return Boolean
     */
    fun checkOverlap(event1: Event, event2: Event): Boolean {
        val event1Start = convertTimeToFloat(event1.startTime)
        val event1End = convertTimeToFloat(event1.endTime)
        val event2Start = convertTimeToFloat(event2.startTime)
        val event2End = convertTimeToFloat(event2.endTime)
        return (event1Start < event2End && event1End > event2Start)
    }
}