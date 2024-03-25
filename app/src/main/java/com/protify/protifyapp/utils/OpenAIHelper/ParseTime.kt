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
    fun parseAMPMTime(time: String, day: LocalDateTime): LocalDateTime {
        val timeSplit = time.split(":")
        val ampmSplit = timeSplit[1].split(" ")
        // Remove AM/PM
        val hour = timeSplit[0].toInt()
        val minute = ampmSplit[0].toInt()
        val ampm = ampmSplit[1]
        if (ampm == "PM") {
            // Handle 12 PM
            if (hour == 12) {
                return day.withHour(hour).withMinute(minute)
            }
            return day.withHour(hour + 12).withMinute(minute)
        }
        return day.withHour(hour).withMinute(minute)

    }

}