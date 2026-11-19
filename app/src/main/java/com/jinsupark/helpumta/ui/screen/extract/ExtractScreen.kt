@file:OptIn(ExperimentalMaterial3Api::class)

package com.jinsupark.helpumta.ui.screen.extract

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.jinsupark.helpumta.domain.model.ExtractSlot
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

    // 캡처 전 이미지 로딩 완료 추적 (순수 UI 관심사라 ViewModel 미경유)
    val totalImageCount = remember(uiState.slots) {
        uiState.slots.count { it is ExtractSlot.Recorded && it.imageUrl.isNotBlank() }
    }
    var settledImageCount by remember(uiState.slots) { mutableIntStateOf(0) }
    val isImagesReady = settledImageCount >= totalImageCount

    val saveBitmap: () -> Unit = {
        scope.launch {
            val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
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
        },
        floatingActionButton = {
            if (uiState.slots.isNotEmpty()) {
                val busy = uiState.isSaving || !isImagesReady

                FloatingActionButton(
                    onClick = {
                        if (busy) return@FloatingActionButton
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            saveBitmap()
                        } else {
                            permissionLauncher.launch(
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Download, contentDescription = "이미지 저장")
                    }
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )

                uiState.slots.isEmpty() -> Text(
                    text = "이 날에는 추출할 기록이 없어요",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> ExtractCanvas(
                    slots = uiState.slots,
                    graphicsLayer = graphicsLayer,
                    onImageSettled = { settledImageCount++ }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// 아래는 앱 화면이 아니라 "저장되어 밖으로 나가는 이미지"다.
// 앱 테마(라이트/다크)를 따라가지 않고 검정으로 고정하며,
// 크기도 전부 고정값을 쓴다.
// ─────────────────────────────────────────────

private const val SLOT_MAX_HEIGHT_RATIO = 0.55f

private val CanvasBackground = Color.Black
private val EmptySlotBackground = Color(0xFF1A1A1A)
private val NoImageBackground = Color(0xFF3B4A5A)

@Composable
private fun ExtractCanvas(
    slots: List<ExtractSlot>,
    graphicsLayer: GraphicsLayer,
    onImageSettled: () -> Unit
) {
    val density = LocalDensity.current

    // 미리보기만 모서리를 둥글게 자른다. 캡처 대상은 바깥 Box 안쪽이라
    // 저장되는 이미지에는 둥근 모서리(=투명/검은 귀퉁이)가 남지 않는다.
    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(9f / 16f)
            .clip(MaterialTheme.shapes.large)
    ) {
        // 글꼴 배율을 1로 고정한다.
        // sp를 그대로 두면 시스템 글꼴을 키운 사람이 뽑은 이미지만 글자가 커져서,
        // 같은 날 같은 기록인데 누가 저장했느냐에 따라 결과물이 달라진다.
        CompositionLocalProvider(
            LocalDensity provides Density(density = density.density, fontScale = 1f)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(graphicsLayer)
                    }
                    .background(CanvasBackground)
            ) {
                val canvasHeight = maxHeight
                val canvasWidth = maxWidth
                val count = slots.size.coerceAtLeast(1)

                val evenHeight = canvasHeight / count
                val slotHeight = minOf(evenHeight, canvasWidth * SLOT_MAX_HEIGHT_RATIO)

                val totalSlotsHeight = slotHeight * count
                val verticalPadding =
                    ((canvasHeight - totalSlotsHeight) / 2).coerceAtLeast(0.dp)

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
                                is ExtractSlot.Recorded -> RecordedSlot(slot, onImageSettled)
                                is ExtractSlot.Empty -> EmptySlot(slot)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordedSlot(
    slot: ExtractSlot.Recorded,
    onImageSettled: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (slot.imageUrl.isNotBlank()) {
            AsyncImage(
                model = slot.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                // 성공/실패 모두 "더 기다릴 필요 없음"으로 처리
                onSuccess = { onImageSettled() },
                onError = { onImageSettled() }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NoImageBackground)
            )
        }

        // 균일한 오버레이는 밝은 사진에서 글자를 못 살린다.
        // 글자가 있는 양끝만 어둡게 깔고 가운데는 사진을 살린다.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.6f),
                        0.3f to Color.Black.copy(alpha = 0.18f),
                        0.7f to Color.Black.copy(alpha = 0.18f),
                        1f to Color.Black.copy(alpha = 0.6f)
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slot.nickname,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (slot.comment.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = slot.comment,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // "운동 시간"/"운동 종목" 라벨을 뺐다.
            // 180분과 등산은 라벨 없이도 읽히고, 9sp 라벨은 저장된 이미지에서 안 보인다.
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${slot.workoutDuration}분",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = slot.workoutType,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 140.dp)
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
            .background(EmptySlotBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = slot.nickname,
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(16.dp))

            // 이모지는 color 지정이 먹지 않아 💤가 파랗게 박혔다. 글자로 바꿨다.
            Text(
                text = "기록 없음",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 13.sp
            )
        }
    }
}