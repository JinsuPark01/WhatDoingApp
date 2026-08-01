@file:OptIn(ExperimentalMaterial3Api::class)

package com.jinsupark.helpumta.ui.screen.group

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.jinsupark.helpumta.ui.theme.HelpumtaTheme

@Composable
fun GroupCreateScreen(
    viewModel: GroupCreateViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToGroup: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is GroupCreateContract.SideEffect.NavigateToGroup -> onNavigateToGroup(effect.groupId)
                is GroupCreateContract.SideEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        // 포토피커가 아닌 폴백 경로(구형 기기)에서 비이미지가 넘어오는 케이스 방어
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType?.startsWith("image/") != true) {
            Toast.makeText(context, "이미지 파일만 선택할 수 있어요", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }

        viewModel.handleIntent(GroupCreateContract.Intent.UpdateImage(uri.toString()))
    }

    GroupCreateContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        onNavigateBack = onNavigateBack,
        onPickImage = {
            imagePickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    )
}

@Composable
private fun GroupCreateContent(
    uiState: GroupCreateContract.UiState,
    onIntent: (GroupCreateContract.Intent) -> Unit,
    onNavigateBack: () -> Unit,
    onPickImage: () -> Unit
) {
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
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onPickImage() },
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
    HelpumtaTheme {
        GroupCreateContent(
            uiState = GroupCreateContract.UiState(),
            onIntent = {},
            onNavigateBack = {},
            onPickImage = {}
        )
    }
}