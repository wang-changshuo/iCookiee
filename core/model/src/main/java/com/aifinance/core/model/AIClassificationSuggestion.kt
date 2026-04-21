package com.aifinance.core.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class AIClassificationSuggestion(
    val id: UUID = UUID.randomUUID(),
    val transactionId: UUID? = null,
    val parsedRecordId: UUID? = null,
    val suggestedCategoryId: UUID,
    val confidence: Float,
    val modelVersion: String,
    val reasoning: String? = null,
    val userAccepted: Boolean? = null,
    val createdAt: Instant = Instant.now(),
)
