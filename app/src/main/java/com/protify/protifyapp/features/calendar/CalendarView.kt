package com.protify.protifyapp.features.calendar
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.protify.protifyapp.features.events.EventView
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class CalendarView {

    // private val dateUtils = DateUtils()
    // private var currentDate: String = dateUtils.formatDate(dateUtils.getCurrentDate())


    @Composable
    fun CalendarHeader(
        data: CalendarUiModel,
        onNextClickListener: (LocalDate) -> Unit,
        onPreviousClickListener: (LocalDate) -> Unit,
        onToggleViewClickListener: () -> Unit,
        onAddEventClickListener: () -> Unit,
        isMonthView: Boolean
    ) {
        var selectedTabIndex by remember { mutableStateOf(1) }
        val tabTitles =
            listOf("Month View", "Home", "Add Event") // Adds the new tab title here

        Column {
            Row {
                Text(
                    if (data.selectedDate.isToday) {
                        "Today"
                    } else {
                        data.selectedDate.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                    fontWeight = FontWeight.Bold
                )
                if (!isMonthView) { // Only show the arrows in week view
                    IconButton(
                        onClick = {
                            onPreviousClickListener(data.startDate.date.minusWeeks(1)) // Go to the previous week
                        },
                        modifier = Modifier.size(48.dp).padding(8.dp).align(Alignment.CenterVertically)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowLeft,
                                contentDescription = "Go to previous week",
                                tint = Color.Blue,
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            onNextClickListener(data.startDate.date.plusWeeks(0)) // Go to the next week
                        },
                        modifier = Modifier.size(48.dp).padding(8.dp).align(Alignment.CenterVertically)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowRight,
                                contentDescription = "Go to next week",
                                tint = Color.Blue,
                            )
                        }
                    }
                }
            }

                TabRow(selectedTabIndex = selectedTabIndex) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            when (index) {
                                0, 1 -> onToggleViewClickListener()
                                2 -> onAddEventClickListener() // Handles click event for the new tab
                            }
                        }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CalendarItem(date: CalendarUiModel.Date, onClickListener: (CalendarUiModel.Date) -> Unit, isMonthView: Boolean) {
        val dataSource = CalendarDataSource()
        val currentUser = FirebaseLoginHelper().getCurrentUser()
        var showDialog by remember { mutableStateOf(false) }
        val backgroundColor by animateColorAsState(
            targetValue = when {
                date.isSelected -> MaterialTheme.colorScheme.primary
                date.isToday -> MaterialTheme.colorScheme.primary // Change this to the color you want for today
                else -> MaterialTheme.colorScheme.secondary
            },
            label = ""
        )

        Column {
            Text(
                text = date.day,
                modifier = Modifier
                    .padding(vertical = 4.dp, horizontal = 4.dp)
                    .align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            ElevatedCard(
                onClick = {
                    onClickListener(date)
                    if (isMonthView) {
                        showDialog = true
                    }
                },
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = backgroundColor,
                )
            ) {
                Column(
                    modifier = Modifier
                        .width(40.dp)
                        .height(40.dp)
                        .padding(10.dp)
                ) {
                    Text(
                        text = date.date.dayOfMonth.toString(),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (date.isSelected) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        ),
                        textAlign = TextAlign.Center,
                    )
                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = { showDialog = false },
                            title = {
                                Text(
                                    date.date.format(
                                        DateTimeFormatter.ofLocalizedDate(
                                            FormatStyle.FULL
                                        )
                                    )
                                )
                            }, // Displays the exact day
                            text = {
                                // Display hourly breakdown here
                                LazyColumn {
                                    items((0..23).toList()) { hour ->
                                        listOf(0, 30).forEach { minute ->
                                            val time = LocalTime.of(hour, minute)
                                            val formattedTime = time.format(DateTimeFormatter.ofPattern("h:mm a"))
                                            val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"))
                                            Text(
                                                text = formattedTime,
                                                modifier = Modifier.padding(16.dp),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (formattedTime == currentTime) Color.Red else MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center
                                            )
                                            // Adds a divider after each hour
                                            if (minute == 30) {
                                                Divider(color = Color.Gray, thickness = 1.dp)
                                            }
                                            // Displays the events for each hour

                                            dataSource.getFirestoreEvents(currentUser!!.uid, currentUser.metadata!!.creationTimestamp, date.date.month.toString(), date.date.dayOfMonth.toString(), date.date.year.toString()) { events ->
                                                if (events.isNotEmpty()) {

                                                    date.events = events

                                                }
                                            }
                                            date.events.forEach { event ->
                                                val eventStartTime = LocalTime.parse(event.startTime, DateTimeFormatter.ofPattern("h:mm a"))
                                                val eventEndTime = LocalTime.parse(event.endTime, DateTimeFormatter.ofPattern("h:mm a"))
                                                if ((eventStartTime.isAfter(time) && eventStartTime.isBefore(time.plusHours(1))) ||
                                                    (eventEndTime.isAfter(time) && eventEndTime.isBefore(time.plusHours(1)))) {
                                                    Text(
                                                        text = "${event.title} (${event.startTime} - ${event.endTime})",
                                                        modifier = Modifier.padding(8.dp),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                            },
                            confirmButton = {
                                Button(onClick = { showDialog = false }) {
                                    Text("OK")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    @Composable
    fun CalendarContent(
        data: CalendarUiModel,
        onDateClickListener: (CalendarUiModel.Date) -> Unit,
        isMonthView: Boolean
    ) {
        if (isMonthView) {
            // Generates a list of dates for all the months you want to display
            val allDates = generateDatesForMonths()

            // Groups the dates by both the month and the year
            val datesGroupedByMonth = allDates.groupBy { YearMonth.from(it.date) }
            val listState = rememberLazyListState()

            // Scrolls to the current month when the CalendarContent is first displayed
            LaunchedEffect(key1 = Unit) {
                listState.animateScrollToItem(index = allDates.indexOfFirst { YearMonth.from(it.date) == YearMonth.now() })
            }

            LazyColumn(state = listState) {
                datesGroupedByMonth.forEach { (yearMonth, dates) ->
                    item {
                        Text(
                            // Formats the yearMonth to display the full name of the month and the year
                            text = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                        )
                    }
                    items(dates.chunked(7)) { week ->
                        Row {
                            week.forEach { date ->
                                CalendarItem(date, onDateClickListener, isMonthView)
                            }
                        }
                    }
                    item {
                        Divider(color = Color.Gray, thickness = 1.dp)
                    }
                }
            }
        } else {
            LazyRow {
                items(items = data.visibleDates) { date ->
                    CalendarItem(date, onDateClickListener, isMonthView)
                }
            }
        }
    }
    // Generates a list of dates for all the months you want to display
    fun generateDatesForMonths(): List<CalendarUiModel.Date> {
        val dataSource = CalendarDataSource()
        val startDate = dataSource.today.withDayOfMonth(1) // Start from the current month
        val endDate = dataSource.today.withDayOfMonth(1).plusMonths(12) // Ends at 12 months later
        val allDates = dataSource.getDatesBetween(startDate, endDate)
        return allDates.map { CalendarUiModel.Date(it, false, it.isEqual(dataSource.today), false) }
    }
    @Composable
    fun Calendar(navigateToAddEvent: () -> Unit) {
        val dataSource = CalendarDataSource()
        //val selectedTabIndex by remember { mutableStateOf(0) }
        var isMonthView by remember { mutableStateOf(false) }
        var calendarUiModel by remember {
            mutableStateOf(
                dataSource.getData(
                    lastSelectedDate = dataSource.today,
                    isMonthView = isMonthView
                )
            )
        }
        var isLoadingEvents by remember { mutableStateOf(true) }

        dataSource.getFirestoreEvents("uid", 1234567890L, "January", "1", "2023") { events ->
            if (events.isNotEmpty()) {
                calendarUiModel.selectedDate.events = events
                isLoadingEvents = false
            } else {

                println("No events found for the given date.")
            }
        }
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isMonthView) 0.85f else 0.5f)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CalendarHeader(
                        data = calendarUiModel,
                        onPreviousClickListener = { startDate ->
                            val finalStartDate =
                                if (isMonthView) startDate.minusMonths(1) else startDate.minusDays(1)
                            calendarUiModel = dataSource.getData(
                                startDate = finalStartDate,
                                lastSelectedDate = calendarUiModel.selectedDate.date,
                                isMonthView = isMonthView
                            )
                        },
                        onNextClickListener = { startDate ->
                            val finalStartDate =
                                if (isMonthView) startDate.plusMonths(1) else startDate.plusDays(1)
                            calendarUiModel = dataSource.getData(
                                startDate = finalStartDate,
                                lastSelectedDate = calendarUiModel.selectedDate.date,
                                isMonthView = isMonthView
                            )
                        },
                        onToggleViewClickListener = {
                            isMonthView = !isMonthView
                            calendarUiModel = dataSource.getData(
                                startDate = dataSource.today,
                                lastSelectedDate = dataSource.today,
                                isMonthView = isMonthView
                            )
                        },
                        onAddEventClickListener = navigateToAddEvent,
                        isMonthView = isMonthView
                    )

                    CalendarContent(data = calendarUiModel, onDateClickListener = { date ->
                        calendarUiModel = dataSource.getData(
                            startDate = calendarUiModel.startDate.date,
                            lastSelectedDate = date.date,
                            isMonthView = isMonthView
                        )
                        isLoadingEvents = true
                        dataSource.getFirestoreEvents(
                            FirebaseLoginHelper().getCurrentUser()!!.uid,
                            FirebaseLoginHelper().getCurrentUser()?.metadata!!.creationTimestamp,
                            date.date.month.toString(),
                            date.date.dayOfMonth.toString(),
                            date.date.year.toString()
                        ) { events ->
                            if (events.isNotEmpty()) {
                                calendarUiModel.selectedDate.events = events
                            }
                            isLoadingEvents = false
                        }


                        val finalStartDate =
                            if (isMonthView) date.date else calendarUiModel.startDate.date
                        calendarUiModel = dataSource.getData(
                            startDate = finalStartDate,
                            lastSelectedDate = date.date,
                            isMonthView = isMonthView
                        )
                    }, isMonthView = isMonthView)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isMonthView) 0.15f else 0.5f)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                    )
            ) {
                if (!isMonthView) { // Only shows the EventCard in week view
                    EventView().EventCard(calendarUiModel, navigateToAddEvent, isLoadingEvents)
                }
            }
        }
        //fun navigateToEventDetails(navController: NavHostController, calendarUiModel: CalendarUiModel) {
        //    navController.navigate("eventDetails/${calendarUiModel.selectedDate.date}/${calendarUiModel.selectedDate.events[0].id}")
    }
}

