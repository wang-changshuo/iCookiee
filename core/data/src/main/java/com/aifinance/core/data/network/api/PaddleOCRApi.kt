package com.aifinance.core.data.network.api

import com.aifinance.core.data.network.model.PaddleOCRJobResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface PaddleOCRApi {

    @Multipart
    @POST("api/v2/ocr/jobs")
    suspend fun submitOCRJob(
        @Header("Authorization") authorization: String,
        @Part("model") model: RequestBody,
        @Part("optionalPayload") optionalPayload: RequestBody?,
        @Part file: MultipartBody.Part?
    ): PaddleOCRJobResponse

    @GET("api/v2/ocr/jobs/{jobId}")
    suspend fun getJobStatus(
        @Header("Authorization") authorization: String,
        @Path("jobId") jobId: String
    ): PaddleOCRJobResponse

    companion object {
        const val BASE_URL = "https://paddleocr.aistudio-app.com/"
        const val TOKEN = "f3686824d9d18683244db73aaf7a507478cc9c38"
    }
}