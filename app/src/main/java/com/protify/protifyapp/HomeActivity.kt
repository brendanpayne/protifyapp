package com.protify.protifyapp

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.protify.protifyapp.features.calendar.CalendarView
import com.protify.protifyapp.features.login.FirebaseLoginHelper
import com.protify.protifyapp.ui.theme.ProtifyTheme
import kotlinx.coroutines.launch


class HomeActivity {

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    fun HomePage(navController: NavHostController, navigateToAddEvent: () -> Unit) {
        val scaffoldState = rememberScaffoldState()
        val scope = rememberCoroutineScope()
        val firestoreHelper = FirestoreHelper()
        val user = FirebaseLoginHelper().getCurrentUser()
        val context = LocalContext.current

        Scaffold(
            scaffoldState = scaffoldState,
            drawerContent = {
                Text(
                    "Settings",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onSurface,
                    textAlign = TextAlign.Start
                )
                Divider()
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
                        Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                "Profile",
                                modifier = Modifier.padding(top = 10.dp).clickable { navController.navigate("profile") },
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colors.onSurface
                            )
                        Spacer(modifier = Modifier.height(32.dp))
                            Divider()
                        Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                "Privacy & Location",
                                modifier = Modifier.padding(top = 10.dp).clickable { navController.navigate("privacyLocation") },
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colors.onSurface
                            )
                        Spacer(modifier = Modifier.height(32.dp))
                            Divider()
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            "Recipe Generator",
                            modifier = Modifier.padding(top = 10.dp).clickable { navController.navigate("recipeGenerator") },
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colors.onSurface
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Divider()
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = { navController.navigate("login") },
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
                Box(modifier = Modifier.fillMaxSize(),contentAlignment = Alignment.TopEnd)  {

                    Column {
                        // TODO: Add a greeting based on the time of day and logged in user.
                        val greeting = "Good Morning, ${user?.email}!"
                        Text(
                            text = greeting,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.h6
                        )
                        CalendarView().Calendar(navigateToAddEvent)
                    }

                    SettingsIconButton(onClick = { scope.launch { scaffoldState.drawerState.open() } })

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

        )
    }


    @Composable
    fun SettingsIconButton(onClick: () -> Unit) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
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
}

