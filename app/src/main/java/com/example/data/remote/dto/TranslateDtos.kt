package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TranslateRequest(
    @Json(name = "text") val text: String,
    @Json(name = "sourceLanguage") val sourceLanguage: String = "en",
    @Json(name = "targetLanguage") val targetLanguage: String = "or"
)

@JsonClass(generateAdapter = true)
data class TranslateResponse(
    @Json(name = "translation") val translation: String,
    @Json(name = "sourceLanguage") val sourceLanguage: String = "en",
    @Json(name = "targetLanguage") val targetLanguage: String = "or"
)

@JsonClass(generateAdapter = true)
data class TtsRequest(
    @Json(name = "text") val text: String,
    @Json(name = "language") val language: String = "or-IN"
)

@JsonClass(generateAdapter = true)
data class HealthResponse(
    @Json(name = "status") val status: String
)
