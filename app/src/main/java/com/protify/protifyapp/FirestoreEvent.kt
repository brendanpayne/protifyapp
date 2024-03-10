package com.protify.protifyapp

import com.protify.protifyapp.features.events.Attendee
import java.time.LocalDateTime

class FirestoreEvent(
    val name: String,
    val nameLower: String,
    var startTime: LocalDateTime,
    var endTime: LocalDateTime,
    var location: String? = "",
    val description: String? = "",
    val timeZone: String? = "",
    val importance: Int? = 1,
    val attendees: List<Attendee>? = null,
    val rainCheck: Boolean = false,
    val isRaining: Boolean = false,
    val mapsCheck: Boolean = false,
    val distance: Int = 0,
    val isOutside: Boolean = false,
    val isOptimized: Boolean = false,
    val isAiSuggestion: Boolean = false,
    var isUserAccepted: Boolean = false,
) {
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
        if (name == "") {
            return Error("Name cannot be empty")
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
}
