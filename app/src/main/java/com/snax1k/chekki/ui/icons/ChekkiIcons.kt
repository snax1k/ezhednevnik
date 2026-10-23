package com.snax1k.chekki.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// Иконки из макета: контуры на сетке 24×24, перенесены из SVG как есть.
// Цвет задаётся через tint у Icon, поэтому здесь все контуры чёрные.
object ChekkiIcons {
    val Menu: ImageVector by lazy { outlineIcon("Menu", "M4 7h16M4 12h16M4 17h10") }

    val Statistics: ImageVector by lazy {
        ImageVector.Builder("Statistics", 24.dp, 24.dp, 24f, 24f)
            .addOutline("M11 4a8 8 0 1 0 8.5 9H11z")
            // Выделенный сектор диаграммы залит
            .addOutline("M14.5 3.2a8 8 0 0 1 6.3 6.3h-6.3z", filled = true)
            .build()
    }

    // Трекеры
    val Habits: ImageVector by lazy {
        outlineIcon(
            "Habits",
            "M12 8v11M12 10C10 6 5 5 4.5 7.5S6 13 9.5 12.5C7 14 6.5 17 8.5 17.5S11.5 15 12 13" +
                "M12 10c2-4 7-5 7.5-2.5S18 13 14.5 12.5c2.5 1.5 3 4.5 1 5S12.5 15 12 13",
            strokeWidth = 2.3f
        )
    }
    val Water: ImageVector by lazy {
        outlineIcon("Water", "M12 3.5s6 6.3 6 10.5a6 6 0 0 1-12 0c0-4.2 6-10.5 6-10.5z", strokeWidth = 2.3f)
    }
    val Goals: ImageVector by lazy {
        outlineIcon("Goals", "M6 21V4M6 4h11l-2.5 4 2.5 4H6", strokeWidth = 2.3f)
    }
    val Stretching: ImageVector by lazy {
        outlineIcon("Stretching", "M14 4h6v6M10 20H4v-6M20 4l-6.5 6.5M4 20l6.5-6.5", strokeWidth = 2.3f)
    }

    // Привычки: к иконкам трекеров из макета (росток, вода, флажок, растяжка) — свои, в том же стиле
    val Run: ImageVector by lazy {
        outlineIcon(
            "Run",
            "M14.5 4.5a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0-3 0M15 8l-4 1.5-2.5 3M12.5 9l-1.5 5 3.5 2.5-1 4.5" +
                "M11 14l-3.5 3.5H4.5M14.5 11.5l2.5 1.5 2.5-1",
            strokeWidth = 2.3f
        )
    }
    val Book: ImageVector by lazy {
        outlineIcon("Book", "M12 6.5C10 5 7 4.5 4 5v13c3-.5 6 0 8 1.5 2-1.5 5-2 8-1.5V5c-3-.5-6 0-8 1.5zM12 6.5v13", strokeWidth = 2.3f)
    }
    val Pill: ImageVector by lazy {
        outlineIcon("Pill", "M10.5 20a5 5 0 0 1-7-7l6-6a5 5 0 0 1 7 7zM7 10.5l6.5 6.5", strokeWidth = 2.3f)
    }
    val Sleep: ImageVector by lazy {
        outlineIcon("Sleep", "M19.5 14.5A8 8 0 1 1 9.5 4.5a6.5 6.5 0 0 0 10 10z", strokeWidth = 2.3f)
    }
    val Tooth: ImageVector by lazy {
        outlineIcon(
            "Tooth",
            "M8 3.5C5.5 3.5 4 5.5 4.5 8.5c.4 2.4 1.4 3.8 1.8 6.3.4 2.7.9 5.7 2.4 5.7s1.6-4 3.3-4 1.8 4 3.3 4 2-3 2.4-5.7" +
                "c.4-2.5 1.4-3.9 1.8-6.3.5-3-1-5-3.5-5-1.8 0-2.6.9-4 .9S9.8 3.5 8 3.5z",
            strokeWidth = 2.3f
        )
    }
    val Shower: ImageVector by lazy {
        outlineIcon(
            "Shower",
            "M5 21V7.5a3.5 3.5 0 0 1 7 0M8.5 10h7a3.5 3.5 0 0 0-7 0zM10 13.5v.5M12 13.5v.5M14 13.5v.5M11 17v.5M13 17v.5M12 20v.5",
            strokeWidth = 2.3f
        )
    }
    val Heart: ImageVector by lazy {
        outlineIcon("Heart", "M12 20s-7.5-4.5-7.5-10A4.5 4.5 0 0 1 12 7a4.5 4.5 0 0 1 7.5 3c0 5.5-7.5 10-7.5 10z", strokeWidth = 2.3f)
    }
    val Sun: ImageVector by lazy {
        outlineIcon(
            "Sun",
            "M12 8a4 4 0 1 0 0 8a4 4 0 1 0 0-8zM12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4" +
                "M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4",
            strokeWidth = 2.3f
        )
    }
    val Minus: ImageVector by lazy { outlineIcon("Minus", "M5 12h14", strokeWidth = 2.4f) }

