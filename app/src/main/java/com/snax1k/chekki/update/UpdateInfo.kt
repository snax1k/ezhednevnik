package com.snax1k.chekki.update

import java.security.MessageDigest
import java.time.Duration
import java.time.Instant

/**
 * Справка о выпуске — файл `update.json`, который лежит в каждом выпуске на GitHub рядом с APK.
 * Приложение читает его по постоянной ссылке на последний выпуск (`releases/latest/download/update.json`).
 */
data class UpdateInfo(
    /** Номер версии для Android (`versionCode`): обновление — только если он больше установленного. */
    val versionCode: Long,
    /** Номер для людей: «0.4». */
    val versionName: String,
    /** Откуда скачать APK. */
    val apkUrl: String,
    /** Контрольная сумма APK (SHA-256, шестнадцатеричная строка): файл скачался целиком и не подменён. */
    val sha256: String,
    /** Размер APK в байтах. */
    val size: Long,
    /** Самая старая версия Android, на которую ставится APK. */
    val minSdk: Int,
    /** «Что нового» — по пункту на строку. */
    val notes: List<String>
)

/** Справка годится: есть номер, ссылка по https (или http в отладочной сборке — до компьютера), правильная контрольная сумма. */
fun UpdateInfo.isWellFormed(allowHttp: Boolean = false): Boolean =
    versionCode > 0 &&
        versionName.isNotBlank() &&
        (apkUrl.startsWith("https://") || (allowHttp && apkUrl.startsWith("http://"))) &&
        isSha256Hex(sha256) &&
        size > 0

/** Есть смысл предлагать: версия новее установленной и подходит для этого Android. */
fun UpdateInfo.isUpdateFor(installedVersionCode: Long, sdkInt: Int): Boolean =
    versionCode > installedVersionCode && sdkInt >= minSdk

/** Как часто проверять обновления при открытии приложения. */
val UPDATE_CHECK_INTERVAL: Duration = Duration.ofHours(6)

/**
 * Пора ли проверять: прошло [interval] с прошлой попытки. Часы телефона могли перевести назад —
 * тогда прошлая попытка «в будущем», и проверяем сразу, иначе пришлось бы ждать.
 */
fun isCheckDue(lastAttempt: Instant?, now: Instant, interval: Duration = UPDATE_CHECK_INTERVAL): Boolean =
    lastAttempt == null || lastAttempt.isAfter(now) || !now.isBefore(lastAttempt.plus(interval))

/** 64 шестнадцатеричных знака — запись SHA-256. */
fun isSha256Hex(value: String): Boolean = value.length == 64 && value.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

/** Контрольная сумма прочитанного, строчными шестнадцатеричными знаками. */
fun MessageDigest.hex(): String = digest().joinToString("") { "%02x".format(it) }

/** Подписи совпадают: наборы сертификатов одинаковы (порядок не важен). */
fun sameSigners(installed: List<ByteArray>, candidate: List<ByteArray>): Boolean {
    if (installed.isEmpty() || candidate.isEmpty()) return false
    val a = installed.map { it.toList() }.toSet()
    val b = candidate.map { it.toList() }.toSet()
    return a == b
}
