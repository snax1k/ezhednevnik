package com.snax1k.chekki.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant

class UpdateInfoTest {

    private val info = UpdateInfo(
        versionCode = 5,
        versionName = "0.5",
        apkUrl = "https://github.com/snax1k/ezhednevnik/releases/download/v0.5/Ezhednevnik-0.5.apk",
        sha256 = "ab".repeat(32),
        size = 2_600_000,
        minSdk = 26,
        notes = listOf("Что-то новое")
    )

    @Test
    fun предлагаетсяТолькоВерсияНовееУстановленной() {
        assertTrue(info.isUpdateFor(installedVersionCode = 4, sdkInt = 34))
        assertFalse(info.isUpdateFor(installedVersionCode = 5, sdkInt = 34))
        assertFalse(info.isUpdateFor(installedVersionCode = 6, sdkInt = 34))
    }

    @Test
    fun неПредлагаетсяНаСлишкомСтаромAndroid() {
        assertFalse(info.copy(minSdk = 30).isUpdateFor(installedVersionCode = 4, sdkInt = 29))
        assertTrue(info.copy(minSdk = 30).isUpdateFor(installedVersionCode = 4, sdkInt = 30))
    }

    @Test
    fun справкаПроверяется() {
        assertTrue(info.isWellFormed())
        assertFalse(info.copy(apkUrl = "http://example.com/a.apk").isWellFormed())
        assertTrue(info.copy(apkUrl = "http://localhost:8765/a.apk").isWellFormed(allowHttp = true))
        assertFalse(info.copy(apkUrl = "ftp://example.com/a.apk").isWellFormed(allowHttp = true))
        assertFalse(info.copy(sha256 = "abc").isWellFormed())
        assertFalse(info.copy(sha256 = "zz".repeat(32)).isWellFormed())
        assertFalse(info.copy(size = 0).isWellFormed())
        assertFalse(info.copy(versionCode = 0).isWellFormed())
        assertFalse(info.copy(versionName = " ").isWellFormed())
    }

    @Test
    fun проверкаНеЧащеРазаВШестьЧасов() {
        val now = Instant.parse("2026-10-27T12:00:00Z")
        assertTrue(isCheckDue(lastAttempt = null, now = now))
        assertFalse(isCheckDue(now.minus(Duration.ofHours(5)), now))
        assertTrue(isCheckDue(now.minus(Duration.ofHours(6)), now))
        assertTrue(isCheckDue(now.minus(Duration.ofDays(3)), now))
    }

    @Test
    fun часыПеревелиНазад_проверяемСразу() {
        val now = Instant.parse("2026-10-27T12:00:00Z")
        assertTrue(isCheckDue(now.plus(Duration.ofHours(2)), now))
    }

    @Test
    fun контрольнаяСуммаСтрочнымиЗнаками() {
        val digest = MessageDigest.getInstance("SHA-256").apply { update("abc".toByteArray()) }
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", digest.hex())
        assertTrue(isSha256Hex("BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD"))
    }

    @Test
    fun подписиСравниваютсяПоСодержимомуБезПорядка() {
        val a = byteArrayOf(1, 2, 3)
        val b = byteArrayOf(4, 5)
        assertTrue(sameSigners(listOf(a, b), listOf(b.copyOf(), a.copyOf())))
        assertFalse(sameSigners(listOf(a), listOf(b)))
        assertFalse(sameSigners(listOf(a), listOf(a, b)))
        assertFalse(sameSigners(emptyList(), emptyList()))
    }
}
