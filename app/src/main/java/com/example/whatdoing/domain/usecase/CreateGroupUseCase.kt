package com.example.whatdoing.domain.usecase

import android.net.Uri
import com.example.whatdoing.domain.repository.AuthRepository
import com.example.whatdoing.domain.repository.GroupRepository
import javax.inject.Inject

class CreateGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        name: String,
        description: String,
        imageUri: String?,  // String으로 변경
        isPrivate: Boolean,
        password: String
    ): Result<String> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("로그인이 필요합니다"))

        return groupRepository.createGroup(
            userId = userId,
            name = name,
            description = description,
            imageUri = imageUri,
            isPrivate = isPrivate,
            password = password
        )
    }
}