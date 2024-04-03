package com.protify.protifyapp.features.events

import android.content.Context
import androidx.compose.runtime.Composable
import com.protify.protifyapp.FirestoreEvent
import com.protify.protifyapp.FirestoreHelper

class EditEvent (private val eventToEdit: FirestoreEvent) : AddEvent() {
    init {
        name = eventToEdit.name
        startTime = eventToEdit.startTime
        endTime = eventToEdit.endTime
        location = eventToEdit.location
        description = eventToEdit.description
        timeZone = eventToEdit.timeZone
        importance = eventToEdit.importance!!
        attendees = eventToEdit.attendees!!
        rainCheck = eventToEdit.rainCheck
        isOutside = eventToEdit.isOutside
        isOptimized = eventToEdit.isOptimized
    }

    override fun saveEvent(
        isOutside: Boolean,
        isOptimized: Boolean,
        user: String?,
        navigateBack: () -> Unit,
        context: Context
    ) {
        val firestoreEvent = FirestoreEvent(
            name = name,
            nameLower = name.trim().lowercase(),
            startTime = startTime,
            endTime = endTime,
            location = location,
            description = description,
            timeZone = timeZone,
            importance = importance,
            attendees = attendees,
            rainCheck = rainCheck,
            isOutside = isOutside,
            isOptimized = isOptimized
        )
        val errors = firestoreEvent.validateEvent(firestoreEvent)
        if (errors.isEmpty() && user != null && !dateError && isTimeSelected()) {
            FirestoreHelper().modifyEvent(
                uid = user,
                eventId = eventToEdit.id,
                event = firestoreEvent,
                callback = { navigateBack() }
            )
            navigateBack()
        } else {
            super.handleErrors(errors, user, dateError, isTimeSelected(), context)
        }
    }

    @Composable
    override fun AddEventHeader(title: String, onBackClick: () -> Unit) {
        super.AddEventHeader("Edit Event", onBackClick)
    }

    @Composable
    override fun EventCreateItem(
        buttonString: String,
        isOutside: Boolean,
        isOptimized: Boolean,
        user: String?,
        navigateBack: () -> Unit,
        context: Context
    ) {
        super.EventCreateItem ("Save Changes", isOutside, isOptimized, user, navigateBack, context)
    }

    @Composable
    override fun AddEventPage(navigateBack: () -> Unit) {
        eventToEdit.let { event ->
            name = event.name
            startTime = event.startTime
            endTime = event.endTime
            location = event.location
            description = event.description
            timeZone = event.timeZone
            importance = event.importance!!
            attendees = event.attendees!!
            isOutside = event.isOutside
            isOptimized = event.isOptimized
        }

        super.AddEventPage(navigateBack)
    }

    @Composable
    fun EditEventPage(navigateBack: () -> Unit) {
        AddEventPage(navigateBack)
    }
}