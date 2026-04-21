package com.aifinance.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aifinance.core.data.repository.CategoryRepository
import com.aifinance.core.data.repository.ScheduledRuleRepository
import com.aifinance.core.data.repository.TransactionRepository
import com.aifinance.core.data.schedule.ScheduleOccurrenceCalculator
import com.aifinance.core.data.scheduler.ScheduledRuleScheduler
import com.aifinance.core.model.ScheduledEndMode
import com.aifinance.core.model.ScheduledRule
import com.aifinance.core.model.Transaction
import com.aifinance.core.model.TransactionSourceType
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

class ScheduledTransactionWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ScheduledWorkerEntryPoint::class.java,
        )
        val scheduledRuleRepository = entryPoint.scheduledRuleRepository()
        val transactionRepository = entryPoint.transactionRepository()
        val categoryRepository = entryPoint.categoryRepository()
        val scheduledRuleScheduler = entryPoint.scheduledRuleScheduler()

        val ruleIdStr = inputData.getString(KEY_RULE_ID) ?: return Result.failure()
        val ruleId = try {
            UUID.fromString(ruleIdStr)
        } catch (_: IllegalArgumentException) {
            return Result.failure()
        }

        val rule = scheduledRuleRepository.getById(ruleId) ?: return Result.success()
        if (!rule.enabled) return Result.success()

        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val today = LocalDate.now(zone)

        if (!canFire(rule, today)) {
            scheduledRuleScheduler.cancelRule(ruleId)
            return Result.success()
        }

        val categoryName = rule.categoryId?.let { categoryRepository.getCategoryById(it)?.name }
        val transaction = Transaction(
            accountId = rule.accountId,
            categoryId = rule.categoryId,
            type = rule.transactionType,
            amount = rule.amount,
            currency = rule.currency,
            title = rule.title.ifBlank { "定时记账" },
            description = categoryName,
            date = today,
            time = now,
            sourceType = TransactionSourceType.SCHEDULED,
            userConfirmed = true,
        )
        transactionRepository.insertTransaction(transaction)

        val newCount = rule.firedCount + 1
        val fireLocal = LocalDateTime.of(today, LocalTime.of(rule.startHour, rule.startMinute))
        val nextBase = ScheduleOccurrenceCalculator.advance(fireLocal, rule.recurrence)
        val nextLocal = ScheduleOccurrenceCalculator.firstLocalDateTimeOnOrAfter(
            start = nextBase,
            recurrence = rule.recurrence,
            zone = zone,
            now = now,
        )
        val nextOccurrenceDate = nextLocal.toLocalDate()

        val maxOcc = rule.maxOccurrences
        val endD = rule.endDate
        val finished = when (rule.endMode) {
            ScheduledEndMode.AFTER_COUNT ->
                maxOcc != null && newCount >= maxOcc
            ScheduledEndMode.END_DATE ->
                endD != null && nextOccurrenceDate.isAfter(endD)
            ScheduledEndMode.NEVER -> false
        }

        val nextInstant = if (finished) {
            null
        } else {
            nextLocal.atZone(zone).toInstant()
        }

        val updatedRule = rule.copy(
            firedCount = newCount,
            lastFiredAt = now,
            nextRunAt = nextInstant,
            updatedAt = Instant.now(),
        )
        scheduledRuleRepository.update(updatedRule)

        if (finished || nextInstant == null) {
            scheduledRuleScheduler.cancelRule(ruleId)
        } else {
            scheduledRuleScheduler.enqueueKnownNext(ruleId, nextInstant)
        }

        return Result.success()
    }

    private fun canFire(rule: ScheduledRule, today: LocalDate): Boolean {
        val endD = rule.endDate
        if (rule.endMode == ScheduledEndMode.END_DATE && endD != null && today.isAfter(endD)) {
            return false
        }
        val maxOcc = rule.maxOccurrences
        if (rule.endMode == ScheduledEndMode.AFTER_COUNT &&
            maxOcc != null &&
            rule.firedCount >= maxOcc
        ) {
            return false
        }
        return true
    }

    companion object {
        const val KEY_RULE_ID = "scheduled_rule_id"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ScheduledWorkerEntryPoint {
    fun scheduledRuleRepository(): ScheduledRuleRepository
    fun transactionRepository(): TransactionRepository
    fun categoryRepository(): CategoryRepository
    fun scheduledRuleScheduler(): ScheduledRuleScheduler
}
