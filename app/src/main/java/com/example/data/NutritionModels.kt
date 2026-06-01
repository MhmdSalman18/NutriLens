package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiTotalMacros(
    @Json(name = "protein_g") val proteinG: Double,
    @Json(name = "carbs_g") val carbsG: Double,
    @Json(name = "fat_g") val fatG: Double,
    @Json(name = "fiber_g") val fiberG: Double
)

@JsonClass(generateAdapter = true)
data class GeminiFoodItem(
    val name: String,
    @Json(name = "portion_g") val portionG: Double,
    val calories: Double,
    @Json(name = "protein_g") val proteinG: Double,
    @Json(name = "carbs_g") val carbsG: Double,
    @Json(name = "fat_g") val fatG: Double,
    val confidence: String // "high", "medium", "low"
)

@JsonClass(generateAdapter = true)
data class GeminiNutritionResponse(
    @Json(name = "meal_name") val mealName: String? = null,
    @Json(name = "total_calories") val totalCalories: Double? = null,
    @Json(name = "total_macros") val totalMacros: GeminiTotalMacros? = null,
    val items: List<GeminiFoodItem>? = null,
    @Json(name = "health_score") val healthScore: Double? = null,
    @Json(name = "health_note") val healthNote: String? = null,
    val warnings: List<String>? = null,
    val error: String? = null
)
