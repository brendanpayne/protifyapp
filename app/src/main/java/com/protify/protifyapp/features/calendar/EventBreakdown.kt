package com.protify.protifyapp.features.calendar

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import com.protify.protifyapp.FirestoreHelper
import java.time.LocalDateTime
import java.time.LocalTime

class EventBreakdown {

    @Composable
    @Preview
    fun DailySchedulePreview() {
        DailySchedule()
    }
    /** This is the main composable for the Daily Schedule. It takes in a list of events and renders them on the screen
     * @param backgroundColor This is the color "behind" the events
     * @param RenderClock This is set to false by default because the current clock is deprecated. It will be removed in the future
     * @param Scale This is the scale of the time slots. If you set it larger, the scroll will be longer and vise versa
     * @param ClockColor This is the color of the line that represents the current time
     * @param ClockHeight This is how thick the line that represents the current time is
     * @param EventList List<Event> Pass in the events for one day. If you don't pass in any events, it will render a mock list of events
     * @param uid String The user id of the user
     * @param day String The day of the events
     * @param month String The month of the events
     * @param year String The year of the events
     */
    @Composable
    fun DailySchedule(
        backgroundColor: Color = Color.White,
        RenderClock: Boolean = false,
        Scale: Double = 1.0,
        ClockColor: Color = Color.Red,
        ClockHeight: Int = 5,
        EventList: List<Event> = listOf(),
        uid: String = "",
        day: String = "",
        month: String = "",
        year: String = ""
    ) {

        // Make a context
        val context: Context = LocalContext.current
        // Create a list of time slots
        var timeSlots: List<TimeSlot>
        if (EventList.isEmpty()) {
            timeSlots = createListTimeSlot()
        } else {
            timeSlots = createListTimeSlot(EventList)
        }

        // This is the scale for the time slots
        val scale: Int = (Scale * 550).toInt()
        // this is for scaling the time slots
        val maxValue = timeSlots.maxOfOrNull { it.height } ?: 0f

        // Make a new scroll state. Since both of the columns will share this state, they will scroll together.
        var scrollState = rememberScrollState()

        // get the layers of the time slots
        val layers = timeSlots.map { it.layer }.distinct()
        // Render the time slots by layer
        for (layer in layers) {
            timeSlotLayer(timeSlots, scale, scrollState, layer, context, uid, day, month, year)
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
        // This is the underlay that displays the horizontal lines every hour
        Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    // Put this on the bottom layer
                    .zIndex(-99f)
        ) {
            for (i in 0..24) {
                val time = if (i < 10) "0$i:00" else "$i:00"
                // This calculates the height in between lines to scale the same as the time slots
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
                            .background(backgroundColor)
                ) {
                    // Make a divider
                    Divider(modifier = Modifier
                        .height(1.dp)
                        .fillMaxWidth()
                        // This is the line itself (since it's on an underlay layer, it will be "greyed out")
                        .background(Color.Black))
                }
            }
        }
        // This is the clock that displays the current time
        if (RenderClock) {
            Clock(scrollState, layers, maxValue, scale, ClockHeight, ClockColor)
        }
    }

    /** Think of a time slot as a box that repesents an event and the time it takes up. These are used in collumnar fashion and are "stacked" on top of each other
     *
     */
    data class TimeSlot(
        val startTime: String,
        val text: String,
        val color: Color,
        val height: Float = 1f,
        val endTime: String = "",
        val layer: Int = 0)

    /** Create a mock list of events for testing
     * @return List<Event>
     */
    fun createListEvent(): List<Event> {
        val event1 = Event()
        event1.title = "Breakfast"
        event1.description = "Eggs and bacon"
        event1.location = "Home"
        event1.startTime = "06:00"
        event1.endTime = "6:30"

        val event2 = Event()
        event2.title = "Work"
        event2.description = "Coding"
        event2.location = "Home"
        event2.startTime = "08:00"
        event2.endTime = "17:00"

        val event3 = Event()
        event3.title = "Standup"
        event3.description = "Daily standup meeting"
        event3.location = "Home"
        event3.startTime = "19:00"
        event3.endTime = "20:30"

        val event4 = Event()
        event4.title = "Gym"
        event4.description = "Workout"
        event4.location = "Gym"
        event4.startTime = "20:45"
        event4.endTime = "22:00"

        val event5 = Event()
        event5.title = "Study"
        event5.description = "Steak and potatoes"
        event5.location = "Home"
        event5.startTime = "21:43"
        event5.endTime = "23:00"

        return listOf(event1, event2, event3, event4, event5)
    }

    /** Converts a list of pre-defined events into a list of time slots ONLY FOR TESTING
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
        val eventbyStartTime = events.sortedBy { it.startTime }

        // Calculate the time from midnight to the first event and add transparent time slot
        val firstEvent = eventbyStartTime.first()
        timeSlots.add(TimeSlot("00:00", "", Color.Transparent, convertTimeToFloat(firstEvent.startTime)))

        // Calculate the size of each event, and make a time slot for empty time between events
        for (i in eventbyStartTime.indices) {
            // Add the event time slot
            val event = eventbyStartTime[i]
            // Calculate which layer the event should be on
            val layer = calculateLayer(event, eventbyStartTime)
            // If there is a next event, calculate the time between the two events
            val nextEvent = if (i < eventbyStartTime.size - 1) eventbyStartTime[i + 1] else null
            // Calculate the height of the event time slot
            val height = (convertTimeToFloat(event.endTime) - convertTimeToFloat(event.startTime))
            timeSlots.add(TimeSlot(event.startTime, event.title, Color.LightGray, height, event.endTime, layer))
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
        val lastEvent = eventbyStartTime.last()
        timeSlots.add(TimeSlot(lastEvent.endTime, "", Color.Transparent, convertTimeToFloat("24:00") - convertTimeToFloat(lastEvent.endTime)))
        return timeSlots
    }
    /** Converts a list of events into a list of time slots
     * This essentially stacks all of the events into a column by putting transparent time slots between events
     * This is designed to be exclusively used for the DailySchedule composable
     * @param events List<Event>
     * @return List<TimeSlot>
     */
    fun createListTimeSlot(events: List<Event>): List<TimeSlot> {
        // Create a list of time slots
        val timeSlots = mutableListOf<TimeSlot>()


        // sort events by start time
        val eventbyStartTime = events.sortedBy { it.startTime }

        // Calculate the time from midnight to the first event and add transparent time slot
        val firstEvent = eventbyStartTime.first()
        timeSlots.add(TimeSlot("00:00", "", Color.Transparent, convertTimeToFloat(firstEvent.startTime)))

        // Calculate the size of each event, and make a time slot for empty time between events
        for (i in eventbyStartTime.indices) {
            // Add the event time slot
            val event = eventbyStartTime[i]
            // Calculate which layer the event should be on
            val layer = calculateLayer(event, eventbyStartTime)
            // If there is a next event, calculate the time between the two events
            val nextEvent = if (i < eventbyStartTime.size - 1) eventbyStartTime[i + 1] else null
            // Calculate the height of the event time slot
            val height = (convertTimeToFloat(event.endTime) - convertTimeToFloat(event.startTime))
            timeSlots.add(TimeSlot(event.startTime, event.title, Color.LightGray, height, event.endTime, layer))
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
        val lastEvent = eventbyStartTime.last()
        timeSlots.add(TimeSlot(lastEvent.endTime, "", Color.Transparent, convertTimeToFloat("24:00") - convertTimeToFloat(lastEvent.endTime)))
        return timeSlots
    }
    /** Renders the time slots for a given layer. This is because when we display the events on the calendar, we have to layer overlapping events for them to display properly
     * @param timeSlots List<TimeSlot>
     * @param layer Int
     * @return List<TimeSlot> (for the given layer)
     */
    fun renderTimeSlotLayer(timeSlots: List<TimeSlot>, layer: Int): List<TimeSlot> {
        val layerTimeSlots = timeSlots.filter { it.layer == layer }
        // Remove the time slots with no endTime
        val timeSlotsWithEndTime = layerTimeSlots.filter { it.endTime != "" }
        // Calculate the time from midnight to the first event and add transparent time slot
        val firstEvent = timeSlotsWithEndTime.first()
        val firstTimeSlot = TimeSlot("00:00", "", Color.Transparent, convertTimeToFloat(firstEvent.startTime))
        val timeSlotsLayer = mutableListOf(firstTimeSlot)
        // Calculate the size of each event, and make a time slot for empty time between events
        for (i in timeSlotsWithEndTime.indices) {
            // Add the event time slot
            val event = timeSlotsWithEndTime[i]
            // If there is a next event, calculate the time between the two events
            val nextEvent = if (i < timeSlotsWithEndTime.size - 1) timeSlotsWithEndTime[i + 1] else null
            // Calculate the height of the event time slot (All of the events together should add up to 24)
            val height = (convertTimeToFloat(event.endTime) - convertTimeToFloat(event.startTime))
            timeSlotsLayer.add(TimeSlot(event.startTime, event.text, Color.LightGray, height, event.endTime, layer))
            // If there is a next event, calculate the time between the two events
            if (nextEvent != null) {
                val timeBetweenEvents = convertTimeToFloat(nextEvent.startTime) - convertTimeToFloat(event.endTime)
                // If there is time between the two events, add a transparent time slot
                if (timeBetweenEvents > 0) {
                    timeSlotsLayer.add(TimeSlot(event.endTime, "", Color.Transparent, timeBetweenEvents))
                }
            }
        }
        // Calculate the time from the last event to midnight and add transparent time slot
        val lastEvent = timeSlotsWithEndTime.last()
        timeSlotsLayer.add(TimeSlot(lastEvent.endTime, "", Color.Transparent, convertTimeToFloat("24:00") - convertTimeToFloat(lastEvent.endTime)))
        return timeSlotsLayer
    }
    /** Helper function to convert the the startTime from event into a float
     * @param time String
     * @return Float (time in hours)
     * @sample convertTimeToFloat("06:00") returns 6.0
     */
    private fun convertTimeToFloat(time: String): Float {
        val timeArray = time.split(":")
        val hour = timeArray[0].toFloat() * 60
        val minute = timeArray[1].toFloat()
        return (hour + minute) / 60
    }
    /** Helper function to calculate the layer of an event
     * @param event Event
     * @param events List<Event>
     * @return Int (layer)
     */
    private fun calculateLayer(event: Event, events: List<Event>): Int {
        // Sort the events by their duration in descending order
        val sortedEvents = events.sortedByDescending { convertTimeToFloat(it.endTime) - convertTimeToFloat(it.startTime) }

        // Initialize the list of layers
        val layers = mutableListOf<MutableList<Event>>()

        // Iterate over the sorted events
        for (e in sortedEvents) {
            // Find the first layer that doesn't overlap with the event
            val layer = layers.indexOfFirst { layer -> layer.none { checkOverlap(e, it) } }

            if (layer == -1) {
                // If all layers overlap with the event, create a new layer and add the event to it
                layers.add(mutableListOf(e))
            } else {
                // If a layer was found that doesn't overlap with the event, add the event to that layer
                layers[layer].add(e)
            }
        }

        // The layer of an event is the index of the layer it was added to
        return layers.indexOfFirst { layer -> layer.contains(event) }
    }
    private fun checkOverlap(event1: Event, event2: Event): Boolean {
        val event1Start = convertTimeToFloat(event1.startTime)
        val event1End = convertTimeToFloat(event1.endTime)
        val event2Start = convertTimeToFloat(event2.startTime)
        val event2End = convertTimeToFloat(event2.endTime)
        return (event1Start < event2End && event1End > event2Start)
    }
    /** This represents one layer of time slots. This is used to layer overlapping events onto each other
     * @param timeSlots List<TimeSlot>
     * @param scale Int
     * @param scrollState ScrollState
     * @param layer Int
     */
    @Composable
    fun timeSlotLayer(
        timeSlots: List<TimeSlot>,
        scale: Int,
        scrollState: ScrollState,
        layer: Int,
        context: Context,
        uid: String,
        day: String,
        month: String,
        year: String
        ) {
        // Color the layers
        val layerColor = when (layer) {
            0 -> Color.LightGray
            1 -> Color.Gray
            2 -> Color.DarkGray
            else -> Color.Red
        }
       // This is to scale the time slots
        val maxValue = timeSlots.maxOfOrNull { it.height } ?: 0f
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {

            renderTimeSlotLayer(timeSlots, layer).forEach { timeSlot ->
                Row(
                    modifier = Modifier
                        // Scale by layer
                        .fillMaxWidth(if (layer == 0) 0.9f else 1f - (layer * 0.3f))
                        // Align the time slots to the right
                        .align((Alignment.End))
                        // This adjusts the scale
                        .height(timeSlot.height / maxValue * scale.dp)
                        // Whatever color the time slot is assigned
                        .background(if (timeSlot.color == Color.Transparent) Color.Transparent else layerColor)
                        // Put a border around each time slot, make it invisible if the time slot is a "separator"
                        .border(
                            1.dp,
                            if (timeSlot.color == Color.Transparent) Color.Transparent else Color.Black
                        )
                        // Layer the time slots
                        .zIndex(layer.toFloat() + 1)
                        .padding(start = 4.dp, end = 8.dp)
                        .clickable {
                            // Fetch the event from Firestore
                            FirestoreHelper().getEvent(uid, day, month, year, timeSlot.text) { documentid, event ->
                                // TODO: Open the event in a edit event screen ( need to figure out how to pass event into nav controller)
                                if (documentid != null) {

                                } else {
                                    // Display a toast that the event was not found
                                    Toast.makeText(context, "Event not found", Toast.LENGTH_SHORT).show()
                                }



                            }
                        }
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

    }
    @Deprecated("This blocks all of the events from being clicked. Do not use")
    @Composable
    private fun Clock(
        scrollState: ScrollState,
        layers: List<Int>,
        maxValue: Float,
        scale: Int,
        height: Int = 5,
        backgroundColor: Color = Color.Red) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .zIndex(
                    layers
                        .max()
                        .toFloat()
                ) // Put this on the top layer
        ) {
            // Get the current time
            val currentTime by remember { mutableStateOf(LocalDateTime.now()) }
            // Turn the current time into a string with the format of "HH:mm"
            val targetTime by remember { mutableStateOf(LocalTime.of(currentTime.hour, currentTime.minute).toString()) }
            val dividerHeight = (convertTimeToFloat(targetTime) - convertTimeToFloat("00:00")) / maxValue * scale.dp
            val secondDividerHeight = (convertTimeToFloat("24:00") - convertTimeToFloat(targetTime)) / maxValue * scale.dp

            // Create an invisible row that that covers the entire height before the current time
            Row(
                modifier = Modifier
                    .height(dividerHeight)
                    .fillMaxWidth()
                    .background(Color.Transparent)
            ) {
                // Leave empty
            }
            // Create a red box that represents the current time
            Box (
                modifier = Modifier
                    .height(height.dp)
                    .fillMaxWidth()
                    .background(backgroundColor)
            )
            // Create an invisible row that that covers the entire height after the current time
            Row(
                modifier = Modifier
                    .height(secondDividerHeight)
                    .fillMaxWidth()
                    .background(Color.Transparent)


            ) {
                // Leave empty
            }
        }
    }
}