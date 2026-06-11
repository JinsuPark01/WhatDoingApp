package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.repository.GroupRepository
import javax.inject.Inject

class GetGroupDetailUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(groupId: String) =
        groupRepository.getGroupById(groupId)
}