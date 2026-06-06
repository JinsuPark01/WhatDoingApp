package com.example.whatdoing.data.repository

import android.net.Uri
import com.example.whatdoing.domain.model.Group
import com.example.whatdoing.domain.repository.GroupRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import com.example.whatdoing.data.mapper.toGroup
import com.google.firebase.firestore.FieldValue

class GroupRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
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
            val imageUrl = imageUri?.let { uriString ->
                val uri = Uri.parse(uriString)
                val fileName = "${UUID.randomUUID()}.jpg"
                val ref = storage.reference.child("groups/$fileName")
                ref.putFile(uri).await()
                ref.downloadUrl.await().toString()
            } ?: ""

            val groupData = hashMapOf(
                "name" to name,
                "description" to description,
                "imageUrl" to imageUrl,
                "members" to listOf(userId),
                "createdAt" to System.currentTimeMillis()
            )

            val docRef = firestore.collection("groups")
                .add(groupData)
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
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

            val doc = docRef.get().await()
            if (!doc.exists()) {
                return Result.failure(Exception("존재하지 않는 그룹입니다"))
            }

            // 가입 전 멤버였는지 확인
            val wasAlreadyMember = (doc.get("members") as? List<*>)
                ?.contains(userId) ?: false

            docRef.update("members", FieldValue.arrayUnion(userId)).await()

            // true = 새로 가입됨, false = 이미 멤버였음
            Result.success(!wasAlreadyMember)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveGroup(groupId: String, userId: String): Result<Unit> {
        return try {
            val docRef = firestore.collection("groups").document(groupId)
            val doc = docRef.get().await()

            if (!doc.exists()) {
                // 이미 없는 그룹이면 성공 처리 (멱등성)
                return Result.success(Unit)
            }

            val members = (doc.get("members") as? List<*>)?.filterIsInstance<String>()
                ?: emptyList()
            val remaining = members.filter { it != userId }

            if (remaining.isEmpty()) {
                // 마지막 멤버 → 그룹 자체 삭제 + 그룹 이미지 삭제
                val imageUrl = doc.getString("imageUrl") ?: ""
                if (imageUrl.isNotBlank()) {
                    runCatching {
                        storage.getReferenceFromUrl(imageUrl).delete().await()
                    }
                }
                docRef.delete().await()
            } else {
                // 멤버 남아있음 → 배열에서 본인만 제거
                docRef.update("members", FieldValue.arrayRemove(userId)).await()
            }

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
}