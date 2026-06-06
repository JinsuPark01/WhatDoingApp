package com.example.whatdoing.domain.repository

interface UserRepository {
    // 있으면 갱신, 없으면 생성 (createdAt은 최초 생성 시만)
    suspend fun saveUser(uid: String, nickname: String, email: String): Result<Unit>

    // uid 목록 → uid:nickname 맵 (zzz 칸 이름용)
    suspend fun getNicknames(uids: List<String>): Result<Map<String, String>>
}