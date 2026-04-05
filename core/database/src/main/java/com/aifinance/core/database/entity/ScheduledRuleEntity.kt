package com.aifinance.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(tableName = "scheduled_rules")
data class ScheduledRuleEntity(
    @PrimaryKey
    val id: UUID,
    val enabled: Boolean,
    val title: String,
    val transactionType: String,
    val categoryId: UUID?,
    val accountId: UUID,
    val amount: BigDecimal,
    val currency: String,
    val startDate: LocalDate,
    val startHour: Int,
    val startMinute: Int,
    val recurrence: String,
    val endMode: String,
    val endDate: LocalDate?,
    val maxOccurrences: Int?,
    val firedCount: Int,
    val lastFiredAt: Instant?,
    val nextRunAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
