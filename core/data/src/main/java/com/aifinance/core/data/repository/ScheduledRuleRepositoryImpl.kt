package com.aifinance.core.data.repository

import com.aifinance.core.database.dao.ScheduledRuleDao
import com.aifinance.core.database.entity.ScheduledRuleEntity
import com.aifinance.core.model.ScheduledEndMode
import com.aifinance.core.model.ScheduledRecurrence
import com.aifinance.core.model.ScheduledRule
import com.aifinance.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduledRuleRepositoryImpl @Inject constructor(
    private val dao: ScheduledRuleDao,
) : ScheduledRuleRepository {

    override fun observeAll(): Flow<List<ScheduledRule>> {
        return dao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAllEnabled(): List<ScheduledRule> {
        return dao.getAllEnabled().map { it.toDomain() }
    }

    override suspend fun getById(id: UUID): ScheduledRule? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun insert(rule: ScheduledRule) {
        dao.insert(rule.toEntity())
    }

    override suspend fun update(rule: ScheduledRule) {
        dao.update(rule.toEntity())
    }

    override suspend fun delete(id: UUID) {
        val entity = dao.getById(id) ?: return
        dao.delete(entity)
    }
}

private fun ScheduledRuleEntity.toDomain(): ScheduledRule {
    return ScheduledRule(
        id = id,
        enabled = enabled,
        title = title,
        transactionType = TransactionType.valueOf(transactionType),
        categoryId = categoryId,
        accountId = accountId,
        amount = amount,
        currency = currency,
        startDate = startDate,
        startHour = startHour,
        startMinute = startMinute,
        recurrence = ScheduledRecurrence.valueOf(recurrence),
        endMode = ScheduledEndMode.valueOf(endMode),
        endDate = endDate,
        maxOccurrences = maxOccurrences,
        firedCount = firedCount,
        lastFiredAt = lastFiredAt,
        nextRunAt = nextRunAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

private fun ScheduledRule.toEntity(): ScheduledRuleEntity {
    return ScheduledRuleEntity(
        id = id,
        enabled = enabled,
        title = title,
        transactionType = transactionType.name,
        categoryId = categoryId,
        accountId = accountId,
        amount = amount,
        currency = currency,
        startDate = startDate,
        startHour = startHour,
        startMinute = startMinute,
        recurrence = recurrence.name,
        endMode = endMode.name,
        endDate = endDate,
        maxOccurrences = maxOccurrences,
        firedCount = firedCount,
        lastFiredAt = lastFiredAt,
        nextRunAt = nextRunAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
