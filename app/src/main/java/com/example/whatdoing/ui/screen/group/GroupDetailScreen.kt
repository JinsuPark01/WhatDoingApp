@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.whatdoing.ui.screen.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.whatdoing.domain.model.Group
import com.example.whatdoing.domain.model.WorkoutRecord
import com.example.whatdoing.ui.screen.group.components.RecordCard
import com.example.whatdoing.ui.theme.WhatDoingTheme

@Composable
fun GroupDetailScreen(
    groupId: String,
    viewModel: GroupDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToRecord: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(groupId) {
        viewModel.handleIntent(GroupDetailContract.Intent.LoadGroupDetail(groupId))
    }

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is GroupDetailContract.SideEffect.NavigateToRecord -> onNavigateToRecord(effect.groupId)
            }
        }
    }

    GroupDetailContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun GroupDetailContent(
    uiState: GroupDetailContract.UiState,
    onIntent: (GroupDetailContract.Intent) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.group?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.group != null && !uiState.hasWroteToday) {
                FloatingActionButton(
                    onClick = {
                        onIntent(GroupDetailContract.Intent.NavigateToRecord)
                    }
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "오늘 기록 작성")
                }
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
                // 그룹 자체를 못 불러온 경우만 전체 에러 화면
                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                // 그룹은 있을 때 → 기록 영역만 처리
                uiState.group != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        when {
                            uiState.recordsErrorMessage != null -> {
                                // 기록만 에러 카드로
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Text(
                                        text = uiState.recordsErrorMessage,
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            uiState.records.isEmpty() -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "아직 운동 기록이 없어요\n첫 번째 기록을 남겨보세요!",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            else -> {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        items = uiState.records,
                                        key = { it.id }
                                    ) { record ->
                                        RecordCard(record = record)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun GroupDetailContentPreview() {
    val dummyState = GroupDetailContract.UiState(
        group = Group(
            id = "1",
            name = "아침 운동 크루",
            memberCount = 5
        ),
        records = listOf(
            WorkoutRecord(
                id = "1",
                groupId = "1",
                userId = "user1",
                userName = "진수",
                workoutType = "헬스",
                workoutDuration = 60,
                imageUrl = "",
                comment = "오늘 하체 했어요 💪 데드 100kg 성공!",
                createdAt = System.currentTimeMillis()
            ),
            WorkoutRecord(
                id = "2",
                groupId = "1",
                userId = "user2",
                userName = "민수",
                workoutType = "러닝",
                workoutDuration = 30,
                imageUrl = "",
                comment = "한강 따라 3km 뛰었어요 🏃",
                createdAt = System.currentTimeMillis() - 3600000
            )
        ),
        hasWroteToday = false
    )

    WhatDoingTheme {
        GroupDetailContent(
            uiState = dummyState,
            onIntent = {},
            onNavigateBack = {}
        )
    }
}