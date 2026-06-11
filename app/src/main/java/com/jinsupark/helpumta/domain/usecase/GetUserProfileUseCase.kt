package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.model.UserProfile
import com.jinsupark.helpumta.domain.repository.AuthRepository
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): UserProfile? {
        val email = authRepository.getCurrentUserEmail() ?: return null
        val nickname = authRepository.getCurrentUserName() ?: "사용자"
        return UserProfile(nickname = nickname, email = email)
    }
}