package com.protify.protifyapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

class RecipeActivity {
    @Composable
    fun RecipePage(navController: NavController) {
        var expanded by remember { mutableStateOf(false) }
        var selectedDiet by remember { mutableStateOf("Select Diet") }
        var ingredients by remember { mutableStateOf("") }
        var excludeIngredients by remember { mutableStateOf("") }
        var time by remember { mutableStateOf("") }

        val diets = listOf("Keto", "Paleo", "Vegan", "Vegetarian", "Mediterranean")

        Box(modifier = Modifier.fillMaxSize()) {
            BackButton(navController)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = selectedDiet,
                    onValueChange = { selectedDiet = it },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Open dropdown")
                        }
                    }
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    diets.forEach { diet ->
                        DropdownMenuItem(onClick = {
                            selectedDiet = diet
                            expanded = false
                        }) {
                            Text(text = diet)
                        }
                    }
                }
                OutlinedTextField(
                    value = ingredients,
                    onValueChange = { ingredients = it },
                    label = { Text("Ingredients") },
                    modifier = Modifier.padding(top = 16.dp)
                )
                OutlinedTextField(
                    value = excludeIngredients,
                    onValueChange = { excludeIngredients = it },
                    label = { Text("Exclude Ingredients") },
                    modifier = Modifier.padding(top = 16.dp)
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time (in minutes)") },
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }

    @Composable
    fun BackButton(navController: NavController) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Go back")
            }
        }
    }
}

