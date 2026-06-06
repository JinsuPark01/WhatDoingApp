package com.example.whatdoing.ui.screen.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatdoing.domain.usecase.CheckAuthStateUseCase
import com.example.whatdoing.domain.usecase.SaveUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val checkAuthStateUseCase: CheckAuthStateUseCase,
    private val saveUserUseCase: SaveUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashContract.UiState>(SplashContract.UiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        if (checkAuthStateUseCase()) {
            // 인증됨 → users 보정 후 홈
            viewModelScope.launch {
                saveUserUseCase()   // 기존 사용자 users 문서 없으면 생성
                _uiState.value = SplashContract.UiState.Authenticated
            }
        } else {
            _uiState.value = SplashContract.UiState.Unauthenticated
        }
    }
}