package com.jinsupark.helpumta.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    primaryContainer = OrangeContainer,
    onPrimaryContainer = OnOrangeContainer,

    secondary = OrangeDark,
    onSecondary = Color.White,
    secondaryContainer = BeigeDark,
    onSecondaryContainer = BrownText,

    tertiary = BrownTextSoft,
    onTertiary = Color.White,

    background = Color.White,
    onBackground = BrownText,

    surface = Color.White,
    onSurface = BrownText,
    surfaceVariant = BeigeDark,
    onSurfaceVariant = BrownTextSoft,

    surfaceContainer = Color.White,
    surfaceContainerHighest = BeigeLight,

    error = ErrorRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    primaryContainer = OrangeDark,
    onPrimaryContainer = OrangeContainer,

    secondary = Color(0xFFFF9D5C),
    onSecondary = Color(0xFF3D3325),
    secondaryContainer = Color(0xFF4A3D2C),
    onSecondaryContainer = Beige,

    tertiary = BeigeDark,
    onTertiary = Color(0xFF3D3325),

    background = Color(0xFF1A1611),     // 어두운 갈색빛 배경
    onBackground = Beige,

    surface = Color(0xFF1A1611),
    onSurface = Beige,
    surfaceVariant = Color(0xFF4A3D2C),
    onSurfaceVariant = BeigeDark,

    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHighest = Color(0xFF252017),

    error = Color(0xFFF2B8B5),
    onError = Color(0xFF3D3325)
)

@Composable
fun HelpumtaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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