package com.aifinance.feature.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinance.core.data.repository.ai.AIRepository
import com.aifinance.core.model.TransactionType
import com.aifinance.feature.home.state.AIRecognitionResult
import com.aifinance.feature.home.state.AIRecognitionState
import com.aifinance.feature.home.state.ProcessingStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

/**
 * AI记账记录ViewModel
 *
 * 管理AI识别图片账单的完整流程，包括OCR识别、AI解析和结果处理。
 *
 * @property aiRepository AI仓库，用于OCR识别和AI对话
 */
@HiltViewModel
class AIRecordViewModel @Inject constructor(
    private val aiRepository: AIRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AIRecognitionState>(AIRecognitionState.Idle)
    val uiState: StateFlow<AIRecognitionState> = _uiState.asStateFlow()

    private var savedUri: Uri? = null
    private var savedFile: File? = null

    /**
     * 处理图片进行AI识别
     *
     * 执行完整的识别流程：上传图片 → OCR识别 → AI解析 → 返回结果
     *
     * @param uri 图片URI，用于重试时重新处理
     * @param file 图片文件，用于上传识别
     */
    fun processImage(uri: Uri, file: File) {
        savedUri = uri
        savedFile = file

        viewModelScope.launch {
            _uiState.value = AIRecognitionState.Processing(ProcessingStep.UPLOADING)

            aiRepository.recognizeImage(file)
                .onSuccess { ocrText ->
                    _uiState.value = AIRecognitionState.Processing(ProcessingStep.RECOGNIZING)
                    val prompt = buildPrompt(ocrText)

                    _uiState.value = AIRecognitionState.Processing(ProcessingStep.PARSING)

                    aiRepository.sendMessage(prompt)
                        .onSuccess { jsonResponse ->
                            parseAndEmitSuccess(jsonResponse)
                        }
                        .onFailure { error ->
                            _uiState.value = AIRecognitionState.Error(
                                message = "AI解析失败: ${error.message}",
                                canRetry = true
                            )
                        }
                }
                .onFailure { error ->
                    _uiState.value = AIRecognitionState.Error(
                        message = "OCR识别失败: ${error.message}",
                        canRetry = true
                    )
                }
        }
    }

    /**
     * 重试上一次识别
     *
     * 使用保存的URI和文件重新执行识别流程
     */
    fun retry() {
        savedFile?.let { file ->
            savedUri?.let { uri ->
                processImage(uri, file)
            }
        }
    }

    /**
     * 重置状态
     *
     * 清除当前状态和保存的数据，恢复到初始状态
     */
    fun reset() {
        _uiState.value = AIRecognitionState.Idle
        savedUri = null
        savedFile = null
    }

    /**
     * 构建AI提示词
     *
     * @param ocrText OCR识别的文本内容
     * @return 用于AI解析的提示词
     */
    private fun buildPrompt(ocrText: String): String = """
        你是一个智能记账助手。请分析以下账单/收据的OCR文本，提取交易信息并智能推断缺失字段。

        提取字段：
        1. amount: 交易金额（纯数字，必须为正数）
        2. merchant: 商家/交易对方名称（简洁明了）
        3. date: 交易日期 (ISO格式 yyyy-MM-dd)
        4. paymentTime: 支付时间 (HH:mm格式，24小时制。如原始时间是"11:49:15"则返回"11:49")
        5. paymentMethod: 支付方式（标准化为：支付宝、微信支付、银行卡、信用卡、现金、其他）
        6. paymentAccount: 支付账户详情（如：湖北农信储蓄卡(7198)、建设银行储蓄卡等）
        7. category: 智能分类，必须使用以下中文分类名之一：餐饮、交通、购物、娱乐、账单、教育、其他
        8. description: 商品/服务描述（简要概括）
        9. type: 交易类型（INCOME 或 EXPENSE）
        10. confidence: 各字段识别置信度对象，格式为 {"amount":1.0,"merchant":0.9,"date":1.0,"paymentTime":0.8,"paymentMethod":0.9,"category":0.7}
           - 如果字段明确识别到，置信度为 1.0
           - 如果字段需要推断或不确定，置信度 0.5-0.9
           - 如果字段完全未找到，置信度为 0.0

        推理规则：
        - 负号金额(-5.98)表示支出，type应为"EXPENSE"
        - 根据商品描述智能推断分类（如"考研数学网课" → "教育"，"吃饭" → "餐饮"）
        - 如果支付方式显示具体银行名称，paymentMethod统一归为"银行卡"
        - 日期格式转换："2026-03-29" 保持不变

        OCR文本: $ocrText

        返回JSON格式：
        {
            "amount": 5.98,
            "merchant": "闲鱼",
            "date": "2026-03-29",
            "paymentTime": "11:49",
            "paymentMethod": "银行卡",
            "paymentAccount": "湖北农信储蓄卡(7198)",
            "category": "教育",
            "description": "2027张宇考研数学网课基础班",
            "type": "EXPENSE",
            "confidence": {"amount":1.0,"merchant":1.0,"date":1.0,"paymentTime":1.0,"paymentMethod":1.0,"category":0.9}
        }
    """.trimIndent()

    @Serializable
    data class ConfidenceResponse(
        val amount: Float = 1.0f,
        val merchant: Float = 1.0f,
        val date: Float = 1.0f,
        val paymentTime: Float = 1.0f,
        val paymentMethod: Float = 1.0f,
        val paymentAccount: Float = 1.0f,
        val description: Float = 1.0f,
        val type: Float = 1.0f,
        val category: Float = 1.0f
    )

    /**
     * AI识别响应数据类
     *
     * 用于解析AI返回的JSON响应
     */
    @Serializable
    data class RecognitionResponse(
        val amount: Double,
        val category: String,
        val merchant: String,
        val date: String,
        val type: String,
        val paymentTime: String? = null,
        val paymentMethod: String? = null,
        val paymentAccount: String? = null,
        val description: String? = null,
        val confidence: ConfidenceResponse? = null
    )

    private fun parseAndEmitSuccess(json: String) {
        try {
            val response = Json.decodeFromString<RecognitionResponse>(json)
            val confidence = response.confidence?.let {
                com.aifinance.feature.home.state.RecognitionConfidence(
                    amount = it.amount,
                    merchant = it.merchant,
                    date = it.date,
                    paymentTime = it.paymentTime,
                    paymentMethod = it.paymentMethod,
                    paymentAccount = it.paymentAccount,
                    description = it.description,
                    type = it.type,
                    category = it.category
                )
            } ?: com.aifinance.feature.home.state.RecognitionConfidence()

            val result = AIRecognitionResult(
                amount = response.amount,
                category = response.category,
                merchant = response.merchant,
                date = response.date,
                paymentTime = response.paymentTime,
                paymentMethod = response.paymentMethod,
                paymentAccount = response.paymentAccount,
                description = response.description,
                type = TransactionType.valueOf(response.type),
                confidence = confidence
            )
            _uiState.value = AIRecognitionState.Success(result)
        } catch (e: Exception) {
            _uiState.value = AIRecognitionState.Error(
                message = "解析结果失败: ${e.message}",
                canRetry = true
            )
        }
    }
}
