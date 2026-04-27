package com.example.whatdoing.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Color.White,
    primaryContainer = Navy80,
    onPrimaryContainer = Silver80,

    secondary = Blue90,
    onSecondary = Color.White,
    secondaryContainer = Navy40,
    onSecondaryContainer = Silver40,

    tertiary = Silver80,
    onTertiary = Navy90,

    background = DarkBackground,
    onBackground = Color.White,

    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = Navy80,
    onSurfaceVariant = Silver80,

    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Navy40,
    onPrimary = Color.White,
    primaryContainer = Blue40,
    onPrimaryContainer = Navy90,

    secondary = Blue40,
    onSecondary = Color.White,
    secondaryContainer = Silver40,
    onSecondaryContainer = Navy90,

    tertiary = Silver90,
    onTertiary = Color.White,

    background = Color(0xFFF5F7FA),
    onBackground = Navy90,

    surface = Color.White,
    onSurface = Navy90,
    surfaceVariant = Silver40,
    onSurfaceVariant = Navy40,

    error = ErrorRed,
    onError = Color.White
)

@Composable
fun WhatDoingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // 다크 네이비 테마 유지를 위해 false로 변경
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}