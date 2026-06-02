package com.example.whatdoing.ui.screen.mypage

import android.app.Activity
import com.example.whatdoing.domain.model.AuthProvider

object MyPageContract {

    data class UiState(
        val nickname: String = "",
        val email: String = "",
        val authProvider: AuthProvider = AuthProvider.UNKNOWN,
        val isDeleting: Boolean = false,
        val errorMessage: String? = null
    )

    sealed interface Intent {
        data object LoadUserInfo : Intent
        data object Logout : Intent
        data class DeleteWithPassword(val password: String) : Intent       // 이메일 사용자
        data class DeleteWithGoogle(val activity: Activity) : Intent        // 구글 사용자
        data object ErrorShown : Intent
    }

    sealed interface SideEffect {
        data object NavigateToLogin : SideEffect   // 로그아웃 / 계정삭제 공통 (로그인 화면으로)
    }
}