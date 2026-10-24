package com.snax1k.chekki.data.oldapp

/**
 * Резервная копия старого ежедневника (`.plnb`) в том виде, в каком она лежит в его базе `Planner.db`.
 * Только строки, которые не удалены (`isRemoved = 0`). Даты и моменты — как в базе, строками:
 * перевод в наши типы делает [OldAppConverter].
 */
data class OldBackup(
    val categories: List<OldCategory>,
    val tasks: List<OldTask>,
    val subtasks: List<OldSubtask>,
    val taskCategories: List<OldTaskCategory>,
    val regularities: List<OldRegularity>,
    val habits: List<OldHabit>,
    val habitRecords: List<OldHabitRecord>,
    val implementations: List<OldImplementation>,
    /** Вложения в архиве (`photo/`, `audio/`): пока не переносятся, только считаются для отчёта. */
    val photoCount: Int = 0,
    val audioCount: Int = 0,
    /** Подпись копии из `backup_meta`: по ней узнаётся повторный перенос той же копии. */
    val hash: String? = null
)

/** Категория: [icon] — имя иконки старого приложения («briefcase», «cart»…), [color] — ARGB. */
data class OldCategory(
    val uid: String,
    val title: String,
    val icon: String?,
    val color: Int,
    val order: Int
)

/**
 * Задача. [nextDate] — местная дата «2026-10-22», [time] — «16:0»; [created], [completedDate], [notifyDate],
 * [deadline] — моменты ISO в UTC. [regularityUid] — код правила повтора («week_1_72_0»), общий для разных задач.
 */
data class OldTask(
    val uid: String,
    val title: String,
    val text: String?,
    val completed: Boolean,
    /** «not», «small», «medium», «large». */
    val importance: String?,
    val created: String?,
    val nextDate: String?,
    val time: String?,
    val completedDate: String?,
    val notifyDate: String?,
    val deadline: String?,
    val regularityUid: String?
)

data class OldSubtask(
    val taskUid: String,
    val title: String,
    val position: Int,
    val completed: Boolean
)

/** Категория задачи; у задачи их может быть несколько, порядок — [order]. */
data class OldTaskCategory(
    val taskUid: String,
    val categoryUid: String,
    val order: Int
)

/**
 * Правило повтора. [type] — «day», «week», «month»; [period] — каждые N; [daysMask] — дни битами:
 * у недели бит 1 — понедельник … бит 7 — воскресенье (при [byTwoWeeks] биты 1–14 на две недели),
 * у месяца бит N — N-е число.
 */
data class OldRegularity(
    val uid: String,
    val type: String,
    val period: Int,
    val daysMask: Long,
    val byTwoWeeks: Boolean
)

/** Привычка. Цели на день отдельным полем нет: это число её «реализаций» ([OldImplementation]). */
data class OldHabit(
    val uid: String,
    val title: String,
    val icon: String?,
    val color: Int,
    val regularityUid: String?,
    val position: Int,
    val startDate: String?,
    val paused: Boolean,
    /** Привычка завершена — у нас это архив. */
    val completed: Boolean
)

/** Отметка привычки за день: [date] — момент ISO в UTC, [maxCount] — цель того дня. */
data class OldHabitRecord(
    val habitUid: String,
    val count: Int,
    val maxCount: Int,
    val date: String
)

/** Одна «реализация» привычки в день; [notifyTime] «20:0» — время напоминания, null — без напоминания. */
data class OldImplementation(
    val habitUid: String,
    val notifyTime: String?
)
