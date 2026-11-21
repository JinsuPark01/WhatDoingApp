package com.jinsupark.helpumta.domain.repository

import com.jinsupark.helpumta.domain.model.AuthProvider
import com.jinsupark.helpumta.domain.model.User

interface AuthRepository {
    suspend fun emailLogin(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String, nickname: String): Result<User>
    suspend fun googleLogin(idToken: String): Result<User>
    fun getCurrentUserId(): String?
    fun getCurrentUserName(): String?
    fun getCurrentUserEmail(): String?

    // 닉네임 원본(displayName) 변경 — users/records는 복사본이라 호출자가 따로 맞춘다
    suspend fun updateDisplayName(name: String): Result<Unit>

    fun logout()
    fun getAuthProvider(): AuthProvider
    suspend fun reauthenticateWithPassword(password: String): Result<Unit>
    suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
}