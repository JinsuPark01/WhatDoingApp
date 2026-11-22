package com.jinsupark.helpumta.ui.screen.group.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jinsupark.helpumta.domain.model.Reaction
import com.jinsupark.helpumta.domain.model.WorkoutRecord
import com.jinsupark.helpumta.ui.theme.HelpumtaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 기록 한 건.
 *
 * 카드 테두리·배경·그림자를 모두 뺐다. 인증샷이 이 화면의 주인공이고,
 * 사진 자체가 덩어리 역할을 하므로 상자를 한 겹 더 두를 필요가 없다.
 * 항목 구분은 목록 쪽(GroupDetailScreen)의 얇은 구분선이 맡는다.
 */
@Composable
fun RecordCard(
    record: WorkoutRecord,
    canEdit: Boolean,
    canDelete: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    authorPhotoUrl: String = "",
    currentUserId: String? = null,
    onReact: (Reaction?) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {

        // ── 작성자 줄
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(name = record.userName, photoUrl = authorPhotoUrl)

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.userName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                // 칩 대신 한 줄로 흘린다
                Text(
                    text = "${record.workoutType} · ${record.workoutDuration}분 · ${formatTime(record.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (canEdit || canDelete) {
                RecordMenu(
                    canEdit = canEdit,
                    canDelete = canDelete,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick
                )
            }
        }

        // ── 사진
        if (record.imageUrl.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            AsyncImage(
                model = record.imageUrl,
                contentDescription = "${record.userName}의 인증샷",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)   // 피드 리듬을 맞추기 위해 비율 고정
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            )
        }

        // ── 한줄 소감
        if (record.comment.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = record.comment,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // ── 리액션
        ReactionRow(
            reactions = record.reactions,
            isMine = record.userId == currentUserId,
            myReaction = currentUserId?.let { record.reactions[it] },
            onReact = onReact
        )
    }
}

/**
 * 리액션 줄.
 * 종류가 3개뿐이라 "추가" 버튼 → 팝업을 거치지 않고 전부 노출한다.
 * 내 기록: 받은 것만 읽기 전용으로 (없으면 줄 자체를 숨김). 누를 수 없는 0개짜리가 자리만 차지하지 않게.
 * 남의 기록: 3개 모두 0부터 — 같은 걸 다시 누르면 취소, 다른 걸 누르면 교체.
 */
@Composable
private fun ReactionRow(
    reactions: Map<String, Reaction>,
    isMine: Boolean,
    myReaction: Reaction?,
    onReact: (Reaction?) -> Unit
) {
    val counts = reactions.values.groupingBy { it }.eachCount()
    val visible = if (isMine) {
        Reaction.entries.filter { (counts[it] ?: 0) > 0 }
    } else {
        Reaction.entries
    }
    if (visible.isEmpty()) return

    // 터치 영역 패딩만큼 왼쪽으로 당겨 이모지가 본문 왼쪽 선에 맞게
    Spacer(Modifier.height(6.dp))
    Row(modifier = Modifier.offset(x = (-8).dp)) {
        visible.forEach { reaction ->
            val isSelected = reaction == myReaction
            ReactionItem(
                emoji = reaction.emoji(),
                count = counts[reaction] ?: 0,
                isSelected = isSelected,
                onClick = if (isMine) null else {
                    { onReact(if (isSelected) null else reaction) }
                }
            )
        }
    }
}

/**
 * 배경 없이 이모지 + 숫자(0 포함).
 * 내가 누른 것만 숫자를 onSurface·SemiBold(titleSmall)로 진하게 — 상태 표시는 이것 하나.
 * 배경은 없어도 터치 영역은 패딩으로 유지한다.
 */
@Composable
private fun ReactionItem(
    emoji: String,
    count: Int,
    isSelected: Boolean,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "$count",
            style = if (isSelected) MaterialTheme.typography.titleSmall
            else MaterialTheme.typography.bodySmall,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 표현은 UI 몫 — 도메인 Reaction은 key만 안다
private fun Reaction.emoji(): String = when (this) {
    Reaction.MUSCLE -> "💪"
    Reaction.FIRE -> "🔥"
    Reaction.CLAP -> "👏"
}

@Composable
private fun Avatar(name: String, photoUrl: String) {
    // 브랜드 색을 장식에 쓰지 않는다. 주황은 FAB 하나로 충분하다.
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl.isNotBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "${name}의 프로필 사진",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = name.firstOrNull()?.toString() ?: "?",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecordMenu(
    canEdit: Boolean,
    canDelete: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = "기록 메뉴",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (canEdit) {
                DropdownMenuItem(
                    text = { Text("수정") },
                    onClick = {
                        expanded = false
                        onEditClick()
                    }
                )
            }
            if (canDelete) {
                DropdownMenuItem(
                    text = {
                        Text("삭제", color = MaterialTheme.colorScheme.error)
                    },
                    onClick = {
                        expanded = false
                        onDeleteClick()
                    }
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("a h:mm", Locale.KOREA).format(Date(millis))

@Preview(showBackground = true)
@Composable
private fun RecordCardPreview() {
    HelpumtaTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // 내 기록 — 받은 리액션만 읽기 전용
            RecordCard(
                record = WorkoutRecord(
                    id = "1",
                    groupId = "1",
                    userId = "me",
                    userName = "이정수",
                    workoutType = "사이클",
                    workoutDuration = 90,
                    imageUrl = "",
                    comment = "퇴근 후",
                    createdAt = System.currentTimeMillis(),
                    reactions = mapOf("u2" to Reaction.FIRE, "u3" to Reaction.FIRE)
                ),
                canEdit = true,
                canDelete = true,
                onEditClick = {},
                onDeleteClick = {},
                currentUserId = "me"
            )
            // 남의 기록 — 내가 💪를 누른 상태
            RecordCard(
                record = WorkoutRecord(
                    id = "2",
                    groupId = "1",
                    userId = "u2",
                    userName = "최이랑",
                    workoutType = "러닝",
                    workoutDuration = 45,
                    imageUrl = "",
                    comment = "한강런",
                    createdAt = System.currentTimeMillis() - 3600000,
                    reactions = mapOf("me" to Reaction.MUSCLE)
                ),
                canEdit = false,
                canDelete = false,
                onEditClick = {},
                onDeleteClick = {},
                currentUserId = "me"
            )
        }
    }
}