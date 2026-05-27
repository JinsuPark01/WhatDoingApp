package com.example.whatdoing.domain.usecase

import com.example.whatdoing.domain.repository.AuthRepository
import com.example.whatdoing.domain.repository.RecordRepository
import javax.inject.Inject

class CreateRecordUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        groupId: String,
        workoutType: String,
        workoutDuration: Int,
        imageUri: String?,
        comment: String
    ): Result<String> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("로그인이 필요합니다"))

        val userName = authRepository.getCurrentUserName() ?: ""

        return recordRepository.createRecord(
            groupId = groupId,
            userId = userId,
            userName = userName,
            workoutType = workoutType,
            workoutDuration = workoutDuration,
            imageUri = imageUri,
            comment = comment
        )
    }
}