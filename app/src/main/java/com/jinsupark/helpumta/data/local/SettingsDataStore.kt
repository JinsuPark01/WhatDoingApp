package com.jinsupark.helpumta.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jinsupark.helpumta.domain.model.HomeLayout
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings"
)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.settingsDataStore

    val homeLayout: Flow<HomeLayout> = dataStore.data
        // 읽기 실패 시 앱이 죽지 않도록 기본값으로 복구
        .catch { e ->
            if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw e
        }
        .map { prefs ->
            val saved = prefs[KEY_HOME_LAYOUT]
            // 저장값이 깨졌거나 없으면 기본값
            runCatching { HomeLayout.valueOf(saved ?: "") }.getOrDefault(HomeLayout.GRID)
        }

    suspend fun setHomeLayout(layout: HomeLayout) {
        dataStore.edit { prefs ->
            prefs[KEY_HOME_LAYOUT] = layout.name
        }
    }

    private companion object {
        val KEY_HOME_LAYOUT = stringPreferencesKey("home_layout")
    }
}