@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.whatdoing.ui.screen.mypage

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.whatdoing.domain.model.AuthProvider
import com.example.whatdoing.ui.theme.WhatDoingTheme

@Composable
fun MyPageScreen(
    viewModel: MyPageViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as Activity

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }   // 삭제 확인
    var showPasswordDialog by remember { mutableStateOf(false) } // 이메일 비번 입력

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                MyPageContract.SideEffect.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    MyPageContent(
        uiState = uiState,
        onLogoutClick = { showLogoutDialog = true },
        onDeleteClick = { showDeleteDialog = true },
        onNavigateBack = onNavigateBack
    )

    // 로그아웃 다이얼로그 (기존)
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("로그아웃") },
            text = { Text("정말 로그아웃 하시겠어요?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.handleIntent(MyPageContract.Intent.Logout)
                }) { Text("로그아웃") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("취소") }
            }
        )
    }

    // 계정 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("계정 삭제") },
            text = {
                Text("계정을 삭제하면 작성한 모든 운동 기록과 사진이 삭제되며 복구할 수 없어요. 정말 삭제하시겠어요?")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    // provider 따라 분기
                    when (uiState.authProvider) {
                        AuthProvider.GOOGLE ->
                            viewModel.handleIntent(MyPageContract.Intent.DeleteWithGoogle(activity))
                        else ->
                            showPasswordDialog = true  // 이메일 → 비번 입력 다이얼로그
                    }
                }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소") }
            }
        )
    }

    // 이메일 사용자 비번 재입력 다이얼로그
    if (showPasswordDialog) {
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("본인 확인") },
            text = {
                Column {
                    Text("보안을 위해 비밀번호를 다시 입력해주세요.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("비밀번호") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPasswordDialog = false
                        viewModel.handleIntent(
                            MyPageContract.Intent.DeleteWithPassword(password)
                        )
                    },
                    enabled = password.isNotBlank()
                ) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) { Text("취소") }
            }
        )
    }

    // 에러 메시지 (간단히 - 필요시 스낵바로 교체)
    uiState.errorMessage?.let { msg ->
        LaunchedEffect(msg) {
            // 임시: 토스트 대신 추후 스낵바 통일 예정
        }
    }
}

@Composable
private fun MyPageContent(
    uiState: MyPageContract.UiState,
    onLogoutClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("마이페이지") },
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
        ) {
            // 프로필 영역
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 프로필 이미지 (닉네임 첫 글자)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.nickname.firstOrNull()?.toString() ?: "?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = uiState.nickname,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = uiState.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            // 로그아웃 (기존)
            ListItem(
                headlineContent = { Text("로그아웃") },
                leadingContent = {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                },
                modifier = Modifier.clickable(onClick = onLogoutClick)
            )

            // 계정 삭제 (추가)
            ListItem(
                headlineContent = {
                    Text("계정 삭제", color = MaterialTheme.colorScheme.error)
                },
                leadingContent = {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.clickable(
                    enabled = !uiState.isDeleting,
                    onClick = onDeleteClick
                )
            )

            // 삭제 진행 중 표시
            if (uiState.isDeleting) {
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MyPageContentPreview() {
    WhatDoingTheme {
        MyPageContent(
            uiState = MyPageContract.UiState(
                nickname = "진수",
                email = "jinsu@example.com"
            ),
            onLogoutClick = {},
            onDeleteClick = {},
            onNavigateBack = {}
        )
    }
}