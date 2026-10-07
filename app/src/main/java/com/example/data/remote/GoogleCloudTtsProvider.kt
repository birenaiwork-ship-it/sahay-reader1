package com.example.data.remote

import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.dto.TtsRequest
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream

class GoogleCloudTtsProvider(
    private val preferencesRepository: UserPreferencesRepository
) : TextToSpeechProvider {

    override suspend fun synthesizeToFile(
        text: String,
        language: String,
        outputFile: File
    ): TtsResult {
        if (text.isBlank()) {
            return TtsResult.Error("Text is empty")
        }
        return try {
            val prefs = preferencesRepository.preferencesFlow.first()
            val apiService = ApiClient.createService(prefs.backendBaseUrl)
            val response = apiService.synthesizeSpeech(
                TtsRequest(
                    text = text.trim(),
                    language = language
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                body.byteStream().use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
                TtsResult.Success(outputFile)
            } else {
                val errorBody = try { response.errorBody()?.string() } catch (_: Exception) { null }
                val errorMsg = if (response.code() == 503) {
                    "Cloud TTS is not configured on the backend server. Please configure Google Cloud credentials in the backend server."
                } else if (!errorBody.isNullOrBlank()) {
                    "TTS server error (${response.code()}): $errorBody"
                } else {
                    "TTS server returned code ${response.code()}: ${response.message()}"
                }
                TtsResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            TtsResult.Error("Failed to reach TTS backend: ${e.localizedMessage ?: "Unknown error"}", e)
        }
    }
}
