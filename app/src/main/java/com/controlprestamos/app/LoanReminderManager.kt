package com.controlprestamos.app

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.controlprestamos.MainActivity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

enum class ReminderDispatchResult {
    SENT,
    BLOCKED_DISABLED,
    BLOCKED_PERMISSION,
    BLOCKED_APP_NOTIFICATIONS,
    NOTHING_TO_NOTIFY,
    SKIPPED_DUPLICATE
}

object LoanReminderManager {
    private const val PREFS_NAME = "ControlPrestamosPrefs"
    private const val KEY_ENABLED = "loan_reminders_enabled"
    private const val KEY_HOUR = "loan_reminders_hour"
    private const val KEY_MINUTE = "loan_reminders_minute"
    private const val KEY_LAST_SENT_DATE = "loan_reminders_last_sent_date"
    private const val KEY_LAST_SENT_SIGNATURE = "loan_reminders_last_sent_signature"

    private const val CHANNEL_ID = "loan_due_reminders"
    private const val CHANNEL_NAME = "Recordatorios de cobro"
    private const val CHANNEL_DESC = "Avisos diarios de préstamos vencidos o próximos a vencer"

    private const val REMINDER_REQUEST_CODE = 4101
    private const val NOTIFICATION_ID = 4102
    private const val TEST_NOTIFICATION_ID = 4103
    private const val ACTION_DAILY_REMINDER = "com.controlprestamos.app.ACTION_DAILY_REMINDER"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun getHour(context: Context): Int =
        prefs(context).getInt(KEY_HOUR, 8).coerceIn(0, 23)

    fun getMinute(context: Context): Int =
        prefs(context).getInt(KEY_MINUTE, 0).coerceIn(0, 59)

    fun saveSettings(
        context: Context,
        enabled: Boolean,
        hour: Int,
        minute: Int
    ) {
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_HOUR, hour.coerceIn(0, 23))
            .putInt(KEY_MINUTE, minute.coerceIn(0, 59))
            .apply()
    }

    fun needsNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    fun hasNotificationRuntimePermission(context: Context): Boolean {
        return !needsNotificationPermission() ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun canPostNotifications(context: Context): Boolean {
        return hasNotificationRuntimePermission(context) && areNotificationsEnabled(context)
    }

    private fun buildReminderPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, LoanReminderReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
            `package` = context.packageName
        }
        return PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun scheduleDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildReminderPendingIntent(context)

        alarmManager.cancel(pendingIntent)

        if (!isEnabled(context)) return

        ensureChannel(context)

        val now = LocalDateTime.now()
        var nextTrigger = now
            .withHour(getHour(context))
            .withMinute(getMinute(context))
            .withSecond(0)
            .withNano(0)

        if (!nextTrigger.isAfter(now)) {
            nextTrigger = nextTrigger.plusDays(1)
        }

        val triggerMillis = nextTrigger
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildReminderPendingIntent(context))
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESC
        }

        manager.createNotificationChannel(channel)
    }

    fun resetDeliveryDedup(context: Context) {
        prefs(context).edit()
            .remove(KEY_LAST_SENT_DATE)
            .remove(KEY_LAST_SENT_SIGNATURE)
            .apply()
    }

    fun sendPreviewNotification(context: Context): ReminderDispatchResult {
        if (!hasNotificationRuntimePermission(context)) return ReminderDispatchResult.BLOCKED_PERMISSION
        if (!areNotificationsEnabled(context)) return ReminderDispatchResult.BLOCKED_APP_NOTIFICATIONS

        ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val openPendingIntent = PendingIntent.getActivity(
            context,
            5002,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Prueba de recordatorio")
            .setContentText("Las notificaciones de Control de Préstamos están funcionando.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Las notificaciones de Control de Préstamos están funcionando."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
        return ReminderDispatchResult.SENT
    }

    fun sendDailySummaryIfNeeded(context: Context): ReminderDispatchResult {
        if (!isEnabled(context)) return ReminderDispatchResult.BLOCKED_DISABLED
        if (!hasNotificationRuntimePermission(context)) return ReminderDispatchResult.BLOCKED_PERMISSION
        if (!areNotificationsEnabled(context)) return ReminderDispatchResult.BLOCKED_APP_NOTIFICATIONS

        val sessionStore = SessionStore(context)
        val activeLoans = sessionStore.readLoans().filter { it.status == "ACTIVO" }

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val overdueCount = activeLoans.count { loan ->
            runCatching { LocalDate.parse(loan.dueDate).isBefore(today) }.getOrDefault(false)
        }

        val dueTodayCount = activeLoans.count { loan ->
            runCatching { LocalDate.parse(loan.dueDate) == today }.getOrDefault(false)
        }

        val dueTomorrowCount = activeLoans.count { loan ->
            runCatching { LocalDate.parse(loan.dueDate) == tomorrow }.getOrDefault(false)
        }

        if (overdueCount + dueTodayCount + dueTomorrowCount == 0) {
            return ReminderDispatchResult.NOTHING_TO_NOTIFY
        }

        val signature = "$overdueCount|$dueTodayCount|$dueTomorrowCount"
        if (wasAlreadySentToday(context, signature)) {
            return ReminderDispatchResult.SKIPPED_DUPLICATE
        }

        ensureChannel(context)

        val title = when {
            overdueCount > 0 -> "Tienes $overdueCount cobros vencidos"
            dueTodayCount > 0 -> "Tienes $dueTodayCount cobros para hoy"
            else -> "Tienes $dueTomorrowCount cobros para mañana"
        }

        val parts = mutableListOf<String>()
        if (overdueCount > 0) parts.add("$overdueCount vencidos")
        if (dueTodayCount > 0) parts.add("$dueTodayCount para hoy")
        if (dueTomorrowCount > 0) parts.add("$dueTomorrowCount para mañana")

        val content = parts.joinToString(" · ")

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val openPendingIntent = PendingIntent.getActivity(
            context,
            5001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        markSentToday(context, signature)
        return ReminderDispatchResult.SENT
    }

    private fun wasAlreadySentToday(context: Context, signature: String): Boolean {
        val prefs = prefs(context)
        val lastDate = prefs.getString(KEY_LAST_SENT_DATE, "") ?: ""
        val lastSignature = prefs.getString(KEY_LAST_SENT_SIGNATURE, "") ?: ""
        return lastDate == LocalDate.now().toString() && lastSignature == signature
    }

    private fun markSentToday(context: Context, signature: String) {
        prefs(context).edit()
            .putString(KEY_LAST_SENT_DATE, LocalDate.now().toString())
            .putString(KEY_LAST_SENT_SIGNATURE, signature)
            .apply()
    }
}
