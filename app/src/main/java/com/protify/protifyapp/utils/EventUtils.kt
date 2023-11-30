package com.protify.protifyapp.utils

import java.util.Calendar
import java.text.SimpleDateFormat

// Utility class for events. Event sorting, filtering, etc.
class EventUtils {
    // Gets the current date from the system.
    fun parseCurrentDate(): String {
        val time = Calendar.getInstance().time
        val formatter = SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault())
        return formatter.format(time)
    }
}