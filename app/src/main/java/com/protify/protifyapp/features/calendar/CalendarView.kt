package com.protify.protifyapp.features.calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.protify.protifyapp.utils.DateUtils

class CalendarView {
    private val dateUtils = DateUtils()
    private var currentDate: String = dateUtils.formatDate(dateUtils.getCurrentDate())
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
        Column {
            Text(
                text = day,
                modifier = Modifier
                    .padding(vertical = 4.dp, horizontal = 4.dp)
                    .align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
            )
            Card(
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(
                    modifier = Modifier
                        .width(40.dp)
                        .height(40.dp)
                        .padding(10.dp)
                ) {
                    Text(
                        text = date,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    @Composable
    fun  CalendarContent() {
        LazyRow {
            items(items = List(7) { Pair("S", "1") }) { date ->
                CalendarItem(date.first, date.second)
            }
        }
    }

    @Composable
    fun Calendar() {
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
                Header()
                CalendarContent()
            }
        }
    }
}