package com.jinsupark.helpumta.data.fcm

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jinsupark.helpumta.MainActivity
import com.jinsupark.helpumta.R
import com.jinsupark.helpumta.domain.usecase.RegisterFcmTokenUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HelpumtaMessagingService : FirebaseMessagingService() {

    @Inject lateinit var registerFcmTokenUseCase: RegisterFcmTokenUseCase

    // Service에는 lifecycleScope가 없음.
    // 짧은 Firestore 쓰기 한 번이라 onDestroy에서 cancel하지 않는다
    // (cancel하면 서비스가 먼저 죽어서 쓰기가 날아감).
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 토큰 갱신 시 호출 (앱 재설치, 데이터 삭제 등)
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // 비로그인 상태면 UseCase가 no-op → 앱 진입 시 재등록으로 커버
        scope.launch { registerFcmTokenUseCase() }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.data["title"] ?: message.notification?.title ?: "헬품타"
        val body = message.data["body"] ?: message.notification?.body ?: return
        val groupId = message.data["groupId"]

        showNotification(title, body, groupId)
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(title: String, body: String, groupId: String?) {
        createChannel()

        // 미작성 그룹이 하나면 거기로, 여러 개면 홈으로 (서버가 groupId를 빼고 보낸다)
        val intent = if (groupId != null) {
            Intent(
                Intent.ACTION_VIEW,
                "https://whatdoing-c4457.web.app/group/$groupId".toUri()
            ).apply {
                setPackage(packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        } else {
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            // 지정하지 않으면 시스템이 임의의 색(보통 파랑)으로 아이콘을 칠한다
            .setColor(ContextCompat.getColor(this, R.color.brand_orange))
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()

        // 하루 한 번 오는 리마인더라 ID를 고정한다.
        // 그룹 해시를 쓰면 미작성 그룹이 날마다 바뀔 때 알림이 지워지지 않고 쌓인다.
        runCatching {
            NotificationManagerCompat.from(this).notify(REMINDER_NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "운동 기록 리마인더",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "오늘 기록이 비어 있을 때 알려줍니다"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "workout_reminder"
        private const val REMINDER_NOTIFICATION_ID = 1001
        private const val REMINDER_REQUEST_CODE = 1001
    }
}