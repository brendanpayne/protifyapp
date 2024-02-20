package com.protify.protifyapp.features.GoogleMapsAPI

import com.protify.protifyapp.FirestoreEvent

class OptimizeRoute {
    private fun calculateTotalDrivingTime(events: List<FirestoreEvent>, distanceMatrix: DrivingTimeMatrix): Int {
        var totalDrivingTime = 0
        for (i in 0 until events.size - 1) {
            val origin = events[i].location
            val destination = events[i + 1].location
            val originIndex = distanceMatrix.originAddresses.indexOf(origin)
            val destinationIndex = distanceMatrix.destinationAddresses.indexOf(destination)
            if (originIndex != -1 && destinationIndex != -1) {
                val drivingTime = distanceMatrix.rows[originIndex].elements[destinationIndex].duration.value
                totalDrivingTime += drivingTime
            }
        }
        return totalDrivingTime
    }

    fun findOptimalRoute(events: List<FirestoreEvent>, distanceMatrix: DrivingTimeMatrix): List<FirestoreEvent> {
        var optimalRoute = events
        var minDrivingTime = Int.MAX_VALUE

        fun permute(input: List<FirestoreEvent>, output: MutableList<FirestoreEvent>, visited: BooleanArray) {
            if (output.size == input.size) {
                val totalDrivingTime = calculateTotalDrivingTime(output, distanceMatrix)
                if (totalDrivingTime < minDrivingTime) {
                    minDrivingTime = totalDrivingTime
                    optimalRoute = output.toList()
                }
                return
            }

            for (i in input.indices) {
                if (visited[i]) continue
                output.add(input[i])
                visited[i] = true
                permute(input, output, visited)
                visited[i] = false
                output.removeAt(output.size - 1)
            }
        }

        permute(events, mutableListOf(), BooleanArray(events.size))
        return optimalRoute
    }
}