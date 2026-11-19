@file:OptIn(ExperimentalMaterial3Api::class)

package com.jinsupark.helpumta.ui.screen.group

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.jinsupark.helpumta.domain.model.Group
import com.jinsupark.helpumta.domain.model.GroupPolicy
import com.jinsupark.helpumta.ui.theme.HelpumtaTheme

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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Text(
                        text = "그룹 참여",
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

                uiState.group == null -> {
                    NotFoundMessage(
                        message = uiState.errorMessage ?: "그룹을 찾을 수 없어요",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    val group = uiState.group
                    val isFull = group.memberCount >= GroupPolicy.MAX_MEMBERS

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.weight(1f))

                        // 원형이 아니라 둥근 사각이다.
                        // 원형은 사람 프로필의 관습이고, 이 이미지는 홈 카드에서
                        // 둥근 사각으로 쓰인다. 같은 이미지가 화면마다 다른 모양이면
                        // 같은 그룹인지 알아보기 어려워진다.
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(MaterialTheme.shapes.large)
                                .background(MaterialTheme.colorScheme.surfaceContainerLow),
                            contentAlignment = Alignment.Center
                        ) {
                            if (group.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = group.imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = group.name.take(1),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "멤버 ${group.memberCount}/${GroupPolicy.MAX_MEMBERS}명",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isFull) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (group.description.isNotBlank()) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = group.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        // 그룹 정보는 떠 있는데 참여만 실패한 경우
                        uiState.errorMessage?.let { message ->
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        Button(
                            onClick = { onIntent(GroupJoinContract.Intent.SubmitJoin) },
                            enabled = !uiState.isJoining && !isFull,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (uiState.isJoining) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (isFull) "정원이 가득 찼어요" else "이 그룹에 참여하기",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 빨간 글씨 한 줄만 띄우지 않는다.
 * 링크가 만료된 건 사용자 잘못도 아니고 시스템 고장도 아닌데,
 * error 색으로 칠하면 뭔가 망가진 것처럼 읽힌다.
 */
@Composable
private fun NotFoundMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "초대 링크가 만료됐거나 그룹이 사라졌을 수 있어요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun GroupJoinContentPreview() {
    HelpumtaTheme {
        GroupJoinContent(
            uiState = GroupJoinContract.UiState(
                group = Group(
                    id = "1",
                    name = "아침 운동 크루",
                    description = "매일 아침 6시에 운동하는 사람들 모임",
                    memberCount = 3
                )
            ),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun GroupJoinFullPreview() {
    HelpumtaTheme {
        GroupJoinContent(
            uiState = GroupJoinContract.UiState(
                group = Group(
                    id = "1",
                    name = "헬스팟",
                    memberCount = 4
                )
            ),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}