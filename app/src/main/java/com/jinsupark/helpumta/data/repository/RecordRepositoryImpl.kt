package com.jinsupark.helpumta.data.repository

import android.net.Uri
import com.jinsupark.helpumta.data.mapper.toWorkoutRecord
import com.jinsupark.helpumta.domain.model.Reaction
import com.jinsupark.helpumta.domain.model.WorkoutRecord
import com.jinsupark.helpumta.domain.repository.RecordRepository
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import com.jinsupark.helpumta.data.util.ImageCleaner
import com.jinsupark.helpumta.data.util.ImageCompressor
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

class RecordRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val imageCompressor: ImageCompressor,
    private val imageCleaner: ImageCleaner
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

    override suspend fun getMyRecordDates(
        groupId: String,
        userId: String,
        startMillis: Long,
        endMillis: Long
    ): Result<List<Long>> {
        return try {
            // hasWroteToday와 같은 쿼리 모양(groupId=, userId=, createdAt 범위)이라
            // 이미 만들어진 복합 인덱스를 그대로 재사용한다.
            val snapshot = firestore.collection("records")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("createdAt", startMillis)
                .whereLessThan("createdAt", endMillis)
                .get()
                .await()

            Result.success(snapshot.documents.mapNotNull { it.getLong("createdAt") })
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
            val imageUrl = imageUri?.let { uploadImage(groupId, userId, it) } ?: ""

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

            val docRef = try {
                firestore.collection("records").add(recordData).await()
            } catch (e: Exception) {
                // 문서 저장 실패 → 방금 올린 이미지는 아무도 참조하지 않으므로 정리
                imageCleaner.delete(imageUrl)
                throw e
            }

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateRecord(
        recordId: String,
        currentUserId: String,
        workoutType: String,
        workoutDuration: Int,
        imageUri: String?,
        comment: String
    ): Result<Unit> {
        return try {
            val docRef = firestore.collection("records").document(recordId)
            val snapshot = docRef.get().await()

            if (!snapshot.exists()) {
                return Result.failure(Exception("기록을 찾을 수 없습니다"))
            }

            // 권한 체크: 본인 기록만
            val ownerId = snapshot.getString("userId")
            if (ownerId != currentUserId) {
                return Result.failure(Exception("본인 기록만 수정할 수 있습니다"))
            }

            val oldImageUrl = snapshot.getString("imageUrl") ?: ""

            // 이미지 3케이스 처리
            // 원칙: Storage 삭제는 Firestore 반영이 끝난 뒤에 (깨진 참조 방지)
            val newImageUrl: String = when {
                // 1. null → 제거 (삭제는 Firestore 업데이트 후로 미룸)
                imageUri == null -> ""
                // 2. 기존 URL 그대로 (https) → 유지
                imageUri.startsWith("http") -> oldImageUrl
                // 3. 새 로컬 이미지 (content/file) → 업로드 (기존 삭제는 아래에서)
                else -> {
                    val groupId = snapshot.getString("groupId") ?: ""
                    uploadImage(groupId, currentUserId, imageUri)
                }
            }

            // Firestore 업데이트 (createdAt, userId, userName, groupId, reactions는 안 건드림)
            try {
                docRef.update(
                    mapOf(
                        "workoutType" to workoutType,
                        "workoutDuration" to workoutDuration,
                        "imageUrl" to newImageUrl,
                        "comment" to comment
                    )
                ).await()
            } catch (e: Exception) {
                // 반영 실패 → 새로 올린 이미지만 정리 (기존 이미지는 아직 참조 중)
                if (newImageUrl != oldImageUrl) imageCleaner.delete(newImageUrl)
                throw e
            }

            // 더 이상 참조되지 않는 기존 이미지는 백그라운드에서 정리
            if (oldImageUrl.isNotBlank() && oldImageUrl != newImageUrl) {
                imageCleaner.delete(oldImageUrl)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserNameInRecords(
        userId: String,
        userName: String
    ): Result<Unit> {
        return try {
            val snapshot = firestore.collection("records")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            // WriteBatch는 한 번에 최대 500건 → 나눠서 커밋.
            // 중간에 실패하면 일부만 바뀐 상태가 되지만 표시용 복사본이라 불변식은 아니고,
            // 같은 닉네임으로 다시 저장하면 전부 맞춰진다.
            snapshot.documents.chunked(500).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { doc -> batch.update(doc.reference, "userName", userName) }
                batch.commit().await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setReaction(
        recordId: String,
        userId: String,
        reaction: Reaction?
    ): Result<Unit> {
        return try {
            // reactions 맵 전체가 아니라 '내 키' 하나만 갱신한다
            // → 다른 멤버가 동시에 눌러도 서로 덮어쓰지 않아 트랜잭션이 필요 없다.
            // uid를 문자열 경로("reactions.$uid")로 이으면 특수문자에 취약하므로 FieldPath로.
            firestore.collection("records").document(recordId)
                .update(
                    FieldPath.of("reactions", userId),
                    reaction?.key ?: FieldValue.delete()
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecordById(recordId: String): Result<WorkoutRecord> {
        return try {
            val doc = firestore.collection("records").document(recordId).get().await()
            if (!doc.exists()) {
                return Result.failure(Exception("기록을 찾을 수 없습니다"))
            }
            val record = doc.toWorkoutRecord()
                ?: return Result.failure(Exception("기록 정보가 올바르지 않습니다"))
            Result.success(record)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRecord(
        recordId: String,
        currentUserId: String
    ): Result<Unit> {
        return try {
            val docRef = firestore.collection("records").document(recordId)
            val snapshot = docRef.get().await()

            if (!snapshot.exists()) {
                return Result.failure(Exception("기록을 찾을 수 없습니다"))
            }

            // 권한 체크: 본인 기록만
            val ownerId = snapshot.getString("userId")
            if (ownerId != currentUserId) {
                return Result.failure(Exception("본인 기록만 삭제할 수 있습니다"))
            }

            val imageUrl = snapshot.getString("imageUrl") ?: ""

            // Firestore 문서 먼저 삭제 → 실패해도 이미지가 남아 기록이 깨지지 않음
            docRef.delete().await()

            // 이미지 정리는 백그라운드로. 호출자를 기다리게 하지 않는다
            imageCleaner.delete(imageUrl)
            Result.success(Unit)
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
                val imageUrl = doc.getString("imageUrl") ?: ""

                // Firestore 문서 먼저 삭제
                doc.reference.delete().await()

                // 이미지 정리는 백그라운드로 (경로에 내 uid가 있어 그룹을 나간 뒤에도 지울 수 있다)
                imageCleaner.delete(imageUrl)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // records/{groupId}/{userId}/{uuid}.jpg — 경로의 userId로 Storage 규칙이 소유자를 확인한다
    private suspend fun uploadImage(groupId: String, userId: String, uriString: String): String {
        val bytes = imageCompressor.compress(Uri.parse(uriString))
        val ref = storage.reference.child("records/$groupId/$userId/${UUID.randomUUID()}.jpg")
        ref.putBytes(bytes, storageMetadata { contentType = "image/jpeg" }).await()
        return ref.downloadUrl.await().toString()
    }

    override suspend fun removeReactionsByUserInGroup(
        groupId: String,
        userId: String
    ): Result<Unit> {
        return try {
            // groupId 조건만 건다. reactions.{uid}는 사람마다 필드 경로가 달라
            // groupId와 묶는 복합 인덱스를 미리 만들 수 없다 → 내 리액션 여부는 여기서 거른다.
            // 그룹을 나갈 때 한 번만 실행되므로 그룹 기록 전체를 읽는 비용은 감수한다.
            val snapshot = firestore.collection("records")
                .whereEqualTo("groupId", groupId)
                .get()
                .await()

            val targets = snapshot.documents.filter { doc ->
                // 내 기록은 바로 뒤에 통째로 지워지므로 건너뛴다
                doc.getString("userId") != userId &&
                        (doc.get("reactions") as? Map<*, *>)?.containsKey(userId) == true
            }

            // 문서마다 규칙이 그룹 문서를 get()으로 확인한다.
            // 배치 1회당 규칙의 문서 조회 한도(20회)를 넘지 않게 작게 나눈다.
            targets.chunked(REACTION_BATCH_SIZE).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { doc ->
                    batch.update(doc.reference, FieldPath.of("reactions", userId), FieldValue.delete())
                }
                batch.commit().await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private companion object {
        const val REACTION_BATCH_SIZE = 10
    }
}