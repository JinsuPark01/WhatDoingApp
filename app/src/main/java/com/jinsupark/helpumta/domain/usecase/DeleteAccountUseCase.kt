package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.repository.AuthRepository
import com.jinsupark.helpumta.domain.repository.GroupRepository
import com.jinsupark.helpumta.domain.repository.UserRepository
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val leaveGroupUseCase: LeaveGroupUseCase
) {
    /**
     * 앱에서 바로 정리하고, 중간에 끊겨 남은 건 서버(cleanupDeletedUser)가 마무리한다.
     */
    suspend operator fun invoke(): Result<Unit> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("로그인 상태가 아닙니다"))

        // 1. 가입한 모든 그룹 조회
        val groups = groupRepository.getGroups(userId).getOrElse {
            return Result.failure(it)
        }

        // 2. 그룹마다 나가기 — 순서(리액션 → 내 기록 → 탈퇴)는 LeaveGroupUseCase 한 곳에서만 정한다.
        //    여기서 Repository를 직접 부르면 나가기 순서가 두 군데로 갈라져 한쪽만 고쳐지게 된다.
        for (group in groups) {
            leaveGroupUseCase(group.id).getOrElse {
                return Result.failure(it)
            }
        }

        // 3. users 문서 삭제 (Auth 삭제 전에 — 삭제 후엔 규칙을 통과할 수 없다)
        userRepository.deleteUser(userId).getOrElse {
            return Result.failure(it)
        }

        // 4. 마지막에 Auth 계정 삭제
        return authRepository.deleteAccount()
    }
}