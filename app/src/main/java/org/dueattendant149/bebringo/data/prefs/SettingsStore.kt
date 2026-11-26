package org.dueattendant149.bebringo.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bebringo_settings")

/** Настройки приложения. Хранятся локально, ничего не уходит наружу. */
class SettingsStore(private val context: Context) {

    private object Keys {
        val onboarded = booleanPreferencesKey("onboarded")
        val goal = stringPreferencesKey("goal")
        val anchorTime = stringPreferencesKey("anchor_time")
        val anchorAfter = stringPreferencesKey("anchor_after")
        val supportLevel = intPreferencesKey("support_level")
        val desiredRetention = intPreferencesKey("desired_retention")
        val llmEndpoint = stringPreferencesKey("llm_endpoint")
        val llmModel = stringPreferencesKey("llm_model")
        val llmKey = stringPreferencesKey("llm_key")
        val webdavUrl = stringPreferencesKey("webdav_url")
        val webdavUser = stringPreferencesKey("webdav_user")
        val webdavPassword = stringPreferencesKey("webdav_password")
        val strictDiacritics = booleanPreferencesKey("strict_diacritics")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val soundEnabled = booleanPreferencesKey("sound_enabled")
        val autoPlayAudio = booleanPreferencesKey("auto_play_audio")
        val selectedTrackFilter = stringPreferencesKey("selected_track_filter")
        val labQuietStreaks = booleanPreferencesKey("lab_quiet_streaks")
        val labDailyReview = booleanPreferencesKey("lab_daily_review")
        val labAutoSupport = booleanPreferencesKey("lab_auto_support")
    }

    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.onboarded] ?: false }
    val goal: Flow<String> = context.dataStore.data.map { it[Keys.goal] ?: "life" }
    val anchorTime: Flow<String> = context.dataStore.data.map { it[Keys.anchorTime] ?: "вечером" }
    val anchorAfter: Flow<String> = context.dataStore.data.map { it[Keys.anchorAfter] ?: "конца работы" }
    val supportLevel: Flow<Int> = context.dataStore.data.map { it[Keys.supportLevel] ?: 1 }
    val strictDiacritics: Flow<Boolean> = context.dataStore.data.map { it[Keys.strictDiacritics] ?: false }
    val desiredRetention: Flow<Int> = context.dataStore.data.map { it[Keys.desiredRetention] ?: 90 }
    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[Keys.dynamicColor] ?: false }
    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.soundEnabled] ?: true }
    val autoPlayAudio: Flow<Boolean> = context.dataStore.data.map { it[Keys.autoPlayAudio] ?: false }
    val selectedTrackFilter: Flow<String> = context.dataStore.data.map { it[Keys.selectedTrackFilter] ?: "all" }

    val labQuietStreaks: Flow<Boolean> = context.dataStore.data.map { it[Keys.labQuietStreaks] ?: true }
    val labDailyReview: Flow<Boolean> = context.dataStore.data.map { it[Keys.labDailyReview] ?: false }
    val labAutoSupport: Flow<Boolean> = context.dataStore.data.map { it[Keys.labAutoSupport] ?: true }

    val llmEndpoint: Flow<String> = context.dataStore.data.map { it[Keys.llmEndpoint] ?: "" }
    val llmModel: Flow<String> = context.dataStore.data.map { it[Keys.llmModel] ?: "gpt-4o-mini" }
    val llmKey: Flow<String> = context.dataStore.data.map { it[Keys.llmKey] ?: "" }

    val webdavUrl: Flow<String> = context.dataStore.data.map { it[Keys.webdavUrl] ?: "" }
    val webdavUser: Flow<String> = context.dataStore.data.map { it[Keys.webdavUser] ?: "" }
    val webdavPassword: Flow<String> = context.dataStore.data.map { it[Keys.webdavPassword] ?: "" }

    suspend fun completeOnboarding(goal: String, anchorTime: String, anchorAfter: String) {
        context.dataStore.edit {
            it[Keys.onboarded] = true
            it[Keys.goal] = goal
            it[Keys.anchorTime] = anchorTime
            it[Keys.anchorAfter] = anchorAfter
        }
    }

    suspend fun setSupportLevel(level: Int) {
        context.dataStore.edit { it[Keys.supportLevel] = level.coerceIn(0, 3) }
    }

    suspend fun setStrictDiacritics(value: Boolean) {
        context.dataStore.edit { it[Keys.strictDiacritics] = value }
    }

    suspend fun setDesiredRetention(percent: Int) {
        context.dataStore.edit { it[Keys.desiredRetention] = percent.coerceIn(80, 95) }
    }

    suspend fun setDynamicColor(value: Boolean) {
        context.dataStore.edit { it[Keys.dynamicColor] = value }
    }

    suspend fun setSoundEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.soundEnabled] = value }
    }

    suspend fun setAutoPlayAudio(value: Boolean) {
        context.dataStore.edit { it[Keys.autoPlayAudio] = value }
    }

    suspend fun setSelectedTrackFilter(trackId: String) {
        context.dataStore.edit { it[Keys.selectedTrackFilter] = trackId }
    }

    suspend fun setLabQuietStreaks(value: Boolean) {
        context.dataStore.edit { it[Keys.labQuietStreaks] = value }
    }

    suspend fun setLabDailyReview(value: Boolean) {
        context.dataStore.edit { it[Keys.labDailyReview] = value }
    }

    suspend fun setLabAutoSupport(value: Boolean) {
        context.dataStore.edit { it[Keys.labAutoSupport] = value }
    }

    suspend fun setLlm(endpoint: String, model: String, key: String) {
        context.dataStore.edit {
            it[Keys.llmEndpoint] = endpoint
            it[Keys.llmModel] = model
            it[Keys.llmKey] = key
        }
    }

    suspend fun setWebdav(url: String, user: String, password: String) {
        context.dataStore.edit {
            it[Keys.webdavUrl] = url
            it[Keys.webdavUser] = user
            it[Keys.webdavPassword] = password
        }
    }
}
