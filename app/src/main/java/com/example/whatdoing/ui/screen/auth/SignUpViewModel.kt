package com.example.whatdoing.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatdoing.domain.usecase.SaveUserUseCase
import com.example.whatdoing.domain.usecase.SignUpUseCase
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
                email = state.email.trim(),       // trim 추가
                password = state.password,
                nickname = state.nickname.trim()  // trim 추가
            )

            result.fold(
                onSuccess = {
                    saveUserUseCase()   // users 저장 (실패해도 흐름 진행)
                    _uiState.update { it.copy(isLoading = false) }
                    _sideEffect.emit(SignUpContract.SideEffect.ShowToast("회원가입이 완료됐어요!"))
                    _sideEffect.emit(SignUpContract.SideEffect.NavigateToHome)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "회원가입에 실패했습니다"
                    )}
                }
            )
        }
    }
}