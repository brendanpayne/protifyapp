package com.protify.protifyapp.features.events

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import com.protify.protifyapp.FirestoreEvent
import com.protify.protifyapp.FirestoreHelper
import java.time.LocalDateTime

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
            isOptimized = !isOptimized
        )
        val errors = firestoreEvent.validateEvent(firestoreEvent)
        if (errors.isEmpty() && user != null && !dateError && isTimeSelected()) {
            FirestoreHelper().modifyEvent(
                uid = user,
                eventId = eventToEdit.id,
                event = firestoreEvent,
                callback = {
                    if(it) {
                        Toast.makeText(context, "Event updated successfully", Toast.LENGTH_SHORT).show()
                        navigateBack()
                    } else {
                        Toast.makeText(context, "Failed to update event", Toast.LENGTH_SHORT).show()
                    }
                           },
            )
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
        user: String?,
        navigateBack: () -> Unit,
        context: Context
    ) {
        super.EventCreateItem ("Save Changes", user, navigateBack, context)
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
            isOptimized = !(event.isOptimized)
            selectedDateGlobal= event.startTime.toLocalDate().atStartOfDay() // Put in date picker
            formattedStartTime = formatTime(event.startTime)
            formattedEndTime = formatTime(event.endTime)
        }

        super.AddEventPage(navigateBack)
    }

    @Composable
    fun EditEventPage(navigateBack: () -> Unit) {
        AddEventPage(navigateBack)
    }
    private fun formatTime(startTime: LocalDateTime): String {
        var formattedStartTime: String
        var hour = startTime.hour
        var minute = startTime.minute

        if (hour >= 12) {
            if (hour > 12) {
                hour -= 12
            }
            formattedStartTime = if (minute < 10) {
                //"$month/$dayOfMonth/$year $hour:0$minute PM"
                "$hour:0$minute PM"
            } else {
                //"$month/$dayOfMonth/$year $hour:$minute PM"
                "$hour:$minute PM"
            }
        } else {
            formattedStartTime = if (minute < 10) {
                //"$month/$dayOfMonth/$year $hour:0$minute AM"
                "$hour:0$minute AM"
            } else {
                //"$month/$dayOfMonth/$year $hour:$minute AM"
                "$hour:$minute AM"
            }
        }
        return formattedStartTime
    }
}