package com.aifinance.core.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class Account(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val type: AccountType,
    val currency: CurrencyCode,
    val initialBalance: BigDecimal,
    val currentBalance: BigDecimal,
    val color: Int,
    val icon: String,
    val note: String? = null,
    val isArchived: Boolean = false,
    val includeInTotalAssets: Boolean = true,
    val isDefaultIncomeExpense: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

enum class AccountType {
    CASH,
    BANK,
    CREDIT_CARD,
    INVESTMENT,
    DIGITAL_WALLET,
    OTHER,
}

typealias CurrencyCode = String
