package com.jinsupark.helpumta.widget

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition

internal val SNAPSHOT_KEY = stringPreferencesKey("widget_snapshot")

/** 외부(WidgetUpdater)에서 스냅샷을 기록 */
internal suspend fun writeSnapshot(context: Context, id: GlanceId, json: String) {
    updateAppWidgetState(context, id) { prefs ->
        prefs[SNAPSHOT_KEY] = json
    }
}

/**
 * 세션 시작(최초 배치·재부팅 등) 시 상태가 비어 있으면 한 번 채운다.
 * 이후 갱신은 WidgetUpdater가 상태를 덮어쓰고 updateAll을 호출한다.
 */
internal suspend fun ensureSnapshot(context: Context, id: GlanceId) {
    val existing = getAppWidgetState(
        context, PreferencesGlanceStateDefinition, id
    )[SNAPSHOT_KEY]

    if (existing != null) return

    writeSnapshot(context, id, WidgetData.load(context).toJson())
}