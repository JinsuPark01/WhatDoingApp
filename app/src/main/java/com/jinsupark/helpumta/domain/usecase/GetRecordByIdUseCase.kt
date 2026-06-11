package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.model.WorkoutRecord
import com.jinsupark.helpumta.domain.repository.RecordRepository
import javax.inject.Inject

class GetRecordByIdUseCase @Inject constructor(
    private val recordRepository: RecordRepository
) {
    suspend operator fun invoke(recordId: String): Result<WorkoutRecord> =
        recordRepository.getRecordById(recordId)
}