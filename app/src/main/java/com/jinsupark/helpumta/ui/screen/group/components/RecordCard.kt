package com.jinsupark.helpumta.ui.screen.group.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
 * 항목 구분은 넉넉한 세로 간격이 맡는다.
 */
@Composable
fun RecordCard(
    record: WorkoutRecord,
    canEdit: Boolean,
    canDelete: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    authorPhotoUrl: String = ""
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
    }
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
            RecordCard(
                record = WorkoutRecord(
                    id = "1",
                    groupId = "1",
                    userId = "u1",
                    userName = "이정수",
                    workoutType = "사이클",
                    workoutDuration = 90,
                    imageUrl = "",
                    comment = "퇴근 후",
                    createdAt = System.currentTimeMillis()
                ),
                canEdit = true,
                canDelete = true,
                onEditClick = {},
                onDeleteClick = {}
            )
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
                    createdAt = System.currentTimeMillis() - 3600000
                ),
                canEdit = false,
                canDelete = false,
                onEditClick = {},
                onDeleteClick = {}
            )
        }
    }
}