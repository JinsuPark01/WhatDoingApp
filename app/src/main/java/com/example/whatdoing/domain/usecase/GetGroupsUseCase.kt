package com.example.whatdoing.domain.usecase

import com.example.whatdoing.domain.repository.GroupRepository
import javax.inject.Inject

class GetGroupsUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(userId: String) =
        groupRepository.getGroups(userId)
}