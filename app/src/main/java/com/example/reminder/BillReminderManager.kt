package com.example.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.entity.RecurringExpense
import java.util.Calendar
import java.util.Locale

object BillReminderManager {
    const val CHANNEL_ID = "finsay_bill_reminders_channel"
    const val ACTION_BILL_REMINDER = "com.example.reminder.ACTION_BILL_REMINDER"
    const val EXTRA_REMINDER_ID = "EXTRA_BILL_REMINDER_ID"
    const val EXTRA_REMINDER_TITLE = "EXTRA_BILL_REMINDER_TITLE"
    const val EXTRA_REMINDER_AMOUNT = "EXTRA_BILL_REMINDER_AMOUNT"
    const val EXTRA_REMINDER_DAY = "EXTRA_BILL_REMINDER_DAY"
    const val EXTRA_REMINDER_HOUR = "EXTRA_BILL_REMINDER_HOUR"
    const val EXTRA_REMINDER_MINUTE = "EXTRA_BILL_REMINDER_MINUTE"
    private const val REQUEST_CODE_BASE = 30000
    private const val TAG = "BillReminderManager"

    fun createNotificationChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Υπενθυμίσεις Πάγιων Εξόδων",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Ειδοποιήσεις για προγραμματισμένους λογαριασμούς και πάγια έξοδα"
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
                nm.createNotificationChannel(channel)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "createNotificationChannel warning: ${t.message}")
        }
    }

    fun calculateNextTriggerTimeMillis(
        dayOfMonth: Int,
        hour: Int,
        minute: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val safeDay = dayOfMonth.coerceIn(1, 31)
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)

        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_MONTH, safeDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
            set(Calendar.HOUR_OF_DAY, safeHour)
            set(Calendar.MINUTE, safeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= nowMillis) {
            cal.add(Calendar.MONTH, 1)
            cal.set(Calendar.DAY_OF_MONTH, safeDay.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
            cal.set(Calendar.HOUR_OF_DAY, safeHour)
            cal.set(Calendar.MINUTE, safeMinute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun scheduleReminder(context: Context, reminder: RecurringExpense) {
        try {
            if (!reminder.isActive || reminder.id <= 0L) {
                cancelReminder(context, reminder.id)
                return
            }
            createNotificationChannel(context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val intent = Intent(context, BillReminderReceiver::class.java).apply {
                action = ACTION_BILL_REMINDER
                putExtra(EXTRA_REMINDER_ID, reminder.id)
                putExtra(EXTRA_REMINDER_TITLE, reminder.title)
                putExtra(EXTRA_REMINDER_AMOUNT, reminder.amount)
                putExtra(EXTRA_REMINDER_DAY, reminder.dayOfMonth)
                putExtra(EXTRA_REMINDER_HOUR, reminder.reminderHour)
                putExtra(EXTRA_REMINDER_MINUTE, reminder.reminderMinute)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val requestCode = REQUEST_CODE_BASE + (reminder.id % 10000L).toInt()
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags) ?: return
            val triggerAt = calculateNextTriggerTimeMillis(
                dayOfMonth = reminder.dayOfMonth,
                hour = reminder.reminderHour,
                minute = reminder.reminderMinute
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
                Log.d(TAG, "Scheduled reminder '${reminder.title}' (#${reminder.id}) for day ${reminder.dayOfMonth} at %02d:%02d".format(reminder.reminderHour, reminder.reminderMinute))
            } catch (t: Throwable) {
                Log.w(TAG, "Failed exact alarm for reminder #${reminder.id}: ${t.message}")
                try {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                } catch (_: Throwable) {}
            }
        } catch (t: Throwable) {
            Log.w(TAG, "scheduleReminder warning: ${t.message}")
        }
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        if (reminderId <= 0L) return
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, BillReminderReceiver::class.java).apply {
                action = ACTION_BILL_REMINDER
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_NO_CREATE
            }
            val requestCode = REQUEST_CODE_BASE + (reminderId % 10000L).toInt()
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "cancelReminder warning: ${t.message}")
        }
    }

    fun showBillNotification(
        context: Context,
        reminderId: Long,
        title: String,
        amount: Double,
        dayOfMonth: Int
    ) {
        try {
            createNotificationChannel(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != 0
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS not granted.")
                return
            }

            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_REMINDER_ID, reminderId)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val tapPendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE_BASE + (reminderId % 10000L).toInt(),
                tapIntent,
                flags
            )

            val notifTitle = "Υπενθύμιση: $title"
            val notifBody = "${String.format(Locale.GERMANY, "%.2f €", amount)} • Κάθε ${dayOfMonth}η του μήνα • Πατήστε για καταγραφή"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_oikonomos_logo_white)
                .setContentTitle(notifTitle)
                .setContentText(notifBody)
                .setStyle(NotificationCompat.BigTextStyle().bigText(notifBody))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(tapPendingIntent)
                .setVibrate(longArrayOf(0, 250, 200, 250))
                .build()

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            nm.notify(REQUEST_CODE_BASE + (reminderId % 10000L).toInt(), notification)
        } catch (t: Throwable) {
            Log.w(TAG, "showBillNotification warning: ${t.message}")
        }
    }
}
