package com.snax1k.chekki.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.LocalDate

/**
 * Сегодняшняя дата, которая сама меняется в полночь: списки и группы переключаются на новый день.
 * Если экран был свёрнут в полночь, поток запускается заново при возврате и сразу отдаёт новую дату.
 */
fun currentDateFlow(clock: Clock = Clock.systemDefaultZone()): Flow<LocalDate> = flow {
    while (true) {
        val today = LocalDate.now(clock)
        emit(today)
        val nextMidnight = today.plusDays(1).atStartOfDay(clock.zone).toInstant()
        delay(Duration.between(clock.instant(), nextMidnight).toMillis().coerceAtLeast(1))
    }
}.distinctUntilChanged()
