package com.protify.protifyapp.tests.features.LocationTesting

import com.protify.protifyapp.FirestoreEvent
import com.protify.protifyapp.features.GoogleMapsAPI.DrivingTimeMatrix
import com.protify.protifyapp.features.GoogleMapsAPI.OptimizeRoute
import com.protify.protifyapp.features.events.Attendee
import org.junit.Test
import java.time.LocalDateTime

class OptimizeScheduleTest {

    @Test
    fun testOptimizeSchedule() {
        val homeAddress = "762 Morning Dew Ln, Maineville, OH 45039, USA"

        //create an empty attendee
        val attendee = listOf(
            Attendee(
                name = "John",
                email = "",
                phoneNumber = ""
        )
        )
        //create a list of events
        val events = listOf(
            FirestoreEvent(
                name = "Gym",
                location = "1234 Main St, Cincinnati, OH 45202, USA",
                startTime = LocalDateTime.of(2024, 1, 1, 8, 0),
                endTime = LocalDateTime.of(2024, 1, 1, 9, 0),
                isOptimized = false,
                nameLower = "gym",
                isOutside = false,
                attendees = attendee,
                description = "",
                distance = 0,
                importance = 0,
                isRaining = false,
                mapsCheck = false,
                rainCheck = false,
                timeZone = "",
            ),
            FirestoreEvent(
                name = "Grocery Store",
                location = "5678 Elm St, Cincinnati, OH 45202, USA",
                startTime = LocalDateTime.of(2024, 1, 1, 9, 0),
                endTime = LocalDateTime.of(2024, 1, 1, 10, 0),
                isOptimized = false,
                nameLower = "grocery store",
                isOutside = false,
                attendees = attendee,
                description = "",
                distance = 0,
                importance = 0,
                isRaining = false,
                mapsCheck = false,
                rainCheck = false,
                timeZone = "",
            ),
            FirestoreEvent(
                name = "Work",
                location = "9101 Walnut St, Cincinnati, OH 45202, USA",
                startTime = LocalDateTime.of(2024, 1, 1, 10, 0),
                endTime = LocalDateTime.of(2024, 1, 1, 17, 0),
                isOptimized = false,
                nameLower = "work",
                isOutside = false,
                attendees = attendee,
                description = "",
                distance = 0,
                importance = 0,
                isRaining = false,
                mapsCheck = false,
                rainCheck = false,
                timeZone = "",


            )
        )

        //Get a list of locations based off of the events
        //map locations from the events
        val locations = mutableListOf<String?>()
        //If the location is empty, use home address
        events.mapTo(locations) {
            if (it.location == "") {
                homeAddress
            } else {
                it.location
            }
        }
        //If the homeAddress can't be found, add it
        if (!locations.contains(homeAddress)) {
            locations.add(homeAddress)
        }

        val rows = locations.map { origin ->
            val elements = locations.map { destination ->
                DrivingTimeMatrix.Row.Element(
                    duration = DrivingTimeMatrix.Row.Element.Duration(
                        text = "1 hour",
                        value = 3600,
                    ),
                    distance = DrivingTimeMatrix.Row.Element.Distance(
                        text = "2 hours",
                        value = 7200,
                    ), status = "OK"
                    )
            }
            DrivingTimeMatrix.Row(
                elements = elements
            )
        }
        // Create the driving time matrix
        val matrix = DrivingTimeMatrix(
            destinationAddresses = locations.filterNotNull(),
            originAddresses = locations.filterNotNull(),
            rows = rows,
            status = "OK"
        )


        //Get optimal order of events
        val optimalEventOrder = OptimizeRoute().findOptimalRoute(events, matrix)

        //Make sure the optimal order of events is not empty
        assert(!optimalEventOrder.isEmpty())

    }
}