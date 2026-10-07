package com.example.data.remote

sealed class TranslationResult {
    data class Success(val translatedText: String, val sourceLanguage: String, val targetLanguage: String) : TranslationResult()
    data class Error(val message: String, val throwable: Throwable? = null) : TranslationResult()
}

interface TranslationProvider {
    suspend fun translate(text: String, sourceLang: String = "en", targetLang: String = "or"): TranslationResult
}
