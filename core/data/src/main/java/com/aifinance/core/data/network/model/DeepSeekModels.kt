package com.aifinance.core.data.network.model

import kotlinx.serialization.Serializable

/**
 * DeepSeek API 请求数据模型
 */
@Serializable
data class DeepSeekRequest(
    val model: String = "deepseek-reasoner",
    val messages: List<DeepSeekMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 2000,
    val stream: Boolean = false
)

@Serializable
data class DeepSeekMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

/**
 * DeepSeek API 响应数据模型
 */
@Serializable
data class DeepSeekResponse(
    val id: String,
    val choices: List<DeepSeekChoice>,
    val usage: DeepSeekUsage? = null
)

@Serializable
data class DeepSeekChoice(
    val index: Int,
    val message: DeepSeekMessage,
    val finish_reason: String? = null
)

@Serializable
data class DeepSeekUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

/**
 * AI助手角色定义
 */
const val AI_SYSTEM_PROMPT = """你是一个智能财务助手，帮助用户管理个人财务。你可以：
1. 回答关于记账、支出、收入的问题
2. 分析用户的消费习惯和财务状况
3. 提供理财建议
4. 帮助用户理解App的各项功能
5. 识别账单图片中的交易信息

请用友好、专业的方式回答用户问题。如果用户上传了账单图片，请帮助提取关键信息（金额、日期、商家、分类等）。"""

fun createSystemMessage(): DeepSeekMessage {
    return DeepSeekMessage(
        role = "system",
        content = AI_SYSTEM_PROMPT
    )
}

fun createSystemMessage(content: String): DeepSeekMessage {
    return DeepSeekMessage(
        role = "system",
        content = content
    )
}

fun createUserMessage(content: String): DeepSeekMessage {
    return DeepSeekMessage(
        role = "user",
        content = content
    )
}

fun createAssistantMessage(content: String): DeepSeekMessage {
    return DeepSeekMessage(
        role = "assistant",
        content = content
    )
}