package com.bbobbo.pet.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bbobbo.pet.domain.NotifyKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "bbobbo_settings")

/** 기획서 S-12 설정. 알림 종류별 ON/OFF 는 [NotifyKind.key] 를 그대로 키로 쓴다. */
data class AppSettings(
    val sound: Boolean = true,
    val bgmVolume: Float = 0.6f,
    val vibration: Boolean = true,
    val notify: Map<String, Boolean> = NotifyKind.entries.associate { it.key to true },
) {
    fun notifyEnabled(kind: NotifyKind): Boolean = notify[kind.key] ?: true
}

class SettingsStore(private val context: Context) {

    val flow: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun setSound(on: Boolean) = edit { it[KEY_SOUND] = on }
    suspend fun setVibration(on: Boolean) = edit { it[KEY_VIBRATION] = on }
    suspend fun setBgmVolume(v: Float) = edit { it[KEY_BGM] = v.coerceIn(0f, 1f) }
    suspend fun setNotify(kind: NotifyKind, on: Boolean) = edit { it[notifyKey(kind)] = on }

    /** 설정 초기화 (데이터 초기화 2단계 확인의 마지막 단계에서 호출) */
    suspend fun clear() = context.dataStore.edit { it.clear() }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    companion object {
        private val KEY_SOUND = booleanPreferencesKey("sound")
        private val KEY_VIBRATION = booleanPreferencesKey("vibration")
        private val KEY_BGM = floatPreferencesKey("bgm_volume")

        private fun notifyKey(kind: NotifyKind) = booleanPreferencesKey(kind.key)

        private fun Preferences.toSettings() = AppSettings(
            sound = this[KEY_SOUND] ?: true,
            bgmVolume = this[KEY_BGM] ?: 0.6f,
            vibration = this[KEY_VIBRATION] ?: true,
            notify = NotifyKind.entries.associate { it.key to (this[notifyKey(it)] ?: true) },
        )
    }
}
