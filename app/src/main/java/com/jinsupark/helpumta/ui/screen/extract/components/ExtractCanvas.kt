package com.jinsupark.helpumta.ui.screen.extract.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jinsupark.helpumta.domain.model.ExtractRatio
import com.jinsupark.helpumta.domain.model.ExtractSlot
import com.jinsupark.helpumta.domain.model.ExtractStyle
import com.jinsupark.helpumta.domain.model.ExtractTemplate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────
// 아래는 앱 화면이 아니라 "저장되어 밖으로 나가는 이미지"다.
// 앱 테마(라이트/다크)를 따라가지 않고 색·크기를 전부 고정값으로 쓴다.
// ─────────────────────────────────────────────

/**
 * 캔버스는 항상 이 폭으로 그리고, 미리보기는 축소해서만 보여준다.
 * 미리보기 크기를 그대로 캡처하면 아래 옵션 UI만큼 작아진 만큼 저장 해상도도 떨어진다.
 * 360dp × 밀도 3.0 ≈ 1080px.
 */
private val CANVAS_WIDTH = 360.dp

private const val STRIP_SLOT_MAX_HEIGHT_RATIO = 0.55f

private val CanvasBackground = Color.Black
private val EmptySlotBackground = Color(0xFF1A1A1A)
private val NoImageBackground = Color(0xFF3B4A5A)

@Composable
fun ExtractPreview(
    slots: List<ExtractSlot>,
    style: ExtractStyle,
    groupName: String,
    dateMillis: Long,
    captureLayer: GraphicsLayer,
    onImageSettled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val canvasWidth = CANVAS_WIDTH
    val canvasHeight = CANVAS_WIDTH / style.ratio.widthOverHeight()

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        // 남는 공간에 맞춰 축소 비율 계산 (확대는 하지 않음)
        val scale = minOf(maxWidth / canvasWidth, maxHeight / canvasHeight, 1f)

        // 미리보기만 모서리를 둥글게 자른다. 캡처는 안쪽 원본 크기라 저장 이미지엔 모서리가 남지 않는다.
        Box(
            modifier = Modifier
                .size(canvasWidth * scale, canvasHeight * scale)
                .clip(MaterialTheme.shapes.large)
        ) {
            // 글꼴 배율을 1로 고정 — 시스템 글꼴을 키운 사람이 뽑은 이미지만 글자가 커지는 걸 막는다
            CompositionLocalProvider(
                LocalDensity provides Density(density = density.density, fontScale = 1f)
            ) {
                Box(
                    modifier = Modifier
                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                        .requiredSize(canvasWidth, canvasHeight)
                        // 축소는 바깥(표시)에만 — 아래 drawWithContent는 축소 전 원본을 기록한다
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                        .drawWithContent {
                            captureLayer.record { this@drawWithContent.drawContent() }
                            drawLayer(captureLayer)
                        }
                        .background(CanvasBackground)
                ) {
                    CanvasContent(
                        slots = slots,
                        style = style,
                        groupName = groupName,
                        dateMillis = dateMillis,
                        onImageSettled = onImageSettled
                    )
                }
            }
        }
    }
}

