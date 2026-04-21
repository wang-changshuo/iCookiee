package com.aifinance.core.data.repository.ai

import com.aifinance.core.data.BuildConfig
import com.aifinance.core.data.network.api.DeepSeekApi
import com.aifinance.core.data.network.api.PaddleOCRApi
import com.aifinance.core.data.network.model.DeepSeekMessage
import com.aifinance.core.data.network.model.DeepSeekRequest
import com.aifinance.core.data.network.model.PaddleOCRJobResponse
import com.aifinance.core.data.network.model.createAssistantMessage
import com.aifinance.core.data.network.model.createSystemMessage
import com.aifinance.core.data.network.model.createUserMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIRepository @Inject constructor(
    private val deepSeekApi: DeepSeekApi,
    private val paddleOCRApi: PaddleOCRApi,
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {

    companion object {
        private const val MAX_RETRY_COUNT = 30
        private const val POLLING_INTERVAL_MS = 3000L
    }

    private val conversationHistory = mutableListOf<DeepSeekMessage>()

    init {
        conversationHistory.add(createSystemMessage())
    }

    /**
     * 发送消息到DeepSeek并获取回复
     */
    suspend fun sendMessage(userMessage: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            conversationHistory.add(createUserMessage(userMessage))

            val request = DeepSeekRequest(
                messages = conversationHistory.toList()
            )

            android.util.Log.d("AIRepository", "Sending message to DeepSeek: $userMessage")

            val response = deepSeekApi.chatCompletion(
                authorization = "Bearer ${BuildConfig.DEEPSEEK_API_KEY}",
                request = request
            )

            android.util.Log.d("AIRepository", "DeepSeek response received")

            val assistantContent = response.choices.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("Empty response from AI"))

            conversationHistory.add(createAssistantMessage(assistantContent))

            // 保留最近20条消息，避免token过多
            if (conversationHistory.size > 20) {
                val systemMessage = conversationHistory.first()
                conversationHistory.clear()
                conversationHistory.add(systemMessage)
                conversationHistory.addAll(
                    conversationHistory.takeLast(19)
                )
            }

            Result.success(assistantContent)
        } catch (e: Exception) {
            android.util.Log.e("AIRepository", "Send message failed", e)
            Result.failure(e)
        }
    }

    /**
     * 发送消息到DeepSeek并获取回复（带动态系统上下文）
     */
    suspend fun sendMessageWithContext(userMessage: String, systemContext: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val messages = mutableListOf<DeepSeekMessage>()
            messages.add(createSystemMessage(systemContext))

            conversationHistory.drop(1).forEach { msg ->
                if (msg.role != "system") {
                    messages.add(msg)
                }
            }

            messages.add(createUserMessage(userMessage))

            val request = DeepSeekRequest(messages = messages)

            android.util.Log.d("AIRepository", "Sending message with context to DeepSeek: $userMessage")

            val response = deepSeekApi.chatCompletion(
                authorization = "Bearer ${BuildConfig.DEEPSEEK_API_KEY}",
                request = request
            )

            android.util.Log.d("AIRepository", "DeepSeek response received")

            val assistantContent = response.choices.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("Empty response from AI"))

            conversationHistory.add(createUserMessage(userMessage))
            conversationHistory.add(createAssistantMessage(assistantContent))

            if (conversationHistory.size > 20) {
                val systemMessage = conversationHistory.first()
                conversationHistory.clear()
                conversationHistory.add(systemMessage)
                conversationHistory.addAll(conversationHistory.takeLast(19))
            }

            Result.success(assistantContent)
        } catch (e: Exception) {
            android.util.Log.e("AIRepository", "Send message with context failed", e)
            Result.failure(e)
        }
    }

    /**
     * 清空对话历史
     */
    fun clearConversation() {
        conversationHistory.clear()
        conversationHistory.add(createSystemMessage())
    }

    /**
     * 上传图片进行OCR识别（异步任务模式）
     */
    suspend fun recognizeImage(file: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) {
                return@withContext Result.failure(Exception("文件不存在: ${file.absolutePath}"))
            }

            if (file.length() == 0L) {
                return@withContext Result.failure(Exception("文件为空"))
            }

            android.util.Log.d("AIRepository", "Uploading file: ${file.absolutePath}, size: ${file.length()} bytes, ext: ${file.extension}")

            val mediaType = when (file.extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "pdf" -> "application/pdf"
                else -> "image/jpeg"
            }.toMediaTypeOrNull()

            android.util.Log.d("AIRepository", "MediaType: $mediaType")

            val fileBody = file.asRequestBody(mediaType)
            
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", "PaddleOCR-VL-1.5")
                .addFormDataPart(
                    "file",
                    file.name,
                    fileBody
                )
                .build()

            val request = Request.Builder()
                .url("${PaddleOCRApi.BASE_URL}api/v2/ocr/jobs")
                .header("Authorization", "bearer ${BuildConfig.PADDLEOCR_TOKEN}")
                .post(requestBody)
                .build()

            android.util.Log.d("AIRepository", "Sending request to: ${PaddleOCRApi.BASE_URL}api/v2/ocr/jobs")
            
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            
            android.util.Log.d("AIRepository", "Upload response code: ${response.code}")
            android.util.Log.d("AIRepository", "Upload response body: $responseBody")
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("上传失败: HTTP ${response.code}, body: $responseBody"))
            }

            if (responseBody == null) {
                return@withContext Result.failure(Exception("服务器返回空响应"))
            }

            val jobResponse = try {
                json.decodeFromString<PaddleOCRJobResponse>(responseBody)
            } catch (e: Exception) {
                android.util.Log.e("AIRepository", "JSON parse error", e)
                return@withContext Result.failure(Exception("解析响应失败: ${e.message}, body: $responseBody"))
            }
            
            val jobId = jobResponse.data.jobId
            android.util.Log.d("AIRepository", "Job created: $jobId")
            pollJobStatus(jobId)
        } catch (e: Exception) {
            android.util.Log.e("AIRepository", "OCR failed: ${e.javaClass.simpleName}: ${e.message}", e)
            Result.failure(Exception("${e.javaClass.simpleName}: ${e.message ?: "未知错误"}"))
        }
    }

    /**
     * 轮询OCR任务状态
     */
    private suspend fun pollJobStatus(jobId: String): Result<String> = withContext(Dispatchers.IO) {
        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                android.util.Log.d("AIRepository", "Polling job $jobId, attempt ${attempt + 1}")
                
                val request = Request.Builder()
                    .url("${PaddleOCRApi.BASE_URL}api/v2/ocr/jobs/$jobId")
                    .header("Authorization", "bearer ${BuildConfig.PADDLEOCR_TOKEN}")
                    .get()
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                
                android.util.Log.d("AIRepository", "Poll response code: ${response.code}")
                
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("状态检查失败: HTTP ${response.code}"))
                }

                if (responseBody == null) {
                    return@withContext Result.failure(Exception("状态响应为空"))
                }

                val statusResponse = json.decodeFromString<PaddleOCRJobResponse>(responseBody)
                val state = statusResponse.data.state
                android.util.Log.d("AIRepository", "Job state: $state")

                when (state) {
                    "done" -> {
                        val jsonUrl = statusResponse.data.resultUrl?.jsonUrl
                            ?: return@withContext Result.failure(Exception("没有结果URL"))
                        android.util.Log.d("AIRepository", "Job done, downloading result from $jsonUrl")
                        val resultText = downloadOCRResult(jsonUrl)
                        return@withContext Result.success(resultText)
                    }
                    "failed" -> {
                        val errorMsg = statusResponse.data.errorMsg ?: "未知错误"
                        android.util.Log.e("AIRepository", "Job failed: $errorMsg")
                        return@withContext Result.failure(Exception("OCR任务失败: $errorMsg"))
                    }
                    "pending", "running", null -> {
                        val progress = statusResponse.data.extractProgress
                        android.util.Log.d("AIRepository", "Job ${state ?: "processing"}, progress: $progress")
                        delay(POLLING_INTERVAL_MS)
                    }
                    else -> {
                        android.util.Log.w("AIRepository", "Unknown state: $state")
                        delay(POLLING_INTERVAL_MS)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AIRepository", "Polling error", e)
                return@withContext Result.failure(Exception("轮询失败: ${e.message}"))
            }
        }
        return@withContext Result.failure(Exception("OCR超时，请稍后重试"))
    }

    /**
     * 下载OCR结果
     */
    private suspend fun downloadOCRResult(jsonUrl: String): String {
        return try {
            val request = Request.Builder()
                .url(jsonUrl)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return "OCR识别完成，但无法下载详细结果"
            }

            val jsonlContent = response.body?.string() ?: return "OCR识别完成，但结果为空"

            val lines = jsonlContent.lines().filter { it.isNotBlank() }
            val results = StringBuilder()

            lines.forEach { line ->
                try {
                    val jsonElement = json.parseToJsonElement(line)
                    val result = jsonElement.jsonObject["result"]?.jsonObject
                    val layoutResults = result?.get("layoutParsingResults")?.jsonArray

                    layoutResults?.forEach { layoutResult ->
                        val markdown = layoutResult.jsonObject["markdown"]?.jsonObject
                        val text = markdown?.get("text")?.jsonPrimitive?.content
                        if (!text.isNullOrBlank()) {
                            results.append(text).append("\n\n")
                        }
                    }
                } catch (_: Exception) {
                }
            }

            if (results.isEmpty()) {
                "OCR识别完成，但未提取到文本内容"
            } else {
                results.toString().trim()
            }
        } catch (e: Exception) {
            "OCR识别完成，但结果解析失败: ${e.message}"
        }
    }
}