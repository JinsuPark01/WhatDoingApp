package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.repository.UserRepository
import javax.inject.Inject

class RegisterFcmTokenUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<Unit> = userRepository.registerFcmToken()
}