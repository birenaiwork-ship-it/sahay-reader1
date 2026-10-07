package com.example.data.remote

import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.dto.TranslateRequest
import kotlinx.coroutines.flow.first

class GoogleCloudTranslationProvider(
    private val preferencesRepository: UserPreferencesRepository
) : TranslationProvider {

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): TranslationResult {
        if (text.isBlank()) {
            return TranslationResult.Success("", sourceLang, targetLang)
        }
        return try {
            val prefs = preferencesRepository.preferencesFlow.first()
            val apiService = ApiClient.createService(prefs.backendBaseUrl)
            val response = apiService.translateText(
                TranslateRequest(
                    text = text.trim(),
                    sourceLanguage = sourceLang,
                    targetLanguage = targetLang
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                TranslationResult.Success(
                    translatedText = body.translation,
                    sourceLanguage = body.sourceLanguage,
                    targetLanguage = body.targetLanguage
                )
            } else {
                val errorBody = try { response.errorBody()?.string() } catch (_: Exception) { null }
                val errorMsg = if (response.code() == 503) {
                    "Cloud Translation is not configured on the backend server. Please configure Google Cloud credentials in the backend server."
                } else if (!errorBody.isNullOrBlank()) {
                    "Translation server error (${response.code()}): $errorBody"
                } else {
                    "Translation server returned code ${response.code()}: ${response.message()}"
                }
                TranslationResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            TranslationResult.Error(
                "Unable to connect to translation server (${e.localizedMessage ?: "Network error"}). Check network and server settings.",
                e
            )
        }
    }
}
