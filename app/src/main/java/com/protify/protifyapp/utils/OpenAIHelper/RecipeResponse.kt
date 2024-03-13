package com.protify.protifyapp.utils.OpenAIHelper

import com.google.gson.annotations.SerializedName

class RecipeResponse(
    @SerializedName ("recipe_name")
    val recipeName: String,
    @SerializedName ("required_time")
    val requiredTime: Int,
    @SerializedName ("ingredients")
    val ingredients: Map<String, String>,
    @SerializedName ("instructions")
    val instructions: List<String>
) {

    // Null check
    fun isNull(): Boolean {
        return recipeName.isEmpty() || requiredTime == 0 || ingredients.isEmpty() || instructions.isEmpty()
    }

}