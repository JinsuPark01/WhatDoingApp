package com.example.whatdoing.ui.common

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun MainBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        Triple("album", "공유앨범", "📷"),
        Triple("ai", "AI수정", "✨"),
        Triple("mypage", "마이", "👤")
    )

    NavigationBar {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = { onNavigate(route) },
                icon = { Text(icon) },
                label = { Text(label) }
            )
        }
    }
}