    // Быстрые входы
    val Categories: ImageVector by lazy {
        outlineIcon("Categories", "M9 6h11M9 12h11M9 18h11M4.5 6h.01M4.5 12h.01M4.5 18h.01")
    }
    val Calendar: ImageVector by lazy { outlineIcon("Calendar", CalendarPath) }
    val Search: ImageVector by lazy { outlineIcon("Search", "M11 4a7 7 0 1 0 0 14a7 7 0 1 0 0-14zM20 20l-4-4") }

    // Группы задач
    val NoDate: ImageVector by lazy {
        outlineIcon("NoDate", "M3 13h5l1.5 3h5l1.5-3h5M5.5 5h13L21 13v6a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-6z")
    }
    val Overdue: ImageVector by lazy {
        outlineIcon("Overdue", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM12 7.5v5M12 16.3v.1", strokeWidth = 2.2f)
    }
    val Today: ImageVector by lazy {
        outlineIcon(
            "Today",
            "M12 8a4 4 0 1 0 0 8a4 4 0 1 0 0-8zM12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2" +
                "M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4",
            strokeWidth = 2.2f
        )
    }
    val Done: ImageVector by lazy {
        outlineIcon("Done", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM8.5 12.2l2.4 2.4 4.6-4.8")
    }
    val Tomorrow: ImageVector by lazy { outlineIcon("Tomorrow", CalendarPath + "M10 14.5h5M13 12.5l2 2-2 2") }
    val ThisWeek: ImageVector by lazy { outlineIcon("ThisWeek", CalendarPath + "M8 14h2M14 14h2M8 17h2") }
    val ThisMonth: ImageVector by lazy {
        outlineIcon("ThisMonth", CalendarPath + "M8 13.5h.5M12 13.5h.5M16 13.5h.5M8 17h.5M12 17h.5M16 17h.5")
    }
    val Later: ImageVector by lazy {
        outlineIcon("Later", "M12 3l9 4.5-9 4.5-9-4.5 9-4.5zM3 12l9 4.5 9-4.5M3 16.5l9 4.5 9-4.5")
    }

    // Нижние кнопки
    val Microphone: ImageVector by lazy {
        outlineIcon("Microphone", "M12 3a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3zM5.5 11a6.5 6.5 0 0 0 13 0M12 17.5V21")
    }
    val Add: ImageVector by lazy { outlineIcon("Add", "M12 5v14M5 12h14", strokeWidth = 2.4f) }

    // Верхняя панель списка задач
    val Back: ImageVector by lazy { outlineIcon("Back", "M19 12H5M11 6l-6 6 6 6") }
    val Sort: ImageVector by lazy { outlineIcon("Sort", "M7 4v16M3.5 16.5L7 20l3.5-3.5M14 6h7M14 11h5M14 16h3") }
    val More: ImageVector by lazy { outlineIcon("More", "M12 5.5v.01M12 12v.01M12 18.5v.01", strokeWidth = 2.6f) }

    // Строка задачи
    val Check: ImageVector by lazy { outlineIcon("Check", "M5 12.5l4.5 4.5L19 7.5", strokeWidth = 3.2f) }
    val Clock: ImageVector by lazy { outlineIcon("Clock", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM12 7.5V12l3 2") }
    val Subtasks: ImageVector by lazy {
        outlineIcon("Subtasks", "M10 6h10M10 12h10M10 18h10M4 6l1.3 1.3L7.5 5M4 12l1.3 1.3 2.2-2.3M4 18h3")
    }
    val Repeat: ImageVector by lazy {
        outlineIcon("Repeat", "M17 3l3 3-3 3M4 11V9.5A3.5 3.5 0 0 1 7.5 6H20M7 21l-3-3 3-3M20 13v1.5a3.5 3.5 0 0 1-3.5 3.5H4")
    }
    val Reminder: ImageVector by lazy {
        outlineIcon("Reminder", "M6 9.5a6 6 0 0 1 12 0c0 5.5 2.5 7 2.5 7h-17S6 15 6 9.5zM10 20a2 2 0 0 0 4 0")
    }
    val PriorityFlag: ImageVector by lazy {
        ImageVector.Builder("PriorityFlag", 24.dp, 24.dp, 24f, 24f)
            .addOutline(PriorityFlagPath, strokeWidth = 1.8f, filled = true)
            .build()
    }
    val PriorityFlagOutline: ImageVector by lazy { outlineIcon("PriorityFlagOutline", PriorityFlagPath) }

    // Панель новой задачи
    val Close: ImageVector by lazy { outlineIcon("Close", "M6 6l12 12M18 6L6 18") }
    val Tag: ImageVector by lazy { outlineIcon("Tag", "M3 12V4h8l10 10-8 8zM7.5 7.5v.01") }
    val ChevronRight: ImageVector by lazy { outlineIcon("ChevronRight", "M9 6l6 6-6 6") }

    // Карточка задачи
    val Delete: ImageVector by lazy { outlineIcon("Delete", "M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3") }
    val Postpone: ImageVector by lazy { outlineIcon("Postpone", "M12 3a9 9 0 1 0 9 9M12 7.5V12l3 2M16 3h5v5") }
    val DragHandle: ImageVector by lazy { outlineIcon("DragHandle", "M5 9h14M5 15h14") }
    val Pause: ImageVector by lazy { outlineIcon("Pause", "M9 6v12M15 6v12") }

    // Категории: первые шесть из макета «Категории», остальные нарисованы в том же стиле
    val Work: ImageVector by lazy {
        outlineIcon(
            "Work",
            "M4 8h16a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1zM9 8V5.5A1.5 1.5 0 0 1 10.5 4h3A1.5 1.5 0 0 1 15 5.5V8M3 13h18"
        )
    }
    val Personal: ImageVector by lazy { outlineIcon("Personal", "M12 4a4 4 0 1 0 0 8a4 4 0 1 0 0-8zM4.5 20a7.5 7.5 0 0 1 15 0") }
    val Home: ImageVector by lazy { outlineIcon("Home", "M4 11l8-7 8 7M6 9.5V20h12V9.5M10 20v-5h4v5") }
    val Health: ImageVector by lazy { outlineIcon("Health", "M3 12h4l3-7 4 14 3-7h4") }
    val Study: ImageVector by lazy { outlineIcon("Study", "M5 5a2 2 0 0 1 2-2h12v15H7a2 2 0 0 0-2 2zM5 20V5M9 7h6") }
    val Finance: ImageVector by lazy {
        outlineIcon("Finance", "M4 7h15a1 1 0 0 1 1 1v11a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V6a2 2 0 0 1 2-2h11v3M16 13.5h.01")
    }
    val Shopping: ImageVector by lazy { outlineIcon("Shopping", "M3 4h2.5l2.2 10.5h10.6L20.5 7H6.6M9.5 19.5h.01M17 19.5h.01") }
    val Car: ImageVector by lazy {
        outlineIcon("Car", "M5 17v-5.5L7 6.5h10l2 5V17M5 17h14M5 17v2M19 17v2M3.5 11.5h17M8 14h.01M16 14h.01")
    }
    val Travel: ImageVector by lazy { outlineIcon("Travel", "M21 3L3 10.5l7 2.5 2.5 7zM10 13L21 3") }
    val Gift: ImageVector by lazy {
        outlineIcon("Gift", "M4 11h16v9H4zM3 7.5h18V11H3zM12 7.5V20M12 7.5C11 4.5 7.5 4 7.5 6s3.5 1.5 4.5 1.5M12 7.5c1-3 4.5-3.5 4.5-1.5S13 7.5 12 7.5")
    }
    val Star: ImageVector by lazy {
        outlineIcon("Star", "M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8-5.2-2.7-5.2 2.7 1-5.8-4.3-4.1 5.9-.9z")
    }
    val Phone: ImageVector by lazy {
        outlineIcon("Phone", "M8 3h8a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1zM11 18h2")
    }
    val Code: ImageVector by lazy { outlineIcon("Code", "M8 8l-4 4 4 4M16 8l4 4-4 4M13.5 5l-3 14") }
    val Music: ImageVector by lazy {
        outlineIcon("Music", "M9 18V5l11-2v13M9 18a3 3 0 1 1-6 0a3 3 0 1 1 6 0zM20 16a3 3 0 1 1-6 0a3 3 0 1 1 6 0z")
    }
}

private const val PriorityFlagPath = "M5 21V4M5 4h11.5l-2.2 4.2 2.2 4.3H5"

private const val CalendarPath =
    "M8 3v3M16 3v3M4 9.5h16M6 5h12a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z"

private fun outlineIcon(name: String, pathData: String, strokeWidth: Float = 2f): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addOutline(pathData, strokeWidth = strokeWidth)
        .build()

private fun ImageVector.Builder.addOutline(
    pathData: String,
    strokeWidth: Float = 2f,
    filled: Boolean = false
): ImageVector.Builder = addPath(
    pathData = addPathNodes(pathData),
    fill = if (filled) SolidColor(Color.Black) else null,
    stroke = SolidColor(Color.Black),
    strokeLineWidth = strokeWidth,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round
)
