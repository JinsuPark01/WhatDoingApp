package com.example.whatdoing.domain.model

data class Group(
    val id: String,
    val name: String,
    val description: String = "",
    val imageUrl: String = "",
    val memberCount: Int
)