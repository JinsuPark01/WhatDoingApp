package com.example.whatdoing.domain.repository

import com.example.whatdoing.domain.model.User

interface AuthRepository {
    suspend fun emailLogin(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String, nickname: String): Result<User>
    suspend fun googleLogin(idToken: String): Result<User>
    fun getCurrentUserId(): String?
    fun getCurrentUserName(): String?
    fun getCurrentUserEmail(): String?
    fun logout()
}