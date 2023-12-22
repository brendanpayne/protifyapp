package com.protify.protifyapp.features.calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.protify.protifyapp.features.events.EventView
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import com.protify.protifyapp.utils.DateUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class CalendarView {

    private val dateUtils = DateUtils()
    private var currentDate: String = dateUtils.formatDate(dateUtils.getCurrentDate())

    private fun updateMonthViewUI() {
        // Update UI elements specific to the month view
        // Show/hide or update specific UI components for the month view
    }

    private fun fetchDataForMonthView() {
        // Fetch and update data specific to the month view
        // Load a month-specific dataset or trigger a network request
    }

    private fun updateDayViewUI() {
        // Update UI elements specific to the day view
        // Show/hide or update specific UI components for the day view
    }

    private fun fetchDataForDayView() {
        // Fetch and update data specific to the day view
        // Load a day-specific dataset or trigger a network request
    }

    @Composable
    fun CalendarHeader(
        data: CalendarUiModel,
        onNextClickListener: (LocalDate) -> Unit,
        onPreviousClickListener: (LocalDate) -> Unit,
        onToggleViewClickListener: () -> Unit
    ) {
        var isMonthView by remember { mutableStateOf(true) }

        Row {
            Text(
                if (data.selectedDate.isToday) {
                    "Today"
                } else {
                    data.selectedDate.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
                },
                modifier = Modifier
                    .padding(16.dp)
                    .clickable(
                        indication = null,
                        interactionSource = MutableInteractionSource(),
                        onClick = onToggleViewClickListener
                    )
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            )
            IconButton(
                onClick = { onPreviousClickListener(data.startDate.date) }
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowLeft,
                    contentDescription = "Left Arrow"
                )
            }
            IconButton(
                onClick = { onNextClickListener(data.endDate.date) }
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowRight,
                    contentDescription = "Right Arrow"
                )
            }
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CalendarItem(date: CalendarUiModel.Date, onClickListener: (CalendarUiModel.Date) -> Unit) {
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
                },
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    // TODO: If there are dates within the week that fall in the previous/next month, they should be a different (darker) color.
                    containerColor = if (date.isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
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
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    @Composable
    fun CalendarContent(data: CalendarUiModel, onDateClickListener: (CalendarUiModel.Date) -> Unit, isMonthView: Boolean) {
        if (isMonthView) {
            LazyColumn {
                items(data.visibleDates.chunked(7)) { week ->
                    Row {
                        for (date in week) {
                            CalendarItem(date, onDateClickListener)
                        }
                    }
                }
            }
        } else {
            LazyRow {
                items(items = data.visibleDates) { date ->
                    CalendarItem(date, onDateClickListener)
                }
            }
        }
    }

    @Composable
    fun Calendar(navigateToAddEvent: () -> Unit) {
        val dataSource = CalendarDataSource()
        var isMonthView by remember { mutableStateOf(true) }
        var calendarUiModel by remember { mutableStateOf(dataSource.getData(lastSelectedDate = dataSource.today, isMonthView = isMonthView))}
        var isLoadingEvents by remember { mutableStateOf(true)}

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                )
        ) {
            Column (
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CalendarHeader(
                    // TODO: Month view jumps two months rather than one due to fetching the first/last day of the month, which in this case is the previous/next month
                    // TODO: Add/Subtract 7 days from the first/last day of the month to fix this?

                    // TODO: Fix weekly view from jumping a whole month? IDK why this happens
                    data = calendarUiModel,
                    onPreviousClickListener = { startDate ->
                        val firstDay = if (isMonthView) startDate.withDayOfMonth(1) else startDate.with(DayOfWeek.SUNDAY)
                        val finalStartDate = firstDay.minusMonths(1)
                        calendarUiModel = dataSource.getData(startDate = finalStartDate, lastSelectedDate = calendarUiModel.selectedDate.date, isMonthView = isMonthView)
                    },
                    onNextClickListener = { endDate ->
                        val lastDay = if (isMonthView) endDate.withDayOfMonth(endDate.lengthOfMonth()) else endDate.with(DayOfWeek.SATURDAY)
                        val finalStartDate = lastDay.plusMonths(1)
                        calendarUiModel = dataSource.getData(startDate = finalStartDate, lastSelectedDate = calendarUiModel.selectedDate.date, isMonthView = isMonthView)
                    },
                    onToggleViewClickListener = {
                        isMonthView = !isMonthView
                        calendarUiModel = dataSource.getData(startDate = dataSource.today, lastSelectedDate = dataSource.today, isMonthView = isMonthView)
                    }
                )
                CalendarContent(data = calendarUiModel, onDateClickListener = { date ->
                    val finalStartDate = if (isMonthView) date.date else calendarUiModel.startDate.date
                    calendarUiModel = dataSource.getData(startDate = finalStartDate, lastSelectedDate = date.date, isMonthView = isMonthView)
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
                }, isMonthView = isMonthView)
            }
        }
        // TODO: Resize the calendar to fit the whole month when in month view
        EventView().EventCard(calendarUiModel, navigateToAddEvent, isLoadingEvents)
    }

    fun navigateToEventDetails(navController: NavHostController, calendarUiModel: CalendarUiModel) {
        navController.navigate("eventDetails/${calendarUiModel.selectedDate.date}/${calendarUiModel.selectedDate.events[0].id}")
    }

}