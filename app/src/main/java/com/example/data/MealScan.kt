package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_scans")
data class MealScan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mealName: String,
    val totalCalories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val healthScore: Int,
    val healthNote: String,
    val warningsJson: String, // Moshi string of list
    val itemsJson: String,    // Moshi string of food items list
    val timestamp: Long = System.currentTimeMillis(),
    val localImagePath: String? = null // Copied image path for offline history
)
