package com.example.whatdoing.ui.navigation

sealed class Screen(val route: String) {
    // 인증
    object Login : Screen("login")
    object SignUp : Screen("signup")

    // 메인
    object Home : Screen("home")
    object WriteRecord : Screen("record/{groupId}?recordId={recordId}") {
        fun createRoute(groupId: String, recordId: String? = null) =
            if (recordId != null) "record/$groupId?recordId=$recordId"
            else "record/$groupId"
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
    object Extract : Screen("extract/{groupId}/{dateMillis}") {
        fun createRoute(groupId: String, dateMillis: Long) = "extract/$groupId/$dateMillis"
    }
}