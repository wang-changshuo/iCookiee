package com.aifinance.core.data.scheduler

import java.time.Instant
import java.util.UUID

interface ScheduledRuleScheduler {
    suspend fun scheduleRule(ruleId: UUID)
    suspend fun enqueueKnownNext(ruleId: UUID, nextInstant: Instant)
    suspend fun cancelRule(ruleId: UUID)
    suspend fun rescheduleAllEnabled()
}
