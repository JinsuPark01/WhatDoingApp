package com.example.whatdoing.domain.usecase

import com.example.whatdoing.domain.model.User
import com.example.whatdoing.domain.repository.AuthRepository
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        nickname: String
    ): Result<User> = authRepository.signUp(email, password, nickname)
}