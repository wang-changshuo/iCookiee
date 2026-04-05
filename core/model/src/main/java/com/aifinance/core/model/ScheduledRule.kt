package com.aifinance.core.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class ScheduledRecurrence {
    DAILY,
    WEEKLY,
    MONTHLY,
    WEEKDAYS,
    WEEKENDS,
    /** Fire on the last calendar day of each month */
    LAST_DAY_OF_MONTH,
    EVERY_THREE_MONTHS,
    EVERY_SIX_MONTHS,
    YEARLY,
}

enum class ScheduledEndMode {
    NEVER,
    AFTER_COUNT,
    END_DATE,
}

data class ScheduledRule(
    val id: UUID = UUID.randomUUID(),
    val enabled: Boolean = true,
    val title: String = "定时记账",
    val transactionType: TransactionType,
    val categoryId: UUID?,
    val accountId: UUID,
    val amount: BigDecimal,
    val currency: CurrencyCode,
    val startDate: LocalDate,
    val startHour: Int,
    val startMinute: Int,
    val recurrence: ScheduledRecurrence,
    val endMode: ScheduledEndMode,
    val endDate: LocalDate?,
    val maxOccurrences: Int?,
    val firedCount: Int = 0,
    val lastFiredAt: Instant? = null,
    val nextRunAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
