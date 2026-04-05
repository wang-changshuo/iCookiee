package com.aifinance.core.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.Instant

object AppDateTime {
    val zoneId: ZoneId = ZoneId.of("Asia/Shanghai")

    fun now(): LocalDateTime = LocalDateTime.now(zoneId)

    fun today(): LocalDate = LocalDate.now(zoneId)

    fun toInstant(dateTime: LocalDateTime): Instant = dateTime.atZone(zoneId).toInstant()
}
