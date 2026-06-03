package com.example.whatdoing.data.repository

import android.net.Uri
import com.example.whatdoing.data.mapper.toWorkoutRecord
import com.example.whatdoing.domain.model.WorkoutRecord
import com.example.whatdoing.domain.repository.RecordRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

class RecordRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : RecordRepository {

    override suspend fun getRecordsByGroup(
        groupId: String,
        startMillis: Long,
        endMillis: Long
    ): Result<List<WorkoutRecord>> {
        return try {
            val snapshot = firestore.collection("records")
                .whereEqualTo("groupId", groupId)
                .whereGreaterThanOrEqualTo("createdAt", startMillis)
                .whereLessThan("createdAt", endMillis)
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

    override suspend fun createRecord(
        groupId: String,
        userId: String,
        userName: String,
        workoutType: String,
        workoutDuration: Int,
        imageUri: String?,
        comment: String
    ): Result<String> {
        return try {
            // 1. 이미지 업로드 (있을 때만)
            val imageUrl = imageUri?.let { uriString ->
                val uri = Uri.parse(uriString)
                val fileName = "${UUID.randomUUID()}.jpg"
                // 그룹별로 경로 정리
                val ref = storage.reference.child("records/$groupId/$fileName")
                ref.putFile(uri).await()
                ref.downloadUrl.await().toString()
            } ?: ""

            // 2. Firestore에 기록 저장
            val recordData = hashMapOf(
                "groupId" to groupId,
                "userId" to userId,
                "userName" to userName,
                "workoutType" to workoutType,
                "workoutDuration" to workoutDuration,
                "imageUrl" to imageUrl,
                "comment" to comment,
                "createdAt" to System.currentTimeMillis()
            )

            val docRef = firestore.collection("records")
                .add(recordData)
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecordsByUser(userId: String): Result<List<WorkoutRecord>> {
        return try {
            val snapshot = firestore.collection("records")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val records = snapshot.documents.mapNotNull { it.toWorkoutRecord() }
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRecordsByUserInGroup(
        groupId: String,
        userId: String
    ): Result<Unit> {
        return try {
            val snapshot = firestore.collection("records")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            for (doc in snapshot.documents) {
                // Storage 이미지 먼저 삭제
                val imageUrl = doc.getString("imageUrl") ?: ""
                if (imageUrl.isNotBlank()) {
                    runCatching {
                        storage.getReferenceFromUrl(imageUrl).delete().await()
                    }
                }
                // Firestore 문서 삭제
                doc.reference.delete().await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}