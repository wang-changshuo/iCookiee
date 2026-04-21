package com.aifinance.core.data.network.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * PaddleOCR 布局解析请求
 */
@Serializable
data class PaddleOCRLayoutRequest(
    val file: String,
    val fileType: Int, // 0 = PDF, 1 = Image
    val useDocOrientationClassify: Boolean = false,
    val useDocUnwarping: Boolean = false,
    val useChartRecognition: Boolean = false
)

@Serializable
data class PaddleOCRLayoutResponse(
    val result: PaddleOCRLayoutResult
)

@Serializable
data class PaddleOCRLayoutResult(
    val layoutParsingResults: List<LayoutParsingResult>
)

@Serializable
data class LayoutParsingResult(
    val markdown: MarkdownResult,
    val outputImages: Map<String, String> = emptyMap()
)

@Serializable
data class MarkdownResult(
    val text: String,
    val images: Map<String, String> = emptyMap()
)

/**
 * PaddleOCR 任务提交请求
 */
@Serializable
data class PaddleOCRJobRequest(
    val fileUrl: String? = null,
    val model: String = "PaddleOCR-VL-1.5",
    val optionalPayload: PaddleOCROptions? = null
)

@Serializable
data class PaddleOCROptions(
    val useDocOrientationClassify: Boolean = false,
    val useDocUnwarping: Boolean = false,
    val useChartRecognition: Boolean = false
)

@Serializable
data class PaddleOCRJobResponse(
    val data: PaddleOCRJobData
)

@Serializable
data class PaddleOCRJobData(
    val jobId: String,
    val state: String? = null,
    val errorMsg: String? = null,
    val extractProgress: ExtractProgress? = null,
    val resultUrl: ResultUrl? = null
)

@Serializable
data class ExtractProgress(
    val totalPages: Int? = null,
    val extractedPages: Int? = null,
    val startTime: String? = null,
    val endTime: String? = null
)

@Serializable
data class ResultUrl(
    val jsonUrl: String
)

/**
 * 解析后的交易记录
 */
data class ParsedTransactionInfo(
    val amount: String? = null,
    val date: String? = null,
    val merchant: String? = null,
    val category: String? = null,
    val description: String? = null,
    val rawText: String = ""
)