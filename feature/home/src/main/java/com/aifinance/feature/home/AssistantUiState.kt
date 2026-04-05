package com.aifinance.feature.home

data class AssistantUiState(
    val inputText: String = "",
    val suggestionGroupIndex: Int = 0,
    val messages: List<AssistantMessage> = emptyList(),
    val isLoading: Boolean = false,
)

data class AssistantMessage(
    val role: AssistantRole,
    val content: String,
)

enum class AssistantRole {
    USER,
    ASSISTANT,
}
