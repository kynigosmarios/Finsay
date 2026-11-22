package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Checks Google Play Store via the official Play In-App Updates API for newer releases
 * of the app and manages visibility/dismissal of the top update banner.
 */
object AppUpdateChecker {
    private const val TAG = "AppUpdateChecker"
    private const val PREFS_NAME = "finsay_app_update_prefs"
    private const val KEY_DISMISSED_VERSION_CODE = "dismissed_update_version_code"

    private val _availableVersionCode = MutableStateFlow<Int?>(null)
    val availableVersionCode: StateFlow<Int?> = _availableVersionCode.asStateFlow()

    private val _isUpdateBannerVisible = MutableStateFlow(false)
    val isUpdateBannerVisible: StateFlow<Boolean> = _isUpdateBannerVisible.asStateFlow()

    fun checkForUpdate(context: Context) {
        try {
            val appContext = context.applicationContext
            val appUpdateManager = AppUpdateManagerFactory.create(appContext)
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { info ->
                    try {
                        val isAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        val remoteVersionCode = info.availableVersionCode()
                        if (isAvailable && remoteVersionCode > BuildConfig.VERSION_CODE) {
                            _availableVersionCode.value = remoteVersionCode
                            val dismissedCode = appContext
                                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                .getInt(KEY_DISMISSED_VERSION_CODE, -1)
                            _isUpdateBannerVisible.value = (dismissedCode != remoteVersionCode)
                        } else {
                            _availableVersionCode.value = null
                            _isUpdateBannerVisible.value = false
                        }
                    } catch (t: Throwable) {
                        Log.w(TAG, "Error handling appUpdateInfo: ${t.message}")
                    }
                }
                .addOnFailureListener { e ->
                    // Expected on non-Play-installed / local debug builds; fail silently.
                    Log.d(TAG, "Play Store update check skipped/unavailable: ${e.message}")
                }
        } catch (t: Throwable) {
            Log.d(TAG, "Play Store update check unavailable: ${t.message}")
        }
    }

    fun dismissUpdateBanner(context: Context) {
        try {
            val versionToDismiss = _availableVersionCode.value
            if (versionToDismiss != null) {
                context.applicationContext
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(KEY_DISMISSED_VERSION_CODE, versionToDismiss)
                    .apply()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed persisting dismissed update version: ${t.message}")
        }
        _isUpdateBannerVisible.value = false
    }

    fun openPlayStoreForUpdate(context: Context) {
        val packageName = context.packageName
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName")
        ).apply {
            setPackage("com.android.vending")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            try {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (t: Throwable) {
                Log.w(TAG, "Could not open Play Store page: ${t.message}")
            }
        } catch (t: Throwable) {
            try {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (_: Throwable) {
                Log.w(TAG, "Could not open Play Store page: ${t.message}")
            }
        }
    }
}
