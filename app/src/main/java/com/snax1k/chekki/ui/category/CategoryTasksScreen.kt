package com.snax1k.chekki.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.snax1k.chekki.R
import com.snax1k.chekki.data.db.CategoryEntity
import com.snax1k.chekki.data.db.CategoryIcon
import com.snax1k.chekki.ui.group.CountText
import com.snax1k.chekki.ui.group.HeaderTitleRow
import com.snax1k.chekki.ui.group.TaskListScreen
import com.snax1k.chekki.ui.group.TaskSection
import com.snax1k.chekki.ui.group.TintedCard
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.main.sampleTodayTasks
import com.snax1k.chekki.ui.tasks.TaskUi
import com.snax1k.chekki.ui.theme.ChekkiTheme
import com.snax1k.chekki.ui.theme.GroupColor
import java.time.LocalDate

/**
 * Невыполненные задачи категории (или без категории): шапка в цвете категории, задачи по дням —
 * просроченные, сегодня, дальше, в конце «Без даты». Свайпы, карточка и «+» — как в группах.
 */
@Composable
fun CategoryTasksScreen(
    state: CategoryTasksUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onToggleDone: (TaskUi) -> Unit = {},
    onTaskClick: (TaskUi) -> Unit = {},
    onPostpone: (TaskUi, LocalDate) -> Unit = { _, _ -> },
    onDelete: (TaskUi) -> Unit = {},
    onVoiceClick: () -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    TaskListScreen(
        today = state.today,
        sections = state.sections,
        isLoading = state.isLoading,
        emptyText = stringResource(if (state.categoryId == null) R.string.category_empty_uncategorized else R.string.category_empty),
        header = { headerModifier -> CategoryHeader(state, headerModifier) },
        modifier = modifier,
        onBack = onBack,
        onToggleDone = onToggleDone,
        onTaskClick = onTaskClick,
        onPostpone = onPostpone,
        onDelete = onDelete,
        onVoiceClick = onVoiceClick,
        onAddClick = onAddClick
    )
}

/** Шапка: карточка в цвете категории, как у групп, с её иконкой, названием и числом невыполненных задач. */
@Composable
private fun CategoryHeader(state: CategoryTasksUiState, modifier: Modifier = Modifier) {
    val category = state.category
    val color = if (category != null) {
        categoryColor(category.colorIndex).let { GroupColor(base = it, tint = it) }
    } else {
        ChekkiTheme.colors.done
    }
    val title = category?.name ?: if (state.categoryId == null) stringResource(R.string.add_task_no_category) else ""
    TintedCard(color, modifier) {
        Box(
            Modifier
                .padding(bottom = 12.dp)
                .size(36.dp)
                .clip(MaterialTheme.shapes.small)
                .background(color.base.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category?.icon?.vector ?: ChekkiIcons.Tag,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = color.tint
            )
        }
        HeaderTitleRow(
            title = title,
            subtitle = null,
            titleColor = MaterialTheme.colorScheme.onSurface,
            subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
            countAlpha = if (state.isLoading) 0f else 1f
        ) {
            CountText(state.tasks.count { !it.done }, color.tint)
        }
    }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun CategoryTasksScreenPreview() {
    val today = LocalDate.of(2026, 10, 23)
    val tasks = sampleTodayTasks()
    ChekkiTheme {
        CategoryTasksScreen(
            CategoryTasksUiState(
                categoryId = 1,
                category = CategoryEntity(id = 1, name = "Работа", colorIndex = 0, icon = CategoryIcon.Work, position = 0),
                today = today,
                sections = listOf(
                    TaskSection(today.minusDays(1), tasks.take(1)),
                    TaskSection(today, tasks.drop(1).take(2)),
                    TaskSection(null, tasks.drop(3).take(1), isNoDate = true)
                )
            )
        )
    }
}
