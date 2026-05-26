package com.example.whatdoing.data.repository

import com.example.whatdoing.domain.model.User
import com.example.whatdoing.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override suspend fun emailLogin(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .await()

            val firebaseUser = result.user
                ?: return Result.failure(Exception("유저 정보를 가져올 수 없습니다"))

            Result.success(
                User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: ""
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid
}