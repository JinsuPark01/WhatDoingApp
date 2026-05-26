package com.example.whatdoing.domain.model

data class WorkoutRecord(
    val id: String,
    val groupId: String,
    val userId: String,
    val userName: String,
    val workoutType: String,
    val workoutDuration: Int, // 분 단위
    val imageUrl: String,
    val comment: String,
    val createdAt: Long
)