package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/**
 * Posts local Android notifications when another connected user modifies shared household data.
 * Respects POST_NOTIFICATIONS runtime permission on Android 13+ (API 33+).
 */
object HouseholdActivityNotificationManager {
    private const val CHANNEL_ID = "finsay_shared_activity_channel"
    private const val TAG = "HouseholdActivityNotif"

    fun createChannel(context: Context, isGreek: Boolean = true) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                if (isGreek) "Κοινή Δραστηριότητα Εξοδολογίου" else "Shared Household Activity",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = if (isGreek) {
                    "Ενημερώσεις όταν άλλα μέλη καταχωρούν ή τροποποιούν συναλλαγές"
                } else {
                    "Updates when other members log or edit shared transactions"
                }
                setShowBadge(true)
            }
            nm.createNotificationChannel(channel)
        } catch (t: Throwable) {
            Log.w(TAG, "createChannel warning: ${t.message}")
        }
    }

    fun notifyRemoteActivity(
        context: Context,
        householdName: String,
        actorName: String,
        summaryText: String,
        notificationId: Int,
        isGreek: Boolean = true
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            createChannel(context, isGreek)
            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(context, 42000 + (notificationId and 0x3FFF), tapIntent, flags)

            val titlePrefix = if (actorName.isNotBlank()) "$actorName • $householdName" else householdName
            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_oikonomos_logo_white)
                .setContentTitle(titlePrefix.ifBlank { "Finsay" })
                .setContentText(summaryText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            nm.notify(42000 + (notificationId and 0x3FFF), notif)
        } catch (t: Throwable) {
            Log.w(TAG, "notifyRemoteActivity warning: ${t.message}")
        }
    }
}
