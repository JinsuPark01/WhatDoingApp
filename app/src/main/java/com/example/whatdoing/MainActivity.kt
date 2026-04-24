package com.example.whatdoing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.whatdoing.ui.ai.AiScreen
import com.example.whatdoing.ui.album.AlbumScreen
import com.example.whatdoing.ui.mypage.MyPageScreen
import com.example.whatdoing.ui.theme.WhatDoingTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WhatDoingTheme {
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }

                val tabs = listOf("공유앨범", "AI수정", "마이")

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            tabs.forEachIndexed { index, title ->
                                NavigationBarItem(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    icon = {
                                        Text(text = when(index) {
                                            0 -> "📷"
                                            1 -> "✨"
                                            else -> "👤"
                                        })
                                    },
                                    label = { Text(text = title) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        0 -> AlbumScreen(modifier = Modifier.padding(innerPadding))
                        1 -> AiScreen(modifier = Modifier.padding(innerPadding))
                        else -> MyPageScreen(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}