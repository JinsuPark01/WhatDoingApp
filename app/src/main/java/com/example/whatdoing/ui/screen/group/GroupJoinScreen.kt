@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.whatdoing.ui.screen.group

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.whatdoing.domain.model.Group
import com.example.whatdoing.domain.model.GroupPolicy
import com.example.whatdoing.ui.theme.WhatDoingTheme

@Composable
fun GroupJoinScreen(
    groupId: String,
    viewModel: GroupJoinViewModel = hiltViewModel(),
    onNavigateToGroup: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(groupId) {
        viewModel.handleIntent(GroupJoinContract.Intent.Initialize(groupId))
    }

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is GroupJoinContract.SideEffect.NavigateToGroup -> onNavigateToGroup(effect.groupId)
                is GroupJoinContract.SideEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    GroupJoinContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun GroupJoinContent(
    uiState: GroupJoinContract.UiState,
    onIntent: (GroupJoinContract.Intent) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("그룹 참여") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
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
                uiState.errorMessage != null && uiState.group == null -> {
                    Text(
                        text = uiState.errorMessage,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                uiState.group != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 그룹 이미지
                        if (uiState.group.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = uiState.group.imageUrl,
                                contentDescription = "그룹 이미지",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = uiState.group.name.firstOrNull()?.toString() ?: "",
                                    style = MaterialTheme.typography.headlineLarge
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 그룹 이름
                        Text(
                            text = uiState.group.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 멤버 수
                        val isFull = uiState.group.memberCount >= GroupPolicy.MAX_MEMBERS
                        Text(
                            text = "멤버 ${uiState.group.memberCount} / ${GroupPolicy.MAX_MEMBERS}명",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isFull) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        if (isFull) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "정원이 가득 찼어요",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        // 그룹 설명
                        if (uiState.group.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = uiState.group.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 에러 메시지
                        uiState.errorMessage?.let {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(48.dp))

                        // 참여 버튼
                        if (uiState.isJoining) {
                            CircularProgressIndicator()
                        } else {
                            val isFull = uiState.group.memberCount >= GroupPolicy.MAX_MEMBERS
                            Button(
                                onClick = { onIntent(GroupJoinContract.Intent.SubmitJoin) },
                                enabled = !uiState.isJoining && !isFull,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(if (isFull) "정원 마감" else "이 그룹에 참여하기")
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
private fun GroupJoinContentPreview() {
    WhatDoingTheme {
        GroupJoinContent(
            uiState = GroupJoinContract.UiState(
                group = Group(
                    id = "1",
                    name = "아침 운동 크루",
                    description = "매일 아침 6시에 운동하는 사람들 모임",
                    memberCount = 5
                )
            ),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}