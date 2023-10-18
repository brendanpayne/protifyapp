package com.protify.protifyapp

import android.provider.CalendarContract.Attendees
import java.time.LocalDateTime

data class FirestoreEvent(
    val name: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val location: String?, //TODO: Change to location object
    val description: String?,
    val timeZone: String?, //TODO: Create a timezone object
    val importance: Int?,
    val attendees: Attendees?

)
