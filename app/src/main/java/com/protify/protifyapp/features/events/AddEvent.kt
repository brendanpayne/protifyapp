package com.protify.protifyapp.features.events

import android.app.TimePickerDialog
import android.content.ContentUris
import android.icu.util.TimeZone
import android.provider.ContactsContract
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.protify.protifyapp.APIKeys
import com.protify.protifyapp.FirestoreEvent
import com.protify.protifyapp.FirestoreHelper
import com.protify.protifyapp.NetworkManager
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import com.protify.protifyapp.utils.MapsDurationUtils
import com.protify.protifyapp.utils.WeatherUtils
import java.time.LocalDateTime

class AddEvent {

    private var name: String by mutableStateOf("")
    private var startTime: LocalDateTime by mutableStateOf(LocalDateTime.now())
    private var endTime: LocalDateTime by mutableStateOf(LocalDateTime.MAX)
    private var location: String? by mutableStateOf("")
    private var description: String? by mutableStateOf("")
    //private var timeZone: TimeZone? by mutableStateOf(TimeZone.getTimeZone("America/New_York"))
    private var timeZone: TimeZone? by mutableStateOf(TimeZone.getDefault())
    private var importance: Int by mutableIntStateOf(3)
    private var attendees: List<Attendee> by mutableStateOf(listOf())
    private var formattedStartTime: String by mutableStateOf("")
    private var formattedEndTime: String by mutableStateOf("")
    private var dateError: Boolean by mutableStateOf(false)
    private var contactList: List<Attendee> by mutableStateOf(listOf())
    private var contactNames: List<String> by mutableStateOf(listOf())
    private var rainingTimesMessage: String by mutableStateOf("")
    private var isRainingTimeConfirmed: Boolean by mutableStateOf(true) // True by default... innocent until guilty

