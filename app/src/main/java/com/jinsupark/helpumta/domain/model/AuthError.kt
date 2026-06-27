package com.jinsupark.helpumta.domain.model

enum class AuthError {
    INVALID_CREDENTIAL,    // 이메일/비번 틀림 (보안상 합침)
    EMAIL_ALREADY_IN_USE,  // 이미 가입된 이메일
    WEAK_PASSWORD,         // 비번 6자 미만
    NETWORK,               // 네트워크 오류
    UNKNOWN
}

class AuthException(val error: AuthError) : Exception()