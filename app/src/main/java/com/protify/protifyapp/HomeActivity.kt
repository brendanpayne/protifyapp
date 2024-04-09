package com.protify.protifyapp

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalDrawer
import androidx.compose.material.ScaffoldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.Surface
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.MapsInitializer
import com.google.firebase.auth.FirebaseUser
import com.protify.protifyapp.features.calendar.CalendarView
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import com.protify.protifyapp.ui.theme.ProtifyTheme
import com.protify.protifyapp.utils.OpenAIHelper.GetAISchedule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
class HomeActivity {
    enum class TimeOfDay(val displayName: String) {
        MORNING("Good Morning"),
        AFTERNOON("Good Afternoon"),
        EVENING("Good Evening"),
        NIGHT("Good Night")
    }

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    fun HomePage(navController: NavHostController, navigateToAddEvent: () -> Unit) {
        val scaffoldState = rememberScaffoldState()
        val scope = rememberCoroutineScope()
        val firestoreHelper = FirestoreHelper()
        val user = FirebaseLoginHelper().getCurrentUser()
        val context = LocalContext.current
        val showOptimizedEvents = remember { mutableStateOf(true) }

        // Initialize Google Maps
        MapsInitializer.initialize(context)

        // Calculate the time of day
        val timeOfDay = when (java.time.LocalTime.now().hour) {
            in 6..11 -> TimeOfDay.MORNING
            in 12..16 -> TimeOfDay.AFTERNOON
            in 17..20 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            ModalDrawer(
                drawerState = scaffoldState.drawerState,
                drawerContent = { DrawerContent(navController, showOptimizedEvents, scaffoldState, scope) },
                content = { HomeContent(timeOfDay, user, navController, navigateToAddEvent, scaffoldState, showOptimizedEvents) }
            )
        }
    }

    @Composable
    private fun DrawerContent(
        navController: NavHostController,
        showOptimizedEvents: MutableState<Boolean>,
        scaffoldState: ScaffoldState,
        scope: CoroutineScope
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            scope.launch {
                                scaffoldState.drawerState.close()
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        // This is a placeholder to push the title to the center
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
                // Group related items
                GroupItem(navController, "Profile", Icons.Filled.PlayArrow, "profile")
                GroupItem(navController, "Privacy & Location", Icons.Filled.PlayArrow, "privacyLocation")
                GroupItem(navController, "Recipe Generator",Icons.Filled.PlayArrow, "recipeGenerator")
                GroupItem(navController, "About", Icons.Filled.PlayArrow, "about")
                OptimizeSlider(showOptimizedEvents)
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { navController.navigate("main") },
                    modifier = Modifier
                        .padding(bottom = 48.dp)
                        .width(200.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Logout",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

    }

    @Composable
    fun OptimizeSlider(showOptimizedEvents: MutableState<Boolean>){
        Column{
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Show AI Optimized Events",
                    style = if (!showOptimizedEvents.value)
                        MaterialTheme.typography.bodyMedium else
                            MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = showOptimizedEvents.value,
                    onCheckedChange = { showOptimizedEvents.value = it }
                )
            }
        }
    }

    @Composable
    private fun HomeContent(
        timeOfDay: TimeOfDay,
        user: FirebaseUser?,
        navController: NavHostController,
        navigateToAddEvent: () -> Unit,
        scaffoldState: ScaffoldState,
        showOptimizedEvents: MutableState<Boolean>
    ) {
        val scope = rememberCoroutineScope()
        val firestoreHelper = FirestoreHelper()
        val context = LocalContext.current
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
            Column {
                var greeting by remember { mutableStateOf(timeOfDay.displayName) }
                val defaultName = "Guest"
                greeting = if (user != null) {
                    if (user.displayName == null || user.displayName == "") {
                        "${timeOfDay.displayName}, $defaultName!"
                    } else {
                        "${timeOfDay.displayName}, ${user.displayName}!"
                    }
                } else {
                    "${timeOfDay.displayName}, $defaultName!"
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        SettingsIconButton(onClick = { scope.launch { scaffoldState.drawerState.open() } }, alignment = Alignment.TopStart)
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
                CalendarView(navController).Calendar(context, navigateToAddEvent, showOptimizedEvents)
            }

            val networkManager = NetworkManager(context)

            val isConnected by remember { mutableStateOf(false) }
            LaunchedEffect(networkManager) {
                networkManager.startListening()
            }
            LaunchedEffect(isConnected) {
                networkManager.setNetworkChangeListener {
                    if (it) {
                        firestoreHelper.toggleOfflineOnline(true)
                    } else {
                        firestoreHelper.toggleOfflineOnline(false)
                    }
                }
            }
        }
    }

    @Composable
    fun GroupItem(navController: NavController, text: String, icon: ImageVector, route: String) {
        Column (modifier = Modifier
            .clickable(onClick = { navController.navigate(route) })
            .height(84.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = "$text Icon")
                Text(
                    text,
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Divider()
    }

    @Composable
    fun SettingsIconButton(onClick: () -> Unit, alignment: Alignment) {
        Box(contentAlignment = alignment) {
            IconButton(onClick = onClick) {
                Icon(Icons.Filled.Menu, contentDescription = "Settings")
            }
        }
    }

    @Preview(showSystemUi = true)
    @Composable
    fun CalendarAppPreview() {
        val navController = rememberNavController()
        ProtifyTheme {
            HomePage(navController, navigateToAddEvent = {})
        }
    }

    fun navigateToAddEvent(navController: NavHostController) {
        navController.navigate("addEvent")
    }
    /** This function runs asynchronously to optimize the schedule for today
     * @param uid The user's unique identifier
     * @return True if the schedule was optimized successfully, false otherwise
     */
    suspend fun optimizeScheduleForToday(uid: String, today: LocalDateTime): Boolean {
        val result = CompletableDeferred<Boolean>()
        CoroutineScope(Dispatchers.IO).launch {
            // Get user's home address and AI preferences
            val profileInfo = FirestoreHelper().getUserProfileInfo(uid)
            if (profileInfo.first == "") {
                result.complete(false)
                return@launch
            }
            // Optimize schedule for today
            GetAISchedule(uid, profileInfo.first).getOptimizedSchedule(profileInfo.second, today) { success -> // If the user hasn't set their use4 preferences, it will use 3.5
                result.complete(success)
            }
        }
        return result.await()
    }
}

