package com.snax1k.chekki.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.edit
import androidx.core.content.pm.PackageInfoCompat
import com.snax1k.chekki.R
import com.snax1k.chekki.data.db.ChekkiDatabase
import com.snax1k.chekki.data.db.DatabaseBackups
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.Instant

/** Что показывает плашка обновления на главном экране. */
sealed interface UpdateState {
    /** Плашки нет: обновлений нет, ещё не проверяли или пользователь нажал «Позже». */
    data object Idle : UpdateState

    data class Available(val info: UpdateInfo) : UpdateState

    /** [progress] — от 0 до 1. */
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateState

    /** Файл скачан и проверен, но Android ещё не разрешил приложению ставить обновления. */
    data class NeedsPermission(val info: UpdateInfo) : UpdateState

    /** Открыто окно Android «Обновить приложение?». */
    data class Installing(val info: UpdateInfo) : UpdateState

    /** Окно Android закрыли без установки: файл готов, можно нажать ещё раз. */
    data class ReadyToInstall(val info: UpdateInfo) : UpdateState

    data class Failed(val info: UpdateInfo, val error: UpdateError) : UpdateState

    /** Первый запуск после обновления из приложения: «Обновлено до 0.4» и что нового. */
    data class Updated(val versionName: String, val notes: List<String>) : UpdateState
}

enum class UpdateError {
    /** Не скачалось: нет связи, GitHub не ответил. */
    Network,

    /** Скачалось не то: размер или контрольная сумма не сходятся. */
    Corrupted,

    /** В файле другое приложение, не новее установленного или с чужой подписью. */
    WrongApp,

    /** Android не поставил обновление. */
    InstallFailed
}

/** Итог проверки вручную из меню ☰. */
enum class ManualCheck { Idle, Checking, UpToDate, Failed }

/**
 * Обновление приложения из выпусков на GitHub (решено 26.10.2026).
 *
 * При открытии приложения, не чаще раза в [UPDATE_CHECK_INTERVAL], читается справка `update.json` по ссылке
 * `R.string.update_url`. Есть версия новее — на главном экране плашка. «Обновить»: APK скачивается во временную
 * папку, проверяются размер, контрольная сумма, имя пакета, номер версии и подпись (тот же ключ, что у
 * установленного приложения), сохраняется копия базы ([DatabaseBackups]) и открывается окно Android
 * «Обновить приложение?». Обновление ставится поверх, данные остаются.
 *
 * Фоновой работы нет: всё происходит, только пока приложение открыто.
 */
