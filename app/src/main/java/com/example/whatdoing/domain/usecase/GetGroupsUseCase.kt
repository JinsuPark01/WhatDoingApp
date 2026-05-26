package com.example.whatdoing.domain.usecase

import com.example.whatdoing.domain.repository.AuthRepository
import com.example.whatdoing.domain.repository.GroupRepository
import javax.inject.Inject

class GetGroupsUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<List<com.example.whatdoing.domain.model.Group>> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("로그인이 필요합니다"))

        return groupRepository.getGroups(userId)
    }
}