package com.protify.protifyapp.utils.OpenAIHelper

import com.protify.protifyapp.FirestoreEvent

class AIPrompts {

    fun comprehensivePromptWithOptimalEventOrder(hasRainingTimes: Boolean, nonRainingTimesString: String, optimalEventOrderString: String, dontRescheduleEvents: List<FirestoreEvent>): String {
        return (if (hasRainingTimes) "Prioritize scheduling events that are outdoors between ${nonRainingTimesString}. "  else "") +
                "Attempt to change the start times and end times of my events so they are in this order: $optimalEventOrderString " +
                "You can do this by changing the startTime and endTime of the events. " +
                //If there are events that aren't allowed to be rescheduled, list them here.
                if (dontRescheduleEvents.isNotEmpty()) { "You may not reschedule the following events: ${dontRescheduleEvents.joinToString(", ") { it.name }} " } else { "" } +
                "You will provide the optimized schedule in json format. One of the objects is to be named OptimizedEvents. " +
                "In OptimizedEvents, you will have a field called Name, StartTime, EndTime, and Location. " +
                "Another object should be called OldEvents, which will be identically formatted to Events, but will contain the original schedule. "
    }
    fun blockedEventsPrompt(allowedOptimalEventOrderString: String, dontRescheduleEvents: List<FirestoreEvent>, ): String {

        return "Attempt to change the start times and end times of my events so they are in this order: $allowedOptimalEventOrderString " +
                "You can do this by changing the startTime and endTime of the events. " +
                if (dontRescheduleEvents.isNotEmpty()) { "You may not change the start or end time of the following events: ${dontRescheduleEvents.joinToString(", ") { it.name }} " } else { "" } +
                "You will provide the optimized schedule in json format. One of the objects is to be named OptimizedEvents. " +
                "In OptimizedEvents, you will have a field called Name, StartTime, EndTime, and Location. " +
                "Another object should be called OldEvents, which will be identically formatted to Events, but will contain the original schedule. "

    }
    fun noEventOrderPrompt(hasRainingTimes: Boolean, nonRainingTimesString: String, dontRescheduleEvents: List<FirestoreEvent>): String {
        return (if (hasRainingTimes) "Prioritize scheduling events that are outdoors between ${nonRainingTimesString}. "  else "") +
                if (dontRescheduleEvents.isNotEmpty()) { "You may not change the start or end time of the following events: ${dontRescheduleEvents.joinToString(", ") { it.name }} " } else { "" } +
                "You will provide the optimized schedule in json format. One of the objects is to be named OptimizedEvents. " +
                "In OptimizedEvents, you will have a field called Name, StartTime, EndTime, and Location. " +
                "Another object should be called OldEvents, which will be identically formatted to Events, but will contain the original schedule. "
    }
    fun overlappingEventsPrompt(dontRescheduleEvents: List<FirestoreEvent>): String {
        return "Remove any events that have overlapping times." +
                "You can do this by changing the startTime and endTime of the events. " +
                if (dontRescheduleEvents.isNotEmpty()) { "You may not change the start or end time of the following events: ${dontRescheduleEvents.joinToString(", ") { it.name }} " } else { "" } +
                "You will provide the optimized schedule in json format. One of the objects is to be named OptimizedEvents. " +
                "In OptimizedEvents, you will have a field called Name, StartTime, EndTime, and Location. " +
                "Another object should be called OldEvents, which will be identically formatted to Events, but will contain the original schedule. "
    }
    fun prioritizeEventOrderPrompt(hasRainingTimes: Boolean, nonRainingTimesString: String, dontRescheduleEvents: List<FirestoreEvent>): String {
        return (if (hasRainingTimes) "For my events that are outdoors, try to move them between the following times. ${nonRainingTimesString}. "  else "") +
        "Attempt change the order of my events so I can minimize back and forth trips. " +
        if (dontRescheduleEvents.isNotEmpty()) { "You may not change the start or end time of the following events: ${dontRescheduleEvents.joinToString(", ") { it.name }} " } else { "" } + "You must return every event in the original schedule. " +
        "You will provide the optimized schedule in json format. One of the objects is to be named OptimizedEvents. " +
        "In OptimizedEvents, you will have a field called Name, StartTime, EndTime, and Location. " +
        "Another object should be called OldEvents, which will be identically formatted to Events, but will contain the original schedule. "
    }
    fun noLocationPrompt(dontRescheduleEvents: List<FirestoreEvent>): String {
                return "Please optimize my schedule for today. You must return every event that is given to you" +
                if (dontRescheduleEvents.isNotEmpty()) { "You may not change the start or end time of the following events: ${dontRescheduleEvents.joinToString(", ") { it.name }} " } else { "" } +
                "You will provide the optimized schedule in json format. One of the objects is to be named OptimizedEvents. " +
                "In OptimizedEvents, you will have a field called Name, StartTime, EndTime, and Location. " +
                "Another object should be called OldEvents, which will be identically formatted to Events, but will contain the original schedule. "
    }

}