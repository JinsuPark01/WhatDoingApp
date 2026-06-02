package com.example.whatdoing.domain.repository

import com.example.whatdoing.domain.model.WorkoutRecord

interface RecordRepository {
    suspend fun getRecordsByGroup(groupId: String): Result<List<WorkoutRecord>>
    suspend fun hasWroteToday(groupId: String, userId: String): Result<Boolean>

    suspend fun createRecord(
        groupId: String,
        userId: String,
        userName: String,
        workoutType: String,
        workoutDuration: Int,
        imageUri: String?,
        comment: String
    ): Result<String>

    suspend fun getRecordsByUser(userId: String): Result<List<WorkoutRecord>>
    suspend fun deleteRecordsByUserInGroup(groupId: String, userId: String): Result<Unit>
}