package com.protify.protifyapp.features.events

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.icu.util.TimeZone
import android.net.Uri
import android.provider.ContactsContract
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.auth.User
import com.protify.protifyapp.APIKeys
import com.protify.protifyapp.FirestoreEvent
import com.protify.protifyapp.FirestoreHelper
import com.protify.protifyapp.NetworkManager
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import com.protify.protifyapp.utils.MapsDurationUtils
import com.protify.protifyapp.utils.WeatherUtils
import com.protify.protifyapp.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

open class AddEvent {

    var name: String by mutableStateOf("")
    var startTime: LocalDateTime by mutableStateOf(LocalDateTime.now())
    var endTime: LocalDateTime by mutableStateOf(LocalDateTime.MAX)
    var location: String? by mutableStateOf("")
    var description: String? by mutableStateOf("")
    var timeZone: String? by mutableStateOf(TimeZone.getDefault().displayName)
    var importance: Int by mutableIntStateOf(3)
    var attendees: List<Attendee> by mutableStateOf(listOf())
    var formattedStartTime: String by mutableStateOf("")
    var formattedEndTime: String by mutableStateOf("")
    var dateError: Boolean by mutableStateOf(false)
    var timeError: Boolean by mutableStateOf(false)
    var contactList: List<Attendee> by mutableStateOf(listOf())
    var contactNames: List<String> by mutableStateOf(listOf())
    var rainingTimesMessage: String by mutableStateOf("")
    var isRainingTimeConfirmed: Boolean by mutableStateOf(true) // True by default... innocent until guilty
    var showRainingTimesDialog: Boolean by mutableStateOf(false)
    var rainCheck: Boolean = false
    var isOptimized: Boolean by mutableStateOf(true)
    var isOutside: Boolean by mutableStateOf(false)

    private fun updateName(newName: String) {
        name = newName
    }

    private fun updateStartTime(hour: Int, minute: Int) {
        val currentDateTime = LocalDateTime.now() // Get the current date and time, just a placeholder
        val newStartTime = LocalDateTime.of(currentDateTime.year, currentDateTime.month, currentDateTime.dayOfMonth, hour, minute)
        startTime = newStartTime
        formattedStartTime(newStartTime)
    }
    private fun updateEndTime(hour: Int, minute: Int) {
        val currentDateTime = LocalDateTime.now() // Get the current date and time, just a placeholder
        val newStartTime = LocalDateTime.of(currentDateTime.year, currentDateTime.month, currentDateTime.dayOfMonth, hour, minute)
        startTime = newStartTime
        formattedStartTime(newStartTime)
    }

    private fun formattedStartTime(startTime: LocalDateTime) {
        val year = startTime.year
        val month = startTime.monthValue
        val dayOfMonth = startTime.dayOfMonth
        var hour = startTime.hour
        var minute = startTime.minute

        if (formattedEndTime != "" || formattedEndTime != null) {
            if (startTime.isAfter(endTime) && formattedEndTime != "Start time cannot be after end time") {
                dateError = true
                //formattedStartTime = "Start time cannot be after end time"
                return
            }
        }
        dateError = false
        if (hour >= 12) {
            if (hour > 12) {
                hour -= 12
            }
            formattedStartTime = if (minute < 10) {
                "$month/$dayOfMonth/$year $hour:0$minute PM"
            } else {
                "$month/$dayOfMonth/$year $hour:$minute PM"
            }
        } else {
            formattedStartTime = if (minute < 10) {
                "$month/$dayOfMonth/$year $hour:0$minute AM"
            } else {
                "$month/$dayOfMonth/$year $hour:$minute AM"
            }
        }
    }
    private fun formattedEndTime(endTime: LocalDateTime) {
        val year = endTime.year
        val month = endTime.monthValue
        val dayOfMonth = endTime.dayOfMonth
        var hour = endTime.hour
        val minute = endTime.minute

        if (formattedStartTime != "" || formattedStartTime != null) {
            if (startTime.isAfter(endTime) && formattedStartTime != "Start time cannot be after end time") {
                dateError = true
                //formattedEndTime = "Start time cannot be after end time"
                return
            }
        }
        dateError = false
        if (hour >= 12) {
            if (hour > 12) {
                hour -= 12
            }
            formattedEndTime = if (minute < 10) {
                "$month/$dayOfMonth/$year $hour:0$minute PM"
            } else {
                "$month/$dayOfMonth/$year $hour:$minute PM"
            }
        } else {
            formattedEndTime = if (minute < 10) {
                "$month/$dayOfMonth/$year $hour:0$minute AM"
            } else {
                "$month/$dayOfMonth/$year $hour:$minute AM"
            }
        }

    }
    fun updateLocation(newLocation: String) {
        location = newLocation
    }
    fun updateDescription(newDescription: String) {
        description = newDescription
    }
    fun updateTimeZone(newTimeZone: String) {
        timeZone = newTimeZone
    }
    fun updateImportance(newImportance: Int) {
        importance = newImportance
    }
    fun updateAttendees(newAttendees: List<Attendee>) {
        attendees = newAttendees
    }
    fun isTimeSelected(): Boolean {
        return formattedStartTime != "" && formattedEndTime != ""
    }
    fun updateAttendeeList(name: String, number: String, email: String) {
        Attendee(name, email, number).let { attendee ->
            contactList = contactList + attendee
        }
    }
    fun updateContactNames(name: String) {
        contactNames = contactNames + name
    }

