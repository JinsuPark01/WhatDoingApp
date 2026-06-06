package com.example.whatdoing.domain.repository

import android.net.Uri
import com.example.whatdoing.domain.model.Group

interface GroupRepository {
    suspend fun getGroups(userId: String): Result<List<Group>>

    suspend fun getGroupById(groupId: String): Result<Group>

    suspend fun createGroup(
        userId: String,
        name: String,
        description: String,
        imageUri: String?
    ): Result<String>

    suspend fun joinGroup(groupId: String, userId: String): Result<Boolean>
    suspend fun leaveGroup(groupId: String, userId: String): Result<Unit>
    suspend fun getMemberIds(groupId: String): Result<List<String>>
}