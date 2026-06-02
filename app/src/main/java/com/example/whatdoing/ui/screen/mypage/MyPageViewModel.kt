package com.example.whatdoing.ui.screen.mypage

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatdoing.data.auth.GoogleAuthClient
import com.example.whatdoing.domain.model.AuthProvider
import com.example.whatdoing.domain.usecase.DeleteAccountUseCase
import com.example.whatdoing.domain.usecase.GetAuthProviderUseCase
import com.example.whatdoing.domain.usecase.GetUserProfileUseCase
import com.example.whatdoing.domain.usecase.LogoutUseCase
import com.example.whatdoing.domain.usecase.ReauthenticateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getAuthProviderUseCase: GetAuthProviderUseCase,
    private val reauthenticateUseCase: ReauthenticateUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val googleAuthClient: GoogleAuthClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPageContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<MyPageContract.SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    init {
        handleIntent(MyPageContract.Intent.LoadUserInfo)
    }

    fun handleIntent(intent: MyPageContract.Intent) {
        when (intent) {
            MyPageContract.Intent.LoadUserInfo -> loadUserInfo()
            MyPageContract.Intent.Logout -> logout()
            is MyPageContract.Intent.DeleteWithPassword ->
                reauthThenDelete { reauthenticateUseCase.withPassword(intent.password) }
            is MyPageContract.Intent.DeleteWithGoogle ->
                deleteWithGoogle(intent.activity)
            MyPageContract.Intent.ErrorShown ->
                _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadUserInfo() {
        val userProfile = getUserProfileUseCase()
        if (userProfile != null) {
            _uiState.update {
                it.copy(
                    nickname = userProfile.nickname,
                    email = userProfile.email,
                    authProvider = getAuthProviderUseCase()
                )
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _sideEffect.emit(MyPageContract.SideEffect.NavigateToLogin)
        }
    }

    private fun deleteWithGoogle(activity: Activity) {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true, errorMessage = null) }

        viewModelScope.launch {
            // 구글 재인증용 idToken 받기 (로그인 패턴과 동일)
            val tokenResult = googleAuthClient.getIdToken(activity)
            tokenResult.fold(
                onSuccess = { idToken ->
                    runReauthAndDelete { reauthenticateUseCase.withGoogle(idToken) }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = "구글 인증에 실패했어요. 다시 시도해주세요."
                        )
                    }
                }
            )
        }
    }

    private fun reauthThenDelete(reauth: suspend () -> Result<Unit>) {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true, errorMessage = null) }
        viewModelScope.launch {
            runReauthAndDelete(reauth)
        }
    }

    // 재인증 → 삭제 공통 로직
    private suspend fun runReauthAndDelete(reauth: suspend () -> Result<Unit>) {
        reauth().fold(
            onSuccess = {
                deleteAccountUseCase().fold(
                    onSuccess = {
                        _uiState.update { it.copy(isDeleting = false) }
                        _sideEffect.emit(MyPageContract.SideEffect.NavigateToLogin)
                    },
                    onFailure = {
                        _uiState.update {
                            it.copy(
                                isDeleting = false,
                                errorMessage = "계정 삭제에 실패했어요. 다시 시도해주세요."
                            )
                        }
                    }
                )
            },
            onFailure = {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = "본인 확인에 실패했어요. 비밀번호를 확인해주세요."
                    )
                }
            }
        )
    }
}