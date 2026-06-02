@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.whatdoing.ui.screen.group

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
    onNavigateToRecord: (String) -> Unit,
    onNavigateToHome: () -> Unit   // 추가
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(groupId) {
        viewModel.handleIntent(GroupDetailContract.Intent.LoadGroupDetail(groupId))
    }

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is GroupDetailContract.SideEffect.NavigateToRecord -> onNavigateToRecord(effect.groupId)
                is GroupDetailContract.SideEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                GroupDetailContract.SideEffect.NavigateToHome -> onNavigateToHome()   // 추가
            }
        }
    }

    GroupDetailContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        onNavigateBack = onNavigateBack,
        onCopyInviteCode = {
            val inviteLink = "helpmuta://group/$groupId"
            clipboardManager.setText(AnnotatedString(inviteLink))
            Toast.makeText(context, "초대 링크를 복사했어요!", Toast.LENGTH_SHORT).show()
        }
    )
}

@Composable
private fun GroupDetailContent(
    uiState: GroupDetailContract.UiState,
    onIntent: (GroupDetailContract.Intent) -> Unit,
    onNavigateBack: () -> Unit,
    onCopyInviteCode: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }

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
                },
                actions = {
                    if (uiState.group != null) {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "메뉴"
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("초대 코드 복사") },
                                onClick = {
                                    menuExpanded = false
                                    onCopyInviteCode()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("그룹 나가기", color = MaterialTheme.colorScheme.error)
                                },
                                onClick = {
                                    menuExpanded = false
                                    showLeaveDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.group != null) {
                FloatingActionButton(
                    onClick = {
                        if (!uiState.hasWroteToday) {
                            onIntent(GroupDetailContract.Intent.NavigateToRecord)
                        }
                    },
                    containerColor = if (uiState.hasWroteToday) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    contentColor = if (uiState.hasWroteToday) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = if (uiState.hasWroteToday) "오늘은 이미 작성했어요" else "오늘 기록 작성"
                    )
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

    // 그룹 나가기 확인 다이얼로그
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("그룹 나가기") },
            text = {
                Text("그룹을 나가면 이 그룹에 작성한 내 기록과 사진이 모두 삭제되며 복구할 수 없어요. 정말 나가시겠어요?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveDialog = false
                        onIntent(GroupDetailContract.Intent.LeaveGroup)
                    },
                    enabled = !uiState.isLeaving
                ) { Text("나가기", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("취소") }
            }
        )
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
            onNavigateBack = {},
            onCopyInviteCode = {}
        )
    }
}