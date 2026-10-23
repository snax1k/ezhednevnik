package com.snax1k.chekki.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.snax1k.chekki.R
import com.snax1k.chekki.data.db.CategoryEntity
import com.snax1k.chekki.data.db.CategoryIcon
import com.snax1k.chekki.data.db.CategorySummary
import com.snax1k.chekki.ui.components.CardProgressBar
import com.snax1k.chekki.ui.components.SquareIconButton
import com.snax1k.chekki.ui.icons.ChekkiIcons
import com.snax1k.chekki.ui.theme.ChekkiTheme
import com.snax1k.chekki.ui.theme.cssLinearGradient

/**
 * Категории: плитки в два столбца по макету «Категории», переведённому на стиль Б (плитки как у групп
 * главного экрана), под ними «Без категории». Нажатие на плитку открывает задачи категории.
 */
@Composable
fun CategoriesScreen(
    state: CategoriesUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onCategoryClick: (Long) -> Unit = {},
    onCategoryLongClick: (Long) -> Unit = {},
    onAddCategory: () -> Unit = {},
    onUncategorizedClick: () -> Unit = {}
) {
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
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
                SquareIconButton(ChekkiIcons.Back, stringResource(R.string.action_back), onBack)
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp + navigationBarHeight),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "title") {
                    Text(
                        text = stringResource(R.string.categories_title),
                        modifier = Modifier
                            .padding(start = 4.dp, bottom = 8.dp)
                            .semantics { heading() },
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                // Две плитки в ряд; у последней нечётной — пустое место справа
                items(state.categories.chunked(2), key = { row -> row.first().category.id }) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { summary ->
                            CategoryTile(
                                summary = summary,
                                countAlpha = if (state.isLoading) 0f else 1f,
                                onClick = { onCategoryClick(summary.category.id) },
                                onLongClick = { onCategoryLongClick(summary.category.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                item(key = "add") { AddCategoryRow(onAddCategory) }
                item(key = "uncategorized") {
                    UncategorizedRow(
                        count = state.uncategorizedCount,
                        countAlpha = if (state.isLoading) 0f else 1f,
                        onClick = onUncategorizedClick,
                        modifier = Modifier.padding(top = 14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Плитка категории: иконка в её цвете и число невыполненных задач, ниже название и прогресс задач
 * на сегодня («сегодня 2/6»).
 */
@Composable
private fun CategoryTile(
    summary: CategorySummary,
    countAlpha: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = summary.category
    val color = categoryColor(category.colorIndex)
    val openText = pluralStringResource(R.plurals.category_open_tasks, summary.openCount, summary.openCount)
    val todayText = if (summary.todayTotal > 0) {
        stringResource(R.string.category_today_progress, summary.todayDone, summary.todayTotal)
    } else {
        stringResource(R.string.category_today_none)
    }
    val todayDescription = if (summary.todayTotal > 0) {
        stringResource(R.string.category_today_description, summary.todayDone, summary.todayTotal)
    } else {
        todayText
    }
    val description = stringResource(R.string.category_tile_description, category.name, openText, todayDescription)
    val editLabel = stringResource(R.string.category_edit_action)
    Column(
        modifier
            .height(132.dp)
            .clip(MaterialTheme.shapes.large)
            .background(cssLinearGradient(145f, color.copy(alpha = 0.28f), color.copy(alpha = 0.08f)))
            // Долгое нажатие — изменить категорию, как долгое нажатие на круг привычки
            .combinedClickable(role = Role.Button, onLongClickLabel = editLabel, onLongClick = onLongClick, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            }
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CategoryIconShape)
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(category.icon.vector, contentDescription = null, Modifier.size(20.dp), tint = color)
            }
            Text(
                text = summary.openCount.toString(),
                modifier = Modifier.alpha(countAlpha),
                style = MaterialTheme.typography.headlineSmall,
                color = color
            )
        }
        Column {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                Modifier
                    .padding(top = 8.dp)
                    .alpha(countAlpha),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CardProgressBar(
                    done = summary.todayDone,
                    total = summary.todayTotal,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    color = color,
                    modifier = Modifier.weight(1f)
                )
                Text(todayText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** «+ Новая категория» под плитками. */
@Composable
private fun AddCategoryRow(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(ChekkiIcons.Add, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(
            text = stringResource(R.string.category_new),
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** «Без категории» с числом невыполненных задач — открывает задачи без категории. */
@Composable
private fun UncategorizedRow(count: Int, countAlpha: Float, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(ChekkiIcons.Tag, contentDescription = null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = stringResource(R.string.add_task_no_category),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = count.toString(),
            modifier = Modifier.alpha(countAlpha),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Icon(ChekkiIcons.ChevronRight, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
    }
}

/** Квадрат со скруглением под иконкой категории, как в макете. */
internal val CategoryIconShape = RoundedCornerShape(14.dp)

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun CategoriesScreenPreview() {
    val names = listOf(
        "Работа" to CategoryIcon.Work,
        "Личное" to CategoryIcon.Personal,
        "Дом" to CategoryIcon.Home,
        "Здоровье" to CategoryIcon.Health,
        "Учёба" to CategoryIcon.Study
    )
    val categories = names.mapIndexed { index, (name, icon) ->
        CategorySummary(
            category = CategoryEntity(id = index + 1L, name = name, colorIndex = index, icon = icon, position = index),
            openCount = 18 - index * 3,
            todayTotal = if (index == 4) 0 else 6 - index,
            todayDone = index % 3
        )
    }
    ChekkiTheme {
        CategoriesScreen(CategoriesUiState(categories, uncategorizedCount = 5))
    }
}
