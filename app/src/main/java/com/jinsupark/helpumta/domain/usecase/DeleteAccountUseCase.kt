package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.repository.AuthRepository
import com.jinsupark.helpumta.domain.repository.GroupRepository
import com.jinsupark.helpumta.domain.repository.RecordRepository
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val recordRepository: RecordRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("로그인 상태가 아닙니다"))

        // 1. 가입한 모든 그룹 조회
        val groups = groupRepository.getGroups(userId).getOrElse {
            return Result.failure(it)
        }

        // 2. 그룹별로 내 기록+이미지 삭제 → 그룹에서 나가기
        //    (A) 방식: 중간 실패 시 즉시 중단. 멱등하게 짜여 재시도하면 이어짐
        for (group in groups) {
            recordRepository.deleteRecordsByUserInGroup(group.id, userId).getOrElse {
                return Result.failure(it)
            }
            groupRepository.leaveGroup(group.id, userId).getOrElse {
                return Result.failure(it)
            }
        }

        // 3. 마지막에 Auth 계정 삭제 (재인증은 호출 전에 화면에서 완료된 상태여야 함)
        return authRepository.deleteAccount()
    }
}