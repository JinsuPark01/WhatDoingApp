package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.model.User
import com.jinsupark.helpumta.domain.repository.AuthRepository
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