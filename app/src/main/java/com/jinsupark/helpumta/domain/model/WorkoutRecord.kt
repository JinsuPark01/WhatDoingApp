package com.jinsupark.helpumta.domain.model

data class WorkoutRecord(
    val id: String,
    val groupId: String,
    val userId: String,
    val userName: String,
    val workoutType: String,
    val workoutDuration: Int, // 분 단위
    val imageUrl: String,
    val comment: String,
    val createdAt: Long,
    // uid → 반응. 기본값을 둬서 기존 생성 코드(프리뷰 등)가 깨지지 않게
    val reactions: Map<String, Reaction> = emptyMap()
)