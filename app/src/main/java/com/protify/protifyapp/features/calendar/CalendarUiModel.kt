package com.protify.protifyapp.features.calendar

import com.protify.protifyapp.utils.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Main class that represents the calendar. Handles adding, removing, and editing events.
class CalendarUiModel(
    val selectedDate: Date,
    val visibleDates: List<Date>,
) {
    val startDate: Date = visibleDates.first()
    val endDate: Date = visibleDates.last()
    data class Date(
        val date: LocalDate,
        val isSelected: Boolean,
        val isToday: Boolean,
    ) {
        val day: String = date.format(DateTimeFormatter.ofPattern("E"))
    }
}