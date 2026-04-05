package com.aifinance.app.work

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.aifinance.core.data.repository.ScheduledRuleRepository
import com.aifinance.core.data.schedule.ScheduleOccurrenceCalculator
import com.aifinance.core.data.scheduler.ScheduledRuleScheduler
import com.aifinance.core.model.ScheduledEndMode
import com.aifinance.core.model.ScheduledRule
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduledRuleWorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ScheduledRuleRepository,
) : ScheduledRuleScheduler {

    private val workManager get() = WorkManager.getInstance(context)
    private val zone: ZoneId get() = ZoneId.systemDefault()

    override suspend fun scheduleRule(ruleId: UUID) {
        val rule = repository.getById(ruleId) ?: return
        if (!rule.enabled) {
            cancelRule(ruleId)
            return
        }
        val start = LocalDateTime.of(rule.startDate, LocalTime.of(rule.startHour, rule.startMinute))
        val aligned = ScheduleOccurrenceCalculator.alignStartToRecurrence(start, rule.recurrence)
        val now = Instant.now()
        val nextLdt = ScheduleOccurrenceCalculator.firstLocalDateTimeOnOrAfter(
            aligned,
            rule.recurrence,
            zone,
            now,
        )
        if (!canScheduleOccurrence(rule, nextLdt.toLocalDate())) {
            repository.update(
                rule.copy(nextRunAt = null, updatedAt = Instant.now()),
            )
            cancelRule(ruleId)
            return
        }
        val nextInstant = nextLdt.atZone(zone).toInstant()
        repository.update(rule.copy(nextRunAt = nextInstant, updatedAt = Instant.now()))
        enqueueWork(ruleId, nextInstant)
    }

    override suspend fun enqueueKnownNext(ruleId: UUID, nextInstant: Instant) {
        val rule = repository.getById(ruleId) ?: return
        if (!rule.enabled) {
            cancelRule(ruleId)
            return
        }
        repository.update(rule.copy(nextRunAt = nextInstant, updatedAt = Instant.now()))
        enqueueWork(ruleId, nextInstant)
    }

    override suspend fun cancelRule(ruleId: UUID) {
        workManager.cancelUniqueWork(uniqueWorkName(ruleId))
    }

    override suspend fun rescheduleAllEnabled() {
        for (rule in repository.getAllEnabled()) {
            scheduleRule(rule.id)
        }
    }

    private fun enqueueWork(ruleId: UUID, whenInstant: Instant) {
        val delayMs = (whenInstant.toEpochMilli() - System.currentTimeMillis()).coerceAtLeast(0L)
        val data = Data.Builder()
            .putString(ScheduledTransactionWorker.KEY_RULE_ID, ruleId.toString())
            .build()
        val request = OneTimeWorkRequestBuilder<ScheduledTransactionWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        workManager.enqueueUniqueWork(
            uniqueWorkName(ruleId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun uniqueWorkName(ruleId: UUID) = "scheduled_tx_$ruleId"

    private fun canScheduleOccurrence(rule: ScheduledRule, occurrenceDate: java.time.LocalDate): Boolean {
        return when (rule.endMode) {
            ScheduledEndMode.NEVER -> true
            ScheduledEndMode.END_DATE ->
                rule.endDate == null || !occurrenceDate.isAfter(rule.endDate)
            ScheduledEndMode.AFTER_COUNT -> {
                val max = rule.maxOccurrences
                max == null || rule.firedCount < max
            }
        }
    }
}
