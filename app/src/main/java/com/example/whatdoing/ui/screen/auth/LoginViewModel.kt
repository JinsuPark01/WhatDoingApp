package com.example.whatdoing.ui.screen.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatdoing.data.auth.GoogleAuthClient
import com.example.whatdoing.domain.usecase.EmailLoginUseCase
import com.example.whatdoing.domain.usecase.GoogleLoginUseCase
import com.example.whatdoing.domain.usecase.SaveUserUseCase
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
    private val googleAuthClient: GoogleAuthClient,
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
            is LoginContract.Intent.GoogleLogin -> googleLogin(intent.activity)
            LoginContract.Intent.SubmitLogin -> emailLogin()
        }
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
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "로그인에 실패했습니다"
                    )}
                }
            )
        }
    }

    private fun googleLogin(activity: Activity) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val tokenResult = googleAuthClient.getIdToken(activity)

            tokenResult.fold(
                onSuccess = { idToken ->
                    val loginResult = googleLoginUseCase(idToken)

                    loginResult.fold(
                        onSuccess = {
                            saveUserUseCase()   // 추가
                            _uiState.update { it.copy(isLoading = false) }
                            _sideEffect.emit(LoginContract.SideEffect.NavigateToHome)
                        },
                        onFailure = { e ->
                            _uiState.update { it.copy(
                                isLoading = false,
                                errorMessage = e.message ?: "구글 로그인에 실패했습니다"
                            )}
                        }
                    )
                },
                onFailure = { e ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "구글 계정을 가져올 수 없습니다"
                    )}
                }
            )
        }
    }
}