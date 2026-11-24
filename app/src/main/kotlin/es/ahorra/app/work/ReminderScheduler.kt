package es.ahorra.app.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import es.ahorra.core.Settings
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Recordatorio diario con WorkManager (sobrevive a cierres de la app y reinicios del móvil).
 * No usa alarmas exactas ni permisos especiales: la hora es aproximada (Android puede retrasarla).
 */
object ReminderScheduler {
    private const val UNIQUE_NAME = "ahorra_daily_reminder"

    fun apply(context: Context, settings: Settings) {
        if (!settings.remindersEnabled) {
            cancel(context)
            return
        }
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(settings.reminderHour, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(Duration.between(now, next))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
    }
}
