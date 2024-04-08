package com.protify.protifyapp.features.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.protify.protifyapp.FirestoreEvent
import com.protify.protifyapp.FirestoreHelper
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import com.protify.protifyapp.utils.EventUtils
import com.protify.protifyapp.utils.MapsDurationUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class EventDetails {
    @Composable
    fun EventDetailsPage(eventId: String, date: String, navController: NavHostController) {
        var event by remember { mutableStateOf<FirestoreEvent?>(null) }
        val uid = FirebaseLoginHelper().getCurrentUser()!!.uid
        val parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val parsedDateTime = parsedDate.atStartOfDay()

        LaunchedEffect(eventId, date) {
            FirestoreHelper().getEventsAndIds(
                uid,
                parsedDateTime.dayOfMonth.toString(),
                parsedDateTime.month.toString(),
                parsedDateTime.year.toString()
            ) { fetchedEvents ->
                event = fetchedEvents.keys.find { it.id == eventId }
            }
        }

        Box(Modifier.fillMaxHeight()) {
            event?.let {
                EventItem(it, onBackClick = { navController.popBackStack() })
            } ?: run {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Loading event...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FloatingActionButton(
                    onClick = {
                        FirestoreHelper().deleteEventById(
                            uid,
                            parsedDateTime.month.toString(),
                            parsedDateTime.year.toString(),
                            eventId
                        ) {
                            if (it) {
                                navController.popBackStack()
                            } else {
                                // Handle error
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete event", tint = MaterialTheme.colorScheme.surface)
                }
                FloatingActionButton(
                    onClick = {
                        navController.navigate("editEvent/${date}/${eventId}")
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit event", tint = MaterialTheme.colorScheme.surface)
                }
            }
        }
    }

    @Composable
    fun EventItem(event: FirestoreEvent, onBackClick: () -> Unit = {}) {
        LazyColumn(content = {
            item {
                EventDetailsHeader(onBackClick = onBackClick)
            }
            item {
                EventDetailsBody(event)
            }
        })
    }

    @Composable
    private fun EventDetailsBody(event: FirestoreEvent) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            //OldEventDetailsBody(event = event)
            Text(
                text = event.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            EventTimeDetails(event = event)
            if (event.attendees != null && event.attendees!!.isNotEmpty()) {
                EventAttendees(attendees = event.attendees!!)
            }
            if (event.description != null && event.description!!.isNotBlank()) {
                EventDescDetails(desc = event.description!!)
            }
            if (event.location != null) {
                EventLocationDetails(event = event)
            }
        }
    }

    @Composable
    private fun EventTimeDetails(event: FirestoreEvent) {
        Row(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = "Event Time",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Column(
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Text(
                    text = event.startTime.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Text(
                    text = "${event.startTime.format(DateTimeFormatter.ofPattern("h:mm a"))} - ${event.endTime.format(DateTimeFormatter.ofPattern("h:mm a"))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }

    @Composable
    private fun EventAttendees(attendees: List<Attendee>) {
        // TODO: Make this more intricate
        Column(
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                text = "Attendees",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            attendees.forEach {
                Text(
                    text = it.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }

    @Composable
    private fun EventDescDetails(desc: String) {
        Column {
            Text(
                text = "About This Event",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ){
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineBreak = LineBreak.Paragraph.copy(
                            strategy = LineBreak.Strategy.Balanced,
                            strictness = LineBreak.Strictness.Strict
                        ),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    //maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                    fontStyle = MaterialTheme.typography.bodyMedium.fontStyle,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    private fun EventLocationDetails(event: FirestoreEvent) {
        val latLng = remember { mutableStateOf(LatLng(0.0, 0.0)) }
        LaunchedEffect(
            key1 = event.location
        ) {
            MapsDurationUtils(LocalDateTime.now()).geocode(event.location!!) { lat, lng ->
                latLng.value = LatLng(lat, lng)
            }
        }
        if (latLng.value.latitude == 0.0 && latLng.value.longitude == 0.0) {
            return
        }
        var isMapLoaded by remember { mutableStateOf(false) }
        val cameraPositionState = CameraPositionState(
            CameraPosition(latLng.value, 15f, 0f, 0f)
        )
        Column {
            Text(
                text = "Location",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            Box (
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ){
                GoogleMap(
                    modifier = Modifier.fillMaxWidth(),
                    contentDescription = "Event Location",
                    cameraPositionState = cameraPositionState,
                    onMapLoaded = {
                        isMapLoaded = true
                    },
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false
                    ),
                    properties = MapProperties(
                        mapType = MapType.NORMAL
                    )
                ) {
                    Marker(
                        state = MarkerState(position = latLng.value),
                        title = "Event Location",
                        snippet = event.location!!
                    )
                }
                if (!isMapLoaded) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    @Composable
    private fun OldEventDetailsBody(event: FirestoreEvent) {
        Column{
            event::class.java.declaredFields.forEach { field ->
                field.isAccessible = true
                val value = field.get(event)
                val readableValue = EventUtils().convertToReadableValue(field.name, value)
                if (readableValue.isNotBlank()) {
                    Card(
                        modifier = Modifier.padding(8.dp),
                        content = {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = field.name.uppercase(Locale.ROOT),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = readableValue,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun EventDetailsHeader(onBackClick: () -> Unit){
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onBackClick() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Event Details",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondary,
                )
                // This is a placeholder to push the title to the center
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
    @Preview
    @Composable
    fun EventDetailsPreview() {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
        ){
            EventItem(
                FirestoreEvent(
                    name = "Event Name",
                    nameLower = "event name",
                    startTime = LocalDate.now().atTime(10, 0),
                    endTime = LocalDate.now().atTime(12, 0),
                    description = "A fun event! ${LoremIpsum(25).values.joinToString()}",
                    timeZone = "Eastern Time",
                    importance = 1,
                    attendees = listOf(
                        Attendee("John Doe", "john@mail.com", "1234567890"),
                        Attendee("Jane Doe", "jane@mail.com", "0987654321")
                    ),
                    rainCheck = false,
                    isRaining = false,
                    mapsCheck = false,
                    distance = 0,
                    isOutside = false,
                    isOptimized = false,
                    isAiSuggestion = false,
                    isUserAccepted = false,
                    id = "id"
                )
            )
        }
    }
}