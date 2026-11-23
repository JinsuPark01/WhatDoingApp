package com.jinsupark.helpumta.ui.screen.extract.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jinsupark.helpumta.R
import com.jinsupark.helpumta.domain.model.ExtractColor
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
 * 360dp × 밀도 3.0 ≈ 1080px.
 */
private val CANVAS_WIDTH = 360.dp

private const val STRIP_SLOT_MAX_HEIGHT_RATIO = 0.55f

private val EmptySlotBackground = Color(0xFF1A1A1A)
private val NoImageBackground = Color(0xFF3B4A5A)

// ── 색 옵션 실제 값
private val BrandOrange = Color(0xFFFD6F22)
private val TextBlack = Color(0xFF1A1A1A)

// 카드 전용 — 완전 흰/검정 배경은 흰 액자와 어울리지 않아 결을 바꾼다
private val CardBeige = Color(0xFFF5F3EF)
private val CardCharcoal = Color(0xFF26231F)

// 카드 액자 안쪽 — 배경색과 무관하게 고정 (흰 글씨를 고르면 흰 액자 안에서 사라진다)
private val CardSurface = Color.White
private val CardPhotoPlaceholder = Color(0xFFE6E3DC)
private val CardCaptionPrimary = Color(0xFF1A1A1A)
private val CardCaptionSecondary = Color(0xFF777777)

/** 색 옵션 → 실제 배경색. 카드만 예외 매핑 */
internal fun ExtractColor.backgroundColor(template: ExtractTemplate): Color = when (this) {
    ExtractColor.ORANGE -> BrandOrange
    ExtractColor.BLACK -> if (template == ExtractTemplate.CARD) CardCharcoal else Color.Black
    ExtractColor.WHITE -> if (template == ExtractTemplate.CARD) CardBeige else Color.White
}

/** 색 옵션 → 실제 글씨색 */
internal fun ExtractColor.textColor(): Color = when (this) {
    ExtractColor.ORANGE -> BrandOrange
    ExtractColor.BLACK -> TextBlack
    ExtractColor.WHITE -> Color.White
}

/** 배경과 헤더 글씨색 (사진 위 글씨·카드 캡션은 여기에 포함되지 않는다) */
private data class CanvasColors(
    val background: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

private fun ExtractStyle.canvasColors(): CanvasColors {
    val p = palette
    val text = p.text.textColor()
    // 주황이 끼면 대비가 약해서 보조 글씨를 덜 흐리게
    val secondaryAlpha =
        if (p.background == ExtractColor.ORANGE || p.text == ExtractColor.ORANGE) 0.85f else 0.65f
    return CanvasColors(
        background = p.background.backgroundColor(template),
        textPrimary = text,
        textSecondary = text.copy(alpha = secondaryAlpha)
    )
}

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
    val colors = style.canvasColors()

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
                        .background(colors.background)
                ) {
                    CanvasContent(
                        slots = slots,
                        style = style,
                        colors = colors,
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
    colors: CanvasColors,
    groupName: String,
    dateMillis: Long,
    onImageSettled: () -> Unit
) {
    val recordedCount = slots.count { it is ExtractSlot.Recorded }
    val visibleSlots = if (style.includeEmpty) slots else slots.filterIsInstance<ExtractSlot.Recorded>()

    // 타이포는 헤더가 레이아웃의 일부라 자체적으로 그린다
    if (style.template == ExtractTemplate.TYPO) {
        TypoLayout(
            slots = visibleSlots,
            memberCount = slots.size,
            recordedCount = recordedCount,
            ratio = style.ratio,
            showHeader = style.showHeader,
            showComment = style.showComment,
            showLogo = style.isLogoVisible,
            colors = colors,
            groupName = groupName,
            dateMillis = dateMillis
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (style.showHeader) {
            CanvasHeader(
                groupName = groupName,
                dateMillis = dateMillis,
                recordedCount = recordedCount,
                memberCount = slots.size,
                showLogo = style.isLogoVisible,
                colors = colors
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
                    color = colors.textSecondary,
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
                    ExtractTemplate.CARD -> CardLayout(
                        slots = visibleSlots,
                        showComment = style.showComment,
                        hasHeader = style.showHeader,
                        onImageSettled = onImageSettled
                    )
                    ExtractTemplate.TYPO -> Unit   // 위에서 처리
                }
            }
        }
    }
}

// ── 헤더: 이미지만 봐도 언제·어느 그룹 기록인지 알 수 있게 + 오른쪽 로고
private val HEADER_LOGO_HEIGHT = 22.dp
private val HEADER_PADDING = 22.dp

@Composable
private fun CanvasHeader(
    groupName: String,
    dateMillis: Long,
    recordedCount: Int,
    memberCount: Int,
    showLogo: Boolean,
    colors: CanvasColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = HEADER_PADDING,
                top = HEADER_PADDING,
                bottom = 16.dp,
                // 로고가 있으면 오른쪽 여백 없이 — 밑줄이 캔버스 끝까지 나간다
                end = if (showLogo) 0.dp else HEADER_PADDING
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (groupName.isNotBlank()) {
                Text(
                    text = groupName,
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = "${formatDate(dateMillis)} · ${recordedCount}/${memberCount}명 기록",
                color = colors.textSecondary,
                fontSize = 13.sp,
                maxLines = 1
            )
        }

        if (showLogo) {
            Spacer(Modifier.width(12.dp))
            // 로고 폭 + 원래 오른쪽 여백만큼 → 선이 여백 구간을 지나 캔버스 끝에 닿는다
            LogoWithUnderline(
                height = HEADER_LOGO_HEIGHT,
                color = colors.textPrimary,
                modifier = Modifier.width(HEADER_LOGO_HEIGHT * LOGO_ASPECT + HEADER_PADDING)
            )
        }
    }
}

