package com.example.whatdoing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.*
import com.example.whatdoing.ui.album.AlbumScreen
import com.example.whatdoing.ui.album.PostDetailScreen
import com.example.whatdoing.ui.album.UploadScreen
import com.example.whatdoing.ui.common.ActionBottomBar
import com.example.whatdoing.ui.common.MainBottomBar
import com.example.whatdoing.ui.common.WhatDoingTopBar
import com.example.whatdoing.ui.mypage.MyPageScreen
import com.example.whatdoing.ui.theme.WhatDoingTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WhatDoingTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {

    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf("album", "ai", "mypage")

    Scaffold(
        // 🔥 inset 충돌 방지
        contentWindowInsets = WindowInsets(0, 0, 0, 0),

        // ✅ TopBar는 여기서 관리
        topBar = {
            when {
                currentRoute == "album" -> {
                    WhatDoingTopBar(
                        title = "지금뭐해?",
                        actions = {
                            IconButton(onClick = {
                                navController.navigate("search")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "검색"
                                )
                            }
                        }
                    )
                }
                currentRoute == "ai" -> {
                    WhatDoingTopBar(title = "AI 수정")
                }
                currentRoute == "mypage" -> {
                    WhatDoingTopBar(title = "마이페이지")
                }
                currentRoute == "upload" -> {
                    WhatDoingTopBar(
                        title = "사진 올리기",
                        actions = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.Close, contentDescription = "닫기")
                            }
                        }
                    )
                }
                currentRoute?.startsWith("detail") == true -> {
                    val detailTitle = currentBackStackEntry?.arguments?.getString("title") ?: "상세"
                    WhatDoingTopBar(
                        title = detailTitle,
                        actions = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.Close, contentDescription = "닫기")
                            }
                        }
                    )
                }
                currentRoute == "search" -> {
                    WhatDoingTopBar(
                        title = "검색",
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        },

        bottomBar = {
            when {
                // 메인 탭
                currentRoute in listOf("album", "ai", "mypage") -> {
                    MainBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo("album") {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                // 업로드
                currentRoute == "upload" -> {
                    ActionBottomBar(
                        buttonText = "업로드",
                        onClick = {
                            // TODO: 업로드 기능 연결
                        }
                    )
                }

                // 상세 게시물
                currentRoute?.startsWith("detail") == true -> {
                    ActionBottomBar(
                        buttonText = "저장하기",
                        onClick = {
                            // TODO: 저장 기능 연결
                        }
                    )
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = "album",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("album") {
                AlbumScreen(navController = navController)
            }

            composable("ai") { /* TODO */ }
            composable("mypage") {
                MyPageScreen(navController = navController)
            }
            composable("upload") {
                UploadScreen(navController = navController)
            }
            composable("search") { /* TODO */ }

            composable("detail/{title}") { backStackEntry ->
                val title = backStackEntry.arguments?.getString("title") ?: ""
                // TODO: Firebase 연동 후 title로 실제 게시물 조회
                PostDetailScreen(
                    navController = navController,
                    title = title,
                    tags = listOf("#야근", "#사무실", "#밤"), // 임시 더미
                    imageUrl = null
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainAppPreview() {
    WhatDoingTheme {
        MainApp()
    }
}