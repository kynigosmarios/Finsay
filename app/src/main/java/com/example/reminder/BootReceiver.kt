package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootReceiver", "System event ($action) received. Rescheduling daily and bill reminders if enabled.")
            if (DailyReminderManager.isReminderEnabled(context)) {
                val hour = DailyReminderManager.getReminderHour(context)
                val minute = DailyReminderManager.getReminderMinute(context)
                DailyReminderManager.scheduleDailyReminder(context, hour, minute)
            }
            val pendingResult = goAsync()
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val db = com.example.data.AppDatabase.getDatabase(context)
                    val list = db.expenseDao().getAllRecurringExpensesList()
                    list.filter { it.isActive }.forEach { rec ->
                        BillReminderManager.scheduleReminder(context, rec)
                    }
                } catch (e: Exception) {
                    Log.w("BootReceiver", "Failed rescheduling bill reminders: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
