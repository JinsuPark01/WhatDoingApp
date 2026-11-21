package com.jinsupark.helpumta.domain.repository

import com.jinsupark.helpumta.domain.model.MemberProfile

interface UserRepository {
    // 있으면 갱신, 없으면 생성 (createdAt은 최초 생성 시만)
    suspend fun saveUser(uid: String, nickname: String, email: String): Result<Unit>

    // uid 목록 → uid:nickname 맵 (zzz 칸 이름용)
    suspend fun getNicknames(uids: List<String>): Result<Map<String, String>>

    // uid 목록 → uid:프로필(닉네임+사진) 맵 — 기록 카드 아바타용
    suspend fun getProfiles(uids: List<String>): Result<Map<String, MemberProfile>>

    suspend fun updateNickname(uid: String, nickname: String): Result<Unit>

    /**
     * imageUri가 null이면 사진 제거, 아니면 업로드 후 교체.
     * 성공 시 새 photoUrl을 반환 (제거면 "")
     */
    suspend fun updateProfileImage(uid: String, imageUri: String?): Result<String>

    suspend fun deleteUser(uid: String): Result<Unit>

    // 현재 로그인 사용자의 FCM 토큰을 서버에 등록 (비로그인 상태면 no-op)
    suspend fun registerFcmToken(): Result<Unit>

    // 로그아웃 시 호출 — 반드시 signOut 전에 (uid가 필요하므로)
    suspend fun clearFcmToken(): Result<Unit>
}