package com.example.whatdoing.data.repository

import android.net.Uri
import com.example.whatdoing.domain.model.Group
import com.example.whatdoing.domain.repository.GroupRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

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

            val groups = snapshot.documents.mapNotNull { doc ->
                Group(
                    id = doc.id,
                    name = doc.getString("name") ?: return@mapNotNull null,
                    description = doc.getString("description") ?: "",
                    imageUrl = doc.getString("imageUrl") ?: "",
                    isPrivate = doc.getBoolean("isPrivate") ?: false,
                    memberCount = ((doc.get("members") as? List<*>)?.size) ?: 0
                )
            }
            Result.success(groups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createGroup(
        userId: String,
        name: String,
        description: String,
        imageUri: Uri?,
        isPrivate: Boolean,
        password: String
    ): Result<String> {
        return try {
            // 1. 이미지 업로드 (있을 때만)
            val imageUrl = imageUri?.let {
                val ref = storage.reference
                    .child("groups/${System.currentTimeMillis()}.jpg")
                ref.putFile(it).await()
                ref.downloadUrl.await().toString()
            } ?: ""

            // 2. Firestore에 그룹 정보 저장
            val groupData = hashMapOf(
                "name" to name,
                "description" to description,
                "imageUrl" to imageUrl,
                "isPrivate" to isPrivate,
                "password" to if (isPrivate) password else "",
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
}