package com.aifinance.core.data.schedule

import com.aifinance.core.model.ScheduledRecurrence
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object ScheduleOccurrenceCalculator {

    /**
     * Snaps the user-chosen start so the first run matches recurrence (e.g. weekends → next Sat/Sun).
     */
    fun alignStartToRecurrence(start: LocalDateTime, recurrence: ScheduledRecurrence): LocalDateTime {
        return when (recurrence) {
            ScheduledRecurrence.WEEKDAYS -> {
                var d = start
                while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) {
                    d = d.plusDays(1)
                }
                d
            }
            ScheduledRecurrence.WEEKENDS -> {
                var d = start
                while (d.dayOfWeek != DayOfWeek.SATURDAY && d.dayOfWeek != DayOfWeek.SUNDAY) {
                    d = d.plusDays(1)
                }
                d
            }
            else -> start
        }
    }

    /**
     * First occurrence at or after [now] (wall clock in [zone]), advancing from [start] by [recurrence].
     */
    fun firstLocalDateTimeOnOrAfter(
        start: LocalDateTime,
        recurrence: ScheduledRecurrence,
        zone: ZoneId,
        now: Instant,
    ): LocalDateTime {
        val nowLdt = LocalDateTime.ofInstant(now, zone)
        var candidate = start
        while (candidate.isBefore(nowLdt)) {
            candidate = advance(candidate, recurrence)
        }
        return candidate
    }

    fun advance(local: LocalDateTime, recurrence: ScheduledRecurrence): LocalDateTime {
        return when (recurrence) {
            ScheduledRecurrence.DAILY -> local.plusDays(1)
            ScheduledRecurrence.WEEKLY -> local.plusWeeks(1)
            ScheduledRecurrence.MONTHLY -> local.plusMonths(1)
            ScheduledRecurrence.WEEKDAYS -> nextWeekday(local)
            ScheduledRecurrence.WEEKENDS -> nextWeekendDay(local)
            ScheduledRecurrence.EVERY_THREE_MONTHS -> local.plusMonths(3)
            ScheduledRecurrence.EVERY_SIX_MONTHS -> local.plusMonths(6)
            ScheduledRecurrence.YEARLY -> local.plusYears(1)
        }
    }

    private fun nextWeekday(local: LocalDateTime): LocalDateTime {
        var next = local.plusDays(1)
        while (next.dayOfWeek == DayOfWeek.SATURDAY || next.dayOfWeek == DayOfWeek.SUNDAY) {
            next = next.plusDays(1)
        }
        return next
    }

    private fun nextWeekendDay(local: LocalDateTime): LocalDateTime {
        var next = local.plusDays(1)
        while (next.dayOfWeek != DayOfWeek.SATURDAY && next.dayOfWeek != DayOfWeek.SUNDAY) {
            next = next.plusDays(1)
        }
        return next
    }
}
