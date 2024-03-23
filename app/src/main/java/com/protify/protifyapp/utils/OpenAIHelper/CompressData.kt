package com.protify.protifyapp.utils.OpenAIHelper

import com.protify.protifyapp.FirestoreEvent

class CompressData() {

    //Map to store the event and its number
    val eventToNumberMap = mutableMapOf<FirestoreEvent, Int>()
    //Iterative event number
    private var nextNumberEvent = 0

    //Map to store location and its number
    val locationToNumberMap = mutableMapOf<String, Int>()
    //Iterative location number
    private var nextNumberLocation = 0

    //Turn an event name into a number
    fun eventToNumber(event: FirestoreEvent): Int {
        //If the event is in not in the map, add it
        if (!eventToNumberMap.containsKey(event)) {
            eventToNumberMap[event] = nextNumberEvent
            nextNumberEvent++
        }
            return eventToNumberMap[event]!!
    }
    //Turn a number into an event name
    fun numberToEvent(number: Int): FirestoreEvent? {
    //If the number is in the map, return the event
    return eventToNumberMap.entries.find { it.value == number }?.key
    }

    //Turn a location into a number
    fun locationToNumber(location: String): Int {
        //If the location is in not in the map, add it
        if (!locationToNumberMap.containsKey(location)) {
            locationToNumberMap[location] = nextNumberLocation
            nextNumberLocation++
        }
        return locationToNumberMap[location]!!
    }
    //Turn a number into a location
    fun numberToLocation(number: Int): String? {
        //If the number is in the map, return the location
        return locationToNumberMap.entries.find { it.value == number }?.key
    }


    //Turn a list of events into a map of events and their numbers
    fun listEventsToNumbers(events: List<FirestoreEvent>): MutableMap<FirestoreEvent, Int> {
        val eventList = mutableMapOf<FirestoreEvent, Int>()
        for (event in events) {
            eventList[event] = eventToNumber(event)
        }
        return eventList
    }
    //Turn a list of locations into a map of locations and their numbers
    fun listLocationsToNumbers(locations: List<String>): MutableMap<String, Int> {
        val locationList = mutableMapOf<String, Int>()
        for (location in locations) {
            locationList[location] = locationToNumber(location)
        }
        return locationList
    }


}