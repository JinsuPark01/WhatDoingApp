package com.example.whatdoing.domain.usecase

import com.example.whatdoing.domain.repository.AuthRepository
import com.example.whatdoing.domain.repository.RecordRepository
import javax.inject.Inject

class UpdateRecordUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        recordId: String,
        workoutType: String,
        workoutDuration: Int,
        imageUri: String?,
        comment: String
    ): Result<Unit> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("로그인이 필요합니다"))

        return recordRepository.updateRecord(
            recordId = recordId,
            currentUserId = userId,
            workoutType = workoutType,
            workoutDuration = workoutDuration,
            imageUri = imageUri,
            comment = comment
        )
    }
}