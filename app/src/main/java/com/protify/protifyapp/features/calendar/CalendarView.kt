package com.protify.protifyapp.features.calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class CalendarView {
    private val dateUtils = DateUtils()
    private var currentDate: String = dateUtils.formatDate(dateUtils.getCurrentDate())
    @Composable
    fun CalendarHeader(data: CalendarUiModel, onNextClickListener: (LocalDate) -> Unit, onPreviousClickListener: (LocalDate) -> Unit) {
        Row{
            Text(
                text = if (data.selectedDate.isToday) {
                    "Today"
                } else {
                    data.selectedDate.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
                },
                modifier = Modifier
                    .padding(16.dp)
                    .clickable(indication = null, interactionSource = MutableInteractionSource(),
                        onClick = {
                        // TODO: When Clicking on the Date, toggle month view

                    })
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
    fun  CalendarContent(data: CalendarUiModel, onDateClickListener: (CalendarUiModel.Date) -> Unit) {
        LazyRow {
            items(items = data.visibleDates) { date ->
                CalendarItem(date, onDateClickListener)
            }
        }
    }

    @Composable
    fun Calendar(navigateToAddEvent: () -> Unit) {
        val dataSource = CalendarDataSource()
        var calendarUiModel by remember { mutableStateOf(dataSource.getData(lastSelectedDate = dataSource.today))}
        var isLoadingEvents by remember { mutableStateOf(true)}
        //var calendarUiModel = dataSource.getData(lastSelectedDate = dataSource.today)

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
                    data = calendarUiModel,
                    onPreviousClickListener = { startDate ->
                        val finalStartDate = startDate.minusDays(1)
                        calendarUiModel = dataSource.getData(startDate = finalStartDate, lastSelectedDate = calendarUiModel.selectedDate.date)

                    },
                    onNextClickListener = { endDate ->
                        val finalStartDate = endDate.plusDays(2)
                        calendarUiModel = dataSource.getData(startDate = finalStartDate, lastSelectedDate = calendarUiModel.selectedDate.date)
                    }
                )
                CalendarContent(data = calendarUiModel) { date ->
                    calendarUiModel = dataSource.getData(startDate = calendarUiModel.startDate.date, lastSelectedDate = date.date)
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

                }
            }
        }
        EventView().EventCard(calendarUiModel, navigateToAddEvent, isLoadingEvents)
    }

    fun navigateToEventDetails(navController: NavHostController, calendarUiModel: CalendarUiModel) {
        navController.navigate("eventDetails/${calendarUiModel.selectedDate.date}/${calendarUiModel.selectedDate.events[0].id}")
    }

}