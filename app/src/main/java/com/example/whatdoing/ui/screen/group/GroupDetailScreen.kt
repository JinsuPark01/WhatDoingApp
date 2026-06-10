@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.whatdoing.ui.screen.group

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.whatdoing.domain.model.Group
import com.example.whatdoing.domain.model.WorkoutRecord
import com.example.whatdoing.ui.screen.group.components.RecordCard
import com.example.whatdoing.ui.theme.DisabledGray
import com.example.whatdoing.ui.theme.OnDisabledGray
import com.example.whatdoing.ui.theme.WhatDoingTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun GroupDetailScreen(
    groupId: String,
    recordCreated: Boolean,
    onRecordCreatedHandled: () -> Unit,
    viewModel: GroupDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToRecord: (String) -> Unit,
    onNavigateToEditRecord: (String, String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToExtract: (String, Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(groupId) {
        viewModel.handleIntent(GroupDetailContract.Intent.LoadGroupDetail(groupId))
    }

    LaunchedEffect(recordCreated) {
        if (recordCreated) {
            viewModel.handleIntent(GroupDetailContract.Intent.RefreshToToday)
            onRecordCreatedHandled()   // 신호 비움 (여기서)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is GroupDetailContract.SideEffect.NavigateToRecord -> onNavigateToRecord(effect.groupId)
                is GroupDetailContract.SideEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                GroupDetailContract.SideEffect.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    GroupDetailContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        onNavigateBack = onNavigateBack,
        onCopyInviteCode = {
            val inviteLink = "helpmuta://group/$groupId"
            clipboardManager.setText(AnnotatedString(inviteLink))
            Toast.makeText(context, "초대 링크를 복사했어요!", Toast.LENGTH_SHORT).show()
        },
        onExtract = { onNavigateToExtract(groupId, uiState.selectedDate) },
        onEditRecord = { recordId -> onNavigateToEditRecord(groupId, recordId) }
    )
}

@Composable
private fun GroupDetailContent(
    uiState: GroupDetailContract.UiState,
    onIntent: (GroupDetailContract.Intent) -> Unit,
    onNavigateBack: () -> Unit,
    onCopyInviteCode: () -> Unit,
    onExtract: () -> Unit,
    onEditRecord: (String) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.group?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                actions = {
                    if (uiState.group != null) {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "메뉴")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("사진 추출") },
                                onClick = {
                                    menuExpanded = false
                                    onExtract()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Image, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("초대 코드 복사") },
                                onClick = {
                                    menuExpanded = false
                                    onCopyInviteCode()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("그룹 나가기", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    showLeaveDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.group != null) {
                FloatingActionButton(
                    onClick = {
                        if (!uiState.hasWroteToday) {
                            onIntent(GroupDetailContract.Intent.NavigateToRecord)
                        }
                    },
                    containerColor = if (uiState.hasWroteToday) {
                        DisabledGray
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (uiState.hasWroteToday) {
                        OnDisabledGray
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = if (uiState.hasWroteToday) "오늘은 이미 작성했어요" else "오늘 기록 작성"
                    )
                }
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
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                uiState.group != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // 날짜 네비게이션 바
                        DateNavigationBar(
                            selectedDate = uiState.selectedDate,
                            onPrevDay = { onIntent(GroupDetailContract.Intent.MoveDay(-1)) },
                            onNextDay = { onIntent(GroupDetailContract.Intent.MoveDay(1)) },
                            onDateClick = { showDatePicker = true }
                        )
                        HorizontalDivider()

                        when {
                            uiState.recordsErrorMessage != null -> {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Text(
                                        text = uiState.recordsErrorMessage,
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            uiState.records.isEmpty() -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "이 날에는 운동 기록이 없어요",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            else -> {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        items = uiState.records,
                                        key = { it.id }
                                    ) { record ->
                                        val canEdit = record.userId == uiState.currentUserId &&
                                                isSameDay(record.createdAt, System.currentTimeMillis())
                                        RecordCard(
                                            record = record,
                                            canEdit = canEdit,
                                            onEditClick = { onEditRecord(record.id) }
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

    // 그룹 나가기 확인 다이얼로그
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("그룹 나가기") },
            text = {
                Text("그룹을 나가면 이 그룹에 작성한 내 기록과 사진이 모두 삭제되며 복구할 수 없어요. 정말 나가시겠어요?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveDialog = false
                        onIntent(GroupDetailContract.Intent.LeaveGroup)
                    },
                    enabled = !uiState.isLeaving
                ) { Text("나가기", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("취소") }
            }
        )
    }

    // 날짜 선택 달력
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDate.takeIf { it > 0L },
            selectableDates = object : SelectableDates {
                // 오늘(로컬 끝)까지만 선택 가능 — 미래 차단
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onIntent(GroupDetailContract.Intent.SelectDate(millis))
                    }
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun DateNavigationBar(
    selectedDate: Long,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onDateClick: () -> Unit
) {
    // 오늘이면 다음날(>) 비활성화
    val isToday = isSameDay(selectedDate, System.currentTimeMillis())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onPrevDay) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "이전 날"
            )
        }

        Text(
            text = formatDate(selectedDate),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .clickable { onDateClick() }
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        IconButton(
            onClick = onNextDay,
            enabled = !isToday
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "다음 날"
            )
        }
    }
}

private fun formatDate(millis: Long): String {
    if (millis <= 0L) return ""
    val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
    return sdf.format(Date(millis))
}

private fun isSameDay(a: Long, b: Long): Boolean {
    if (a <= 0L) return false
    val calA = Calendar.getInstance().apply { timeInMillis = a }
    val calB = Calendar.getInstance().apply { timeInMillis = b }
    return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun GroupDetailContentPreview() {
    val dummyState = GroupDetailContract.UiState(
        group = Group(
            id = "1",
            name = "아침 운동 크루",
            memberCount = 5
        ),
        records = listOf(
            WorkoutRecord(
                id = "1",
                groupId = "1",
                userId = "user1",
                userName = "진수",
                workoutType = "헬스",
                workoutDuration = 60,
                imageUrl = "",
                comment = "오늘 하체 했어요 💪 데드 100kg 성공!",
                createdAt = System.currentTimeMillis()
            ),
            WorkoutRecord(
                id = "2",
                groupId = "1",
                userId = "user2",
                userName = "민수",
                workoutType = "러닝",
                workoutDuration = 30,
                imageUrl = "",
                comment = "한강 따라 3km 뛰었어요 🏃",
                createdAt = System.currentTimeMillis() - 3600000
            )
        ),
        hasWroteToday = false,
        selectedDate = System.currentTimeMillis()
    )

    WhatDoingTheme {
        GroupDetailContent(
            uiState = dummyState,
            onIntent = {},
            onNavigateBack = {},
            onCopyInviteCode = {},
            onExtract = {},
            onEditRecord = {}
        )
    }
}