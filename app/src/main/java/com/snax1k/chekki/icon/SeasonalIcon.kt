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
 * март–май весенняя, июнь–август летняя, сентябрь–ноябрь осенняя. Коды фиксированные: хранятся в настройках.
 */
enum class Season(val code: Int, @DrawableRes val mascot: Int, val aliasName: String) {
    Winter(1, R.drawable.mascot_winter, "com.snax1k.chekki.LauncherWinter"),
    Spring(2, R.drawable.mascot_spring, "com.snax1k.chekki.LauncherSpring"),
    Summer(3, R.drawable.mascot_summer, "com.snax1k.chekki.LauncherSummer"),
    Autumn(4, R.drawable.mascot_autumn, "com.snax1k.chekki.LauncherAutumn");

    companion object {
        fun of(date: LocalDate): Season = when (date.month) {
            Month.DECEMBER, Month.JANUARY, Month.FEBRUARY -> Winter
            Month.MARCH, Month.APRIL, Month.MAY -> Spring
            Month.JUNE, Month.JULY, Month.AUGUST -> Summer
            Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER -> Autumn
        }

        fun fromCode(code: Int): Season? = entries.firstOrNull { it.code == code }
    }
}

/**
 * Какой сезон показывать: со включённой сезонной иконкой — по дате, с выключенной — тот, что был,
 * когда её выключили ([frozen]; обычной иконки без наряда нет).
 */
fun shownSeason(enabled: Boolean, frozen: Season?, today: LocalDate): Season =
    if (enabled) Season.of(today) else frozen ?: Season.of(today)

/** Состояние для меню ☰: включена ли сезонная иконка и какой маскот сейчас. */
data class SeasonalIconState(val enabled: Boolean, val season: Season)

/**
 * Сезонная иконка приложения. В манифесте четыре входа (`activity-alias` на MainActivity) со своими иконками,
 * включён всегда один — его иконку и показывает рабочий стол. Переключается при выходе из приложения
 * ([MainActivity.onStop]) и после перезагрузки, смены времени и обновления ([RescheduleReceiver]):
 * не при открытом экране, потому что некоторые лаунчеры при смене входа закрывают приложение.
 * Фоновой работы нет: новый сезон наступает на иконке после первого выхода из приложения в этом сезоне.
 */
class SeasonalIcon(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(readState(LocalDate.now()))
    val state: StateFlow<SeasonalIconState> = _state.asStateFlow()

    /** Переключатель «Сезонная иконка» в меню ☰. Выключили — сезон замирает на нынешнем. */
    fun setEnabled(enabled: Boolean, today: LocalDate = LocalDate.now()) {
        val current = readState(today)
        preferences.edit {
            putBoolean(KEY_ENABLED, enabled)
            if (enabled) remove(KEY_FROZEN) else putInt(KEY_FROZEN, current.season.code)
        }
        _state.value = readState(today)
    }

    /** Пересчитать сезон (например, при открытии приложения в новый день). */
    fun refresh(today: LocalDate = LocalDate.now()) {
        _state.value = readState(today)
    }

    /** Включает вход с иконкой нужного сезона и выключает остальные; если уже так — ничего не делает. */
    fun applyToLauncher(today: LocalDate = LocalDate.now()) {
        val target = readState(today).season
        _state.value = readState(today)
        val packageManager = appContext.packageManager
        try {
            // Сначала включаем нужный вход, потом выключаем остальные: у приложения всегда есть хотя бы один
            setAlias(packageManager, target, enabled = true)
            Season.entries.filter { it != target }.forEach { setAlias(packageManager, it, enabled = false) }
        } catch (e: Exception) {
            Log.w(TAG, "Иконка не переключилась", e)
        }
    }

    private fun setAlias(packageManager: PackageManager, season: Season, enabled: Boolean) {
        val component = ComponentName(appContext, season.aliasName)
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
        val frozen = Season.fromCode(preferences.getInt(KEY_FROZEN, 0))
        return SeasonalIconState(enabled, shownSeason(enabled, frozen, today))
    }

    private companion object {
        const val TAG = "SeasonalIcon"
        const val PREFERENCES = "seasonal_icon"
        const val KEY_ENABLED = "enabled"
        const val KEY_FROZEN = "frozen_season"
    }
}
