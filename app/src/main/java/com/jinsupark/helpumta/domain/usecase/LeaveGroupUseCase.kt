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
    /**
     * 순서 고정: 리액션 → 내 기록 → 그룹에서 나가기 (0명이면 그룹 삭제)
     *
     * 리액션 삭제 규칙은 '그 그룹 멤버'를 확인하므로 반드시 나가기 전에.
     * 앞 단계가 실패하면 나가지 않는다 — 나간 뒤엔 남은 리액션을 지울 권한이 없다.
     * (다시 시도하면 이미 지운 건 건너뛰고 이어서 진행된다)
     */
    suspend operator fun invoke(groupId: String): Result<Unit> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("로그인 상태가 아닙니다"))

        recordRepository.removeReactionsByUserInGroup(groupId, userId).getOrElse {
            return Result.failure(it)
        }
        recordRepository.deleteRecordsByUserInGroup(groupId, userId).getOrElse {
            return Result.failure(it)
        }
        return groupRepository.leaveGroup(groupId, userId)
    }
}