package com.snax1k.chekki.ui.category

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.snax1k.chekki.R
import com.snax1k.chekki.data.db.CategorySummary
import com.snax1k.chekki.ui.icons.ChekkiIcons

/**
 * Порядок категорий на экране «Категории»: строки перетаскиваются за ручку «≡», как на экране
 * «Все привычки». Пока строку тащат, порядок свой; отпустили — он уходит в базу через [onReorder].
 */
@Stable
class CategoryOrderState(
    private val listState: LazyListState,
    private val haptics: HapticFeedback
) {
    var order by mutableStateOf<List<Long>>(emptyList())
    var draggedId by mutableStateOf<Long?>(null)
        private set
    var dragOffset by mutableFloatStateOf(0f)
        private set

    fun start(id: Long, current: List<Long>) {
        order = current
        draggedId = id
        dragOffset = 0f
        haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }

    fun finish(onReorder: (List<Long>) -> Unit) {
        draggedId = null
        dragOffset = 0f
        onReorder(order)
    }

    /** Палец сдвинулся на [amount]; середина строки зашла на соседнюю — меняем их местами. */
    fun drag(amount: Float) {
        dragOffset += amount
        val id = draggedId ?: return
        val visible = listState.layoutInfo.visibleItemsInfo
        val current = visible.firstOrNull { it.key == id } ?: return
        val middle = (current.offset + dragOffset + current.size / 2f).toInt()
        val target = visible.firstOrNull { info ->
            info.key != id && info.key in order && middle in info.offset..(info.offset + info.size)
        } ?: return
        val from = order.indexOf(id)
        val to = order.indexOf(target.key)
        order = order.toMutableList().apply { add(to, removeAt(from)) }
        // Строка встанет на место соседней, а под пальцем должна остаться там же
        dragOffset += current.offset - target.offset
        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }
}

@Composable
fun rememberCategoryOrderState(listState: LazyListState): CategoryOrderState {
    val haptics = LocalHapticFeedback.current
    return remember(listState, haptics) { CategoryOrderState(listState, haptics) }
}

/**
 * Строки порядка в списке. Строки рисуются из свежих данных в местном порядке: категория, ушедшая
 * в архив или удалённая, не задерживается ни на кадр (иначе её ключ встретился бы в списке дважды).
 */
fun LazyListScope.categoryOrderItems(
    categories: List<CategorySummary>,
    state: CategoryOrderState,
    onReorder: (List<Long>) -> Unit
) {
    val byId = categories.associateBy { it.category.id }
    val shown = state.order.mapNotNull { byId[it] } + categories.filter { it.category.id !in state.order }
    val ids = shown.map { it.category.id }
    items(shown, key = { it.category.id }) { summary ->
        val id = summary.category.id
        val index = ids.indexOf(id)
        val isDragged = id == state.draggedId
        val currentIds by rememberUpdatedState(ids)
        val currentOnReorder by rememberUpdatedState(onReorder)
        fun move(by: Int) {
            val to = index + by
            if (to !in ids.indices) return
            state.order = ids.toMutableList().apply { add(to, removeAt(index)) }
            onReorder(state.order)
        }
        OrderRow(
            summary = summary,
            isDragged = isDragged,
            onMoveUp = if (index > 0) ({ move(-1) }) else null,
            onMoveDown = if (index < ids.lastIndex) ({ move(1) }) else null,
            dragHandle = Modifier.pointerInput(id) {
                detectDragGestures(
                    onDragStart = { state.start(id, currentIds) },
                    onDragEnd = { state.finish(currentOnReorder) },
                    onDragCancel = { state.finish(currentOnReorder) },
                    onDrag = { change, amount ->
                        change.consume()
                        state.drag(amount.y)
                    }
                )
            },
            modifier = Modifier
                .zIndex(if (isDragged) 1f else 0f)
                .graphicsLayer { translationY = if (isDragged) state.dragOffset else 0f }
                // Остальные строки плавно уступают место; перетаскиваемая идёт за пальцем
                .then(if (isDragged) Modifier else Modifier.animateItem())
        )
    }
}

/** Строка порядка: иконка в цвете категории, название и ручка «≡». Для TalkBack — «Выше» и «Ниже». */
@Composable
private fun OrderRow(
    summary: CategorySummary,
    isDragged: Boolean,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    dragHandle: Modifier,
    modifier: Modifier = Modifier
) {
    val category = summary.category
    val color = categoryColor(category.colorIndex)
    val scale by animateFloatAsState(if (isDragged) 1.03f else 1f, label = "categoryRowScale")
    val moveUpLabel = stringResource(R.string.habit_move_up)
    val moveDownLabel = stringResource(R.string.habit_move_down)
    val dragLabel = stringResource(R.string.habit_drag, category.name)
    Row(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .fillMaxWidth()
            .height(64.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(if (isDragged) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer)
            .semantics(mergeDescendants = true) {
                customActions = listOfNotNull(
                    onMoveUp?.let { action -> CustomAccessibilityAction(moveUpLabel) { action(); true } },
                    onMoveDown?.let { action -> CustomAccessibilityAction(moveDownLabel) { action(); true } }
                )
            }
            .padding(start = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
            text = category.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            Modifier
                .size(48.dp)
                .then(dragHandle)
                .semantics { contentDescription = dragLabel },
            contentAlignment = Alignment.Center
        ) {
            Icon(ChekkiIcons.DragHandle, contentDescription = null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Местный порядок забывается, когда база прислала новый, а строку никто не тащит. */
@Composable
fun SyncCategoryOrder(state: CategoryOrderState, categories: List<CategorySummary>) {
    LaunchedEffect(categories) {
        if (state.draggedId == null) state.order = categories.map { it.category.id }
    }
}
