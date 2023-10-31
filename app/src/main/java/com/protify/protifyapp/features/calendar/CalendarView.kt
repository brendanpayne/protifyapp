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
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.protify.protifyapp.utils.DateUtils

class CalendarView {
    private val dateUtils = DateUtils()
    private var currentDate: String = dateUtils.formatDate(dateUtils.getCurrentDate())
    @Composable
    fun CalendarHeader(data: CalendarUiModel) {
        Row{
            Text(
                text = if (data.selectedDate.isToday) "Today" else data.selectedDate.date.toString(),
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
                onClick = { TODO("Cycle through previous dates.") }
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowLeft,
                    contentDescription = "Left Arrow"
                )
            }
            IconButton(
                onClick = { TODO("Cycle through future dates.") }
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
    fun CalendarItem(date: CalendarUiModel.Date) {
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
                    // TODO: When Clicking on the Date, highlight the date and show the events for that date
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
    fun  CalendarContent(data: CalendarUiModel) {
        LazyRow {
            items(items = data.visibleDates) { date ->
                CalendarItem(date)
            }
        }
    }

    @Composable
    fun Calendar() {
        val dataSource = CalendarDataSource()
        val calendarUiModel = dataSource.getData(lastSelectedDate = dataSource.today)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(
                    color = Color(0xFFF5F5F5)
                )
        ) {
            Column (
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CalendarHeader(data = calendarUiModel)
                CalendarContent(data = calendarUiModel)
            }
        }
    }
}