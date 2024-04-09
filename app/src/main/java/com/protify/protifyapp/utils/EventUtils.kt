package com.protify.protifyapp.utils

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.maps.model.LatLng
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.protify.protifyapp.APIKeys
import com.protify.protifyapp.features.events.Attendee
import okhttp3.OkHttpClient
import java.util.Calendar
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// Utility class for events. Event sorting, filtering, etc.
class EventUtils {
    // Gets the current date from the system.
    fun parseCurrentDate(): String {
        val time = Calendar.getInstance().time
        val formatter = SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault())
        return formatter.format(time)
    }

    fun convertToReadableValue(field: String, value: Any): String {
        return when (field) {
            "attendees" -> {
                val attendees = value as List<Attendee>
                if (attendees.isNotEmpty()) attendees.joinToString { it.name } else ""
            }
            "distance" -> {
                val distance = value as Int
                if (distance == 0) "" else "$distance miles"
            }
            "startTime", "endTime" -> {
                val time = value as LocalDateTime
                time.format(DateTimeFormatter.ofPattern("h:mm a"))
            }
            "isAiSuggestion", "isUserAccepted", "isRaining", "isOutside", "isOptimized", "rainCheck", "mapsCheck" -> {
                val bool = value as Boolean
                if (bool) "Yes" else ""
            }
            "name", "nameLower", "id", "timeZone","\$stable" -> {
                "" // never show these fields
            }
            else -> value.toString()
        }
    }
}