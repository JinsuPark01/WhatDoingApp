package com.jinsupark.helpumta.ui.screen.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jinsupark.helpumta.domain.model.HomeLayout
import com.jinsupark.helpumta.domain.usecase.DeleteAccountUseCase
import com.jinsupark.helpumta.domain.usecase.GetAuthProviderUseCase
import com.jinsupark.helpumta.domain.usecase.GetHomeLayoutUseCase
import com.jinsupark.helpumta.domain.usecase.GetUserProfileUseCase
import com.jinsupark.helpumta.domain.usecase.LogoutUseCase
import com.jinsupark.helpumta.domain.usecase.ReauthenticateUseCase
import com.jinsupark.helpumta.domain.usecase.SetHomeLayoutUseCase
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
    private val getHomeLayoutUseCase: GetHomeLayoutUseCase,
    private val setHomeLayoutUseCase: SetHomeLayoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPageContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<MyPageContract.SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    init {
        handleIntent(MyPageContract.Intent.LoadUserInfo)
        observeHomeLayout()
    }

    fun handleIntent(intent: MyPageContract.Intent) {
        when (intent) {
            MyPageContract.Intent.LoadUserInfo -> loadUserInfo()
            MyPageContract.Intent.Logout -> logout()
            is MyPageContract.Intent.DeleteWithPassword ->
                reauthThenDelete { reauthenticateUseCase.withPassword(intent.password) }
            is MyPageContract.Intent.DeleteWithGoogle ->
                reauthThenDelete { reauthenticateUseCase.withGoogle(intent.idToken) }
            MyPageContract.Intent.GoogleReauthCancelled -> {
                _uiState.update { it.copy(isDeleting = false) }
                viewModelScope.launch {
                    _sideEffect.emit(
                        MyPageContract.SideEffect.ShowToast("구글 인증에 실패했어요. 다시 시도해주세요.")
                    )
                }
            }
            is MyPageContract.Intent.ChangeHomeLayout ->
                changeHomeLayout(intent.layout)
        }
    }

    // Screen에서 Credential Manager 팝업을 띄우기 직전에 호출
    fun onGoogleReauthStarted() {
        _uiState.update { it.copy(isDeleting = true) }
    }

    private fun observeHomeLayout() {
        viewModelScope.launch {
            getHomeLayoutUseCase().collect { layout ->
                _uiState.update { it.copy(homeLayout = layout) }
            }
        }
    }

    private fun changeHomeLayout(layout: HomeLayout) {
        viewModelScope.launch {
            setHomeLayoutUseCase(layout)
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

    private fun reauthThenDelete(reauth: suspend () -> Result<Unit>) {
        _uiState.update { it.copy(isDeleting = true) }
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
                        _uiState.update { it.copy(isDeleting = false) }
                        _sideEffect.emit(
                            MyPageContract.SideEffect.ShowToast("계정 삭제에 실패했어요. 다시 시도해주세요.")
                        )
                    }
                )
            },
            onFailure = {
                _uiState.update { it.copy(isDeleting = false) }
                _sideEffect.emit(
                    MyPageContract.SideEffect.ShowToast("본인 확인에 실패했어요. 비밀번호를 확인해주세요.")
                )
            }
        )
    }
}