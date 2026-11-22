package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.repository.WorkoutTypeHistoryRepository
import javax.inject.Inject

class AddRecentWorkoutTypeUseCase @Inject constructor(
    private val repository: WorkoutTypeHistoryRepository
) {
    suspend operator fun invoke(type: String) = repository.addRecentType(type)
}