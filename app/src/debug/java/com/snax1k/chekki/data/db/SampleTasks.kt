package com.snax1k.chekki.data.db

import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Тестовые задачи для отладочной сборки, пока нет экрана добавления. Даты считаются от сегодняшнего дня,
 * чтобы были заполнены все группы. Чтобы получить их заново, очистите данные приложения.
 * В выпускной сборке вместо этого файла пустой из src/release.
 */
internal object SampleTasks {

    // id категорий из DefaultCategories
    private const val WORK = 1L
    private const val PERSONAL = 2L
    private const val HOME = 3L
    private const val HEALTH = 4L
    private const val STUDY = 5L
    private const val FINANCE = 6L

    private val daily = RepeatRule(RepeatUnit.Day, every = 1, weekdays = 0)
    private val weekly = RepeatRule(RepeatUnit.Week, every = 1, weekdays = 0)
    private val monthly = RepeatRule(RepeatUnit.Month, every = 1, weekdays = 0)
    private val quarterly = RepeatRule(RepeatUnit.Month, every = 3, weekdays = 0)

    fun insert(db: SupportSQLiteDatabase, today: LocalDate) {
        val writer = Writer(db)
        with(writer) {
            // Сегодня — как в макете экрана «Сегодня»
            val run = task(
                "Утренняя пробежка", today, LocalTime.of(7, 30), HEALTH,
                repeat = daily, done = true
            )
            task(
                "Созвон с командой", today, LocalTime.of(10, 30), WORK,
                priority = TaskPriority.High, repeat = weekly, reminder = 15
            )
            task(
                "Отправить отчёт за квартал", today, LocalTime.of(14, 0), WORK,
                priority = TaskPriority.Medium, repeat = quarterly, reminder = 30,
                note = "Свести продажи из таблицы, приложить графики и отправить Ольге до конца дня.",
                subtasks = listOf(
                    "Выгрузить данные о продажах" to true,
                    "Построить графики" to false,
                    "Написать короткий вывод" to false
                )
            )
            task(
                "Купить продукты", today, LocalTime.of(18, 0), HOME,
                subtasks = listOf(
                    "Молоко" to true, "Хлеб" to true, "Яйца" to false, "Сыр" to false, "Яблоки" to false
                )
            )
            task("Позвонить маме", today, LocalTime.of(19, 0), PERSONAL, repeat = weekly, reminder = 10)
            task(
                "Прочитать 30 страниц", today, LocalTime.of(21, 0), STUDY,
                priority = TaskPriority.Low, repeat = daily
            )
            task("Оплатить коммуналку", today, null, FINANCE, priority = TaskPriority.High, reminder = 0)
            task(
                "Полить цветы", today, null, HOME,
                repeat = RepeatRule(RepeatUnit.Day, every = 3, weekdays = 0)
            )

            // Копия пробежки на завтра: её создало выполнение сегодняшней
            task(
                "Утренняя пробежка", today.plusDays(1), LocalTime.of(7, 30), HEALTH,
                repeat = daily, repeatedFromId = run
            )
            task("Стоматолог", today.plusDays(1), LocalTime.of(9, 0), HEALTH, reminder = 60)
            task("Забрать посылку", today.plusDays(1), LocalTime.of(18, 30), PERSONAL)

            // Просроченные
            task("Оплатить интернет", today.minusDays(2), null, FINANCE, priority = TaskPriority.High)
            task("Записаться к врачу", today.minusDays(1), null, HEALTH)
            task("Вернуть книгу в библиотеку", today.minusDays(5), null, STUDY)

            // Без даты
            task("Разобрать антресоль", null, null, HOME)
            task("Выбрать подарок Ане", null, null, PERSONAL)
            task("Записаться на курсы английского", null, null, STUDY)

            // Дальше: в какие группы попадут, зависит от дня недели и месяца
            task(
                "Подготовить презентацию", today.plusDays(3), LocalTime.of(12, 0), WORK,
                priority = TaskPriority.Medium,
                subtasks = listOf("Собрать цифры из отчёта" to false, "Сделать 10 слайдов" to false)
            )
            task("День рождения Пети", today.plusDays(5), null, PERSONAL, reminder = 0)
            task("Поменять масло в машине", today.plusDays(12), null, HOME)
            task("Сдать показания счётчиков", today.plusDays(20), null, FINANCE, repeat = monthly)
            task("Забронировать отель на отпуск", today.plusDays(40), null, PERSONAL)
            task("Продлить страховку", today.plusDays(90), null, FINANCE, priority = TaskPriority.Medium)

            // Выполненное раньше
            task("Отправить резюме", today.minusDays(1), null, WORK, done = true)
        }
    }

    private class Writer(private val db: SupportSQLiteDatabase) {
        private val converters = Converters()
        private var createdAt = Instant.now()

        fun task(
            title: String,
            date: LocalDate?,
            time: LocalTime?,
            categoryId: Long?,
            priority: TaskPriority = TaskPriority.None,
            repeat: RepeatRule = RepeatRule.None,
            reminder: Int? = null,
            note: String = "",
            done: Boolean = false,
            repeatedFromId: Long? = null,
            subtasks: List<Pair<String, Boolean>> = emptyList()
        ): Long {
            // Разное время создания, чтобы порядок задач с одинаковым временем был предсказуемым
            createdAt = createdAt.plusMillis(1)
            val id = db.insert(
                "tasks",
                SQLiteDatabase.CONFLICT_ABORT,
                contentValuesOf(
                    "title" to title,
                    "note" to note,
                    "date" to date?.let(converters::localDateToEpochDay),
                    "time" to time?.let(converters::localTimeToMinutes),
                    "category_id" to categoryId,
                    "priority" to converters.priorityToCode(priority),
                    "repeat_unit" to converters.repeatUnitToCode(repeat.unit),
                    "repeat_every" to repeat.every,
                    "repeat_weekdays" to repeat.weekdays,
                    "reminder_minutes_before" to reminder,
                    "is_done" to done,
                    "completed_at" to if (done) converters.instantToMillis(createdAt) else null,
                    "created_at" to converters.instantToMillis(createdAt),
                    "repeated_from_id" to repeatedFromId
                )
            )
            subtasks.forEachIndexed { index, (subtaskTitle, subtaskDone) ->
                db.insert(
                    "subtasks",
                    SQLiteDatabase.CONFLICT_ABORT,
                    contentValuesOf(
                        "task_id" to id,
                        "title" to subtaskTitle,
                        "is_done" to subtaskDone,
                        "position" to index
                    )
                )
            }
            return id
        }
    }
}
