package com.snax1k.chekki.ui.main

import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.tasks.CategoryUi
import com.snax1k.chekki.ui.tasks.Priority
import com.snax1k.chekki.ui.tasks.TaskUi
import java.time.LocalDate
import java.time.LocalTime

// Тестовые данные по макету. Уйдут, когда появится база задач.

private val Work = CategoryUi(name = "Работа", colorIndex = 0)
private val Personal = CategoryUi(name = "Личное", colorIndex = 1)
private val Home = CategoryUi(name = "Дом", colorIndex = 2)
private val Health = CategoryUi(name = "Здоровье", colorIndex = 3)
private val Study = CategoryUi(name = "Учёба", colorIndex = 4)
private val Finance = CategoryUi(name = "Финансы", colorIndex = 5)

/** Задачи на сегодня из макета экрана «Сегодня». */
fun sampleTodayTasks(): List<TaskUi> = listOf(
    TaskUi(
        id = 1, title = "Утренняя пробежка", time = LocalTime.of(7, 30), category = Health,
        priority = null, repeats = true, done = true
    ),
    TaskUi(
        id = 2, title = "Созвон с командой", time = LocalTime.of(10, 30), category = Work,
        priority = Priority.High, repeats = true, hasReminder = true
    ),
    TaskUi(
        id = 3, title = "Отправить отчёт за квартал", time = LocalTime.of(14, 0), category = Work,
        priority = Priority.Medium, hasReminder = true
    ),
    TaskUi(
        id = 4, title = "Купить продукты", time = LocalTime.of(18, 0), category = Home,
        priority = null, subtasksDone = 2, subtasksTotal = 5
    ),
    TaskUi(
        id = 5, title = "Позвонить маме", time = LocalTime.of(19, 0), category = Personal,
        priority = null, repeats = true, hasReminder = true
    ),
    TaskUi(
        id = 6, title = "Прочитать 30 страниц", time = LocalTime.of(21, 0), category = Study,
        priority = Priority.Low, repeats = true
    ),
    TaskUi(
        id = 7, title = "Оплатить коммуналку", time = null, category = Finance,
        priority = Priority.High, hasReminder = true
    ),
    TaskUi(
        id = 8, title = "Полить цветы", time = null, category = Home,
        priority = null, repeats = true
    )
)

/**
 * Трекеры верхней панели. Пока тестовые и в базе не хранятся: набор трекеров не утверждён.
 * В макете у всех 0, здесь часть заполнена, чтобы было видно кольца.
 */
fun sampleTrackers(): List<TrackerUi> = listOf(
    TrackerUi(name = "Привычки", icon = ChekkiIcons.Habits, done = 0, total = 1),
    TrackerUi(name = "Вода", icon = ChekkiIcons.Water, done = 2, total = 6),
    TrackerUi(name = "Цели", icon = ChekkiIcons.Goals, done = 1, total = 2),
    TrackerUi(name = "Растяжка", icon = ChekkiIcons.Stretching, done = 0, total = 1)
)

/** Главный экран для превью. Карточка «Сегодня» считается по [todayTasks], остальные счётчики из макета. */
fun sampleMainUiState(
    todayTasks: List<TaskUi> = sampleTodayTasks(),
    date: LocalDate = LocalDate.now()
) = MainUiState(
    date = date,
    trackers = sampleTrackers(),
    noDateCount = 20,
    overdueCount = 25,
    overdueTitles = listOf("Оплатить интернет", "Записаться к врачу"),
    todayDone = todayTasks.count { it.done },
    todayTotal = todayTasks.size,
    todayUpcoming = todayTasks
        .filter { !it.done }
        .take(2)
        .map { TaskPreviewUi(title = it.title, time = it.time) },
    tomorrowCount = 5,
    thisWeekCount = 11,
    thisMonthCount = 13,
    laterCount = 50
)
