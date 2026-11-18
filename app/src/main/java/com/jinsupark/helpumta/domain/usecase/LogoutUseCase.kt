package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.repository.AuthRepository
import com.jinsupark.helpumta.domain.repository.UserRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    // 토큰 삭제 → 로그아웃 순서 고정.
    // signOut 후에는 uid를 못 꺼내므로 이 순서가 뒤바뀌면 토큰이 남는다.
    suspend operator fun invoke() {
        userRepository.clearFcmToken()  // 실패해도 로그아웃은 막지 않음
        authRepository.logout()
    }
}