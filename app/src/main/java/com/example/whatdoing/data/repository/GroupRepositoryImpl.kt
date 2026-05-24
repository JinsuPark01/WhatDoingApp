package com.example.whatdoing.data.repository

import com.example.whatdoing.domain.model.Group
import com.example.whatdoing.domain.repository.GroupRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GroupRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
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
                    memberCount = (doc.getLong("memberCount") ?: 0L).toInt()
                )
            }
            Result.success(groups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}