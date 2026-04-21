package com.aifinance.core.model

import java.math.BigDecimal
import java.time.LocalDate
import java.time.Instant
import java.util.UUID

data class ParsedRecord(
    val id: UUID = UUID.randomUUID(),
    val batchId: UUID,
    val rawData: String,
    val parsedDate: LocalDate? = null,
    val parsedAmount: BigDecimal? = null,
    val parsedMerchant: String? = null,
    val parsedDescription: String? = null,
    val parsedType: TransactionType? = null,
    val status: ParsedRecordStatus = ParsedRecordStatus.PENDING,
    val matchedTransactionId: UUID? = null,
    val errorMessage: String? = null,
    val createdAt: Instant = Instant.now(),
)

enum class ParsedRecordStatus {
    PENDING,
    MATCHED,
    NEEDS_REVIEW,
    FAILED,
    IGNORED,
}
