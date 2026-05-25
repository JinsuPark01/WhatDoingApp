package com.example.whatdoing.domain.repository

import android.net.Uri
import com.example.whatdoing.domain.model.Group

interface GroupRepository {
    suspend fun getGroups(userId: String): Result<List<Group>>

    suspend fun createGroup(
        userId: String,
        name: String,
        description: String,
        imageUri: Uri?,
        isPrivate: Boolean,
        password: String
    ): Result<String> // 생성된 groupId 반환
}