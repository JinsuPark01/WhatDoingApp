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
    fun logout()
    fun getAuthProvider(): AuthProvider
    suspend fun reauthenticateWithPassword(password: String): Result<Unit>
    suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
}