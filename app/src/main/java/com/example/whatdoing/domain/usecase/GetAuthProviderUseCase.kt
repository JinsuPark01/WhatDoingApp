package com.example.whatdoing.domain.usecase

import com.example.whatdoing.domain.model.AuthProvider
import com.example.whatdoing.domain.repository.AuthRepository
import javax.inject.Inject

class GetAuthProviderUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): AuthProvider = authRepository.getAuthProvider()
}