// ─────────────────────────────────────────────
// 로고
// ic_logo_mark.png (270×173px, 흰색) — 글씨색으로 칠해서 쓴다.
// 밑줄이 '이미지 맨 아래 5px'에 정확히 걸리고, 오른쪽 끝 열도 두께가 그대로인 지점에서 잘랐다.
// ─────────────────────────────────────────────

private const val LOGO_ASPECT = 270f / 173f
private const val LOGO_UNDERLINE_RATIO = 5f / 173f

@Composable
private fun LogoMark(
    height: Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(R.drawable.ic_logo_mark),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(color),
        modifier = modifier.size(width = height * LOGO_ASPECT, height = height)
    )
}

/**
 * 로고 + 오른쪽으로 이어지는 밑줄.
 * 폭은 호출하는 쪽이 정한다 — 하단은 fillMaxWidth, 상단은 로고 폭 + 오른쪽 여백.
 * 어느 쪽이든 선이 캔버스 오른쪽 끝에 닿아 "밖으로 나가는" 모양이 된다.
 */
@Composable
private fun LogoWithUnderline(
    height: Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    val logoWidth = height * LOGO_ASPECT
    val lineThickness = height * LOGO_UNDERLINE_RATIO

    Box(modifier = modifier.height(height)) {
        // 선을 먼저 그리고 로고를 위에 얹는다.
        // 로고 오른쪽 끝보다 1dp 안쪽에서 시작 — 축소 시 가장자리가 흐려져 생기는 틈을 가린다.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = logoWidth - 1.dp)
                .fillMaxWidth()
                .height(lineThickness)
                .background(color)
        )
        LogoMark(
            height = height,
            color = color,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

// ─────────────────────────────────────────────
// 스트립: 사진 띠를 세로로 나열
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
                // 순서를 바꿔도 칸이 데이터를 따라 이동 → 이미지를 다시 로드하지 않는다
                key(slot) {
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
}

@Composable
private fun StripRecordedSlot(
    slot: ExtractSlot.Recorded,
    showComment: Boolean,
    onImageSettled: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        SlotImage(slot = slot, onImageSettled = onImageSettled)

        // 글자가 있는 양끝만 어둡게 깔고 가운데는 사진을 살린다.
        // 사진 위 글씨는 이 그라데이션을 전제로 흰색 고정 — 색 옵션 대상이 아니다.
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

            Text(
                text = "기록 없음",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 13.sp
            )
        }
    }
}

// ─────────────────────────────────────────────
// 그리드: 1~2명 → 세로로 쌓기 / 3명 → 위 2칸 + 아래 1칸 / 4명 → 2×2
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
                    key(slot) {
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
}

