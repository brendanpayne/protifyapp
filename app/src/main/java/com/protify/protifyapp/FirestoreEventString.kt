import com.protify.protifyapp.features.events.Attendee

data class FirestoreEventString(
    val name: String,
    val nameLower: String,
    var startTime: String,
    var endTime: String,
    var location: String?,
    val description: String?,
    val timeZone: String?,
    val importance: Int?,
    val attendees: List<Attendee>?,
    val rainCheck: Boolean,
    val isRaining: Boolean,
    val mapsCheck: Boolean,
    val distance: Int,
    val isOutside: Boolean,
    val isOptimized: Boolean,
    val isAiSuggestion: Boolean,
    val isUserAccepted: Boolean,
)