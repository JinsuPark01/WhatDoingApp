@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.whatdoing.ui.screen.extract

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.whatdoing.domain.model.ExtractSlot
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
    val graphicsLayer = rememberGraphicsLayer()
    // 권한 런처 (Android 9 이하용)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                viewModel.handleIntent(ExtractContract.Intent.Save(bitmap))
            }
        } else {
            Toast.makeText(context, "저장하려면 권한이 필요해요", Toast.LENGTH_SHORT).show()
        }
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
        topBar = {
            TopAppBar(
                title = { Text("사진 추출") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.slots.isNotEmpty() && !uiState.isSaving) {
                FloatingActionButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // 10+ : 권한 없이 바로 저장
                            scope.launch {
                                val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                viewModel.handleIntent(ExtractContract.Intent.Save(bitmap))
                            }
                        } else {
                            // 9 이하 : 권한 요청 후 저장
                            permissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Download, contentDescription = "이미지 저장")
                }
            } else if (uiState.isSaving) {
                FloatingActionButton(
                    onClick = {},
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
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
                    color = MaterialTheme.colorScheme.error
                )
                uiState.slots.isEmpty() -> Text(
                    text = "이 날에는 추출할 기록이 없어요",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> ExtractCanvas(slots = uiState.slots, graphicsLayer = graphicsLayer)
            }
        }
    }
}

// 추출 결과물 본체 (나중에 이걸 캡처함)
// 칸 세로 최대 비율 (칸높이 = 캔버스폭 × 이 값). 보면서 조정.
private const val SLOT_MAX_HEIGHT_RATIO = 0.55f

@Composable
private fun ExtractCanvas(
    slots: List<ExtractSlot>,
    graphicsLayer: GraphicsLayer
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(16.dp))
            .drawWithContent {
                // 레이어에 기록 후 화면에도 그림
                graphicsLayer.record { this@drawWithContent.drawContent() }
                drawLayer(graphicsLayer)
            }
            .background(Color.Black)
    ) {
        val canvasHeight = maxHeight
        val canvasWidth = maxWidth
        val count = slots.size.coerceAtLeast(1)

        // 칸 높이 = min(꽉채운높이, 상한). 상한 = 폭 × 비율
        val evenHeight = canvasHeight / count
        val maxHeight = canvasWidth * SLOT_MAX_HEIGHT_RATIO
        val slotHeight = minOf(evenHeight, maxHeight)

        // 위아래 여백 (남는 공간 절반씩)
        val totalSlotsHeight = slotHeight * count
        val verticalPadding = ((canvasHeight - totalSlotsHeight) / 2).coerceAtLeast(0.dp)

        Column(modifier = Modifier.fillMaxSize()) {
            if (verticalPadding > 0.dp) {
                Spacer(Modifier.height(verticalPadding))
            }
            slots.forEach { slot ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(slotHeight)
                ) {
                    when (slot) {
                        is ExtractSlot.Recorded -> RecordedSlot(slot)
                        is ExtractSlot.Empty -> EmptySlot(slot)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordedSlot(slot: ExtractSlot.Recorded) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 배경: 인증샷
        if (slot.imageUrl.isNotBlank()) {
            AsyncImage(
                model = slot.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF3B4A5A))
            )
        }
        // 가독성용 어둡게
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
        )
        // 텍스트 (좌: 이름/소감, 우: 시간/종목)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 좌측
            Column {
                Text(
                    text = slot.nickname,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                if (slot.comment.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = slot.comment,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
            // 우측 (값 + 라벨)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${slot.workoutDuration}분",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "운동 시간",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 9.sp
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = slot.workoutType,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "운동 종목",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun EmptySlot(slot: ExtractSlot.Empty) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "zzz",
                color = Color(0xFF888888),
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = slot.nickname,
                color = Color(0xFF777777),
                fontSize = 13.sp
            )
        }
    }
}