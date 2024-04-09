import com.protify.protifyapp.features.events.Attendee

data class FirestoreEventString(
    val name: String,
    val nameLower: String,
    var startTime: String,
    var endTime: String,
    var location: String? = "",
    val description: String?,
    val timeZone: String? = "",
    val importance: Int? = 3,
    val attendees: List<Attendee>? = List(1) { Attendee("", "", "") },
    val rainCheck: Boolean = false,
    val isRaining: Boolean = false,
    val mapsCheck: Boolean = false,
    val distance: Int = 0,
    val isOutside: Boolean = false,
    val isOptimized: Boolean = false,
    val isAiSuggestion: Boolean = false,
    val isUserAccepted: Boolean = false,
)