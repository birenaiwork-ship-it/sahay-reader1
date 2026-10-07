package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateAudiobookJobRequest(
    @Json(name = "bookTitle") val bookTitle: String,
    @Json(name = "languageMode") val languageMode: String, // ENGLISH, ODIA, BILINGUAL
    @Json(name = "items") val items: List<AudiobookUnitItem>
)

@JsonClass(generateAdapter = true)
data class AudiobookUnitItem(
    @Json(name = "unitId") val unitId: String,
    @Json(name = "pageNumber") val pageNumber: Int,
    @Json(name = "englishText") val englishText: String,
    @Json(name = "odiaText") val odiaText: String?
)

@JsonClass(generateAdapter = true)
data class AudiobookJobResponse(
    @Json(name = "jobId") val jobId: String,
    @Json(name = "status") val status: String
)

@JsonClass(generateAdapter = true)
data class AudiobookJobStatusResponse(
    @Json(name = "jobId") val jobId: String,
    @Json(name = "status") val status: String,
    @Json(name = "progress") val progress: Int,
    @Json(name = "currentPage") val currentPage: Int,
    @Json(name = "totalPages") val totalPages: Int,
    @Json(name = "downloadUrl") val downloadUrl: String?
)
