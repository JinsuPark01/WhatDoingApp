package com.example.whatdoing.ui.screen.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {

    private val _uiState =
        MutableStateFlow(LoginContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect =
        MutableSharedFlow<LoginContract.SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    fun handleIntent(intent: LoginContract.Intent) {
        when (intent) {
            LoginContract.Intent.GoogleLogin -> {
                googleLogin()
            }
        }
    }

    private fun googleLogin() {
        // TODO
    }
}