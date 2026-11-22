package com.jinsupark.helpumta.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// 설정(settings)과 분리된 파일. 사용자가 고른 값이 아니라 입력하며 쌓이는 이력이다.
private val Context.workoutHistoryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "workout_history"
)

@Singleton
class WorkoutHistoryDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.workoutHistoryDataStore

    // 최근 운동 종류 — 최근 순, 최대 MAX_RECENT_TYPES개
    val recentWorkoutTypes: Flow<List<String>> = dataStore.data
        // 읽기 실패 시 앱이 죽지 않도록 빈 목록으로 복구
        .catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }
        .map { prefs -> decode(prefs[KEY_RECENT_WORKOUT_TYPES]) }

    suspend fun addRecentWorkoutType(type: String) {
        val trimmed = type.trim()
        if (trimmed.isEmpty()) return

        dataStore.edit { prefs ->
            val current = decode(prefs[KEY_RECENT_WORKOUT_TYPES])
            // 이미 있으면 맨 앞으로 끌어올리고, 넘치면 오래된 것부터 버린다
            val updated = (listOf(trimmed) + current.filter { it != trimmed })
                .take(MAX_RECENT_TYPES)
            prefs[KEY_RECENT_WORKOUT_TYPES] = updated.joinToString(SEPARATOR)
        }
    }

    // StringSet은 순서를 보장하지 않아 "최근 순"을 담을 수 없다 → 구분자로 이은 문자열 하나로 저장
    private fun decode(raw: String?): List<String> =
        raw?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()

    private companion object {
        val KEY_RECENT_WORKOUT_TYPES = stringPreferencesKey("recent_workout_types")

        const val MAX_RECENT_TYPES = 5
        // 사용자가 입력할 일이 없는 제어 문자(Unit Separator)를 구분자로
        const val SEPARATOR = "\u001F"
    }
}