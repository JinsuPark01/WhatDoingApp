@file:OptIn(ExperimentalMaterial3Api::class)

package com.jinsupark.helpumta.ui.screen.record

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.jinsupark.helpumta.ui.component.FieldLabel
import com.jinsupark.helpumta.ui.component.FormField
import com.jinsupark.helpumta.ui.component.PillChip
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

        viewModel.handleIntent(RecordContract.Intent.UpdateImage(uri.toString()))
    }

    RecordContent(
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
private fun RecordContent(
    uiState: RecordContract.UiState,
    onIntent: (RecordContract.Intent) -> Unit,
    onNavigateBack: () -> Unit,
    onPickImage: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Text(
                        text = if (uiState.isEditMode) "기록 수정" else "운동 기록",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
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
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // ── 운동 종류 + 최근 종목 칩
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FormField(
                        label = "운동 종류",
                        value = uiState.workoutType,
                        onValueChange = { onIntent(RecordContract.Intent.UpdateWorkoutType(it)) },
                        placeholder = "헬스, 러닝, 사이클…"
                    )

                    if (uiState.recentWorkoutTypes.isNotEmpty()) {
                        RecentTypeChips(
                            types = uiState.recentWorkoutTypes,
                            selected = uiState.workoutType.trim(),
                            onSelect = { onIntent(RecordContract.Intent.UpdateWorkoutType(it)) }
                        )
                    }
                }

                FormField(
                    // "최대 1440"을 라벨에서 뺐다.
                    // 24시간이라는 상한을 사용자가 미리 알아야 할 이유가 없고,
                    // 넘겼을 때 알려주는 게 라벨에 적어두는 것보다 낫다.
                    label = "운동 시간 (분)",
                    value = uiState.workoutDuration,
                    onValueChange = { onIntent(RecordContract.Intent.UpdateDuration(it)) },
                    placeholder = "60",
                    keyboardType = KeyboardType.Number
                )

                // ── 인증샷
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FieldLabel("인증샷 (선택)")

                        if (uiState.imageUri != null) {
                            Text(
                                text = "제거",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable {
                                        onIntent(RecordContract.Intent.UpdateImage(null))
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            // 그룹 상세의 RecordCard가 4:3으로 자른다.
                            // 미리보기를 16:9로 두면 올릴 때 본 구도와 올린 뒤 구도가 달라진다.
                            .aspectRatio(4f / 3f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable { onPickImage() },
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
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "사진 추가",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                FormField(
                    label = "한줄 소감 (선택)",
                    value = uiState.comment,
                    onValueChange = { onIntent(RecordContract.Intent.UpdateComment(it)) },
                    placeholder = "오늘 운동 어땠나요?",
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { onIntent(RecordContract.Intent.SubmitRecord) },
                    enabled = uiState.isSubmitEnabled && !uiState.isLoading,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    // 버튼을 스피너로 갈아끼우면 높이가 달라져 화면이 들썩인다.
                    // 버튼은 그대로 두고 내용만 바꾼다.
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (uiState.isEditMode) "수정 완료" else "기록 남기기",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

/**
 * 최근 종목 칩 — 앱 공용 PillChip 사용.
 * 입력값과 같은 종목은 선택 상태로 칠해서 "지금 이걸로 들어가 있음"을 보여준다.
 */
@Composable
private fun RecentTypeChips(
    types: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        types.forEach { type ->
            PillChip(
                text = type,
                selected = type == selected,
                onClick = { onSelect(type) }
            )
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
            onNavigateBack = {},
            onPickImage = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RecordContentFilledPreview() {
    HelpumtaTheme {
        RecordContent(
            uiState = RecordContract.UiState(
                workoutType = "사이클",
                workoutDuration = "90",
                comment = "퇴근하고 바로 감",
                recentWorkoutTypes = listOf("사이클", "하체", "러닝", "등")
            ),
            onIntent = {},
            onNavigateBack = {},
            onPickImage = {}
        )
    }
}