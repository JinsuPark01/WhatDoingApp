@file:OptIn(ExperimentalMaterial3Api::class)

package com.jinsupark.helpumta.ui.screen.record

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.jinsupark.helpumta.ui.theme.HelpumtaTheme

@Composable
fun RecordScreen(
    groupId: String,
    recordId: String? = null,
    viewModel: RecordViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onRecordCreated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(groupId, recordId) {
        viewModel.handleIntent(RecordContract.Intent.Initialize(groupId, recordId))
    }

    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                RecordContract.SideEffect.NavigateBack -> onRecordCreated()
                is RecordContract.SideEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    RecordContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun RecordContent(
    uiState: RecordContract.UiState,
    onIntent: (RecordContract.Intent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onIntent(RecordContract.Intent.UpdateImage(uri?.toString()))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "기록 수정" else "운동 기록") },
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
        if (uiState.isInitializing) {
            // 수정 모드: 기존 기록 불러오는 중
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 운동 종류
                OutlinedTextField(
                    value = uiState.workoutType,
                    onValueChange = { onIntent(RecordContract.Intent.UpdateWorkoutType(it)) },
                    label = { Text("운동 종류") },
                    placeholder = { Text("예: 헬스, 러닝, 사이클") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 운동 시간
                OutlinedTextField(
                    value = uiState.workoutDuration,
                    onValueChange = { onIntent(RecordContract.Intent.UpdateDuration(it)) },
                    label = { Text("운동 시간 (분)") },
                    placeholder = { Text("예: 60") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // 인증샷 라벨 + 사진 제거 (한 줄)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "인증샷 (선택)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (uiState.imageUri != null) {
                        TextButton(
                            onClick = { onIntent(RecordContract.Intent.UpdateImage(null)) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("사진 제거")
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.imageUri != null) {
                        AsyncImage(
                            model = uiState.imageUri,
                            contentDescription = "인증샷",
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
                                "사진 추가",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 한줄 소감
                OutlinedTextField(
                    value = uiState.comment,
                    onValueChange = { onIntent(RecordContract.Intent.UpdateComment(it)) },
                    label = { Text("한줄 소감 (선택)") },
                    placeholder = { Text("오늘 운동 어땠나요?") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                // 에러 메시지
                uiState.errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 작성/수정 완료 버튼
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    Button(
                        onClick = { onIntent(RecordContract.Intent.SubmitRecord) },
                        enabled = uiState.isSubmitEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(if (uiState.isEditMode) "수정 완료" else "기록 남기기")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RecordContentPreview() {
    HelpumtaTheme {
        RecordContent(
            uiState = RecordContract.UiState(),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}