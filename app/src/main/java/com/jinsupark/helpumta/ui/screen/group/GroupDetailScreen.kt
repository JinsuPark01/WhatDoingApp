@file:OptIn(ExperimentalMaterial3Api::class)

package com.jinsupark.helpumta.ui.screen.group

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jinsupark.helpumta.domain.model.Group
import com.jinsupark.helpumta.domain.model.WorkoutRecord
import com.jinsupark.helpumta.ui.screen.group.components.DayProgress
import com.jinsupark.helpumta.ui.screen.group.components.RecordCalendarDialog
import com.jinsupark.helpumta.ui.screen.group.components.RecordCard
import com.jinsupark.helpumta.ui.screen.group.components.StatsContent
import com.jinsupark.helpumta.ui.theme.HelpumtaTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun GroupDetailScreen(
    groupId: String,
    recordCreated: Boolean,
    onRecordCreatedHandled: () -> Unit,
    groupUpdated: Boolean,
    onGroupUpdatedHandled: () -> Unit,
    viewModel: GroupDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToRecord: (String) -> Unit,
    onNavigateToEditRecord: (String, String) -> Unit,
    onNavigateToEditGroup: (String) -> Unit,
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
            onRecordCreatedHandled()
        }
    }

    LaunchedEffect(groupUpdated) {
        if (groupUpdated) {
            viewModel.handleIntent(GroupDetailContract.Intent.RefreshGroup)
            onGroupUpdatedHandled()
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
            val inviteLink = "https://whatdoing-c4457.web.app/group/$groupId"
            clipboardManager.setText(AnnotatedString(inviteLink))
            Toast.makeText(context, "초대 링크를 복사했어요", Toast.LENGTH_SHORT).show()
        },
        onExtract = { onNavigateToExtract(groupId, uiState.selectedDate) },
        onEditGroup = { onNavigateToEditGroup(groupId) },
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
    onEditGroup: () -> Unit,
    onEditRecord: (String) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<WorkoutRecord?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Column {
                        Text(
                            text = uiState.group?.name ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        uiState.group?.let { group ->
                            Text(
                                text = "멤버 ${group.memberCount}명",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
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
                                text = { Text("초대 링크 복사") },
                                onClick = {
                                    menuExpanded = false
                                    onCopyInviteCode()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("그룹 정보 수정") },
                                onClick = {
                                    menuExpanded = false
                                    onEditGroup()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
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
            if (uiState.group != null &&
                uiState.selectedTab == GroupDetailContract.Tab.RECORDS
            ) {
                val done = uiState.hasWroteToday
                FloatingActionButton(
                    onClick = {
                        if (done) onIntent(GroupDetailContract.Intent.AlreadyWroteToday)
                        else onIntent(GroupDetailContract.Intent.NavigateToRecord)
                    },
                    // 테마 색을 따라가게 해서 다크모드에서도 자연스럽게 보이도록
                    containerColor = if (done) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (done) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = if (done) "오늘은 이미 작성했어요" else "오늘 기록 작성"
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

                        GroupTabs(
                            selected = uiState.selectedTab,
                            onSelect = { tab ->
                                onIntent(GroupDetailContract.Intent.SelectTab(tab))
                            }
                        )

                        when (uiState.selectedTab) {
                            GroupDetailContract.Tab.RECORDS -> {
                                DateHeader(
                                    selectedDate = uiState.selectedDate,
                                    wroteCount = uiState.records.map { it.userId }.distinct().size,
                                    memberCount = uiState.group.memberCount,
                                    onPrevDay = { onIntent(GroupDetailContract.Intent.MoveDay(-1)) },
                                    onNextDay = { onIntent(GroupDetailContract.Intent.MoveDay(1)) },
                                    onDateClick = {
                                        onIntent(GroupDetailContract.Intent.OpenCalendar)
                                        showDatePicker = true
                                    }
                                )

                                when {
                                    uiState.recordsErrorMessage != null -> {
                                        EmptyMessage(uiState.recordsErrorMessage)
                                    }
                                    uiState.records.isEmpty() -> {
                                        val isToday = isSameDay(
                                            uiState.selectedDate,
                                            System.currentTimeMillis()
                                        )
                                        EmptyMessage(
                                            if (isToday) "오늘 첫 기록을 남겨보세요"
                                            else "이 날은 기록이 없어요"
                                        )
                                    }
                                    else -> {
                                        LazyColumn(
                                            contentPadding = PaddingValues(
                                                start = 20.dp,
                                                end = 20.dp,
                                                top = 12.dp,
                                                bottom = 100.dp   // FAB 가림 방지
                                            ),
                                            verticalArrangement = Arrangement.spacedBy(32.dp)
                                        ) {
                                            items(
                                                items = uiState.records,
                                                key = { it.id }
                                            ) { record ->
                                                val isMine = record.userId == uiState.currentUserId
                                                val canEdit = isMine && isSameDay(
                                                    record.createdAt,
                                                    System.currentTimeMillis()
                                                )
                                                RecordCard(
                                                    record = record,
                                                    canEdit = canEdit,
                                                    canDelete = isMine,
                                                    onEditClick = { onEditRecord(record.id) },
                                                    onDeleteClick = { deleteTarget = record },
                                                    authorPhotoUrl = uiState.memberProfiles[record.userId]
                                                        ?.photoUrl.orEmpty()
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            GroupDetailContract.Tab.STATS -> {
                                when {
                                    uiState.isStatsLoading -> {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                    uiState.statsErrorMessage != null -> {
                                        EmptyMessage(uiState.statsErrorMessage)
                                    }
                                    uiState.stats != null -> {
                                        StatsContent(
                                            stats = uiState.stats,
                                            todayIndex = todayDayIndex()
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

    // 그룹 나가기 확인
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("그룹 나가기") },
            text = {
                Text("그룹을 나가면 이 그룹에 작성한 내 기록과 사진이 모두 삭제돼요. 되돌릴 수 없어요.")
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

    // 기록 삭제 확인
    deleteTarget?.let { target ->
        val isToday = isSameDay(target.createdAt, System.currentTimeMillis())
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("기록 삭제") },
            text = {
                Text(
                    if (isToday) "사진도 함께 삭제돼요. 오늘 기록은 다시 작성할 수 있어요."
                    else "사진도 함께 삭제돼요. 지난 날짜는 다시 작성할 수 없어요."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onIntent(GroupDetailContract.Intent.DeleteRecord(target.id))
                    deleteTarget = null
                }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("취소") }
            }
        )
    }

    // 날짜 선택 달력
    if (showDatePicker) {
        RecordCalendarDialog(
            selectedDate = uiState.selectedDate,
            displayedMonth = uiState.calendarMonth,
            markedDays = uiState.calendarMarks[uiState.calendarMonth] ?: emptySet(),
            isLoading = uiState.isCalendarLoading,
            onPrevMonth = { onIntent(GroupDetailContract.Intent.MoveMonth(-1)) },
            onNextMonth = { onIntent(GroupDetailContract.Intent.MoveMonth(1)) },
            onSelectDate = { millis ->
                onIntent(GroupDetailContract.Intent.SelectDate(millis))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

/**
 * 탭 2개뿐이라 TabRow를 쓰지 않는다.
 * 기본 TabRow는 선택/비선택 모두 primary로 칠해서 화면에 주황이 과해지고,
 * 인디케이터 오프셋 API가 Material3 버전마다 달라 손이 더 간다.
 * 선택된 탭만 onSurface로 또렷하게, 밑줄은 짧은 알약 하나.
 */
@Composable
private fun GroupTabs(
    selected: GroupDetailContract.Tab,
    onSelect: (GroupDetailContract.Tab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        GroupDetailContract.Tab.entries.forEach { tab ->
            val isSelected = tab == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    // when으로 쓰면 탭이 늘어났을 때 컴파일이 깨져서 알려준다
                    text = when (tab) {
                        GroupDetailContract.Tab.RECORDS -> "기록"
                        GroupDetailContract.Tab.STATS -> "통계"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(Modifier.height(8.dp))

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                } else {
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}

@Composable
private fun DateHeader(
    selectedDate: Long,
    wroteCount: Int,
    memberCount: Int,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onDateClick: () -> Unit
) {
    val isToday = isSameDay(selectedDate, System.currentTimeMillis())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevDay) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "이전 날",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formatDate(selectedDate),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .clickable { onDateClick() }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            IconButton(onClick = onNextDay, enabled = !isToday) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "다음 날",
                    tint = if (isToday) MaterialTheme.colorScheme.outlineVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        DayProgress(wroteCount = wroteCount, memberCount = memberCount)
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// "6월 27일 금" — 숫자 나열보다 읽기 쉽다
private fun formatDate(millis: Long): String {
    if (millis <= 0L) return ""
    return SimpleDateFormat("M월 d일 E", Locale.KOREA).format(Date(millis))
}

private fun isSameDay(a: Long, b: Long): Boolean {
    if (a <= 0L) return false
    val calA = Calendar.getInstance().apply { timeInMillis = a }
    val calB = Calendar.getInstance().apply { timeInMillis = b }
    return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
}

// 오늘 요일 (월=0 ... 일=6)
private fun todayDayIndex(): Int {
    val dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    return (dow + 5) % 7
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun GroupDetailContentPreview() {
    val dummyState = GroupDetailContract.UiState(
        group = Group(id = "1", name = "헬스팟", memberCount = 4),
        records = listOf(
            WorkoutRecord(
                id = "1", groupId = "1", userId = "user1", userName = "이정수",
                workoutType = "사이클", workoutDuration = 90,
                imageUrl = "", comment = "퇴근 후",
                createdAt = System.currentTimeMillis()
            ),
            WorkoutRecord(
                id = "2", groupId = "1", userId = "user2", userName = "최이랑",
                workoutType = "러닝", workoutDuration = 45,
                imageUrl = "", comment = "한강런",
                createdAt = System.currentTimeMillis() - 3600000
            )
        ),
        hasWroteToday = false,
        selectedDate = System.currentTimeMillis()
    )

    HelpumtaTheme {
        GroupDetailContent(
            uiState = dummyState,
            onIntent = {},
            onNavigateBack = {},
            onCopyInviteCode = {},
            onExtract = {},
            onEditGroup = {},
            onEditRecord = {}
        )
    }
}