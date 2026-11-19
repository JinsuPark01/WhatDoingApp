package com.jinsupark.helpumta.widget

import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import com.jinsupark.helpumta.ui.theme.DarkColorScheme
import com.jinsupark.helpumta.ui.theme.LightColorScheme

/**
 * 위젯 색을 앱 테마에 묶는다.
 *
 * 인자 없는 GlanceTheme은 Android 12+에서 시스템 동적 색상을 쓴다.
 * 그래서 사용자의 배경화면 색이 위젯에 그대로 들어와, 하늘 사진을 깔아둔 기기에서는
 * 헬품타 위젯이 파란색으로 나왔다. 앱 테마에서 dynamicColor를 끈 것과 같은 이유로 끈다.
 */
@Composable
fun HelpumtaWidgetTheme(content: @Composable () -> Unit) {
    GlanceTheme(
        colors = ColorProviders(light = LightColorScheme, dark = DarkColorScheme)
    ) {
        content()
    }
}