package com.example.whatdoing.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatdoing.domain.usecase.EmailLoginUseCase
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
    private val emailLoginUseCase: EmailLoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<LoginContract.SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    fun handleIntent(intent: LoginContract.Intent) {
        when (intent) {
            is LoginContract.Intent.UpdateEmail -> {
                _uiState.update { it.copy(
                    email = intent.email,
                    errorMessage = null
                )}
            }
            is LoginContract.Intent.UpdatePassword -> {
                _uiState.update { it.copy(
                    password = intent.password,
                    errorMessage = null
                )}
            }
            LoginContract.Intent.GoogleLogin -> googleLogin()
            LoginContract.Intent.SubmitLogin -> emailLogin()
        }
    }

    private fun googleLogin() {
        // TODO
    }

    private fun emailLogin() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = emailLoginUseCase(
                email = _uiState.value.email,
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
}