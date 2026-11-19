package com.example.ui.components

import android.app.TimePickerDialog
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Household
import com.example.data.model.UserProfile
import com.example.service.BankNotificationListenerService
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald700
import com.example.util.Localization
import com.google.firebase.auth.FirebaseUser

@Composable
fun AppSettingsDialog(
    currentUser: FirebaseUser?,
    household: Household?,
    isSyncing: Boolean,
    themeMode: String,
    appLanguage: String = "EL",
    launchWithChooser: Boolean,
    userProfile: UserProfile? = null,
    reminderEnabled: Boolean = false,
    reminderHour: Int = 21,
    reminderMinute: Int = 30,
    notificationConsentHandled: Boolean = false,
    bankAutoCatcherEnabled: Boolean = true,
    isPro: Boolean = false,
    onShowPremiumPaywall: (String) -> Unit = {},
    onAcceptNotificationConsent: () -> Unit = {},
    onToggleReminder: (Boolean) -> Unit = {},
    onSetReminderTime: (Int, Int) -> Unit = { _, _ -> },
    onToggleBankAutoCatcher: (Boolean) -> Unit = {},
    onThemeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit = {},
    onToggleLaunchWithChooser: (Boolean) -> Unit,
    onCreateHousehold: (String) -> Unit,
    onJoinHousehold: (String) -> Unit,
    onOpenReminders: (() -> Unit)? = null,
    onSignOut: () -> Unit,
    onClearAllDataAndSignOut: () -> Unit = {},
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showNotificationConsentDialog by remember { mutableStateOf(false) }
    var showBankInfoDialog by remember { mutableStateOf(false) }
    var showBankDisclosureDialog by remember { mutableStateOf(false) }
    val lang = Localization.Language.fromCode(appLanguage)
    val context = androidx.compose.ui.platform.LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onToggleReminder(true)
        }
    }

    val openBankNotificationAccessSettings = {
        val componentName = ComponentName(context, BankNotificationListenerService::class.java)
        val openedDirect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val detailIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                    putExtra(
                        Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        componentName.flattenToString()
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(detailIntent)
                true
            } catch (_: Exception) {
                false
            }
        } else {
            false
        }
        if (!openedDirect) {
            try {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            } catch (_: Exception) {
                onMessage(
                    if (lang == Localization.Language.ENGLISH)
                        "Go to Settings > Notifications > Notification access to enable Finsay"
                    else
                        "Μεταβείτε στις Ρυθμίσεις > Ειδοποιήσεις για ενεργοποίηση του Finsay"
                )
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(reminderEnabled, notificationConsentHandled) {
        if (reminderEnabled && !notificationConsentHandled) {
            showNotificationConsentDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .testTag("app_settings_dialog")
    ) {
        // Page Header Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("app_settings_close_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (lang == Localization.Language.ENGLISH) "Back" else "Επιστροφή",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Emerald700.copy(alpha = 0.12f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = Localization.appSettings(lang),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {


                    // 3. Theme Selection (Light, Dark, System) - Only Icons
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ThemeCardOption(
                                icon = Icons.Default.LightMode,
                                isSelected = themeMode == "LIGHT",
                                onClick = { onThemeChange("LIGHT") },
                                modifier = Modifier.weight(1f)
                            )

                            ThemeCardOption(
                                icon = Icons.Default.DarkMode,
                                isSelected = themeMode == "DARK",
                                onClick = { onThemeChange("DARK") },
                                modifier = Modifier.weight(1f)
                            )

                            ThemeCardOption(
                                icon = Icons.Default.SettingsBrightness,
                                isSelected = themeMode == "SYSTEM",
                                onClick = { onThemeChange("SYSTEM") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 4. Section: Υπενθύμιση καταχώρησης (Entry Reminder)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Emerald700.copy(alpha = 0.12f),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (reminderEnabled) Icons.Default.NotificationsActive else Icons.Default.Bedtime,
                                                contentDescription = null,
                                                tint = Emerald700,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (lang == Localization.Language.ENGLISH) "Entry Reminder" else "Υπενθύμιση καταχώρησης",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Switch(
                                        checked = reminderEnabled,
                                        onCheckedChange = { isChecked ->
                                            if (isChecked) {
                                                if (!notificationConsentHandled) {
                                                    showNotificationConsentDialog = true
                                                } else {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                        val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                                                            context,
                                                            android.Manifest.permission.POST_NOTIFICATIONS
                                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                                        if (!hasPerm) {
                                                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                                        }
                                                    }
                                                    onToggleReminder(true)
                                                }
                                            } else {
                                                onToggleReminder(false)
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Emerald700
                                        ),
                                        modifier = Modifier.testTag("daily_reminder_switch")
                                    )
                                }

                                AnimatedVisibility(
                                    visible = reminderEnabled,
                                    enter = FinsayMotion.ExpandEnter,
                                    exit = FinsayMotion.ShrinkExit
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Spacer(modifier = Modifier.height(14.dp))

                                        var timeInput by remember(reminderHour, reminderMinute) {
                                            mutableStateOf("%02d:%02d".format(reminderHour, reminderMinute))
                                        }
                                        val focusManager = LocalFocusManager.current
                                        val keyboardController = LocalSoftwareKeyboardController.current

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable {
                                                    TimePickerDialog(
                                                        context,
                                                        { _, pickedHour, pickedMinute ->
                                                            onSetReminderTime(pickedHour, pickedMinute)
                                                            timeInput = "%02d:%02d".format(pickedHour, pickedMinute)
                                                        },
                                                        reminderHour,
                                                        reminderMinute,
                                                        true
                                                    ).show()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    tint = Emerald700,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (lang == Localization.Language.ENGLISH) "Reminder Time" else "Ώρα ειδοποίησης",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.5f)),
                                                modifier = Modifier
                                                    .width(88.dp)
                                                    .height(40.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        focusManager.clearFocus()
                                                        keyboardController?.hide()
                                                        TimePickerDialog(
                                                            context,
                                                            { _, pickedHour, pickedMinute ->
                                                                onSetReminderTime(pickedHour, pickedMinute)
                                                                timeInput = "%02d:%02d".format(pickedHour, pickedMinute)
                                                            },
                                                            reminderHour,
                                                            reminderMinute,
                                                            true
                                                        ).show()
                                                    }
                                                    .testTag("reminder_time_picker_button")
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.padding(horizontal = 6.dp)
                                                ) {
                                                    Text(
                                                        text = "%02d:%02d".format(reminderHour, reminderMinute),
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Emerald700,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.testTag("reminder_time_input")
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }


                    // Section: Ανάγνωση Τραπεζικών Push (Bank Auto-Catcher)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Emerald700.copy(alpha = 0.12f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = Emerald700,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH) "Bank Push Reader" else "Ανάγνωση Τραπεζικών Push",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Amber700.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, Amber700.copy(alpha = 0.45f))
                                    ) {
                                        Text(
                                            text = "Pro",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Amber700,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { showBankInfoDialog = true },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("bank_push_info_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = if (lang == Localization.Language.ENGLISH) "Info" else "Πληροφορίες",
                                            tint = Emerald700,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Switch(
                                    checked = bankAutoCatcherEnabled && isPro,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked && !isPro) {
                                            onShowPremiumPaywall("bank_auto_catcher")
                                        } else if (isChecked) {
                                            showBankDisclosureDialog = true
                                        } else {
                                            onToggleBankAutoCatcher(false)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Emerald700
                                    ),
                                    modifier = Modifier.testTag("bank_auto_catcher_switch")
                                )
                            }
                        }
                    }

                    // Section: Επιλογή Γλώσσας (Language Selection)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LanguageOptionCard(
                                title = "Ελληνικά",
                                flag = "🇬🇷",
                                isSelected = appLanguage == "EL",
                                onClick = { onLanguageChange("EL") },
                                modifier = Modifier.weight(1f)
                            )

                            LanguageOptionCard(
                                title = "English",
                                flag = "🇬🇧",
                                isSelected = appLanguage == "EN",
                                onClick = { onLanguageChange("EN") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Section: Διαχείριση Δεδομένων (Clear Household Data Row)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showResetConfirmDialog = true }
                                .testTag("clear_household_data_row")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = Localization.clearAllData(lang),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = if (lang == Localization.Language.ENGLISH) "Clear Household Data?" else "Εκκαθάριση Δεδομένων Εξοδολογίου;",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                val householdTitle = household?.name?.ifBlank { null }
                    ?: if (lang == Localization.Language.ENGLISH) "active household" else "τρέχον εξοδολόγιο"
                Text(
                    text = if (lang == Localization.Language.ENGLISH)
                        "All expenses, incomes, members, categories, goals, and reminders of \"$householdTitle\" will be cleared. A backup remains safely stored in Firebase so the owner can restore it if needed."
                    else
                        "Θα εκκαθαριστούν όλα τα έξοδα, έσοδα, μέλη, κατηγορίες, στόχοι και υπενθυμίσεις από το «$householdTitle». Τα δεδομένα διατηρούνται με ασφάλεια στο Firebase για επαναφορά από τον ιδιοκτήτη αν χρειαστεί.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        onDismiss()
                        onClearAllDataAndSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_clear_household_data_btn")
                ) {
                    Text(
                        text = if (lang == Localization.Language.ENGLISH) "Clear Data" else "Εκκαθάριση",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(Localization.cancel(lang), fontSize = 12.5.sp)
                }
            }
        )
    }

    if (showNotificationConsentDialog) {
        NotificationConsentDialog(
            appLanguage = appLanguage,
            onAccept = {
                onAcceptNotificationConsent()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (!hasPerm) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                onToggleReminder(true)
                showNotificationConsentDialog = false
            },
            onDismiss = {
                onToggleReminder(false)
                showNotificationConsentDialog = false
            }
        )
    }

    if (showBankInfoDialog) {
        AlertDialog(
            onDismissRequest = { showBankInfoDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Emerald700,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lang == Localization.Language.ENGLISH) "Bank Push Reader" else "Ανάγνωση Τραπεζικών Push",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Text(
                    text = if (lang == Localization.Language.ENGLISH)
                        "This feature automatically detects purchase push notifications STRICTLY from supported banks & wallets:\n\n• Google Pay / Google Wallet\n• Eurobank\n• Alpha Bank\n• National Bank of Greece (NBG)\n• Piraeus Bank\n• Revolut\n• Payzy\n• Viber Pay\n• PayPal\n• Snappi\n\nIt NEVER triggers from emails or non-banking notifications. All processing is 100% private and on-device."
                    else
                        "Η λειτουργία αυτή αναγνωρίζει αυτόματα ειδοποιήσεις αγορών (Push Notifications) ΑΥΣΤΗΡΑ και μόνο από τις υποστηριζόμενες τράπεζες & πορτοφόλια:\n\n• Google Pay / Πορτοφόλι Google\n• Eurobank\n• Alpha Bank\n• Εθνική Τράπεζα (NBG)\n• Τράπεζα Πειραιώς\n• Revolut\n• Payzy\n• Viber Pay\n• PayPal\n• Snappi\n\nΔεν ενεργοποιείται ΠΟΤΕ από emails ή άλλες προσωπικές ειδοποιήσεις. Η επεξεργασία γίνεται 100% τοπικά στη συσκευή σας για απόλυτη ασφάλεια.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { showBankInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(Localization.close(lang), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showBankDisclosureDialog) {
        AlertDialog(
            onDismissRequest = { showBankDisclosureDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Emerald700,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lang == Localization.Language.ENGLISH)
                            "Notification Access Disclosure"
                        else
                            "Γνωστοποίηση Πρόσβασης Ειδοποιήσεων",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.5.sp
                    )
                }
            },
            text = {
                Text(
                    text = if (lang == Localization.Language.ENGLISH)
                        "To automatically suggest expense entries from your card and wallet payments, Finsay requests Android Notification Access (NotificationListenerService).\n\n" +
                            "• What is read: Strictly incoming payment push notifications from supported banking & wallet apps (Google Pay/Wallet, Eurobank, Alpha Bank, NBG, Piraeus Bank, Revolut, Payzy, Viber Pay, PayPal, Snappi) to extract only the transaction amount and merchant name.\n" +
                            "• What is NEVER read: Personal messages, SMS, OTP codes, emails, or notifications from any other app are immediately ignored.\n" +
                            "• Privacy: 100% of notification parsing runs locally on your device. Notification text is never uploaded, logged, or shared with third parties or advertisers.\n\n" +
                            "By tapping \"Accept & Continue\", you agree to enable this feature and proceed to Android Notification Access settings."
                    else
                        "Για να σας προτείνει αυτόματα καταχώρηση εξόδων από τις πληρωμές με κάρτα ή ψηφιακό πορτοφόλι, το Finsay ζητά άδεια Πρόσβασης Ειδοποιήσεων Android (NotificationListenerService).\n\n" +
                            "• Τι διαβάζεται: Αποκλειστικά και μόνο εισερχόμενες τραπεζικές ειδοποιήσεις πληρωμών από υποστηριζόμενες εφαρμογές (Google Pay/Wallet, Eurobank, Alpha Bank, Εθνική Τράπεζα, Πειραιώς, Revolut, Payzy, Viber Pay, PayPal, Snappi) για εξαγωγή μόνο του ποσού και του καταστήματος.\n" +
                            "• Τι ΔΕΝ διαβάζεται ποτέ: Προσωπικά μηνύματα, SMS, κωδικοί OTP, emails ή ειδοποιήσεις από άλλες εφαρμογές αγνοούνται άμεσα.\n" +
                            "• Ιδιωτικότητα: Η επεξεργασία γίνεται 100% τοπικά στη συσκευή σας. Το κείμενο των ειδοποιήσεων δεν αποστέλλεται και δεν κοινοποιείται ποτέ σε τρίτους ή διαφημιστές.\n\n" +
                            "Πατώντας «Αποδοχή & Συνέχεια», συναινείτε στην ενεργοποίηση και θα μεταφερθείτε στις ρυθμίσεις Πρόσβασης Ειδοποιήσεων του Android.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBankDisclosureDialog = false
                        onToggleBankAutoCatcher(true)
                        openBankNotificationAccessSettings()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("bank_disclosure_accept_btn")
                ) {
                    Text(
                        text = if (lang == Localization.Language.ENGLISH) "Accept & Continue" else "Αποδοχή & Συνέχεια",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBankDisclosureDialog = false },
                    modifier = Modifier.testTag("bank_disclosure_cancel_btn")
                ) {
                    Text(
                        text = Localization.cancel(lang),
                        fontSize = 12.5.sp
                    )
                }
            }
        )
    }
}

@Composable
private fun LanguageOptionCard(
    title: String,
    flag: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Emerald700.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        animationSpec = tween(200),
        label = "langBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Emerald700 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        animationSpec = tween(200),
        label = "langBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Emerald700 else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200),
        label = "langText"
    )
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = borderColor
        ),
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = flag, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }
            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = Emerald700,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeCardOption(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Emerald700.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        animationSpec = tween(200),
        label = "themeBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Emerald700 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        animationSpec = tween(200),
        label = "themeBorder"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "themeTint"
    )
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = borderColor
        ),
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = iconTint
            )
        }
    }
}
