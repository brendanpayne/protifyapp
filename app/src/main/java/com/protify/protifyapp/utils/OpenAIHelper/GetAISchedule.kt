package com.protify.protifyapp.utils.OpenAIHelper

import com.protify.protifyapp.FirestoreEvent
import com.protify.protifyapp.FirestoreHelper
import com.protify.protifyapp.features.GoogleMapsAPI.OptimizeRoute
import com.protify.protifyapp.utils.MapsDurationUtils
import com.protify.protifyapp.utils.WeatherUtils
import java.time.LocalDateTime

class GetAISchedule(uid: String, homeAddress: String) {

    // Get the current time
    val uid = uid
    val homeAddress = homeAddress

    /** Gets the schedule for a day
     * @param uid the user id
     * @return A list of firestore events for that day and a boolean representing if the AI should be run
     */
    fun getSchedule(today: LocalDateTime, callback: (List<FirestoreEvent>, Boolean) -> Unit) {
        FirestoreHelper().getEvents(
            uid = uid,
            day = today.dayOfMonth.toString(),
            month = today.month.toString(),
            year = today.year.toString()
        ) { events ->
            // Get the count of AI suggestions not user accepted and the count of non AI suggestions
            val aiSuggestions = events.filter { it.isAiSuggestion && !it.isUserAccepted } // AI suggestions that are not user accepted
            val nonAiSuggestions = events.filter { !it.isAiSuggestion } // Non AI suggestions
            // If there are less AI suggestions than non AI suggestions, then we should run the AI
           // Wait for at least 3 event or return an empty list
            if (events.size < 3) {
                callback(events, false)
            } else if (aiSuggestions.size < nonAiSuggestions.size && aiSuggestions.isNotEmpty()) { // If there are any ai suggestions and there are less ai suggestions than non ai suggestions
                // Delete the old AI suggestions
                FirestoreHelper().deleteAIGeneratedEvents(uid = uid, day = today.dayOfMonth.toString(), month = today.month.toString(), year = today.year.toString()) {
                    if (it) {
                        callback(nonAiSuggestions, true)
                    }
                }
            } else {

                // If there are no AI suggestions, run it back
                val shouldOptimize = events.none { it.isAiSuggestion }
                callback(events, shouldOptimize)
            }
        }
    }
    /** This function attempts to get all of the events for a given day, and if there needs to be an optimization, it
     * will get the optimized schedule and store it in the database
     * @param callback a callback function that returns a boolean representing if the optimization was successful
     */
    fun getOptimizedSchedule(use4: Boolean, today: LocalDateTime, callback: (Boolean) -> Unit) {
        getSchedule(today) { events, shouldOptimize ->
            if (shouldOptimize) {
                // Get the locations from each of the events
                val locations = events.map { it.location }.toMutableList()
                // If distinct locations is less than 2, then we need to run a different optimization
                if(locations.distinct().size < 2) {
                    // geocode the location
                    events[0].location?.let {
                        MapsDurationUtils(today).geocode(it) { lat, long ->
                            // Get the non raining times
                            WeatherUtils(lat, long).getNonRainingTimes(today) { nonRainingTimes ->
                                // Make the call
                                OptimizeSchedule(homeAddress = homeAddress,
                                    day = today.dayOfMonth.toString(),
                                    events = events,
                                    month = today.monthValue.toString(),
                                    optimalEventOrder = events,
                                    travelTime = mutableListOf(),
                                    year = today.year.toString()).makeCallNoLocation(use4 = use4, nonRainingTimes = nonRainingTimes) { optimizedEvents ->
                                    if (optimizedEvents.events.isNotEmpty() && optimizedEvents.oldEvents.isNotEmpty()) {
                                        // Store the events into the database
                                        FirestoreHelper().importAIGeneratedEvent(optimizedSchedule = optimizedEvents, today, uid) { didSucceed ->
                                            if (didSucceed) {
                                                callback(true)
                                                // If the events were not able to be stored, then the optimization failed
                                            } else {
                                                callback(false)
                                            }
                                        }
                                        // If an empty list was returned, then the optimization failed
                                    } else {
                                        callback(false)
                                    }

                                }
                            }

                        }
                    }
                }
                // Get Travel time
                MapsDurationUtils(today).getDrivingTimes(homeAddress = homeAddress, endLocations = locations) {drivingTimes ->
                    // Get the matrix
                    MapsDurationUtils(today).getMatrix(homeAddress = homeAddress, endLocations = locations) { matrix ->
                        if (matrix != null && drivingTimes.isNotEmpty())
                        {
                            // Find the optimal order
                            val optimalOrder = OptimizeRoute().findOptimalRoute(events, matrix)
                            // Geocode the first event
                            optimalOrder[0].location?.let {
                                MapsDurationUtils(today).geocode(it) { lat, long ->
                                    // Get the non raining times
                                    WeatherUtils(lat, long).getNonRainingTimes(today) { nonRainingTimes ->
                                        // Make the call
                                        OptimizeSchedule(homeAddress = homeAddress,
                                            day = today.dayOfMonth.toString(),
                                            events = events,
                                            month = today.monthValue.toString(),
                                            optimalEventOrder = optimalOrder,
                                            travelTime = drivingTimes.toMutableList(),
                                            year = today.year.toString()).makeCallParallel(use4 = use4, nonRainingTimes = nonRainingTimes) { optimizedEvents ->
                                                if (optimizedEvents.events.isNotEmpty() && optimizedEvents.oldEvents.isNotEmpty()) {
                                                    // Store the events into the database
                                                    FirestoreHelper().importAIGeneratedEvent(optimizedSchedule = optimizedEvents, today, uid) { didSucceed ->
                                                        if (didSucceed) {
                                                            callback(true)
                                                            // If the events were not able to be stored, then the optimization failed
                                                        } else {
                                                            callback(false)
                                                        }
                                                    }
                                                        // If an empty list was returned, then the optimization failed
                                                } else {
                                                    callback(false)
                                                }

                                        }
                                    }

                                }
                            }
                            // If the matrix and the driving times were not able to be populated, then the optimization failed
                        } else {
                            callback(false)
                        }
                    }

                }
                // If there are no events to optimize, don't run the optimization
            } else {
                callback(false)
            }
        }
    }
    /** This function runs the optimization check for every day of the week (which is what weather data is available for)
     */
    fun checkWeekScheduleForOptimization(use4: Boolean) {
        for (i in 0..6) {
            val today = LocalDateTime.now().plusDays(i.toLong())
            // I've got basic error checking to see if it failed, but I don't see a reason to do anything with the error
            getOptimizedSchedule(use4, today) {}
        }
    }
}