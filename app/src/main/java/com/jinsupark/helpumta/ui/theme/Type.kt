package com.jinsupark.helpumta.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.jinsupark.helpumta.R

/**
 * Pretendard (SIL Open Font License 1.1)
 *
 * weight는 400 / 600 / 700 세 단계만 등록한다.
 * 500(Medium)은 한글 본문 크기에서 400과 거의 구분되지 않아 용량만 늘린다.
 *
 * 등록하지 않은 weight를 요청하면 Compose가 가장 가까운 것으로 합성해서
 * 의도와 다르게 렌더링되므로, 아래 스케일은 세 단계만 사용한다.
 */
private val AppFont = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold)
)

/**
 * 한글 UI 기준으로 조정한 스케일.
 *
 * Material3 기본값은 letterSpacing이 +0.5sp 등 양수인데,
 * 라틴 문자 기준이라 한글에 쓰면 글자가 흩어져 보인다. 0 또는 음수로 낮췄다.
 *
 * 크기 차이도 의도적으로 벌렸다. 기존 화면이 밋밋했던 이유 중 하나가
 * 제목과 본문의 크기·굵기 차이가 거의 없어서였다.
 *
 * 15개 슬롯을 전부 명시한다. 하나라도 비우면 그 슬롯은 M3 기본값으로 남아
 * 시스템 폰트가 섞여 나온다.
 */
private val KoreanLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

val Typography = Typography(
    // ── 거의 쓰지 않지만, 시스템 폰트 혼입을 막기 위해 채워둔다
    displayLarge = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.0).sp,
        lineHeightStyle = KoreanLineHeight
    ),
    displayMedium = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.9).sp,
        lineHeightStyle = KoreanLineHeight
    ),

    // ── 화면 최상단 큰 제목
    displaySmall = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp,
        lineHeightStyle = KoreanLineHeight
    ),
    headlineLarge = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.6).sp,
        lineHeightStyle = KoreanLineHeight
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp,
        lineHeightStyle = KoreanLineHeight
    ),
    headlineSmall = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp,
        lineHeightStyle = KoreanLineHeight
    ),

    // ── 섹션 제목·카드 제목
    titleLarge = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
        lineHeightStyle = KoreanLineHeight
    ),
    titleMedium = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp,
        lineHeightStyle = KoreanLineHeight
    ),
    // Medium(500) → SemiBold(600).
    // bodyMedium과 크기가 같은 14sp라서 굵기만으로 구분해야 한다.
    titleSmall = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = KoreanLineHeight
    ),

    // ── 본문
    bodyLarge = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = KoreanLineHeight
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = KoreanLineHeight
    ),
    bodySmall = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = KoreanLineHeight
    ),

    // ── 버튼·라벨
    labelLarge = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = KoreanLineHeight
    ),
    // Medium(500) → SemiBold(600). 작은 버튼·칩 텍스트용 강조
    labelMedium = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = KoreanLineHeight
    ),
    // Medium(500) → Normal(400).
    // 12sp 회색 보조 텍스트에 굵기를 주면 지저분해 보인다.
    labelSmall = TextStyle(
        fontFamily = AppFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = KoreanLineHeight
    )
)