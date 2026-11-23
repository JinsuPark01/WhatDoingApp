package com.jinsupark.helpumta.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jinsupark.helpumta.domain.model.ExtractColor
import com.jinsupark.helpumta.domain.model.ExtractPalette
import com.jinsupark.helpumta.domain.model.ExtractRatio
import com.jinsupark.helpumta.domain.model.ExtractStyle
import com.jinsupark.helpumta.domain.model.ExtractTemplate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.extractStyleDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "extract_style"
)

@Singleton
class ExtractStyleDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.extractStyleDataStore

    val style: Flow<ExtractStyle> = dataStore.data
        // 읽기 실패 시 앱이 죽지 않도록 기본값으로 복구
        .catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }
        .map { prefs ->
            val default = ExtractStyle()
            ExtractStyle(
                // 저장값이 깨졌거나(템플릿이 삭제된 경우 등) 없으면 기본값
                template = prefs[KEY_TEMPLATE]
                    ?.let { runCatching { ExtractTemplate.valueOf(it) }.getOrNull() }
                    ?: default.template,
                ratio = prefs[KEY_RATIO]
                    ?.let { runCatching { ExtractRatio.valueOf(it) }.getOrNull() }
                    ?: default.ratio,
                showHeader = prefs[KEY_SHOW_HEADER] ?: default.showHeader,
                showComment = prefs[KEY_SHOW_COMMENT] ?: default.showComment,
                includeEmpty = prefs[KEY_INCLUDE_EMPTY] ?: default.includeEmpty,
                showLogo = prefs[KEY_SHOW_LOGO] ?: default.showLogo,
                // 고른 적 있는 템플릿만 들어간다. 없으면 도메인이 대표 색으로 채운다
                palettes = ExtractTemplate.entries.mapNotNull { template ->
                    prefs[paletteKey(template)]
                        ?.let(::decodePalette)
                        ?.let { template to it }
                }.toMap()
            )
        }

    suspend fun save(style: ExtractStyle) {
        dataStore.edit { prefs ->
            prefs[KEY_TEMPLATE] = style.template.name
            prefs[KEY_RATIO] = style.ratio.name
            prefs[KEY_SHOW_HEADER] = style.showHeader
            prefs[KEY_SHOW_COMMENT] = style.showComment
            prefs[KEY_INCLUDE_EMPTY] = style.includeEmpty
            prefs[KEY_SHOW_LOGO] = style.showLogo
            ExtractTemplate.entries.forEach { template ->
                val key = paletteKey(template)
                val palette = style.palettes[template]
                if (palette != null) prefs[key] = encodePalette(palette)
                else prefs.remove(key)
            }
        }
    }

    // "BLACK:WHITE" 형태 — 배경:글씨
    private fun encodePalette(palette: ExtractPalette): String =
        "${palette.background.name}$PALETTE_SEPARATOR${palette.text.name}"

    private fun decodePalette(raw: String): ExtractPalette? {
        val parts = raw.split(PALETTE_SEPARATOR)
        if (parts.size != 2) return null
        val background = runCatching { ExtractColor.valueOf(parts[0]) }.getOrNull() ?: return null
        val text = runCatching { ExtractColor.valueOf(parts[1]) }.getOrNull() ?: return null
        return ExtractPalette(background, text).takeIf { it.isValid }
    }

    private companion object {
        val KEY_TEMPLATE = stringPreferencesKey("template")
        val KEY_RATIO = stringPreferencesKey("ratio")
        val KEY_SHOW_HEADER = booleanPreferencesKey("show_header")
        val KEY_SHOW_COMMENT = booleanPreferencesKey("show_comment")
        val KEY_INCLUDE_EMPTY = booleanPreferencesKey("include_empty")
        val KEY_SHOW_LOGO = booleanPreferencesKey("show_logo")

        const val PALETTE_SEPARATOR = ":"

        // 템플릿마다 키 하나 — palette_STRIP, palette_GRID ...
        fun paletteKey(template: ExtractTemplate) =
            stringPreferencesKey("palette_${template.name}")
    }
}