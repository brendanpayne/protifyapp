package com.protify.protifyapp.features.events

import android.app.TimePickerDialog
import android.provider.CalendarContract
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.protify.protifyapp.FirestoreHelper
import com.protify.protifyapp.NetworkManager
import java.time.LocalDateTime

class AddEvent {

    private var name: String by mutableStateOf("")
    private var startTime: LocalDateTime by mutableStateOf(LocalDateTime.now())
    private var endTime: LocalDateTime by mutableStateOf(LocalDateTime.now())
    private var location: String? by mutableStateOf("")
    private var description: String? by mutableStateOf("")
    private var timeZone: String? by mutableStateOf("")
    private var importance: Int by mutableIntStateOf(0)
    private var attendees: CalendarContract.Attendees? by mutableStateOf(null)
    private var formattedStartTime: String by mutableStateOf("")

    private fun updateName(newName: String) {
        name = newName
    }
    private fun updateStartTime(month: Int, dayOfMonth: Int, year: Int, hour: Int, minute: Int ) {
        var newStartTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute)
        startTime = newStartTime
        formattedStartTime(newStartTime)
    }
    private fun formattedStartTime(startTime: LocalDateTime) {
        val year = startTime.year
        val month = startTime.monthValue
        val dayOfMonth = startTime.dayOfMonth
        var hour = startTime.hour
        val minute = startTime.minute

        if (hour > 12) {
            hour -= 12
            formattedStartTime = "$month/$dayOfMonth/$year $hour:$minute PM"
        } else {
            formattedStartTime = "$month/$dayOfMonth/$year $hour:$minute AM"
        }
    }
    private fun updateEndTime(newEndTime: LocalDateTime) {
        endTime = newEndTime
    }
    private fun updateLocation(newLocation: String) {
        location = newLocation
    }
    private fun updateDescription(newDescription: String) {
        description = newDescription
    }
    private fun updateTimeZone(newTimeZone: String) {
        timeZone = newTimeZone
    }
    private fun updateImportance(newImportance: Int) {
        importance = newImportance
    }
    private fun updateAttendees(newAttendees: CalendarContract.Attendees) {
        attendees = newAttendees
    }
    private fun dateTimeFormatter(date: String, time: String) {

    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AddEventPage(navigateBack: () -> Unit) {

        val context = LocalContext.current
        val networkManager = NetworkManager(context)
        //Date Picker Dialog
        val dayOfMonth = LocalDateTime.now().dayOfMonth
        val month = LocalDateTime.now().monthValue
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

        val listState = rememberLazyListState()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
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
                        onValueChange = { name -> updateName(name) },
                        label = { Text("Event Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                    )
                }
                item {
                    Button(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),

                        onClick = {
                            datePickerDialog.show()
                            datePickerDialog.setOnDateSetListener { view, year, month, dayOfMonth ->
                                selectedDate.value = "$month/$dayOfMonth/$year"
                                selectedMonth = month
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
                }
                item {
                    OutlinedTextField(
                        value = formattedStartTime,
                        //This displays the formatted version of the start time
                        onValueChange = { formattedStartTime(startTime) },
                        label = { Text("Start Time") },
                        enabled = false
                    )
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

    fun navigateBack(navController: NavController) {
        navController.popBackStack()
    }
}
