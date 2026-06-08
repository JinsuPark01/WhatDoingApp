@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.whatdoing.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.whatdoing.domain.model.Group
import com.example.whatdoing.ui.screen.home.components.GroupCard
import com.example.whatdoing.ui.theme.WhatDoingTheme

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToGroup: (String) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToMyPage: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // 화면이 보일 때마다 그룹 목록 새로고침
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.handleIntent(HomeContract.Intent.LoadGroups)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is HomeContract.SideEffect.NavigateToGroup -> onNavigateToGroup(effect.groupId)
                HomeContract.SideEffect.NavigateToCreateGroup -> onNavigateToCreateGroup()
            }
        }
    }

    HomeContent(
        uiState = uiState,
        onGroupClick = { groupId ->
            viewModel.handleIntent(HomeContract.Intent.NavigateToGroup(groupId))
        },
        onCreateClick = {
            viewModel.handleIntent(HomeContract.Intent.NavigateToCreateGroup)
        },
        onMyPageClick = onNavigateToMyPage
    )
}

@Composable
private fun HomeContent(
    uiState: HomeContract.UiState,
    onGroupClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    onMyPageClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("헬품타") },
                actions = {
                    IconButton(onClick = onMyPageClick) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "마이페이지"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ){
                Icon(Icons.Default.Add, contentDescription = "그룹 만들기")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                uiState.groups.isEmpty() -> {
                    Text(
                        text = "아직 가입한 그룹이 없어요\n그룹을 만들어보세요!",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.groups,
                            key = { it.id }
                        ) { group ->
                            GroupCard(
                                group = group,
                                onClick = { onGroupClick(group.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeContentPreview() {
    val dummyState = HomeContract.UiState(
        groups = listOf(
            Group(id = "1", name = "아침 운동 크루", memberCount = 5),
            Group(id = "2", name = "저녁 헬스 팀", memberCount = 3),
            Group(id = "3", name = "주말 등산", memberCount = 8),
            Group(id = "4", name = "헬창 모임", memberCount = 12)
        )
    )

    WhatDoingTheme {
        HomeContent(
            uiState = dummyState,
            onGroupClick = {},
            onCreateClick = {},
            onMyPageClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeContentEmptyPreview() {
    WhatDoingTheme {
        HomeContent(
            uiState = HomeContract.UiState(),
            onGroupClick = {},
            onCreateClick = {},
            onMyPageClick = {}
        )
    }
}