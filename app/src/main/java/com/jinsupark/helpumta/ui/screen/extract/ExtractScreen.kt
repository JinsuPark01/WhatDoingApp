@file:OptIn(ExperimentalMaterial3Api::class)

package com.jinsupark.helpumta.ui.screen.extract

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jinsupark.helpumta.domain.model.ExtractRatio
import com.jinsupark.helpumta.domain.model.ExtractSlot
import com.jinsupark.helpumta.domain.model.ExtractStyle
import com.jinsupark.helpumta.domain.model.ExtractTemplate
import com.jinsupark.helpumta.ui.component.PillChip
import com.jinsupark.helpumta.ui.screen.extract.components.ExtractPreview
import kotlinx.coroutines.launch

@Composable
fun ExtractScreen(
    groupId: String,
    dateMillis: Long,
    viewModel: ExtractViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val captureLayer = rememberGraphicsLayer()

    // 캡처 전 이미지 로딩 완료 추적 (순수 UI 관심사라 ViewModel 미경유)
    // 템플릿이 바뀌면 이미지 컴포저블이 새로 만들어져 다시 로드되므로 카운트도 다시 센다.
    // (비율·옵션 변경은 같은 컴포저블이 유지되어 콜백이 다시 오지 않으므로 키에 넣지 않는다)
    val totalImageCount = remember(uiState.slots) {
        uiState.slots.count { it is ExtractSlot.Recorded && it.imageUrl.isNotBlank() }
    }
    var settledImageCount by remember(uiState.slots, uiState.style.template) {
        mutableIntStateOf(0)
    }
    val isImagesReady = settledImageCount >= totalImageCount

    val saveBitmap: () -> Unit = {
        scope.launch {
            val bitmap = captureLayer.toImageBitmap().asAndroidBitmap()
            viewModel.handleIntent(ExtractContract.Intent.Save(bitmap))
        }
    }

    // 권한 런처 (Android 9 이하용)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) saveBitmap()
        else Toast.makeText(context, "저장하려면 권한이 필요해요", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(groupId, dateMillis) {
        viewModel.handleIntent(ExtractContract.Intent.Load(groupId, dateMillis))
    }

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is ExtractContract.SideEffect.ShowToast ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                ExtractContract.SideEffect.NavigateBack ->
                    onNavigateBack()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = { Text("사진 추출", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator()

                uiState.errorMessage != null -> Text(
                    text = uiState.errorMessage!!,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )

                uiState.slots.isEmpty() -> Text(
                    text = "이 날에는 추출할 기록이 없어요",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    // 미리보기는 남는 공간에 맞춰 축소된다. 저장 해상도는 영향받지 않는다.
                    ExtractPreview(
                        slots = uiState.slots,
                        style = uiState.style,
                        groupName = uiState.groupName,
                        dateMillis = uiState.selectedDate,
                        captureLayer = captureLayer,
                        onImageSettled = { settledImageCount++ },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    )

                    ExtractControls(
                        style = uiState.style,
                        onStyleChange = { style ->
                            viewModel.handleIntent(ExtractContract.Intent.UpdateStyle(style))
                        }
                    )

                    val busy = uiState.isSaving || !isImagesReady
                    // FAB는 아래 옵션 칩을 가린다 → 다른 폼 화면과 같은 하단 버튼으로
                    Button(
                        onClick = {
                            if (busy) return@Button
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                saveBitmap()
                            } else {
                                permissionLauncher.launch(
                                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                )
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)
                            .height(52.dp)
                    ) {
                        // 버튼을 스피너로 갈아끼우면 높이가 달라져 화면이 들썩인다 → 내용만 교체
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("이미지 저장", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtractControls(
    style: ExtractStyle,
    onStyleChange: (ExtractStyle) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ControlRow(label = "스타일") {
            ExtractTemplate.entries.forEach { template ->
                PillChip(
                    text = template.label(),
                    selected = style.template == template,
                    onClick = { onStyleChange(style.copy(template = template)) }
                )
            }
        }
        ControlRow(label = "비율") {
            ExtractRatio.entries.forEach { ratio ->
                PillChip(
                    text = ratio.label(),
                    selected = style.ratio == ratio,
                    onClick = { onStyleChange(style.copy(ratio = ratio)) }
                )
            }
        }
        ControlRow(label = "표시") {
            PillChip(
                text = "그룹·날짜",
                selected = style.showHeader,
                onClick = { onStyleChange(style.copy(showHeader = !style.showHeader)) }
            )
            PillChip(
                text = "소감",
                selected = style.showComment,
                onClick = { onStyleChange(style.copy(showComment = !style.showComment)) }
            )
            PillChip(
                text = "기록 없는 멤버",
                selected = style.includeEmpty,
                onClick = { onStyleChange(style.copy(includeEmpty = !style.includeEmpty)) }
            )
        }
    }
}

// 라벨 + 가로 스크롤 칩 줄. 라벨 폭을 고정해 세 줄의 칩 시작점을 맞춘다
@Composable
private fun ControlRow(
    label: String,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(44.dp)
        )
        content()
    }
}

// 표시 이름은 UI 몫 — 도메인 enum은 이름을 모른다
private fun ExtractTemplate.label(): String = when (this) {
    ExtractTemplate.STRIP -> "스트립"
    ExtractTemplate.GRID -> "그리드"
}

private fun ExtractRatio.label(): String = when (this) {
    ExtractRatio.STORY -> "9:16"
    ExtractRatio.FEED -> "4:5"
    ExtractRatio.SQUARE -> "1:1"
}