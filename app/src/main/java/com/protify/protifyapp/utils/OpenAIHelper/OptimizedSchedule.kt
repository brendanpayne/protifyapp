import com.google.gson.annotations.SerializedName

data class OptimizedSchedule(
    @SerializedName("Events")
    val events: List<Event>,
    @SerializedName("TimeSaved")
    val timeSaved: Int
) {

    data class Event(
        @SerializedName("Name")
        val name: String,
        @SerializedName("StartTime")
        val startTime: String, // Consider changing to a time format if needed
        @SerializedName("EndTime")
        val endTime: String, // Consider changing to a time format if needed
        @SerializedName("Location")
        val location: String
    ) {
        // Add additional methods or properties specific to events if needed
    }

    // Add additional methods or properties specific to the optimized schedule if needed

    fun nullCheck(): Boolean {
        if (events == null || timeSaved == null) {
            return false
        }
        return events.isNotEmpty() && events.all { event ->
            event.name.isNotBlank() &&
                    event.startTime.isNotBlank() &&
                    event.endTime.isNotBlank() &&
                    event.location.isNotBlank()
        } && timeSaved >= 0
    }
}
