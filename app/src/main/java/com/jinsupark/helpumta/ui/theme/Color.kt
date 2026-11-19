package com.jinsupark.helpumta.ui.theme

import androidx.compose.ui.graphics.Color

// ───────── 브랜드 ─────────
// 라이트·다크 모두 같은 값. 모드에 따라 색이 변하면 브랜드 정체성이 사라진다.
// (기존엔 다크에서 주황이 분홍으로 바뀌고 있었다)
val Orange = Color(0xFFFD6F22)
val OrangePressed = Color(0xFFE05E15)
val OrangeTintLight = Color(0xFFFFF1E9)   // 라이트: 아주 옅은 주황 면
val OrangeTintDark = Color(0xFF3A2216)    // 다크: 대응되는 어두운 면

// ───────── 중립 (라이트) ─────────
// 살짝 쿨한 회색. 따뜻한 회색을 쓰면 주황과 섞여 베이지처럼 보인다.
val White = Color(0xFFFFFFFF)
val Gray50 = Color(0xFFF7F7F8)    // 카드·입력 필드 배경
val Gray100 = Color(0xFFECECEF)   // 아주 옅은 경계
val Gray200 = Color(0xFFDDDEE2)   // 경계
val Gray400 = Color(0xFF9A9DA4)   // 비활성 텍스트
val Gray600 = Color(0xFF6E7177)   // 보조 텍스트
val Gray900 = Color(0xFF17181C)   // 본문 텍스트 (순수 검정 아님)

// ───────── 중립 (다크) ─────────
val Dark0 = Color(0xFF0F1012)     // 배경
val Dark50 = Color(0xFF1A1B1E)    // 카드·면
val Dark100 = Color(0xFF2A2B2F)   // 경계
val DarkText = Color(0xFFF2F3F5)  // 본문 텍스트

// ───────── 상태 ─────────
val ErrorRed = Color(0xFFE5484D)
val ErrorRedDark = Color(0xFFFF6369)
val ErrorContainerLight = Color(0xFFFFE8E8)
val OnErrorContainerLight = Color(0xFF8E1116)
val ErrorContainerDark = Color(0xFF3D1517)

val Scrim = Color(0x99000000)

// ───────── 레거시 (2단계에서 제거 예정) ─────────
// GroupDetailScreen의 FAB가 '오늘 이미 작성함' 상태를 표현하는 데 쓴다.
// 고정값이라 다크모드를 무시한다 → 테마 색(surfaceContainerHigh)으로 교체할 것
val DisabledGray = Gray100
val OnDisabledGray = Gray400