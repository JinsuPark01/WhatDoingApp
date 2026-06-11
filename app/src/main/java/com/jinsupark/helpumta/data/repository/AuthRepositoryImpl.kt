package com.jinsupark.helpumta.data.repository

import com.jinsupark.helpumta.domain.model.AuthProvider
import com.jinsupark.helpumta.domain.model.User
import com.jinsupark.helpumta.domain.repository.AuthRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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
    override suspend fun googleLogin(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()

            val firebaseUser = result.user
                ?: return Result.failure(Exception("구글 로그인에 실패했습니다"))

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

    override fun getCurrentUserEmail(): String? {
        return firebaseAuth.currentUser?.email
    }

    override fun logout() {
        firebaseAuth.signOut()
    }

    override fun getAuthProvider(): AuthProvider {
        val user = firebaseAuth.currentUser ?: return AuthProvider.UNKNOWN
        return when {
            user.providerData.any { it.providerId == "google.com" } -> AuthProvider.GOOGLE
            user.providerData.any { it.providerId == "password" } -> AuthProvider.EMAIL
            else -> AuthProvider.UNKNOWN
        }
    }

    override suspend fun reauthenticateWithPassword(password: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return Result.failure(Exception("로그인 상태가 아닙니다"))
            val email = user.email
                ?: return Result.failure(Exception("이메일 정보를 가져올 수 없습니다"))

            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return Result.failure(Exception("로그인 상태가 아닙니다"))

            val credential = GoogleAuthProvider.getCredential(idToken, null)
            user.reauthenticate(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return Result.failure(Exception("로그인 상태가 아닙니다"))
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}