package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryTeal,
    secondary = Teal100,
    tertiary = OrangeFlame,
    background = Slate900,
    surface = Color(0xFF1E293B), // slate-800
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = Teal700,
    onPrimaryContainer = Teal50,
    surfaceVariant = Color(0xFF334155), // slate-700
    onSurfaceVariant = Color.White
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryTeal,
    secondary = Teal700,
    tertiary = OrangeFlame,
    background = AppBackground,
    surface = Color.White,
    primaryContainer = Teal100,
    onPrimaryContainer = Teal700,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Slate900,
    onSurface = Slate900,
    surfaceVariant = Slate50,
    onSurfaceVariant = Slate900,
    secondaryContainer = Teal50,
    onSecondaryContainer = Teal700
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disabling dynamic colors to guarantee our gorgeous custom Vibrant Palette shows as requested
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
