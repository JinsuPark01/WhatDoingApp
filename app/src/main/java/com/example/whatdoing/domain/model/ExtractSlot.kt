package com.example.whatdoing.domain.model

sealed interface ExtractSlot {
    val nickname: String

    // 기록한 사람
    data class Recorded(
        override val nickname: String,
        val workoutType: String,
        val workoutDuration: Int,
        val comment: String,
        val imageUrl: String
    ) : ExtractSlot

    // 미기록자 (zzz)
    data class Empty(
        override val nickname: String
    ) : ExtractSlot
}