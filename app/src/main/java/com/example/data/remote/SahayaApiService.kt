package com.example.data.remote

import com.example.data.remote.dto.AudiobookJobResponse
import com.example.data.remote.dto.AudiobookJobStatusResponse
import com.example.data.remote.dto.CreateAudiobookJobRequest
import com.example.data.remote.dto.HealthResponse
import com.example.data.remote.dto.TranslateRequest
import com.example.data.remote.dto.TranslateResponse
import com.example.data.remote.dto.TtsRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

interface SahayaApiService {

    @POST("translate")
    suspend fun translateText(@Body request: TranslateRequest): Response<TranslateResponse>

    @Streaming
    @POST("tts")
    suspend fun synthesizeSpeech(@Body request: TtsRequest): Response<ResponseBody>

    @POST("audiobook")
    suspend fun startAudiobookJob(@Body request: CreateAudiobookJobRequest): Response<AudiobookJobResponse>

    @GET("audiobook/{id}")
    suspend fun getAudiobookJobStatus(@Path("id") jobId: String): Response<AudiobookJobStatusResponse>

    @GET("health")
    suspend fun checkHealth(): Response<HealthResponse>
}
