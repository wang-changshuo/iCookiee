package com.aifinance.core.data.repository

import com.aifinance.core.model.ScheduledRule
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface ScheduledRuleRepository {
    fun observeAll(): Flow<List<ScheduledRule>>
    suspend fun getAllEnabled(): List<ScheduledRule>
    suspend fun getById(id: UUID): ScheduledRule?
    suspend fun insert(rule: ScheduledRule)
    suspend fun update(rule: ScheduledRule)
    suspend fun delete(id: UUID)
}
