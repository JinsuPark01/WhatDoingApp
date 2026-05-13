package com.example.whatdoing.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            // TODO LoginScreen()
        }
        composable(Screen.Home.route) {
            // TODO HomeScreen()
        }
        composable(Screen.WriteRecord.route) {
            // TODO WriteRecordScreen()
        }
        composable(Screen.MyPage.route) {
            // TODO MyPageScreen()
        }
        composable(Screen.GroupCreate.route) {
            // TODO GroupCreateScreen()
        }
        composable(Screen.GroupJoin.route) {
            // TODO GroupJoinScreen()
        }
        composable(Screen.GroupDetail.route) {
            // TODO GroupDetailScreen()
        }
    }
}