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
import com.jinsupark.helpumta.domain.model.GroupPolicy
import com.jinsupark.helpumta.ui.component.FormField
import com.jinsupark.helpumta.ui.theme.HelpumtaTheme

@Composable
fun GroupCreateScreen(
    groupId: String?,
    viewModel: GroupCreateViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToGroup: (String) -> Unit,
    onGroupUpdated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(groupId) {
        viewModel.handleIntent(GroupCreateContract.Intent.Initialize(groupId))
    }

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is GroupCreateContract.SideEffect.NavigateToGroup -> onNavigateToGroup(effect.groupId)
                GroupCreateContract.SideEffect.GroupUpdated -> onGroupUpdated()
                GroupCreateContract.SideEffect.NavigateBack -> onNavigateBack()
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
    val title = if (uiState.isEditMode) "그룹 정보 수정" else "그룹 만들기"
    val submitLabel = if (uiState.isEditMode) "저장" else "그룹 만들기"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Text(
                        text = title,
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── 그룹 대표 이미지
            // 16:9가 아니라 정사각이다. 홈 카드도, 참여 화면도 이 이미지를
            // 정사각으로 잘라 쓰기 때문에, 고를 때 보는 구도가 그대로여야 한다.
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
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
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (uiState.imageUri != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "이미지 변경",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { onPickImage() }
                        )
                        Text(
                            text = "제거",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable {
                                onIntent(GroupCreateContract.Intent.UpdateImage(null))
                            }
                        )
                    }
                } else {
                    Text(
                        text = "그룹 대표 이미지 (선택)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FormField(
                    label = "그룹 이름",
                    value = uiState.name,
                    onValueChange = { onIntent(GroupCreateContract.Intent.UpdateName(it)) },
                    placeholder = "헬스팟, 아침 러닝…"
                )
                LengthCounter(
                    current = uiState.name.length,
                    max = GroupPolicy.MAX_NAME_LENGTH
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FormField(
                    label = "그룹 설명 (선택)",
                    value = uiState.description,
                    onValueChange = { onIntent(GroupCreateContract.Intent.UpdateDescription(it)) },
                    placeholder = "어떤 모임인지 적어주세요",
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5
                )
                LengthCounter(
                    current = uiState.description.length,
                    max = GroupPolicy.MAX_DESCRIPTION_LENGTH
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { onIntent(GroupCreateContract.Intent.Submit) },
                enabled = uiState.isSubmitEnabled && !uiState.isLoading,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = submitLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

// "3/20" — 입력 칸 오른쪽 아래. 강조할 정보가 아니라 onSurfaceVariant로 조용히
@Composable
private fun LengthCounter(current: Int, max: Int) {
    Text(
        text = "$current/$max",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.End)
    )
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun GroupEditContentPreview() {
    HelpumtaTheme {
        GroupCreateContent(
            uiState = GroupCreateContract.UiState(
                groupId = "1",
                name = "헬스팟",
                description = "평일 저녁 운동 모임"
            ),
            onIntent = {},
            onNavigateBack = {},
            onPickImage = {}
        )
    }
}