package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.repository.AuthRepository
import com.jinsupark.helpumta.domain.repository.RecordRepository
import javax.inject.Inject

class GetMyRecordDatesUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        groupId: String,
        startMillis: Long,
        endMillis: Long
    ): Result<List<Long>> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("로그인이 필요합니다"))

        return recordRepository.getMyRecordDates(groupId, userId, startMillis, endMillis)
    }
}