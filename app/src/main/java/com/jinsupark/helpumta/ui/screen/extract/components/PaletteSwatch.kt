package com.jinsupark.helpumta.ui.screen.extract.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jinsupark.helpumta.domain.model.ExtractPalette
import com.jinsupark.helpumta.domain.model.ExtractTemplate

/**
 * 색 조합 하나 = 동그라미 하나. 배경색을 채우고 "가"를 글씨색으로.
 * 캔버스와 같은 매핑을 써서 카드에선 베이지/차콜로 보인다 — 고른 색과 결과가 같아야 한다.
 */
@Composable
fun PaletteSwatch(
    palette: ExtractPalette,
    template: ExtractTemplate,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ringColor = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            // 선택 표시 — 바깥 링
            .border(2.dp, ringColor, CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(palette.background.backgroundColor(template))
            // 흰·베이지 동그라미가 밝은 화면 배경에 묻히지 않게 옅은 테두리
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "가",
            color = palette.text.textColor(),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}