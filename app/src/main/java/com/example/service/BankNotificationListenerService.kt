package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.util.BankNotificationHolder
import com.example.util.BankNotificationParser
import com.example.util.BankNotificationSystemManager
import java.lang.ref.WeakReference

class BankNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        try {
            super.onListenerConnected()
            activeInstance = WeakReference(this)
            Log.d(TAG, "BankNotificationListenerService connected successfully")
            scanActiveNotificationsInternal()
        } catch (t: Throwable) {
            Log.w(TAG, "onListenerConnected warning: ${t.message}")
        }
    }

    override fun onListenerDisconnected() {
        try {
            super.onListenerDisconnected()
            activeInstance = null
            Log.d(TAG, "BankNotificationListenerService disconnected")
        } catch (t: Throwable) {
            Log.w(TAG, "onListenerDisconnected warning: ${t.message}")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            super.onNotificationPosted(sbn)
            if (sbn == null) return
            activeInstance = WeakReference(this)
            processStatusBarNotification(sbn, hasGroupChildren = false)
        } catch (t: Throwable) {
            Log.w(TAG, "onNotificationPosted warning: ${t.message}")
        }
    }

    fun scanActiveNotificationsInternal() {
        try {
            if (!BankNotificationSystemManager.isAutoCatcherActiveForPro(this)) return
            val active = activeNotifications ?: return

            // Identify groups that have concrete non-summary child notifications
            val groupsWithChildren = active
                .filter { (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 && !it.groupKey.isNullOrBlank() }
                .map { "${it.packageName}|${it.groupKey}" }
                .toSet()

            // Sort oldest first so newest ends up on top of the stack
            active.sortedBy { it.postTime }.forEach { sbn ->
                val isSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
                val groupId = "${sbn.packageName}|${sbn.groupKey}"
                val hasChildren = isSummary && !sbn.groupKey.isNullOrBlank() && groupsWithChildren.contains(groupId)
                processStatusBarNotification(sbn, hasGroupChildren = hasChildren)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error scanning active notifications: ${t.message}")
        }
    }

    private fun processStatusBarNotification(sbn: StatusBarNotification, hasGroupChildren: Boolean) {
        try {
            if (!BankNotificationSystemManager.isAutoCatcherActiveForPro(this)) {
                return
            }

            val packageName = sbn.packageName.orEmpty()
            if (packageName == this.packageName) return
            if (!BankNotificationParser.isBankPackage(packageName)) {
                return
            }

            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return
            val isSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

            if (isSummary && hasGroupChildren) {
                return
            }

            val title = (extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
                ?: extras.getCharSequence(Notification.EXTRA_TITLE))?.toString().orEmpty()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
            val fullText = listOf(bigText, text, subText).firstOrNull { it.isNotBlank() }.orEmpty()

            // If this is a standalone summary notification with InboxStyle lines, process each line
            val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            if (isSummary && textLines != null && textLines.isNotEmpty()) {
                var parsedAnyLine = false
                textLines.forEachIndexed { idx, line ->
                    val lineStr = line?.toString().orEmpty().trim()
                    if (lineStr.isNotBlank()) {
                        val lineSig = "$packageName|${sbn.id}|${sbn.postTime}|line$idx|${lineStr.take(60)}"
                        if (!BankNotificationHolder.isSignatureHandled(lineSig, this)) {
                            val parsedLine = BankNotificationParser.parse(
                                packageName = packageName,
                                title = title,
                                text = lineStr,
                                timestamp = sbn.postTime
                            )
                            if (parsedLine != null) {
                                parsedAnyLine = true
                                BankNotificationHolder.markSignatureHandled(lineSig, this)
                                val added = BankNotificationHolder.addTransaction(parsedLine, this)
                                if (added) {
                                    BankNotificationSystemManager.showNotification(this, parsedLine)
                                }
                            }
                        }
                    }
                }
                if (parsedAnyLine) return
            }

            val sig = "$packageName|${sbn.id}|${sbn.postTime}|${title.take(40)}|${fullText.take(80)}"
            if (BankNotificationHolder.isSignatureHandled(sig, this)) {
                return
            }

            val parsed = BankNotificationParser.parse(
                packageName = packageName,
                title = title,
                text = fullText,
                timestamp = sbn.postTime
            )

            if (parsed != null) {
                val txKind = if (parsed.isIncome) "INCOME" else "EXPENSE"
                Log.d(TAG, "Caught bank $txKind notification")
                BankNotificationHolder.markSignatureHandled(sig, this)
                val added = BankNotificationHolder.addTransaction(parsed, this)
                if (added) {
                    BankNotificationSystemManager.showNotification(this, parsed)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error processing notification: ${t.message}", t)
        }
    }

    companion object {
        private const val TAG = "BankNotification"

        @Volatile
        private var activeInstance: WeakReference<BankNotificationListenerService>? = null

        fun scanActiveNotifications(context: Context) {
            try {
                activeInstance?.get()?.scanActiveNotificationsInternal()
            } catch (t: Throwable) {
                Log.w(TAG, "scanActiveNotifications failed: ${t.message}")
            }
        }

        fun requestRebindIfPossible(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    val pkgName = context.packageName
                    val flat = Settings.Secure.getString(
                        context.contentResolver,
                        "enabled_notification_listeners"
                    )
                    if (flat != null && flat.contains(pkgName)) {
                        val component = ComponentName(context, BankNotificationListenerService::class.java)
                        requestRebind(component)
                        Log.d(TAG, "Requested rebind for BankNotificationListenerService")
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "requestRebind failed: ${t.message}")
                }
            }
        }
    }
}
