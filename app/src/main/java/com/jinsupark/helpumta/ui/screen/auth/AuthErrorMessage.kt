package com.jinsupark.helpumta.ui.screen.auth

import com.jinsupark.helpumta.domain.model.AuthError

fun AuthError.toMessage(): String = when (this) {
    AuthError.INVALID_CREDENTIAL   -> "이메일 또는 비밀번호가 올바르지 않습니다"
    AuthError.EMAIL_ALREADY_IN_USE -> "이미 가입된 이메일입니다"
    AuthError.WEAK_PASSWORD        -> "비밀번호는 6자 이상이어야 합니다"
    AuthError.NETWORK              -> "네트워크 연결을 확인해주세요"
    AuthError.UNKNOWN              -> "잠시 후 다시 시도해주세요"
}