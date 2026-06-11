package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.model.AuthProvider
import com.jinsupark.helpumta.domain.repository.AuthRepository
import javax.inject.Inject

class GetAuthProviderUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): AuthProvider = authRepository.getAuthProvider()
}