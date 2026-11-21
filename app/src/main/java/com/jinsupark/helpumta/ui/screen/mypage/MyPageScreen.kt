@file:OptIn(ExperimentalMaterial3Api::class)

package com.jinsupark.helpumta.ui.screen.mypage

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.jinsupark.helpumta.BuildConfig
import com.jinsupark.helpumta.data.auth.GoogleAuthClient
import com.jinsupark.helpumta.domain.model.AuthProvider
import com.jinsupark.helpumta.domain.model.HomeLayout
import com.jinsupark.helpumta.domain.model.UserPolicy
import com.jinsupark.helpumta.ui.component.FormField
import com.jinsupark.helpumta.ui.screen.auth.components.AuthTextField
import com.jinsupark.helpumta.ui.theme.HelpumtaTheme
import com.jinsupark.helpumta.ui.util.openAppNotificationSettings
import kotlinx.coroutines.launch

private const val PRIVACY_URL = "https://jinsupark01.github.io/helpumta-policy/privacy.html"

@Composable
fun MyPageScreen(
    viewModel: MyPageViewModel = hiltViewModel(),
    googleAuthClient: GoogleAuthClient,
    onNavigateToLogin: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    var showNicknameDialog by remember { mutableStateOf(false) }
    var nicknameInput by remember { mutableStateOf("") }

    // 알림 on/off는 앱이 따로 저장하지 않고 OS 설정을 단일 진실 소스로 쓴다.
    // 앱 내 토글을 두면 OS 상태와 어긋나 "켜져 있는데 안 와요"가 생긴다.
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    // 시스템 설정에서 바꾸고 돌아왔을 때 화면이 거짓말하지 않도록 재진입마다 다시 읽는다
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled =
                    NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                MyPageContract.SideEffect.NavigateToLogin -> onNavigateToLogin()
                is MyPageContract.SideEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        // 포토피커가 아닌 폴백 경로(구형 기기)에서 비이미지가 넘어오는 케이스 방어
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType?.startsWith("image/") != true) {
            Toast.makeText(context, "이미지 파일만 선택할 수 있어요", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }

        viewModel.handleIntent(MyPageContract.Intent.UpdatePhoto(uri.toString()))
    }

    MyPageContent(
        uiState = uiState,
        notificationsEnabled = notificationsEnabled,
        onPickPhoto = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onRemovePhoto = { viewModel.handleIntent(MyPageContract.Intent.UpdatePhoto(null)) },
        onEditNickname = {
            nicknameInput = uiState.nickname
            showNicknameDialog = true
        },
        onLogoutClick = { showLogoutDialog = true },
        onDeleteClick = { showDeleteDialog = true },
        onNotificationClick = { context.openAppNotificationSettings() },
        onPrivacyClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, PRIVACY_URL.toUri()))
        },
        onNavigateBack = onNavigateBack,
        onChangeHomeLayout = { layout ->
            viewModel.handleIntent(MyPageContract.Intent.ChangeHomeLayout(layout))
        }
    )

    if (showNicknameDialog) {
        val trimmed = nicknameInput.trim()
        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            title = { Text("닉네임 변경") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FormField(
                        label = "닉네임",
                        value = nicknameInput,
                        onValueChange = { new ->
                            // 제한 이내면 허용, 넘으면 무시 (잘라내면 한글 조합이 깨짐).
                            // 제한이 생기기 전 이름은 이미 길 수 있으므로 줄어드는 변경은 항상 허용
                            if (new.length <= UserPolicy.MAX_NICKNAME_LENGTH ||
                                new.length < nicknameInput.length
                            ) {
                                nicknameInput = new
                            }
                        },
                        placeholder = "그룹원에게 보일 이름"
                    )
                    Text(
                        text = "${nicknameInput.length}/${UserPolicy.MAX_NICKNAME_LENGTH}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.End)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNicknameDialog = false
                        viewModel.handleIntent(MyPageContract.Intent.UpdateNickname(nicknameInput))
                    },
                    // 비어 있거나 그대로면 저장할 게 없다
                    enabled = trimmed.isNotEmpty() && trimmed != uiState.nickname
                ) { Text("저장") }
            },
            dismissButton = {
                TextButton(onClick = { showNicknameDialog = false }) { Text("취소") }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("로그아웃") },
            text = { Text("정말 로그아웃 하시겠어요?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.handleIntent(MyPageContract.Intent.Logout)
                }) { Text("로그아웃") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("취소") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("계정 삭제") },
            text = {
                Text("작성한 모든 운동 기록과 사진이 삭제되며 복구할 수 없어요.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    when (uiState.authProvider) {
                        AuthProvider.GOOGLE -> {
                            // Credential Manager 팝업은 Activity가 필요 → Screen에서 처리
                            viewModel.onGoogleReauthStarted()
                            scope.launch {
                                googleAuthClient.getIdToken(activity).fold(
                                    onSuccess = { idToken ->
                                        viewModel.handleIntent(
                                            MyPageContract.Intent.DeleteWithGoogle(idToken)
                                        )
                                    },
                                    onFailure = {
                                        viewModel.handleIntent(
                                            MyPageContract.Intent.GoogleReauthCancelled
                                        )
                                    }
                                )
                            }
                        }
                        else -> showPasswordDialog = true
                    }
                }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소") }
            }
        )
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                password = ""
            },
            title = { Text("본인 확인") },
            text = {
                Column {
                    Text("보안을 위해 비밀번호를 다시 입력해주세요.")
                    Spacer(Modifier.height(16.dp))
                    AuthTextField(
                        label = "비밀번호",
                        value = password,
                        onValueChange = { password = it },
                        isPassword = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPasswordDialog = false
                        viewModel.handleIntent(
                            MyPageContract.Intent.DeleteWithPassword(password)
                        )
                        password = ""
                    },
                    enabled = password.isNotBlank()
                ) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordDialog = false
                    password = ""
                }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun MyPageContent(
    uiState: MyPageContract.UiState,
    notificationsEnabled: Boolean,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onEditNickname: () -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onChangeHomeLayout: (HomeLayout) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Text("마이페이지", style = MaterialTheme.typography.titleLarge)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            ProfileHeader(
                nickname = uiState.nickname,
                email = uiState.email,
                photoUrl = uiState.photoUrl,
                isUpdatingPhoto = uiState.isUpdatingPhoto,
                isUpdatingNickname = uiState.isUpdatingNickname,
                onPickPhoto = onPickPhoto,
                onRemovePhoto = onRemovePhoto,
                onEditNickname = onEditNickname
            )

            SectionLabel("설정")

            SettingRow(
                title = "홈 화면 배치",
                description = if (uiState.homeLayout == HomeLayout.GRID) "2열 그리드" else "1열 목록",
                trailing = {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = uiState.homeLayout == HomeLayout.GRID,
                            onClick = { onChangeHomeLayout(HomeLayout.GRID) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {}
                        ) {
                            Icon(
                                Icons.Default.GridView,
                                contentDescription = "2열 그리드",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        SegmentedButton(
                            selected = uiState.homeLayout == HomeLayout.LIST,
                            onClick = { onChangeHomeLayout(HomeLayout.LIST) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {}
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = "1열 목록",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            )

            SettingRow(
                title = "알림",
                description = if (notificationsEnabled) {
                    "기록을 남기지 않은 날 저녁에 알려드려요"
                } else {
                    "꺼져 있어요 · 탭하면 설정으로 이동"
                },
                onClick = onNotificationClick
            )

            SectionLabel("계정")

            SettingRow(
                title = "로그아웃",
                onClick = onLogoutClick
            )

            SettingRow(
                title = "계정 삭제",
                description = if (uiState.isDeleting) "삭제하는 중…" else null,
                titleColor = MaterialTheme.colorScheme.error,
                onClick = if (uiState.isDeleting) null else onDeleteClick
            )

            SectionLabel("정보")

            SettingRow(
                title = "개인정보처리방침",
                onClick = onPrivacyClick,
                trailing = {
                    Text(
                        text = "↗",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            SettingRow(
                title = "버전",
                trailing = {
                    Text(
                        text = BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    nickname: String,
    email: String,
    photoUrl: String,
    isUpdatingPhoto: Boolean,
    isUpdatingNickname: Boolean,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onEditNickname: () -> Unit
) {
    var photoMenuExpanded by remember { mutableStateOf(false) }
    val hasPhoto = photoUrl.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── 아바타 + 연필 배지
        Box {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(enabled = !isUpdatingPhoto) {
                        // 사진이 없으면 고를 것만 남으므로 메뉴 없이 바로 피커
                        if (hasPhoto) photoMenuExpanded = true else onPickPhoto()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (hasPhoto) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "프로필 사진",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = nickname.firstOrNull()?.toString() ?: "?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isUpdatingPhoto) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // 배지는 터치를 소비하지 않는다 → 눌러도 아래 아바타의 clickable이 받는다.
            // 바깥 링을 배경색으로 둘러 사진/이니셜 위에서도 경계가 보이게
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(14.dp)
                )
            }

            DropdownMenu(
                expanded = photoMenuExpanded,
                onDismissRequest = { photoMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("사진 변경") },
                    onClick = {
                        photoMenuExpanded = false
                        onPickPhoto()
                    }
                )
                DropdownMenuItem(
                    text = { Text("사진 제거") },
                    onClick = {
                        photoMenuExpanded = false
                        onRemovePhoto()
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── 닉네임 + 연필
        // 왼쪽에 아이콘 폭만큼 빈 공간을 둬서 이름이 화면 가운데에 오게 한다
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(enabled = !isUpdatingNickname, onClick = onEditNickname)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(20.dp))
            Text(
                text = nickname,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.Edit,
                contentDescription = "닉네임 변경",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(Modifier.height(2.dp))

        Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        // 연한 회색에서 본문 색으로. 작지만 또렷해야 "라벨"로 읽힌다.
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 10.dp)
    )
}

/**
 * 설정 한 줄. 아이콘을 두지 않는다 —
 * 모든 줄에 아이콘이 붙으면 왼쪽에 의미 없는 기호 열이 생기고,
 * 정작 강조해야 할 "계정 삭제"가 묻힌다.
 */
@Composable
private fun SettingRow(
    title: String,
    description: String? = null,
    titleColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (titleColor == Color.Unspecified) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    titleColor
                }
            )
            if (description != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MyPageContentPreview() {
    HelpumtaTheme {
        MyPageContent(
            uiState = MyPageContract.UiState(
                nickname = "진수",
                email = "jinsu@jinsupark.com"
            ),
            notificationsEnabled = true,
            onPickPhoto = {},
            onRemovePhoto = {},
            onEditNickname = {},
            onLogoutClick = {},
            onDeleteClick = {},
            onNotificationClick = {},
            onPrivacyClick = {},
            onNavigateBack = {},
            onChangeHomeLayout = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "알림 꺼짐")
@Composable
private fun MyPageContentNotificationOffPreview() {
    HelpumtaTheme {
        MyPageContent(
            uiState = MyPageContract.UiState(
                nickname = "진수",
                email = "jinsu@jinsupark.com",
                homeLayout = HomeLayout.LIST
            ),
            notificationsEnabled = false,
            onPickPhoto = {},
            onRemovePhoto = {},
            onEditNickname = {},
            onLogoutClick = {},
            onDeleteClick = {},
            onNotificationClick = {},
            onPrivacyClick = {},
            onNavigateBack = {},
            onChangeHomeLayout = {}
        )
    }
}