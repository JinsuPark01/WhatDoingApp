package com.jinsupark.helpumta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.jinsupark.helpumta.data.auth.GoogleAuthClient
import com.jinsupark.helpumta.ui.navigation.NavGraph
import com.jinsupark.helpumta.ui.navigation.Screen
import com.jinsupark.helpumta.ui.screen.splash.SplashContract
import com.jinsupark.helpumta.ui.screen.splash.SplashViewModel
import com.jinsupark.helpumta.ui.theme.HelpumtaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: SplashViewModel by viewModels()

    // Credential Manager 호출은 Activity가 필요 → UI 레이어에서 보유
    @Inject lateinit var googleAuthClient: GoogleAuthClient

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            viewModel.uiState.value == SplashContract.UiState.Loading
        }

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            HelpumtaTheme {
                val navController = rememberNavController()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                when (uiState) {
                    SplashContract.UiState.Loading -> Unit
                    SplashContract.UiState.Authenticated -> {
                        NavGraph(
                            navController = navController,
                            startDestination = Screen.Home.route,
                            googleAuthClient = googleAuthClient
                        )
                    }
                    SplashContract.UiState.Unauthenticated -> {
                        NavGraph(
                            navController = navController,
                            startDestination = Screen.Login.route,
                            googleAuthClient = googleAuthClient
                        )
                    }
                }
            }
        }
    }
}