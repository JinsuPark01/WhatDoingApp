package com.example.whatdoing.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.whatdoing.ui.screen.auth.LoginScreen
import com.example.whatdoing.ui.screen.auth.SignUpScreen
import com.example.whatdoing.ui.screen.extract.ExtractScreen
import com.example.whatdoing.ui.screen.group.GroupCreateScreen
import com.example.whatdoing.ui.screen.group.GroupDetailScreen
import com.example.whatdoing.ui.screen.group.GroupJoinScreen
import com.example.whatdoing.ui.screen.home.HomeScreen
import com.example.whatdoing.ui.screen.mypage.MyPageScreen
import com.example.whatdoing.ui.screen.record.RecordScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String  // 추가
) {
    NavHost(
        navController = navController,
        startDestination = startDestination  // 변경
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                }
            )
        }
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToGroup = { groupId ->
                    navController.navigate(Screen.GroupDetail.createRoute(groupId))
                },
                onNavigateToCreateGroup = {
                    navController.navigate(Screen.GroupCreate.route)
                },
                onNavigateToMyPage = {
                    navController.navigate(Screen.MyPage.route)
                }
            )
        }
        composable(
            route = Screen.WriteRecord.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            RecordScreen(
                groupId = groupId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.MyPage.route) {
            MyPageScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.GroupCreate.route) {
            GroupCreateScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGroup = { groupId ->
                    navController.navigate(Screen.GroupDetail.createRoute(groupId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }
        composable(
            route = Screen.GroupJoin.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "helpmuta://group/{groupId}"
                }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            GroupJoinScreen(
                groupId = groupId,
                onNavigateToGroup = { gId ->
                    navController.navigate(Screen.GroupDetail.createRoute(gId)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.GroupDetail.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable

            GroupDetailScreen(
                groupId = groupId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRecord = { gId ->
                    navController.navigate(Screen.WriteRecord.createRoute(gId))
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToExtract = { gId, date ->
                    navController.navigate(Screen.Extract.createRoute(gId, date))
                }
            )
        }
        composable(
            route = Screen.Extract.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("dateMillis") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val dateMillis = backStackEntry.arguments?.getLong("dateMillis") ?: return@composable
            ExtractScreen(
                groupId = groupId,
                dateMillis = dateMillis,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}