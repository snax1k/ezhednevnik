package com.snax1k.chekki.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Цвета варианта Б, тёмная тема. Источник — макет MainB.dc.html, подробности в CLAUDE.md.

// Основа
private val Background = Color(0xFF0D0E13)
private val Surface = Color(0xFF1A1C24)
private val SurfaceRaised = Color(0xFF242733)
private val TextPrimary = Color(0xFFF1F2F6)
private val TextSecondary = Color(0xFFA9AEBC)
private val Accent = Color(0xFFFFA25E)
private val AccentTint = Color(0xFFFFB27A)
private val OnAccent = Color(0xFF2B1606)

// Цвета для стандартных компонентов Material (кнопки, поля, диалоги, нижние панели).
// Значения с пометкой «в макете нет» выведены из основы, чтобы не осталось фиолетовых цветов Material.
internal val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    // В макете нет: акцент на 20% поверх фона
    primaryContainer = Color(0xFF3D2C22),
    onPrimaryContainer = AccentTint,
    // В макете нет второстепенного цвета, берём нейтральные
    secondary = TextSecondary,
    onSecondary = Background,
    secondaryContainer = SurfaceRaised,
    onSecondaryContainer = TextPrimary,
    background = Background,
    onBackground = TextPrimary,
    surface = Background,
    onSurface = TextPrimary,
    surfaceVariant = Surface,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = Background,
    // В макете нет: середина между фоном и поверхностью
    surfaceContainerLow = Color(0xFF14151C),
    surfaceContainer = Surface,
    surfaceContainerHigh = SurfaceRaised,
    surfaceContainerHighest = SurfaceRaised,
    surfaceBright = SurfaceRaised,
    surfaceDim = Background,
    // Светлая плашка «Задача удалена · Отменить» (Snackbar) на тёмном фоне
    inverseSurface = TextPrimary,
    inverseOnSurface = Background,
    // Кнопка «Отменить» на плашке: тёмный оттенок акцента из макета «Состояния и жесты»
    inversePrimary = Color(0xFFB04F08),
    // В макете нет: середина между фоном и вторичным текстом
    outline = Color(0xFF5B5E68),
    outlineVariant = SurfaceRaised
)

/** Цвет группы задач: [base] — для градиента фона, [tint] — для иконки и счётчика. */
@Immutable
data class GroupColor(val base: Color, val tint: Color)

/** Цвета яркой карточки группы («Просроченные», «Сегодня»). */
@Immutable
data class HighlightCardColors(
    val gradientStart: Color,
    val gradientEnd: Color,
    val shadow: Color,
    val content: Color,
    val contentSecondary: Color,
    /** Фон квадрата под иконкой. */
    val iconContainer: Color
)

/** Цвета круглого трекера в верхней панели. */
@Immutable
data class TrackerColors(val fill: Color, val content: Color)

/** Цвета приложения, которых нет в Material ColorScheme. Доступ: `ChekkiTheme.colors`. */
@Immutable
data class ChekkiColors(
    val noDate: GroupColor,
    val tomorrow: GroupColor,
    val thisWeek: GroupColor,
    val thisMonth: GroupColor,
    val later: GroupColor,
    val done: GroupColor,
    val overdue: HighlightCardColors,
    val today: HighlightCardColors,
    /** Дорожка полосы прогресса в карточке «Сегодня». */
    val todayProgressTrack: Color,
    val fabGradientStart: Color,
    val fabGradientEnd: Color,
    val fabShadow: Color,
    val onFab: Color,
    /** Тень круглых кнопок, у которых нет своего цвета тени (голосовой ввод). */
    val floatingShadow: Color,
    /** Заливки круглых трекеров по порядку. Набор трекеров ещё не утверждён. */
    val trackers: List<TrackerColors>,
    val trackerRing: Color,
    val trackerRingTrack: Color,
    /** Палитра категорий: Работа, Личное, Дом, Здоровье, Учёба, Финансы. */
    val categories: List<Color>,
    val priorityHigh: Color,
    val priorityMedium: Color,
    val priorityLow: Color,
    /** Заливка отметки выполненной задачи. */
    val taskDone: Color,
    /** Галочка на отметке выполненной задачи. */
    val onTaskDone: Color,
    /** Название выполненной задачи. */
    val taskDoneTitle: Color,
    /** Подсказка в пустом поле ввода («Что нужно сделать?»). */
    val textPlaceholder: Color,
    /** Кнопка «Отложить» под строкой задачи при свайпе влево. Свайп вправо («Выполнить») — цвета [taskDone]. */
    val swipePostpone: Color,
    val onSwipePostpone: Color,
    /** Кнопка «Удалить» под строкой задачи при свайпе влево. */
    val swipeDelete: Color,
    val onSwipeDelete: Color
)

