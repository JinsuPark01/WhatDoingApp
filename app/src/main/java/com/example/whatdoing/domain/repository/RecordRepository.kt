package com.example.whatdoing.domain.repository

import com.example.whatdoing.domain.model.WorkoutRecord

interface RecordRepository {
    suspend fun getRecordsByGroup(groupId: String): Result<List<WorkoutRecord>>
    suspend fun hasWroteToday(groupId: String, userId: String): Result<Boolean>
}