package com.jinsupark.helpumta.ui.screen.group.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(36.dp)
    ) {
        // ── 이번 주: 시간이 주인공, 차트가 본체
        StatsSection(title = "이번 주") {
            Text(
                text = formatMinutes(stats.myWeeklyMinutesTotal),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (stats.myWeeklyDays == 0) "아직 기록이 없어요"
                else "${stats.myWeeklyDays}일 운동했어요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            WeeklyBarChart(
                minutesPerDay = stats.myWeeklyMinutes,
                todayIndex = todayIndex
            )
        }

        // ── 이번 달: 꾸준함이 주인공.
        //    주간과 같은 "시간+일수" 2개 메트릭을 쓰면 복사한 것처럼 보이므로
        //    큰 숫자를 일수로 바꾸고 시간을 보조로 내린다.
        StatsSection(title = "이번 달") {
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                MetricItem(value = "${stats.myMonthlyDays}일", label = "운동한 날")
                MetricItem(
                    value = formatMinutes(stats.myMonthlyMinutes),
                    label = "운동 시간"
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "최근 30일 중 ${stats.myActiveDaysLast30}일 운동했어요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ── 그룹원 비교
        StatsSection(title = "이번 주 그룹원") {
            if (stats.memberRanking.isEmpty()) {
                Text(
                    text = "아직 이번 주 기록이 없어요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val topMinutes = stats.memberRanking.first().totalMinutes.coerceAtLeast(1)
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
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
    }
}

/**
 * 카드를 쓰지 않는다.
 * 기록 탭에서 카드를 걷어냈는데 통계 탭만 회색 블록이 쌓이면 같은 화면의 두 탭이 따로 논다.
 * 구분은 섹션 제목과 여백이 맡는다.
 */
@Composable
private fun StatsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun MetricItem(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
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
    Row(verticalAlignment = Alignment.Top) {
        // 주황 원을 뺐다. 순위는 순서를 알려주는 정보지 강조할 대상이 아니다.
        Text(
            text = "$rank",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(20.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (member.isMe) "${member.userName} (나)" else member.userName,
                    // fontWeight를 덮어쓰지 않고 타이포 슬롯으로 구분한다.
                    // titleSmall(SemiBold)과 bodyMedium(Normal)은 둘 다 14sp.
                    style = if (member.isMe) MaterialTheme.typography.titleSmall
                    else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "${formatMinutes(member.totalMinutes)} · ${member.recordCount}회",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            RatioBar(
                ratio = ratio,
                // outlineVariant는 트랙(surfaceContainerHigh)과 명도가 거의 같아 채움이 사라진다.
                color = if (member.isMe) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )
        }
    }
}

/**
 * LinearProgressIndicator 대신 Box 두 개.
 * gapSize·drawStopIndicator 같은 파라미터가 Material3 버전마다 달라서
 * 단순한 막대 하나에 버전 의존성을 만들 이유가 없다.
 */
@Composable
private fun RatioBar(ratio: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(ratio.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(color)
        )
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