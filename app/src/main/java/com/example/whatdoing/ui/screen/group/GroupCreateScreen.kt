@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.whatdoing.ui.screen.group

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.whatdoing.ui.theme.WhatDoingTheme

@Composable
fun GroupCreateScreen(
    viewModel: GroupCreateViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToGroup: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is GroupCreateContract.SideEffect.NavigateToGroup -> onNavigateToGroup(effect.groupId)
                is GroupCreateContract.SideEffect.ShowToast -> { /* TODO */ }
            }
        }
    }

    GroupCreateContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun GroupCreateContent(
    uiState: GroupCreateContract.UiState,
    onIntent: (GroupCreateContract.Intent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onIntent(GroupCreateContract.Intent.UpdateImage(uri))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("그룹 만들기") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 그룹 대표 이미지
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (uiState.imageUri != null) {
                    AsyncImage(
                        model = uiState.imageUri,
                        contentDescription = "그룹 이미지",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "이미지 추가",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "그룹 대표 이미지 추가",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 그룹 이름
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { onIntent(GroupCreateContract.Intent.UpdateName(it)) },
                label = { Text("그룹 이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 그룹 설명
            OutlinedTextField(
                value = uiState.description,
                onValueChange = { onIntent(GroupCreateContract.Intent.UpdateDescription(it)) },
                label = { Text("그룹 설명 (선택)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            // 공개/비공개 토글
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "비공개 그룹",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "비밀번호를 아는 사람만 가입 가능",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.isPrivate,
                    onCheckedChange = {
                        onIntent(GroupCreateContract.Intent.UpdatePrivate(it))
                    }
                )
            }

            // 비밀번호 (비공개일 때만)
            if (uiState.isPrivate) {
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = { onIntent(GroupCreateContract.Intent.UpdatePassword(it)) },
                    label = { Text("비밀번호") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 에러 메시지
            uiState.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 생성 버튼
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                Button(
                    onClick = { onIntent(GroupCreateContract.Intent.SubmitCreate) },
                    enabled = uiState.isCreateEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("그룹 만들기")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun GroupCreateContentPreview() {
    WhatDoingTheme {
        GroupCreateContent(
            uiState = GroupCreateContract.UiState(),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}