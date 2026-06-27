package com.jinsupark.helpumta.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jinsupark.helpumta.domain.model.AuthException
import com.jinsupark.helpumta.domain.usecase.SaveUserUseCase
import com.jinsupark.helpumta.domain.usecase.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase,
    private val saveUserUseCase: SaveUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<SignUpContract.SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    fun handleIntent(intent: SignUpContract.Intent) {
        when (intent) {
            is SignUpContract.Intent.UpdateEmail -> {
                _uiState.update { it.copy(email = intent.email, errorMessage = null) }
            }
            is SignUpContract.Intent.UpdatePassword -> {
                _uiState.update { it.copy(password = intent.password, errorMessage = null) }
            }
            is SignUpContract.Intent.UpdatePasswordConfirm -> {
                _uiState.update { it.copy(passwordConfirm = intent.passwordConfirm, errorMessage = null) }
            }
            is SignUpContract.Intent.UpdateNickname -> {
                _uiState.update { it.copy(nickname = intent.nickname, errorMessage = null) }
            }
            SignUpContract.Intent.SubmitSignUp -> signUp()
        }
    }

    private fun signUp() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val state = _uiState.value
            val result = signUpUseCase(
                email = state.email.trim(),
                password = state.password,
                nickname = state.nickname.trim()
            )

            result.fold(
                onSuccess = {
                    saveUserUseCase()
                    _uiState.update { it.copy(isLoading = false) }
                    _sideEffect.emit(SignUpContract.SideEffect.ShowToast("회원가입이 완료됐어요!"))
                    _sideEffect.emit(SignUpContract.SideEffect.NavigateToHome)
                },
                onFailure = { e ->
                    val msg = (e as? AuthException)?.error?.toMessage()
                        ?: "잠시 후 다시 시도해주세요"
                    _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                }
            )
        }
    }
}