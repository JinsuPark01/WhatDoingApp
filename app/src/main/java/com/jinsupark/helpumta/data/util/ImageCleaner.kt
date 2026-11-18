package com.jinsupark.helpumta.data.util

import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 더 이상 참조되지 않는 Storage 이미지를 백그라운드에서 정리한다.
 *
 * Storage 삭제는 '되면 좋은 뒷정리'지 지켜야 할 불변식이 아니다.
 * (불변식은 Firestore 문서/필드가 바뀌는 것이고, 그건 이미 끝난 뒤에 호출된다.)
 *
 * 호출자의 코루틴에서 기다리면
 *  - 토스트·위젯·통계 갱신이 전부 그만큼 늦어지고
 *  - 그 사이 취소가 일어나면 runCatching이 취소 예외까지 삼켜서
 *    뒤따르는 suspend 작업이 조용히 사라진다. (위젯 갱신이 안 되던 원인)
 */
@Singleton
class ImageCleaner @Inject constructor(
    private val storage: FirebaseStorage
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun delete(url: String) {
        if (url.isBlank()) return
        scope.launch {
            runCatching { storage.getReferenceFromUrl(url).delete().await() }
        }
    }
}