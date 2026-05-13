package com.example.whatdoing.ui.screen.auth

object LoginContract {

    data class UiState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    )

    sealed interface Intent {
        data object GoogleLogin : Intent
    }

    sealed interface SideEffect {
        data object NavigateToHome : SideEffect
        data class ShowToast(val message: String) : SideEffect
    }
}