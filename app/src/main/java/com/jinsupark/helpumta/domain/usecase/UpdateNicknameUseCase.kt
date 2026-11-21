package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.repository.AuthRepository
import com.jinsupark.helpumta.domain.repository.RecordRepository
import com.jinsupark.helpumta.domain.repository.UserRepository
import javax.inject.Inject

/**
 * 닉네임 원본은 Auth displayName이고, users.nickname / records.userName은 복사본이다.
 * 원본 → 복사본 순서로 맞추므로 중간에 실패해도 같은 값으로 다시 저장하면 전부 맞춰진다.
 * (users만 바꾸면 다음 로그인 때 SaveUserUseCase가 Auth 값으로 되돌린다)
 */
class UpdateNicknameUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val recordRepository: RecordRepository
) {
    suspend operator fun invoke(nickname: String): Result<Unit> {
        val uid = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("로그인이 필요합니다"))

        val trimmed = nickname.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(Exception("닉네임을 입력해주세요"))
        }

        authRepository.updateDisplayName(trimmed).getOrElse { return Result.failure(it) }
        userRepository.updateNickname(uid, trimmed).getOrElse { return Result.failure(it) }
        return recordRepository.updateUserNameInRecords(uid, trimmed)
    }
}