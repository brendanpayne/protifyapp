package com.protify.protifyapp.features.calendar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.protify.protifyapp.utils.EventUtils

class CalendarView {
    private val eventUtils = EventUtils()
    private var currentDate: String = eventUtils.parseCurrentDate()
    @Composable
    fun Header() {
        Row{
            Text(
                text = currentDate,
                modifier = Modifier
                    .padding(16.dp)
                    .clickable(onClick = { /*TODO: When Clicking on the Date do something*/ })
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            )
            IconButton(
                onClick = { /*TODO: When Clicking on the Left Arrow do something*/ }
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowLeft,
                    contentDescription = "Left Arrow"
                )
            }
            IconButton(
                onClick = { /*TODO: When Clicking on the Right Arrow do something*/ }
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowRight,
                    contentDescription = "Right Arrow"
                )
            }
        }
    }

    @Composable
    fun CalendarItem(day: String, date: String) {
        Card(
            modifier = Modifier
                .padding(vertical = 4.dp, horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(40.dp)
                    .height(48.dp)
                    .padding(4.dp)
            ) {
                Text(
                    text = day,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.body1
                )
                Text(
                    text = date,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.body2
                )
            }
        }
    }

    @Composable
    fun  CalendarContent() {
        LazyRow {
            items(items = List(7) { Pair("Sun", "1") }) { date ->
                CalendarItem(date.first, date.second)
            }
        }
    }
}