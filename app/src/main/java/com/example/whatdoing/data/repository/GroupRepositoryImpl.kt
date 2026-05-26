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
        imageUri: String?,
        isPrivate: Boolean,
        password: String
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
}