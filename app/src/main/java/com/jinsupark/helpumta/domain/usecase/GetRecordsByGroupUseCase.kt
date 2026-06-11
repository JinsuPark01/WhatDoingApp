package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.repository.RecordRepository
import javax.inject.Inject

class GetRecordsByGroupUseCase @Inject constructor(
    private val recordRepository: RecordRepository
) {
    suspend operator fun invoke(groupId: String, startMillis: Long, endMillis: Long) =
        recordRepository.getRecordsByGroup(groupId, startMillis, endMillis)
}