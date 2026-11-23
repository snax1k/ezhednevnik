package com.snax1k.chekki.icon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.content.edit
import com.snax1k.chekki.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.Month

/**
 * Время года для иконки и маскота (решено 23.11.2026): по календарю — декабрь–февраль новогодняя,
 * март–май весенняя, июнь–август летняя, сентябрь–ноябрь осенняя.
 */
enum class Season(@DrawableRes val mascot: Int, val aliasName: String) {
    Winter(R.drawable.mascot_winter, "com.snax1k.chekki.LauncherWinter"),
    Spring(R.drawable.mascot_spring, "com.snax1k.chekki.LauncherSpring"),
    Summer(R.drawable.mascot_summer, "com.snax1k.chekki.LauncherSummer"),
    Autumn(R.drawable.mascot_autumn, "com.snax1k.chekki.LauncherAutumn");

    companion object {
        fun of(date: LocalDate): Season = when (date.month) {
            Month.DECEMBER, Month.JANUARY, Month.FEBRUARY -> Winter
            Month.MARCH, Month.APRIL, Month.MAY -> Spring
            Month.JUNE, Month.JULY, Month.AUGUST -> Summer
            Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER -> Autumn
        }
    }
}

/** Вход с обычной иконкой (маскот без наряда) — когда сезонная иконка выключена. */
const val DEFAULT_LAUNCHER_ALIAS = "com.snax1k.chekki.LauncherDefault"

/** Обычный маскот для шапки меню. */
@DrawableRes
val DEFAULT_MASCOT: Int = R.drawable.mascot_default

/** Какой сезон показывать: со включённой сезонной иконкой — по дате, с выключенной — никакой (обычная иконка). */
fun shownSeason(enabled: Boolean, today: LocalDate): Season? = if (enabled) Season.of(today) else null

/** Состояние для меню ☰: включена ли сезонная иконка и какой сезон сейчас; null — обычная иконка. */
data class SeasonalIconState(val enabled: Boolean, val season: Season?)

/**
 * Сезонная иконка приложения. В манифесте пять входов (`activity-alias` на MainActivity): четыре сезонных
 * и обычный; включён всегда один — его иконку и показывает рабочий стол. Переключается при выходе из приложения
 * ([MainActivity.onStop]) и после перезагрузки, смены времени и обновления ([RescheduleReceiver]):
 * не при открытом экране, потому что некоторые лаунчеры при смене входа закрывают приложение.
 * Фоновой работы нет: новый сезон наступает на иконке после первого выхода из приложения в этом сезоне.
 * Выключили переключатель в меню ☰ — обычная иконка, как до сезонных (решено 23.11.2026).
 */
class SeasonalIcon(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(readState(LocalDate.now()))
    val state: StateFlow<SeasonalIconState> = _state.asStateFlow()

    /** Переключатель «Сезонная иконка» в меню ☰. Иконка на рабочем столе сменится при выходе из приложения. */
    fun setEnabled(enabled: Boolean, today: LocalDate = LocalDate.now()) {
        preferences.edit { putBoolean(KEY_ENABLED, enabled) }
        _state.value = readState(today)
    }

    /** Пересчитать сезон (например, при открытии приложения в новый день). */
    fun refresh(today: LocalDate = LocalDate.now()) {
        _state.value = readState(today)
    }

    /** Включает вход с нужной иконкой и выключает остальные; если уже так — ничего не делает. */
    fun applyToLauncher(today: LocalDate = LocalDate.now()) {
        val state = readState(today)
        _state.value = state
        val target = state.season?.aliasName ?: DEFAULT_LAUNCHER_ALIAS
        val packageManager = appContext.packageManager
        try {
            // Сначала включаем нужный вход, потом выключаем остальные: у приложения всегда есть хотя бы один
            setAlias(packageManager, target, enabled = true)
            ALL_ALIASES.filter { it != target }.forEach { setAlias(packageManager, it, enabled = false) }
        } catch (e: Exception) {
            Log.w(TAG, "Иконка не переключилась", e)
        }
    }

    private fun setAlias(packageManager: PackageManager, aliasName: String, enabled: Boolean) {
        val component = ComponentName(appContext, aliasName)
        // Лишний раз не трогаем: каждое переключение заставляет лаунчер перечитать иконки
        if (isEnabled(packageManager, component) == enabled) return
        val wanted = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        packageManager.setComponentEnabledSetting(component, wanted, PackageManager.DONT_KILL_APP)
    }

    /** Включён ли вход сейчас; пока его не переключали, — как в манифесте. */
    private fun isEnabled(packageManager: PackageManager, component: ComponentName): Boolean =
        when (packageManager.getComponentEnabledSetting(component)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT ->
                packageManager.getActivityInfo(component, PackageManager.MATCH_DISABLED_COMPONENTS).enabled
            else -> false
        }

    private fun readState(today: LocalDate): SeasonalIconState {
        val enabled = preferences.getBoolean(KEY_ENABLED, true)
        return SeasonalIconState(enabled, shownSeason(enabled, today))
    }

    private companion object {
        const val TAG = "SeasonalIcon"
        const val PREFERENCES = "seasonal_icon"
        const val KEY_ENABLED = "enabled"
        val ALL_ALIASES = Season.entries.map { it.aliasName } + DEFAULT_LAUNCHER_ALIAS
    }
}
