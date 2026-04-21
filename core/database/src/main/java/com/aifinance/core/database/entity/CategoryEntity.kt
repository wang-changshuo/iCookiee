package com.aifinance.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: UUID,
    val name: String,
    val icon: String,
    val color: Int,
    val type: String,
    val parentId: UUID?,
    val isDefault: Boolean,
    val order: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)
