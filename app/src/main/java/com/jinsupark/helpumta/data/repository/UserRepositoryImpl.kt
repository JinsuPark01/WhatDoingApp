package com.jinsupark.helpumta.data.repository

import com.jinsupark.helpumta.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
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
}