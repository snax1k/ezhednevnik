package com.snax1k.chekki.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snax1k.chekki.data.db.ChekkiDatabase
import com.snax1k.chekki.data.db.GroupCounts
import com.snax1k.chekki.data.db.InitialDataCallback
import com.snax1k.chekki.data.db.RepeatRule
import com.snax1k.chekki.data.db.RepeatUnit
import com.snax1k.chekki.data.db.TaskEntity
import com.snax1k.chekki.data.db.TaskPriority
import com.snax1k.chekki.reminders.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

/**
 * Проверка базы на устройстве: база в памяти с начальными данными отладочной сборки.
 * «Сегодня» — четверг 22 октября 2026: завтра 23.10, неделя 24–25.10, месяц 26–31.10, потом с 1.11.
 */
@RunWith(AndroidJUnit4::class)
class TaskRepositoryTest {

    private val today = LocalDate.of(2026, 10, 22)
    private lateinit var database: ChekkiDatabase
    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, ChekkiDatabase::class.java)
            .addCallback(InitialDataCallback(today = { today }))
            .build()
        repository = TaskRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun категорииСоздаютсяВместеСБазой() {
        val names = database.openHelper.readableDatabase
            .query("SELECT name FROM categories ORDER BY position")
            .use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }
        assertEquals(listOf("Работа", "Личное", "Дом", "Здоровье", "Учёба", "Финансы"), names)
    }

    @Test
    fun задачиНаСегодняИдутПоВремениАВесьДеньВКонце() {
        runBlocking {
            val tasks = repository.observeTasksOn(today).first()
            assertEquals(
                listOf(
                    "Утренняя пробежка", "Созвон с командой", "Отправить отчёт за квартал", "Купить продукты",
                    "Позвонить маме", "Прочитать 30 страниц", "Оплатить коммуналку", "Полить цветы"
                ),
                tasks.map { it.task.title }
            )
            assertEquals(1, tasks.count { it.task.isDone })
            val shopping = tasks.single { it.task.title == "Купить продукты" }
            assertEquals(2, shopping.subtasksDone)
            assertEquals(5, shopping.subtasksTotal)
            assertEquals("Дом", shopping.categoryName)
            assertEquals(2, shopping.categoryColorIndex)
        }
    }

    @Test
    fun счётчикиГрупп() {
        runBlocking {
            assertEquals(
                GroupCounts(noDate = 3, overdue = 3, tomorrow = 3, thisWeek = 1, thisMonth = 1, later = 4),
                repository.observeGroupCounts(today).first()
            )
        }
    }

    @Test
    fun просроченныеСамыеСтарыеСверху() {
        runBlocking {
            assertEquals(
                listOf("Вернуть книгу в библиотеку", "Оплатить интернет"),
                repository.observeOverdueTitles(today, limit = 2).first()
            )
        }
    }

    @Test
    fun задачиНаЗавтраИдутПоВремени() {
        runBlocking {
            val tomorrow = today.plusDays(1)
            assertEquals(
                listOf("Утренняя пробежка", "Стоматолог", "Забрать посылку"),
                repository.observeOpenTasksBetween(tomorrow..tomorrow).first().map { it.task.title }
            )
        }
    }

    @Test
    fun отмеченнаяНаЭкранеЗадачаОстаётсяВСпискеПокаЕёДержат() {
        runBlocking {
            val tomorrow = today.plusDays(1)
            val dentist = repository.observeOpenTasksBetween(tomorrow..tomorrow).first()
                .single { it.task.title == "Стоматолог" }
            repository.toggleDone(dentist.task.id, today = today)

            val kept = repository.observeOpenTasksBetween(tomorrow..tomorrow, keepIds = setOf(dentist.task.id)).first()
            assertTrue(kept.single { it.task.id == dentist.task.id }.task.isDone)
            val fresh = repository.observeOpenTasksBetween(tomorrow..tomorrow).first()
            assertTrue(fresh.none { it.task.id == dentist.task.id })
        }
    }

    @Test
    fun задачиБезДатыНовыеСверху() {
        runBlocking {
            assertEquals(
                listOf("Записаться на курсы английского", "Выбрать подарок Ане", "Разобрать антресоль"),
                repository.observeOpenTasksWithoutDate().first().map { it.task.title }
            )
        }
    }

    @Test
    fun выполнениеПовтораСоздаётКопиюАСнятиеОтметкиЕёУбирает() {
        runBlocking {
            val call = repository.observeTasksOn(today).first().single { it.task.title == "Позвонить маме" }

            repository.toggleDone(call.task.id, today = today)
            assertTrue(repository.observeTasksOn(today).first().single { it.task.id == call.task.id }.task.isDone)
            val copy = repository.observeTasksOn(today.plusWeeks(1)).first().single { it.task.title == "Позвонить маме" }
            assertFalse(copy.task.isDone)
            assertEquals(call.task.id, copy.task.repeatedFromId)

            repository.toggleDone(call.task.id, today = today)
            assertFalse(repository.observeTasksOn(today).first().single { it.task.id == call.task.id }.task.isDone)
            assertTrue(repository.observeTasksOn(today.plusWeeks(1)).first().none { it.task.title == "Позвонить маме" })
        }
    }

    @Test
    fun копияПовтораПолучаетНеотмеченныеПодзадачи() {
        runBlocking {
            val report = repository.observeTasksOn(today).first().single { it.task.title == "Отправить отчёт за квартал" }
            repository.toggleDone(report.task.id, today = today)
            val copy = repository.observeTasksOn(today.plusMonths(3)).first()
                .single { it.task.title == "Отправить отчёт за квартал" }
            assertEquals(0, copy.subtasksDone)
            assertEquals(3, copy.subtasksTotal)
        }
    }

    @Test
    fun обычнаяЗадачаВыполняетсяБезКопии() {
        runBlocking {
            val before = countTasks()
            val shopping = repository.observeTasksOn(today).first().single { it.task.title == "Купить продукты" }
            repository.toggleDone(shopping.task.id, today = today)
            assertTrue(repository.observeTasksOn(today).first().single { it.task.id == shopping.task.id }.task.isDone)
            assertEquals(before, countTasks())
            // Счётчик «Сегодня» на главном считается по списку дня, а группы — только по невыполненным
            assertEquals(3, repository.observeGroupCounts(today).first().overdue)
        }
    }

    @Test
    fun категорииПриходятВПорядкеСписка() {
        runBlocking {
            val categories = repository.observeCategories().first()
            assertEquals(listOf("Работа", "Личное", "Дом", "Здоровье", "Учёба", "Финансы"), categories.map { it.name })
            assertEquals((0..5).toList(), categories.map { it.colorIndex })
        }
    }

    @Test
    fun новаяЗадачаПоявляетсяВСпискеДняИВСчётчиках() {
        runBlocking {
            val countsBefore = repository.observeGroupCounts(today).first()
            val workId = repository.observeCategories().first().first { it.name == "Работа" }.id

            repository.addTask(
                title = "Купить подарок",
                note = "Что-нибудь к чаю",
                date = today,
                time = null,
                categoryId = workId,
                priority = TaskPriority.High
            )
            repository.addTask(
                title = "Разобрать почту",
                note = "",
                date = null,
                time = null,
                categoryId = null,
                priority = TaskPriority.None
            )

            val added = repository.observeTasksOn(today).first().last()
            assertEquals("Купить подарок", added.task.title)
            assertEquals("Что-нибудь к чаю", added.task.note)
            assertEquals(null, added.task.time)
            assertEquals("Работа", added.categoryName)
            assertEquals(TaskPriority.High, added.task.priority)
            assertEquals(countsBefore.noDate + 1, repository.observeGroupCounts(today).first().noDate)
        }
    }

    @Test
    fun задачаСоВременемВстаётВСписокПоВремени() {
        runBlocking {
            repository.addTask(
                title = "Обед с Олей",
                note = "",
                date = today,
                time = LocalTime.of(12, 0),
                categoryId = null,
                priority = TaskPriority.None
            )
            val titles = repository.observeTasksOn(today).first().map { it.task.title }
            // Между созвоном в 10:30 и отчётом в 14:00
            assertEquals(titles.indexOf("Созвон с командой") + 1, titles.indexOf("Обед с Олей"))
            assertEquals(titles.indexOf("Обед с Олей") + 1, titles.indexOf("Отправить отчёт за квартал"))
        }
    }

    @Test
    fun новаяЗадачаСохраняетсяСПодзадачамиПоПорядку() {
        runBlocking {
            val taskId = repository.addTask(
                title = "Собрать рюкзак",
                note = "",
                date = today,
                time = null,
                categoryId = null,
                priority = TaskPriority.None,
                subtasks = listOf("Палатка", "Спальник", "Фонарик")
            )
            val added = repository.observeTasksOn(today).first().single { it.task.id == taskId }
            assertEquals(0, added.subtasksDone)
            assertEquals(3, added.subtasksTotal)
            val titles = database.openHelper.readableDatabase
                .query("SELECT title FROM subtasks WHERE task_id = $taskId ORDER BY position")
                .use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }
            assertEquals(listOf("Палатка", "Спальник", "Фонарик"), titles)
        }
    }

    @Test
    fun новаяПовторяющаясяЗадачаСНапоминаниемРаботаетКакОстальные() {
        runBlocking {
            val weekly = RepeatRule(RepeatUnit.Week, every = 1, weekdays = 0)
            val taskId = repository.addTask(
                title = "Тренировка",
                note = "",
                date = today,
                time = LocalTime.of(20, 0),
                categoryId = null,
                priority = TaskPriority.None,
                repeat = weekly,
                reminderMinutesBefore = 30
            )
            val added = repository.observeTasksOn(today).first().single { it.task.id == taskId }
            assertEquals(weekly, added.task.repeat)
            assertEquals(30, added.task.reminderMinutesBefore)

            // Выполнение создаёт копию через неделю с тем же повтором и напоминанием
            repository.toggleDone(taskId, today = today)
            val copy = repository.observeTasksOn(today.plusWeeks(1)).first().single { it.task.title == "Тренировка" }
            assertEquals(weekly, copy.task.repeat)
            assertEquals(30, copy.task.reminderMinutesBefore)
        }
    }

    /** Записывает, какие будильники репозиторий просил поставить и снять. */
    private class RecordingScheduler : ReminderScheduler {
        val updated = mutableListOf<TaskEntity>()
        val cancelled = mutableListOf<Long>()
        override fun update(task: TaskEntity) {
            updated += task
        }
        override fun cancel(taskId: Long) {
            cancelled += taskId
        }
    }

    @Test
    fun будильникСтавитсяДляНовойЗадачи() {
        runBlocking {
            val scheduler = RecordingScheduler()
            val repository = TaskRepository(database, scheduler)
            val taskId = repository.addTask(
                title = "Тренировка",
                note = "",
                date = today,
                time = LocalTime.of(20, 0),
                categoryId = null,
                priority = TaskPriority.None,
                reminderMinutesBefore = 30
            )
            val scheduled = scheduler.updated.single()
            assertEquals(taskId, scheduled.id)
            assertEquals(30, scheduled.reminderMinutesBefore)
        }
    }

    @Test
    fun выполнениеСнимаетБудильникИСтавитЕгоКопии() {
        runBlocking {
            val scheduler = RecordingScheduler()
            val repository = TaskRepository(database, scheduler)
            val call = repository.observeTasksOn(today).first().single { it.task.title == "Позвонить маме" }

            repository.toggleDone(call.task.id, today = today)
            assertEquals(listOf(call.task.id), scheduler.cancelled)
            val copy = scheduler.updated.single()
            assertEquals(call.task.id, copy.repeatedFromId)
            assertEquals(today.plusWeeks(1), copy.date)

            // Отметку сняли: будильник копии снимается, исходной задачи — ставится снова
            scheduler.updated.clear()
            scheduler.cancelled.clear()
            repository.toggleDone(call.task.id, today = today)
            assertEquals(listOf(copy.id), scheduler.cancelled)
            assertEquals(listOf(call.task.id), scheduler.updated.map { it.id })
            assertFalse(scheduler.updated.single().isDone)
        }
    }

    @Test
    fun перепланированиеБерётБудущиеНевыполненныеНапоминания() {
        runBlocking {
            val scheduler = RecordingScheduler()
            TaskRepository(database, scheduler).rescheduleAllReminders(today)
            assertEquals(
                setOf(
                    "Созвон с командой", "Отправить отчёт за квартал", "Позвонить маме",
                    "Оплатить коммуналку", "Стоматолог", "День рождения Пети"
                ),
                scheduler.updated.map { it.title }.toSet()
            )
        }
    }

    private fun countTasks(): Int =
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM tasks").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
}
