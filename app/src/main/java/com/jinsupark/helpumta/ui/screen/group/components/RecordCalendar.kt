package com.jinsupark.helpumta.ui.screen.group.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jinsupark.helpumta.ui.theme.HelpumtaTheme
import java.util.Calendar

private val WEEK_LABELS = listOf("월", "화", "수", "목", "금", "토", "일")

/**
 * 기록 표시가 들어간 월 단위 캘린더.
 *
 * Material3 DatePicker는 개별 날짜를 꾸미는 슬롯이 없어서 직접 그린다.
 * 주 시작은 월요일 — 통계(GetGroupStatsUseCase)와 기준을 맞춘다.
 */
@Composable
fun RecordCalendarDialog(
    selectedDate: Long,          // 선택된 날짜 (로컬 00:00 millis)
    displayedMonth: Long,        // 표시 중인 달의 1일 00:00 millis
    markedDays: Set<Int>,        // 이 달에서 내 기록이 있는 일(1~31)
    isLoading: Boolean,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        },
        text = {
            CalendarBody(
                selectedDate = selectedDate,
                displayedMonth = displayedMonth,
                markedDays = markedDays,
                isLoading = isLoading,
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
                onSelectDate = onSelectDate
            )
        }
    )
}

@Composable
private fun CalendarBody(
    selectedDate: Long,
    displayedMonth: Long,
    markedDays: Set<Int>,
    isLoading: Boolean,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (Long) -> Unit
) {
    val cal = Calendar.getInstance().apply { timeInMillis = displayedMonth }
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    // 1일의 요일을 월=0 ... 일=6 으로 변환
    val firstDayOffset = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7

    val todayStart = startOfDayLocal(System.currentTimeMillis())
    val thisMonthStart = startOfMonthLocal(System.currentTimeMillis())
    val isCurrentMonth = displayedMonth >= thisMonthStart

    Column(modifier = Modifier.fillMaxWidth()) {

        // 월 네비게이션
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrevMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "이전 달"
                )
            }
            Text(
                text = "${year}년 ${month + 1}월",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onNextMonth,
                enabled = !isCurrentMonth   // 미래 달 차단
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "다음 달"
                )
            }
        }

        // 로딩 표시 — 자리를 차지해서 그리드가 흔들리지 않게 높이를 고정
        Box(modifier = Modifier.fillMaxWidth().height(3.dp)) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 요일 헤더
        Row(modifier = Modifier.fillMaxWidth()) {
            WEEK_LABELS.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 날짜 그리드 — 앞쪽 빈칸(null) + 1..말일
        val cells: List<Int?> = List(firstDayOffset) { null } + (1..daysInMonth).toList()

        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    if (day == null) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val dayMillis = dayMillisLocal(year, month, day)
                        DayCell(
                            day = day,
                            isSelected = dayMillis == selectedDate,
                            isToday = dayMillis == todayStart,
                            isFuture = dayMillis > todayStart,
                            hasRecord = markedDays.contains(day),
                            onClick = { onSelectDate(dayMillis) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                // 마지막 주가 7칸이 안 되면 빈칸으로 채워 정렬 유지
                repeat(7 - week.size) {
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 범례
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "내가 기록한 날",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    hasRecord: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(enabled = !isFuture, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 점은 항상 자리를 차지한다 — 있고 없고에 따라 날짜가 위아래로 움직이지 않게
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            !hasRecord -> Color.Transparent
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
            )
        }
    }
}

// --- 날짜 헬퍼 (표시 전용) ---

private fun startOfDayLocal(millis: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun startOfMonthLocal(millis: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun dayMillisLocal(year: Int, month: Int, day: Int): Long =
    Calendar.getInstance().apply {
        clear()
        set(year, month, day, 0, 0, 0)
    }.timeInMillis

@Preview(showBackground = true)
@Composable
private fun RecordCalendarPreview() {
    val today = System.currentTimeMillis()
    HelpumtaTheme {
        Surface {
            CalendarBody(
                selectedDate = startOfDayLocal(today),
                displayedMonth = startOfMonthLocal(today),
                markedDays = setOf(3, 4, 7, 11, 12, 15, 18),
                isLoading = false,
                onPrevMonth = {},
                onNextMonth = {},
                onSelectDate = {}
            )
        }
    }
}