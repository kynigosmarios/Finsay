package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.util.Localization

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("DailyReminderReceiver", "Alarm triggered for daily expense check.")
        if (DailyReminderManager.isReminderEnabled(context)) {
            val langCode = try {
                context.getSharedPreferences("oikonomos_app_prefs", Context.MODE_PRIVATE)
                    .getString("app_language", Localization.Language.ENGLISH.code)
            } catch (_: Throwable) {
                Localization.Language.ENGLISH.code
            }
            val lang = Localization.Language.fromCode(langCode)
            DailyReminderManager.showNotification(
                context,
                Localization.tr(
                    lang,
                    "Did you log today's expenses?",
                    "Πέρασες τα σημερινά σου έξοδα;"
                ),
                Localization.tr(
                    lang,
                    "Type or speak them now so you don't forget 🎙️",
                    "Γράψε τα ή πες τα με τη φωνή σου για να μη τα ξεχάσεις 🎙️"
                )
            )
        }
        val hour = DailyReminderManager.getReminderHour(context)
        val minute = DailyReminderManager.getReminderMinute(context)
        DailyReminderManager.scheduleDailyReminder(context, hour, minute)
    }
}
