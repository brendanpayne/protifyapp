package com.protify.protifyapp.tests.features.AITesting

import com.google.gson.Gson
import com.protify.protifyapp.utils.OpenAIHelper.Recipe
import com.protify.protifyapp.utils.OpenAIHelper.RecipeResponse
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch

class RecipeTest {

    // Test Recipe
    val recipe = Recipe()
    val diet = Recipe.Diet.Balanced
    val time = 30
    val ingredients = listOf("chicken", "rice", "broccoli")
    val excludeIngredients = listOf("peanuts")

    // Init gson
    val gson = Gson()
    @Before
    fun `Valid AI Response`() {
        // init recipeString
        var recipeString = ""
        // init countDownLatch
        val countDownLatch = CountDownLatch(1)
        recipe.getRecipe(diet, time, ingredients, excludeIngredients) {
            recipeString = it
            countDownLatch.countDown() // Decrement the count
        }
        // Wait 30 seconds for async call to complete
        countDownLatch.await(30, java.util.concurrent.TimeUnit.SECONDS)

        // Assert that the recipe is not empty
        assert(recipeString != "Error") { "Invalid response" }
    }

    @Test
    fun `Serialize Response`() {

        // Init countDownLatch
        val countDownLatch = CountDownLatch(1)
        // Init recipeString
        var recipeString = ""
        recipe.getRecipe(diet, time, ingredients, excludeIngredients) {
            recipeString = it
            countDownLatch.countDown() // Decrement the count
        }
        // Wait 30 seconds for async call to complete
        countDownLatch.await(30, java.util.concurrent.TimeUnit.SECONDS)

        // Serialize the response
        try {
            val recipeResponse = gson.fromJson(recipeString, RecipeResponse::class.java)
            assert(!recipeResponse.isNull()) { "Recipe is null" }
        } catch (e: Exception) {
            assert(false) { "Failed to serialize response" }
        }
    }
}