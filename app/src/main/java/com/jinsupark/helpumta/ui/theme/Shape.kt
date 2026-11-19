package com.jinsupark.helpumta.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// 기본값(4/8/12/16/28)보다 전반적으로 크게.
// 모서리가 둥글수록 부드럽고 최근 앱처럼 보인다.
val HelpumtaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),   // 카드·입력 필드
    large = RoundedCornerShape(20.dp),    // 큰 카드·시트
    extraLarge = RoundedCornerShape(28.dp)
)