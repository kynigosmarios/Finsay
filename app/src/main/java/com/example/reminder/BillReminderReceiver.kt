package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.entity.RecurringExpense

class BillReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val id = intent.getLongExtra(BillReminderManager.EXTRA_REMINDER_ID, 0L)
        val title = intent.getStringExtra(BillReminderManager.EXTRA_REMINDER_TITLE).orEmpty()
        val amount = intent.getDoubleExtra(BillReminderManager.EXTRA_REMINDER_AMOUNT, 0.0)
        val frequency = intent.getStringExtra(BillReminderManager.EXTRA_REMINDER_FREQUENCY)
            ?: com.example.util.RecurringScheduleHelper.FREQ_MONTHLY
        val day = intent.getIntExtra(BillReminderManager.EXTRA_REMINDER_DAY, 1)
        val anchorMonth = intent.getIntExtra(BillReminderManager.EXTRA_REMINDER_ANCHOR_MONTH, 0)
        val hour = intent.getIntExtra(BillReminderManager.EXTRA_REMINDER_HOUR, 9)
        val minute = intent.getIntExtra(BillReminderManager.EXTRA_REMINDER_MINUTE, 0)

        Log.d("BillReminderReceiver", "Alarm triggered for reminder #$id: $title [$frequency]")
        if (id > 0L && title.isNotBlank()) {
            BillReminderManager.showBillNotification(
                context = context,
                reminderId = id,
                title = title,
                amount = amount,
                dayOfMonth = day,
                frequency = frequency,
                anchorMonth = anchorMonth
            )
            BillReminderManager.scheduleReminder(
                context,
                RecurringExpense(
                    id = id,
                    title = title,
                    amount = amount,
                    categoryId = 0L,
                    categoryName = "",
                    frequency = frequency,
                    dayOfMonth = day,
                    reminderHour = hour,
                    reminderMinute = minute,
                    isActive = true,
                    anchorMonth = anchorMonth
                )
            )
        }
    }
}
