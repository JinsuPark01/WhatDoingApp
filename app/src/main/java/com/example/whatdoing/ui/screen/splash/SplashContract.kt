package com.example.whatdoing.ui.screen.splash

object SplashContract {
    sealed interface UiState {
        data object Loading : UiState
        data object Authenticated : UiState
        data object Unauthenticated : UiState
    }
}