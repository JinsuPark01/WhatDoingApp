package com.example.whatdoing.domain.usecase

import com.example.whatdoing.domain.model.WorkoutRecord
import com.example.whatdoing.domain.repository.RecordRepository
import javax.inject.Inject

class GetRecordByIdUseCase @Inject constructor(
    private val recordRepository: RecordRepository
) {
    suspend operator fun invoke(recordId: String): Result<WorkoutRecord> =
        recordRepository.getRecordById(recordId)
}