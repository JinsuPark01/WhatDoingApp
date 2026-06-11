package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.repository.AuthRepository
import javax.inject.Inject

class ReauthenticateUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend fun withPassword(password: String): Result<Unit> =
        authRepository.reauthenticateWithPassword(password)

    suspend fun withGoogle(idToken: String): Result<Unit> =
        authRepository.reauthenticateWithGoogle(idToken)
}