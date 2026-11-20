package com.jinsupark.helpumta.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.jinsupark.helpumta.data.auth.GoogleAuthClient
import com.jinsupark.helpumta.ui.screen.auth.LoginScreen
import com.jinsupark.helpumta.ui.screen.auth.SignUpScreen
import com.jinsupark.helpumta.ui.screen.extract.ExtractScreen
import com.jinsupark.helpumta.ui.screen.group.GroupCreateScreen
import com.jinsupark.helpumta.ui.screen.group.GroupDetailScreen
import com.jinsupark.helpumta.ui.screen.group.GroupJoinScreen
import com.jinsupark.helpumta.ui.screen.home.HomeScreen
import com.jinsupark.helpumta.ui.screen.mypage.MyPageScreen
import com.jinsupark.helpumta.ui.screen.record.RecordScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    googleAuthClient: GoogleAuthClient
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                googleAuthClient = googleAuthClient,
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
                    navController.navigate(Screen.GroupCreate.createRoute())
                },
                onNavigateToMyPage = {
                    navController.navigate(Screen.MyPage.route)
                }
            )
        }
        composable(
            route = Screen.WriteRecord.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("recordId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val recordId = backStackEntry.arguments?.getString("recordId")
            RecordScreen(
                groupId = groupId,
                recordId = recordId,
                onNavigateBack = { navController.popBackStack() },
                onRecordCreated = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle?.set("recordCreated", true)
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.MyPage.route) {
            MyPageScreen(
                googleAuthClient = googleAuthClient,
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
        composable(
            route = Screen.GroupCreate.route,
            arguments = listOf(
                navArgument("groupId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId")
            GroupCreateScreen(
                groupId = groupId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGroup = { createdId ->
                    navController.navigate(Screen.GroupDetail.createRoute(createdId)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onGroupUpdated = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle?.set("groupUpdated", true)
                    navController.popBackStack()
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
                    uriPattern = "helpumta://group/{groupId}"
                },
                navDeepLink {
                    uriPattern = "https://whatdoing-c4457.web.app/group/{groupId}"
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
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable

            val recordCreated by backStackEntry.savedStateHandle
                .getStateFlow("recordCreated", false)
                .collectAsStateWithLifecycle()

            val groupUpdated by backStackEntry.savedStateHandle
                .getStateFlow("groupUpdated", false)
                .collectAsStateWithLifecycle()

            GroupDetailScreen(
                groupId = groupId,
                recordCreated = recordCreated,
                onRecordCreatedHandled = {
                    backStackEntry.savedStateHandle["recordCreated"] = false
                },
                groupUpdated = groupUpdated,
                onGroupUpdatedHandled = {
                    backStackEntry.savedStateHandle["groupUpdated"] = false
                },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRecord = { gId ->
                    navController.navigate(Screen.WriteRecord.createRoute(gId))
                },
                onNavigateToEditRecord = { gId, recordId ->
                    navController.navigate(Screen.WriteRecord.createRoute(gId, recordId))
                },
                onNavigateToEditGroup = { gId ->
                    navController.navigate(Screen.GroupCreate.createRoute(gId))
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