    fun updateIsOutside(isOutside: Boolean) {
        this.isOutside = isOutside
    }

    fun updateIsOptimized(isOptimized: Boolean) {
        this.isOptimized = isOptimized
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AddEventUI(navigateBack: () -> Unit) {
        val context = LocalContext.current
        val user = FirebaseLoginHelper().getCurrentUser()?.uid
        var requiredEmpty by remember { mutableStateOf(false) }
        val networkManager = NetworkManager(context)
        //Date Picker Dialog
        val dayOfMonth = LocalDateTime.now().dayOfMonth
        val month = LocalDateTime.now().monthValue - 1 //Subtract 1 to select the right day in the DatePickerDialog
        val year = LocalDateTime.now().year
        val selectedDate = remember { mutableStateOf("")}
        var selectedMonth = 0
        var selectedDayOfMonth = 0
        var selectedYear = 0
        val datePickerDialog = android.app.DatePickerDialog(
            context, {_, year, month, dayOfMonth ->
            //selectedDate.value = "$month/$dayOfMonth/$year"
            }, year, month, dayOfMonth
        )
        //Time Picker Dialog
        val hour = LocalDateTime.now().hour
        val minute = LocalDateTime.now().minute
        val selectedTime = remember { mutableStateOf("")}
        var selectedHour = 0
        var selectedMinute = 0
        val timePickerDialog = TimePickerDialog(
            context, {_, hour, minute ->
            selectedTime.value = "$hour:$minute"
                selectedHour = hour
                selectedMinute = minute
            }, hour, minute, false
        )
        //Time Zone
        val timeZoneNames = TimeZone.getAvailableIDs()
        //Importance
        var expandedImportance by remember { mutableStateOf(false) }
        //Contacts Launcher
        var contactName by remember {
            mutableStateOf("")
        }
        var contactEmail by remember {
            mutableStateOf("")
        }
        var contactNumber by remember {
            mutableStateOf("")
        }
        var expandedContact by remember { mutableStateOf("")}
        val permission = android.Manifest.permission.READ_CONTACTS
        var contactsGranted by remember { mutableStateOf(false) }
        val requestPermissionsLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (isGranted) {
                    contactsGranted = true
                }
            }
        )
        val contactsLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickContact(),
            onResult = {
                if (it != null) {
                    val contactId = ContentUris.parseId(it)
                    val cursor = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                        arrayOf(contactId.toString()),
                        null,
                    )
                    if (cursor != null && cursor.moveToFirst()) {
                        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val emailIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                        contactNumber = cursor.getString(numberIndex) ?: ""
                        contactName = cursor.getString(nameIndex) ?: ""
                        contactEmail = cursor.getString(emailIndex) ?: ""
                        if (!contactEmail.contains("@")) {
                            contactEmail = ""
                        }
                        if (contactNumber.contains("@")) {
                            contactNumber = ""
                        }
                        if (contactName == "") {
                            Toast.makeText(context, "Contact must have a name", Toast.LENGTH_LONG).show()
                        }
                        else {
                            updateAttendeeList(contactName, contactNumber, contactEmail)
                            updateContactNames(contactName)
                        }
                        cursor.close()
                    }
                }
            }
        )
        //Auto Complete
        val mapsAPI = APIKeys().getMapsKey()
        Places.initialize(context, mapsAPI)
        val placesClient = remember { Places.createClient(context) }
        val autocompleteLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = {
                if (it.resultCode == android.app.Activity.RESULT_OK) {
                    val place = Autocomplete.getPlaceFromIntent(it.data!!)
                    updateLocation(place.address ?: "")
                }
            }
        )
        //isOutside boolean
        //var isOutside by remember { mutableStateOf(false) }
        //isOptimized boolean
        //var isOptimized by remember { mutableStateOf(true)}
        //Get network state so we can toggle Firestore offline/online
        val isConnected by remember { mutableStateOf(false) }
        LaunchedEffect(networkManager) {
            networkManager.startListening()
        }
        LaunchedEffect(isConnected) {
            networkManager.setNetworkChangeListener {
                if(it) {
                    FirestoreHelper().toggleOfflineOnline(true)
                } else {
                    FirestoreHelper().toggleOfflineOnline(false)
                }
            }
        }
        // Get the non-raining times if there are values for start time and end time and update when any of those change
        LaunchedEffect(startTime, endTime, location, isOutside) {
            rainCheck = false
            if (startTime != LocalDateTime.now() && endTime != LocalDateTime.MAX && location != "" && isOutside) { // Make sure the user has already selected a start time, end time, isOutside is true, and location is not empty
                if (startTime.isAfter(LocalDateTime.now()) && endTime.isBefore(LocalDateTime.now().plusDays(7))) // Make sure the start time is in the future and the end time is within a week
                    MapsDurationUtils(startTime).geocode(location!!) { lat, long -> // Geocode the location to get the latitude and longitude
                        if (lat != 0.0 && long != 0.0) { // If response is not bad, then proceed
                            WeatherUtils(long, lat).getNonRainingTimes(startTime) { nonRainingTimes -> // Get non raining times for the day of the event
                                getNonRainingTimes(nonRainingTimes, startTime, long, lat, startTime, endTime) { isRaining, message -> // Run function to check if the event is within the non-raining times
                                    if (isRaining) {
                                        rainingTimesMessage = message // If it's raining, then set the message to the message returned from the function
                                        isRainingTimeConfirmed = false // Set the boolean to false if the event is during the raining time
                                    } else {
                                        rainingTimesMessage = "" // If it's not raining, then set the message to an empty string
                                        isRainingTimeConfirmed = true // Set back to true if the user changed the event to a time that is not raining
                                    }
                                    rainCheck = true
                                }
                            }
                        }
                    }

            } else { // If the user has not selected a start time, end time, location, or isOutside is false, then set the message to an empty string and the boolean to true
                showRainingTimesDialog = false
                isRainingTimeConfirmed = true
            }
        }


        val listState = rememberLazyListState()
        Surface(
            modifier = Modifier.fillMaxWidth(1f).fillMaxHeight(0.9f),
            color = MaterialTheme.colorScheme.background
        ) {
            ScrollView(LocalContext.current)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Text(
                        text = "Event Information",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
                item {
                    EventNameItem(
                        name = name,
                        updateName = { updateName(it) },
                        requiredEmpty = requiredEmpty
                    )
                }
                item {
                    EventDateItem(
                        datePickerDialog = datePickerDialog,
                        dateError = dateError,
                    )
                }
                item {
                    EventTimeItem(
                        formattedStartTime = formattedStartTime,
                        formattedEndTime = formattedEndTime,
                        timePickerDialog = timePickerDialog,
                        timeError = timeError,
                        requiredEmpty = requiredEmpty,
                        selectedTime = selectedTime,
                    )
                }
                item {
                    EventDropdownsItem()
                }
                item {
                    EventLocationItem(
                        autocompleteLauncher = autocompleteLauncher,
                        context = context,
                    )
                }
                item {
                    EventDescriptionItem()
                }
                item {
                    EventOutdoorsItem(
                        isOutside = isOutside,
                        onCheckedChange = { isChecked -> updateIsOutside(isChecked) }
                    )
                }
                item {
                    EventAIItem(
                        isOptimized = isOptimized,
                        onCheckedChange = { isChecked -> updateIsOptimized(isChecked) }
                    )
                }
                item{
                    Text(
                        text = "Attendees",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
                item {
                    EventAttendeesItem(
                        initialContactList = contactList,
                        initialExpandedContact = expandedContact,
                        contactsGranted = contactsGranted,
                        requestPermissionsLauncher = requestPermissionsLauncher,
                        contactsLauncher = contactsLauncher
                    )
                }
            }
        }
    }

    @Composable
    fun EventNameItem(name: String, updateName: (String) -> Unit, requiredEmpty: Boolean) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth(1f)
                .padding(horizontal = 8.dp),
            value = name,
            placeholder = { Text("Event Name") },
            onValueChange = { name -> updateName(name) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (name.length > 50) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                unfocusedBorderColor = if (name.length > 50 || (name.isEmpty() && requiredEmpty)) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ),
            supportingText = {
                when {
                    name.length > 50 -> Text("${name.length}/50", color = MaterialTheme.colorScheme.error)
                    name.isEmpty() && requiredEmpty -> Text("Required", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    @Composable
    fun EventDateItem(datePickerDialog: DatePickerDialog, dateError: Boolean) {
        var selectedDate by remember { mutableStateOf("") }
        var selectedMonth by remember { mutableIntStateOf(0) }
        var selectedDayOfMonth by remember { mutableIntStateOf(0) }
        var selectedYear by remember { mutableIntStateOf(0) }
        OutlinedTextField(
            value = selectedDate,
            onValueChange = { selectedDate = it },
            placeholder = { Text("Date") },
            modifier = Modifier
                .fillMaxWidth(1f)
                .padding(horizontal = 8.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        datePickerDialog.show()
                        datePickerDialog.setOnDateSetListener { _, year, month, dayOfMonth ->
                            selectedDate = "$month/$dayOfMonth/$year"
                            selectedMonth = month + 1
                            selectedDayOfMonth = dayOfMonth
                            selectedYear = year
                        }
                    }
                ),
            enabled = false,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Date Picker",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = if (selectedDate == "" && dateError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
            ),
            supportingText = {
                if (selectedDate == "" && !dateError) {
                    Text(
                        text = "Required",
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        )
    }

    @Composable
    fun EventTimeItem(formattedStartTime: String, formattedEndTime: String, timePickerDialog: TimePickerDialog, timeError: Boolean, requiredEmpty: Boolean, selectedTime: MutableState<String>) {
        var selectedHour by remember { mutableIntStateOf(0) }
        var selectedMinute by remember { mutableIntStateOf(0) }

        Row(
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth(1f)
        ) {
            OutlinedTextField(
                value = formattedStartTime,
                onValueChange = { this@AddEvent.formattedStartTime = it },
                placeholder = { Text("Start Time") },
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(horizontal = 8.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {
                            timePickerDialog.show()
                            timePickerDialog.setOnDismissListener {
                                if (selectedTime.value != "") {
                                    updateStartTime(selectedHour, selectedMinute) // TODO fix this
                                }
                            }
                        }
                    ),
                enabled = false,
                trailingIcon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.schedule),
                        contentDescription = "Time Picker",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = if ((formattedStartTime == "" && requiredEmpty) || timeError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                ),
                supportingText = {
                    if (timeError) {
                        Text(
                            modifier = Modifier.fillMaxSize(),
                            text = "Start time cannot be after end time",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (formattedStartTime == "" && !timeError) {
                        Text(
                            text = "Required",
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(
                value = formattedEndTime,
                onValueChange = { this@AddEvent.formattedEndTime = it },
                placeholder = { Text("End Time") },
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(horizontal = 8.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {
                            timePickerDialog.show()
                            timePickerDialog.setOnDismissListener {
                                if (selectedTime.value != "") {
                                    updateEndTime(selectedHour, selectedMinute)
                                }
                            }
                        }
                    ),
                enabled = false,
                trailingIcon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.schedule),
                        contentDescription = "Time Picker",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = if ((formattedEndTime == "" && requiredEmpty) || timeError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                ),
                supportingText = {
                    if (timeError) {
                        Text(
                            modifier = Modifier.fillMaxSize(),
                            text = "End time cannot be before start time",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (formattedEndTime == "" && !timeError) {
                        Text(
                            text = "Required",
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun EventDropdownsItem() {
        var expandedTimeZone by remember { mutableStateOf(false) }
        var expandedImportance by remember { mutableStateOf(false) }
        val americanTimeZoneNames = listOf(
            "America/New_York",
            "America/Chicago",
            "America/Denver",
            "America/Los_Angeles",
            "America/Anchorage",
            "America/Adak",
            "Pacific/Honolulu"
        ).sorted()
        Row(
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth(1f)
        ) {
            ExposedDropdownMenuBox(
                expanded = expandedTimeZone,
                onExpandedChange = { expandedTimeZone = !expandedTimeZone},
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(horizontal = 8.dp),
                content = {
                    OutlinedTextField(
                        value = timeZone?: "",
                        onValueChange = {},
                        placeholder = { Text("Time Zone") },
                        readOnly = true,
                        modifier = Modifier.menuAnchor(),
                        trailingIcon =  {ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTimeZone)},
                        supportingText = {
                            Text(
                                text = "Optional",
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTimeZone,
                        onDismissRequest = { expandedTimeZone = false}
                    ) {
                        americanTimeZoneNames.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(TimeZone.getTimeZone(item).displayName) },
                                onClick = {
                                    updateTimeZone(TimeZone.getTimeZone(item).displayName)
                                    expandedTimeZone = false
                                }
                            )
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.width(4.dp))
            val importanceNames = listOf(
                "1",
                "2",
                "3",
                "4",
                "5"
            )
            ExposedDropdownMenuBox(
                expanded = expandedImportance,
                onExpandedChange = { expandedImportance = !expandedImportance },
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(horizontal = 8.dp),
                content = {
                    OutlinedTextField(
                        value = importance.toString(),
                        onValueChange = {},
                        placeholder = { Text("Importance") },
                        readOnly = true,
                        modifier = Modifier.menuAnchor(),
                        trailingIcon =  {ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedImportance)},
                        supportingText = {
                            Text(
                                text = "Optional",
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedImportance,
                        onDismissRequest = { expandedImportance = false }
                    ) {
                        importanceNames.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    updateImportance(item.toInt())
                                    expandedImportance = false
                                }
                            )
                        }
                    }
                }
            )
        }
    }

    @Composable
    fun EventLocationItem(autocompleteLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>, context: Context) {
        var isFocused by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = location ?: "",
            onValueChange = { location = it },
            placeholder = { Text("Location") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location Picker",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            },
            modifier = Modifier
                .fillMaxSize(1f)
                .padding(horizontal = 8.dp)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused && !isFocused) {
                        isFocused = true
                        autocompleteLauncher.launch(
                            Autocomplete
                                .IntentBuilder(
                                    AutocompleteActivityMode.OVERLAY,
                                    listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS)
                                )
                                .build(context)
                        )
                    } else if (!focusState.isFocused) {
                        isFocused = false
                    }
                },
            supportingText = {
                Text(
                    text = "Optional",
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        )
    }

    @Composable
    fun EventDescriptionItem() {
        OutlinedTextField(
            value = description ?: "",
            onValueChange = { description -> updateDescription(description) },
            placeholder = { Text("Type event description here...") },
            modifier = Modifier
                .fillMaxWidth(1f)
                .padding(horizontal = 8.dp)
                .height(150.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (description?.length!! > 250) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = if (description?.length!! > 250) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
            ),
            supportingText = {
                if (description?.length!! > 250) {
                    Text(
                        text = "${description!!.length}/250",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(
                        text = "Optional",
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        )
    }

    @Composable
    fun EventOutdoorsItem(isOutside: Boolean, onCheckedChange: (Boolean) -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth(1f)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Is this event outdoors?",
                style = if (!isOutside) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)

            )
            Switch(
                checked = isOutside,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.primary,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    }

    @Composable
    fun EventAIItem(isOptimized: Boolean, onCheckedChange: (Boolean) -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth(1f)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Allow AI to optimize this event?",
                style = if (!isOptimized) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)

            )
            Switch(
                checked = isOptimized,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.primary,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    }

    @Composable
    fun EventAttendeesItem(
        initialContactList: List<Attendee>,
        initialExpandedContact: String,
        contactsGranted: Boolean,
        requestPermissionsLauncher: ActivityResultLauncher<String>,
        contactsLauncher: ManagedActivityResultLauncher<Void?, Uri?>
    ) {
        var contactList by remember { mutableStateOf(initialContactList) }
        var expandedContact by remember { mutableStateOf(initialExpandedContact) }
        val permission = android.Manifest.permission.READ_CONTACTS
        ElevatedCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            )
        ){
            Box(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        content = {
                            Text(
                                text = if (contactList.isEmpty()) "No Attendees" else "Attendees",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                            IconButton(
                                onClick = {
                                    if (contactsGranted) {
                                        contactsLauncher.launch(null)
                                    } else {
                                        requestPermissionsLauncher.launch(permission)
                                        if (contactsGranted) {
                                            contactsLauncher.launch(null)
                                        }
                                    }
                                },
                                modifier = Modifier.padding(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Attendee",
                                        tint = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        contactList.forEach() { attendee ->
                            Button(
                                onClick = { expandedContact = attendee.name }
                            ) {
                                Column {
                                    Row {
                                        Text(attendee.name)
                                        if (expandedContact != attendee.name) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "More Info",
                                                tint = MaterialTheme.colorScheme.surface,
                                            )
                                        }
                                    }
                                    if (expandedContact == attendee.name) {
                                        if (attendee.phoneNumber != "") {
                                            Text(text = "Phone Number " + attendee.phoneNumber)
                                        }
                                        if (attendee.email != "") {
                                            Text(text = "Email " + attendee.email)
                                        }
                                    }
                                }
                            }
                            Button(
                                onClick = { contactList = contactList - attendee },
                            ) {
                                Icon (
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.surface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    open fun EventCreateItem(
        buttonText: String = "Create Event",
        isOutside: Boolean,
        isOptimized: Boolean,
        user: String?,
        navigateBack: () -> Unit,
        context: Context
    ) {
        Button(
            modifier = Modifier
                .fillMaxSize()
                .padding(start= 16.dp, end = 16.dp, bottom = 16.dp),
            onClick = {
                saveEvent(isOutside, isOptimized, user, navigateBack, context)
            },
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(8.dp)
            ) {
            Text(buttonText)
        }
        if (showRainingTimesDialog) {
            eventDuringRainingTimesConfirmDialog(isRainingTimeConfirmed, rainingTimesMessage)
        }
    }

    open fun saveEvent(
        isOutside: Boolean,
        isOptimized: Boolean,
        user: String?,
        navigateBack: () -> Unit,
        context: Context
    ) {
        if (isRainingTimeConfirmed) {
            val firestoreEvent = FirestoreEvent(
                attendees = contactList,
                description = description,
                endTime = endTime,
                startTime = startTime,
                timeZone = timeZone,
                name = name,
                nameLower = name.trim().lowercase(),
                importance = importance,
                location = location,
                rainCheck = rainCheck,
                isRaining = (rainingTimesMessage != ""),
                mapsCheck = false,
                distance = 0,
                isOutside = isOutside,
                isOptimized = !isOptimized,
                isAiSuggestion = false,
                isUserAccepted = false
            )
            val errors = firestoreEvent.validateEvent(firestoreEvent)
            if (errors.isEmpty() && user != null && !dateError && isTimeSelected())  {
                FirestoreHelper().createEvent(user, firestoreEvent) {
                    if (it) {
                        Toast.makeText(context, "Event added successfully", Toast.LENGTH_LONG).show()
                        navigateBack()
                    } else {
                        Toast.makeText(context, "Event could not be added. Please check your network connection", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                handleErrors(errors, user, dateError, isTimeSelected(), context)
            }
        } else {
            showRainingTimesDialog = true
            Toast.makeText(context, "You need to confirm your event time", Toast.LENGTH_LONG).show()
        }
    }

    /** Function getNonRainingTimes checks if an event is within the non-raining times
     * @param nonRainingTimes List of non-raining times
     * @param today Current date
     * @param long Longitude of the event
     * @param lat Latitude of the event
     * @param startTime Start time of the event
     * @param endTime End time of the event
     * @param onComplete Callback function that returns false and an empty string if it's not raining during the event and true and a message if it is
     */
    fun getNonRainingTimes(nonRainingTimes: List<Pair<LocalDateTime, LocalDateTime>>, today: LocalDateTime, long: Double, lat: Double, startTime: LocalDateTime, endTime: LocalDateTime, onComplete: (Boolean, String) -> Unit) {
        // Get the times that it will rain for the given day
            if (nonRainingTimes.isEmpty()) {
                onComplete(false, "")
            } else {
                // Check if the event is within the non-raining times
                val isRaining = nonRainingTimes.any { nonRaining ->
                    // Check if the time between the start time and the end time fall between the non-raining times
                    !(startTime.isAfter(nonRaining.first) && startTime.isBefore(nonRaining.second)) || !(endTime.isAfter(nonRaining.first) && endTime.isBefore(nonRaining.second))
                }
                val nonRainingTimesString = nonRainingTimes.joinToString(", ") { "${it.first.format(
                    DateTimeFormatter.ofPattern("hh:mm a"))} - ${it.second.format(
                    DateTimeFormatter.ofPattern("hh:mm a"))}" }
                if (isRaining) {
                    onComplete(true, "It will be raining during your event, you should move it within ${if (nonRainingTimes.size > 1) "these times: $nonRainingTimesString" else "this time: $nonRainingTimesString"}")
                } else {
                    onComplete(false, "")
                }
            }
    }
    /** Function eventDuringRainingTimesConfirmDialog shows a dialog to confirm if the user wants to schedule the event during the raining time
     * @param rainingTimeConfirmed Boolean that checks if the event is during the raining time
     * @param message Message to display in the dialog
     */
    @Composable
    fun eventDuringRainingTimesConfirmDialog(rainingTimeConfirmed: Boolean, message: String) {
        val openDialog = remember { mutableStateOf(true) } // True by default
        if (!rainingTimeConfirmed && openDialog.value && message != "") { // Only show the event if the event is during the raining time and the message is not empty
            AlertDialog(
                onDismissRequest = { openDialog.value = false },
                title = { Text("Confirmation") },
                text = { Text("$message") },
                confirmButton = {
                    Button(onClick = {
                        openDialog.value = false
                        isRainingTimeConfirmed = true // Set the boolean to true if the user confirms that they want to schedule the event during the raining time
                        showRainingTimesDialog = false
                    }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        openDialog.value = false
                        showRainingTimesDialog = true
                    }) {
                        Text("No") // Just close the dialog if the user doesn't denies
                    }
                }
            )

        }
    }

    @Composable
    open fun AddEventHeader(title: String, onBackClick: () -> Unit) {
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
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondary,
                )
                // This is a placeholder to push the title to the center
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }

    @Composable
    open fun AddEventPage(navigateBack: () -> Unit) {
        val context = LocalContext.current
        val user = FirebaseLoginHelper().getCurrentUser()?.uid
        FirebaseApp.initializeApp(context)
        Column {
            AddEventHeader(title = "Add New Event", onBackClick = navigateBack)
            AddEventUI(navigateBack)
            EventCreateItem("Create Event", isOutside, isOptimized, user, navigateBack, context)
        }
    }

    fun handleErrors(errors: List<Error>, user: String?, dateError: Boolean, isTimeSelected: Boolean, context: Context) {
        if (user == null) {
            Toast.makeText(context, "You are logged out. Please log in and try again", Toast.LENGTH_LONG).show()
        }
        if (!isTimeSelected) {
            Toast.makeText(context, "Please select a valid start and end time", Toast.LENGTH_LONG).show()
        }
        if (dateError) {
            Toast.makeText(context, "Start time cannot be after end time", Toast.LENGTH_LONG).show()
        }
        if (errors.isNotEmpty()) {
            errors.forEach { error ->
                Toast.makeText(context, error.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    @Preview
    @Composable
    fun AddEventHeaderPreview() {
        Box {
            AddEventHeader(title = "Add new Event", onBackClick = {})
        }
    }

    @Preview
    @Composable
    fun EventNamePreview() {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            EventNameItem(
                name = "Event Name",
                updateName = {},
                requiredEmpty = false
            )
        }
    }

    @Preview
    @Composable
    fun EventAtendeesPreview() {
        Box(Modifier
            .background(MaterialTheme.colorScheme.background)
            .height(300.dp)
        ) {
            EventAttendeesItem(
                initialContactList = listOf(),
                initialExpandedContact = "",
                contactsGranted = false,
                requestPermissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = {}
                ),
                contactsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickContact(),
                    onResult = {}
                )
            )
        }
    }

    fun navigateBack(navController: NavController) {
        navController.popBackStack()
    }
}
