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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DAY_LABELS = listOf("월", "화", "수", "목", "금", "토", "일")

@Composable
fun WeeklyBarChart(
    minutesPerDay: List<Int>,        // 월~일 7개
    todayIndex: Int,                 // 오늘 요일 (월=0)
    modifier: Modifier = Modifier
) {
    val maxMinutes = (minutesPerDay.maxOrNull() ?: 0).coerceAtLeast(1)
    val barColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        minutesPerDay.forEachIndexed { index, minutes ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 값 (0이면 생략)
                Text(
                    text = if (minutes > 0) "$minutes" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )

                Spacer(Modifier.height(4.dp))

                // 막대
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
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

                Spacer(Modifier.height(6.dp))

                // 요일 (오늘 강조)
                Text(
                    text = DAY_LABELS[index],
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (index == todayIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (index == todayIndex) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}