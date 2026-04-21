package com.aifinance.core.model

import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import java.util.UUID

data class BudgetPlan(
    val id: UUID = UUID.randomUUID(),
    val categoryId: UUID?,
    val amount: BigDecimal,
    val period: BudgetPeriod,
    val startMonth: YearMonth,
    val endMonth: YearMonth? = null,
    val alertThreshold: Float = 0.8f,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

enum class BudgetPeriod {
    WEEKLY,
    MONTHLY,
    YEARLY,
}
