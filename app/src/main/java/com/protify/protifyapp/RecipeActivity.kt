package com.protify.protifyapp

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
        var ingredients = remember { mutableStateOf(listOf<String>()) }
        var excludeIngredients = remember { mutableStateOf(listOf<String>()) }
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
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Recipe Generator",
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                Box {// Wrap text field and dropdown in a box to allow for dropdown to be displayed over text field
                    OutlinedTextField(
                        value = selectedDiet,
                        onValueChange = { selectedDiet = it },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Open dropdown")
                            }
                        },
                        isError = selectedDiet == "Select Diet"
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .width(200.dp)
                            .height(300.dp)
                            .align(Alignment.BottomStart)
                    ) {
                        diets.forEach { diet ->
                            DropdownMenuItem(
                                text = {Text(text = diet.toString())},
                                onClick = {
                                    selectedDiet = diet.toString()
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                if (ingredients.value.isEmpty()) { // Add the first ingredient
                    ingredients.value = listOf("")
                }
                ingredients.value.forEachIndexed { index, ingredient ->
                    OutlinedTextField(
                        value = ingredient,
                        onValueChange = { newValue ->
                            ingredients.value = ingredients.value.toMutableList().apply {
                                if (newValue.isEmpty() && this.size > 1) {
                                    removeAt(index)
                                } else {
                                    this[index] = newValue
                                }
                            }
                        },
                        label = { Text("Ingredients") },
                        modifier = Modifier.padding(top = 16.dp),
                        isError = ingredient.isEmpty() && ingredients.value.size == 1
                    )
                    // Add a new ingredient field if the last field is filled
                    if (index == ingredients.value.size - 1 && ingredient.isNotEmpty()) {
                        ingredients.value = ingredients.value.toMutableList().apply {
                            add("")
                        }
                    }
                }
                if (excludeIngredients.value.isEmpty()) { // Add the first exclude ingredient
                    excludeIngredients.value = listOf("")
                }
                excludeIngredients.value.forEachIndexed { index, excludeIngredient ->
                    OutlinedTextField(
                        value = excludeIngredient,
                        onValueChange = { newValue ->
                            excludeIngredients.value = excludeIngredients.value.toMutableList().apply {
                                if (newValue.isEmpty() && this.size > 1) {
                                    removeAt(index)
                                } else {
                                    this[index] = newValue
                                }
                            }
                        },
                        label = { Text("Exclude Ingredients") },
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    // Add a new exclude ingredient field if the last field is filled
                    if (index == excludeIngredients.value.size - 1 && excludeIngredient.isNotEmpty()) {
                        excludeIngredients.value = excludeIngredients.value.toMutableList().apply {
                            add("")
                        }
                    }
                }
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
                        if (selectedDiet == "Select Diet" || ingredients.value.size < minIngredients || (time.toIntOrNull()
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
                            ingredients.value,
                            excludeIngredients.value
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

