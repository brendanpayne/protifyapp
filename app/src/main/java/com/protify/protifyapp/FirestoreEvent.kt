package com.protify.protifyapp

import com.protify.protifyapp.features.events.Attendee
import java.time.LocalDateTime

class FirestoreEvent(
    var name: String,
    val nameLower: String,
    var startTime: LocalDateTime,
    var endTime: LocalDateTime,
    var location: String? = "",
    var description: String? = "",
    var timeZone: String? = "",
    var importance: Int? = 1,
    var attendees: List<Attendee>? = null,
    var rainCheck: Boolean = false,
    var isRaining: Boolean = false,
    var mapsCheck: Boolean = false,
    var distance: Int = 0,
    var isOutside: Boolean = false,
    var isOptimized: Boolean = false,
    var isAiSuggestion: Boolean = false,
    var isUserAccepted: Boolean = false,
    var id: String = "",
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
