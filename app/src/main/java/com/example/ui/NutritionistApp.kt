package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.GeminiFoodItem
import com.example.data.GeminiNutritionResponse
import com.example.data.MealScan
import com.example.network.GeminiRetrofitClient
import com.example.viewmodel.NutritionUiState
import com.example.viewmodel.NutritionViewModel
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.AppBackground
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate100
import com.example.ui.theme.Teal50
import com.example.ui.theme.Teal700
import com.example.ui.theme.OrangeFlame
import com.example.ui.theme.Orange100
import com.squareup.moshi.Types
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NutritionistApp(
    viewModel: NutritionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedBitmap by viewModel.selectedBitmap.collectAsStateWithLifecycle()
    val mealHistory by viewModel.mealHistory.collectAsStateWithLifecycle()

    val targetCalories by viewModel.targetCalories.collectAsStateWithLifecycle()
    val targetProtein by viewModel.targetProtein.collectAsStateWithLifecycle()
    val targetCarbs by viewModel.targetCarbs.collectAsStateWithLifecycle()
    val targetFat by viewModel.targetFat.collectAsStateWithLifecycle()
    val targetFiber by viewModel.targetFiber.collectAsStateWithLifecycle()

    val waterIntake by viewModel.waterIntake.collectAsStateWithLifecycle()
    val targetWater by viewModel.targetWater.collectAsStateWithLifecycle()

    var showGoalDialog by remember { mutableStateOf(false) }
    var showManualLogDialog by remember { mutableStateOf(false) }

    // Contracts for capturing images
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = loadBitmapFromUri(context, it)
            if (bitmap != null) {
                viewModel.setSelectedImage(bitmap)
                viewModel.analyzeFoodImage(bitmap)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            viewModel.setSelectedImage(it)
            viewModel.analyzeFoodImage(it)
        }
    }

    // Calculations for eaten totals today
    val todayStart = getStartOfToday()
    val todayMeals = mealHistory.filter { it.timestamp >= todayStart }
    
    val totalCaloriesEaten = todayMeals.sumOf { it.totalCalories }
    val totalProteinEaten = todayMeals.sumOf { it.protein }
    val totalCarbsEaten = todayMeals.sumOf { it.carbs }
    val totalFatEaten = todayMeals.sumOf { it.fat }
    val totalFiberEaten = todayMeals.sumOf { it.fiber }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .size(36.dp)
                                .background(Teal50, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonitorWeight,
                                contentDescription = "Logo",
                                tint = PrimaryTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Meal Analysis",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Slate900
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showGoalDialog = true },
                        modifier = Modifier
                            .testTag("goals_button")
                            .clip(CircleShape)
                            .background(Teal50)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configure Goals",
                            tint = PrimaryTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(80.dp)
                        .background(Color.White)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left element: "Journal"
                    Column(
                        modifier = Modifier
                            .clickable(onClick = {})
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Journal",
                            tint = PrimaryTeal,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "JOURNAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Center Element: Floating scan button
                    Box(
                        modifier = Modifier
                            .offset(y = (-10).dp)
                            .size(60.dp)
                            .background(PrimaryTeal, CircleShape)
                            .border(4.dp, AppBackground, CircleShape)
                            .clickable {
                                try {
                                    cameraLauncher.launch()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Camera is not available on this device.",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Scan",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Right element: "Settings"
                    Column(
                        modifier = Modifier
                            .clickable { showGoalDialog = true }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "SETTINGS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dashboard Progress
            item {
                DailyProgressCard(
                    caloriesEaten = totalCaloriesEaten,
                    proteinEaten = totalProteinEaten,
                    carbsEaten = totalCarbsEaten,
                    fatEaten = totalFatEaten,
                    fiberEaten = totalFiberEaten,
                    targetCalories = targetCalories,
                    targetProtein = targetProtein,
                    targetCarbs = targetCarbs,
                    targetFat = targetFat,
                    targetFiber = targetFiber,
                    onManageGoals = { showGoalDialog = true }
                )
            }

            // Hydration Tracking
            item {
                WaterTrackerCard(
                    waterIntake = waterIntake,
                    targetWater = targetWater,
                    onAddWater = { viewModel.addWater(it) },
                    onResetWater = { viewModel.resetWater() }
                )
            }

            // Weekly Calorie Trends Analytics
            item {
                WeeklyTrendsCard(
                    mealHistory = mealHistory,
                    targetCalories = targetCalories
                )
            }

            // Quick Scan Presets Section
            item {
                PresetScannerSection(
                    onPresetSelected = { resourceId ->
                        viewModel.selectPresetMeal(resourceId)
                    }
                )
            }

            // Text-based AI Meal Description
            item {
                TextMealSearchCard(
                    onAnalyzeText = { viewModel.analyzeTextMeal(it) },
                    onManualLog = { showManualLogDialog = true }
                )
            }

            // Photo Capture Buttons
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Analyze New Meal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Snap a food picture or select a photograph from your device gallery to analyze calories instantly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        cameraLauncher.launch()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Camera is not available on this device.",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("camera_button"),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = "Camera")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Camera Snap")
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        galleryLauncher.launch("image/*")
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Gallery or File storage is not available on this device.",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("gallery_button"),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upload Photo")
                            }
                        }
                    }
                }
            }

            // Scanning Status / Success Cards
            item {
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "scan_views"
                ) { state ->
                    when (state) {
                        is NutritionUiState.Idle -> {
                            // Empty state guidance
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Ready to analyze food. Pick a preset or snap a picture above!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        is NutritionUiState.Loading -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("loading_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 4.dp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Nutritionist AI Scanning...",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Identifying items, calculating macros & running USDA portions comparison",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        is NutritionUiState.Success -> {
                            ScanResultCard(
                                response = state.response,
                                bitmap = state.bitmap,
                                isSimulated = state.isSimulated,
                                onSaveLog = {
                                    viewModel.logAnalyzedMeal(state.response, state.bitmap)
                                },
                                onCancel = {
                                    viewModel.setSelectedImage(null)
                                }
                            )
                        }
                        is NutritionUiState.Error -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("error_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Error Logo",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Diagnostic Scan Flagged",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.runSimulatedScan() },
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Try Simulation", color = MaterialTheme.colorScheme.error)
                                        }
                                        Button(
                                            onClick = { viewModel.setSelectedImage(null) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Text("Dismiss", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Eating Logs History Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nutrition History Log",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (mealHistory.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearLogHistory() },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Text("Clear All")
                        }
                    }
                }
            }

            // Eating Logs Empty State or Items List
            if (mealHistory.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fastfood,
                                contentDescription = "No Food Logged",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Your Eat Log is Empty",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Successfully scanned and logged meals will appear here to record daily streaks and aggregate nutrients.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(mealHistory, key = { it.id }) { meal ->
                    LoggedMealCard(
                        meal = meal,
                        onDelete = { viewModel.deleteLoggedMeal(meal.id) }
                    )
                }
            }
        }
    }

    // Editable Goals modal
    if (showGoalDialog) {
        ProfileGoalsDialog(
            currentCalories = targetCalories,
            currentProtein = targetProtein,
            currentCarbs = targetCarbs,
            currentFat = targetFat,
            currentFiber = targetFiber,
            onDismiss = { showGoalDialog = false },
            onSave = { cal, prot, carb, fat, fib ->
                viewModel.updateDailyTargets(cal, prot, carb, fat, fib)
                showGoalDialog = false
            }
        )
    }

    // Manual quick log dialog
    if (showManualLogDialog) {
        ManualLogDialog(
            onDismiss = { showManualLogDialog = false },
            onSave = { name, cal, prot, carb, fat, fib ->
                viewModel.logManualMeal(name, cal, prot, carb, fat, fib)
                showManualLogDialog = false
            }
        )
    }
}

