package com.example.whatdoing.domain.usecase

import com.example.whatdoing.domain.repository.AuthRepository
import com.example.whatdoing.domain.repository.UserRepository
import javax.inject.Inject

class SaveUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    // 현재 로그인된 사용자 정보를 users에 upsert
    suspend operator fun invoke(): Result<Unit> {
        val uid = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("로그인 상태가 아닙니다"))
        val nickname = authRepository.getCurrentUserName() ?: "사용자"
        val email = authRepository.getCurrentUserEmail() ?: ""
        return userRepository.saveUser(uid, nickname, email)
    }
}