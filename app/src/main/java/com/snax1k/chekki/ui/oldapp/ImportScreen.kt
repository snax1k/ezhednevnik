package com.snax1k.chekki.ui.oldapp

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.snax1k.chekki.R
import com.snax1k.chekki.data.oldapp.ImportReport
import com.snax1k.chekki.ui.addtask.SaveButton
import com.snax1k.chekki.ui.components.SquareIconButton
import com.snax1k.chekki.ui.components.formatDayMonth
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.theme.ChekkiTheme
import java.time.LocalDate

/**
 * Перенос из старого ежедневника. Макета нет. Шаги: объяснение и «Выбрать файл» (системное окно, разрешения
 * не нужны) → «Читаю копию…» → предпросмотр: что перенесётся и что не полностью → «Перенести» → «Переношу…» → итог.
 */
@Composable
fun ImportScreen(
    step: ImportStep,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onFilePicked: (android.net.Uri) -> Unit = {},
    onImport: () -> Unit = {},
    onReset: () -> Unit = {}
) {
    // У файла .plnb нет своего типа, поэтому в окне показываются все файлы
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onFilePicked(uri)
    }
    val pickFile = { picker.launch(arrayOf("*/*")) }
    val busy = step is ImportStep.Reading || step is ImportStep.Importing
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Пока идёт запись, уходить нельзя: транзакция всё равно доделается, но итог потеряется
                if (!busy) SquareIconButton(ChekkiIcons.Back, stringResource(R.string.action_back), onBack)
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp + bottom),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.import_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                when (step) {
                    ImportStep.Start -> {
                        BodyText(stringResource(R.string.import_intro))
                        SaveButton(enabled = true, onClick = pickFile, text = stringResource(R.string.import_pick_file))
                    }
                    ImportStep.Reading -> Progress(stringResource(R.string.import_reading))
                    ImportStep.Importing -> Progress(stringResource(R.string.import_importing))
                    is ImportStep.Preview -> {
                        BodyText(stringResource(R.string.import_preview_intro))
                        ReportCard(step.report)
                        Caveats(step.report)
                        step.importedOn?.let { day ->
                            Notice(stringResource(R.string.import_already_imported, formatDayMonth(day, LocalDate.now())), warning = true)
                        }
                        SaveButton(enabled = true, onClick = onImport, text = stringResource(R.string.import_start))
                        TextButton(onClick = pickFile, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.import_pick_other), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    is ImportStep.Done -> {
                        Notice(stringResource(R.string.import_done), warning = false)
                        ReportCard(step.report)
                        SaveButton(enabled = true, onClick = onBack, text = stringResource(R.string.import_to_main))
                    }
                    is ImportStep.Failed -> {
                        Notice(stringResource(step.message), warning = true)
                        SaveButton(
                            enabled = true,
                            onClick = {
                                onReset()
                                pickFile()
                            },
                            text = stringResource(R.string.import_pick_other)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BodyText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Progress(text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** Числа: категории, задачи, подзадачи, привычки. */
@Composable
private fun ReportCard(report: ImportReport) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        ReportRow(stringResource(R.string.import_row_categories), report.categories.toString())
        ReportRow(stringResource(R.string.import_row_open_tasks), report.openTasks.toString())
        ReportRow(stringResource(R.string.import_row_done_tasks), report.doneTasks.toString())
        ReportRow(stringResource(R.string.import_row_subtasks), report.subtasks.toString())
        ReportRow(stringResource(R.string.import_row_habits), report.habits.toString())
        ReportRow(stringResource(R.string.import_row_habit_checks), report.habitChecks.toString())
        ReportRow(stringResource(R.string.import_row_habit_reminders), report.habitReminders.toString())
    }
}

@Composable
private fun ReportRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Что перенесётся не полностью — только непустые пункты. */
@Composable
private fun Caveats(report: ImportReport) {
    val lines = buildList {
        if (report.mergedDuplicates > 0) add(stringResource(R.string.import_caveat_duplicates, report.mergedDuplicates))
        if (report.missedRepeats > 0) add(stringResource(R.string.import_caveat_missed, report.missedRepeats))
        if (report.extraCategories > 0) add(stringResource(R.string.import_caveat_categories, report.extraCategories))
        if (report.deadlines > 0) add(stringResource(R.string.import_caveat_deadlines, report.deadlines))
        if (report.approximatedRepeats.isNotEmpty()) {
            add(stringResource(R.string.import_caveat_repeats, report.approximatedRepeats.joinToString(", ") { "«$it»" }))
        }
        if (report.photos > 0 || report.audio > 0) add(stringResource(R.string.import_caveat_media, report.photos, report.audio))
    }
    if (lines.isEmpty()) return
    Text(
        text = stringResource(R.string.import_caveats_title),
        modifier = Modifier
            .padding(top = 6.dp)
            .semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    lines.forEach { line ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Плашка: предупреждение — цветом приоритета «высокий», итог — цветом выполненной задачи. */
@Composable
private fun Notice(text: String, warning: Boolean) {
    val color: Color = if (warning) ChekkiTheme.colors.priorityHigh else ChekkiTheme.colors.taskDone
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(color.copy(alpha = 0.14f))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (warning) ChekkiIcons.Overdue else ChekkiIcons.Check, contentDescription = null, Modifier.size(20.dp), tint = color)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun ImportPreviewPreview() {
    val report = ImportReport(
        categories = 17, openTasks = 147, doneTasks = 8430, subtasks = 17152, habits = 4, habitChecks = 482,
        habitReminders = 7, mergedDuplicates = 49, missedRepeats = 5, approximatedRepeats = listOf("Счётчики"),
        extraCategories = 15, deadlines = 823, photos = 303, audio = 2
    )
    ChekkiTheme {
        ImportScreen(ImportStep.Preview(report, importedOn = LocalDate.of(2026, 10, 25)))
    }
}
