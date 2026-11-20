package com.jinsupark.helpumta.domain.repository

import com.jinsupark.helpumta.domain.model.Group

interface GroupRepository {
    suspend fun getGroups(userId: String): Result<List<Group>>

    suspend fun getGroupById(groupId: String): Result<Group>

    suspend fun createGroup(
        userId: String,
        name: String,
        description: String,
        imageUri: String?
    ): Result<String>

    /**
     * imageUri 규칙 (기록 수정과 동일)
     *  - null        → 이미지 삭제
     *  - http(s) URL → 유지
     *  - 그 외 로컬 URI → 새로 업로드 후 교체
     */
    suspend fun updateGroup(
        groupId: String,
        userId: String,
        name: String,
        description: String,
        imageUri: String?
    ): Result<Unit>

    suspend fun joinGroup(groupId: String, userId: String): Result<Boolean>
    suspend fun leaveGroup(groupId: String, userId: String): Result<Unit>
    suspend fun getMemberIds(groupId: String): Result<List<String>>
}