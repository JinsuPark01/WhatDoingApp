@file:OptIn(ExperimentalMaterial3Api::class)

package com.jinsupark.helpumta.ui.screen.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jinsupark.helpumta.domain.model.Group
import com.jinsupark.helpumta.domain.model.GroupPolicy
import com.jinsupark.helpumta.domain.model.GroupToday
import com.jinsupark.helpumta.domain.model.HomeLayout
import com.jinsupark.helpumta.ui.screen.home.components.GroupCard
import com.jinsupark.helpumta.ui.screen.home.components.GroupListCard
import com.jinsupark.helpumta.ui.theme.HelpumtaTheme
import com.jinsupark.helpumta.ui.util.openAppNotificationSettings

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToGroup: (String) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToMyPage: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    // 거부 직후에만 뜨는 안내. 저장하지 않으므로 화면을 벗어나면 사라진다.
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    // 안드로이드는 2번 거부하면 다이얼로그를 영구 차단한다.
    // 그래서 거부 직후 이 자리에서 설정 경로를 알려주는 게 유일하게 타이밍이 좋다.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) showPermissionDeniedDialog = true
    }

    // 화면이 보일 때마다 그룹 목록 새로고침
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.handleIntent(HomeContract.Intent.LoadGroups)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is HomeContract.SideEffect.NavigateToGroup -> onNavigateToGroup(effect.groupId)
                HomeContract.SideEffect.NavigateToCreateGroup -> onNavigateToCreateGroup()
                HomeContract.SideEffect.RequestNotificationPermission -> {
                    // Android 13(API 33) 미만은 런타임 권한이 없음 → 아무것도 하지 않음
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!granted) {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("알림을 받을 수 없어요") },
            text = {
                Text("오늘 기록을 남기지 않았을 때 저녁에 알려드려요.\n휴대폰 설정에서 언제든 켤 수 있어요.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDeniedDialog = false
                        context.openAppNotificationSettings()
                    }
                ) {
                    Text("설정으로 이동")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("나중에")
                }
            }
        )
    }

    HomeContent(
        uiState = uiState,
        onGroupClick = { groupId ->
            viewModel.handleIntent(HomeContract.Intent.NavigateToGroup(groupId))
        },
        onCreateClick = {
            viewModel.handleIntent(HomeContract.Intent.NavigateToCreateGroup)
        },
        onMyPageClick = onNavigateToMyPage
    )
}

@Composable
private fun HomeContent(
    uiState: HomeContract.UiState,
    onGroupClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    onMyPageClick: () -> Unit
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
                        text = "헬품타",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onMyPageClick) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "마이페이지",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "그룹 만들기")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.groups.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                uiState.groups.isEmpty() -> {
                    EmptyGuide(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    val padding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 8.dp,
                        bottom = 100.dp   // FAB 가림 방지
                    )

                    when (uiState.homeLayout) {
                        HomeLayout.GRID -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = padding,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = uiState.groups,
                                    key = { it.group.id }
                                ) { item ->
                                    GroupCard(
                                        item = item,
                                        onClick = { onGroupClick(item.group.id) }
                                    )
                                }
                            }
                        }
                        HomeLayout.LIST -> {
                            LazyColumn(
                                contentPadding = padding,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(
                                    items = uiState.groups,
                                    key = { it.group.id }
                                ) { item ->
                                    GroupListCard(
                                        item = item,
                                        onClick = { onGroupClick(item.group.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGuide(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        Text(
            text = "헬품타에 오신 걸 환영해요",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "친구들과 그룹을 만들어\n운동 기록을 함께 공유해보세요",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(36.dp))

        GuideLine("최대 ${GroupPolicy.MAX_MEMBERS}명까지 함께할 수 있어요")
        GuideLine("하루에 한 번 운동 기록을 남겨요")
        GuideLine("그룹원들의 하루 기록을 사진 한 장으로 저장해요")

        Spacer(Modifier.height(36.dp))

        Text(
            text = "오른쪽 아래 + 버튼으로 시작하거나\n친구에게 받은 초대 링크로 참여할 수 있어요",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GuideLine(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeContentPreview() {
    val dummyState = HomeContract.UiState(
        groups = listOf(
            GroupToday(
                group = Group(id = "1", name = "아침 운동 크루", memberCount = 4),
                wroteCount = 2, didIWrite = false
            ),
            GroupToday(
                group = Group(id = "2", name = "저녁 헬스 팀", memberCount = 3),
                wroteCount = 3, didIWrite = true
            ),
            GroupToday(
                group = Group(id = "3", name = "주말 등산", memberCount = 4),
                wroteCount = 0, didIWrite = false
            ),
            GroupToday(
                group = Group(id = "4", name = "헬창 모임", memberCount = 2),
                wroteCount = 1, didIWrite = true
            )
        )
    )

    HelpumtaTheme {
        HomeContent(
            uiState = dummyState,
            onGroupClick = {},
            onCreateClick = {},
            onMyPageClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeContentEmptyPreview() {
    HelpumtaTheme {
        HomeContent(
            uiState = HomeContract.UiState(),
            onGroupClick = {},
            onCreateClick = {},
            onMyPageClick = {}
        )
    }
}