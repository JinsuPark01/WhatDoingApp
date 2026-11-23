package com.jinsupark.helpumta.data.repository

import android.net.Uri
import com.jinsupark.helpumta.domain.model.Group
import com.jinsupark.helpumta.domain.repository.GroupRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import com.jinsupark.helpumta.data.mapper.toGroup
import com.jinsupark.helpumta.domain.model.GroupPolicy
import com.google.firebase.storage.storageMetadata
import com.jinsupark.helpumta.data.util.ImageCleaner
import com.jinsupark.helpumta.data.util.ImageCompressor

class GroupRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val imageCompressor: ImageCompressor,
    private val imageCleaner: ImageCleaner
) : GroupRepository {

    override suspend fun getGroups(userId: String): Result<List<Group>> {
        return try {
            val snapshot = firestore.collection("groups")
                .whereArrayContains("members", userId)
                .get()
                .await()

            val groups = snapshot.documents.mapNotNull { it.toGroup() }
            Result.success(groups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createGroup(
        userId: String,
        name: String,
        description: String,
        imageUri: String?
    ): Result<String> {
        return try {
            // ID를 먼저 만든다 — 이미지를 groups/{groupId}/ 아래에 올려야
            // Storage 규칙이 그룹 멤버 기준으로 수정·삭제를 판단할 수 있다.
            // (document()는 로컬에서 ID만 만들고 서버엔 아무것도 쓰지 않는다)
            val docRef = firestore.collection("groups").document()

            val imageUrl = imageUri?.let { uploadGroupImage(docRef.id, it) } ?: ""

            val groupData = hashMapOf(
                "name" to name,
                "description" to description,
                "imageUrl" to imageUrl,
                "members" to listOf(userId),
                "createdAt" to System.currentTimeMillis()
            )

            try {
                docRef.set(groupData).await()
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

    override suspend fun updateGroup(
        groupId: String,
        userId: String,
        name: String,
        description: String,
        imageUri: String?
    ): Result<Unit> {
        // 새 로컬 이미지일 때만 업로드. 트랜잭션은 재시도될 수 있으므로 업로드는 밖에서 한 번만
        val uploadedUrl: String? = try {
            if (imageUri != null && !imageUri.startsWith("http")) {
                uploadGroupImage(groupId, imageUri)
            } else {
                null
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }

        return try {
            val docRef = firestore.collection("groups").document(groupId)

            // 멤버 누구나 수정 가능 → 화면을 연 뒤 다른 멤버가 이미지를 바꿨을 수 있다.
            // 화면에서 읽어둔 URL이 아니라 트랜잭션 안에서 읽은 '현재' URL 기준으로 교체/정리한다.
            val replacedUrl = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    throw Exception("그룹을 찾을 수 없습니다")
                }

                val members = (snapshot.get("members") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?: emptyList()
                if (userId !in members) {
                    throw Exception("그룹 멤버만 수정할 수 있습니다")
                }

                val currentUrl = snapshot.getString("imageUrl").orEmpty()
                val updates = mutableMapOf<String, Any>(
                    "name" to name,
                    "description" to description
                )

                val replaced: String? = when {
                    imageUri == null -> {          // 삭제
                        updates["imageUrl"] = ""
                        currentUrl
                    }
                    uploadedUrl != null -> {       // 교체
                        updates["imageUrl"] = uploadedUrl
                        currentUrl
                    }
                    else -> null                   // 유지 — imageUrl 필드를 건드리지 않는다
                }

                transaction.update(docRef, updates)
                replaced?.takeIf { it.isNotBlank() }
            }.await()

            // Firestore 반영이 끝난 뒤 기존 이미지 정리 (백그라운드)
            imageCleaner.delete(replacedUrl.orEmpty())

            Result.success(Unit)
        } catch (e: Exception) {
            // 문서 반영 실패 → 방금 올린 이미지는 아무도 참조하지 않으므로 정리
            uploadedUrl?.let { imageCleaner.delete(it) }
            Result.failure(e)
        }
    }

    override suspend fun getGroupById(groupId: String): Result<Group> {
        return try {
            val doc = firestore.collection("groups")
                .document(groupId)
                .get()
                .await()

            if (!doc.exists()) {
                return Result.failure(Exception("그룹을 찾을 수 없습니다"))
            }

            val group = doc.toGroup()
                ?: return Result.failure(Exception("그룹 정보가 올바르지 않습니다"))

            Result.success(group)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinGroup(groupId: String, userId: String): Result<Boolean> {
        return try {
            val docRef = firestore.collection("groups").document(groupId)

            val joined = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    throw Exception("존재하지 않는 그룹입니다")
                }

                val members = (snapshot.get("members") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?: emptyList()

                // 이미 멤버면 추가 안 함 (멱등 — 기존 동작 유지)
                if (members.contains(userId)) {
                    return@runTransaction false
                }

                // 정원 체크
                if (members.size >= GroupPolicy.MAX_MEMBERS) {
                    throw Exception("그룹 정원이 가득 찼어요 (최대 ${GroupPolicy.MAX_MEMBERS}명)")
                }

                // 추가 (읽은 값 기준으로 직접 update → 트랜잭션 정합성)
                transaction.update(docRef, "members", members + userId)
                true
            }.await()

            Result.success(joined)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveGroup(groupId: String, userId: String): Result<Unit> {
        return try {
            val docRef = firestore.collection("groups").document(groupId)

            // 동시 탈퇴 시 멤버 0명 그룹이 남는 것을 방지 — 읽기~쓰기를 트랜잭션으로 묶음
            // Storage 삭제는 트랜잭션 재시도 대상이 되면 안 되므로 밖에서 처리
            val deletedImageUrl = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    // 이미 없는 그룹이면 성공 처리 (멱등성)
                    return@runTransaction null
                }

                val members = (snapshot.get("members") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?: emptyList()
                val remaining = members.filter { it != userId }

                if (remaining.isEmpty()) {
                    // 마지막 멤버 → 그룹 삭제, 이미지 URL은 밖에서 정리
                    transaction.delete(docRef)
                    snapshot.getString("imageUrl")?.takeIf { it.isNotBlank() }
                } else {
                    // 멤버 남아있음 → 읽은 값 기준으로 직접 write (트랜잭션 정합성)
                    transaction.update(docRef, "members", remaining)
                    null
                }
            }.await()

            // 그룹이 실제로 삭제된 경우에만 대표 이미지 정리 (백그라운드)
            // 그룹 문서가 이미 없으므로 Storage 규칙의 "그룹이 사라졌으면 삭제 허용"으로 통과한다
            imageCleaner.delete(deletedImageUrl.orEmpty())

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMemberIds(groupId: String): Result<List<String>> {
        return try {
            val doc = firestore.collection("groups").document(groupId).get().await()
            if (!doc.exists()) {
                return Result.failure(Exception("그룹을 찾을 수 없습니다"))
            }
            val members = (doc.get("members") as? List<*>)?.filterIsInstance<String>()
                ?: emptyList()
            Result.success(members)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // groups/{groupId}/{uuid}.jpg — 업로더가 아니라 그룹 단위 (멤버 누구나 교체·정리)
    private suspend fun uploadGroupImage(groupId: String, uriString: String): String {
        val bytes = imageCompressor.compress(Uri.parse(uriString), maxSize = 1080)
        val ref = storage.reference.child("groups/$groupId/${UUID.randomUUID()}.jpg")
        ref.putBytes(bytes, storageMetadata { contentType = "image/jpeg" }).await()
        return ref.downloadUrl.await().toString()
    }
}