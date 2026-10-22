package com.snax1k.chekki.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.snax1k.chekki.R
import com.snax1k.chekki.ui.components.BottomFade
import com.snax1k.chekki.ui.components.CardProgressBar
import com.snax1k.chekki.ui.components.FloatingButtonsAreaHeight
import com.snax1k.chekki.ui.components.FloatingTaskButtons
import com.snax1k.chekki.ui.components.HighlightCard
import com.snax1k.chekki.ui.components.ProgressCountText
import com.snax1k.chekki.ui.components.SquareIconButton
import com.snax1k.chekki.ui.components.formatDayTitle
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.main.sampleTodayTasks
import com.snax1k.chekki.ui.tasks.TaskRow
import com.snax1k.chekki.ui.tasks.TaskUi
import com.snax1k.chekki.ui.theme.ChekkiTheme
import java.time.LocalDate

/**
 * Список задач на сегодня. Макет есть только в стиле А, здесь он переведён на Б:
 * шапка — яркая карточка «Сегодня» как на главном экране, задачи — скруглённые карточки.
 */
@Composable
fun TodayScreen(
    state: TodayUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onToggleDone: (TaskUi) -> Unit = {},
    onSortClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onVoiceClick: () -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                TodayTopBar(onBack, onSortClick, onMoreClick)
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 6.dp,
                        bottom = FloatingButtonsAreaHeight + navigationBarHeight
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        TodayHeader(
                            date = state.date,
                            done = state.tasks.count { it.done },
                            total = state.tasks.size,
                            showProgress = !state.isLoading,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    items(state.tasks, key = { it.id }) { task ->
                        TaskRow(task = task, onToggleDone = { onToggleDone(task) })
                    }
                    if (!state.isLoading && state.tasks.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.today_empty),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            BottomFade(Modifier.align(Alignment.BottomCenter))
            FloatingTaskButtons(
                onVoiceClick = onVoiceClick,
                onAddClick = onAddClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun TodayTopBar(onBack: () -> Unit, onSortClick: () -> Unit, onMoreClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SquareIconButton(ChekkiIcons.Back, stringResource(R.string.action_back), onBack)
        Spacer(Modifier.weight(1f))
        SquareIconButton(ChekkiIcons.Sort, stringResource(R.string.list_sort), onSortClick)
        SquareIconButton(ChekkiIcons.More, stringResource(R.string.list_more), onMoreClick)
    }
}

/**
 * Шапка: заголовок, дата, счётчик «выполнено/всего» и полоса прогресса.
 * Пока задачи грузятся, счётчик и полоса прозрачные, но место занимают, чтобы шапка не прыгала.
 */
@Composable
private fun TodayHeader(
    date: LocalDate,
    done: Int,
    total: Int,
    showProgress: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = ChekkiTheme.colors.today
    val progressAlpha = if (showProgress) 1f else 0f
    HighlightCard(colors, modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.group_today),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.content
                )
                Text(
                    text = formatDayTitle(date),
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.contentSecondary
                )
            }
            Box(Modifier.alpha(progressAlpha)) {
                ProgressCountText(done, total, colors)
            }
        }
        CardProgressBar(
            done = done,
            total = total,
            trackColor = ChekkiTheme.colors.todayProgressTrack,
            color = colors.content,
            modifier = Modifier
                .padding(top = 14.dp)
                .alpha(progressAlpha)
        )
    }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun TodayScreenPreview() {
    ChekkiTheme {
        var tasks by remember { mutableStateOf(sampleTodayTasks()) }
        TodayScreen(
            state = TodayUiState(date = LocalDate.of(2026, 10, 22), tasks = tasks),
            onToggleDone = { task ->
                tasks = tasks.map { if (it.id == task.id) it.copy(done = !it.done) else it }
            }
        )
    }
}
