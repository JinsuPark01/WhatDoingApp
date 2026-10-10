package com.jinsupark.helpumta.ui.screen.mypage

import android.app.Activity
import com.jinsupark.helpumta.domain.model.AuthProvider
import com.jinsupark.helpumta.domain.model.HomeLayout

object MyPageContract {

    data class UiState(
        val nickname: String = "",
        val email: String = "",
        val authProvider: AuthProvider = AuthProvider.UNKNOWN,
        val isDeleting: Boolean = false,
        val homeLayout: HomeLayout = HomeLayout.GRID
    )

    sealed interface Intent {
        data object LoadUserInfo : Intent
        data object Logout : Intent
        data class DeleteWithPassword(val password: String) : Intent       // 이메일 사용자
        data class DeleteWithGoogle(val activity: Activity) : Intent        // 구글 사용자
        data class ChangeHomeLayout(val layout: HomeLayout) : Intent
    }

    sealed interface SideEffect {
        data object NavigateToLogin : SideEffect   // 로그아웃 / 계정삭제 공통 (로그인 화면으로)
        data class ShowToast(val message: String) : SideEffect
    }
}