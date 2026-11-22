package com.jinsupark.helpumta.domain.repository

import kotlinx.coroutines.flow.Flow

interface WorkoutTypeHistoryRepository {
    // 최근 입력한 운동 종류 (최근 순)
    fun getRecentTypes(): Flow<List<String>>

    suspend fun addRecentType(type: String)
}