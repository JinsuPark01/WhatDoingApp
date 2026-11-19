package com.jinsupark.helpumta.ui.screen.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 오늘 그룹원 중 몇 명이 기록했는지.
 *
 * 점은 무채색으로 둔다. 주황은 "내가 아직 안 썼다"는 뱃지 하나에만 쓴다 —
 * 그룹이 네 개면 주황 점이 열여섯 개가 되어 아무것도 강조되지 않는다.
 *
 * @param onImage 사진 위에 얹히는 경우. 배경이 어두워지므로 흰 계열로 전환한다.
 */
@Composable
fun TodayDots(
    wroteCount: Int,
    memberCount: Int,
    modifier: Modifier = Modifier,
    onImage: Boolean = false
) {
    val filled = if (onImage) Color.White
    else MaterialTheme.colorScheme.onSurfaceVariant

    // outlineVariant는 카드 배경(surfaceContainerLow)과 명도가 거의 같아서
    // 빈 점이 통째로 사라진다. onSurfaceVariant를 흐리게 쓰면
    // 채운 점과 같은 색상 계열이라 "켜짐/꺼짐"으로 읽힌다.
    val empty = if (onImage) Color.White.copy(alpha = 0.45f)
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)

    val textColor = if (onImage) Color.White.copy(alpha = 0.9f)
    else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(memberCount.coerceIn(0, 8)) { index ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (index < wroteCount) filled else empty)
            )
        }

        Text(
            // "1/4"만 있으면 무슨 수인지 알 수 없다. 멤버 수를 빼고 날짜 맥락을 넣는다.
            text = "오늘 $wroteCount/$memberCount",
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(start = 3.dp)
        )
    }
}