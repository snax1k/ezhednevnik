package com.snax1k.chekki.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snax1k.chekki.data.db.ChekkiDatabase
import com.snax1k.chekki.data.db.HabitEntity
import com.snax1k.chekki.data.db.HabitIcon
import com.snax1k.chekki.data.db.InitialDataCallback
import com.snax1k.chekki.data.db.bit
import com.snax1k.chekki.reminders.HabitReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Привычки на устройстве: база в памяти с тестовыми привычками отладочной сборки.
 * «Сегодня» — четверг 22 октября 2026, «Бассейн» по расписанию только в выходные.
 */
@RunWith(AndroidJUnit4::class)
class HabitRepositoryTest {

    private val today = LocalDate.of(2026, 10, 22)
    private lateinit var database: ChekkiDatabase
    private lateinit var repository: HabitRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, ChekkiDatabase::class.java)
            .addCallback(InitialDataCallback(today = { today }))
            .build()
        repository = HabitRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun habitsToday() = repository.observeHabitsOn(today).first()

    private suspend fun idOf(title: String) = habitsToday().single { it.habit.title == title }.habit.id

    @Test
    fun наСегодняПривычкиПоРасписаниюИПоПорядку() {
        runBlocking {
            assertEquals(
                listOf("Утренняя рутина", "Пить воду", "Английский", "Растяжка", "Витамины"),
                habitsToday().map { it.habit.title }
            )
            assertEquals(listOf(0, 2, 1, 0, 0), habitsToday().map { it.count })
            // В субботу появляется «Бассейн», а сегодняшние отметки воды туда не переходят
            val saturday = repository.observeHabitsOn(today.plusDays(2)).first()
            assertTrue(saturday.any { it.habit.title == "Бассейн" })
            assertEquals(0, saturday.single { it.habit.title == "Пить воду" }.count)
        }
    }

    @Test
    fun отметокНеБольшеЦели() {
        runBlocking {
            val english = idOf("Английский")
            assertEquals(2, repository.check(english, today))
            assertEquals(2, repository.check(english, today))
            assertEquals(2, habitsToday().single { it.habit.id == english }.count)
        }
    }

    @Test
    fun перваяОтметкаДняСоздаётСтрокуСЦельюДня() {
        runBlocking {
            val routine = idOf("Утренняя рутина")
            assertEquals(1, repository.check(routine, today))
            val check = database.habitDao().getCheck(routine, today)!!
            assertEquals(1, check.count)
            assertEquals(1, check.target)
        }
    }

    @Test
    fun новаяЦельМеняетСегодняИНеТрогаетПрошлое() {
        runBlocking {
            val water = idOf("Пить воду")
            repository.updateHabit(water, "Пить воду", HabitIcon.Water, 1, dailyTarget = 8, weekdays = HabitEntity.EVERY_DAY, today = today)
            assertEquals(8, database.habitDao().getCheck(water, today)!!.target)
            assertEquals(6, database.habitDao().getCheck(water, today.minusDays(1))!!.target)

            // Цель меньше отмеченного — сегодняшние отметки срезаются до неё
            repository.updateHabit(water, "Пить воду", HabitIcon.Water, 1, dailyTarget = 1, weekdays = HabitEntity.EVERY_DAY, today = today)
            assertEquals(1, database.habitDao().getCheck(water, today)!!.count)
        }
    }

    @Test
    fun сменаРасписанияУбираетПривычкуССегодня() {
        runBlocking {
            val stretch = idOf("Растяжка")
            repository.updateHabit(stretch, "Растяжка", HabitIcon.Stretch, 3, dailyTarget = 1, weekdays = DayOfWeek.MONDAY.bit, today = today)
            assertTrue(habitsToday().none { it.habit.id == stretch })
        }
    }

    @Test
    fun минусУбираетОтметкуАПоследняяУдаляетДень() {
        runBlocking {
            val english = idOf("Английский")
            assertEquals(0, repository.uncheck(english, today))
            assertNull(database.habitDao().getCheck(english, today))
            // Ниже нуля не уходит
            assertEquals(0, repository.uncheck(english, today))
        }
    }

    @Test
    fun прошлыйДеньОтмечаетсяСоСвоейЦелью() {
        runBlocking {
            val water = idOf("Пить воду")
            val yesterday = today.minusDays(1)
            repository.updateHabit(water, "Пить воду", HabitIcon.Water, 1, dailyTarget = 8, weekdays = HabitEntity.EVERY_DAY, today = today)
            // Вчера цель была 6 и выполнена — больше не отмечается
            assertEquals(6, repository.check(water, yesterday))
            assertEquals(5, repository.uncheck(water, yesterday))
            assertEquals(6, database.habitDao().getCheck(water, yesterday)!!.target)

            // Позавчера отметок не было — день получает нынешнюю цель
            assertEquals(1, repository.check(water, today.minusDays(2)))
            assertEquals(8, database.habitDao().getCheck(water, today.minusDays(2))!!.target)
        }
    }

    @Test
    fun историяОтметокПоПорядку() {
        runBlocking {
            val water = idOf("Пить воду")
            assertEquals(listOf(today.minusDays(1), today), repository.observeChecks(water).first().map { it.date })
        }
    }

    /** Записывает, на какое время репозиторий ставил будильник каждой привычки и что убирал. */
    private class RecordingScheduler : HabitReminderScheduler {
        val triggers = mutableMapOf<Long, Instant?>()
        val dismissed = mutableListOf<Long>()
        val cancelled = mutableListOf<Long>()
        override fun update(habitId: Long, trigger: Instant?) {
            triggers[habitId] = trigger
        }
        override fun cancel(habitId: Long) {
            cancelled += habitId
        }
        override fun dismiss(habitId: Long) {
            dismissed += habitId
        }
    }

    /** Репозиторий с часами, которые всегда показывают [time] сегодняшнего дня (UTC). */
    private fun repositoryAt(time: LocalTime, scheduler: HabitReminderScheduler) =
        HabitRepository(database, scheduler, Clock.fixed(today.atTime(time).toInstant(ZoneOffset.UTC), ZoneOffset.UTC))

    private fun at(date: LocalDate, hour: Int) = date.atTime(hour, 0).toInstant(ZoneOffset.UTC)

    @Test
    fun будильникНаБлижайшееНапоминание() {
        runBlocking {
            val scheduler = RecordingScheduler()
            val repository = repositoryAt(LocalTime.of(11, 0), scheduler)
            val water = idOf("Пить воду")
            repository.rescheduleAllReminders()
            // У воды напоминания в 10, 13, 16 и 19 — в 11:00 ближайшее 13:00
            assertEquals(mapOf(water to at(today, 13)), scheduler.triggers)
        }
    }

    @Test
    fun набраннаяЦельПереноситНапоминаниеНаЗавтраИУбираетУведомление() {
        runBlocking {
            val scheduler = RecordingScheduler()
            val repository = repositoryAt(LocalTime.of(11, 0), scheduler)
            val water = idOf("Пить воду")
            repeat(4) { repository.check(water, today) }
            assertEquals(at(today.plusDays(1), 10), scheduler.triggers[water])
            assertEquals(listOf(water), scheduler.dismissed)

            // Убрали одну отметку — сегодняшние напоминания вернулись
            repository.uncheck(water, today)
            assertEquals(at(today, 13), scheduler.triggers[water])
        }
    }

    @Test
    fun сработавшийБудильникПоказываетИСтавитСледующий() {
        runBlocking {
            val scheduler = RecordingScheduler()
            val repository = repositoryAt(LocalTime.of(13, 0), scheduler)
            val water = idOf("Пить воду")
            val shown = repository.reminderToShow(water, at(today, 13))!!
            assertEquals("Пить воду", shown.first.title)
            assertEquals(2, shown.second)
            assertEquals(at(today, 16), scheduler.triggers[water])

            // Время убрали из напоминаний, а старый будильник успел сработать — молчим
            repository.updateHabit(
                water, "Пить воду", HabitIcon.Water, 1, dailyTarget = 6, weekdays = HabitEntity.EVERY_DAY,
                reminderTimes = listOf(LocalTime.of(19, 0)), today = today
            )
            assertNull(repository.reminderToShow(water, at(today, 16)))
            assertEquals(at(today, 19), scheduler.triggers[water])
        }
    }

    @Test
    fun уведомлениеНеПриходитПриНабраннойЦели() {
        runBlocking {
            val repository = repositoryAt(LocalTime.of(16, 0), RecordingScheduler())
            val water = idOf("Пить воду")
            repeat(4) { repository.check(water, today) }
            assertNull(repository.reminderToShow(water, at(today, 16)))
        }
    }

    @Test
    fun удалениеСнимаетБудильникАОтменаВозвращаетНапоминания() {
        runBlocking {
            val scheduler = RecordingScheduler()
            val repository = repositoryAt(LocalTime.of(11, 0), scheduler)
            val water = idOf("Пить воду")
            val deleted = repository.deleteHabit(water)!!
            assertEquals(listOf(water), scheduler.cancelled)
            assertTrue(repository.getReminderTimes(water).isEmpty())

            repository.restoreHabit(deleted)
            assertEquals(4, repository.getReminderTimes(water).size)
            assertEquals(at(today, 13), scheduler.triggers[water])
        }
    }

    @Test
    fun всеПривычкиПоПорядкуСЧисломНапоминаний() {
        runBlocking {
            val all = repository.observeAllHabits().first()
            assertEquals(
                listOf("Утренняя рутина", "Пить воду", "Английский", "Растяжка", "Витамины", "Бассейн"),
                all.map { it.habit.title }
            )
            assertEquals(4, all.single { it.habit.title == "Пить воду" }.reminderCount)
        }
    }

    @Test
    fun новыйПорядокМеняетРядНаверху() {
        runBlocking {
            val ids = repository.observeAllHabits().first().map { it.habit.id }
            // «Растяжку» перетащили в начало
            repository.reorder(listOf(ids[3], ids[0], ids[1], ids[2], ids[4], ids[5]))
            assertEquals(
                listOf("Растяжка", "Утренняя рутина", "Пить воду", "Английский", "Витамины"),
                habitsToday().map { it.habit.title }
            )
        }
    }

    @Test
    fun архивУбираетСВерхуИСнимаетБудильникАВозвратСтавитВКонец() {
        runBlocking {
            val scheduler = RecordingScheduler()
            val repository = repositoryAt(LocalTime.of(11, 0), scheduler)
            val water = idOf("Пить воду")

            repository.setArchived(water, archived = true)
            assertTrue(habitsToday().none { it.habit.id == water })
            assertEquals(null, scheduler.triggers[water])
            assertEquals(listOf(water), scheduler.dismissed)
            // В списке «Все привычки» она в конце, отметки на месте
            assertEquals("Пить воду", repository.observeAllHabits().first().last().habit.title)
            assertEquals(2, database.habitDao().getCheck(water, today)!!.count)

            repository.setArchived(water, archived = false)
            assertEquals("Пить воду", habitsToday().last().habit.title)
            assertEquals(at(today, 13), scheduler.triggers[water])
        }
    }

    @Test
    fun новаяПривычкаВстаётВКонецРяда() {
        runBlocking {
            repository.addHabit("Медитация", HabitIcon.Heart, colorIndex = 6, dailyTarget = 1, weekdays = HabitEntity.EVERY_DAY)
            assertEquals("Медитация", habitsToday().last().habit.title)
        }
    }

    @Test
    fun удалениеИОтменаВозвращаютПривычкуСОтметками() {
        runBlocking {
            val water = idOf("Пить воду")
            val deleted = repository.deleteHabit(water)!!
            assertTrue(habitsToday().none { it.habit.id == water })
            assertNull(database.habitDao().getCheck(water, today))

            repository.restoreHabit(deleted)
            assertEquals(listOf("Утренняя рутина", "Пить воду"), habitsToday().take(2).map { it.habit.title })
            assertEquals(2, habitsToday().single { it.habit.id == water }.count)
            assertEquals(6, database.habitDao().getCheck(water, today.minusDays(1))!!.count)
        }
    }
}
