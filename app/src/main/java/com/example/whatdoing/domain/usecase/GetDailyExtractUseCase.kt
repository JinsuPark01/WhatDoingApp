package com.example.whatdoing.domain.usecase

import com.example.whatdoing.domain.model.ExtractSlot
import com.example.whatdoing.domain.repository.GroupRepository
import com.example.whatdoing.domain.repository.RecordRepository
import com.example.whatdoing.domain.repository.UserRepository
import javax.inject.Inject

class GetDailyExtractUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val recordRepository: RecordRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        groupId: String,
        startMillis: Long,
        endMillis: Long
    ): Result<List<ExtractSlot>> {
        // 1. 그날 기록들
        val records = recordRepository.getRecordsByGroup(groupId, startMillis, endMillis)
            .getOrElse { return Result.failure(it) }

        // 2. 그룹 전체 멤버
        val memberIds = groupRepository.getMemberIds(groupId)
            .getOrElse { return Result.failure(it) }

        // 3. 기록한 사람 uid
        val recordedUserIds = records.map { it.userId }.toSet()

        // 4. 미기록자 uid
        val missingIds = memberIds.filter { it !in recordedUserIds }

        // 5. 미기록자 닉네임
        val missingNicknames = if (missingIds.isEmpty()) {
            emptyMap()
        } else {
            userRepository.getNicknames(missingIds).getOrElse { return Result.failure(it) }
        }

        // 6. 칸 목록 조립: 기록자 먼저, 미기록자(zzz) 뒤에
        val recordedSlots = records.map { record ->
            ExtractSlot.Recorded(
                nickname = record.userName,
                workoutType = record.workoutType,
                workoutDuration = record.workoutDuration,
                comment = record.comment,
                imageUrl = record.imageUrl
            )
        }

        val emptySlots = missingIds.map { uid ->
            ExtractSlot.Empty(
                nickname = missingNicknames[uid] ?: "알 수 없음"
            )
        }

        return Result.success(recordedSlots + emptySlots)
    }
}