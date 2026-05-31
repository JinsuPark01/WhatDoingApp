package com.example.whatdoing.domain.usecase

import com.example.whatdoing.domain.repository.AuthRepository
import javax.inject.Inject

class CheckAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Boolean = authRepository.getCurrentUserId() != null
}