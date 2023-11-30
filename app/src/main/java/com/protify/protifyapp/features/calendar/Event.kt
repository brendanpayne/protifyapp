package com.protify.protifyapp.features.calendar

import com.protify.protifyapp.features.events.Attendee

// Event class for defining events to be displayed on the calendar.
class Event {
    val id: String = ""
    var title: String = ""
    var description: String = ""
    var location: String = ""
    var startTime: String = ""
    var endTime: String = ""
    var attendees: List<Attendee> = listOf()
}