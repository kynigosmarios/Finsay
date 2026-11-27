package com.example.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.MainActivity
import com.example.util.Localization

class QuickExpenseTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile
        if (tile != null) {
            val langCode = try {
                getSharedPreferences("oikonomos_app_prefs", Context.MODE_PRIVATE)
                    .getString("app_language", Localization.Language.ENGLISH.code)
            } catch (_: Throwable) {
                Localization.Language.ENGLISH.code
            }
            val lang = Localization.Language.fromCode(langCode)
            tile.label = Localization.tr(lang, "Quick Expense", "Γρήγορη καταχώρηση")
            tile.contentDescription = Localization.tr(lang, "Quick expense entry", "Γρήγορη καταχώρηση εξόδου")
            tile.state = Tile.STATE_ACTIVE
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_QUICK_ACTION", "ACTION_QUICK_EXPENSE")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
