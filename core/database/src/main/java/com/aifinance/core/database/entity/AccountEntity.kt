package com.aifinance.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: UUID,
    val name: String,
    val type: String,
    val currency: String,
    val initialBalance: BigDecimal,
    val currentBalance: BigDecimal,
    val color: Int,
    val icon: String,
    val note: String?,
    val isArchived: Boolean,
    val includeInTotalAssets: Boolean,
    val isDefaultIncomeExpense: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
