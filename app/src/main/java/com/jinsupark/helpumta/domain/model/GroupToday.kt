package com.jinsupark.helpumta.domain.model

/**
 * 그룹 + 오늘 현황.
 *
 * Group에 필드를 추가하지 않고 분리한 이유:
 * Group은 조회 시점과 무관한 정보만 담는다. "오늘 누가 썼는지"는 자정이 지나면
 * 같은 Group이라도 값이 달라지므로, 섞으면 캐시나 비교 로직에서 사고가 난다.
 */
data class GroupToday(
    val group: Group,
    val wroteCount: Int,
    val didIWrite: Boolean
)