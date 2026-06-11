package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.repository.AuthRepository
import com.jinsupark.helpumta.domain.repository.GroupRepository
import com.jinsupark.helpumta.domain.repository.RecordRepository
import javax.inject.Inject

class LeaveGroupUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val recordRepository: RecordRepository
) {
    suspend operator fun invoke(groupId: String): Result<Unit> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("로그인 상태가 아닙니다"))

        // 내 기록+이미지 먼저 삭제 → 그룹에서 나가기 (0명이면 그룹 삭제)
        recordRepository.deleteRecordsByUserInGroup(groupId, userId).getOrElse {
            return Result.failure(it)
        }
        return groupRepository.leaveGroup(groupId, userId)
    }
}