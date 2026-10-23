package com.snax1k.chekki.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snax1k.chekki.data.db.ChekkiDatabase
import com.snax1k.chekki.data.db.HabitEntity
import com.snax1k.chekki.data.db.HabitIcon
import com.snax1k.chekki.data.db.InitialDataCallback
import com.snax1k.chekki.data.db.bit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate

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
