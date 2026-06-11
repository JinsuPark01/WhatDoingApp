package com.jinsupark.helpumta.domain.repository

import com.jinsupark.helpumta.domain.model.WorkoutRecord

interface RecordRepository {
    suspend fun getRecordsByGroup(
        groupId: String,
        startMillis: Long,
        endMillis: Long
    ): Result<List<WorkoutRecord>>
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

    suspend fun updateRecord(
        recordId: String,
        currentUserId: String,
        workoutType: String,
        workoutDuration: Int,
        imageUri: String?,
        comment: String
    ): Result<Unit>

    suspend fun getRecordsByUser(userId: String): Result<List<WorkoutRecord>>
    suspend fun getRecordById(recordId: String): Result<WorkoutRecord>
    suspend fun deleteRecordsByUserInGroup(groupId: String, userId: String): Result<Unit>
}