package com.example.whatdoing.domain.usecase

import com.example.whatdoing.domain.repository.GroupRepository
import javax.inject.Inject

class GetGroupDetailUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(groupId: String) =
        groupRepository.getGroupById(groupId)
}