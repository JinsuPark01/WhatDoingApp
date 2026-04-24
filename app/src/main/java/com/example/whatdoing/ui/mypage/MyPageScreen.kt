package com.example.whatdoing.ui.mypage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whatdoing.ui.theme.WhatDoingTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(modifier: Modifier = Modifier) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("마이페이지") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // 프로필 영역
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 프로필 이미지 (임시)
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "👤", fontSize = 40.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "사용자 닉네임",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Divider()

            // 메뉴 목록
            MenuSection(title = "계정") {
                MenuItem(text = "내 게시물", onClick = { /* TODO */ })
                MenuItem(text = "내 정보 관리", onClick = { /* TODO */ })
                MenuItem(text = "차단 계정 관리", onClick = { /* TODO */ })
            }

            Divider()

            MenuSection(title = "서비스") {
                MenuItem(text = "의견 보내기", onClick = { /* TODO */ })
                MenuItem(text = "약관 및 정책", onClick = { /* TODO */ })
            }

            Divider()

            MenuSection(title = "기타") {
                MenuItem(text = "로그아웃", onClick = { showLogoutDialog = true })
                MenuItem(text = "탈퇴하기", onClick = { showDeleteAccountDialog = true })
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // 로그아웃 다이얼로그
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("로그아웃") },
            text = { Text("로그아웃 하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    // TODO: 로그아웃 처리
                }) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // 탈퇴 다이얼로그
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("탈퇴하기") },
            text = { Text("정말 탈퇴하시겠습니까?\n모든 데이터가 삭제되며 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        // TODO: 탈퇴 처리
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("탈퇴")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun MenuSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
fun MenuItem(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MyPageScreenPreview() {
    WhatDoingTheme {
        MyPageScreen()
    }
}