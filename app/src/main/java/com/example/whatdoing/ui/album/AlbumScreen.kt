package com.example.whatdoing.ui.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.whatdoing.ui.theme.WhatDoingTheme

@Composable
fun AlbumScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val dummyPosts = listOf(
        Pair("야근하는 밤", listOf("#야근", "#사무실", "#밤")),
        Pair("회식 삼겹살", listOf("#회식", "#삼겹살", "#소주")),
        Pair("헬스장", listOf("#헬스", "#운동", "#땀")),
        Pair("카페 공부", listOf("#카페", "#공부", "#아메리카노")),
        Pair("친구들이랑", listOf("#술자리", "#친구", "#맥주")),
        Pair("야외 운동", listOf("#러닝", "#운동", "#공원")),
        Pair("나 카페야", listOf("#카페", "#성수", "#케이크")),
        Pair("요아정 먹기", listOf("#자취방", "#요아정", "#친구")),
    )

    Box(modifier = modifier.fillMaxSize()) {

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(dummyPosts.size) { index ->
                val post = dummyPosts[index]
                PostCard(
                    title = post.first,
                    tags = post.second,
                    onClick = {
                        navController.navigate("detail/${post.first}")
                    }
                )
            }
        }

        FloatingActionButton(
            onClick = { navController.navigate("upload") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("사진 올리기", fontSize = 14.sp)
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
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {}

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

@Preview(showBackground = true)
@Composable
fun AlbumScreenPreview() {
    WhatDoingTheme {
        AlbumScreen(navController = rememberNavController())
    }
}