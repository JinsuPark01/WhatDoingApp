package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.model.GroupStats
import com.jinsupark.helpumta.domain.model.MemberStat
import com.jinsupark.helpumta.domain.model.WorkoutRecord
import com.jinsupark.helpumta.domain.repository.AuthRepository
import com.jinsupark.helpumta.domain.repository.RecordRepository
import java.util.Calendar
import javax.inject.Inject

class GetGroupStatsUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(groupId: String): Result<GroupStats> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("로그인이 필요합니다"))

        val now = System.currentTimeMillis()
        // 최근 30일 + 이번 주/달을 모두 덮도록 넉넉히 조회 (쿼리 1회로 전 지표 계산)
        val rangeStart = minOf(
            startOfDayBefore(now, 29),
            startOfWeek(now),
            startOfMonth(now)
        )
        val rangeEnd = endOfDay(now)

        return recordRepository.getRecordsByGroup(groupId, rangeStart, rangeEnd)
            .map { records -> buildStats(records, userId, now) }
    }

    private fun buildStats(
        records: List<WorkoutRecord>,
        userId: String,
        now: Long
    ): GroupStats {
        val weekStart = startOfWeek(now)
        val monthStart = startOfMonth(now)
        val last30Start = startOfDayBefore(now, 29)

        val myRecords = records.filter { it.userId == userId }

        // 이번 주 요일별 (월=0 ... 일=6)
        val weekly = IntArray(7)
        myRecords.filter { it.createdAt >= weekStart }
            .forEach { record ->
                val index = dayIndexInWeek(record.createdAt)
                weekly[index] += record.workoutDuration
            }

        // 이번 달
        val monthRecords = myRecords.filter { it.createdAt >= monthStart }
        val monthlyMinutes = monthRecords.sumOf { it.workoutDuration }
        val monthlyDays = monthRecords.map { dayKey(it.createdAt) }.distinct().size

        // 최근 30일 중 운동한 날
        val activeDays = myRecords
            .filter { it.createdAt >= last30Start }
            .map { dayKey(it.createdAt) }
            .distinct()
            .size

        // 이번 주 그룹원 순위
        val ranking = records
            .filter { it.createdAt >= weekStart }
            .groupBy { it.userId }
            .map { (uid, list) ->
                MemberStat(
                    userId = uid,
                    userName = list.first().userName,
                    totalMinutes = list.sumOf { it.workoutDuration },
                    recordCount = list.size,
                    isMe = uid == userId
                )
            }
            .sortedByDescending { it.totalMinutes }

        return GroupStats(
            myWeeklyMinutes = weekly.toList(),
            myMonthlyMinutes = monthlyMinutes,
            myMonthlyDays = monthlyDays,
            myActiveDaysLast30 = activeDays,
            memberRanking = ranking
        )
    }

    // --- 날짜 헬퍼 ---

    private fun calendarAtStartOfDay(millis: Long): Calendar =
        Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    private fun endOfDay(millis: Long): Long =
        calendarAtStartOfDay(millis).apply { add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis

    private fun startOfDayBefore(millis: Long, days: Int): Long =
        calendarAtStartOfDay(millis).apply { add(Calendar.DAY_OF_MONTH, -days) }.timeInMillis

    // 주 시작 = 월요일
    private fun startOfWeek(millis: Long): Long =
        calendarAtStartOfDay(millis).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }.timeInMillis

    private fun startOfMonth(millis: Long): Long =
        calendarAtStartOfDay(millis).apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis

    // 월=0 ... 일=6
    private fun dayIndexInWeek(millis: Long): Int {
        val dow = Calendar.getInstance().apply { timeInMillis = millis }
            .get(Calendar.DAY_OF_WEEK)
        return (dow + 5) % 7   // SUNDAY(1)→6, MONDAY(2)→0 ...
    }

    private fun dayKey(millis: Long): String =
        Calendar.getInstance().apply { timeInMillis = millis }.let {
            "${it.get(Calendar.YEAR)}-${it.get(Calendar.DAY_OF_YEAR)}"
        }
}