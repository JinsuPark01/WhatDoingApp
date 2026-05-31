package com.example.whatdoing.ui.screen.splash

import androidx.lifecycle.ViewModel
import com.example.whatdoing.domain.usecase.CheckAuthStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val checkAuthStateUseCase: CheckAuthStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashContract.UiState>(SplashContract.UiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        _uiState.value = if (checkAuthStateUseCase()) {
            SplashContract.UiState.Authenticated
        } else {
            SplashContract.UiState.Unauthenticated
        }
    }
}