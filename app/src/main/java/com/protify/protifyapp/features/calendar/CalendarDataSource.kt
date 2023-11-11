package com.protify.protifyapp.features.calendar

import com.protify.protifyapp.FirestoreEvent
import com.protify.protifyapp.FirestoreHelper
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.stream.Collectors
import java.util.stream.Stream

class CalendarDataSource {

    val today: LocalDate
        get() {
            return LocalDate.now()
        }


    fun getData(startDate: LocalDate = today, lastSelectedDate: LocalDate): CalendarUiModel {
        val firstDayOfWeek = startDate.with(DayOfWeek.SUNDAY)
        val endDayOfWeek = firstDayOfWeek.plusDays(7)
        val visibleDates = getDatesBetween(firstDayOfWeek, endDayOfWeek)
        return toUiModel(visibleDates, lastSelectedDate)
    }

    private fun getDatesBetween(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        val numOfDays = ChronoUnit.DAYS.between(startDate, endDate)
        return Stream.iterate(startDate) { date ->
            date.plusDays(1)
        }
            .limit(numOfDays)
            .collect(Collectors.toList())
    }

    private fun toUiModel(
        dateList: List<LocalDate>,
        lastSelectedDate: LocalDate
    ): CalendarUiModel {
        return CalendarUiModel(
            selectedDate = toItemUiModel(lastSelectedDate, true),
            visibleDates = dateList.map {
                toItemUiModel(it, it.isEqual(lastSelectedDate))
            },
        )
    }
 fun getFirestoreEvents(uid: String, dateCreated: Long, month: String, day: String, year: String, callback: (List<Event>) -> Unit) {
        FirestoreHelper().userExists(uid,dateCreated) { userExists ->
            if (userExists) {
                FirestoreHelper().getEvents(uid, day, month, year) { events ->
                    if (events.isNotEmpty()) {
                        val convertedEvents = mutableListOf<Event>()
                        events.forEach {
                            convertedEvents.add(convertFirestoreEvent(it))
                        }
                        callback(convertedEvents)
                    }
                }
            }
        }
    }
    private fun convertFirestoreEvent (firestoreEvent: FirestoreEvent): Event {
        val convertedEvent = Event()
        convertedEvent.attendees = firestoreEvent.attendees!!
        convertedEvent.description = firestoreEvent.description!!
        convertedEvent.endTime = firestoreEvent.endTime.format(DateTimeFormatter.ofPattern("h:mm a"))
        convertedEvent.startTime = firestoreEvent.startTime.format(DateTimeFormatter.ofPattern("h:mm a"))
        convertedEvent.location = firestoreEvent.location!!
        convertedEvent.title = firestoreEvent.name!!
        return convertedEvent

        }

    private fun toItemUiModel(date: LocalDate, isSelectedDate: Boolean) = CalendarUiModel.Date(
        isSelected = isSelectedDate,
        isToday = date.isEqual(today),
        date = date,
        hasEvents = true,

    )


}