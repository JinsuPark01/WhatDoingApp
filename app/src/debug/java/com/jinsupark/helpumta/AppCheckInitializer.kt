package com.jinsupark.helpumta

import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

// 디버그 빌드 — Play Integrity 검증이 불가하므로 디버그 토큰 사용
fun installAppCheck() {
    Firebase.appCheck.installAppCheckProviderFactory(
        DebugAppCheckProviderFactory.getInstance()
    )
}