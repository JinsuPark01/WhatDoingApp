package com.jinsupark.helpumta.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

// 시스템의 앱 알림 설정 화면으로 이동.
// 앱 내에 알림 on/off를 따로 두지 않고 OS 설정을 단일 진실 소스로 쓴다.
fun Context.openAppNotificationSettings() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    } else {
        // API 26 미만엔 알림 설정 화면이 없음 → 앱 정보 화면으로
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
    }
    runCatching { startActivity(intent) }
}