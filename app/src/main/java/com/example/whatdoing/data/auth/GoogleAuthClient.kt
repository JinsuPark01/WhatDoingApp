package com.example.whatdoing.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.whatdoing.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun getIdToken(): Result<String> {
        // 6번 - WEB_CLIENT_ID 방어
        if (BuildConfig.WEB_CLIENT_ID.isBlank()) {
            return Result.failure(Exception("WEB_CLIENT_ID 설정이 없습니다"))
        }

        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

                val googleIdTokenCredential = GoogleIdTokenCredential
                    .createFrom(credential.data)

                Result.success(googleIdTokenCredential.idToken)
            } else {
                Result.failure(Exception("구글 로그인 응답이 올바르지 않습니다"))
            }
        } catch (e: GetCredentialCancellationException) {
            // 4번 - 사용자가 취소한 경우는 별도 처리
            Result.failure(Exception("로그인이 취소되었습니다"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}