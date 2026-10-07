package com.example.data.remote

import java.io.File

sealed class TtsResult {
    data class Success(val audioFile: File) : TtsResult()
    data class Error(val message: String, val throwable: Throwable? = null) : TtsResult()
}

interface TextToSpeechProvider {
    suspend fun synthesizeToFile(text: String, language: String, outputFile: File): TtsResult
}
