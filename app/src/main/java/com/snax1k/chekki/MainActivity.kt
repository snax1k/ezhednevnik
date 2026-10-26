package com.snax1k.chekki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.snax1k.chekki.ui.navigation.ChekkiApp
import com.snax1k.chekki.ui.theme.ChekkiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Тема всегда тёмная, поэтому значки в системных панелях всегда светлые
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            ChekkiTheme {
                ChekkiApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Проверка обновления — только при открытом приложении, не чаще раза в 6 часов
        (application as ChekkiApplication).container.updateManager.onAppStart()
    }

    override fun onResume() {
        super.onResume()
        // Вернулись из настроек, где разрешили установку обновлений, — продолжаем
        (application as ChekkiApplication).container.updateManager.onResume()
    }
}
