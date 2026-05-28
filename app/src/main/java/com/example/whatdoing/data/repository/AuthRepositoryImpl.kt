package com.example.whatdoing.data.repository

import com.example.whatdoing.domain.model.User
import com.example.whatdoing.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
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

    override suspend fun signUp(
        email: String,
        password: String,
        nickname: String
    ): Result<User> {
        return try {
            val result = firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .await()

            val firebaseUser = result.user
                ?: return Result.failure(Exception("회원가입에 실패했습니다"))

            // 닉네임을 displayName으로 설정
            val profileUpdate = userProfileChangeRequest {
                displayName = nickname
            }
            firebaseUser.updateProfile(profileUpdate).await()

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

    override fun getCurrentUserName(): String? {
        val user = firebaseAuth.currentUser ?: return null
        // displayName이 없으면 email의 @ 앞부분 사용
        return user.displayName ?: user.email?.substringBefore("@")
    }
}