package com.snax1k.chekki.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat
import com.snax1k.chekki.ChekkiApplication

/**
 * Ответы Android на установку обновления ([UpdateManager.start]). Сначала приходит «нужно подтверждение» —
 * открываем окно «Обновить приложение?», потом итог. Получатель не открыт другим приложениям: сюда пишет
 * только наш PendingIntent.
 */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java) ?: return
            context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        (context.applicationContext as ChekkiApplication).container.updateManager.onInstallResult(status, message)
    }
}
