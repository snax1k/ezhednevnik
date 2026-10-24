package com.snax1k.chekki.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskSearchTest {

    private fun match(query: String, title: String, note: String = "", subtasks: List<String> = emptyList()) =
        matchTask(searchWords(query), title, note, subtasks)

    @Test
    fun безУчётаРегистраИЁ() {
        val found = match("ОТЧЕТ", "Отправить отчёт за квартал")!!
        assertEquals(10 until 15, found.titleRange)
        assertNotNull(match("елку", "Купить ёлку"))
    }

    @Test
    fun всеСловаЗапросаДолжныНайтись() {
        assertNotNull(match("отчёт квартал", "Отправить отчёт за квартал"))
        assertNull(match("отчёт год", "Отправить отчёт за квартал"))
        // Слова могут быть в разных местах: одно в названии, другое в заметке
        assertNotNull(match("отчёт бухгалтер", "Отправить отчёт", note = "Копию — бухгалтеру"))
    }

    @Test
    fun вЗаметкеПоказываетсяКусокСтроки() {
        val found = match("паспорт", "Записаться в МФЦ", note = "Взять с собой:\nпаспорт и СНИЛС\nкопии")!!
        assertNull(found.titleRange)
        assertEquals("паспорт и СНИЛС", found.snippet)
    }

    @Test
    fun длиннаяСтрокаЗаметкиОбрезается() {
        val note = "а".repeat(100) + " паспорт " + "б".repeat(100)
        val snippet = match("паспорт", "Задача", note = note)!!.snippet!!
        assertTrue(snippet.startsWith("…") && snippet.endsWith("…"))
        assertTrue("паспорт" in snippet)
    }

    @Test
    fun вПодзадаче() {
        val found = match("молоко", "Купить продукты", subtasks = listOf("Хлеб", "Молоко 2 л"))!!
        assertEquals("Молоко 2 л", found.snippet)
        assertTrue(found.snippetIsSubtask)
    }

    @Test
    fun пустойЗапросПодходитВсем() {
        assertEquals(SearchMatch(), match("  ", "Что угодно"))
    }
}
