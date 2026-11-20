package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.repository.AuthRepository
import com.jinsupark.helpumta.domain.repository.GroupRepository
import javax.inject.Inject

class UpdateGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        groupId: String,
        name: String,
        description: String,
        imageUri: String?
    ): Result<Unit> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("로그인이 필요합니다"))

        return groupRepository.updateGroup(
            groupId = groupId,
            userId = userId,
            name = name,
            description = description,
            imageUri = imageUri
        )
    }
}