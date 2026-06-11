package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.model.User
import com.jinsupark.helpumta.domain.repository.AuthRepository
import javax.inject.Inject

class GoogleLoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): Result<User> {
        return authRepository.googleLogin(idToken)
    }
}