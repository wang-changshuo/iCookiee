package com.aifinance.core.model

import java.time.Instant
import java.util.UUID

data class Category(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val icon: String,
    val color: Int,
    val type: TransactionType = TransactionType.EXPENSE,
    val parentId: UUID? = null,
    val isDefault: Boolean = false,
    val order: Int = 0,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
