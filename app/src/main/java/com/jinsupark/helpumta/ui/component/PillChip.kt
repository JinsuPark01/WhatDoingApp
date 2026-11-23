package com.jinsupark.helpumta.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * 앱 공용 선택 칩(알약).
 * Material FilterChip 대신 직접 그린다 — 기본 칩은 테두리·체크 아이콘이 붙어 무게가 크다.
 * 선택 표시는 주황이 아니라 onSurface 반전 — 주황은 주요 버튼 몫이다.
 *
 * enabled = false: 선택 상태는 그대로 보여주되 흐리게 + 누를 수 없음.
 * (다른 옵션에 묶여 지금은 적용되지 않는 설정 — 묶인 옵션을 다시 켜면 그 상태로 돌아온다)
 */
@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = modifier
            .alpha(if (enabled) 1f else 0.38f)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}