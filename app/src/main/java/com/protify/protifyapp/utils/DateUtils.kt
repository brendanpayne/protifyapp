package com.protify.protifyapp.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Utility class for dates. Date formatting, parsing, calculations, etc.
class DateUtils {
    fun getCurrentDate(): Date {
        return Calendar.getInstance().time
    }

    fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
        return formatter.format(date)
    }

    fun parseDate(date: String): Date {
        return Date()
    }

    fun getDifferenceBetweenDates(date1: Date, date2: Date): Int {
        return 0
    }
}