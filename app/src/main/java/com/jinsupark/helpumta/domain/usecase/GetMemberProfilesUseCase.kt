package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.model.MemberProfile
import com.jinsupark.helpumta.domain.repository.GroupRepository
import com.jinsupark.helpumta.domain.repository.UserRepository
import javax.inject.Inject

class GetMemberProfilesUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository
) {
    // 그룹 멤버(최대 4명)의 uid → 프로필
    suspend operator fun invoke(groupId: String): Result<Map<String, MemberProfile>> {
        val memberIds = groupRepository.getMemberIds(groupId).getOrElse {
            return Result.failure(it)
        }
        return userRepository.getProfiles(memberIds)
    }
}