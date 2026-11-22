package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.repository.WorkoutTypeHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentWorkoutTypesUseCase @Inject constructor(
    private val repository: WorkoutTypeHistoryRepository
) {
    operator fun invoke(): Flow<List<String>> = repository.getRecentTypes()
}