package com.aifinance.core.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Transaction(
    val id: UUID = UUID.randomUUID(),
    val accountId: UUID,
    val categoryId: UUID?,
    val type: TransactionType,
    val amount: BigDecimal,
    val currency: CurrencyCode,
    val title: String,
    val description: String? = null,
    val date: LocalDate,
    val time: Instant = Instant.now(),
    val isPending: Boolean = false,
    val receiptImagePath: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    
    val sourceType: TransactionSourceType = TransactionSourceType.MANUAL,
    val importBatchId: UUID? = null,
    val rawText: String? = null,
    val aiCategory: UUID? = null,
    val aiConfidence: Float? = null,
    val userConfirmed: Boolean = false,
    val ocrSourceId: UUID? = null,
    val paymentMethod: String? = null,
    val paymentAccount: String? = null,
)

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER,
}

enum class TransactionSourceType {
    MANUAL,
    IMPORTED_ALIPAY,
    IMPORTED_WECHAT,
    IMPORTED_BANK,
    OCR_RECEIPT,
    OCR_INVOICE,
    SCHEDULED,
}
