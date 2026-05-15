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
        data object GoogleLogin : Intent
        data object SubmitLogin : Intent
    }

    sealed interface SideEffect {
        data object NavigateToHome : SideEffect
        data class ShowToast(val message: String) : SideEffect
    }
}