class UpdateManager(
    context: Context,
    private val scope: CoroutineScope,
    private val database: () -> ChekkiDatabase
) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val updateUrl = appContext.getString(R.string.update_url)

    /** Отладочной сборке можно ходить на компьютер по http: так проверяется обновление на эмуляторе. */
    private val isDebuggable = appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** Последняя найденная версия новее установленной, даже если плашку скрыли «Позже»: её видно в меню ☰. */
    private val _latest = MutableStateFlow<UpdateInfo?>(null)
    val latest: StateFlow<UpdateInfo?> = _latest.asStateFlow()

    private val _manualCheck = MutableStateFlow(ManualCheck.Idle)
    val manualCheck: StateFlow<ManualCheck> = _manualCheck.asStateFlow()

    private val installedInfo: PackageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
    val installedVersionCode: Long = PackageInfoCompat.getLongVersionCode(installedInfo)
    val installedVersionName: String = installedInfo.versionName.orEmpty()

    private var checkJob: Job? = null
    private var downloadJob: Job? = null
    private var startHandled = false

    /** Приложение открыли (или вернулись в него): «Обновлено до…» один раз и проверка, если пора. */
    fun onAppStart() {
        if (!startHandled) {
            startHandled = true
            announceUpdateIfJustInstalled()
            restoreKnownUpdate()
        }
        if (isCheckDue(lastAttempt(), Instant.now())) runCheck(manual = false)
    }

    /** Вернулись из настроек «Установка неизвестных приложений»: если разрешили — продолжаем. */
    fun onResume() {
        val current = _state.value
        if (current is UpdateState.NeedsPermission && canInstallPackages()) install(current.info)
    }

    /** Проверка из меню ☰. */
    fun checkNow() = runCheck(manual = true)

    /** Меню закрыли: в следующий раз строка снова «Версия … · проверить обновления». */
    fun clearManualCheck() {
        if (_manualCheck.value != ManualCheck.Checking) _manualCheck.value = ManualCheck.Idle
    }

    /** «Позже»: плашка этой версии больше не показывается, обновиться можно из меню ☰. */
    fun dismiss() {
        val info = currentInfo() ?: return
        preferences.edit { putLong(KEY_DISMISSED_VERSION, info.versionCode) }
        _state.value = UpdateState.Idle
    }

    /** Из меню ☰: вернуть плашку скрытой версии. */
    fun showLatest() {
        val info = _latest.value ?: return
        preferences.edit { remove(KEY_DISMISSED_VERSION) }
        if (_state.value is UpdateState.Idle) _state.value = UpdateState.Available(info)
    }

    /** «Понятно» на плашке «Обновлено до…». */
    fun dismissUpdated() {
        if (_state.value is UpdateState.Updated) _state.value = UpdateState.Idle
    }

    /** «Обновить», «Повторить», «Установить». */
    fun start() {
        val info = currentInfo() ?: return
        if (downloadJob?.isActive == true) return
        downloadJob = scope.launch {
            withContext(Dispatchers.IO) { readyApk(info) } ?: download(info) ?: return@launch
            install(info)
        }
    }

    /** Итог установки от Android ([UpdateInstallReceiver]). Удачу не увидим: приложение закроется и обновится. */
    fun onInstallResult(status: Int, message: String?) {
        val info = currentInfo() ?: return
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> Unit
            // Окно закрыли «Отмена» или «Назад»
            PackageInstaller.STATUS_FAILURE_ABORTED -> _state.value = UpdateState.ReadyToInstall(info)
            else -> {
                Log.w(TAG, "Установка не удалась: $status $message")
                _state.value = UpdateState.Failed(info, UpdateError.InstallFailed)
            }
        }
    }

    /** Экран «Установка неизвестных приложений» для этого приложения. */
    fun installPermissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${appContext.packageName}"))

    private fun runCheck(manual: Boolean) {
        if (checkJob?.isActive == true) return
        if (manual) _manualCheck.value = ManualCheck.Checking
        checkJob = scope.launch {
            preferences.edit { putLong(KEY_LAST_ATTEMPT, Instant.now().toEpochMilli()) }
            val text = withContext(Dispatchers.IO) { fetchInfoText() }
            val info = text?.let(::parseUpdateInfo)?.takeIf { it.isWellFormed(allowHttp = isDebuggable) }
            if (info == null) {
                if (manual) _manualCheck.value = ManualCheck.Failed
                return@launch
            }
            // Справка запоминается: если Android перезапустит приложение (например, после разрешения на установку),
            // плашка вернётся без новой проверки
            preferences.edit { putString(KEY_LATEST_JSON, text) }
            val isNewer = info.isUpdateFor(installedVersionCode, Build.VERSION.SDK_INT)
            _latest.value = if (isNewer) info else null
            if (manual) _manualCheck.value = if (isNewer) ManualCheck.Idle else ManualCheck.UpToDate
            if (!isNewer) return@launch
            val dismissed = preferences.getLong(KEY_DISMISSED_VERSION, 0)
            _state.update { current ->
                when {
                    // Загрузку или установку не перебиваем
                    current !is UpdateState.Idle && current !is UpdateState.Available && current !is UpdateState.Failed -> current
                    info.versionCode == dismissed && !manual -> current
                    else -> UpdateState.Available(info)
                }
            }
            if (manual) preferences.edit { remove(KEY_DISMISSED_VERSION) }
        }
    }

    private fun fetchInfoText(): String? = try {
        val connection = open(updateUrl)
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Справка об обновлении: ответ ${connection.responseCode}")
                null
            } else {
                connection.inputStream.bufferedReader().use { it.readText() }
            }
        } finally {
            connection.disconnect()
        }
    } catch (e: Exception) {
        Log.w(TAG, "Справка об обновлении не скачалась", e)
        null
    }

    /** Скачивает APK с ходом загрузки и проверяет. null — не вышло, причина уже в [state]. */
    private suspend fun download(info: UpdateInfo): File? {
        _state.value = UpdateState.Downloading(info, 0f)
        val result = withContext(Dispatchers.IO) {
            val target = apkFile(info)
            val part = File(target.path + ".part")
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                val connection = open(info.apkUrl)
                try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext UpdateError.Network
                    var loaded = 0L
                    connection.inputStream.use { input ->
                        part.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                digest.update(buffer, 0, read)
                                loaded += read
                                // Сервер прислал больше обещанного — это не тот файл
                                if (loaded > info.size) return@withContext UpdateError.Corrupted
                                _state.value = UpdateState.Downloading(info, loaded.toFloat() / info.size)
                            }
                        }
                    }
                    if (loaded != info.size || digest.hex() != info.sha256) return@withContext UpdateError.Corrupted
                } finally {
                    connection.disconnect()
                }
                if (!isSameAppNewer(part)) return@withContext UpdateError.WrongApp
                target.delete()
                if (!part.renameTo(target)) return@withContext UpdateError.Corrupted
                null
            } catch (e: Exception) {
                Log.w(TAG, "APK не скачался", e)
                UpdateError.Network
            } finally {
                part.delete()
            }
        }
        if (result != null) {
            _state.value = UpdateState.Failed(info, result)
            return null
        }
        return apkFile(info)
    }

    /** Уже скачанный и проверенный APK этой версии (например, если в первый раз не дали разрешение). */
    private fun readyApk(info: UpdateInfo): File? {
        val file = apkFile(info)
        if (!file.exists()) return null
        val ok = file.length() == info.size && file.sha256() == info.sha256 && isSameAppNewer(file)
        if (!ok) file.delete()
        return file.takeIf { ok }
    }

    /** В файле это же приложение, новее установленного и подписано тем же ключом. */
    private fun isSameAppNewer(apk: File): Boolean {
        val packageManager = appContext.packageManager
        val candidate = archiveInfo(apk) ?: return false
        if (candidate.packageName != appContext.packageName) return false
        if (PackageInfoCompat.getLongVersionCode(candidate) <= installedVersionCode) return false
        val installed = installedPackageInfoWithSigners(packageManager)
        return sameSigners(signers(installed), signers(candidate))
    }

    private fun install(info: UpdateInfo) {
        if (!canInstallPackages()) {
            _state.value = UpdateState.NeedsPermission(info)
            return
        }
        _state.value = UpdateState.Installing(info)
        scope.launch(Dispatchers.IO) {
            try {
                DatabaseBackups.backupOpen(appContext, database(), "before-${info.versionName}")
            } catch (e: Exception) {
                // Копия — страховка: если её не вышло сделать, лучше не обновляться
                Log.w(TAG, "Копия базы перед обновлением не сохранилась", e)
                _state.value = UpdateState.Failed(info, UpdateError.InstallFailed)
                return@launch
            }
            // Что нового — покажем после обновления
            preferences.edit {
                putLong(KEY_PENDING_VERSION, info.versionCode)
                putString(KEY_PENDING_NOTES, info.notes.joinToString(NOTES_SEPARATOR))
            }
            try {
                commitSession(apkFile(info))
            } catch (e: Exception) {
                Log.w(TAG, "Установка не началась", e)
                _state.value = UpdateState.Failed(info, UpdateError.InstallFailed)
            }
        }
    }

    private fun commitSession(apk: File) {
        val installer = appContext.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(appContext.packageName)
            setSize(apk.length())
            // Всегда окно Android «Обновить приложение?»: тихо ставить обновления умеет только магазин
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("base.apk", 0, apk.length()).use { output ->
                apk.inputStream().use { it.copyTo(output) }
                session.fsync(output)
            }
            val intent = Intent(appContext, UpdateInstallReceiver::class.java)
            // Android дописывает в намерение итог установки, поэтому оно изменяемое
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            val pending = PendingIntent.getBroadcast(appContext, REQUEST_INSTALL, intent, flags)
            session.commit(pending.intentSender)
        }
    }

    /** Первый запуск после обновления: «Обновлено до…» и что нового, если ставили из приложения. */
    private fun announceUpdateIfJustInstalled() {
        val lastSeen = preferences.getLong(KEY_LAST_SEEN_VERSION, 0)
        if (lastSeen == installedVersionCode) return
        val pendingVersion = preferences.getLong(KEY_PENDING_VERSION, 0)
        val notes = preferences.getString(KEY_PENDING_NOTES, null)
            ?.split(NOTES_SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()
        preferences.edit {
            putLong(KEY_LAST_SEEN_VERSION, installedVersionCode)
            remove(KEY_PENDING_VERSION)
            remove(KEY_PENDING_NOTES)
            remove(KEY_DISMISSED_VERSION)
        }
        // Скачанные APK больше не нужны
        scope.launch(Dispatchers.IO) { updatesDir().listFiles()?.forEach { it.delete() } }
        // Самый первый запуск версии с обновлением (или установка вручную) — объявлять нечего
        if (lastSeen != 0L && pendingVersion == installedVersionCode) {
            _state.value = UpdateState.Updated(installedVersionName, notes)
        }
    }

    /** Плашка версии, найденной при прошлой проверке, — до новой проверки по сети. */
    private fun restoreKnownUpdate() {
        val info = preferences.getString(KEY_LATEST_JSON, null)?.let(::parseUpdateInfo) ?: return
        if (!info.isWellFormed(allowHttp = isDebuggable) || !info.isUpdateFor(installedVersionCode, Build.VERSION.SDK_INT)) return
        _latest.value = info
        if (_state.value is UpdateState.Idle && preferences.getLong(KEY_DISMISSED_VERSION, 0) != info.versionCode) {
            _state.value = UpdateState.Available(info)
        }
    }

    private fun currentInfo(): UpdateInfo? = when (val current = _state.value) {
        is UpdateState.Available -> current.info
        is UpdateState.Downloading -> current.info
        is UpdateState.NeedsPermission -> current.info
        is UpdateState.Installing -> current.info
        is UpdateState.ReadyToInstall -> current.info
        is UpdateState.Failed -> current.info
        UpdateState.Idle, is UpdateState.Updated -> null
    }

    private fun canInstallPackages(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || appContext.packageManager.canRequestPackageInstalls()

    private fun lastAttempt(): Instant? =
        preferences.getLong(KEY_LAST_ATTEMPT, 0).takeIf { it > 0 }?.let(Instant::ofEpochMilli)

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            useCaches = false
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("User-Agent", "Ezhednevnik/$installedVersionName (Android ${Build.VERSION.RELEASE})")
        }

    private fun updatesDir(): File = File(appContext.cacheDir, "updates").apply { mkdirs() }

    private fun apkFile(info: UpdateInfo): File = File(updatesDir(), "Ezhednevnik-${info.versionCode}.apk")

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.hex()
    }

    @Suppress("DEPRECATION")
    private fun archiveInfo(apk: File): PackageInfo? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        return appContext.packageManager.getPackageArchiveInfo(apk.path, flags)
    }

    @Suppress("DEPRECATION")
    private fun installedPackageInfoWithSigners(packageManager: PackageManager): PackageInfo {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        return packageManager.getPackageInfo(appContext.packageName, flags)
    }

    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo): List<ByteArray> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signing = info.signingInfo ?: return emptyList()
            val certificates = if (signing.hasMultipleSigners()) signing.apkContentsSigners else signing.signingCertificateHistory
            certificates.orEmpty().map { it.toByteArray() }
        } else {
            info.signatures.orEmpty().map { it.toByteArray() }
        }

    private companion object {
        const val TAG = "UpdateManager"
        const val PREFERENCES = "update"
        const val KEY_LAST_ATTEMPT = "last_attempt"
        const val KEY_DISMISSED_VERSION = "dismissed_version"
        const val KEY_LAST_SEEN_VERSION = "last_seen_version"
        const val KEY_PENDING_VERSION = "pending_version"
        const val KEY_PENDING_NOTES = "pending_notes"
        const val KEY_LATEST_JSON = "latest_json"
        const val NOTES_SEPARATOR = "\n"
        const val REQUEST_INSTALL = 1
    }
}
