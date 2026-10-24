package com.snax1k.chekki.ui.oldapp

import android.database.sqlite.SQLiteException
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snax1k.chekki.R
import com.snax1k.chekki.data.oldapp.ImportHistory
import com.snax1k.chekki.data.oldapp.ImportPlan
import com.snax1k.chekki.data.oldapp.ImportReport
import com.snax1k.chekki.data.oldapp.OldAppImporter
import com.snax1k.chekki.data.oldapp.OldBackupReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.time.LocalDate

/** Где сейчас перенос. */
sealed interface ImportStep {
    /** Ничего не выбрано: объяснение и «Выбрать файл». */
    data object Start : ImportStep

    data object Reading : ImportStep

    /** Копия прочитана, ничего ещё не записано. [importedOn] — эту же копию уже переносили в этот день. */
    data class Preview(val report: ImportReport, val importedOn: LocalDate?) : ImportStep

    data object Importing : ImportStep

    data class Done(val report: ImportReport) : ImportStep

    data class Failed(@param:StringRes val message: Int) : ImportStep
}

/**
 * Экран переноса из старого ежедневника. Чтение и перевод ничего не меняют — база меняется только по «Перенести»,
 * одной транзакцией. [openStream] открывает выбранный файл (через ContentResolver), [workDir] — для временных файлов.
 */
class ImportViewModel(
    private val importer: OldAppImporter,
    private val history: ImportHistory,
    private val openStream: (Uri) -> InputStream?,
    private val workDir: File
) : ViewModel() {

    private val _step = MutableStateFlow<ImportStep>(ImportStep.Start)
    val step: StateFlow<ImportStep> = _step.asStateFlow()

    private var plan: ImportPlan? = null
    private var hash: String? = null

    /** Выбран файл в системном окне. */
    fun onFilePicked(uri: Uri) {
        _step.value = ImportStep.Reading
        viewModelScope.launch {
            _step.value = try {
                val (backup, newPlan) = withContext(Dispatchers.IO) {
                    val input = openStream(uri) ?: throw OldBackupReader.NotABackupException("Файл не открылся")
                    input.use { importer.read(it, workDir) }
                }
                plan = newPlan
                hash = backup.hash
                ImportStep.Preview(newPlan.report, history.importedOn(backup.hash))
            } catch (e: OldBackupReader.NotABackupException) {
                ImportStep.Failed(R.string.import_error_not_backup)
            } catch (e: SQLiteException) {
                Log.w(TAG, "Не читается Planner.db", e)
                ImportStep.Failed(R.string.import_error_not_backup)
            } catch (e: Exception) {
                Log.w(TAG, "Копия не прочиталась", e)
                ImportStep.Failed(R.string.import_error_read)
            }
        }
    }

    /** «Перенести»: запись одной транзакцией — либо всё, либо ничего. */
    fun import() {
        val current = plan ?: return
        _step.value = ImportStep.Importing
        viewModelScope.launch {
            _step.value = try {
                withContext(Dispatchers.IO) { importer.import(current) }
                history.remember(hash, LocalDate.now())
                plan = null
                ImportStep.Done(current.report)
            } catch (e: Exception) {
                Log.w(TAG, "Перенос не записался", e)
                ImportStep.Failed(R.string.import_error_write)
            }
        }
    }

    /** «Выбрать другой файл» и «Попробовать снова». */
    fun reset() {
        plan = null
        hash = null
        _step.value = ImportStep.Start
    }

    private companion object {
        const val TAG = "Import"
    }
}
