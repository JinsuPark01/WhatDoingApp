package com.jinsupark.helpumta.ui.screen.auth

object LoginContract {

    data class UiState(
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    ) {
        val isLoginEnabled: Boolean
            get() = email.isNotBlank() && password.isNotBlank()
    }

    sealed interface Intent {
        data class UpdateEmail(val email: String) : Intent
        data class UpdatePassword(val password: String) : Intent
        data object SubmitLogin : Intent
        // Credential Manager 호출은 Screen에서, ViewModel은 idToken만 받음
        data class GoogleLogin(val idToken: String) : Intent
        data object GoogleLoginCancelled : Intent
    }

    sealed interface SideEffect {
        data object NavigateToHome : SideEffect
        data class ShowToast(val message: String) : SideEffect
    }
}