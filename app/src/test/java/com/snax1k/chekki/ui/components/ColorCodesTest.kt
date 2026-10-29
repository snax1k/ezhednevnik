package com.snax1k.chekki.ui.components

import androidx.compose.ui.graphics.Color
import com.snax1k.chekki.ui.category.customCategoryColorCode
import com.snax1k.chekki.ui.category.isCustomCategoryColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorCodesTest {

    @Test
    fun hexВЦветИОбратно() {
        assertEquals(Color(0xFF7CC2FF), parseHex("#7CC2FF"))
        assertEquals(Color(0xFF7CC2FF), parseHex("7cc2ff"))
        assertEquals(Color(0xFF77CCFF), parseHex("#7CF"))
        assertEquals("#7CC2FF", colorToHex(Color(0xFF7CC2FF)))
    }

    @Test
    fun неЦвет() {
        assertNull(parseHex(""))
        assertNull(parseHex("#12345"))
        assertNull(parseHex("#GGGGGG"))
    }

    @Test
    fun свойЦветКатегорииОтрицательный_аНомерПалитрыНет() {
        val code = customCategoryColorCode(Color(0xFFFFA322))
        assertTrue(isCustomCategoryColor(code))
        assertEquals(Color(0xFFFFA322), Color(code))
        // Даже полупрозрачный цвет записывается непрозрачным
        assertEquals(Color(0xFF000000), Color(customCategoryColorCode(Color(0x00000000))))
        (0..11).forEach { assertFalse(isCustomCategoryColor(it)) }
    }
}
