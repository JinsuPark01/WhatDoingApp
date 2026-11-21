package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.repository.AuthRepository
import com.jinsupark.helpumta.domain.repository.UserRepository
import javax.inject.Inject

class GetMyPhotoUrlUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<String> {
        val uid = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("로그인이 필요합니다"))
        return userRepository.getProfiles(listOf(uid)).map { it[uid]?.photoUrl.orEmpty() }
    }
}