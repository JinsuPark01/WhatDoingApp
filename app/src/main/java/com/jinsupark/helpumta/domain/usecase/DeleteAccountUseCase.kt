package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.repository.AuthRepository
import com.jinsupark.helpumta.domain.repository.GroupRepository
import com.jinsupark.helpumta.domain.repository.RecordRepository
import com.jinsupark.helpumta.domain.repository.UserRepository
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val recordRepository: RecordRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("로그인 상태가 아닙니다"))

        // 1. 가입한 모든 그룹 조회
        val groups = groupRepository.getGroups(userId).getOrElse {
            return Result.failure(it)
        }

        // 2. 그룹별로 내 기록+이미지 삭제 → 그룹에서 나가기
        for (group in groups) {
            recordRepository.deleteRecordsByUserInGroup(group.id, userId).getOrElse {
                return Result.failure(it)
            }
            groupRepository.leaveGroup(group.id, userId).getOrElse {
                return Result.failure(it)
            }
        }

        // 3. users 문서 삭제 (Auth 삭제 전에 — 삭제 후엔 uid 못 씀)
        userRepository.deleteUser(userId).getOrElse {
            return Result.failure(it)
        }

        // 4. 마지막에 Auth 계정 삭제
        return authRepository.deleteAccount()
    }
}