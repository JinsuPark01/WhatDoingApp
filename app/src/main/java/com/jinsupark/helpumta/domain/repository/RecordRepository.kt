package com.jinsupark.helpumta.domain.repository

import com.jinsupark.helpumta.domain.model.Reaction
import com.jinsupark.helpumta.domain.model.WorkoutRecord

interface RecordRepository {
    suspend fun getRecordsByGroup(
        groupId: String,
        startMillis: Long,
        endMillis: Long
    ): Result<List<WorkoutRecord>>

    // 캘린더 표시용 — 특정 기간에 내가 기록한 createdAt 목록
    suspend fun getMyRecordDates(
        groupId: String,
        userId: String,
        startMillis: Long,
        endMillis: Long
    ): Result<List<Long>>
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

    // 닉네임 변경 시 — 내 모든 기록의 userName(복사본)을 맞춘다
    suspend fun updateUserNameInRecords(userId: String, userName: String): Result<Unit>

    // 내 리액션 설정. null이면 취소
    suspend fun setReaction(recordId: String, userId: String, reaction: Reaction?): Result<Unit>
    suspend fun getRecordById(recordId: String): Result<WorkoutRecord>
    suspend fun deleteRecord(recordId: String, currentUserId: String): Result<Unit>
    suspend fun deleteRecordsByUserInGroup(groupId: String, userId: String): Result<Unit>
    suspend fun removeReactionsByUserInGroup(groupId: String, userId: String): Result<Unit>
}