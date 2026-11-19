package com.jinsupark.helpumta.ui.screen.group.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

private val DAY_LABELS = listOf("월", "화", "수", "목", "금", "토", "일")

@Composable
fun WeeklyBarChart(
    minutesPerDay: List<Int>,        // 월~일 7개
    todayIndex: Int,                 // 오늘 요일 (월=0)
    modifier: Modifier = Modifier
) {
    val maxMinutes = (minutesPerDay.maxOrNull() ?: 0).coerceAtLeast(1)
    val barColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Row(
        // 높이를 고정하지 않는다.
        // 160dp로 묶어두면 내용(약 128dp)보다 커서 위쪽에 빈 공간이 남는다.
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        minutesPerDay.forEachIndexed { index, minutes ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 값 (0이면 자리만 차지)
                Text(
                    text = if (minutes > 0) "$minutes" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(6.dp))

                // 막대
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    val ratio = minutes.toFloat() / maxMinutes
                    val barHeight = (size.height * ratio).coerceAtLeast(
                        if (minutes > 0) 6.dp.toPx() else 4.dp.toPx()
                    )
                    val barWidth = size.width * 0.7f
                    val left = (size.width - barWidth) / 2f

                    drawRoundRect(
                        color = if (minutes > 0) barColor else emptyColor,
                        topLeft = Offset(left, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 3f)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // 요일 — 오늘은 굵기로만 구분한다.
                // 주황은 막대가 이미 쓰고 있어서, 레이블까지 주황이면 화면에 포인트가 흩어진다.
                // 두 슬롯 모두 13sp라 굵기만 달라지고 베이스라인은 흔들리지 않는다.
                Text(
                    text = DAY_LABELS[index],
                    style = if (index == todayIndex) {
                        MaterialTheme.typography.labelMedium   // 13sp SemiBold
                    } else {
                        MaterialTheme.typography.bodySmall     // 13sp Normal
                    },
                    color = if (index == todayIndex) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}