package com.protify.protifyapp.features.GoogleMapsAPI

import com.protify.protifyapp.FirestoreEvent

class OptimizeRoute {
    // This function calculates the total driving time for a given sequence of events
    private fun calculateTotalDrivingTime(events: List<FirestoreEvent>, distanceMatrix: DrivingTimeMatrix): Int {
        var totalDrivingTime = 0
        // Loop through each pair of consecutive events
        for (i in 0 until events.size - 1) {
            // Get the origin and destination locations for the current pair of events
            val origin = events[i].location
            val destination = events[i + 1].location
            // Find the indices of the origin and destination locations in the distance matrix
            val originIndex = distanceMatrix.originAddresses.indexOf(origin)
            val destinationIndex = distanceMatrix.destinationAddresses.indexOf(destination)
            // If both locations are found in the distance matrix, add the driving time between them to the total driving time
            if (originIndex != -1 && destinationIndex != -1) {
                val drivingTime = distanceMatrix.rows[originIndex].elements[destinationIndex].duration.value
                totalDrivingTime += drivingTime
            }
        }
        // Return the total driving time
        return totalDrivingTime
    }

    // This function finds the sequence of events that results in the shortest total driving time
    fun findOptimalRoute(events: List<FirestoreEvent>, distanceMatrix: DrivingTimeMatrix): List<FirestoreEvent> {
        var optimalRoute = events
        var minDrivingTime = Int.MAX_VALUE

        // This function generates all permutations of the events
        fun permute(input: List<FirestoreEvent>, output: MutableList<FirestoreEvent>, visited: BooleanArray) {
            // If a permutation is complete
            if (output.size == input.size) {
                // Calculate the total driving time for the current permutation
                val totalDrivingTime = calculateTotalDrivingTime(output, distanceMatrix)
                // If the total driving time is less than the minimum found so far, update the minimum and the optimal route
                if (totalDrivingTime < minDrivingTime) {
                    minDrivingTime = totalDrivingTime
                    optimalRoute = output.toList()
                }
                return
            }

            // Generate the next event in the permutation
            for (i in input.indices) {
                // If the event has already been included in the permutation, skip it
                if (visited[i]) continue
                // Add the event to the permutation
                output.add(input[i])
                visited[i] = true
                // Recursively generate the rest of the permutation
                permute(input, output, visited)
                // Backtrack: remove the event from the permutation and mark it as not visited
                visited[i] = false
                output.removeAt(output.size - 1)
            }
        }

        // Start generating permutations
        permute(events, mutableListOf(), BooleanArray(events.size))
        // Return the optimal route
        return optimalRoute
    }
}