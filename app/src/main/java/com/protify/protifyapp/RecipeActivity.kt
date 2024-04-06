package com.protify.protifyapp

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.protify.protifyapp.utils.OpenAIHelper.Recipe
import com.protify.protifyapp.utils.OpenAIHelper.RecipeResponse

class RecipeActivity {
    @Composable
    fun RecipePage(navController: NavController) {
        var expanded by remember { mutableStateOf(false) }
        var selectedDiet by remember { mutableStateOf("Select Diet") }
        var ingredients by remember { mutableStateOf("") }
        var excludeIngredients by remember { mutableStateOf("") }
        var time by remember { mutableStateOf("") }
        var recipeResponse by remember { mutableStateOf(RecipeResponse("", 0, mapOf(), listOf())) }
        val context = LocalContext.current
        // Check for at least two ingredients
        val minIngredients = 2
        // Check for greater than 5 minutes
        val minTime = 5

        val diets = Recipe.Diet.entries.map { it } // Diet enum

        var showDialog by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxSize()) {
            BackButton(navController)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Recipe Generator",
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                OutlinedTextField(
                    value = selectedDiet,
                    onValueChange = { selectedDiet = it },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Open dropdown")
                        }
                    },
                    isError = selectedDiet == "Select Diet",

                    )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .width(200.dp)
                        .height(300.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    diets.forEach { diet ->
                        DropdownMenuItem(onClick = {
                            selectedDiet = diet.toString()
                            expanded = false
                        }) {
                            Text(text = diet.toString())
                        }
                    }
                }
                OutlinedTextField(
                    value = ingredients,
                    onValueChange = { ingredients = it },
                    label = { Text("Ingredients") },
                    modifier = Modifier.padding(top = 16.dp),
                    isError = ingredients.split(",").size < minIngredients
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
                    modifier = Modifier.padding(top = 16.dp),
                    isError = (time.toIntOrNull() ?: 0) < minTime
                )
                // Submit button
                Button(
                    onClick = {
                        // Check if all fields are filled
                        if (selectedDiet == "Select Diet" || ingredients.split(",").size < minIngredients || (time.toIntOrNull()
                                ?: 0) < minTime
                        ) {
                            Toast.makeText(
                                context,
                                "Please fill all required fields",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        isLoading = true

                        Recipe().getRecipe(
                            Recipe.Diet.valueOf(selectedDiet),
                            time.toInt(),
                            ingredients.split(","),
                            excludeIngredients.split(",")
                        ) { response ->
                            isLoading = false
                            try {
                                recipeResponse = Gson().fromJson(
                                    response,
                                    RecipeResponse::class.java
                                ) // Parse response
                                showDialog = true
                            } catch (e: Exception) {
                                // Handle error
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 32.dp)
                ) {
                    Text("Generate Recipe")
                }
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 32.dp))
                }

                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text(text = recipeResponse.recipeName) },
                        text = {
                            Column {
                                Text("Time required: ${recipeResponse.requiredTime} minutes")
                                recipeResponse.ingredients.forEach { (ingredient, measurement) ->
                                    Text("$ingredient: $measurement")
                                }
                                recipeResponse.instructions.forEachIndexed { index, instruction ->
                                    Text("${index + 1}. $instruction")
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = { showDialog = false }) {
                                Text("OK")
                            }
                        }
                    )
                }
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

