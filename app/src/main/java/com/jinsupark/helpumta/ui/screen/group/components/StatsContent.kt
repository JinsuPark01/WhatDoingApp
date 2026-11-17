package com.jinsupark.helpumta.ui.screen.group.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jinsupark.helpumta.domain.model.GroupStats
import com.jinsupark.helpumta.domain.model.MemberStat

@Composable
fun StatsContent(
    stats: GroupStats,
    todayIndex: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 이번 주
        StatsCard(title = "이번 주") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                MetricItem("운동 시간", formatMinutes(stats.myWeeklyMinutesTotal))
                MetricItem("운동한 날", "${stats.myWeeklyDays}일")
            }

            Spacer(Modifier.height(16.dp))

            WeeklyBarChart(
                minutesPerDay = stats.myWeeklyMinutes,
                todayIndex = todayIndex
            )
        }

        // 이번 달 + 최근 30일
        StatsCard(title = "이번 달") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                MetricItem("운동 시간", formatMinutes(stats.myMonthlyMinutes))
                MetricItem("운동한 날", "${stats.myMonthlyDays}일")
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text(
                text = "최근 30일 중 ${stats.myActiveDaysLast30}일 운동했어요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 그룹원 비교
        StatsCard(title = "이번 주 그룹원") {
            if (stats.memberRanking.isEmpty()) {
                Text(
                    text = "아직 이번 주 기록이 없어요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val topMinutes = stats.memberRanking.first().totalMinutes.coerceAtLeast(1)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    stats.memberRanking.forEachIndexed { index, member ->
                        MemberRankRow(
                            rank = index + 1,
                            member = member,
                            ratio = member.totalMinutes.toFloat() / topMinutes
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(72.dp))   // FAB 영역 여백
    }
}

@Composable
private fun StatsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MemberRankRow(
    rank: Int,
    member: MemberStat,
    ratio: Float
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // 순위
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (member.isMe) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (member.isMe) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (member.isMe) "${member.userName} (나)" else member.userName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (member.isMe) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "${formatMinutes(member.totalMinutes)} · ${member.recordCount}회",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))

            // 비율 바
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (member.isMe) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }
    }
}

private fun formatMinutes(minutes: Int): String =
    if (minutes >= 60) {
        val h = minutes / 60
        val m = minutes % 60
        if (m == 0) "${h}시간" else "${h}시간 ${m}분"
    } else {
        "${minutes}분"
    }