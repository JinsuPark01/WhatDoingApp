package com.jinsupark.helpumta.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.jinsupark.helpumta.domain.repository.UserRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : UserRepository {

    override suspend fun saveUser(
        uid: String,
        nickname: String,
        email: String
    ): Result<Unit> {
        return try {
            val docRef = firestore.collection("users").document(uid)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                // 이미 있으면 nickname/email만 갱신 (createdAt 보존)
                docRef.set(
                    mapOf(
                        "nickname" to nickname,
                        "email" to email
                    ),
                    SetOptions.merge()
                ).await()
            } else {
                // 신규 생성
                docRef.set(
                    mapOf(
                        "nickname" to nickname,
                        "email" to email,
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNicknames(uids: List<String>): Result<Map<String, String>> {
        return try {
            if (uids.isEmpty()) return Result.success(emptyMap())

            val result = mutableMapOf<String, String>()
            // Firestore whereIn은 최대 30개 → 친구 그룹 규모(4명)라 단순 개별 조회로 충분
            for (uid in uids) {
                val doc = firestore.collection("users").document(uid).get().await()
                val nickname = doc.getString("nickname")
                if (nickname != null) {
                    result[uid] = nickname
                }
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteUser(uid: String): Result<Unit> {
        return try {
            firestore.collection("users").document(uid).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerFcmToken(): Result<Unit> {
        return try {
            // 비로그인 상태면 저장할 대상이 없음 → 실패가 아니라 그냥 통과
            val uid = auth.currentUser?.uid ?: return Result.success(Unit)
            val token = FirebaseMessaging.getInstance().token.await()
            firestore.collection("users").document(uid)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearFcmToken(): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.success(Unit)
            // update()는 문서가 없으면 실패 → set(merge)로 delete 센티넬 전송
            firestore.collection("users").document(uid)
                .set(mapOf("fcmToken" to FieldValue.delete()), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}