package com.snax1k.chekki.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Категория задач. */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Номер цвета в палитре категорий темы, а не сам цвет: так цвет подстроится под светлую тему. */
    @ColumnInfo(name = "color_index") val colorIndex: Int,
    val icon: CategoryIcon = CategoryIcon.Tag,
    /** Порядок в списке категорий. */
    val position: Int,
    /** В архиве: не предлагается при выборе категории, задачи остаются. */
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false
)

/** Категория для экрана «Категории»: сколько задач не выполнено и как дела с задачами на сегодня. */
data class CategorySummary(
    @Embedded val category: CategoryEntity,
    /** Невыполненные задачи категории, с датой и без. */
    @ColumnInfo(name = "open_count") val openCount: Int,
    /** Задачи категории на сегодня, включая выполненные. */
    @ColumnInfo(name = "today_total") val todayTotal: Int,
    @ColumnInfo(name = "today_done") val todayDone: Int
)

/**
 * Иконки категорий. У значений фиксированные коды для базы, как у иконок привычек:
 * коды менять нельзя, новые иконки — только с новыми кодами.
 */
enum class CategoryIcon(val code: Int) {
    Work(1),
    Personal(2),
    Home(3),
    Health(4),
    Study(5),
    Finance(6),
    Shopping(7),
    Car(8),
    Travel(9),
    Gift(10),
    Heart(11),
    Star(12),
    Phone(13),
    Code(14),
    Music(15),
    Tag(16),

    // Добавлены 29.10.2026: часть нарисована заново, часть — те же картинки, что у привычек и в интерфейсе
    Coffee(17),
    Food(18),
    Fitness(19),
    Run(20),
    Bike(21),
    Water(22),
    Pill(23),
    Medical(24),
    Tooth(25),
    Sleep(26),
    Umbrella(27),
    Sun(28),
    Cloud(29),
    Leaf(30),
    Plant(31),
    Pet(32),
    Family(33),
    Smile(34),
    Cake(35),
    Book(36),
    Idea(37),
    Target(38),
    Flag(39),
    Trophy(40),
    Calendar(41),
    Clock(42),
    Bell(43),
    Mail(44),
    Chat(45),
    Laptop(46),
    Pen(47),
    Building(48),
    Tools(49),
    Key(50),
    Lock(51),
    Bus(52),
    Place(53),
    Globe(54),
    Camera(55),
    Art(56),
    Game(57),
    Movie(58),
    Clothes(59),
    Money(60);

    companion object {
        /** Иконка по коду; незнакомый код (например, из более новой версии) — ярлык. */
        fun fromCode(code: Int): CategoryIcon = entries.firstOrNull { it.code == code } ?: Tag
    }
}
