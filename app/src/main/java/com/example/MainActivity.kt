package com.example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppLockScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.EmailVerificationScreen
import com.example.ui.screens.MainScreen
import com.example.ui.theme.Emerald700
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.OikoNomosViewModel
import com.example.util.AppLockManager
import com.example.util.InAppReviewManager

class MainActivity : FragmentActivity() {

    companion object {
        const val ACTION_QUICK_EXPENSE = "ACTION_QUICK_EXPENSE"
        const val ACTION_SHORTCUT_VOICE = "com.example.ACTION_SHORTCUT_VOICE"
        const val ACTION_SHORTCUT_SCAN = "com.example.ACTION_SHORTCUT_SCAN"
        const val ACTION_SHORTCUT_EXPENSE = "com.example.ACTION_SHORTCUT_EXPENSE"
    }

    private val viewModel: OikoNomosViewModel by viewModels()
    private lateinit var billingManager: com.example.billing.BillingManager
    private var isAppLocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (t: Throwable) {
            Log.w("MainActivity", "enableEdgeToEdge safe fallback: ${t.message}")
        }

        isAppLocked = AppLockManager.shouldShowLockOnLaunch(this)

        try {
            InAppReviewManager.recordAppOpen(this)
        } catch (t: Throwable) {
            Log.w("MainActivity", "InAppReviewManager init: ${t.message}")
        }

        try {
            billingManager = com.example.billing.BillingManager(
                context = this,
                onPurchasePending = {
                    runOnUiThread {
                        viewModel.onPurchasePending()
                    }
                },
                onSubscriptionActivated = { isActivated ->
                    runOnUiThread {
                        if (isActivated) {
                            viewModel.unlockPro()
                        } else {
                            viewModel.revokePro()
                        }
                    }
                }
            )
            billingManager.startConnection()
        } catch (t: Throwable) {
            Log.w("MainActivity", "BillingManager init error: ${t.message}")
        }

        try {
            com.example.util.BankNotificationHolder.init(this)
            com.example.util.BankNotificationSystemManager.init(this)
            com.example.util.BankNotificationHolder.onTransactionRemoved = { id ->
                com.example.util.BankNotificationSystemManager.cancelNotification(this, id)
            }
        } catch (t: Throwable) {
            Log.w("MainActivity", "Bank notification init: ${t.message}")
        }

        try {
            com.example.util.AppUpdateChecker.checkForUpdate(this)
        } catch (t: Throwable) {
            Log.w("MainActivity", "AppUpdateChecker init: ${t.message}")
        }

        try {
            handleIntent(intent)
        } catch (t: Throwable) {
            Log.w("MainActivity", "handleIntent safe fallback: ${t.message}")
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
            val isDarkTheme = when {
                themeMode.equals("DARK", ignoreCase = true) -> true
                themeMode.equals("LIGHT", ignoreCase = true) -> false
                else -> isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppRootNavigator(
                            viewModel = viewModel,
                            onLaunchBilling = { act, planId ->
                                try {
                                    if (::billingManager.isInitialized) {
                                        billingManager.launchPurchaseFlow(act, planId)
                                    } else {
                                        false
                                    }
                                } catch (t: Throwable) {
                                    Log.w("MainActivity", "launchPurchaseFlow safe fallback: ${t.message}")
                                    false
                                }
                            }
                        )

                        if (isAppLocked) {
                            AppLockScreen(
                                appLanguage = appLanguage,
                                onUnlocked = { isAppLocked = false }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            AppLockManager.onAppBackgrounded()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::billingManager.isInitialized) {
                billingManager.endConnection()
            }
        } catch (t: Throwable) {
            Log.w("MainActivity", "BillingManager endConnection: ${t.message}")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        try {
            handleIntent(intent)
        } catch (t: Throwable) {
            Log.w("MainActivity", "onNewIntent safe fallback: ${t.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (AppLockManager.shouldRelockOnResume(this)) {
                isAppLocked = true
            }
            viewModel.onAppResumed()
            if (::billingManager.isInitialized) {
                billingManager.queryActivePurchases()
            }
            com.example.util.BankNotificationHolder.reloadFromPrefs(this)
            com.example.service.BankNotificationListenerService.requestRebindIfPossible(this)
            com.example.service.BankNotificationListenerService.scanActiveNotifications(this)
            com.example.util.AppUpdateChecker.checkForUpdate(this)
        } catch (t: Throwable) {
            Log.w("MainActivity", "onResume safe fallback: ${t.message}")
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        try {
            val action = intent.action
            val extraAction = intent.getStringExtra("EXTRA_QUICK_ACTION")

            when {
                action == ACTION_SHORTCUT_VOICE ||
                    action == ACTION_QUICK_EXPENSE ||
                    extraAction == ACTION_QUICK_EXPENSE -> {
                    viewModel.openVoiceInputDialog()
                }
                action == ACTION_SHORTCUT_SCAN -> {
                    viewModel.openReceiptScanDialog()
                }
                action == ACTION_SHORTCUT_EXPENSE -> {
                    viewModel.openManualExpenseDialog(null)
                }
            }

            val bankTxId = intent.getStringExtra("EXTRA_BANK_TX_ID")
            if (!bankTxId.isNullOrBlank()) {
                Log.d("MainActivity", "User opened app from bank transaction notification: $bankTxId")
            }

            val billReminderId = intent.getLongExtra(com.example.reminder.BillReminderManager.EXTRA_REMINDER_ID, 0L)
            if (billReminderId > 0L) {
                viewModel.triggerReminderById(billReminderId)
            }
        } catch (t: Throwable) {
            Log.w("MainActivity", "Error parsing intent: ${t.message}")
        }
    }
}

private enum class RootScreen {
    LOADING,
    AUTH,
    EMAIL_VERIFICATION,
    MAIN
}

@Composable
fun AppRootNavigator(
    viewModel: OikoNomosViewModel,
    onLaunchBilling: (android.app.Activity, String) -> Boolean = { _, _ -> false }
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isUserVerified by viewModel.isUserVerifiedState.collectAsStateWithLifecycle()
    val isAuthInitialized by viewModel.isAuthInitialized.collectAsStateWithLifecycle()

    val currentScreen = when {
        !isAuthInitialized -> RootScreen.LOADING
        currentUser == null -> RootScreen.AUTH
        !isUserVerified -> RootScreen.EMAIL_VERIFICATION
        else -> RootScreen.MAIN
    }

    Crossfade(targetState = currentScreen, label = "root_navigation_crossfade") { screen ->
        when (screen) {
            RootScreen.LOADING -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Emerald700)
                }
            }
            RootScreen.AUTH -> {
                AuthScreen(viewModel = viewModel)
            }
            RootScreen.EMAIL_VERIFICATION -> {
                currentUser?.let { user ->
                    EmailVerificationScreen(
                        user = user,
                        viewModel = viewModel
                    )
                } ?: AuthScreen(viewModel = viewModel)
            }
            RootScreen.MAIN -> {
                MainScreen(
                    viewModel = viewModel,
                    onLaunchBilling = onLaunchBilling
                )
            }
        }
    }
}
