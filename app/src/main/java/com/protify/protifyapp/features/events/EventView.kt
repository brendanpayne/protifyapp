package com.protify.protifyapp.features.events

import android.widget.Toast
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.protify.protifyapp.R
import com.protify.protifyapp.features.calendar.CalendarUiModel
import com.protify.protifyapp.features.calendar.Event
import com.protify.protifyapp.features.calendar.EventBreakdown
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate


class EventView (private val navController: NavController) {
    @Composable
    fun EventHeader() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            content = {
                Text(
                    text = "Events",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = {
                        navController.navigate("addEvent")
                    },
                    modifier = Modifier.padding(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Event",
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        )
    }

    @Composable
    fun LoadingText() {
        val scope = rememberCoroutineScope()
        val periodCount = remember { mutableIntStateOf(0) }

        LaunchedEffect(key1 = periodCount) {
            scope.launch {
                while (true) {
                    delay(1000L)
                    periodCount.intValue = (periodCount.intValue + 1) % 3
                }
            }
        }

        val animatedPeriodCount by animateIntAsState(targetValue = periodCount.intValue, label = "")

        Text(
            text = "Loading events." + ".".repeat(animatedPeriodCount),
            modifier = Modifier
                .padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }

    @Composable
    fun EventList(
        data: CalendarUiModel,
        isLoadingEvents: Boolean,
        isOptimizingEvents: Boolean,
        optimizeEventClickListener: () -> Unit,
        showOptimizedEvents: MutableState<Boolean>
    ) {
        if (isLoadingEvents) {
            Row(
                modifier = Modifier
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                LoadingText()
            }
        } else {
            if (isOptimizingEvents) {
                Column {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(16.dp)
                            .size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "Optimizing events...",
                        modifier = Modifier
                            .padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                if (data.selectedDate.hasEvents) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(1f)
                    ) {
                        EventBreakdown().DailySchedule(
                            scale = 1.0,
                            eventList = data.selectedDate.events,
                            date = data.selectedDate.date,
                            navController = navController,
                            showOptimizedEvents = showOptimizedEvents
                        )
                        if (data.selectedDate.date.isBefore(LocalDate.now())) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .align(Alignment.BottomCenter)
                            ) {
                                OptimizeButton(optimizeEventClickListener)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No events for this day!",
                            modifier = Modifier
                                .padding(16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun OptimizeButton(optimizeEventClickListener: () -> Unit) {
        ElevatedButton(
            onClick = optimizeEventClickListener,
            elevation = ButtonDefaults.buttonElevation(8.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Optimize Events",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.outline_auto_fix_high_24),
                        contentDescription = "Optimize Events",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun EventCard(
        data: CalendarUiModel,
        optimizeEventClickListener: () -> Unit,
        isLoadingEvents: Boolean,
        isOptimizingEvents: Boolean,
        showOptimizedEvents: MutableState<Boolean>
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                EventHeader()
                EventList(data, isLoadingEvents, isOptimizingEvents, optimizeEventClickListener, showOptimizedEvents)
            }
        }
    }
}