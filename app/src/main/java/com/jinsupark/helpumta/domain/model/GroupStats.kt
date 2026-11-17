package com.jinsupark.helpumta.domain.model

/**
 * 그룹 단위 통계.
 * 여러 그룹에 같은 운동을 기록할 수 있어 전체 합산은 의미가 없으므로 그룹 범위로 한정한다.
 */
data class GroupStats(
    // 내 이번 주 요일별 운동 시간 (월~일, 분 단위, 7개 고정)
    val myWeeklyMinutes: List<Int> = List(7) { 0 },
    // 내 이번 달 총 운동 시간 (분)
    val myMonthlyMinutes: Int = 0,
    // 내 이번 달 운동한 날 수
    val myMonthlyDays: Int = 0,
    // 최근 30일 중 내가 운동한 날 수
    val myActiveDaysLast30: Int = 0,
    // 이번 주 그룹원 순위 (운동 시간 내림차순)
    val memberRanking: List<MemberStat> = emptyList()
) {
    val myWeeklyMinutesTotal: Int get() = myWeeklyMinutes.sum()
    val myWeeklyDays: Int get() = myWeeklyMinutes.count { it > 0 }
}

data class MemberStat(
    val userId: String,
    val userName: String,
    val totalMinutes: Int,
    val recordCount: Int,
    val isMe: Boolean
)