// Subcomponents:

@Composable
fun DailyProgressCard(
    caloriesEaten: Double,
    proteinEaten: Double,
    carbsEaten: Double,
    fatEaten: Double,
    fiberEaten: Double,
    targetCalories: Double,
    targetProtein: Double,
    targetCarbs: Double,
    targetFat: Double,
    targetFiber: Double,
    onManageGoals: () -> Unit
) {
    val caloriePercentage = if (targetCalories > 0.0) {
        val pct = caloriesEaten / targetCalories
        if (pct.isNaN() || pct.isInfinite()) 0f else pct.coerceIn(0.0, 1.0).toFloat()
    } else 0f
    val proteinPercentage = if (targetProtein > 0.0) {
        val pct = proteinEaten / targetProtein
        if (pct.isNaN() || pct.isInfinite()) 0f else pct.coerceIn(0.0, 1.0).toFloat()
    } else 0f
    val carbPercentage = if (targetCarbs > 0.0) {
        val pct = carbsEaten / targetCarbs
        if (pct.isNaN() || pct.isInfinite()) 0f else pct.coerceIn(0.0, 1.0).toFloat()
    } else 0f
    val fatPercentage = if (targetFat > 0.0) {
        val pct = fatEaten / targetFat
        if (pct.isNaN() || pct.isInfinite()) 0f else pct.coerceIn(0.0, 1.0).toFloat()
    } else 0f
    val fiberPercentage = if (targetFiber > 0.0) {
        val pct = fiberEaten / targetFiber
        if (pct.isNaN() || pct.isInfinite()) 0f else pct.coerceIn(0.0, 1.0).toFloat()
    } else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp), // modern 3xl rounded corner
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate100),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Consumption",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    Text(
                        text = "Compare current active intake against goal markers",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                FilledTonalButton(
                    onClick = onManageGoals,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Teal50,
                        contentColor = PrimaryTeal
                    )
                ) {
                    Text("Targets", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Row showing Calorie Progress Circle and numeric counters
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Static grey track
                    Canvas(modifier = Modifier.size(100.dp)) {
                        drawCircle(
                            color = Slate100,
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    // Colored progress sweep
                    Canvas(modifier = Modifier.size(100.dp)) {
                        drawArc(
                            color = PrimaryTeal,
                            startAngle = -90f,
                            sweepAngle = caloriePercentage * 360f,
                            useCenter = false,
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${caloriesEaten.toInt()}",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = Slate900
                        )
                        Text(
                            text = "of ${targetCalories.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Text(
                            text = "kcal",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Macros column breakdown with customized vibrant colors aligning with theme
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MacroProgressBar(
                        label = "Protein",
                        value = proteinEaten,
                        target = targetProtein,
                        percent = proteinPercentage,
                        color = Color(0xFF60A5FA), // Vibrant Blue
                        suffix = "g"
                    )
                    MacroProgressBar(
                        label = "Carbs",
                        value = carbsEaten,
                        target = targetCarbs,
                        percent = carbPercentage,
                        color = Color(0xFF34D399), // Vibrant Emerald
                        suffix = "g"
                    )
                    MacroProgressBar(
                        label = "Fat",
                        value = fatEaten,
                        target = targetFat,
                        percent = fatPercentage,
                        color = Color(0xFFFBBF24), // Vibrant Amber
                        suffix = "g"
                    )
                    MacroProgressBar(
                        label = "Fiber",
                        value = fiberEaten,
                        target = targetFiber,
                        percent = fiberPercentage,
                        color = Color(0xFF2DD4BF), // Vibrant Mint/Teal
                        suffix = "g"
                    )
                }
            }
        }
    }
}

@Composable
fun MacroProgressBar(
    label: String,
    value: Double,
    target: Double,
    percent: Float,
    color: Color,
    suffix: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Text(
                text = "${String.format("%.1f", value)}$suffix / ${target.toInt()}$suffix",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { percent },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = color,
            trackColor = Color.LightGray.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun PresetScannerSection(
    onPresetSelected: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Fast Analysis Presets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            item {
                PresetItemCard(
                    title = "Avocado Toast",
                    category = "Healthy Breakfast",
                    imageResId = R.drawable.avocado_toast,
                    onClick = { onPresetSelected(R.drawable.avocado_toast) }
                )
            }
            item {
                PresetItemCard(
                    title = "Pepperoni Pizza",
                    category = "Cheesy Indulgence",
                    imageResId = R.drawable.pepperoni_pizza,
                    onClick = { onPresetSelected(R.drawable.pepperoni_pizza) }
                )
            }
            item {
                PresetItemCard(
                    title = "Salmon Salad",
                    category = "Hi-Protein Meal",
                    imageResId = R.drawable.salmon_salad,
                    onClick = { onPresetSelected(R.drawable.salmon_salad) }
                )
            }
        }
    }
}

@Composable
fun PresetItemCard(
    title: String,
    category: String,
    imageResId: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(150.dp)
            .height(180.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = androidx.compose.ui.res.painterResource(id = imageResId),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic vignette background overlay for text contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 100f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = category.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primaryContainer
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScanResultCard(
    response: GeminiNutritionResponse,
    bitmap: Bitmap?,
    isSimulated: Boolean = false,
    onSaveLog: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("success_card"),
        shape = RoundedCornerShape(24.dp), // modern 3xl rounded corner
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Optional Header Display image with Absolute Overlay
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = response.mealName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient vignette
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                                    startY = 100f
                                )
                            )
                    )
                    // Verified by AI overlay
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.9f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "Verified by AI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryTeal
                        )
                    }
                    
                    Text(
                        text = response.mealName ?: "Detected Meal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                if (isSimulated) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Simulated Demo Active",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Demo Mode: Scan is intelligently simulated. Connect a live GEMINI_API_KEY in the Secrets panel anytime.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                // Health Score and Quick Stats Stacked
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (bitmap == null) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = response.mealName ?: "Detected Meal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Calories Stat Card (Mocked visual on left of health score)
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Slate100)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Orange100, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = OrangeFlame,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "${(response.totalCalories ?: 0.0).toInt()}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Slate900
                                )
                                Text(
                                    text = "Calories",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // Health Score component matching theme visual (Teal, solid background with mini sweep)
                    Card(
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimaryTeal)
                    ) {
                        Column(
                            modifier = Modifier.padding(11.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "${((response.healthScore ?: 5.0).toInt())}.0",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "HEALTH SCORE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Small white progress line representing the score ratio
                            val rawScore = response.healthScore ?: 5.0
                            val progressRatio = if (rawScore.isNaN() || rawScore <= 0.0) 0.5f else (rawScore / 10.0).toFloat().coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressRatio)
                                        .height(5.dp)
                                        .background(Color.White, CircleShape)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Modern bg-slate-900 Macronutrient Breakdown panel directly matching mockup!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Macronutrients".uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.LightGray.copy(alpha = 0.7f),
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF334155)) // slate-700
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "USDA STANDARD",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.LightGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Segmented ratio bar (Blue, Emerald, Amber)
                        val pVal = (response.totalMacros?.proteinG ?: 0.0).toFloat()
                        val cVal = (response.totalMacros?.carbsG ?: 0.0).toFloat()
                        val fVal = (response.totalMacros?.fatG ?: 0.0).toFloat()
                        val fibVal = (response.totalMacros?.fiberG ?: 0.0).toFloat()
                        SegmentedMacroBar(pVal, cVal, fVal, fibVal)

                        Spacer(modifier = Modifier.height(12.dp))

                        // Macro indicators columns
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Protein", fontSize = 11.sp, color = Color.Gray)
                                Text("${String.format("%.1f", pVal)}g", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text("Carbs", fontSize = 11.sp, color = Color.Gray)
                                Text("${String.format("%.1f", cVal)}g", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text("Fat", fontSize = 11.sp, color = Color.Gray)
                                Text("${String.format("%.1f", fVal)}g", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text("Fiber", fontSize = 11.sp, color = Color.Gray)
                                Text("${String.format("%.1f", fibVal)}g", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sticky Health Note / Recommendation from mock
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Teal50)
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Health Recommendation",
                        tint = PrimaryTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = response.healthNote ?: "Intake aligns perfectly with balanced dietary targets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Teal700,
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Warnings section
                if (!response.warnings.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warnings",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Nutrient Warnings",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        response.warnings.forEach { warning ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text("• ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                Text(warning, fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Identified sub items breakdown
                if (!response.items.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Item Breakdown",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${response.items.size} meals detected",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Slate100)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            response.items.forEachIndexed { idx, item ->
                                val dotColor = when (idx % 4) {
                                    0 -> Color(0xFF60A5FA)
                                    1 -> Color(0xFF34D399)
                                    2 -> Color(0xFFFBBF24)
                                    else -> Color(0xFF2DD4BF)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Small rounded color indicator dot matching macro
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(dotColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate900,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${item.portionG.toInt()}g",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "${item.calories.toInt()} kcal",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                if (idx < response.items.size - 1) {
                                    Divider(color = Slate50, thickness = 1.dp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Log Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Discard")
                    }

                    Button(
                        onClick = onSaveLog,
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("save_meal_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Log Meal")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save to Journal")
                    }
                }
            }
        }
    }
}

@Composable
fun HealthScoreBadge(score: Int) {
    val containerColor = when {
        score >= 8 -> Color(0xFFE2F3E8)     // light green
        score >= 6 -> Color(0xFFFEF6E1)     // light yellow
        else -> Color(0xFFFFEAEA)           // light red
    }
    val textColor = when {
        score >= 8 -> Color(0xFF247F46)
        score >= 6 -> Color(0xFFB07F05)
        else -> Color(0xFFD32F2F)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score/10",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
            Text(
                text = "Nutrition",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun RowScope.MacroSubBadge(
    label: String,
    value: Double,
    color: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = "${String.format("%.1f", value)}g",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900
            )
        }
    }
}

@Composable
fun ItemLineBreakdown(item: GeminiFoodItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.3f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "USDA Est: ${item.portionG.toInt()}g",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    ConfidenceBadge(confidence = item.confidence)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${item.calories.toInt()} kcal",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${item.proteinG.toInt()}p/${item.carbsG.toInt()}c/${item.fatG.toInt()}f",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ConfidenceBadge(confidence: String) {
    val label = confidence.uppercase()
    val badgeColor = when (confidence.lowercase()) {
        "high" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
        "medium" -> Color(0xFFFFC107).copy(alpha = 0.2f)
        else -> Color(0xFFF44336).copy(alpha = 0.2f)
    }
    val textColor = when (confidence.lowercase()) {
        "high" -> Color(0xFF388E3C)
        "medium" -> Color(0xFFF57F17)
        else -> Color(0xFFD32F2F)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(badgeColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$label Match",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun LoggedMealCard(
    meal: MealScan,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Decode Room sub lists from saved strings
    val warnings = remember(meal.warningsJson) { parseWarningsJson(meal.warningsJson) }
    val items = remember(meal.itemsJson) { parseItemsJson(meal.itemsJson) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("logged_meal_card_${meal.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Miniature Photo Display
                if (meal.localImagePath != null) {
                    AsyncImage(
                        model = meal.localImagePath,
                        contentDescription = meal.mealName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fastfood,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Core Log details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meal.mealName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val (catLabel, catIcon, catColor) = getMealCategoryDetails(meal.timestamp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(catColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = catIcon,
                                contentDescription = catLabel,
                                tint = catColor,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = catLabel.uppercase(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = catColor
                            )
                        }

                        Text(
                            text = "${meal.totalCalories.toInt()} kcal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = formatTimestamp(meal.timestamp),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Health mini badge & delete
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HealthBadgeScoreSmall(score = meal.healthScore)
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_meal_button_${meal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Expandable details (Clinical checklist + macros breakdowns + child component lines)
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // Clinical note tips
                    Row {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = meal.healthNote.ifEmpty { "Balanced meal matching macro targets." },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Warning tags in expansion
                    if (warnings.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Warnings: " + warnings.joinToString(", "),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Detail macros counts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MacroBadgeTiny("Prot", meal.protein, Color(0xFFFF5964))
                        MacroBadgeTiny("Carb", meal.carbs, Color(0xFF35A7FF))
                        MacroBadgeTiny("Fat", meal.fat, Color(0xFFFFC045))
                        MacroBadgeTiny("Fib", meal.fiber, Color(0xFF329F5B))
                    }

                    // Individual items
                    if (items.isNotEmpty()) {
                        Text(
                            text = "Item Breakdowns:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        items.forEach { subItem ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• ${subItem.name} (${subItem.portionG.toInt()}g)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${subItem.calories.toInt()} kcal",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HealthBadgeScoreSmall(score: Int) {
    val color = when {
        score >= 8 -> Color(0xFF247F46)
        score >= 6 -> Color(0xFFB07F05)
        else -> Color(0xFFD32F2F)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$score/10",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    }
}

@Composable
fun MacroBadgeTiny(
    label: String,
    value: Double,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = "$label: ${value.toInt()}g",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun ProfileGoalsDialog(
    currentCalories: Double,
    currentProtein: Double,
    currentCarbs: Double,
    currentFat: Double,
    currentFiber: Double,
    onDismiss: () -> Unit,
    onSave: (Double, Double, Double, Double, Double) -> Unit
) {
    var calories by remember { mutableStateOf(currentCalories.toInt().toString()) }
    var protein by remember { mutableStateOf(currentProtein.toInt().toString()) }
    var carbs by remember { mutableStateOf(currentCarbs.toInt().toString()) }
    var fat by remember { mutableStateOf(currentFat.toInt().toString()) }
    var fiber by remember { mutableStateOf(currentFiber.toInt().toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("goals_dialog"),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customize Targets",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text("Daily Calories Target (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("calories_target_input"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    label = { Text("Daily Protein Target (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    label = { Text("Daily Carbs Target (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text("Daily Fat Target (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = fiber,
                    onValueChange = { fiber = it },
                    label = { Text("Daily Fiber Target (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val cal = calories.toDoubleOrNull() ?: currentCalories
                        val prot = protein.toDoubleOrNull() ?: currentProtein
                        val carb = carbs.toDoubleOrNull() ?: currentCarbs
                        val f = fat.toDoubleOrNull() ?: currentFat
                        val fib = fiber.toDoubleOrNull() ?: currentFiber
                        onSave(cal, prot, carb, f, fib)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_goals_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Goals")
                }
            }
        }
    }
}

// Global Help Functions:

fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getStartOfToday(): Long {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun parseWarningsJson(json: String): List<String> {
    return try {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        GeminiRetrofitClient.moshi.adapter<List<String>>(type).fromJson(json) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

fun parseItemsJson(json: String): List<GeminiFoodItem> {
    return try {
        val type = Types.newParameterizedType(List::class.java, GeminiFoodItem::class.java)
        GeminiRetrofitClient.moshi.adapter<List<GeminiFoodItem>>(type).fromJson(json) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun SegmentedMacroBar(
    proteinPercent: Float,
    carbsPercent: Float,
    fatPercent: Float,
    fiberPercent: Float
) {
    val pSafe = if (proteinPercent.isNaN() || proteinPercent < 0f) 0f else proteinPercent
    val cSafe = if (carbsPercent.isNaN() || carbsPercent < 0f) 0f else carbsPercent
    val fSafe = if (fatPercent.isNaN() || fatPercent < 0f) 0f else fatPercent
    val fibSafe = if (fiberPercent.isNaN() || fiberPercent < 0f) 0f else fiberPercent
    val total = pSafe + cSafe + fSafe + fibSafe

    val protW = if (total > 0f) pSafe / total else 0.25f
    val carbW = if (total > 0f) cSafe / total else 0.25f
    val fatW = if (total > 0f) fSafe / total else 0.25f
    val fibW = if (total > 0f) fibSafe / total else 0.25f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (protW > 0.05f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(protW)
                    .background(Color(0xFF60A5FA), CircleShape)
            )
        }
        if (carbW > 0.05f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(carbW)
                    .background(Color(0xFF34D399), CircleShape)
            )
        }
        if (fatW > 0.05f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(fatW)
                    .background(Color(0xFFFBBF24), CircleShape)
            )
        }
        if (fibW > 0.05f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(fibW)
                    .background(Color(0xFF2DD4BF), CircleShape)
            )
        }
    }
}

@Composable
fun rememberWeeklyCalorieData(mealHistory: List<MealScan>, targetCalories: Double): List<Pair<String, Float>> {
    return remember(mealHistory) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFormat = SimpleDateFormat("EEE", Locale.US)

        val dailyMap = mutableMapOf<String, Float>()
        val dateList = mutableListOf<Pair<String, String>>()

        for (i in 6 downTo 0) {
            val d = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -i) }
            val dateKey = sdf.format(d.time)
            val dayLabel = dayFormat.format(d.time)
            dailyMap[dateKey] = 0f
            dateList.add(dateKey to dayLabel)
        }

        mealHistory.forEach { scan ->
            val scanDate = sdf.format(Date(scan.timestamp))
            if (dailyMap.containsKey(scanDate)) {
                dailyMap[scanDate] = dailyMap[scanDate]!! + scan.totalCalories.toFloat()
            }
        }

        dateList.map { (dateKey, label) ->
            label to dailyMap[dateKey]!!
        }
    }
}

@Composable
fun WeeklyTrendsCard(
    mealHistory: List<MealScan>,
    targetCalories: Double
) {
    val weeklyData = rememberWeeklyCalorieData(mealHistory, targetCalories)
    val maxCalVal = remember(weeklyData, targetCalories) {
        maxOf(weeklyData.maxOfOrNull { it.second } ?: 0f, targetCalories.toFloat(), 1000f) * 1.15f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate100),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "7-Day Calorie Trends",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900
            )
            Text(
                text = "Tracking your daily intake compared to your current target",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    val barWidth = 32.dp.toPx()
                    val totalSpacing = canvasWidth - (barWidth * 7)
                    val barSpacing = totalSpacing / 8

                    val targetY = canvasHeight - (targetCalories.toFloat() / maxCalVal) * canvasHeight

                    val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    drawLine(
                        color = Color.Red.copy(alpha = 0.5f),
                        start = androidx.compose.ui.geometry.Offset(0f, targetY),
                        end = androidx.compose.ui.geometry.Offset(canvasWidth, targetY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = pathEffect
                    )

                    weeklyData.forEachIndexed { index, (label, valAmount) ->
                        val x = barSpacing + index * (barWidth + barSpacing)
                        val barHeight = (valAmount / maxCalVal) * canvasHeight
                        val y = canvasHeight - barHeight

                        drawRoundRect(
                            color = Slate100.copy(alpha = 0.5f),
                            topLeft = androidx.compose.ui.geometry.Offset(x, 0f),
                            size = androidx.compose.ui.geometry.Size(barWidth, canvasHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )

                        if (valAmount > 0f) {
                            val brush = Brush.verticalGradient(
                                colors = listOf(PrimaryTeal, Teal700),
                                startY = y,
                                endY = canvasHeight
                            )
                            drawRoundRect(
                                brush = brush,
                                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx())
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weeklyData.forEach { (label, valAmount) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = if (valAmount > 0) "${valAmount.toInt()}" else "-",
                            fontSize = 10.sp,
                            color = if (valAmount >= targetCalories) PrimaryTeal else Color.Gray,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WaterTrackerCard(
    waterIntake: Int,
    targetWater: Int,
    onAddWater: (Int) -> Unit,
    onResetWater: () -> Unit
) {
    val progress = if (targetWater > 0) {
        (waterIntake.toFloat() / targetWater.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate100),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hydration Tracker",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    Text(
                        text = "Keep up with your daily water goal",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFEFF6FF), CircleShape)
                        .clickable { onResetWater() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Water",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF0F9FF)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val waterHeight = canvasHeight * progress
                        
                        drawRect(
                            color = Color(0xFF60A5FA),
                            topLeft = androidx.compose.ui.geometry.Offset(0f, canvasHeight - waterHeight),
                            size = androidx.compose.ui.geometry.Size(canvasWidth, waterHeight)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "Water Drop",
                            tint = if (progress > 0.4f) Color.White else Color(0xFF2563EB),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (progress > 0.4f) Color.White else Color(0xFF1E3A8A)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$waterIntake ml / $targetWater ml",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onAddWater(250) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalCafe,
                                contentDescription = "Cup",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+250ml", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { onAddWater(500) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalDrink,
                                contentDescription = "Bottle",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+500ml", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TextMealSearchCard(
    onAnalyzeText: (String) -> Unit,
    onManualLog: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate100),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Describe What You Ate",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900
            )
            Text(
                text = "Input a text description to estimate USDA calorie breakdown instantly",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        text = "e.g., 2 medium scrambled eggs with a handful of spinach and one sourdough slice",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryTeal,
                    unfocusedBorderColor = Slate100
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onManualLog() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Slate100)
                ) {
                    Text("Manual Quick Log", fontSize = 12.sp, color = Slate900, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onAnalyzeText(textInput)
                            textInput = ""
                        }
                    },
                    enabled = textInput.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "AI")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Analyze with AI", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ManualLogDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, calories: Double, protein: Double, carbs: Double, fat: Double, fiber: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Manual Quick Log",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = Slate900
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Meal Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text("Calories (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = { Text("Fat (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = fiber,
                        onValueChange = { fiber = it },
                        label = { Text("Fiber (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = Slate900)
                    }

                    Button(
                        onClick = {
                            onSave(
                                name.ifBlank { "Custom Log" },
                                calories.toDoubleOrNull() ?: 0.0,
                                protein.toDoubleOrNull() ?: 0.0,
                                carbs.toDoubleOrNull() ?: 0.0,
                                fat.toDoubleOrNull() ?: 0.0,
                                fiber.toDoubleOrNull() ?: 0.0
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                    ) {
                        Text("Log Meal")
                    }
                }
            }
        }
    }
}

fun getMealCategoryDetails(timestamp: Long): Triple<String, androidx.compose.ui.graphics.vector.ImageVector, Color> {
    val hour = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }.get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..10 -> Triple("Breakfast", Icons.Default.Coffee, Color(0xFFFBBF24))
        in 11..15 -> Triple("Lunch", Icons.Default.Restaurant, Color(0xFFF97316))
        in 16..21 -> Triple("Dinner", Icons.Default.Restaurant, Color(0xFF6366F1))
        else -> Triple("Snack", Icons.Default.Fastfood, Color(0xFFEC4899))
    }
}

