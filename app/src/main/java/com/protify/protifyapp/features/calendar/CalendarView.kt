package com.protify.protifyapp.features.calendar
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.protify.protifyapp.HomeActivity
import com.protify.protifyapp.features.events.EventView
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class CalendarView(private val navController: NavController) {
    private var eventsForAllDates = mutableStateMapOf<LocalDate, List<Event>>()
    private var selectedDate: CalendarUiModel.Date? by mutableStateOf(LocalDate.now().let {
        CalendarUiModel.Date(
            date = it,
            isSelected = true,
            isToday = true,
            hasEvents = false
        )
    })

    @Composable
    fun CalendarItem(
        date: CalendarUiModel.Date,
        onClickListener: (CalendarUiModel.Date) -> Unit,
        isMonthView: Boolean,
        isSameMonth: Boolean
    ) {
        val isSelected = selectedDate == date
        val isToday = date.date == LocalDate.now()
        val eventCount = eventsForAllDates[date.date]?.size ?: 0
        val backgroundColor by animateColorAsState(
            targetValue = when {
                isSelected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surface
            },
            label = ""
        )
        val textColor by animateColorAsState(
            targetValue = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurface
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
                    selectedDate = date // Update remember variable

                },
                modifier = Modifier.padding(horizontal = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = backgroundColor,
                )
            ) {
                Box(
                    modifier = Modifier
                        .border(
                            width = 2.dp,
                            color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
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
                            color = textColor.copy(
                                alpha = if (isSameMonth) 1f else 0.5f
                            ),
                            textAlign = TextAlign.Center,
                        )
                    }
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
        val scope = rememberCoroutineScope()
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
        var isCurrentMonth: Boolean = true

        LaunchedEffect (monthState){
            snapshotFlow { monthState.currentPage }.collect { page ->
                val monthToShow = startMonth.plusMonths(page.toLong())
                val visibleDates = dataSource.getDatesBetween(
                    monthToShow.withDayOfMonth(1),
                    monthToShow.withDayOfMonth(monthToShow.lengthOfMonth()),
                    true
                )
                val monthData = dataSource.toUiModel(visibleDates, dataSource.today)
                if (monthData.visibleDates.isNotEmpty()) {
                    selectedDate = monthData.visibleDates[6]
                    fetchEvents(
                        scope,
                        monthData,
                        dataSource,
                        eventsForAllDates,
                        mutableStateOf(true)
                    )
                }
            }
        }


        if (isMonthView) {
            HorizontalPager(
                state = monthState,
                modifier = Modifier.fillMaxWidth(),
                beyondBoundsPageCount = 0,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = monthState,
                    pagerSnapDistance = PagerSnapDistance.atMost(1)
                )
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
                        modifier = Modifier.fillMaxWidth(),
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
                            DayOfWeekLabels()
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
                                    isCurrentMonth = date.date.month == monthData.visibleDates[15].date.month
                                    CalendarItem(date, onDateClickListener, true, isCurrentMonth)
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
                    modifier = Modifier.fillMaxWidth(),
                    beyondBoundsPageCount = 1,
                    flingBehavior = PagerDefaults.flingBehavior(
                        state = weekState,
                        pagerSnapDistance = PagerSnapDistance.atMost(1)
                    )
                ) { pageIndex ->
                    val weekToShow = startWeek.plusWeeks(pageIndex.toLong())
                    val visibleDates = dataSource.getDatesBetween(
                        weekToShow,
                        weekToShow.plusDays(6),
                        false
                    )
                    val weekData = dataSource.toUiModel(visibleDates, dataSource.today)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Top
                    ) {
                        DayOfWeekLabels()
                        if (weekData.visibleDates.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp)
                            ) {
                                items(weekData.visibleDates) { date ->
                                    CalendarItem(date, onDateClickListener, false, isCurrentMonth) // TODO: Fix isSelected not updating on click
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    @Composable
    fun DayOfWeekLabels() {
        Row(
            modifier = Modifier
                .padding(start = 14.dp, end = 14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dow = DayOfWeek.entries
            val daysOfWeek = dow.slice(6..6) + dow.slice(0..5)
            daysOfWeek.forEach { dayOfWeek ->
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
    @Composable
    fun Calendar(
        context: Context,
        showOptimizedEvents: MutableState<Boolean>
    ) {
        val dataSource = CalendarDataSource()
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
        var isAiCompleted by remember { mutableStateOf(true) } // This is for the loading spinner when the AI is optimizing the schedule
        var isAiSuccessful by remember { mutableStateOf(false) }
        var showAiEvents by remember { mutableStateOf(false) } // THis is for the AI event toggle button
        val date = calendarUiModel.selectedDate
        val user = FirebaseLoginHelper().getCurrentUser()

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
                    //.weight(if (isMonthView) 1.2f else 0.3f)
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
                                user!!.uid,
                                user.metadata!!.creationTimestamp,
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
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                EventView(navController = navController).EventCard(
                    // isAiCompleted is used improperly. It is used to show the loading spinner when the AI is optimizing the schedule
                    // If the AI runs and fails, then isAiCompleted will be false and the loading spinner will show
                    calendarUiModel,
                    {
                        CoroutineScope(Dispatchers.Main).launch  {
                            Toast.makeText(context, "Optimizing Schedule", Toast.LENGTH_SHORT).show()
                            isAiCompleted = false // Set to false to show loading spinner
                            isAiSuccessful = HomeActivity().optimizeScheduleForToday(user!!.uid, date.date.atStartOfDay())
                            isAiCompleted = true // Set to true to show events
                            if (isAiSuccessful) {
                                Toast.makeText(context, "AI Optimization Completed", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "AI Optimization Failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    isLoadingEvents,
                    isAiCompleted,
                    showOptimizedEvents
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
                                calendarUiModel.selectedDate.hasEvents = events.isNotEmpty()
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
    }
}

