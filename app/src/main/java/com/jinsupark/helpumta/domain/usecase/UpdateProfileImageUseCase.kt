package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.repository.AuthRepository
import com.jinsupark.helpumta.domain.repository.UserRepository
import javax.inject.Inject

class UpdateProfileImageUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    // imageUri가 null이면 제거. 성공 시 새 photoUrl (제거면 "")
    suspend operator fun invoke(imageUri: String?): Result<String> {
        val uid = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("로그인이 필요합니다"))
        return userRepository.updateProfileImage(uid, imageUri)
    }
}