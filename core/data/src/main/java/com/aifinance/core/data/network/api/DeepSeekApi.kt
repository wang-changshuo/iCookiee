package com.aifinance.core.data.network.api

import com.aifinance.core.data.network.model.DeepSeekRequest
import com.aifinance.core.data.network.model.DeepSeekResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface DeepSeekApi {

    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: DeepSeekRequest
    ): DeepSeekResponse

    companion object {
        const val BASE_URL = "https://api.deepseek.com/v1/"
    }
}