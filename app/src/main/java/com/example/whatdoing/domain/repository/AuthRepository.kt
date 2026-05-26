package com.example.whatdoing.domain.repository

import com.example.whatdoing.domain.model.User

interface AuthRepository {
    suspend fun emailLogin(email: String, password: String): Result<User>
    fun getCurrentUserId(): String?
}