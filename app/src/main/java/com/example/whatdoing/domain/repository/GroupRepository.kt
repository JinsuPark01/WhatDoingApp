package com.example.whatdoing.domain.repository

import com.example.whatdoing.domain.model.Group

interface GroupRepository {
    suspend fun getGroups(userId: String): Result<List<Group>>
}