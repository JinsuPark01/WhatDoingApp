package com.example.whatdoing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.whatdoing.ui.navigation.NavGraph
import com.example.whatdoing.ui.navigation.Screen
import com.example.whatdoing.ui.screen.splash.SplashContract
import com.example.whatdoing.ui.screen.splash.SplashViewModel
import com.example.whatdoing.ui.theme.WhatDoingTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            viewModel.uiState.value == SplashContract.UiState.Loading
        }

        enableEdgeToEdge()
        setContent {
            WhatDoingTheme {
                val navController = rememberNavController()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                when (uiState) {
                    SplashContract.UiState.Loading -> Unit
                    SplashContract.UiState.Authenticated -> {
                        NavGraph(
                            navController = navController,
                            startDestination = Screen.Home.route
                        )
                    }
                    SplashContract.UiState.Unauthenticated -> {
                        NavGraph(
                            navController = navController,
                            startDestination = Screen.Login.route
                        )
                    }
                }
            }
        }
    }
}