package com.example.whatdoing.data.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.whatdoing.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthClient @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    private val credentialManager = CredentialManager.create(appContext)

    suspend fun getIdToken(activity: Activity): Result<String> {
        if (BuildConfig.WEB_CLIENT_ID.isBlank()) {
            return Result.failure(Exception("WEB_CLIENT_ID 설정이 없습니다"))
        }

        return try {
            // 1차 시도: One Tap (기존 가입자용)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )

            extractIdToken(result)
        } catch (e: NoCredentialException) {
            // 1차 실패 시 신규 가입용
            signInWithGoogle(activity)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("로그인이 취소되었습니다"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun signInWithGoogle(activity: Activity): Result<String> {
        return try {
            val signInOption = GetSignInWithGoogleOption.Builder(BuildConfig.WEB_CLIENT_ID)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )

            extractIdToken(result)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("로그인이 취소되었습니다"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractIdToken(result: GetCredentialResponse): Result<String> {
        val credential = result.credential
        return if (credential is CustomCredential &&
            credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

            val googleIdTokenCredential = GoogleIdTokenCredential
                .createFrom(credential.data)

            Result.success(googleIdTokenCredential.idToken)
        } else {
            Result.failure(Exception("구글 로그인 응답이 올바르지 않습니다"))
        }
    }
}