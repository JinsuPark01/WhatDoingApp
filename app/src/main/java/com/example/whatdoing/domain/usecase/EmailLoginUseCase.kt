package com.example.whatdoing.domain.usecase

import com.example.whatdoing.domain.repository.AuthRepository
import javax.inject.Inject

class EmailLoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String) =
        authRepository.emailLogin(email, password)
}