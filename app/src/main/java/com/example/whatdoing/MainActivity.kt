package com.example.whatdoing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.*
import com.example.whatdoing.ui.album.AlbumScreen
import com.example.whatdoing.ui.album.PostDetailScreen
import com.example.whatdoing.ui.album.UploadScreen
import com.example.whatdoing.ui.common.ActionBottomBar
import com.example.whatdoing.ui.common.MainBottomBar
import com.example.whatdoing.ui.common.SearchTopBar
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

    var isSearchMode by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }
    var searchTags by remember { mutableStateOf(listOf<String>()) }
    val recentSearches = remember { mutableStateListOf("#야근", "#회식", "#헬스") }

    // 상세페이지 메뉴/신고 상태
    var showDetailMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }

    fun addTag() {
        val tag = searchInput.trim().let {
            if (it.startsWith("#")) it else "#$it"
        }

        if (tag.length > 1 && !searchTags.contains(tag)) {
            searchTags = searchTags + tag

            if (!recentSearches.contains(tag)) {
                recentSearches.add(0, tag)
            }
        }

        searchInput = ""
    }

    val showMainBottomBar = currentRoute in listOf("album", "ai", "mypage")

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),

        topBar = {
            when {

                currentRoute == "album" && isSearchMode -> {
                    SearchTopBar(
                        inputText = searchInput,
                        onInputChange = { searchInput = it },
                        searchTags = searchTags,
                        onAddTag = { addTag() },
                        onRemoveTag = { tag ->
                            searchTags = searchTags - tag
                        },
                        onBackClick = {
                            isSearchMode = false
                            searchTags = emptyList()
                            searchInput = ""
                        }
                    )
                }

                currentRoute == "album" -> {
                    WhatDoingTopBar(
                        title = "지금뭐해?",
                        actions = {
                            IconButton(
                                onClick = { isSearchMode = true }
                            ) {
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
                        onBackClick = { navController.popBackStack() }
                    )
                }

                currentRoute?.startsWith("detail") == true -> {
                    val detailTitle =
                        currentBackStackEntry?.arguments?.getString("title") ?: "상세"

                    WhatDoingTopBar(
                        title = detailTitle,
                        onBackClick = { navController.popBackStack() },
                        actions = {
                            IconButton(
                                onClick = { showDetailMenu = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "더보기"
                                )
                            }

                            DropdownMenu(
                                expanded = showDetailMenu,
                                onDismissRequest = {
                                    showDetailMenu = false
                                }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("신고하기") },
                                    onClick = {
                                        showDetailMenu = false
                                        showReportDialog = true
                                    }
                                )
                            }
                        }
                    )
                }
            }
        },

        bottomBar = {
            when {

                currentRoute?.startsWith("detail") == true -> {
                    ActionBottomBar(
                        buttonText = "저장하기",
                        onClick = {
                            // TODO 저장 기능
                        }
                    )
                }

                currentRoute == "upload" -> {
                    ActionBottomBar(
                        buttonText = "업로드",
                        onClick = {
                            // TODO 업로드 기능
                        }
                    )
                }

                showMainBottomBar -> {
                    MainBottomBar(
                        currentRoute = currentRoute ?: "album",
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo("album") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
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
                AlbumScreen(
                    navController = navController,
                    searchTags = searchTags,
                    recentSearches = recentSearches,
                    isSearchMode = isSearchMode,
                    onRecentSearchClick = { tag ->
                        searchInput = tag.removePrefix("#")
                        addTag()
                    }
                )
            }

            composable("ai") { /* TODO */ }

            composable("mypage") {
                MyPageScreen(navController = navController)
            }

            composable("upload") {
                UploadScreen(navController = navController)
            }

            composable("detail/{title}") { backStackEntry ->
                val title = backStackEntry.arguments?.getString("title") ?: ""

                PostDetailScreen(
                    navController = navController,
                    title = title,
                    tags = listOf("#야근", "#사무실", "#밤"),
                    imageUrl = null
                )
            }
        }

        // 신고 다이얼로그
        if (showReportDialog) {
            AlertDialog(
                onDismissRequest = {
                    showReportDialog = false
                    reportReason = ""
                },
                title = {
                    Text("신고하기")
                },
                text = {
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        placeholder = {
                            Text("신고 사유를 입력해주세요")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // TODO 신고 처리
                            showReportDialog = false
                            reportReason = ""
                        },
                        enabled = reportReason.isNotBlank()
                    ) {
                        Text("신고")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showReportDialog = false
                            reportReason = ""
                        }
                    ) {
                        Text("취소")
                    }
                }
            )
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