package com.example.whatdoing.domain.usecase

import com.example.whatdoing.domain.model.User
import com.example.whatdoing.domain.repository.AuthRepository
import javax.inject.Inject

class GoogleLoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): Result<User> {
        return authRepository.googleLogin(idToken)
    }
}