package com.jinsupark.helpumta.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 색 원칙 세 가지
 *  1. 배경·텍스트는 완전 무채색. 브랜드 색이 surface까지 번지지 않게 한다.
 *  2. 주황은 모드가 바뀌어도 같은 값.
 *  3. 주황은 포인트에만 — FAB, 주요 버튼, 달성 표시. 나머지는 무채색.
 *
 * secondary·tertiary까지 무채색으로 고정한 건 의도적이다.
 * 비워두면 Material3가 primary에서 색을 파생시켜 베이지가 다시 번진다.
 */
internal val LightColorScheme = lightColorScheme(
    primary = Orange,
    onPrimary = White,
    primaryContainer = OrangeTintLight,
    onPrimaryContainer = OrangePressed,

    secondary = Gray900,
    onSecondary = White,
    secondaryContainer = Gray100,
    onSecondaryContainer = Gray900,

    tertiary = Gray600,
    onTertiary = White,
    tertiaryContainer = Gray100,
    onTertiaryContainer = Gray900,

    background = White,
    onBackground = Gray900,

    surface = White,
    onSurface = Gray900,
    surfaceVariant = Gray50,
    onSurfaceVariant = Gray600,

    surfaceContainerLowest = White,
    surfaceContainerLow = Gray50,
    surfaceContainer = Gray50,
    surfaceContainerHigh = Gray100,
    surfaceContainerHighest = Gray100,

    outline = Gray200,
    outlineVariant = Gray100,

    error = ErrorRed,
    onError = White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,

    inverseSurface = Gray900,
    inverseOnSurface = White,
    scrim = Scrim
)

internal val DarkColorScheme = darkColorScheme(
    primary = Orange,            // 라이트와 동일
    onPrimary = White,
    primaryContainer = OrangeTintDark,
    onPrimaryContainer = Orange,

    secondary = DarkText,
    onSecondary = Dark0,
    secondaryContainer = Dark100,
    onSecondaryContainer = DarkText,

    tertiary = Gray400,
    onTertiary = Dark0,
    tertiaryContainer = Dark100,
    onTertiaryContainer = DarkText,

    background = Dark0,
    onBackground = DarkText,

    surface = Dark0,
    onSurface = DarkText,
    surfaceVariant = Dark50,
    onSurfaceVariant = Gray400,

    surfaceContainerLowest = Dark0,
    surfaceContainerLow = Dark50,
    surfaceContainer = Dark50,
    surfaceContainerHigh = Dark100,
    surfaceContainerHighest = Dark100,

    outline = Dark100,
    outlineVariant = Dark100,

    error = ErrorRedDark,
    onError = Dark0,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorRedDark,

    inverseSurface = DarkText,
    inverseOnSurface = Dark0,
    scrim = Scrim
)

@Composable
fun HelpumtaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // dynamicColor 파라미터를 제거했다.
    // 기기 배경화면 색을 따라가면 브랜드 색이 사라진다.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = HelpumtaShapes,
        content = content
    )
}