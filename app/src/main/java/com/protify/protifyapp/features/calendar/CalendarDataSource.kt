package com.protify.protifyapp.features.calendar

import com.protify.protifyapp.FirestoreEvent
import com.protify.protifyapp.FirestoreHelper
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.stream.Collectors
import java.util.stream.Stream

class CalendarDataSource {

    val today: LocalDate
        get() {
            return LocalDate.now()
        }


        fun getData(startDate: LocalDate = today.plusWeeks(-1), lastSelectedDate: LocalDate, isMonthView: Boolean): CalendarUiModel {
            //Old Code May need to still use: fun getData(startDate: LocalDate = today, lastSelectedDate: LocalDate, isMonthView: Boolean): CalendarUiModel {
        val firstDay = if (isMonthView) {
            val firstDayOfMonth = startDate.withDayOfMonth(1)
            if (firstDayOfMonth.dayOfWeek == DayOfWeek.SUNDAY) {
                firstDayOfMonth
            } else {
                firstDayOfMonth.with(TemporalAdjusters.previous(DayOfWeek.SUNDAY))
            }
        } else {
            startDate.with(DayOfWeek.SUNDAY)
        }
        val lastDay = if (isMonthView) {
            val lastDayOfMonth = startDate.withDayOfMonth(startDate.lengthOfMonth())
            if (lastDayOfMonth.dayOfWeek != DayOfWeek.SATURDAY) {
                lastDayOfMonth.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
            } else {
                lastDayOfMonth.plusDays(1)
            }
        } else {
            firstDay.plusDays(7)
        }
        val visibleDates = getDatesBetween(firstDay, lastDay)
        return toUiModel(visibleDates, lastSelectedDate)
    }

    fun getDatesBetween(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
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
                    else {
                        callback(listOf())
                    }
                }
            }
            else {
                callback(listOf())
            }
        }
    }
    fun getFirestoreEventsAndIds(uid: String, dateCreated: Long, month: String, day: String, year: String, callback: (List<Event>) -> Unit) {
        FirestoreHelper().userExists(uid,dateCreated) { userExists ->
            if (userExists) {
                FirestoreHelper().getEventsAndIds(uid, day, month, year) { hashMap ->
                    if (hashMap.isNotEmpty()) {
                        val convertedEvents = mutableListOf<Event>()
                        hashMap.forEach {
                            convertedEvents.add(convertFirestoreEvent(it.key, it.value))
                        }
                        callback(convertedEvents)
                    }
                    else {
                        callback(listOf())
                    }

                }
            }
            else {
            }
        }
    }
    private fun convertFirestoreEvent (firestoreEvent: FirestoreEvent, id: String? = null): Event {
        val convertedEvent = Event()
        convertedEvent.id = firestoreEvent.id!!
        convertedEvent.attendees = firestoreEvent.attendees!!
        convertedEvent.description = firestoreEvent.description!!
        convertedEvent.endTime = firestoreEvent.endTime.format(DateTimeFormatter.ofPattern("h:mm a"))
        convertedEvent.startTime = firestoreEvent.startTime.format(DateTimeFormatter.ofPattern("h:mm a"))
        convertedEvent.location = firestoreEvent.location!!
        convertedEvent.title = firestoreEvent.name!!
        convertedEvent.isAiSuggestion = firestoreEvent.isAiSuggestion
        if (id != null && id != "") {
            convertedEvent.id = id
        }

        return convertedEvent

        }

    private fun toItemUiModel(date: LocalDate, isSelectedDate: Boolean) = CalendarUiModel.Date(
        isSelected = isSelectedDate,
        isToday = date.isEqual(today),
        date = date,
        hasEvents = true,
    )
}