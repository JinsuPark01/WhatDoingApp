package com.jinsupark.helpumta.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 기록·그룹이 바뀌었을 때 위젯을 갱신한다.
 *
 * 자체 스코프에서 돌린다. 호출자의 viewModelScope에 묶으면
 * 갱신이 끝날 때까지 토스트·네비게이션이 대기하고,
 * 그 사이 화면이 바뀌면 갱신이 중간에 죽는다. (ImageCleaner와 같은 이유)
 *
 * 데이터를 먼저 조회해 위젯 상태에 기록한 뒤 updateAll을 호출한다.
 * 상태를 안 거치면 재구성이 세션 시작 때 캡처된 옛 값을 그대로 다시 그린다.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun refresh() {
        scope.launch {
            runCatching {
                // 세 위젯이 같은 데이터를 쓰므로 조회는 한 번만
                val json = WidgetData.load(context).toJson()

                push(GroupStatusWidget(), json)
                push(TodaySummaryWidget(), json)
                push(WeeklyWidget(), json)
            }
        }
    }

    private suspend fun push(widget: GlanceAppWidget, json: String) {
        runCatching {
            val ids = GlanceAppWidgetManager(context).getGlanceIds(widget.javaClass)
            if (ids.isEmpty()) return   // 홈 화면에 없는 위젯은 건너뜀
            ids.forEach { id -> writeSnapshot(context, id, json) }
            widget.updateAll(context)
        }
    }
}