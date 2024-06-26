package com.protify.protifyapp.utils.OpenAIHelper

import OpenAIResponse
import com.google.gson.GsonBuilder
import com.protify.protifyapp.APIKeys
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

class Recipe {

    // Get API Key
    val apiKey = APIKeys().getOpenAIKey()

    //Request struct
    data class Request(
        val model: String,
        val response_format: String,
        var systemContent: String,
        var userContent: String
    )

    // Diet struct
    enum class Diet(val dietString: String) {
        Balanced(""),
        HighProtein("high-protein"),
        LowCarb("low-carb"),
        LowFat("low-fat"),
        Paleo("paleo"),
        Vegan("vegan"),
        Vegetarian("vegetarian"),
        Whole30("whole30"),
        Ketogenic("keto")
    }
    private val model = "gpt-3.5-turbo-0125"
    //GPT 4 goated. GPT 3.5 is not reorganizing the shcedule properly, but GPT 4 is
    //private val model = "gpt-4-0125-preview"
    private val response_format = """{ "type": "json_object" }"""

    private val client: OkHttpClient = OkHttpClient().newBuilder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val gson = GsonBuilder().create()

    private val request = okhttp3.Request.Builder()
        .url("https://api.openai.com/v1/chat/completions")
        .addHeader("Content-Type", "application/json")
        .addHeader("Authorization", apiKey)
        .build()

    /**
     * Get a recipe based on the diet, time, ingredients, and optionally, exclude ingredients
     * @param diet The diet type (Diet enum)
     * @param time The time to make the recipe in minutes
     * @param ingredients The ingredients to include in the recipe
     * @param excludeIngredients The ingredients to exclude from the recipe
     * @return Returns a string hopefully in the format of a RecipeResponse that needs to be serialized
     */
    fun getRecipe(diet: Diet, time: Int, ingredients: List<String>, excludeIngredients: List<String>? = null, callback: (String) -> Unit) {

        // Convert ingredients to string
        val ingredientsString = ingredients.joinToString(", ")
        // Convert excludeIngredients to string if not null
        val excludeIngredientsString = excludeIngredients?.joinToString(", ") ?: ""

        // Build system content
        val systemContent = "Please give me a recipe that only includes the ingredients given to you and honors the type" +
                " of diet specified. You must give me a recipe that I can make within the given time frame." +
                " You must output in in the following json format. One object will be called ingredients and" +
                " you will list the ingredients in a list with their measurements in metric." +
                " The other object will be called instructions and you will" +
                " list the instructions in a numbered list." +
                " Another object will be called required_time and you will list the time in minutes." +
                " lastly, you will have an object called recipe_name and you will provide the name of the recipe."

        // Build user content
        val userContent = "I want a ${diet.dietString} recipe that includes $ingredientsString ${if (excludeIngredientsString.isNotEmpty()) "but excludes $excludeIngredientsString" else ""} and takes $time minutes to make."

        // Create request
        val request = Request(model, response_format, systemContent, userContent)

        // Make the request
        makeRequest(request, callback)


    }

    private fun makeRequest(httpPost: Request, onComplete: (String) -> Unit) {

        val requestBody = request.newBuilder()
            .post(
                """{
    "model": "${httpPost.model}",
    "response_format": ${httpPost.response_format},
    "messages": [
      {
        "role": "system",
        "content": "${httpPost.systemContent}"
      },
      {
        "role": "user",
        "content": "${httpPost.userContent}"
      }
    ]
  }""".trimIndent().toRequestBody()
            )
            .build()

        //Make the request
        client.newCall(requestBody).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                onComplete("Error")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string()
                if (response.code == 200) {
                    val openAIResponse = gson.fromJson(body, OpenAIResponse::class.java)
                    onComplete(openAIResponse.choices[0].message.content)
                }
                else {
                    onComplete("Error")
                }
            }
        })

    }
}