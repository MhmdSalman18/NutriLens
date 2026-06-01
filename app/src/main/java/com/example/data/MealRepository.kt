package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class MealRepository(
    private val context: Context,
    private val mealScanDao: MealScanDao
) {
    val allMeals: Flow<List<MealScan>> = mealScanDao.getAllMeals()

    suspend fun insert(meal: MealScan): Long = withContext(Dispatchers.IO) {
        mealScanDao.insertMeal(meal)
    }

    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        mealScanDao.deleteMealById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        mealScanDao.clearAllMeals()
    }

    /**
     * Copies an image from a URI to the app's internal filesystem.
     * This makes it permanent for offline viewing in the saved history list.
     */
    suspend fun saveImageLocally(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val file = File(context.filesDir, "scan_${UUID.randomUUID()}.jpg")
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                file.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Compresses and saves a Bitmap to local files.
     */
    suspend fun saveBitmapLocally(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, "scan_${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Loads a drawable resource to a Bitmap.
     */
    suspend fun loadBitmapFromResource(resourceId: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            BitmapFactory.decodeResource(context.resources, resourceId)
        } catch (e: Exception) {
            null
        }
    }
}
