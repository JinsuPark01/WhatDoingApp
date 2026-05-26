package com.example.whatdoing.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.whatdoing.ui.screen.auth.LoginScreen
import com.example.whatdoing.ui.screen.group.GroupCreateScreen
import com.example.whatdoing.ui.screen.group.GroupDetailScreen
import com.example.whatdoing.ui.screen.home.HomeScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToGroup = { groupId ->
                    navController.navigate(Screen.GroupDetail.createRoute(groupId))
                },
                onNavigateToCreateGroup = {
                    navController.navigate(Screen.GroupCreate.route)
                }
            )
        }
        composable(Screen.WriteRecord.route) {
            // TODO WriteRecordScreen()
        }
        composable(Screen.MyPage.route) {
            // TODO MyPageScreen()
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
        composable(Screen.GroupJoin.route) {
            // TODO GroupJoinScreen()
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
                    navController.navigate(Screen.WriteRecord.route + "?groupId=$gId")
                }
            )
        }
    }
}