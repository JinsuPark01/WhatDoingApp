package com.jinsupark.helpumta.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jinsupark.helpumta.domain.model.AuthException
import com.jinsupark.helpumta.domain.usecase.EmailLoginUseCase
import com.jinsupark.helpumta.domain.usecase.GoogleLoginUseCase
import com.jinsupark.helpumta.domain.usecase.SaveUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val emailLoginUseCase: EmailLoginUseCase,
    private val googleLoginUseCase: GoogleLoginUseCase,
    private val saveUserUseCase: SaveUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<LoginContract.SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    fun handleIntent(intent: LoginContract.Intent) {
        when (intent) {
            is LoginContract.Intent.UpdateEmail -> {
                _uiState.update { it.copy(email = intent.email, errorMessage = null) }
            }
            is LoginContract.Intent.UpdatePassword -> {
                _uiState.update { it.copy(password = intent.password, errorMessage = null) }
            }
            is LoginContract.Intent.GoogleLogin -> googleLogin(intent.idToken)
            LoginContract.Intent.GoogleLoginCancelled -> {
                _uiState.update { it.copy(isLoading = false) }
            }
            LoginContract.Intent.SubmitLogin -> emailLogin()
        }
    }

    // Screen에서 Credential Manager 팝업을 띄우기 직전에 호출
    fun onGoogleLoginStarted() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
    }

    private fun emailLogin() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                errorMessage = null
            )}

            val result = emailLoginUseCase(
                email = _uiState.value.email.trim(),
                password = _uiState.value.password
            )

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    _sideEffect.emit(LoginContract.SideEffect.NavigateToHome)
                },
                onFailure = { e ->
                    val msg = (e as? AuthException)?.error?.toMessage()
                        ?: "잠시 후 다시 시도해주세요"
                    _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                }
            )
        }
    }

    private fun googleLogin(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            googleLoginUseCase(idToken).fold(
                onSuccess = {
                    saveUserUseCase()
                    _uiState.update { it.copy(isLoading = false) }
                    _sideEffect.emit(LoginContract.SideEffect.NavigateToHome)
                },
                onFailure = { e ->
                    val msg = (e as? AuthException)?.error?.toMessage()
                        ?: "구글 로그인에 실패했습니다"
                    _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                }
            )
        }
    }
}