package com.protify.protifyapp.features.calendar

import com.protify.protifyapp.features.events.Attendee

// Event class for defining events to be displayed on the calendar.
class Event {
    val title: String = ""
    val description: String = ""
    val location: String = ""
    val startTime: String = ""
    val endTime: String = ""
    val attendees: List<Attendee> = listOf()
}