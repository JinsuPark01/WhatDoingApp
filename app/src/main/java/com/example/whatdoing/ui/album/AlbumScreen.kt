package com.example.whatdoing.ui.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(modifier: Modifier = Modifier) {

    // 임시 더미 데이터
    val dummyPosts = listOf(
        Pair("야근하는 밤", listOf("#야근", "#사무실", "#밤")),
        Pair("회식 삼겹살", listOf("#회식", "#삼겹살", "#소주")),
        Pair("헬스장", listOf("#헬스", "#운동", "#땀")),
        Pair("카페 공부", listOf("#카페", "#공부", "#아메리카노")),
        Pair("친구들이랑", listOf("#술자리", "#친구", "#맥주")),
        Pair("야외 운동", listOf("#러닝", "#운동", "#공원")),
    )

    var showSearch by remember { mutableStateOf(false) }

    if (showSearch) {
        SearchScreen(onBack = { showSearch = false })
        return
    }

    var showUpload by remember { mutableStateOf(false) }

    if (showUpload) {
        UploadScreen(onBack = { showUpload = false })
        return
    }

    var selectedPost by remember { mutableStateOf<Pair<String, List<String>>?>(null) }

    if (selectedPost != null) {
        PostDetailScreen(
            title = selectedPost!!.first,
            tags = selectedPost!!.second,
            imageUrl = null,
            onBack = { selectedPost = null },
            modifier = modifier
        )
        return
    }

    Scaffold(
        topBar = {
            Column {
                // 1행: 앱이름 + 검색버튼
                TopAppBar(
                    title = {
                        Text(
                            text = "지금뭐해?",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "검색"
                            )
                        }
                    }
                )
                // 2행: 업로드 버튼 (임시 위치, 나중에 탭바 위로 이동)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 사진 그리드
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(dummyPosts.size) { index ->
                    val post = dummyPosts[index]
                    PostCard(title = post.first, tags = post.second, onClick = { selectedPost = post })
                }
            }

            // 업로드 버튼 (탭바 위)
            Button(
                onClick = { showUpload = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "+ 사진 올리기", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun PostCard(title: String, tags: List<String>, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 나중에 실제 이미지로 교체
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {}

            // 태그 표시
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tags.joinToString(" "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}