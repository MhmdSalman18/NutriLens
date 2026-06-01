package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.GeminiFoodItem
import com.example.data.GeminiNutritionResponse
import com.example.data.GeminiTotalMacros
import com.example.data.MealRepository
import com.example.data.MealScan
import com.example.network.GeminiContent
import com.example.network.GeminiInlineData
import com.example.network.GeminiPart
import com.example.network.GeminiRequest
import com.example.network.GeminiResponseConfig
import com.example.network.GeminiRetrofitClient
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

sealed interface NutritionUiState {
    object Idle : NutritionUiState
    object Loading : NutritionUiState
    data class Success(val response: GeminiNutritionResponse, val bitmap: Bitmap?, val isSimulated: Boolean = false) : NutritionUiState
    data class Error(val message: String) : NutritionUiState
}

class NutritionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = MealRepository(application, db.mealScanDao())
    private val sharedPrefs = application.getSharedPreferences("nutrition_prefs", Context.MODE_PRIVATE)

    // History Log flow
    val mealHistory: StateFlow<List<MealScan>> = repository.allMeals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI Scanning States
    private val _uiState = MutableStateFlow<NutritionUiState>(NutritionUiState.Idle)
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()

    private val _selectedBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedBitmap: StateFlow<Bitmap?> = _selectedBitmap.asStateFlow()

    // Daily Goal targets
    private val _targetCalories = MutableStateFlow(2000.0)
    val targetCalories = _targetCalories.asStateFlow()

    private val _targetProtein = MutableStateFlow(120.0)
    val targetProtein = _targetProtein.asStateFlow()

    private val _targetCarbs = MutableStateFlow(230.0)
    val targetCarbs = _targetCarbs.asStateFlow()

    private val _targetFat = MutableStateFlow(65.0)
    val targetFat = _targetFat.asStateFlow()

    private val _targetFiber = MutableStateFlow(25.0)
    val targetFiber = _targetFiber.asStateFlow()

    // Water Tracking states
    private val _waterIntake = MutableStateFlow(0)
    val waterIntake = _waterIntake.asStateFlow()

    private val _targetWater = MutableStateFlow(2000)
    val targetWater = _targetWater.asStateFlow()

    private val moshi = GeminiRetrofitClient.moshi

    init {
        // Load target preferences
        _targetCalories.value = sharedPrefs.getFloat("target_calories", 2000f).toDouble()
        _targetProtein.value = sharedPrefs.getFloat("target_protein", 120f).toDouble()
        _targetCarbs.value = sharedPrefs.getFloat("target_carbs", 230f).toDouble()
        _targetFat.value = sharedPrefs.getFloat("target_fat", 65f).toDouble()
        _targetFiber.value = sharedPrefs.getFloat("target_fiber", 25f).toDouble()

        // Water tracking auto reset and load
        checkAndResetWaterIntake()
        _targetWater.value = sharedPrefs.getInt("target_water", 2000)
    }

    fun updateDailyTargets(calories: Double, protein: Double, carbs: Double, fat: Double, fiber: Double) {
        viewModelScope.launch {
            _targetCalories.value = calories
            _targetProtein.value = protein
            _targetCarbs.value = carbs
            _targetFat.value = fat
            _targetFiber.value = fiber

            sharedPrefs.edit().apply {
                putFloat("target_calories", calories.toFloat())
                putFloat("target_protein", protein.toFloat())
                putFloat("target_carbs", carbs.toFloat())
                putFloat("target_fat", fat.toFloat())
                putFloat("target_fiber", fiber.toFloat())
                apply()
            }
        }
    }

    fun setSelectedImage(bitmap: Bitmap?) {
        _selectedBitmap.value = bitmap
        if (bitmap == null) {
            _uiState.value = NutritionUiState.Idle
        }
    }

    /**
     * Set one of the high-quality preset food resource cards.
     */
    fun selectPresetMeal(resourceId: Int) {
        viewModelScope.launch {
            _uiState.value = NutritionUiState.Loading
            val bitmap = repository.loadBitmapFromResource(resourceId)
            if (bitmap != null) {
                _selectedBitmap.value = bitmap
                analyzeFoodImage(bitmap, resourceId)
            } else {
                _uiState.value = NutritionUiState.Error("Failed to load preset resource.")
            }
        }
    }

    /**
     * Base64 compression helper.
     */
    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val bytes = outputStream.toByteArray()
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }

    /**
     * Interacts with Gemini models via Direct REST endpoints.
     */
    fun analyzeFoodImage(bitmap: Bitmap, presetResId: Int? = null) {
        viewModelScope.launch {
            _uiState.value = NutritionUiState.Loading

            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                // Auto simulated demo fallback for a perfect out-of-the-box experience
                kotlinx.coroutines.delay(1200)
                val response = getSimulatedResponse(presetResId)
                _uiState.value = NutritionUiState.Success(response, bitmap, isSimulated = true)
                return@launch
            }

            try {
                // Convert bitmap to base64
                val base64Image = bitmap.toBase64()

                // Nutrition evaluation rules (as requested by user)
                val corePrompt = """
                    You are a professional nutritionist AI and food recognition expert. Your job is to analyze food images and return a highly accurate calorie and nutrition breakdown based strictly on USDA FoodData Central standards.

                    Estimation Logic & Hidden Calories:
                    1. Decompose the image and identify every ingredient, including base ingredients, toppings, spreads, and dressings.
                    2. **CRITICAL: Estimate hidden cooking oils, butter, sugars, and sauces.** These are the most common sources of hidden calories. Always assume standard cooking fats are present unless it is clearly raw or steamed.
                    3. Use these standard portion weight anchors for reference:
                       - Standard slice of bread: 35-40g
                       - Large egg: 50g
                       - Palm-sized chicken breast/meat fillet: 120-150g
                       - 1 tablespoon of cooking oil/butter: 14g
                       - Medium piece of fruit (apple, banana): 120-150g
                       - 1 cup of cooked rice/pasta: 150-180g
                    4. Check mathematical consistency: **Total Calories MUST strictly equal (Protein * 4) + (Carbs * 4) + (Fat * 9)**. Adjust individual item calories and totals to ensure this exact mathematical relationship is satisfied.

                    Return a structured JSON response with this exact schema:

                    {
                      "meal_name": "string — short descriptive name of the overall meal",
                      "total_calories": number,
                      "total_macros": {
                        "protein_g": number,
                        "carbs_g": number,
                        "fat_g": number,
                        "fiber_g": number
                      },
                      "items": [
                        {
                          "name": "string — food item name",
                          "portion_g": number,
                          "calories": number,
                          "protein_g": number,
                          "carbs_g": number,
                          "fat_g": number,
                          "confidence": "high | medium | low"
                        }
                      ],
                      "health_score": number (1–10, where 10 is most nutritious),
                      "health_note": "string — 1–2 sentence personalized tip about this meal",
                      "warnings": ["string"] or []
                    }

                    Rules:
                    - Always respond ONLY with valid JSON. No preamble, no markdown backticks.
                    - If you cannot identify a food item, include it with name "Unknown item" and confidence "low".
                    - Round all numbers to 1 decimal place.
                    - If the image contains no food, return: {"error": "No food detected in the image."}
                    - For mixed dishes (curry, stew, salad), break down into estimated main components.
                """.trimIndent()

                // Prepare standard Gemini content request body
                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(
                                GeminiPart(text = "Analyze this image and return the Nutrition JSON breakdown."),
                                GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Image))
                            )
                        )
                    ),
                    generationConfig = GeminiResponseConfig(
                        responseMimeType = "application/json",
                        temperature = 0.2
                    ),
                    systemInstruction = GeminiContent(
                        parts = listOf(GeminiPart(text = corePrompt))
                    )
                )

                // Network execution
                val rawResponse = GeminiRetrofitClient.service.generateContent(apiKey, request)

                // Parsing text output
                val rawText = rawResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (rawText != null) {
                    // Sanitize potential backticks
                    val sanitizedJson = rawText.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()

                    // Parse JSON with Moshi
                    val adapter = moshi.adapter(GeminiNutritionResponse::class.java)
                    val result = adapter.fromJson(sanitizedJson)

                    if (result != null) {
                        if (result.error != null) {
                            _uiState.value = NutritionUiState.Error(result.error)
                        } else {
                            _uiState.value = NutritionUiState.Success(result, bitmap)
                        }
                    } else {
                        _uiState.value = NutritionUiState.Error("Analyst output failed to parse correctly.")
                    }
                } else {
                    _uiState.value = NutritionUiState.Error("Nutritionist AI did not return a description. Please try again.")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = NutritionUiState.Error("Connection error: ${e.localizedMessage ?: "Unknown network failure"}")
            }
        }
    }

    /**
     * Logs the scanned meal to Local Room database history logs.
     */
    fun logAnalyzedMeal(response: GeminiNutritionResponse, bitmap: Bitmap?) {
        viewModelScope.launch {
            try {
                // Seralize lists to save in database columns
                val warningsAdapter = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
                val itemsAdapter = moshi.adapter<List<GeminiFoodItem>>(Types.newParameterizedType(List::class.java, GeminiFoodItem::class.java))

                val warningsStr = warningsAdapter.toJson(response.warnings ?: emptyList())
                val itemsStr = itemsAdapter.toJson(response.items ?: emptyList())

                // Optionally save bitmap copy to local file storage for reliable permanent access in lists
                val imagePath = if (bitmap != null) {
                    repository.saveBitmapLocally(bitmap)
                } else null

                val mealLog = MealScan(
                    mealName = response.mealName ?: "Healthy Scan",
                    totalCalories = response.totalCalories ?: 0.0,
                    protein = response.totalMacros?.proteinG ?: 0.0,
                    carbs = response.totalMacros?.carbsG ?: 0.0,
                    fat = response.totalMacros?.fatG ?: 0.0,
                    fiber = response.totalMacros?.fiberG ?: 0.0,
                    healthScore = (response.healthScore ?: 5.0).toInt(),
                    healthNote = response.healthNote ?: "",
                    warningsJson = warningsStr,
                    itemsJson = itemsStr,
                    localImagePath = imagePath
                )

                // Insert entity
                repository.insert(mealLog)

                // Clean visual state
                _uiState.value = NutritionUiState.Idle
                _selectedBitmap.value = null

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteLoggedMeal(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun clearLogHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun getSimulatedResponse(presetResId: Int?): GeminiNutritionResponse {
        return when (presetResId) {
            com.example.R.drawable.avocado_toast -> {
                GeminiNutritionResponse(
                    mealName = "Whole Wheat Sourdough Avocado Toast",
                    totalCalories = 290.0,
                    totalMacros = GeminiTotalMacros(proteinG = 8.5, carbsG = 28.0, fatG = 16.5, fiberG = 7.5),
                    items = listOf(
                        GeminiFoodItem(name = "Avocado (Fresh Hass)", portionG = 100.0, calories = 160.0, proteinG = 2.0, carbsG = 8.5, fatG = 14.5, confidence = "high"),
                        GeminiFoodItem(name = "Whole Wheat Sourdough (2 Slices)", portionG = 70.0, calories = 110.0, proteinG = 5.0, carbsG = 18.0, fatG = 1.0, confidence = "high"),
                        GeminiFoodItem(name = "Sesame Seeds Sprinkles", portionG = 5.0, calories = 20.0, proteinG = 1.5, carbsG = 1.5, fatG = 1.0, confidence = "medium")
                    ),
                    healthScore = 9.0,
                    healthNote = "Excellent breakfast high in healthy monounsaturated fats and dietary fibers! Ideal for cardiovascular wellbeing.",
                    warnings = emptyList()
                )
            }
            com.example.R.drawable.pepperoni_pizza -> {
                GeminiNutritionResponse(
                    mealName = "Stone Baked Pepperoni Pizza",
                    totalCalories = 480.0,
                    totalMacros = GeminiTotalMacros(proteinG = 18.0, carbsG = 42.0, fatG = 24.0, fiberG = 2.0),
                    items = listOf(
                        GeminiFoodItem(name = "Pizza Crust (Refined Flour)", portionG = 120.0, calories = 260.0, proteinG = 6.0, carbsG = 34.0, fatG = 3.0, confidence = "high"),
                        GeminiFoodItem(name = "Mozzarella Cheese (Whole Milk)", portionG = 60.0, calories = 130.0, proteinG = 8.0, carbsG = 2.0, fatG = 11.0, confidence = "high"),
                        GeminiFoodItem(name = "Dry-Cured Pork Pepperoni Slices", portionG = 25.0, calories = 90.0, proteinG = 4.0, carbsG = 6.0, fatG = 10.0, confidence = "high")
                    ),
                    healthScore = 4.5,
                    healthNote = "Contains high amount of sodium and saturated fats. Enjoy in moderation and try pairing with a lettuce salad.",
                    warnings = listOf("High sodium content detected (over 850mg)", "Significant saturated fat portions")
                )
            }
            com.example.R.drawable.salmon_salad -> {
                GeminiNutritionResponse(
                    mealName = "Seared Atlantic Salmon Salad",
                    totalCalories = 360.0,
                    totalMacros = GeminiTotalMacros(proteinG = 29.5, carbsG = 9.0, fatG = 19.0, fiberG = 3.5),
                    items = listOf(
                        GeminiFoodItem(name = "Pan-Seared Salmon Fillet", portionG = 150.0, calories = 240.0, proteinG = 26.0, carbsG = 0.0, fatG = 14.0, confidence = "high"),
                        GeminiFoodItem(name = "Baby Spinach & Arugula Greens Salad", portionG = 80.0, calories = 15.0, proteinG = 1.5, carbsG = 2.5, fatG = 0.0, confidence = "high"),
                        GeminiFoodItem(name = "Citrus Olive Oil Dressing", portionG = 15.0, calories = 105.0, proteinG = 2.0, carbsG = 6.5, fatG = 5.0, confidence = "medium")
                    ),
                    healthScore = 9.5,
                    healthNote = "A perfect high-protein, nutrient-dense choice! Salmon is loaded with heart-healthy omega-3 essential fatty acids and minerals.",
                    warnings = emptyList()
                )
            }
            else -> {
                GeminiNutritionResponse(
                    mealName = "Garden Harvest Power Bowl",
                    totalCalories = 390.0,
                    totalMacros = GeminiTotalMacros(proteinG = 24.0, carbsG = 32.0, fatG = 14.0, fiberG = 6.5),
                    items = listOf(
                        GeminiFoodItem(name = "Grilled Chicken Breast Strips", portionG = 120.0, calories = 180.0, proteinG = 21.0, carbsG = 0.0, fatG = 4.0, confidence = "high"),
                        GeminiFoodItem(name = "Tri-Color Quinoa & Roasted Veggies", portionG = 100.0, calories = 130.0, proteinG = 2.0, carbsG = 24.0, fatG = 2.0, confidence = "high"),
                        GeminiFoodItem(name = "Crumbled Feta Cheese", portionG = 20.0, calories = 80.0, proteinG = 1.0, carbsG = 8.0, fatG = 8.0, confidence = "medium")
                    ),
                    healthScore = 8.5,
                    healthNote = "Excellent balanced plate filled with complex carbohydrates, lean protein, and heart-healthy dietary fats.",
                    warnings = emptyList()
                )
            }
        }
    }

    private fun checkAndResetWaterIntake() {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val lastResetDate = sharedPrefs.getString("last_water_reset_date", "")
        if (todayStr != lastResetDate) {
            sharedPrefs.edit().apply {
                putInt("water_intake", 0)
                putString("last_water_reset_date", todayStr)
                apply()
            }
            _waterIntake.value = 0
        } else {
            _waterIntake.value = sharedPrefs.getInt("water_intake", 0)
        }
    }

    fun addWater(ml: Int) {
        viewModelScope.launch {
            checkAndResetWaterIntake()
            val newIntake = _waterIntake.value + ml
            _waterIntake.value = newIntake
            sharedPrefs.edit().putInt("water_intake", newIntake).apply()
        }
    }

    fun resetWater() {
        viewModelScope.launch {
            _waterIntake.value = 0
            sharedPrefs.edit().putInt("water_intake", 0).apply()
        }
    }

    fun updateWaterTarget(ml: Int) {
        viewModelScope.launch {
            _targetWater.value = ml
            sharedPrefs.edit().putInt("target_water", ml).apply()
        }
    }

    /**
     * AI Food text analyzer
     */
    fun analyzeTextMeal(description: String) {
        if (description.isBlank()) return
        viewModelScope.launch {
            _uiState.value = NutritionUiState.Loading

            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                kotlinx.coroutines.delay(1200)
                val response = getSimulatedTextResponse(description)
                _uiState.value = NutritionUiState.Success(response, null, isSimulated = true)
                return@launch
            }

            try {
                val corePrompt = """
                    You are a professional nutritionist AI and food recognition expert. Your job is to analyze descriptions of food eaten and return a highly accurate calorie and nutrition breakdown based strictly on USDA FoodData Central standards.

                    Estimation Logic & Hidden Calories:
                    1. Decompose the described meal and identify every ingredient, including base ingredients, toppings, spreads, and dressings.
                    2. **CRITICAL: Estimate hidden cooking oils, butter, sugars, and sauces.** These are the most common sources of hidden calories. Always assume standard cooking fats are present unless it is clearly described as raw or steamed.
                    3. Use these standard portion weight anchors for reference:
                       - Standard slice of bread: 35-40g
                       - Large egg: 50g
                       - Palm-sized chicken breast/meat fillet: 120-150g
                       - 1 tablespoon of cooking oil/butter: 14g
                       - Medium piece of fruit (apple, banana): 120-150g
                       - 1 cup of cooked rice/pasta: 150-180g
                    4. Check mathematical consistency: **Total Calories MUST strictly equal (Protein * 4) + (Carbs * 4) + (Fat * 9)**. Adjust individual item calories and totals to ensure this exact mathematical relationship is satisfied.

                    Return a structured JSON response with this exact schema:

                    {
                      "meal_name": "string — short descriptive name of the overall meal",
                      "total_calories": number,
                      "total_macros": {
                        "protein_g": number,
                        "carbs_g": number,
                        "fat_g": number,
                        "fiber_g": number
                      },
                      "items": [
                        {
                          "name": "string — food item name",
                          "portion_g": number,
                          "calories": number,
                          "protein_g": number,
                          "carbs_g": number,
                          "fat_g": number,
                          "confidence": "high | medium | low"
                        }
                      ],
                      "health_score": number (1–10, where 10 is most nutritious),
                      "health_note": "string — 1–2 sentence personalized tip about this meal",
                      "warnings": ["string"] or []
                    }

                    Rules:
                    - Always respond ONLY with valid JSON. No preamble, no markdown backticks.
                    - If you cannot identify a food item, include it with name "Unknown item" and confidence "low".
                    - Round all numbers to 1 decimal place.
                    - If the description does not refer to food, return: {"error": "The text does not seem to contain food or meal items."}
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(
                                GeminiPart(text = "Analyze my meal: ${description}")
                            )
                        )
                    ),
                    generationConfig = GeminiResponseConfig(
                        responseMimeType = "application/json",
                        temperature = 0.2
                    ),
                    systemInstruction = GeminiContent(
                        parts = listOf(GeminiPart(text = corePrompt))
                    )
                )

                val rawResponse = GeminiRetrofitClient.service.generateContent(apiKey, request)
                val rawText = rawResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (rawText != null) {
                    val sanitizedJson = rawText.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()

                    val adapter = moshi.adapter(GeminiNutritionResponse::class.java)
                    val result = adapter.fromJson(sanitizedJson)

                    if (result != null) {
                        if (result.error != null) {
                            _uiState.value = NutritionUiState.Error(result.error)
                        } else {
                            _uiState.value = NutritionUiState.Success(result, null)
                        }
                    } else {
                        _uiState.value = NutritionUiState.Error("Analyst output failed to parse correctly.")
                    }
                } else {
                    _uiState.value = NutritionUiState.Error("Nutritionist AI did not return a description. Please try again.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = NutritionUiState.Error("Connection error: ${e.localizedMessage ?: "Unknown network failure"}")
            }
        }
    }

    private fun getSimulatedTextResponse(description: String): GeminiNutritionResponse {
        val descLower = description.lowercase()
        return when {
            descLower.contains("egg") || descLower.contains("toast") -> {
                GeminiNutritionResponse(
                    mealName = "Scrambled Eggs & Sourdough Toast",
                    totalCalories = 310.0,
                    totalMacros = GeminiTotalMacros(proteinG = 16.0, carbsG = 22.0, fatG = 15.5, fiberG = 2.0),
                    items = listOf(
                        GeminiFoodItem(name = "Large Scrambled Eggs (2)", portionG = 100.0, calories = 140.0, proteinG = 12.0, carbsG = 1.0, fatG = 10.0, confidence = "high"),
                        GeminiFoodItem(name = "Sourdough Toast (1 slice)", portionG = 40.0, calories = 110.0, proteinG = 4.0, carbsG = 20.0, fatG = 0.5, confidence = "high"),
                        GeminiFoodItem(name = "Butter (for toast)", portionG = 7.0, calories = 60.0, proteinG = 0.0, carbsG = 0.0, fatG = 5.0, confidence = "medium")
                    ),
                    healthScore = 8.0,
                    healthNote = "Excellent source of high-quality protein and amino acids to kickstart your day! Pair with spinach for iron.",
                    warnings = emptyList()
                )
            }
            descLower.contains("apple") || descLower.contains("banana") || descLower.contains("fruit") -> {
                GeminiNutritionResponse(
                    mealName = "Fresh Fruits and Nut Butter",
                    totalCalories = 280.0,
                    totalMacros = GeminiTotalMacros(proteinG = 7.0, carbsG = 34.0, fatG = 16.0, fiberG = 6.0),
                    items = listOf(
                        GeminiFoodItem(name = "Fresh Red Apple", portionG = 150.0, calories = 90.0, proteinG = 0.5, carbsG = 22.0, fatG = 0.3, confidence = "high"),
                        GeminiFoodItem(name = "Creamy Peanut Butter (1.5 tbsp)", portionG = 24.0, calories = 190.0, proteinG = 6.5, carbsG = 12.0, fatG = 15.7, confidence = "high")
                    ),
                    healthScore = 9.0,
                    healthNote = "Healthy source of complex dietary fiber and monounsaturated fats. Keeps blood sugar stable.",
                    warnings = emptyList()
                )
            }
            else -> {
                GeminiNutritionResponse(
                    mealName = "Custom Described Meal Log",
                    totalCalories = 350.0,
                    totalMacros = GeminiTotalMacros(proteinG = 20.0, carbsG = 40.0, fatG = 12.0, fiberG = 4.0),
                    items = listOf(
                        GeminiFoodItem(name = "Identified food item from description", portionG = 150.0, calories = 250.0, proteinG = 15.0, carbsG = 30.0, fatG = 8.0, confidence = "medium"),
                        GeminiFoodItem(name = "Other mixed ingredient estimates", portionG = 50.0, calories = 100.0, proteinG = 5.0, carbsG = 10.0, fatG = 4.0, confidence = "low")
                    ),
                    healthScore = 8.0,
                    healthNote = "A well-balanced quick log meal. Matches standard USDA macro estimates.",
                    warnings = emptyList()
                )
            }
        }
    }

    /**
     * Logs custom manual meal input without AI.
     */
    fun logManualMeal(name: String, calories: Double, protein: Double, carbs: Double, fat: Double, fiber: Double) {
        viewModelScope.launch {
            try {
                val warningsAdapter = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
                val itemsAdapter = moshi.adapter<List<GeminiFoodItem>>(Types.newParameterizedType(List::class.java, GeminiFoodItem::class.java))

                val warningsStr = warningsAdapter.toJson(emptyList())
                val itemsStr = itemsAdapter.toJson(
                    listOf(GeminiFoodItem(name = name, portionG = 0.0, calories = calories, proteinG = protein, carbsG = carbs, fatG = fat, confidence = "high"))
                )

                val mealLog = MealScan(
                    mealName = name,
                    totalCalories = calories,
                    protein = protein,
                    carbs = carbs,
                    fat = fat,
                    fiber = fiber,
                    healthScore = 8,
                    healthNote = "Custom logged manually.",
                    warningsJson = warningsStr,
                    itemsJson = itemsStr,
                    localImagePath = null
                )

                repository.insert(mealLog)

                _uiState.value = NutritionUiState.Idle
                _selectedBitmap.value = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun runSimulatedScan(presetResId: Int? = null) {
        viewModelScope.launch {
            _uiState.value = NutritionUiState.Loading
            kotlinx.coroutines.delay(1200)
            val response = getSimulatedResponse(presetResId)
            _uiState.value = NutritionUiState.Success(response, _selectedBitmap.value, isSimulated = true)
        }
    }

    fun runSimulatedTextScan(description: String) {
        viewModelScope.launch {
            _uiState.value = NutritionUiState.Loading
            kotlinx.coroutines.delay(1200)
            val response = getSimulatedTextResponse(description)
            _uiState.value = NutritionUiState.Success(response, null, isSimulated = true)
        }
    }
}
