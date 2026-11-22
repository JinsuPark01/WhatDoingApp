package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.model.Reaction
import com.jinsupark.helpumta.domain.repository.AuthRepository
import com.jinsupark.helpumta.domain.repository.RecordRepository
import javax.inject.Inject

class SetReactionUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val authRepository: AuthRepository
) {
    // reaction이 null이면 내 리액션 취소
    suspend operator fun invoke(recordId: String, reaction: Reaction?): Result<Unit> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("로그인이 필요합니다"))
        return recordRepository.setReaction(recordId, userId, reaction)
    }
}