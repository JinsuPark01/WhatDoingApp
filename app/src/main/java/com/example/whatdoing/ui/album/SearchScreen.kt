package com.example.whatdoing.ui.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var searchTags by remember { mutableStateOf(listOf<String>()) }
    val recentSearches = remember { mutableStateListOf("#야근", "#회식", "#헬스") } // 임시 최근검색어
    val focusRequester = remember { FocusRequester() }

    // 태그 추가 함수
    fun addTag() {
        val tag = inputText.trim().let {
            if (it.startsWith("#")) it else "#$it"
        }
        if (tag.length > 1 && !searchTags.contains(tag)) {
            searchTags = searchTags + tag
        }
        inputText = ""
    }

    // 임시 더미 검색결과
    val dummyResults = listOf(
        Pair("야근하는 밤", listOf("#야근", "#사무실", "#밤")),
        Pair("회식 삼겹살", listOf("#회식", "#삼겹살", "#소주")),
        Pair("헬스장", listOf("#헬스", "#운동", "#땀")),
        Pair("카페 공부", listOf("#카페", "#공부", "#아메리카노")),
    ).filter { post ->
        searchTags.isEmpty() || searchTags.all { tag -> post.second.contains(tag) }
    }

    Scaffold(
        topBar = {
            Column {
                // 검색창
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("태그로 검색 (예: 야근)") },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { addTag() }),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 선택된 태그 칩들
                if (searchTags.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(searchTags.size) { index ->
                            InputChip(
                                selected = true,
                                onClick = {
                                    searchTags = searchTags - searchTags[index]
                                },
                                label = { Text(searchTags[index]) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "태그 제거",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (searchTags.isEmpty()) {
                // 태그 없을 때 최근 검색어 표시
                Text(
                    text = "최근 검색",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentSearches.size) { index ->
                        SuggestionChip(
                            onClick = {
                                inputText = recentSearches[index].removePrefix("#")
                                addTag()
                            },
                            label = { Text(recentSearches[index]) }
                        )
                    }
                }
            } else {
                // 검색 결과
                Text(
                    text = "검색 결과 ${dummyResults.size}개",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(dummyResults.size) { index ->
                        val post = dummyResults[index]
                        PostCard(title = post.first, tags = post.second)
                    }
                }
            }
        }
    }
}