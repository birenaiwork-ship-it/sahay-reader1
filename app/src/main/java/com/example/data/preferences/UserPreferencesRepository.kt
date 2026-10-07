package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sahaya_preferences")

data class UserPreferences(
    val speechRateEnglish: Float = 1.0f,
    val speechRateOdia: Float = 1.0f,
    val defaultReadingMode: String = "BILINGUAL", // ENGLISH, ODIA, BILINGUAL
    val bilingualPauseSec: Float = 1.0f,
    val bilingualOrder: String = "EN_THEN_OR", // EN_THEN_OR or OR_THEN_EN
    val highContrast: Boolean = false,
    val largeText: Boolean = false,
    val hapticFeedback: Boolean = true,
    val keepScreenAwake: Boolean = true,
    val autoTranslate: Boolean = true,
    val autoPlayNextUnit: Boolean = true,
    val backendBaseUrl: String = "http://10.0.2.2:8080/",
    val hasCompletedOnboarding: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val SPEECH_RATE_EN = floatPreferencesKey("speech_rate_en")
        val SPEECH_RATE_OR = floatPreferencesKey("speech_rate_or")
        val READING_MODE = stringPreferencesKey("reading_mode")
        val BILINGUAL_PAUSE = floatPreferencesKey("bilingual_pause")
        val BILINGUAL_ORDER = stringPreferencesKey("bilingual_order")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val LARGE_TEXT = booleanPreferencesKey("large_text")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val AUTO_TRANSLATE = booleanPreferencesKey("auto_translate")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val BACKEND_BASE_URL = stringPreferencesKey("backend_base_url")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            speechRateEnglish = preferences[PreferencesKeys.SPEECH_RATE_EN] ?: 1.0f,
            speechRateOdia = preferences[PreferencesKeys.SPEECH_RATE_OR] ?: 1.0f,
            defaultReadingMode = preferences[PreferencesKeys.READING_MODE] ?: "BILINGUAL",
            bilingualPauseSec = preferences[PreferencesKeys.BILINGUAL_PAUSE] ?: 1.0f,
            bilingualOrder = preferences[PreferencesKeys.BILINGUAL_ORDER] ?: "EN_THEN_OR",
            highContrast = preferences[PreferencesKeys.HIGH_CONTRAST] ?: false,
            largeText = preferences[PreferencesKeys.LARGE_TEXT] ?: false,
            hapticFeedback = preferences[PreferencesKeys.HAPTIC_FEEDBACK] ?: true,
            keepScreenAwake = preferences[PreferencesKeys.KEEP_SCREEN_AWAKE] ?: true,
            autoTranslate = preferences[PreferencesKeys.AUTO_TRANSLATE] ?: true,
            autoPlayNextUnit = preferences[PreferencesKeys.AUTO_PLAY_NEXT] ?: true,
            backendBaseUrl = preferences[PreferencesKeys.BACKEND_BASE_URL] ?: "http://10.0.2.2:8080/",
            hasCompletedOnboarding = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun setSpeechRateEnglish(rate: Float) {
        context.dataStore.edit { it[PreferencesKeys.SPEECH_RATE_EN] = rate }
    }

    suspend fun setSpeechRateOdia(rate: Float) {
        context.dataStore.edit { it[PreferencesKeys.SPEECH_RATE_OR] = rate }
    }

    suspend fun setDefaultReadingMode(mode: String) {
        context.dataStore.edit { it[PreferencesKeys.READING_MODE] = mode }
    }

    suspend fun setBilingualPauseSec(seconds: Float) {
        context.dataStore.edit { it[PreferencesKeys.BILINGUAL_PAUSE] = seconds }
    }

    suspend fun setBilingualOrder(order: String) {
        context.dataStore.edit { it[PreferencesKeys.BILINGUAL_ORDER] = order }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HIGH_CONTRAST] = enabled }
    }

    suspend fun setLargeText(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.LARGE_TEXT] = enabled }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setKeepScreenAwake(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.KEEP_SCREEN_AWAKE] = enabled }
    }

    suspend fun setAutoTranslate(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_TRANSLATE] = enabled }
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_PLAY_NEXT] = enabled }
    }

    suspend fun setBackendBaseUrl(url: String) {
        context.dataStore.edit { it[PreferencesKeys.BACKEND_BASE_URL] = url }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.ONBOARDING_COMPLETED] = completed }
    }
}
