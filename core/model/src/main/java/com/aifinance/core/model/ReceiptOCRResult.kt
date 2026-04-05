package com.aifinance.core.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ReceiptOCRResult(
    val id: UUID = UUID.randomUUID(),
    val imagePath: String,
    val merchantName: String? = null,
    val totalAmount: BigDecimal? = null,
    val date: LocalDate? = null,
    val items: List<OCRItem> = emptyList(),
    val rawText: String? = null,
    val confidence: Float? = null,
    val status: OCRStatus = OCRStatus.PENDING,
    val errorMessage: String? = null,
    val createdAt: Instant = Instant.now(),
    val processedAt: Instant? = null,
)

data class OCRItem(
    val name: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal,
)

enum class OCRStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
}
