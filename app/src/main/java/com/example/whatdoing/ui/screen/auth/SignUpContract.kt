package com.example.whatdoing.ui.screen.auth

import android.util.Patterns

object SignUpContract {

    data class UiState(
        val email: String = "",
        val password: String = "",
        val passwordConfirm: String = "",
        val nickname: String = "",
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    ) {
        val isValidEmail: Boolean
            get() = Patterns.EMAIL_ADDRESS.matcher(email).matches()

        val isSignUpEnabled: Boolean
            get() = isValidEmail &&
                    password.length >= 6 &&
                    password == passwordConfirm &&
                    nickname.trim().isNotBlank()
    }

    sealed interface Intent {
        data class UpdateEmail(val email: String) : Intent
        data class UpdatePassword(val password: String) : Intent
        data class UpdatePasswordConfirm(val passwordConfirm: String) : Intent
        data class UpdateNickname(val nickname: String) : Intent
        data object SubmitSignUp : Intent
    }

    sealed interface SideEffect {
        data object NavigateToHome : SideEffect
        data class ShowToast(val message: String) : SideEffect
    }
}