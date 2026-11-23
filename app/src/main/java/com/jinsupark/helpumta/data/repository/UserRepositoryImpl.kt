package com.jinsupark.helpumta.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import com.jinsupark.helpumta.data.util.ImageCleaner
import com.jinsupark.helpumta.data.util.ImageCompressor
import com.jinsupark.helpumta.domain.model.MemberProfile
import com.jinsupark.helpumta.domain.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
    private val imageCompressor: ImageCompressor,
    private val imageCleaner: ImageCleaner
) : UserRepository {

    override suspend fun saveUser(
        uid: String,
        nickname: String
    ): Result<Unit> {
        return try {
            val docRef = userDoc(uid)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                // 이미 있으면 nickname만 갱신 (createdAt 보존).
                // 예전 버전이 남긴 공개 필드도 같이 지운다 — 규칙이 이 필드가 남은 문서로의 쓰기를 거부하므로
                // 지우지 않으면 이후 닉네임·사진 수정이 전부 막힌다.
                docRef.set(
                    mapOf(
                        "nickname" to nickname,
                        LEGACY_FCM_TOKEN to FieldValue.delete(),
                        LEGACY_EMAIL to FieldValue.delete()
                    ),
                    SetOptions.merge()
                ).await()
            } else {
                // 신규 생성
                docRef.set(
                    mapOf(
                        "nickname" to nickname,
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
                val doc = userDoc(uid).get().await()
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

    override suspend fun getProfiles(uids: List<String>): Result<Map<String, MemberProfile>> {
        return try {
            if (uids.isEmpty()) return Result.success(emptyMap())

            val result = mutableMapOf<String, MemberProfile>()
            // getNicknames와 같은 이유로 개별 조회 (그룹 최대 4명)
            for (uid in uids) {
                val doc = userDoc(uid).get().await()
                if (!doc.exists()) continue
                result[uid] = MemberProfile(
                    uid = uid,
                    nickname = doc.getString("nickname").orEmpty(),
                    photoUrl = doc.getString("photoUrl").orEmpty()
                )
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateNickname(uid: String, nickname: String): Result<Unit> {
        return try {
            userDoc(uid)
                .set(mapOf("nickname" to nickname), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfileImage(uid: String, imageUri: String?): Result<String> {
        return try {
            val docRef = userDoc(uid)

            // 내 문서는 나만 쓴다 → 동시 수정 경합이 없어 트랜잭션 불필요 (기록 수정과 같은 판단)
            val oldUrl = docRef.get().await().getString("photoUrl").orEmpty()
            val newUrl = imageUri?.let { uploadProfileImage(uid, it) } ?: ""

            try {
                docRef.set(mapOf("photoUrl" to newUrl), SetOptions.merge()).await()
            } catch (e: Exception) {
                // 문서 반영 실패 → 방금 올린 이미지는 아무도 참조하지 않으므로 정리
                if (newUrl.isNotBlank()) imageCleaner.delete(newUrl)
                throw e
            }

            // Firestore 반영이 끝난 뒤 기존 이미지 정리 (백그라운드)
            if (oldUrl.isNotBlank() && oldUrl != newUrl) {
                imageCleaner.delete(oldUrl)
            }

            Result.success(newUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun deleteUser(uid: String): Result<Unit> {
        return try {
            val docRef = userDoc(uid)
            val photoUrl = docRef.get().await().getString("photoUrl").orEmpty()

            // 하위 컬렉션은 부모 문서를 지워도 같이 지워지지 않는다 → 토큰 문서를 먼저 직접 삭제.
            // (Auth 계정이 지워지면 본인 확인 규칙을 통과할 수 없어 다시는 못 지운다)
            fcmDoc(uid).delete().await()
            docRef.delete().await()

            // 계정 삭제 흐름에서는 이 직후 Auth 계정이 지워진다.
            // 백그라운드(ImageCleaner)로 넘기면 요청이 나가기 전에 로그아웃 상태가 되어
            // Storage 규칙에 거부될 수 있으므로, 여기서는 끝날 때까지 기다린다.
            if (photoUrl.isNotBlank()) {
                try {
                    storage.getReferenceFromUrl(photoUrl).delete().await()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // 사진 정리 실패는 계정 삭제를 막을 이유가 아니다
                }
            }

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
            // users는 로그인한 누구나 읽는다 → 토큰은 본인만 읽는 하위 문서에
            fcmDoc(uid)
                .set(
                    mapOf(
                        "token" to token,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearFcmToken(): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.success(Unit)
            // 문서가 없어도 delete는 성공한다
            fcmDoc(uid).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun userDoc(uid: String): DocumentReference =
        firestore.collection(USERS).document(uid)

    private fun fcmDoc(uid: String): DocumentReference =
        userDoc(uid).collection(PRIVATE).document(FCM_DOC)

    // profiles/{uid}/{uuid}.jpg — 경로의 uid로 Storage 규칙이 소유자를 확인한다
    // 아바타는 최대 80dp로 표시 → 512px이면 고밀도 화면에서도 충분
    private suspend fun uploadProfileImage(uid: String, uriString: String): String {
        val bytes = imageCompressor.compress(Uri.parse(uriString), maxSize = 512)
        val ref = storage.reference.child("profiles/$uid/${UUID.randomUUID()}.jpg")
        ref.putBytes(bytes, storageMetadata { contentType = "image/jpeg" }).await()
        return ref.downloadUrl.await().toString()
    }
    private companion object {
        const val USERS = "users"
        // Functions(index.ts)와 경로가 같아야 한다
        const val PRIVATE = "private"
        const val FCM_DOC = "fcm"

        // 예전 버전이 users 문서에 직접 두던 필드 — 정리용으로만 남긴다
        const val LEGACY_FCM_TOKEN = "fcmToken"
        const val LEGACY_EMAIL = "email"
    }
}