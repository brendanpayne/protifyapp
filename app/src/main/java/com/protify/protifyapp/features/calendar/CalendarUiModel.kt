package com.protify.protifyapp.features.calendar

import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Main class that represents the calendar. Handles adding, removing, and editing events.
class CalendarUiModel(
    var selectedDate: Date,
    var visibleDates: List<Date>,
) {
    val startDate: Date = visibleDates.first()
    val endDate: Date = visibleDates.last()
    data class Date(
        val date: LocalDate,
        var isSelected: Boolean,
        val isToday: Boolean,
        var hasEvents: Boolean,
    ) {
        var events: List<Event> = listOf()
        val day: String = date.format(DateTimeFormatter.ofPattern("E"))
    }
}