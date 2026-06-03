package com.example.whatdoing.domain.usecase

import com.example.whatdoing.domain.repository.RecordRepository
import javax.inject.Inject

class GetRecordsByGroupUseCase @Inject constructor(
    private val recordRepository: RecordRepository
) {
    suspend operator fun invoke(groupId: String, startMillis: Long, endMillis: Long) =
        recordRepository.getRecordsByGroup(groupId, startMillis, endMillis)
}