@Composable
private fun CanvasContent(
    slots: List<ExtractSlot>,
    style: ExtractStyle,
    groupName: String,
    dateMillis: Long,
    onImageSettled: () -> Unit
) {
    val recordedCount = slots.count { it is ExtractSlot.Recorded }
    val visibleSlots = if (style.includeEmpty) slots else slots.filterIsInstance<ExtractSlot.Recorded>()

    Column(modifier = Modifier.fillMaxSize()) {
        if (style.showHeader) {
            CanvasHeader(
                groupName = groupName,
                dateMillis = dateMillis,
                recordedCount = recordedCount,
                memberCount = slots.size
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (visibleSlots.isEmpty()) {
                Text(
                    text = "기록 없음",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 15.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                when (style.template) {
                    ExtractTemplate.STRIP -> StripLayout(
                        slots = visibleSlots,
                        showComment = style.showComment,
                        onImageSettled = onImageSettled
                    )
                    ExtractTemplate.GRID -> GridLayout(
                        slots = visibleSlots,
                        showComment = style.showComment,
                        hasHeader = style.showHeader,
                        onImageSettled = onImageSettled
                    )
                }
            }
        }
    }
}

// ── 헤더: 이미지만 봐도 언제·어느 그룹 기록인지 알 수 있게
@Composable
private fun CanvasHeader(
    groupName: String,
    dateMillis: Long,
    recordedCount: Int,
    memberCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 16.dp)
    ) {
        if (groupName.isNotBlank()) {
            Text(
                text = groupName,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
        }
        Text(
            text = "${formatDate(dateMillis)} · ${recordedCount}/${memberCount}명 기록",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

// ─────────────────────────────────────────────
// 스트립: 사진 띠를 세로로 나열 (기존 레이아웃)
// ─────────────────────────────────────────────

@Composable
private fun StripLayout(
    slots: List<ExtractSlot>,
    showComment: Boolean,
    onImageSettled: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val count = slots.size.coerceAtLeast(1)
        val evenHeight = maxHeight / count
        // 인원이 적을 때 띠 하나가 화면을 다 먹지 않도록 높이 상한
        val slotHeight = minOf(evenHeight, maxWidth * STRIP_SLOT_MAX_HEIGHT_RATIO)
        val verticalPadding = ((maxHeight - slotHeight * count) / 2).coerceAtLeast(0.dp)

        Column(modifier = Modifier.fillMaxSize()) {
            if (verticalPadding > 0.dp) Spacer(Modifier.height(verticalPadding))
            slots.forEach { slot ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(slotHeight)
                ) {
                    when (slot) {
                        is ExtractSlot.Recorded -> StripRecordedSlot(slot, showComment, onImageSettled)
                        is ExtractSlot.Empty -> StripEmptySlot(slot)
                    }
                }
            }
        }
    }
}

@Composable
private fun StripRecordedSlot(
    slot: ExtractSlot.Recorded,
    showComment: Boolean,
    onImageSettled: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        SlotImage(slot = slot, onImageSettled = onImageSettled)

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
                if (showComment && slot.comment.isNotBlank()) {
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
private fun StripEmptySlot(slot: ExtractSlot.Empty) {
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

// ─────────────────────────────────────────────
// 그리드: 그룹 최대 4명에 맞춘 타일
//  1~2명 → 세로로 쌓기 / 3명 → 위 2칸 + 아래 1칸(전체 폭) / 4명 → 2×2
// ─────────────────────────────────────────────

private val GRID_GAP = 4.dp
private val GRID_PADDING = 12.dp

@Composable
private fun GridLayout(
    slots: List<ExtractSlot>,
    showComment: Boolean,
    hasHeader: Boolean,
    onImageSettled: () -> Unit
) {
    val rows: List<List<ExtractSlot>> =
        if (slots.size <= 2) slots.map { listOf(it) } else slots.chunked(2)

    Column(
        modifier = Modifier
            .fillMaxSize()
            // 헤더가 있으면 위 여백은 헤더가 이미 줬다
            .padding(
                start = GRID_PADDING,
                end = GRID_PADDING,
                bottom = GRID_PADDING,
                top = if (hasHeader) 0.dp else GRID_PADDING
            ),
        verticalArrangement = Arrangement.spacedBy(GRID_GAP)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GRID_GAP)
            ) {
                row.forEach { slot ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        when (slot) {
                            is ExtractSlot.Recorded -> GridRecordedCell(slot, showComment, onImageSettled)
                            is ExtractSlot.Empty -> GridEmptyCell(slot)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridRecordedCell(
    slot: ExtractSlot.Recorded,
    showComment: Boolean,
    onImageSettled: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        SlotImage(slot = slot, onImageSettled = onImageSettled)

        // 글자가 아래쪽에 모이므로 아래만 어둡게 — 사진 윗부분은 그대로 살린다
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.5f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.65f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Text(
                text = slot.nickname,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${slot.workoutDuration}분 · ${slot.workoutType}",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showComment && slot.comment.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = slot.comment,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GridEmptyCell(slot: ExtractSlot.Empty) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmptySlotBackground)
            .padding(12.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = slot.nickname,
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = "기록 없음",
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 12.sp
        )
    }
}

// ─────────────────────────────────────────────
// 공통
// ─────────────────────────────────────────────

@Composable
private fun SlotImage(
    slot: ExtractSlot.Recorded,
    onImageSettled: () -> Unit
) {
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
}

private fun ExtractRatio.widthOverHeight(): Float = when (this) {
    ExtractRatio.STORY -> 9f / 16f
    ExtractRatio.FEED -> 4f / 5f
    ExtractRatio.SQUARE -> 1f
}

// "11월 22일 토"
private fun formatDate(millis: Long): String =
    SimpleDateFormat("M월 d일 E", Locale.KOREA).format(Date(millis))