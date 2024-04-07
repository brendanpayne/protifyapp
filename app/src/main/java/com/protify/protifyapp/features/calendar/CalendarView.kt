package com.protify.protifyapp.features.calendar
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.protify.protifyapp.features.events.EventView
import com.protify.protifyapp.HomeActivity
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class CalendarView(private val navController: NavController) {
    private var eventsForAllDates = mutableStateMapOf<LocalDate, List<Event>>()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CalendarItem(
        date: CalendarUiModel.Date,
        onClickListener: (CalendarUiModel.Date) -> Unit,
        isMonthView: Boolean
    ) {
        val eventCount = eventsForAllDates[date.date]?.size ?: 0
        val backgroundColor by animateColorAsState(
            targetValue = when {
                date.isSelected -> MaterialTheme.colorScheme.primary
                date.isToday -> MaterialTheme.colorScheme.outline
                else -> MaterialTheme.colorScheme.surface
            },
            label = ""
        )
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(50.dp)
                .height(60.dp)
        ) {
            ElevatedCard(
                onClick = {
                    onClickListener(date)
                },
                modifier = Modifier.padding(horizontal = 4.dp),
                shape = RoundedCornerShape(8.dp),
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
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Row(modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until if (eventCount > 3) 3 else eventCount) {
                    Box (
                        modifier = Modifier
                            .size(8.dp)
                            .padding(1.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun CalendarContent(
        data: CalendarUiModel,
        onDateClickListener: (CalendarUiModel.Date) -> Unit,
        onToggleViewClickListener: () -> Unit,
        isMonthView: Boolean
    ) {
        val dataSource = CalendarDataSource()
        val startMonth = LocalDate.of(1970, 1, 1)
        val startWeek = LocalDate.of(1970, 1, 4)
        val currentMonth = LocalDate.now().withDayOfMonth(1)
        val currentWeek = LocalDate.now()
        val monthsSince1970 = Period.between(startMonth, currentMonth).toTotalMonths().toInt()
        val daysSince1970 = ChronoUnit.DAYS.between(startWeek, currentWeek).toInt()
        val weeksSince1970 = daysSince1970 / 7
        val monthState = rememberPagerState(pageCount = {
            1000
        },
            initialPage = monthsSince1970
        )
        val weekState = rememberPagerState(pageCount = {
            5000
        },
            initialPage = weeksSince1970
        )

        if (isMonthView) {
            HorizontalPager(
                state = monthState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val monthToShow = startMonth.plusMonths(pageIndex.toLong())
                val visibleDates = dataSource.getDatesBetween(
                    monthToShow.withDayOfMonth(1),
                    monthToShow.withDayOfMonth(monthToShow.lengthOfMonth()),
                    true
                )
                val monthData = dataSource.toUiModel(visibleDates, dataSource.today)

                if (monthData.visibleDates.isNotEmpty()) {
                    LazyColumn (
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Top
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val monthFormat = if (monthData.startDate.date.year == dataSource.today.year)
                                    "MMMM" else "MMMM yyyy"
                                Text(
                                    text = monthData.startDate.date.plusDays(15)
                                        .format(DateTimeFormatter.ofPattern(monthFormat)),
                                    modifier = Modifier
                                        .padding(start = 16.dp)
                                        .align(Alignment.CenterVertically),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { onToggleViewClickListener() }) {
                                    Icon(
                                        imageVector = Icons.Outlined.KeyboardArrowUp,
                                        contentDescription = "Week View",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        item{
                            Row(
                                modifier = Modifier
                                    .padding(start = 14.dp, end = 14.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DayOfWeek.entries.forEach { dayOfWeek ->
                                    Text(
                                        text = dayOfWeek.name.substring(0, 3),
                                        modifier = Modifier
                                            .padding(start = 8.dp, end = 8.dp)
                                            .weight(1f),
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        items(monthData.visibleDates.chunked(7)) { week ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                week.forEach { date ->
                                    CalendarItem(date, onDateClickListener, true)
                                    // TODO: Fix isSelected not updating on click
                                    // TODO: Fix event view/firebase flakiness on month view
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val weekFormat = if (data.selectedDate.date.year == dataSource.today.year)
                "MMMM" else "MMMM yyyy"
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = data.selectedDate.date.format(DateTimeFormatter.ofPattern(weekFormat)),
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .align(Alignment.CenterVertically),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { onToggleViewClickListener() }) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "Month View",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                HorizontalPager(
                    state = weekState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val weekToShow = startWeek.plusWeeks(pageIndex.toLong())
                    val visibleDates = dataSource.getDatesBetween(
                        weekToShow,
                        weekToShow.plusDays(6),
                        false
                    )
                    val weekData = dataSource.toUiModel(visibleDates, dataSource.today)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(start = 14.dp, end = 14.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DayOfWeek.entries.forEach { dayOfWeek ->
                                Text(
                                    text = dayOfWeek.name.substring(0, 3),
                                    modifier = Modifier
                                        .padding(start = 8.dp, end = 8.dp)
                                        .weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (weekData.visibleDates.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp)
                            ) {
                                items(weekData.visibleDates) { date ->
                                    CalendarItem(date, onDateClickListener, false) // TODO: Fix isSelected not updating on click
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    @Composable
    fun Calendar(context: Context, navigateToAddEvent: () -> Unit) {
        val dataSource = CalendarDataSource()
        val user = FirebaseLoginHelper().getCurrentUser()
        //val selectedTabIndex by remember { mutableStateOf(0) }
        var events by remember { mutableStateOf(listOf<Event>()) }
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
        var isAiCompleted by remember { mutableStateOf(false) }
        var showAiEvents by remember { mutableStateOf(false) } // THis is for the AI event toggle button
        val date = calendarUiModel.selectedDate
        dataSource.getFirestoreEventsAndIds(
            FirebaseLoginHelper().getCurrentUser()!!.uid,
            FirebaseLoginHelper().getCurrentUser()?.metadata!!.creationTimestamp,
            date.date.month.toString(),
            date.date.dayOfMonth.toString(),
            date.date.year.toString()
        ) { fetchedEvents ->
                events = fetchedEvents
            isLoadingEvents = false
        }

        fetchEvents(
            scope = rememberCoroutineScope(),
            calendarUiModel = calendarUiModel,
            dataSource = dataSource,
            eventsForAllDates = eventsForAllDates,
            isLoadingEvents = remember { mutableStateOf(isLoadingEvents) }
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isMonthView) 1.2f else 0.3f)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CalendarContent(
                        data = calendarUiModel,
                        onDateClickListener = { date ->
                            calendarUiModel = dataSource.getData(
                                startDate = calendarUiModel.startDate.date,
                                lastSelectedDate = date.date,
                                isMonthView = isMonthView
                            )

                            isLoadingEvents = true
                            dataSource.getFirestoreEventsAndIds(
                                FirebaseLoginHelper().getCurrentUser()!!.uid,
                                FirebaseLoginHelper().getCurrentUser()?.metadata!!.creationTimestamp,
                                date.date.month.toString(),
                                date.date.dayOfMonth.toString(),
                                date.date.year.toString()
                            ) { events ->
                                calendarUiModel.selectedDate.hasEvents = events.isNotEmpty()
                                calendarUiModel.selectedDate.events = events
                                eventsForAllDates[date.date] = events
                                isLoadingEvents = false
                            }
                        },
                        onToggleViewClickListener = {
                            isMonthView = !isMonthView
                            calendarUiModel = dataSource.getData(
                                startDate = dataSource.today,
                                lastSelectedDate = dataSource.today,
                                isMonthView = isMonthView
                            )
                        },
                        isMonthView = isMonthView
                    )
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
                        dataSource.getFirestoreEventsAndIds(
                            FirebaseLoginHelper().getCurrentUser()!!.uid,
                            FirebaseLoginHelper().getCurrentUser()?.metadata!!.creationTimestamp,
                            date.date.month.toString(),
                            date.date.dayOfMonth.toString(),
                            date.date.year.toString()
                        ) { events ->
                            calendarUiModel.selectedDate.hasEvents = events.isNotEmpty()
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
                    .weight(1f)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                    )
            ) {
                EventView(navController = navController).EventCard(
                    calendarUiModel,
                    navigateToAddEvent,
                    isLoadingEvents
                )
            }
        }
    }
    private fun fetchEvents(
        scope: CoroutineScope,
        calendarUiModel: CalendarUiModel,
        dataSource: CalendarDataSource,
        eventsForAllDates: MutableMap<LocalDate, List<Event>>,
        isLoadingEvents: MutableState<Boolean>
    ) {
        isLoadingEvents.value = true
        scope.launch {
            val allDates = calendarUiModel.visibleDates
            coroutineScope {
                allDates.forEach { date ->
                    launch {
                        dataSource.getFirestoreEventsAndIds(
                            FirebaseLoginHelper().getCurrentUser()!!.uid,
                            FirebaseLoginHelper().getCurrentUser()?.metadata!!.creationTimestamp,
                            date.date.month.toString(),
                            date.date.dayOfMonth.toString(),
                            date.date.year.toString()
                        ) { events ->
                            if (events.isNotEmpty()) {
                                calendarUiModel.selectedDate.events = events
                                eventsForAllDates[date.date] = events
                                calendarUiModel.selectedDate.hasEvents = true
                            } else {
                                calendarUiModel.selectedDate.hasEvents = false
                            }
                        }
                    }
                }
            }
            isLoadingEvents.value = false
        }
        //fun navigateToEventDetails(navController: NavHostController, calendarUiModel: CalendarUiModel) {
        //    navController.navigate("eventDetails/${calendarUiModel.selectedDate.date}/${calendarUiModel.selectedDate.events[0].id}")
    }
    @Composable
    fun startAiButton(onAddEventClickListener: () -> Unit) {
    // Add AI Button
        Button(
            onClick = onAddEventClickListener,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text("Optimize Schedule")

        }


    }
}

