package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.repository.AuthRepository
import com.jinsupark.helpumta.domain.repository.UserRepository
import javax.inject.Inject

class SaveUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    // 현재 로그인된 사용자 정보를 users에 upsert.
    // 이메일은 저장하지 않는다 — users는 로그인한 누구나 읽을 수 있고,
    // 이메일이 필요한 곳(마이페이지)은 Auth에서 직접 꺼낸다.
    suspend operator fun invoke(): Result<Unit> {
        val uid = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("로그인 상태가 아닙니다"))
        val nickname = authRepository.getCurrentUserName() ?: "사용자"
        return userRepository.saveUser(uid, nickname)
    }
}