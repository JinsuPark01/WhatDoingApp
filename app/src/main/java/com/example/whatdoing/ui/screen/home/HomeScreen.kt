@file:OptIn(ExperimentalMaterial3Api::class) // 추후 범위 조정

package com.example.whatdoing.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.whatdoing.ui.theme.WhatDoingTheme

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("헬품타") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.handleIntent(HomeContract.Intent.NavigateToCreateGroup)
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "그룹 만들기")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                // TODO: 그룹 리스트
                Text("그룹 목록")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    WhatDoingTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("헬품타") }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {}) {
                    Icon(Icons.Default.Add, contentDescription = "그룹 만들기")
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("그룹 목록")
            }
        }
    }
}