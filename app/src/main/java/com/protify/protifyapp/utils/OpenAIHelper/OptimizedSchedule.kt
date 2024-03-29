import com.google.gson.annotations.SerializedName

data class OptimizedSchedule(
    @SerializedName("OptimizedEvents")
    val events: List<Event>,
    @SerializedName("OldEvents")
    val oldEvents: List<Event>
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
        // If both lists are not empty, return true
        return (events.isNotEmpty() && oldEvents.isNotEmpty())
    }
}
