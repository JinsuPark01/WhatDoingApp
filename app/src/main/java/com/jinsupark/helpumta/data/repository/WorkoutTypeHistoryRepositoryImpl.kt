package com.jinsupark.helpumta.data.repository

import com.jinsupark.helpumta.data.local.WorkoutHistoryDataStore
import com.jinsupark.helpumta.domain.repository.WorkoutTypeHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 기기 로컬(DataStore)에 저장한다.
 * Firestore에서 내 기록 전체를 읽어 뽑으면 기록이 쌓일수록 읽기 비용이 늘어난다.
 * 추천 목록은 잃어도 되는 편의 기능이라 재설치·기기 변경 시 초기화되는 쪽을 택했다.
 */
class WorkoutTypeHistoryRepositoryImpl @Inject constructor(
    private val workoutHistoryDataStore: WorkoutHistoryDataStore
) : WorkoutTypeHistoryRepository {

    override fun getRecentTypes(): Flow<List<String>> =
        workoutHistoryDataStore.recentWorkoutTypes

    override suspend fun addRecentType(type: String) {
        workoutHistoryDataStore.addRecentWorkoutType(type)
    }
}