@Composable
private fun GridRecordedCell(
    slot: ExtractSlot.Recorded,
    showComment: Boolean,
    onImageSettled: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        SlotImage(slot = slot, onImageSettled = onImageSettled)

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
// 카드: 배경 위 흰 액자. 글자는 사진 아래 여백에
//  1~3명 → 세로로 쌓기 / 4명 → 2×2
// ─────────────────────────────────────────────

private val CARD_GAP = 10.dp
private val CARD_PADDING = 16.dp

@Composable
private fun CardLayout(
    slots: List<ExtractSlot>,
    showComment: Boolean,
    hasHeader: Boolean,
    onImageSettled: () -> Unit
) {
    val rows: List<List<ExtractSlot>> =
        if (slots.size <= 3) slots.map { listOf(it) } else slots.chunked(2)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = CARD_PADDING,
                end = CARD_PADDING,
                bottom = CARD_PADDING,
                top = if (hasHeader) 0.dp else CARD_PADDING
            ),
        verticalArrangement = Arrangement.spacedBy(CARD_GAP)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CARD_GAP)
            ) {
                row.forEach { slot ->
                    key(slot) {
                        CardItem(
                            slot = slot,
                            showComment = showComment,
                            onImageSettled = onImageSettled,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardItem(
    slot: ExtractSlot,
    showComment: Boolean,
    onImageSettled: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CardSurface)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(CardPhotoPlaceholder),
            contentAlignment = Alignment.Center
        ) {
            when (slot) {
                is ExtractSlot.Recorded -> {
                    if (slot.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = slot.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onSuccess = { onImageSettled() },
                            onError = { onImageSettled() }
                        )
                    } else {
                        Text(
                            text = slot.workoutType,
                            color = CardCaptionSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
                is ExtractSlot.Empty -> Text(
                    text = "기록 없음",
                    color = CardCaptionSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // 캡션은 흰 액자 안이라 색 옵션 대상이 아니다
        Spacer(Modifier.height(8.dp))
        when (slot) {
            is ExtractSlot.Recorded -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = slot.nickname,
                        color = CardCaptionPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${slot.workoutDuration}분 · ${slot.workoutType}",
                        color = CardCaptionSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (showComment && slot.comment.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = slot.comment,
                        color = CardCaptionSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            is ExtractSlot.Empty -> Text(
                text = slot.nickname,
                color = CardCaptionSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────
// 타이포: 사진 없이 글자로. 색 옵션이 전체(구분선·로고 밑줄 포함)에 적용된다
// ─────────────────────────────────────────────

private val TYPO_PADDING = 28.dp
private val TYPO_LOGO_HEIGHT = 28.dp
private val TYPO_TOP_LOGO_HEIGHT = 22.dp

@Composable
private fun TypoLayout(
    slots: List<ExtractSlot>,
    memberCount: Int,
    recordedCount: Int,
    ratio: ExtractRatio,
    showHeader: Boolean,
    showComment: Boolean,
    showLogo: Boolean,
    colors: CanvasColors,
    groupName: String,
    dateMillis: Long
) {
    // 4:5·1:1은 세로가 짧아 하단 로고가 목록과 겹친다 → 오른쪽 상단(날짜 줄)으로
    val logoOnTop = showLogo && ratio != ExtractRatio.STORY
    val logoOnBottom = showLogo && ratio == ExtractRatio.STORY
    val hasTopRow = showHeader || logoOnTop

    Column(modifier = Modifier.fillMaxSize()) {
        // ── 상단 줄: 날짜(왼쪽) + 로고(오른쪽). 로고가 있으면 오른쪽 여백 없이 선이 끝까지 나간다
        if (hasTopRow) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = TYPO_PADDING,
                        top = TYPO_PADDING,
                        end = if (logoOnTop) 0.dp else TYPO_PADDING
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showHeader) {
                    Text(
                        text = formatDate(dateMillis),
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (logoOnTop) {
                    Spacer(Modifier.width(12.dp))
                    LogoWithUnderline(
                        height = TYPO_TOP_LOGO_HEIGHT,
                        color = colors.textPrimary,
                        modifier = Modifier.width(TYPO_TOP_LOGO_HEIGHT * LOGO_ASPECT + TYPO_PADDING)
                    )
                }
            }
        }

        // ── 본문: 좌우 여백 안쪽
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(
                    start = TYPO_PADDING,
                    end = TYPO_PADDING,
                    top = when {
                        !hasTopRow -> TYPO_PADDING
                        showHeader -> 6.dp      // 날짜 바로 아래 그룹 이름
                        else -> 20.dp           // 로고만 있는 줄 아래
                    }
                )
        ) {
            if (showHeader) {
                if (groupName.isNotBlank()) {
                    Text(
                        text = groupName,
                        color = colors.textPrimary,
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${recordedCount}/${memberCount}명 기록",
                    color = colors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(24.dp))
            }

            if (slots.isEmpty()) {
                Text(
                    text = "기록 없음",
                    color = colors.textPrimary.copy(alpha = 0.7f),
                    fontSize = 15.sp
                )
            } else {
                slots.forEach { slot ->
                    key(slot) {
                        TypoRow(slot = slot, showComment = showComment, color = colors.textPrimary)
                    }
                }
            }
        }

        // ── 9:16 하단 로고: 오른쪽 여백 없이 캔버스 끝까지
        if (logoOnBottom) {
            LogoWithUnderline(
                height = TYPO_LOGO_HEIGHT,
                color = colors.textPrimary,
                modifier = Modifier
                    .padding(start = TYPO_PADDING, bottom = TYPO_PADDING)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TypoRow(
    slot: ExtractSlot,
    showComment: Boolean,
    color: Color
) {
    val isEmpty = slot is ExtractSlot.Empty

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isEmpty) 0.55f else 1f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(color.copy(alpha = 0.35f))
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when (slot) {
                    is ExtractSlot.Recorded -> slot.nickname
                    is ExtractSlot.Empty -> slot.nickname
                },
                color = color,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = when (slot) {
                    is ExtractSlot.Recorded -> "${slot.workoutDuration}분 · ${slot.workoutType}"
                    is ExtractSlot.Empty -> "기록 없음"
                },
                color = color,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 180.dp)
            )
        }
        if (showComment && slot is ExtractSlot.Recorded && slot.comment.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = slot.comment,
                color = color.copy(alpha = 0.8f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(12.dp))
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

// "11월 22일 일"
private fun formatDate(millis: Long): String =
    SimpleDateFormat("M월 d일 E", Locale.KOREA).format(Date(millis))
