package com.aifinance.core.model

import java.time.Instant
import java.util.UUID

data class ImportBatch(
    val id: UUID = UUID.randomUUID(),
    val sourceType: ImportSourceType,
    val fileName: String? = null,
    val totalCount: Int,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val status: ImportStatus = ImportStatus.PENDING,
    val startedAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val errorMessage: String? = null,
)

enum class ImportSourceType {
    ALIPAY_CSV,
    WECHAT_CSV,
    BANK_CSV,
    EXCEL,
    UNKNOWN,
}

enum class ImportStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    PARTIAL,
}
