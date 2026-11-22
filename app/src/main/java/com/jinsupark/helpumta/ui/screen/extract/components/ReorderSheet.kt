@file:OptIn(ExperimentalMaterial3Api::class)

package com.jinsupark.helpumta.ui.screen.extract.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jinsupark.helpumta.domain.model.ExtractSlot
import kotlinx.coroutines.launch

private val ITEM_HEIGHT = 56.dp

/**
 * 추출 이미지에 들어갈 멤버 순서를 바꾸는 바텀 시트.
 * 시트 안에서는 복사본으로 옮기고, 완료/닫기 때 한 번만 반영한다
 * — 끌 때마다 반영하면 뒤의 미리보기가 계속 다시 그려진다.
 */
@Composable
fun ReorderSheet(
    slots: List<ExtractSlot>,
    onApply: (List<ExtractSlot>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(slots) }

    // 완료 버튼이든 아래로 내리기든 결과는 같게 — 옮긴 순서를 버리지 않는다
    val close: () -> Unit = {
        onApply(working)
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = {
            onApply(working)
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            Text(
                text = "순서 바꾸기",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "길게 눌러서 끌어 옮기세요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            ReorderList(
                items = working,
                onMove = { from, to ->
                    working = working.toMutableList().apply { add(to, removeAt(from)) }
                }
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = close,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("완료", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * 항목이 최대 4개라 라이브러리 없이 직접 구현한다.
 * 끄는 동안 옆 칸 높이의 절반을 넘으면 자리를 바꾸고, 넘은 만큼 오프셋을 되돌린다.
 * key(slot)로 묶어 자리가 바뀌어도 같은 노드가 이동 → 진행 중인 드래그 제스처가 끊기지 않는다.
 */
@Composable
private fun ReorderList(
    items: List<ExtractSlot>,
    onMove: (from: Int, to: Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val itemHeightPx = with(LocalDensity.current) { ITEM_HEIGHT.toPx() }

    var draggingSlot by remember { mutableStateOf<ExtractSlot?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    // 제스처 콜백 안에서 항상 최신 목록을 보도록
    val currentItems by rememberUpdatedState(items)

    Column {
        items.forEach { slot ->
            key(slot) {
                val isDragging = slot == draggingSlot
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ITEM_HEIGHT)
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh
                            else Color.Transparent
                        )
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    draggingSlot = slot
                                    dragOffset = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount.y

                                    val from = currentItems.indexOf(slot)
                                    if (from < 0) return@detectDragGesturesAfterLongPress
                                    when {
                                        dragOffset > itemHeightPx / 2 && from < currentItems.lastIndex -> {
                                            onMove(from, from + 1)
                                            dragOffset -= itemHeightPx
                                        }
                                        dragOffset < -itemHeightPx / 2 && from > 0 -> {
                                            onMove(from, from - 1)
                                            dragOffset += itemHeightPx
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggingSlot = null
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    draggingSlot = null
                                    dragOffset = 0f
                                }
                            )
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DragHandle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = slot.nicknameOf(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (slot is ExtractSlot.Empty) {
                        Text(
                            text = "기록 없음",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun ExtractSlot.nicknameOf(): String = when (this) {
    is ExtractSlot.Recorded -> nickname
    is ExtractSlot.Empty -> nickname
}