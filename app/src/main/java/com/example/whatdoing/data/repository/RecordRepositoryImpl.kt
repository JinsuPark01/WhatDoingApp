package com.example.whatdoing.data.repository

import com.example.whatdoing.data.mapper.toWorkoutRecord
import com.example.whatdoing.domain.model.WorkoutRecord
import com.example.whatdoing.domain.repository.RecordRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject

class RecordRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : RecordRepository {

    override suspend fun getRecordsByGroup(groupId: String): Result<List<WorkoutRecord>> {
        return try {
            val snapshot = firestore.collection("records")
                .whereEqualTo("groupId", groupId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val records = snapshot.documents.mapNotNull { it.toWorkoutRecord() }
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun hasWroteToday(groupId: String, userId: String): Result<Boolean> {
        return try {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val snapshot = firestore.collection("records")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("createdAt", todayStart)
                .limit(1)
                .get()
                .await()

            Result.success(!snapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}