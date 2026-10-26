package com.snax1k.chekki.update

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Разбор `update.json` — на эмуляторе, потому что `org.json` настоящий только в Android. */
@RunWith(AndroidJUnit4::class)
class UpdateJsonTest {

    @Test
    fun справкаРазбирается() {
        val info = parseUpdateInfo(
            """
            {
              "versionCode": 4,
              "versionName": "0.4",
              "apk": "https://github.com/snax1k/ezhednevnik/releases/download/v0.4/Ezhednevnik-0.4.apk",
              "sha256": "6A1647C6D41ACA508F49D0CF94F075BA59B7CD2BA93BBECAF3E304AB5175248A",
              "size": 2556766,
              "minSdk": 26,
              "notes": ["Кружок задачи в цвете категории", "", "Обновление из приложения"],
              "newField": {"later": true}
            }
            """.trimIndent()
        )!!
        assertEquals(4L, info.versionCode)
        assertEquals("0.4", info.versionName)
        assertEquals("https://github.com/snax1k/ezhednevnik/releases/download/v0.4/Ezhednevnik-0.4.apk", info.apkUrl)
        // Контрольная сумма приводится к строчным знакам, как её считает приложение
        assertEquals("6a1647c6d41aca508f49d0cf94f075ba59b7cd2ba93bbecaf3e304ab5175248a", info.sha256)
        assertEquals(2556766L, info.size)
        assertEquals(26, info.minSdk)
        // Пустые пункты пропускаются, незнакомые поля не мешают
        assertEquals(listOf("Кружок задачи в цвете категории", "Обновление из приложения"), info.notes)
    }

    @Test
    fun безЧтоНовогоИMinSdk() {
        val sha = "a".repeat(64)
        val info = parseUpdateInfo(
            """{"versionCode": 5, "versionName": "0.5", "apk": "https://x/y.apk", "sha256": "$sha", "size": 1}"""
        )!!
        assertEquals(emptyList<String>(), info.notes)
        assertEquals(1, info.minSdk)
    }

    @Test
    fun испорченнаяСправка() {
        assertNull(parseUpdateInfo("<html>Not found</html>"))
        assertNull(parseUpdateInfo("""{"versionName": "0.5"}"""))
        assertNull(parseUpdateInfo(""))
    }
}
