package com.protify.protifyapp

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.protify.protifyapp.features.calendar.CalendarView
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import com.protify.protifyapp.ui.theme.ProtifyTheme
import com.protify.protifyapp.utils.OpenAIHelper.GetAISchedule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime


@Composable
fun GroupItem(navController: NavController, text: String, icon: ImageVector, route: String) {
    Spacer(modifier = Modifier.height(32.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = "$text Icon")
        Text(
            text,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
                .clickable { navController.navigate(route) },
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colors.onSurface
        )
    }
    Spacer(modifier = Modifier.height(32.dp))
    Divider()
}
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

        // Calculate the time of day
        val timeOfDay = when (java.time.LocalTime.now().hour) {
            in 6..11 -> TimeOfDay.MORNING
            in 12..16 -> TimeOfDay.AFTERNOON
            in 17..20 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }

        Scaffold(
            scaffoldState = scaffoldState,
            drawerContent = {
                Column(
                    modifier = Modifier.fillMaxSize(),
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
                                .background(MaterialTheme.colors.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Settings",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.h6,
                                color = Color.White,
                                textAlign = TextAlign.Start
                            )
                        }
                        Divider()
                        // Group related items
                        GroupItem(navController, "Profile", Icons.Filled.PlayArrow, "profile")
                        GroupItem(navController, "Privacy & Location", Icons.Filled.PlayArrow, "privacyLocation")
                        GroupItem(navController, "Recipe Generator",Icons.Filled.PlayArrow, "recipeGenerator")
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
                                color = Color.White
                            )
                        }
                        Divider()
                    }
                }
            },
            content = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column {
                        var greeting by remember { mutableStateOf(timeOfDay.displayName) }
                        val defaultName = "Guest"
                        val displayName = user?.displayName ?: defaultName
                        greeting = "${timeOfDay.displayName}, $displayName!"
                        if (user?.displayName != null || user?.displayName != "") {
                            greeting = "${timeOfDay.displayName}, ${user?.displayName}!"
                        }
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.CenterEnd) {
                            Text(
                                text = greeting,
                                style = MaterialTheme.typography.h6
                            )
                        }
                        CalendarView().Calendar(navigateToAddEvent)
                    }

                    SettingsIconButton(onClick = { scope.launch { scaffoldState.drawerState.open() } }, alignment = Alignment.TopStart)

                    val networkManager = NetworkManager(context)

                    val isConnected by remember { mutableStateOf(false) }
                    LaunchedEffect(networkManager) {
                        networkManager.startListening()
                    }
//                    LaunchedEffect(isConnected) {
//                        networkManager.setNetworkChangeListener {
//                            if (it) {
//                                firestoreHelper.toggleOfflineOnline(true)
//                            } else {
//                                firestoreHelper.toggleOfflineOnline(false)
//                            }
//                        }
//                    }

                    // Optimize schedule for today in a new coroutine
//                    var isOptimizing by rememberSaveable { // Only allow optimization once (if a user navigates away, this will remember)
//                        mutableStateOf(false)
//                    }
//                    LaunchedEffect(user) {
//                        user?.let {
//                            if (isOptimizing) {
//                                Toast.makeText(context, "Optimizing schedule for today...", Toast.LENGTH_SHORT).show() // Runs when a user navigates away and back
//                                return@LaunchedEffect
//                            }
//                                isOptimizing = true
//                                scaffoldState.snackbarHostState.showSnackbar("Optimizing schedule for today...")
//                                val result = optimizeScheduleForToday(it.uid, context)
//                                isOptimizing = false
//                                if (result) {
//                                    scaffoldState.snackbarHostState.showSnackbar("Optimized schedule for today!")
//                                } else {
//                                    scaffoldState.snackbarHostState.showSnackbar("No optimization needed for today.")
//
//                                }
//                        }
//                    }
                }
            }

        )
    }


    @Composable
    fun SettingsIconButton(onClick: () -> Unit, alignment: Alignment) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = alignment) {
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
     * @param context The context for showing toasts
     * @return True if the schedule was optimized successfully, false otherwise
     */
    suspend fun optimizeScheduleForToday(uid: String, today: LocalDateTime): Boolean {
        val result = CompletableDeferred<Boolean>()
        CoroutineScope(Dispatchers.IO).launch {
            // Get user's home address
            val homeAddress = FirestoreHelper().getUserHomeAddress(uid)
            if (homeAddress == "" || homeAddress == "No home address found") {
                result.complete(false)
                return@launch
            }
            // Optimize schedule for today
            GetAISchedule(uid, homeAddress).getOptimizedSchedule(true, today) { success -> // For now, do one day in advance
                result.complete(success)
            }
        }
        return result.await()
    }
}

