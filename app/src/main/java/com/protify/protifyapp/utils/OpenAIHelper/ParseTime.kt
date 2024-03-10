package com.protify.protifyapp.utils.OpenAIHelper

import java.time.LocalDateTime

class ParseTime {

    /**
     * parseTime will parse the time string from the AI output and convert it to a LocalDateTime object
     * @param time The time string to parse
     * @param day The day of the event
     * @return The LocalDateTime object
     */
    fun parseTime(time: String, day: LocalDateTime): LocalDateTime {
        val timeSplit = time.split(":")
        val hour = timeSplit[0].toInt()
        val minute = timeSplit[1].toInt()
        return day.withHour(hour).withMinute(minute)
    }

}