package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MealScanDao {
    @Query("SELECT * FROM meal_scans ORDER BY timestamp DESC")
    fun getAllMeals(): Flow<List<MealScan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealScan): Long

    @Query("DELETE FROM meal_scans WHERE id = :id")
    suspend fun deleteMealById(id: Int)

    @Query("DELETE FROM meal_scans")
    suspend fun clearAllMeals()
}
