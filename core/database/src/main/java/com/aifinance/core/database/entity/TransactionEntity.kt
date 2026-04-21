package com.aifinance.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: UUID,
    val accountId: UUID,
    val categoryId: UUID?,
    val type: String,
    val amount: BigDecimal,
    val currency: String,
    val title: String,
    val description: String?,
    val date: LocalDate,
    val time: Instant,
    val isPending: Boolean,
    val receiptImagePath: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val sourceType: String,
    val importBatchId: UUID?,
    val rawText: String?,
    val aiCategory: UUID?,
    val aiConfidence: Float?,
    val userConfirmed: Boolean,
    val ocrSourceId: UUID?,
    val paymentMethod: String? = null,
    val paymentAccount: String? = null,
)
