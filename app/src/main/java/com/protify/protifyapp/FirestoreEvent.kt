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
fun validateEvent(event: FirestoreEvent): List<Error> {
    val errors = mutableListOf<Error>()
    validateName(event.name)?.let { errors.add(it) }
    event.description?.let { validateDescription(it)?.let { errors.add(it) } }
    event.importance?.let { validateImportance(it)?.let { errors.add(it) } }
    return errors
}
private fun validateName(name: String): Error? {
    if (name.length > 50) {
        return Error("Name is too long")
    }
    return null
}
private fun validateDescription(description: String): Error? {
    if (description.length > 250) {
        return Error("Description is too long")
    }
    return null
}
private fun validateImportance(importance: Int): Error? {
    if (importance < 1 || importance > 5) {
        return Error("Importance must be between 1 and 5")
    }
    return null
}