    private fun updateName(newName: String) {
        name = newName
    }
    private fun updateStartTime(month: Int, dayOfMonth: Int, year: Int, hour: Int, minute: Int ) {
        var newStartTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute)
        startTime = newStartTime
        formattedStartTime(newStartTime)
    }
    private fun updateEndTime(month: Int, dayOfMonth: Int, year: Int, hour: Int, minute: Int ) {
        var newEndTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute)
        endTime = newEndTime
        formattedEndTime(newEndTime)
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
        if (hour > 12) {
            hour -= 12
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
        if (hour > 12) {
            hour -= 12
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
    private fun updateLocation(newLocation: String) {
        location = newLocation
    }
    private fun updateDescription(newDescription: String) {
        description = newDescription
    }
    private fun updateTimeZone(newTimeZone: TimeZone) {
        timeZone = newTimeZone
    }
    private fun updateImportance(newImportance: Int) {
        importance = newImportance
    }
    private fun updateAttendees(newAttendees: List<Attendee>) {
        attendees = newAttendees
    }
    private fun isTimeSelected(): Boolean {
        return formattedStartTime != "" && formattedEndTime != ""
    }
    private fun updateAttendeeList(name: String, number: String, email: String) {
        Attendee(name, email, number).let { attendee ->
            contactList = contactList + attendee
        }
    }
    private fun updateContactNames(name: String) {
        contactNames = contactNames + name
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AddEventPage(navigateBack: () -> Unit) {

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
        val americanTimeZoneNames = listOf(
            "America/New_York",
            "America/Chicago",
            "America/Denver",
            "America/Los_Angeles",
            "America/Anchorage",
            "America/Adak",
            "Pacific/Honolulu"
        ).sorted()
        var expandedTimeZone by remember { mutableStateOf(false) }
        //Importance
        val importanceNames = listOf(
            "1",
            "2",
            "3",
            "4",
            "5"
        )
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
        var isOutside by remember { mutableStateOf(false) }
        //isOptimized boolean
        var isOptimized by remember { mutableStateOf(true)}
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
                                }
                            }
                        }
                    }
                }

            }
        }
        eventDuringRainingTimesConfirmDialog(eventDuringRainingTime = isRainingTimeConfirmed, message =rainingTimesMessage) // Show the dialog if the event is during the raining time

        val listState = rememberLazyListState()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ScrollView(LocalContext.current)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(
                        text = "Add Event",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                                name -> updateName(name)
                                        },
                        label = { Text("Event Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = if (name.length > 50 || (name.isEmpty() && requiredEmpty)) {
                                MaterialTheme.colorScheme.error
                            }

                            else {
                                MaterialTheme.colorScheme.outline
                            },
                            focusedBorderColor = if (name.length > 50) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        ),
                        supportingText = {
                            if (name.length > 50) {
                                Text(
                                    text = "${name.length}/50",
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            if(name.isEmpty()) {
                                Text(
                                    text = "Required",
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    )
                }
                item {
                    Row {
                        Button(
                            modifier = Modifier
                                .weight(5f)
                                .padding(vertical = 16.dp, horizontal = 8.dp),

                            onClick = {
                                datePickerDialog.show()
                                datePickerDialog.setOnDateSetListener { view, year, month, dayOfMonth ->
                                    selectedDate.value = "$month/$dayOfMonth/$year"
                                    selectedMonth = month + 1
                                    selectedDayOfMonth = dayOfMonth
                                    selectedYear = year
                                    timePickerDialog.show()
                                    timePickerDialog.setOnDismissListener { view ->
                                        //If the user has selected a date and time, update the start time
                                        if (selectedTime.value != "" && selectedDate.value != "") {
                                            updateStartTime(selectedMonth, selectedDayOfMonth, selectedYear, selectedHour, selectedMinute)
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("Select Start Time")
                        }
                        OutlinedTextField(
                            value = formattedStartTime,
                            //This displays the formatted version of the start time
                            onValueChange = { formattedStartTime(startTime) },
                            label = { Text("Start Time") },
                            modifier = Modifier
                                .weight(5f)
                                .padding(vertical = 16.dp, horizontal = 8.dp),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = if ((formattedStartTime == "" && requiredEmpty) || dateError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                            ),
                            supportingText = {
                                if (dateError) {
                                    Text(
                                        modifier = Modifier.fillMaxSize(),
                                        text = "Start time cannot be after end time",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                if (formattedStartTime == "" && !dateError) {
                                    Text(
                                        text = "Required",
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            }
                        )
                    }

                }

                item {
                    Row {
                        Button(
                            modifier = Modifier
                                .weight(5f)
                                .padding(vertical = 16.dp, horizontal = 8.dp),

                            onClick = {
                                datePickerDialog.show()
                                datePickerDialog.setOnDateSetListener { _, year, month, dayOfMonth ->
                                    selectedDate.value = "$month/$dayOfMonth/$year"
                                    selectedMonth = month + 1
                                    selectedDayOfMonth = dayOfMonth
                                    selectedYear = year
                                    timePickerDialog.show()
                                    timePickerDialog.setOnDismissListener {
                                        //If the user has selected a date and time, update the end time
                                        if (selectedTime.value != "" && selectedDate.value != "") {
                                            updateEndTime(selectedMonth, selectedDayOfMonth, selectedYear, selectedHour, selectedMinute)
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("Select End Time")
                        }
                        OutlinedTextField(
                            value = formattedEndTime,
                            //This displays the formatted version of the end time
                            onValueChange = { formattedEndTime(endTime) },
                            label = { Text("End Time") },
                            modifier = Modifier
                                .weight(5f)
                                .padding(vertical = 16.dp, horizontal = 8.dp),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = if ((formattedEndTime == "" && requiredEmpty) || dateError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                            ),
                            supportingText = {
                                if (dateError) {
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = "Start time cannot be after end time",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                if (formattedEndTime == "" && !dateError) {
                                    Text(
                                        text = "Required",
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            }
                        )
                    }

                }
                item {
                    var isFocused by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = location ?: "",
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        trailingIcon = {
                            IconButton(onClick = { location = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.outline,
                                )
                            }
                        },
                        modifier = Modifier.onFocusChanged { focusState ->
                            if (focusState.isFocused && !isFocused) {
                                isFocused = true
                                autocompleteLauncher.launch(
                                    Autocomplete.IntentBuilder(
                                        AutocompleteActivityMode.OVERLAY,
                                        listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS)
                                    ).build(context)
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
                        },
                        readOnly = true
                    )
                }
                item {
                    //Make a tick box to check if the event is outdoors
                    Row {
                        Text(
                            text = "Is this event outdoors?",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                        Checkbox(
                            checked =isOutside,
                            onCheckedChange = { isChecked -> isOutside = isChecked },
                            modifier = Modifier.padding(16.dp),
                        )


                    }
                }
                item {
                    OutlinedTextField(
                        value = description ?: "",
                        onValueChange = { description -> updateDescription(description) },
                        label = { Text("Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(200.dp),
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
                item {
                    ExposedDropdownMenuBox(
                        expanded = expandedTimeZone,
                        onExpandedChange = { expandedTimeZone = !expandedTimeZone },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        content = {
                            TextField(
                                value = timeZone?.displayName ?: "",
                                onValueChange = {},
                                label = { Text("Time Zone") },
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
                                onDismissRequest = { expandedTimeZone = false }
                            ) {
                                americanTimeZoneNames.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item) },
                                        onClick = {
                                            updateTimeZone(TimeZone.getTimeZone(item))
                                            expandedTimeZone = false
                                                  }
                                    )
                                }
                            }

                        }
                    )
                }
                item {
                    ExposedDropdownMenuBox(
                        expanded = expandedImportance,
                        onExpandedChange = { expandedImportance = !expandedImportance },
                    ) {
                        TextField(
                            value = importance.toString(),
                            onValueChange = {},
                            label = { Text("Importance") },
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
                }
                item {
                    Button(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        onClick = {
                            if (contactsGranted) {
                                contactsLauncher.launch(null)
                            } else {
                                requestPermissionsLauncher.launch(permission)
                                if (contactsGranted) {
                                    contactsLauncher.launch(null)
                                }
                            }
                        }
                    ) {
                        Text("Add Attendees")
                    }
                }
                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                    )
                    ){
                        Text(
                            text = if (contactList.isEmpty()) "No Attendees" else "Attendees",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            contactList.forEach() { attendee ->
                                Button(
                                    onClick = {expandedContact = attendee.name }
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
                item {
                    Row {
                        //Button to allows the AI to make optimizations to this event or not
                        Text (
                            text = "Do you want to allow the AI to optimize this event?",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                        Checkbox(
                            checked = isOptimized,
                            onCheckedChange = { isChecked -> isOptimized = isChecked },
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                item {
                    Button(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        onClick = {
                            var firestoreEvent:FirestoreEvent = FirestoreEvent(
                                attendees = contactList,
                                description = description,
                                endTime = endTime,
                                startTime = startTime,
                                timeZone = timeZone?.displayName,
                                name = name,
                                //Additional field to store the name in lowercase for searching
                                nameLower = name.trim().lowercase(),
                                importance = importance,
                                location = location,
                                rainCheck = false,
                                isRaining = false,
                                mapsCheck = false,
                                distance = 0,
                                //This will be used in the AI model to determine whether this event can be scheduled if it's raining outside
                                isOutside = isOutside,
                                //This will be used to determine whether or not an event is allowed to be optimized by the AI
                                //My naming scheme is so bad that I have to reverse the boolean to make it make sense
                                isOptimized = !isOptimized,
                                isAiSuggestion = false,
                                isUserAccepted = false
                            )
                            val errors = firestoreEvent.validateEvent(firestoreEvent)
                            if (errors.isEmpty() && user != null && !dateError && isTimeSelected() && isRainingTimeConfirmed)  {
                                FirestoreHelper().createEvent(user, firestoreEvent) {
                                    if (it) {
                                        Toast.makeText(context, "Event added successfully", Toast.LENGTH_LONG).show()
                                        navigateBack()
                                    } else {
                                        Toast.makeText(context, "Event could not be added. Please check your network connection", Toast.LENGTH_LONG).show()

                                    }
                                }
                            }
                            if (user == null) {
                                Toast.makeText(context, "You are logged out. Please log in and try again", Toast.LENGTH_LONG).show()
                                //Add redirection to login screen
                            }
                            if (!isTimeSelected()) {
                                Toast.makeText(context, "Please select a valid start and end time", Toast.LENGTH_LONG).show()
                            }
                            if (dateError) {
                                Toast.makeText(context, "Start time cannot be after end time", Toast.LENGTH_LONG).show()
                            }
                            else {
                                requiredEmpty = true
                                errors.forEach { error ->
                                    Toast.makeText(context, error.localizedMessage, Toast.LENGTH_LONG).show()
                                }
                            }

                        }) {
                        Text("Add Event")
                    }
                }
                item {
                    Button(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        onClick = {
                            navigateBack()
                        }
                    ) {
                        Text("Back")
                    }
                }
            }
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
                val isRaining = nonRainingTimes.any { it.first.isBefore(startTime) && it.second.isAfter(endTime) }
                val nonRainingTimesString = nonRainingTimes.joinToString(", ") { "${it.first} - ${it.second}" }
                if (isRaining) {
                    onComplete(true, "It will be raining during your event, you should move it within ${if (nonRainingTimes.size > 1) "these times: $nonRainingTimesString" else "this time: $nonRainingTimesString"}")
                } else {
                    onComplete(false, "")
                }
            }
    }
    @Composable
    fun eventDuringRainingTimesConfirmDialog(eventDuringRainingTime: Boolean, message: String) {
        val openDialog = remember { mutableStateOf(true) } // False by default
        if (eventDuringRainingTime && openDialog.value) {
            AlertDialog(
                onDismissRequest = { openDialog.value = false },
                title = { Text("Confirmation") },
                text = { Text("$message") },
                confirmButton = {
                    Button(onClick = {
                        openDialog.value = false
                        isRainingTimeConfirmed = true // Set the boolean to true if the user confirms that they want to schedule the event during the raining time
                    }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    Button(onClick = { openDialog.value = false }) {
                        Text("No") // Just close the dialog if the user doesn't denies
                    }
                }
            )

        }
    }

    fun navigateBack(navController: NavController) {
        navController.popBackStack()
    }
}
