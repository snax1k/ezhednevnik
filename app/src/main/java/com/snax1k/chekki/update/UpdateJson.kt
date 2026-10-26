package com.snax1k.chekki.update

import org.json.JSONException
import org.json.JSONObject

/**
 * Разбор `update.json`. Пример:
 * ```
 * {
 *   "versionCode": 4,
 *   "versionName": "0.4",
 *   "apk": "https://github.com/snax1k/ezhednevnik/releases/download/v0.4/Ezhednevnik-0.4.apk",
 *   "sha256": "6a1647c6…",
 *   "size": 2556766,
 *   "minSdk": 26,
 *   "notes": ["Кружок задачи в цвете категории", "…"]
 * }
 * ```
 * Файл пишет скрипт выпуска `tools/release.py`. Незнакомые поля пропускаются: так в справку можно добавлять
 * новое, не ломая старые версии приложения. Испорченный файл — null.
 */
fun parseUpdateInfo(text: String): UpdateInfo? = try {
    val json = JSONObject(text)
    val notes = json.optJSONArray("notes")
    UpdateInfo(
        versionCode = json.getLong("versionCode"),
        versionName = json.getString("versionName"),
        apkUrl = json.getString("apk"),
        sha256 = json.getString("sha256").lowercase(),
        size = json.getLong("size"),
        minSdk = json.optInt("minSdk", 1),
        notes = if (notes == null) emptyList() else List(notes.length()) { notes.getString(it) }.filter { it.isNotBlank() }
    )
} catch (e: JSONException) {
    null
}
