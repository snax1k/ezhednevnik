package com.snax1k.chekki.data

import java.time.LocalTime

/**
 * От какого времени отсчитывается напоминание задачи на весь день: «в день задачи» — в это время,
 * «накануне» — в это же время днём раньше. Потом можно вынести в настройки.
 */
val AllDayReminderTime: LocalTime = LocalTime.of(9, 0)
