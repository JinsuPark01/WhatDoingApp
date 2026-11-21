package com.jinsupark.helpumta.domain.model

data class MemberProfile(
    val uid: String,
    val nickname: String,
    val photoUrl: String = ""
)