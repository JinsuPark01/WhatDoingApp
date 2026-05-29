package com.example.whatdoing.domain.usecase

import com.example.whatdoing.domain.model.UserProfile
import com.example.whatdoing.domain.repository.AuthRepository
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