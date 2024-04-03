package com.protify.protifyapp

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity {
    val user = FirebaseAuth.getInstance().currentUser
    var name by mutableStateOf(user?.displayName.takeIf { it?.isNotEmpty() == true } ?: "Enter your Display Name") // Mutable state to recompose when the name changes
    val email = user?.email ?: "Unknown"


    @Composable
    fun BackButton(navController: NavController) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Go back")
        }
    }

    @Composable
    fun ProfilePage(navController: NavController) {
        var homeAddress by remember { mutableStateOf("Loading...") } // Mutable state to recompose when the home address changes
        var newName by remember { mutableStateOf(name) }
        var newHomeAddress by remember { mutableStateOf("Loading...") }
        var use4 by remember { mutableStateOf(false) }
        var newUse4 by remember { mutableStateOf(false) }
        var context = LocalContext.current // Context for showing toasts
        // Fetch the user's home address from Firestore async so it doesn't block the loading of the page
        LaunchedEffect(key1 = user) {// This should only run once because user is a stable reference
            val fetchedUserData = FirestoreHelper().getUserProfileInfo(user!!.uid)
                homeAddress = fetchedUserData.first
                use4 = fetchedUserData.second
                newHomeAddress = homeAddress
                newUse4 = use4

        }
        // Maps API for autocomplete on home address
        val mapsAPI = APIKeys().getMapsKey()
        Places.initialize(context, mapsAPI)
        val autocompleteLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = {
                if (it.resultCode == Activity.RESULT_OK) {
                    val place = Autocomplete.getPlaceFromIntent(it.data!!)
                    newHomeAddress = place.address ?: ""
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(navController)
                Spacer(modifier = Modifier.width(16.dp)) 
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.profile_picture),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(16.dp)
                )
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.padding(16.dp)
                )
                OutlinedTextField(
                    value = newHomeAddress,
                    onValueChange = { newHomeAddress = it },
                    label = { Text("Home Address") },
                    trailingIcon = {
                        androidx.compose.material3.IconButton(onClick = { newHomeAddress = "" }) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.outline,
                            )
                        }
                    },
                    modifier = Modifier
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                autocompleteLauncher.launch(
                                    Autocomplete
                                        .IntentBuilder(
                                            AutocompleteActivityMode.OVERLAY,
                                            listOf(
                                                Place.Field.ID,
                                                Place.Field.NAME,
                                                Place.Field.ADDRESS
                                            )
                                        )
                                        .build(context)
                                )
                            }
                        }
                        .padding(16.dp),
                    readOnly = true
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Use GPT-4",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp)) // Add some space between the text and the checkbox
                    androidx.compose.material3.Checkbox(
                        checked = newUse4,
                        onCheckedChange = { newUse4 = it }
                    )
                }
                Text(
                    text = email,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(16.dp)
                )
                if (newHomeAddress != homeAddress || newName != name || newUse4 != use4) { // Show save button only if there are changes
                     Button(
                        onClick = {
                            if (newName != name) { // Update display name if it's different
                                user?.updateProfile(
                                    UserProfileChangeRequest.Builder()
                                        .setDisplayName(newName)
                                        .build()
                                )?.addOnCompleteListener { updateDisplayName ->
                                    if (updateDisplayName.isSuccessful) {
                                        name = newName
                                        Toast.makeText(
                                            context,
                                            "Display name updated successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Failed to update display name",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                            CoroutineScope(Dispatchers.IO).launch {
                                // If both the home address and the use4 value have changed, update both in Firestore
                                if (newHomeAddress != homeAddress && newUse4 != use4) {
                                    FirestoreHelper().setUserProfileInfo(
                                        user!!.uid,
                                        newHomeAddress,
                                        newUse4
                                    )
                                    homeAddress = newHomeAddress
                                    use4 = newUse4
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Profile updated successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else if (newHomeAddress != homeAddress) { // Update home address if it's different
                                    FirestoreHelper().setUserProfileInfo(
                                        user!!.uid,
                                        newHomeAddress,
                                        null
                                    )
                                    homeAddress = newHomeAddress
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Home Address updated successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else if (newUse4 != use4) { // Update use4 value if it's different
                                    FirestoreHelper().setUserProfileInfo(
                                        user!!.uid,
                                        null,
                                        newUse4
                                    )
                                    use4 = newUse4
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "AI Preference updated successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                    },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}