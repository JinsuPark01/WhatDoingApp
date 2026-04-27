package com.example.whatdoing.ui.album

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.example.whatdoing.ui.theme.WhatDoingTheme

@Composable
fun PostDetailScreen(
    navController: NavController,
    title: String,
    tags: List<String>,
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    var showReportDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // 컨텐츠 + FAB
        Box(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 사진
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                ) {
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "📷", fontSize = 48.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 제목
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 태그
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tags.size) { index ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text(tags[index]) }
                        )
                    }
                }
            }

            // 신고 FAB (AlbumScreen 플로팅 버튼이랑 동일 위치)
            FloatingActionButton(
                onClick = { showReportDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("신고하기", fontSize = 14.sp)
                }
            }
        }

    }

    // 신고 다이얼로그
    if (showReportDialog) {
        var reportReason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("신고하기") },
            text = {
                OutlinedTextField(
                    value = reportReason,
                    onValueChange = { reportReason = it },
                    placeholder = { Text("신고 사유를 입력해주세요") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showReportDialog = false },
                    enabled = reportReason.isNotBlank()
                ) {
                    Text("신고")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PostDetailScreenPreview() {
    WhatDoingTheme {
        PostDetailScreen(
            navController = rememberNavController(),
            title = "야근하는 밤",
            tags = listOf("#야근", "#사무실", "#밤"),
            imageUrl = null
        )
    }
}