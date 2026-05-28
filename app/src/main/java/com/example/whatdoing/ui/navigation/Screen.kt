package com.example.whatdoing.ui.navigation

sealed class Screen(val route: String) {
    // 인증
    object Login : Screen("login")
    object SignUp : Screen("signup")

    // 메인
    object Home : Screen("home")
    object WriteRecord : Screen("record/{groupId}") {
        fun createRoute(groupId: String) = "record/$groupId"
    }
    object MyPage : Screen("mypage")

    // 그룹
    object GroupCreate : Screen("group_create")
    object GroupJoin : Screen("group_join/{groupId}") {
        fun createRoute(groupId: String) = "group_join/$groupId"
    }
    object GroupDetail : Screen("group_detail/{groupId}") {
        fun createRoute(groupId: String) = "group_detail/$groupId"
    }
}