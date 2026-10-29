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
    // Карандаш из макета «Категории»
    val Edit: ImageVector by lazy { outlineIcon("Edit", "M4 20h4L19 9l-4-4L4 16zM14 6l4 4") }
    // Коробка «Архив» из макета «Категории»
    val Archive: ImageVector by lazy { outlineIcon("Archive", "M4 5h16v4H4zM5.5 9v10h13V9M10 13h4") }
    // «Слушать» и значок видео на миниатюре
    val Play: ImageVector by lazy { outlineIcon("Play", "M8 5.5v13l10.5-6.5z") }
    // Картинка — «из галереи»
    val Gallery: ImageVector by lazy { outlineIcon("Gallery", "M4 5h16v14H4zM4 16l4.5-4.5 3.5 3.5 2.5-2.5L20 18M15 8.5h.01") }
    // Скрепка — вложения задачи; нарисована в том же стиле
    val Attachment: ImageVector by lazy {
        outlineIcon("Attachment", "M15.5 7.5l-6.8 6.8a2 2 0 0 0 2.8 2.8l7-7a4 4 0 0 0-5.7-5.7l-7 7a6 6 0 0 0 8.5 8.5l6.3-6.3")
    }
    // Обновление приложения: стрелка вниз в лоток, нарисована в том же стиле
    val Update: ImageVector by lazy { outlineIcon("Update", "M12 4v11M7 10l5 5 5-5M5 20h14") }

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

    // Иконки категорий, добавленные 29.10.2026, в том же стиле: контур 2 dp, скруглённые концы, сетка 24×24
    val CategoryCoffee: ImageVector by lazy { outlineIcon("CategoryCoffee", "M5 9h11v5a5 5 0 0 1-5 5h-1a5 5 0 0 1-5-5zM16 10h1.5a2.5 2.5 0 0 1 0 5H16M8 3.5v2M11 3v2.5M14 3.5v2") }
    val CategoryFood: ImageVector by lazy { outlineIcon("CategoryFood", "M7 3v18M4.5 3v5a2.5 2.5 0 0 0 5 0V3M17 21V3c-2.2 1-3.5 3.5-3.5 7v3H17") }
    val CategoryFitness: ImageVector by lazy { outlineIcon("CategoryFitness", "M6.5 6.5v11M17.5 6.5v11M3.5 9v6M20.5 9v6M6.5 12h11") }
    val CategoryBike: ImageVector by lazy { outlineIcon("CategoryBike", "M2 17.5a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0zM15 17.5a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0zM5.5 17.5L9 9h6l3.5 8.5M9 9l3 8.5M8 6h3M15 9l-1-3h2.5") }
    val CategoryMedical: ImageVector by lazy { outlineIcon("CategoryMedical", "M9.5 3.5h5v6h6v5h-6v6h-5v-6h-6v-5h6z") }
    val CategoryUmbrella: ImageVector by lazy { outlineIcon("CategoryUmbrella", "M12 3a9 9 0 0 1 9 9H3a9 9 0 0 1 9-9zM12 12v6.5a2 2 0 0 1-4 0") }
    val CategoryCloud: ImageVector by lazy { outlineIcon("CategoryCloud", "M7 18a4 4 0 0 1-.5-8 5.5 5.5 0 0 1 10.6-1.5A4.5 4.5 0 0 1 17 18z") }
    val CategoryLeaf: ImageVector by lazy { outlineIcon("CategoryLeaf", "M5 19C5 10 10 5 20 4c0 9-5 15-12 15zM5 19c3-4 6-7 10-10") }
    val CategoryPlant: ImageVector by lazy { outlineIcon("CategoryPlant", "M7 14h10l-1.5 7h-7zM12 14V8M12 10c-3 0-4.5-2-4.5-5 3 0 4.5 2 4.5 5zM12 8c0-3 1.5-5 4.5-5 0 3-1.5 5-4.5 5z") }
    val CategoryPet: ImageVector by lazy { outlineIcon("CategoryPet", "M12 13c-3 0-5.5 3-5.5 5 0 1.5 1.5 2 2.5 2 1.2 0 2-.8 3-.8s1.8.8 3 .8c1 0 2.5-.5 2.5-2 0-2-2.5-5-5.5-5zM4.5 10a1.5 2 0 1 0 3 0a1.5 2 0 1 0-3 0zM16.5 10a1.5 2 0 1 0 3 0a1.5 2 0 1 0-3 0zM8 6a1.5 2 0 1 0 3 0a1.5 2 0 1 0-3 0zM13 6a1.5 2 0 1 0 3 0a1.5 2 0 1 0-3 0z") }
    val CategoryFamily: ImageVector by lazy { outlineIcon("CategoryFamily", "M9 4a3 3 0 1 0 0 6a3 3 0 1 0 0-6zM3 20a6 6 0 0 1 12 0M16.5 6.5a2.5 2.5 0 1 1 0 5M17.5 14a5 5 0 0 1 3.5 6") }
    val CategorySmile: ImageVector by lazy { outlineIcon("CategorySmile", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM8.5 14.5c1.8 2 5.2 2 7 0M9 9.5h.01M15 9.5h.01") }
    val CategoryCake: ImageVector by lazy { outlineIcon("CategoryCake", "M4 21h16M5 21v-7h14v7M5 16.5c1.2 1 2.3 1 3.5 0s2.3-1 3.5 0 2.3 1 3.5 0 2.3-1 3.5 0M12 14v-3M12 8.5c-.8-.6-1-1.6 0-3 1 1.4.8 2.4 0 3z") }
    val CategoryIdea: ImageVector by lazy { outlineIcon("CategoryIdea", "M9 18h6M10 21h4M12 3a6 6 0 0 0-3.5 10.9c.8.6 1.5 1.6 1.5 2.6V17h4v-.5c0-1 .7-2 1.5-2.6A6 6 0 0 0 12 3z") }
    val CategoryTarget: ImageVector by lazy { outlineIcon("CategoryTarget", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM12 7a5 5 0 1 0 0 10a5 5 0 1 0 0-10zM12 11a1 1 0 1 0 0 2a1 1 0 1 0 0-2z") }
    val CategoryTrophy: ImageVector by lazy { outlineIcon("CategoryTrophy", "M8 4h8v6a4 4 0 0 1-8 0zM8 6H5v1.5A3 3 0 0 0 8 10.5M16 6h3v1.5a3 3 0 0 1-3 3M12 14v3M10 17h4v3h-4zM8.5 20h7") }
    val CategoryMail: ImageVector by lazy { outlineIcon("CategoryMail", "M4 6h16a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1zM3.5 7l8.5 6 8.5-6") }
    val CategoryChat: ImageVector by lazy { outlineIcon("CategoryChat", "M5 5h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-8l-5 4v-4H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z") }
    val CategoryLaptop: ImageVector by lazy { outlineIcon("CategoryLaptop", "M5 5h14a1 1 0 0 1 1 1v9H4V6a1 1 0 0 1 1-1zM2.5 18.5h19l-1.5-3.5H4z") }
    val CategoryBuilding: ImageVector by lazy { outlineIcon("CategoryBuilding", "M5 21V4h10v17M15 9h4v12M3 21h18M8 8h.01M12 8h.01M8 12h.01M12 12h.01M8 16h.01M12 16h.01") }
    val CategoryTools: ImageVector by lazy { outlineIcon("CategoryTools", "M15 4a5 5 0 0 0-4.6 6.9L4 17.3a1.9 1.9 0 0 0 2.7 2.7l6.4-6.4A5 5 0 0 0 20 9l-3 3-3-1-1-3 3-3z") }
    val CategoryKey: ImageVector by lazy { outlineIcon("CategoryKey", "M8 11a4 4 0 1 0 0 8a4 4 0 1 0 0-8zM11 12l9-9M17 6l2.5 2.5M14.5 8.5l2 2") }
    val CategoryLock: ImageVector by lazy { outlineIcon("CategoryLock", "M6 11h12v9H6zM8.5 11V8a3.5 3.5 0 0 1 7 0v3") }
    val CategoryBus: ImageVector by lazy { outlineIcon("CategoryBus", "M6 4h12a2 2 0 0 1 2 2v11H4V6a2 2 0 0 1 2-2zM4 11h16M7 17v2.5M17 17v2.5M7.5 14h.01M16.5 14h.01") }
    val CategoryPlace: ImageVector by lazy { outlineIcon("CategoryPlace", "M12 21s-7-6-7-11.5a7 7 0 0 1 14 0C19 15 12 21 12 21zM12 7a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5z") }
    val CategoryGlobe: ImageVector by lazy { outlineIcon("CategoryGlobe", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM3 12h18M12 3c2.5 2.5 3.5 5.5 3.5 9s-1 6.5-3.5 9c-2.5-2.5-3.5-5.5-3.5-9s1-6.5 3.5-9z") }
    val CategoryCamera: ImageVector by lazy { outlineIcon("CategoryCamera", "M4 8h3l1.5-2.5h7L17 8h3a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1zM12 10a3.5 3.5 0 1 0 0 7a3.5 3.5 0 1 0 0-7z") }
    val CategoryArt: ImageVector by lazy { outlineIcon("CategoryArt", "M12 3a9 9 0 0 0 0 18c1.2 0 2-.8 2-1.8 0-1.4-1.2-1.7-1.2-3 0-1 .8-1.7 1.8-1.7H17a4 4 0 0 0 4-4C21 6.5 17 3 12 3zM7.5 11.5h.01M10 7.5h.01M15 7.5h.01") }
    val CategoryGame: ImageVector by lazy { outlineIcon("CategoryGame", "M7 8h10a4 4 0 0 1 4 4v1a4 4 0 0 1-7 2.6l-.5-.6h-3l-.5.6A4 4 0 0 1 3 13v-1a4 4 0 0 1 4-4zM7.5 10.5v3M6 12h3M15.5 11.5h.01M17.5 13h.01") }
    val CategoryMovie: ImageVector by lazy { outlineIcon("CategoryMovie", "M4 5h16v14H4zM4 9h16M4 15h16M8 5v4M16 5v4M8 15v4M16 15v4") }
    val CategoryMoney: ImageVector by lazy {
        outlineIcon(
            "CategoryMoney",
            "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM14.5 9c-.5-1-1.5-1.5-2.5-1.5-1.5 0-2.5.8-2.5 2s1 1.7 2.5 2 2.5.8 2.5 2-1 2-2.5 2c-1 0-2-.5-2.5-1.5M12 6v1.5M12 16.5V18"
        )
    }
    val CategoryClothes: ImageVector by lazy { outlineIcon("CategoryClothes", "M8 4l-5 3 2 4 2.5-1V20h9V10l2.5 1 2-4-5-3c-.5 1.5-2 2.5-4 2.5S8.5 5.5 8 4z") }
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
