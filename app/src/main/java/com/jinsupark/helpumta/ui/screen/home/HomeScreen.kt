@file:OptIn(ExperimentalMaterial3Api::class)

package com.jinsupark.helpumta.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jinsupark.helpumta.domain.model.Group
import com.jinsupark.helpumta.domain.model.GroupPolicy
import com.jinsupark.helpumta.ui.screen.home.components.GroupCard
import com.jinsupark.helpumta.ui.theme.HelpumtaTheme

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToGroup: (String) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToMyPage: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

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
            }
        }
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
        topBar = {
            TopAppBar(
                title = { Text("헬품타") },
                actions = {
                    IconButton(onClick = onMyPageClick) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "마이페이지"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ){
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
                uiState.isLoading -> {
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
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.groups,
                            key = { it.id }
                        ) { group ->
                            GroupCard(
                                group = group,
                                onClick = { onGroupClick(group.id) }
                            )
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
            .padding(horizontal = 24.dp)
    ) {
        // 세로 라인 + 인트로 문단
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "헬품타에 오신 걸 환영해요",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "친구들과 그룹을 만들어\n운동 기록을 함께 공유해보세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GuideItem("그룹은 최대 ${GroupPolicy.MAX_MEMBERS}명까지 참여할 수 있어요")
                GuideItem("하루에 한 번 운동 기록을 남길 수 있어요")
                GuideItem("그룹원들의 하루 기록을 한 장의 사진으로 저장할 수 있어요")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .shadow(2.dp, RoundedCornerShape(5.dp))
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "버튼으로 그룹을 만들어보세요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "친구에게 받은 초대 링크로도 참여할 수 있어요",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun GuideItem(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "·",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeContentPreview() {
    val dummyState = HomeContract.UiState(
        groups = listOf(
            Group(id = "1", name = "아침 운동 크루", memberCount = 5),
            Group(id = "2", name = "저녁 헬스 팀", memberCount = 3),
            Group(id = "3", name = "주말 등산", memberCount = 8),
            Group(id = "4", name = "헬창 모임", memberCount = 12)
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