internal val DarkChekkiColors = ChekkiColors(
    noDate = GroupColor(base = Accent, tint = AccentTint),
    tomorrow = GroupColor(base = Color(0xFF6EC3E8), tint = Color(0xFF7CCDF0)),
    thisWeek = GroupColor(base = Color(0xFF8E9BFF), tint = Color(0xFFA3AEFF)),
    thisMonth = GroupColor(base = Color(0xFFC08CF0), tint = Color(0xFFCFA3F5)),
    later = GroupColor(base = Color(0xFFF07AC8), tint = Color(0xFFF49AD6)),
    done = GroupColor(base = Color(0xFFC3C8D4), tint = Color(0xFFC3C8D4)),
    overdue = HighlightCardColors(
        gradientStart = Color(0xFFD9475A),
        gradientEnd = Color(0xFFB0304F),
        shadow = Color(0xFFD9475A).copy(alpha = 0.25f),
        content = Color.White,
        contentSecondary = Color(0xFFFFE4E6),
        iconContainer = Color.White.copy(alpha = 0.18f)
    ),
    today = HighlightCardColors(
        gradientStart = Color(0xFF23864C),
        gradientEnd = Color(0xFF17695C),
        shadow = Color(0xFF23864C).copy(alpha = 0.28f),
        content = Color.White,
        // В макете у времени задач оттенок чуть светлее (#DDF4E5), на глаз разницы нет
        contentSecondary = Color(0xFFD4F2DF),
        iconContainer = Color.White.copy(alpha = 0.18f)
    ),
    todayProgressTrack = Color.White.copy(alpha = 0.22f),
    fabGradientStart = Color(0xFFFFB070),
    fabGradientEnd = Color(0xFFF27A45),
    fabShadow = Color(0xFFF27A45).copy(alpha = 0.4f),
    onFab = OnAccent,
    floatingShadow = Color.Black.copy(alpha = 0.4f),
    trackers = listOf(
        // Привычки
        TrackerColors(fill = Color(0xFF2C7FA8), content = Color.White),
        // Вода
        TrackerColors(fill = Color(0xFF4A55C9), content = Color.White),
        // Цели
        TrackerColors(fill = Color(0xFF16857C), content = Color.White),
        // Растяжка
        TrackerColors(fill = Color(0xFFF0924A), content = OnAccent)
    ),
    trackerRing = Color.White,
    trackerRingTrack = Color.White.copy(alpha = 0.10f),
    categories = listOf(
        Color(0xFF8AB4FF),
        Color(0xFFF29BC1),
        Color(0xFFF5BE6A),
        Color(0xFF7BD49A),
        Color(0xFFB89CFF),
        Color(0xFF6FD3CF)
    ),
    priorityHigh = Color(0xFFFF8A7A),
    priorityMedium = Color(0xFFF5BE6A),
    priorityLow = Color(0xFF8AB4FF),
    // В варианте Б список задач не нарисован, отметки взяты из макета А
    taskDone = Color(0xFF7BD49A),
    onTaskDone = Color(0xFF0E1A12),
    taskDoneTitle = Color(0xFF7C828D),
    // Из макета панели новой задачи (вариант А), в Б её нет
    textPlaceholder = Color(0xFF7C828D),
    // Из макета «Состояния и жесты» (вариант А): те же жёлтый и красный, что у приоритетов
    swipePostpone = Color(0xFFF5BE6A),
    onSwipePostpone = Color(0xFF2A1C04),
    swipeDelete = Color(0xFFFF8A7A),
    onSwipeDelete = Color(0xFF2B0E0A)
)
