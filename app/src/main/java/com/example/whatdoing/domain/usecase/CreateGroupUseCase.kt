package com.example.whatdoing.domain.usecase

import android.net.Uri
import com.example.whatdoing.domain.repository.GroupRepository
import javax.inject.Inject

class CreateGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(
        userId: String,
        name: String,
        description: String,
        imageUri: Uri?,
        isPrivate: Boolean,
        password: String
    ): Result<String> = groupRepository.createGroup(
        userId = userId,
        name = name,
        description = description,
        imageUri = imageUri,
        isPrivate = isPrivate,
        password = password
    )
}