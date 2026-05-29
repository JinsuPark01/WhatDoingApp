object MyPageContract {

    data class UiState(
        val nickname: String = "",
        val email: String = ""
    )

    sealed interface Intent {
        data object LoadUserInfo : Intent
        data object Logout : Intent
    }

    sealed interface SideEffect {
        data object NavigateToLogin : SideEffect
    }
}