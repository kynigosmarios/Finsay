package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.example.ui.components.FinsayMotion
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBar
import com.example.ui.components.OikoNomosLogo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.example.ui.components.FinsayProShowcasePage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.Savings
import com.example.ui.components.AppSettingsDialog
import com.example.ui.components.AppUpdateBanner
import com.example.ui.components.BankNotificationBanner
import com.example.ui.components.BurgerMenuDrawer
import com.example.ui.components.CategoryDialog
import com.example.ui.components.ExodologioDetailsDialog
import com.example.ui.components.ExodologioManagerDialog
import com.example.ui.components.HouseholdActiveBar
import com.example.ui.components.HouseholdActivityDialog
import com.example.ui.components.HouseholdInviteQrDialog
import com.example.ui.components.IncomingInviteConfirmDialog
import com.example.ui.components.LegalDocumentsDialog
import com.example.ui.components.ExportReportDialog
import com.example.ui.components.ManualExpenseDialog
import com.example.ui.components.ManualIncomeDialog
import com.example.ui.components.MemberDialog
import com.example.ui.components.OnboardingCarouselDialog
import com.example.ui.components.PrivacyConsentDialog
import com.example.ui.components.ReceiptScanDialog
import com.example.ui.components.ReminderNotificationBanner
import com.example.ui.components.RemindersManagerDialog
import com.example.ui.components.UndoDeletionBanner
import com.example.ui.components.UserAvatar
import com.example.ui.components.PremiumPaywallDialog
import com.example.ui.components.QuickActionChooserDialog
import com.example.ui.components.RecurringExpenseDialog
import com.example.ui.components.SavingsGoalDialog
import com.example.ui.components.SavingsGoalsManagerDialog
import com.example.ui.components.TileSettingsDialog
import com.example.ui.components.UserProfileDialog
import com.example.ui.components.VoiceConfirmationDialog
import com.example.ui.components.VoiceInputDialog
import com.example.ui.components.VoiceListeningOverlay
import com.example.ui.components.VoiceMicButton
import com.example.ui.components.WelcomeOnboardingCarouselDialog
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Emerald900
import com.example.ui.theme.Emerald950
import com.example.ui.theme.LogoGreen
import com.example.ui.theme.ModernFontFamily
import com.example.ui.viewmodel.OikoNomosViewModel
import com.example.util.AppUpdateChecker
import com.example.util.InAppReviewManager
import com.example.util.LocalCurrencySymbol
import com.example.util.SpeechRecognitionHelper
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    EXPENSES("Αρχική"),
    ANALYTICS("Στατιστικά"),
    MEMBERS("Μέλη"),
    CATEGORIES("Κατηγορίες")
}

private enum class InAppSubPage {
    PROFILE,
    HOUSEHOLDS,
    REMINDERS,
    GOALS,
    SETTINGS,
    FINSAY_PRO,
    GUIDE,
    LEGAL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: OikoNomosViewModel,
    proPlanPrices: com.example.billing.BillingManager.ProPlanPrices = com.example.billing.BillingManager.ProPlanPrices(),
    onLaunchBilling: (android.app.Activity, String) -> Boolean = { _, _ -> false }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableIntStateOf(0) }
    var analyticsFilterScope by remember { mutableStateOf("MONTH") }
    var analyticsPeriodKey by remember { mutableStateOf("CURRENT_MONTH") }
    var analyticsYearKey by remember { mutableStateOf<String?>(null) }
    var analyticsSplitMode by remember { mutableIntStateOf(0) }

    // State collected from ViewModel
    val expenses by viewModel.filteredExpenses.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val members by viewModel.allMembers.collectAsStateWithLifecycle()
    val deletedMembers by viewModel.deletedMembers.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val incomeCategories by viewModel.incomeCategories.collectAsStateWithLifecycle()
    val deletedCategories by viewModel.deletedCategories.collectAsStateWithLifecycle()
    val selectedMemberFilter by viewModel.selectedMemberFilter.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val homeSelectedMonthKey by viewModel.homeSelectedMonthKey.collectAsStateWithLifecycle()
    val homeSelectedTypeFilter by viewModel.homeSelectedTypeFilter.collectAsStateWithLifecycle()
    val activeSpecialFilter by viewModel.activeSpecialFilter.collectAsStateWithLifecycle()
    val advancedFilter by viewModel.advancedFilter.collectAsStateWithLifecycle()
    val hasMoreOlderHistory by viewModel.hasMoreOlderHistory.collectAsStateWithLifecycle()
    val isLoadingOlderHistory by viewModel.isLoadingOlderHistory.collectAsStateWithLifecycle()
    val activeHouseholdInvite by viewModel.activeHouseholdInvite.collectAsStateWithLifecycle()
    val isGeneratingInvite by viewModel.isGeneratingInvite.collectAsStateWithLifecycle()
    val pendingIncomingInviteCode by viewModel.pendingIncomingInviteCode.collectAsStateWithLifecycle()
    val homeDrillDownScrollTrigger by viewModel.homeDrillDownScrollTrigger.collectAsStateWithLifecycle()
    val homeScrollToTopTrigger by viewModel.homeScrollToTopTrigger.collectAsStateWithLifecycle()

    val pendingParsedExpense by viewModel.pendingParsedExpense.collectAsStateWithLifecycle()
    val countdownState by viewModel.countdownState.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()

    val showManualExpenseDialog by viewModel.showManualExpenseDialog.collectAsStateWithLifecycle()
    val editingExpense by viewModel.editingExpense.collectAsStateWithLifecycle()

    val allIncomes by viewModel.allIncomes.collectAsStateWithLifecycle()
    val filteredIncomes by viewModel.filteredIncomes.collectAsStateWithLifecycle()
    val showManualIncomeDialog by viewModel.showManualIncomeDialog.collectAsStateWithLifecycle()
    val editingIncome by viewModel.editingIncome.collectAsStateWithLifecycle()

    val showMemberDialog by viewModel.showMemberDialog.collectAsStateWithLifecycle()
    val editingMember by viewModel.editingMember.collectAsStateWithLifecycle()

    val showCategoryDialog by viewModel.showCategoryDialog.collectAsStateWithLifecycle()
    val editingCategory by viewModel.editingCategory.collectAsStateWithLifecycle()
    val editingCategoryType by viewModel.editingCategoryType.collectAsStateWithLifecycle()
    val pendingNotificationDeepLink by viewModel.pendingNotificationDeepLink.collectAsStateWithLifecycle()
    val requestedTabIndex by viewModel.requestedTabIndex.collectAsStateWithLifecycle()

    val showSettingsDialog by viewModel.showSettingsDialog.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val lang = com.example.util.Localization.Language.fromCode(appLanguage)

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentHousehold by viewModel.currentHousehold.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val isInitialExpensesLoading by viewModel.isInitialExpensesLoading.collectAsStateWithLifecycle()

    val userHouseholds by viewModel.userHouseholds.collectAsStateWithLifecycle()
    val canManage by viewModel.canManageMembersAndCategories.collectAsStateWithLifecycle()
    val isCreator by viewModel.isCurrentHouseholdCreator.collectAsStateWithLifecycle()
    val verifiedUserLookupState by viewModel.verifiedUserLookupState.collectAsStateWithLifecycle()
    val resolvedUsersByUid by viewModel.resolvedUsersByUid.collectAsStateWithLifecycle()
    val syncStatusState by viewModel.syncStatusState.collectAsStateWithLifecycle()
    val connectedHouseholdUsers by viewModel.connectedHouseholdUsers.collectAsStateWithLifecycle()
    val defaultPayerMembers by viewModel.defaultPayerMembers.collectAsStateWithLifecycle()
    val defaultPayerMember by viewModel.defaultPayerMember.collectAsStateWithLifecycle()
    val pendingUndoDeletion by viewModel.pendingUndoDeletion.collectAsStateWithLifecycle()
    val householdActivities by viewModel.householdActivities.collectAsStateWithLifecycle()
    val showActivityDialog by viewModel.showActivityDialog.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val showExodologioManagerDialog by viewModel.showExodologioManagerDialog.collectAsStateWithLifecycle()
    val showBurgerMenu by viewModel.showBurgerMenu.collectAsStateWithLifecycle()
    val showUserProfileDialog by viewModel.showUserProfileDialog.collectAsStateWithLifecycle()
    val showExodologioDetailsDialog by viewModel.showExodologioDetailsDialog.collectAsStateWithLifecycle()
    val isAppUpdateBannerVisible by AppUpdateChecker.isUpdateBannerVisible.collectAsStateWithLifecycle()
    val premiumPaywallReason by viewModel.premiumPaywallReason.collectAsStateWithLifecycle()

    val showQuickActionChooser by viewModel.showQuickActionChooser.collectAsStateWithLifecycle()
    val showVoiceInputDialog by viewModel.showVoiceInputDialog.collectAsStateWithLifecycle()
    val showReceiptScanDialog by viewModel.showReceiptScanDialog.collectAsStateWithLifecycle()
    val pendingBankTransactions by viewModel.pendingBankTransactions.collectAsStateWithLifecycle()
    val monthlyVoiceUsage by viewModel.monthlyVoiceUsage.collectAsStateWithLifecycle()
    val showExportReportDialog by viewModel.showExportReportDialog.collectAsStateWithLifecycle()
    val showSavingsGoalsManagerDialog by viewModel.showSavingsGoalsManagerDialog.collectAsStateWithLifecycle()
    val showSavingsGoalDialog by viewModel.showSavingsGoalDialog.collectAsStateWithLifecycle()
    val editingSavingsGoal by viewModel.editingSavingsGoal.collectAsStateWithLifecycle()
    val showRecurringExpenseDialog by viewModel.showRecurringExpenseDialog.collectAsStateWithLifecycle()
    val editingRecurringExpense by viewModel.editingRecurringExpense.collectAsStateWithLifecycle()
    val showRemindersManagerDialog by viewModel.showRemindersManagerDialog.collectAsStateWithLifecycle()
    val activeDueReminders by viewModel.activeDueReminders.collectAsStateWithLifecycle()
    val recurringExpenses by viewModel.allRecurringExpenses.collectAsStateWithLifecycle()
    val savingsGoals by viewModel.allSavingsGoals.collectAsStateWithLifecycle()
    val launchWithChooser by viewModel.launchWithChooser.collectAsStateWithLifecycle()
    val showOnboardingCarousel by viewModel.showOnboardingCarousel.collectAsStateWithLifecycle()
    val showWelcomeCarousel by viewModel.showWelcomeCarousel.collectAsStateWithLifecycle()
    val dailyReminderEnabled by viewModel.dailyReminderEnabled.collectAsStateWithLifecycle()
    val dailyReminderHour by viewModel.dailyReminderHour.collectAsStateWithLifecycle()
    val dailyReminderMinute by viewModel.dailyReminderMinute.collectAsStateWithLifecycle()
    val showPrivacyConsentDialog by viewModel.showPrivacyConsentDialog.collectAsStateWithLifecycle()
    val notificationConsentCompleted by viewModel.notificationConsentCompleted.collectAsStateWithLifecycle()
    val bankAutoCatcherEnabled by viewModel.bankAutoCatcherEnabled.collectAsStateWithLifecycle()
    val legalDocumentsInitialTab by viewModel.legalDocumentsInitialTab.collectAsStateWithLifecycle()
    val showFinsayProPage by viewModel.showFinsayProPage.collectAsStateWithLifecycle()
    val isUserPro by viewModel.isProUser.collectAsStateWithLifecycle()
    val pendingInAppReviewPrompt by viewModel.pendingInAppReviewPrompt.collectAsStateWithLifecycle()

    val activeSubPage = when {
        legalDocumentsInitialTab != null -> InAppSubPage.LEGAL
        showUserProfileDialog -> InAppSubPage.PROFILE
        showExodologioDetailsDialog -> InAppSubPage.HOUSEHOLDS
        canManage && showRemindersManagerDialog -> InAppSubPage.REMINDERS
        showSavingsGoalsManagerDialog -> InAppSubPage.GOALS
        showSettingsDialog -> InAppSubPage.SETTINGS
        showFinsayProPage -> InAppSubPage.FINSAY_PRO
        showOnboardingCarousel -> InAppSubPage.GUIDE
        else -> null
    }

    BackHandler(enabled = activeSubPage != null && !showBurgerMenu) {
        if (activeSubPage == InAppSubPage.HOUSEHOLDS) {
            viewModel.clearVerifiedUserLookupState()
        }
        viewModel.dismissAllSubPages()
    }

    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    // Handle snackbar
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Discreet Google Play In-App Review prompt once user meets usage milestones
    LaunchedEffect(pendingInAppReviewPrompt) {
        if (pendingInAppReviewPrompt) {
            viewModel.consumeInAppReviewPrompt()
            val act = context as? android.app.Activity
            if (act != null) {
                InAppReviewManager.launchReviewIfEligible(act)
            }
        }
    }

    // Programmatic tab switch (e.g. from notification deep-links)
    LaunchedEffect(requestedTabIndex) {
        requestedTabIndex?.let { tabIdx ->
            currentTab = tabIdx.coerceIn(0, 3)
            viewModel.consumeRequestedTabIndex()
        }
    }

    // Universal notification tap deep-linking: opens the exact dialog/screen for every Finsay notification
    LaunchedEffect(pendingNotificationDeepLink, recurringExpenses.size, pendingBankTransactions.size) {
        val link = pendingNotificationDeepLink ?: return@LaunchedEffect
        when (link) {
            is OikoNomosViewModel.NotificationDeepLink.BankTransaction -> {
                com.example.util.BankNotificationHolder.reloadFromPrefs(context)
                val allHeld = com.example.util.BankNotificationHolder.pendingTransactions.value
                val targetTx = allHeld.find { it.id == link.txId }
                    ?: pendingBankTransactions.find { it.id == link.txId }
                    ?: pendingBankTransactions.firstOrNull()
                viewModel.dismissAllSubPages()
                currentTab = 0
                if (targetTx != null) {
                    viewModel.confirmBankTransaction(targetTx)
                }
                viewModel.clearPendingNotificationDeepLink()
            }
            is OikoNomosViewModel.NotificationDeepLink.BillReminder -> {
                val matching = recurringExpenses.find { it.id == link.reminderId }
                if (matching != null) {
                    viewModel.dismissAllSubPages()
                    currentTab = 0
                    viewModel.logExpenseFromReminder(matching)
                    viewModel.clearPendingNotificationDeepLink()
                } else {
                    // On cold start, wait briefly for Room/Firestore recurring list to load before falling back
                    kotlinx.coroutines.delay(900L)
                    val retryMatch = viewModel.allRecurringExpenses.value.find { it.id == link.reminderId }
                    viewModel.dismissAllSubPages()
                    currentTab = 0
                    if (retryMatch != null) {
                        viewModel.logExpenseFromReminder(retryMatch)
                    } else if (canManage) {
                        viewModel.openRemindersManagerDialog()
                    }
                    viewModel.clearPendingNotificationDeepLink()
                }
            }
            is OikoNomosViewModel.NotificationDeepLink.DailyReminder -> {
                viewModel.dismissAllSubPages()
                currentTab = 0
                viewModel.openQuickActionChooser()
                viewModel.clearPendingNotificationDeepLink()
            }
            is OikoNomosViewModel.NotificationDeepLink.HouseholdActivity -> {
                viewModel.dismissAllSubPages()
                currentTab = 0
                viewModel.openActivityDialog()
                viewModel.clearPendingNotificationDeepLink()
            }
        }
    }

    // Speech Recognition Helper (dynamically aligned with selected app language)
    val latestAppLanguage by rememberUpdatedState(appLanguage)
    val speechHelper = remember {
        SpeechRecognitionHelper(
            context = context,
            onListeningStateChanged = { listening ->
                viewModel.setListening(listening)
            },
            onResult = { text ->
                viewModel.processVoiceInput(text)
                viewModel.dismissVoiceInputDialog()
            },
            onError = { errMsg ->
                viewModel.showSnackbar(errMsg)
            },
            onRmsChanged = { rms ->
                viewModel.setMicSoundLevel(rms)
            },
            languageProvider = { latestAppLanguage }
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                speechHelper.cancelListening()
                viewModel.dismissVoiceInputDialog()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            speechHelper.cancelListening()
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val langNow = com.example.util.Localization.Language.fromCode(latestAppLanguage)
        if (isGranted) {
            try {
                speechHelper.startListening()
            } catch (t: Throwable) {
                viewModel.showSnackbar(
                    com.example.util.Localization.tr(langNow, "Microphone start error", "Σφάλμα εκκίνησης μικροφώνου")
                )
            }
        } else {
            viewModel.showSnackbar(
                com.example.util.Localization.tr(
                    langNow,
                    "Microphone permission is required for voice entry. You can also use the quick examples!",
                    "Απαιτείται άδεια μικροφώνου για φωνητική καταχώρηση. Μπορείτε να χρησιμοποιήσετε τα έτοιμα παραδείγματα!"
                )
            )
        }
    }

    fun handleMicToggle() {
        try {
            if (isListening) {
                speechHelper.stopListening()
            } else {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    speechHelper.startListening()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        } catch (t: Throwable) {
            val langNow = com.example.util.Localization.Language.fromCode(latestAppLanguage)
            val fallback = com.example.util.Localization.tr(langNow, "unavailable", "μη διαθέσιμο")
            viewModel.showSnackbar(
                com.example.util.Localization.trFormat(
                    langNow,
                    "Microphone error: %1\$s",
                    "Σφάλμα μικροφώνου: %1\$s",
                    t.localizedMessage ?: fallback
                )
            )
        }
    }

    val isDarkTheme = when {
        themeMode.equals("DARK", ignoreCase = true) -> true
        themeMode.equals("LIGHT", ignoreCase = true) -> false
        else -> isSystemInDarkTheme()
    }

    CompositionLocalProvider(LocalCurrencySymbol provides currencySymbol) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeightDp = maxHeight
        val fontScale = LocalDensity.current.fontScale

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                val topBarBg = if (isDarkTheme) Color(0xFF0F172A) else Color.White
                val topBarBorder = if (isDarkTheme) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0).copy(alpha = 0.6f)
                val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val needsTallScreenHeaderCompensation =
                    fontScale < 0.96f || statusBarTop < 46.dp || (screenHeightDp >= 860.dp && statusBarTop < 48.dp)
                val targetTotalHeaderHeight = (screenHeightDp * 0.138f).coerceIn(112.dp, 134.dp)
                val defaultHeaderHeight = statusBarTop + 64.dp
                val missingHeaderHeight = if (needsTallScreenHeaderCompensation) {
                    val deficit = (targetTotalHeaderHeight - defaultHeaderHeight).coerceAtLeast(0.dp)
                    val smallFontBonus = if (fontScale < 0.96f) {
                        ((1.0f - fontScale) * 40f).dp.coerceIn(0.dp, 12.dp)
                    } else 0.dp
                    (deficit + smallFontBonus).coerceIn(0.dp, 36.dp)
                } else {
                    0.dp
                }
                val extraHeaderTop = (missingHeaderHeight * 0.72f).coerceIn(0.dp, 26.dp)
                val extraHeaderBottom = (missingHeaderHeight - extraHeaderTop).coerceIn(0.dp, 10.dp)
                Surface(
                    color = topBarBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(width = 1.dp, color = topBarBorder)
                ) {
                    TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (activeSubPage == InAppSubPage.HOUSEHOLDS) {
                                        viewModel.clearVerifiedUserLookupState()
                                    }
                                    viewModel.dismissAllSubPages()
                                    currentTab = 0
                                    viewModel.setSearchQuery("")
                                    viewModel.clearSpecialDrillDownFilter()
                                    viewModel.requestHomeScrollToTop()
                                }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                                .testTag("app_brand_home_button")
                        ) {
                            OikoNomosLogo(
                                isDarkTheme = false,
                                size = 36.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Finsay",
                                fontFamily = ModernFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 25.sp,
                                letterSpacing = (-0.5).sp,
                                color = LogoGreen
                            )
                        }
                    },
                    actions = {
                        Surface(
                            shape = CircleShape,
                            color = Emerald700.copy(alpha = 0.12f),
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { viewModel.openUserProfileDialog() }
                                .testTag("topbar_user_avatar")
                        ) {
                            UserAvatar(
                                photoUrl = userProfile?.photoUrl?.ifBlank { null } ?: currentUser?.photoUrl?.toString(),
                                displayName = userProfile?.displayName?.ifBlank { null } ?: currentUser?.displayName,
                                modifier = Modifier.fillMaxSize(),
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { viewModel.openBurgerMenu() },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("burger_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = com.example.util.Localization.menuTitle(lang),
                                tint = if (isDarkTheme) Color.White else Emerald950,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = topBarBg,
                        titleContentColor = if (isDarkTheme) Color.White else Emerald950,
                        actionIconContentColor = if (isDarkTheme) Color.White else Emerald950
                    ),
                    modifier = Modifier.padding(top = extraHeaderTop, bottom = extraHeaderBottom)
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Active Voice Listening notification banner
                VoiceListeningOverlay(
                    isListening = isListening,
                    onCloseListening = {
                        speechHelper.stopListening()
                    }
                )

                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = if (isDarkTheme) Color(0xFF10B981) else Color(0xFF065F46),
                    selectedTextColor = if (isDarkTheme) Color(0xFF34D399) else Color(0xFF065F46),
                    indicatorColor = if (isDarkTheme) Color(0xFF064E3B).copy(alpha = 0.6f) else Color(0xFFD1FAE5),
                    unselectedIconColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569),
                    unselectedTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                )

                // Navigation Bar
                NavigationBar(
                    containerColor = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F2),
                    tonalElevation = 4.dp,
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    )
                ) {
                    NavigationBarItem(
                        selected = activeSubPage == null && currentTab == 0,
                        onClick = {
                            if (activeSubPage == InAppSubPage.HOUSEHOLDS) viewModel.clearVerifiedUserLookupState()
                            viewModel.dismissAllSubPages()
                            currentTab = 0
                        },
                        icon = {
                            Icon(
                                imageVector = if (activeSubPage == null && currentTab == 0) Icons.Default.Receipt else Icons.Outlined.Receipt,
                                contentDescription = com.example.util.Localization.tabExpenses(lang)
                            )
                        },
                        label = {
                            Text(
                                text = com.example.util.Localization.tabExpenses(lang),
                                fontSize = 10.5.sp,
                                fontWeight = if (activeSubPage == null && currentTab == 0) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        },
                        colors = navItemColors,
                        modifier = Modifier.testTag("tab_expenses")
                    )

                    NavigationBarItem(
                        selected = activeSubPage == null && currentTab == 1,
                        onClick = {
                            if (activeSubPage == InAppSubPage.HOUSEHOLDS) viewModel.clearVerifiedUserLookupState()
                            viewModel.dismissAllSubPages()
                            currentTab = 1
                        },
                        icon = {
                            Icon(
                                imageVector = if (activeSubPage == null && currentTab == 1) Icons.Default.PieChart else Icons.Outlined.PieChart,
                                contentDescription = com.example.util.Localization.tabAnalytics(lang)
                            )
                        },
                        label = {
                            Text(
                                text = com.example.util.Localization.tabAnalytics(lang),
                                fontSize = 10.5.sp,
                                fontWeight = if (activeSubPage == null && currentTab == 1) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        },
                        colors = navItemColors,
                        modifier = Modifier.testTag("tab_analytics")
                    )

                    NavigationBarItem(
                        selected = activeSubPage == null && currentTab == 2,
                        onClick = {
                            if (activeSubPage == InAppSubPage.HOUSEHOLDS) viewModel.clearVerifiedUserLookupState()
                            viewModel.dismissAllSubPages()
                            currentTab = 2
                        },
                        icon = {
                            Icon(
                                imageVector = if (activeSubPage == null && currentTab == 2) Icons.Default.Group else Icons.Outlined.Group,
                                contentDescription = com.example.util.Localization.tabMembers(lang)
                            )
                        },
                        label = {
                            Text(
                                text = com.example.util.Localization.tabMembers(lang),
                                fontSize = 10.5.sp,
                                fontWeight = if (activeSubPage == null && currentTab == 2) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        },
                        colors = navItemColors,
                        modifier = Modifier.testTag("tab_members")
                    )

                    NavigationBarItem(
                        selected = activeSubPage == null && currentTab == 3,
                        onClick = {
                            if (activeSubPage == InAppSubPage.HOUSEHOLDS) viewModel.clearVerifiedUserLookupState()
                            viewModel.dismissAllSubPages()
                            currentTab = 3
                        },
                        icon = {
                            Icon(
                                imageVector = if (activeSubPage == null && currentTab == 3) Icons.Default.Category else Icons.Outlined.Category,
                                contentDescription = com.example.util.Localization.tabCategories(lang)
                            )
                        },
                        label = {
                            Text(
                                text = com.example.util.Localization.tabCategories(lang),
                                fontSize = 10.5.sp,
                                fontWeight = if (activeSubPage == null && currentTab == 3) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        },
                        colors = navItemColors,
                        modifier = Modifier.testTag("tab_categories")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Active Household Bar (Visible across all tabs so user can always see and switch active exodologio)
            HouseholdActiveBar(
                currentHousehold = currentHousehold,
                households = userHouseholds,
                appLanguage = appLanguage,
                isDarkTheme = isDarkTheme,
                isPro = isUserPro,
                currentUserId = currentUser?.uid.orEmpty(),
                syncStatus = syncStatusState,
                activityCount = householdActivities.size,
                onOpenActivity = { viewModel.openActivityDialog() },
                onRefreshSync = { viewModel.refreshCloudSync() },
                onSelectHousehold = { viewModel.switchExodologio(it) },
                onManageHouseholdsClick = { viewModel.openExodologioDetailsDialog() },
                onCreateNewHouseholdClick = { viewModel.openExodologioManager() },
                onSwitchClick = { viewModel.openExodologioManager() }
            )

            // Google Play Store App Update Banner
            AnimatedVisibility(
                visible = isAppUpdateBannerVisible,
                enter = FinsayMotion.BannerEnter,
                exit = FinsayMotion.BannerExit
            ) {
                AppUpdateBanner(
                    appLanguage = appLanguage,
                    onUpdateClick = { AppUpdateChecker.openPlayStoreForUpdate(context) },
                    onDismissClick = { AppUpdateChecker.dismissUpdateBanner(context) }
                )
            }

            // Bank Notification Auto-Catcher Banner (Pro-only)
            AnimatedVisibility(
                visible = isUserPro && bankAutoCatcherEnabled && pendingBankTransactions.isNotEmpty(),
                enter = FinsayMotion.BannerEnter,
                exit = FinsayMotion.BannerExit
            ) {
                BankNotificationBanner(
                    transactions = pendingBankTransactions,
                    appLanguage = appLanguage,
                    resolveSuggestedCategory = { tx ->
                        viewModel.resolveSuggestedCategoryForBankTx(tx)?.let {
                            com.example.util.Localization.translateCategory(it.name, lang)
                        }
                    },
                    onConfirmTransaction = { viewModel.confirmBankTransaction(it) },
                    onDismissTransaction = { viewModel.dismissBankTransaction(it) }
                )
            }

            // Bill / Expense Reminders Due Banner (CREATOR/EDITOR only per RBAC 4.2)
            AnimatedVisibility(
                visible = canManage && activeDueReminders.isNotEmpty(),
                enter = FinsayMotion.BannerEnter,
                exit = FinsayMotion.BannerExit
            ) {
                ReminderNotificationBanner(
                    reminders = activeDueReminders,
                    appLanguage = appLanguage,
                    onRecordReminder = { viewModel.logExpenseFromReminder(it) },
                    onDismissReminder = { viewModel.dismissDueReminder(it) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = activeSubPage ?: currentTab,
                    transitionSpec = {
                        val target = targetState
                        val initial = initialState
                        val enteringSubPage = target is InAppSubPage && initial !is InAppSubPage
                        val leavingSubPage = target !is InAppSubPage && initial is InAppSubPage
                        val tabSlideDir = if (target is Int && initial is Int) {
                            if (target >= initial) 1 else -1
                        } else 1

                        when {
                            enteringSubPage -> {
                                (slideInHorizontally(
                                    animationSpec = tween(FinsayMotion.SUBPAGE_ENTER_MS, easing = FastOutSlowInEasing),
                                    initialOffsetX = { fullWidth -> fullWidth / 5 }
                                ) + fadeIn(tween(FinsayMotion.SUBPAGE_ENTER_MS, easing = FastOutSlowInEasing))) togetherWith
                                    (slideOutHorizontally(
                                        animationSpec = tween(FinsayMotion.SUBPAGE_EXIT_MS, easing = FastOutSlowInEasing),
                                        targetOffsetX = { fullWidth -> -fullWidth / 14 }
                                    ) + fadeOut(tween(150)))
                            }
                            leavingSubPage -> {
                                (slideInHorizontally(
                                    animationSpec = tween(FinsayMotion.SUBPAGE_ENTER_MS, easing = FastOutSlowInEasing),
                                    initialOffsetX = { fullWidth -> -fullWidth / 14 }
                                ) + fadeIn(tween(FinsayMotion.SUBPAGE_ENTER_MS, easing = FastOutSlowInEasing))) togetherWith
                                    (slideOutHorizontally(
                                        animationSpec = tween(FinsayMotion.SUBPAGE_EXIT_MS, easing = FastOutSlowInEasing),
                                        targetOffsetX = { fullWidth -> fullWidth / 5 }
                                    ) + fadeOut(tween(160)))
                            }
                            else -> {
                                (slideInHorizontally(
                                    animationSpec = tween(FinsayMotion.TAB_DURATION_MS, easing = FastOutSlowInEasing),
                                    initialOffsetX = { fullWidth -> (fullWidth / 14) * tabSlideDir }
                                ) + fadeIn(tween(FinsayMotion.TAB_DURATION_MS, easing = FastOutSlowInEasing))) togetherWith
                                    (slideOutHorizontally(
                                        animationSpec = tween(160, easing = FastOutSlowInEasing),
                                        targetOffsetX = { fullWidth -> -(fullWidth / 18) * tabSlideDir }
                                    ) + fadeOut(tween(130)))
                            }
                        }.using(SizeTransform(clip = false))
                    },
                    label = "tab_navigation_animated_content"
                ) { route ->
                when (route) {
                    0 -> ExpensesScreen(
                        expenses = expenses,
                        incomes = filteredIncomes,
                        allExpenses = allExpenses,
                        allIncomes = allIncomes,
                        categories = categories,
                        members = members,
                        selectedMemberFilter = selectedMemberFilter,
                        selectedCategoryFilter = selectedCategoryFilter,
                        searchQuery = searchQuery,
                        selectedMonthKey = homeSelectedMonthKey,
                        selectedTypeFilter = homeSelectedTypeFilter,
                        activeSpecialFilter = activeSpecialFilter,
                        drillDownScrollTrigger = homeDrillDownScrollTrigger,
                        scrollToTopTrigger = homeScrollToTopTrigger,
                        appLanguage = appLanguage,
                        onMemberFilterChange = { viewModel.setMemberFilter(it) },
                        onCategoryFilterChange = { viewModel.setCategoryFilter(it) },
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onMonthKeyChange = { viewModel.setHomeSelectedMonthKey(it) },
                        onTypeFilterChange = { viewModel.setHomeSelectedTypeFilter(it) },
                        onClearSpecialFilter = { viewModel.clearSpecialDrillDownFilter() },
                        onAddManualExpense = { viewModel.openManualExpenseDialog() },
                        onAddManualIncome = { viewModel.openManualIncomeDialog() },
                        onVoiceExpense = { viewModel.openVoiceInputDialog() },
                        onEditExpense = { viewModel.openManualExpenseDialog(it) },
                        onDeleteExpense = { viewModel.deleteExpense(it) },
                        onDuplicateExpense = { viewModel.duplicateExpenseToday(it) },
                        onEditIncome = { viewModel.openManualIncomeDialog(it) },
                        onDeleteIncome = { viewModel.deleteIncome(it) },
                        onExportExcel = { viewModel.openExportReportDialog() },
                        onExportPdf = { viewModel.openExportReportDialog() },
                        onShareReport = { viewModel.openExportReportDialog() },
                        onAddMember = { viewModel.openMemberDialog() },
                        onAddCategory = { viewModel.openCategoryDialog() },
                        onSeedStandardCategories = { viewModel.seedStandardCategories() },
                        isInitialLoading = isInitialExpensesLoading,
                        advancedFilter = advancedFilter,
                        onAdvancedFilterChange = { viewModel.setAdvancedFilter(it) },
                        onBulkUpdateExpenseCategory = { ids, cat -> viewModel.bulkUpdateExpenseCategory(ids, cat) },
                        householdName = currentHousehold?.name.orEmpty(),
                        hasMoreOlderHistory = hasMoreOlderHistory,
                        isLoadingOlderHistory = isLoadingOlderHistory,
                        onLoadOlderHistory = { viewModel.loadOlderHistory() }
                    )
                    1 -> AnalyticsScreen(
                        expenses = allExpenses,
                        incomes = allIncomes,
                        categories = categories,
                        members = members,
                        recurringExpenses = recurringExpenses,
                        savingsGoals = savingsGoals,
                        appLanguage = appLanguage,
                        isPro = isUserPro,
                        canManageStructure = canManage,
                        onShowPremiumPaywall = { viewModel.showPremiumPaywall(it) },
                        onExportReport = { viewModel.openExportReportDialog() },
                        onManageCategories = { currentTab = 3 },
                        onOpenSavingsGoals = { viewModel.openSavingsGoalsManagerDialog() },
                        onDrillDownToHome = { request ->
                            viewModel.applyHomeDrillDown(request)
                            currentTab = 0
                        },
                        onSettleTransfer = { from, to, amount, ts ->
                            viewModel.recordSettlementTransfer(from, to, amount, ts)
                        },
                        initialFilterScope = analyticsFilterScope,
                        initialPeriodKey = analyticsPeriodKey,
                        initialYearKey = analyticsYearKey,
                        initialSplitMode = analyticsSplitMode,
                        onAnalyticsStateChange = { scope, period, year, split ->
                            analyticsFilterScope = scope
                            analyticsPeriodKey = period
                            analyticsYearKey = year
                            analyticsSplitMode = split
                        }
                    )
                    2 -> MembersScreen(
                        members = members,
                        currentHousehold = currentHousehold,
                        isPremium = isUserPro,
                        appLanguage = appLanguage,
                        canManage = canManage,
                        currentUserEmail = currentUser?.email ?: userProfile?.email ?: "",
                        deletedMembers = deletedMembers,
                        expenses = allExpenses,
                        onAddMember = { viewModel.openMemberDialog() },
                        onEditMember = { viewModel.openMemberDialog(it) },
                        onDeleteMember = { viewModel.deleteMember(it) },
                        onRestoreMember = { viewModel.restoreMember(it) },
                        onPermanentDeleteMember = { viewModel.permanentDeleteMember(it) },
                        onUpdateMemberColor = { member, color -> viewModel.updateMemberColor(member, color) },
                        onMoveMemberUp = { viewModel.moveMemberUp(it) },
                        onMoveMemberDown = { viewModel.moveMemberDown(it) },
                        onShowPremiumPaywall = { viewModel.showPremiumPaywall(it) },
                        onShowMessage = { viewModel.showSnackbar(it) }
                    )
                    3 -> CategoriesScreen(
                        categories = categories,
                        incomeCategories = incomeCategories,
                        appLanguage = appLanguage,
                        canManage = canManage,
                        isPro = isUserPro,
                        deletedCategories = deletedCategories,
                        onAddCategory = { type -> viewModel.openCategoryDialog(null, type) },
                        onEditCategory = { viewModel.openCategoryDialog(it, it.categoryType) },
                        onDeleteCategory = { viewModel.deleteCategory(it) },
                        onRestoreCategory = { viewModel.restoreCategory(it) },
                        onPermanentDeleteCategory = { viewModel.permanentDeleteCategory(it) },
                        onMoveCategoryUp = { viewModel.moveCategoryUp(it) },
                        onMoveCategoryDown = { viewModel.moveCategoryDown(it) },
                        onSeedStandardCategories = { type -> viewModel.seedStandardCategories(type) }
                    )
                    InAppSubPage.PROFILE -> UserProfileDialog(
                        currentUser = currentUser,
                        userProfile = userProfile,
                        isPro = isUserPro,
                        expenseCount = expenses.size,
                        totalSpent = expenses.sumOf { it.amount },
                        currencySymbol = currencySymbol,
                        appLanguage = appLanguage,
                        onSaveDisplayName = { viewModel.updateUserDisplayName(it) },
                        onSaveFullProfile = { first, last, birth, phone, occ, city ->
                            viewModel.updateUserFullProfile(first, last, birth, phone, occ, city)
                        },
                        onUpdateProfilePhoto = { viewModel.updateUserProfilePhoto(it) },
                        onShowPremiumPaywall = { viewModel.showPremiumPaywall("profile_upgrade") },
                        onSignOut = { viewModel.signOutUser() },
                        onDeleteAccount = { viewModel.deleteCurrentUserAccount() },
                        onDeleteAccountWithPassword = { password, onSuccess, onError ->
                            viewModel.deleteCurrentUserAccountWithPassword(password, onSuccess, onError)
                        },
                        onDeleteAccountWithGoogleToken = { idToken, onSuccess, onError ->
                            viewModel.deleteCurrentUserAccountWithGoogleToken(idToken, onSuccess, onError)
                        },
                        onDismiss = { viewModel.dismissUserProfileDialog() }
                    )
                    InAppSubPage.HOUSEHOLDS -> ExodologioDetailsDialog(
                        currentHousehold = currentHousehold,
                        allHouseholds = userHouseholds,
                        isPremium = isUserPro,
                        isOwner = isCreator,
                        isHouseholdOwner = { viewModel.isUserCreatorOfHousehold(it) },
                        getHouseholdRole = { viewModel.getUserRoleForHousehold(it) },
                        lookupState = verifiedUserLookupState,
                        resolvedUsersByUid = resolvedUsersByUid,
                        allMembers = members,
                        connectedUsers = connectedHouseholdUsers,
                        onAssignDefaultPayer = { uid, mid -> viewModel.assignUserDefaultPayer(uid, mid) },
                        onAssignDefaultPayers = { hid, uid, ids -> viewModel.assignUserDefaultPayers(hid, uid, ids) },
                        onSearchUser = { email, hid -> viewModel.searchVerifiedUserForSharing(email, hid) },
                        onClearLookup = { viewModel.clearVerifiedUserLookupState() },
                        onAddSharedUser = { hid, user, role -> viewModel.shareHouseholdWithVerifiedUser(hid, user, role) },
                        onChangeSharedUserRole = { hid, uid, email, role -> viewModel.updateSharedUserRole(hid, uid, email, role) },
                        onRemoveSharedUser = { hid, uid, email -> viewModel.removeSharedUserFromHousehold(hid, uid, email) },
                        appLanguage = appLanguage,
                        currentUserId = currentUser?.uid.orEmpty(),
                        onRenameHousehold = { viewModel.renameCurrentHousehold(it) },
                        onRenameHouseholdById = { id, name -> viewModel.renameHousehold(id, name) },
                        onSelectHousehold = { viewModel.switchExodologio(it) },
                        onCreateNewHousehold = { viewModel.createNewExodologio(it) },
                        onCreateNewHouseholdWithCurrency = { name, code -> viewModel.createNewExodologio(name, code) },
                        onUpdateHouseholdCurrency = { hid, code -> viewModel.updateHouseholdCurrency(hid, code) },
                        onDeleteHousehold = { viewModel.deleteHousehold(it) },
                        onManageMembers = {
                            viewModel.clearVerifiedUserLookupState()
                            viewModel.dismissAllSubPages()
                            currentTab = 2
                        },
                        onShowPremiumPaywall = { viewModel.showPremiumPaywall(it) },
                        onClearExpenses = { viewModel.clearCurrentExodologioExpenses() },
                        isGeneratingInvite = isGeneratingInvite,
                        onGenerateInviteLink = { h, role -> viewModel.generateHouseholdInvite(h, role) },
                        onJoinHouseholdByCode = { code -> viewModel.joinHousehold(code) },
                        onDismiss = {
                            viewModel.clearVerifiedUserLookupState()
                            viewModel.dismissExodologioDetailsDialog()
                        }
                    )
                    InAppSubPage.REMINDERS -> RemindersManagerDialog(
                        reminders = recurringExpenses,
                        categories = categories,
                        members = members,
                        defaultPayerMember = defaultPayerMember,
                        defaultPayerMembers = defaultPayerMembers,
                        onSuggestCategory = { m, afm -> viewModel.suggestCategoryForMerchant(m, afm) },
                        onLearnCategory = { m, cid, cname, afm -> viewModel.learnCategoryForMerchant(m, cid, cname, afm) },
                        isPro = isUserPro,
                        onShowPremiumPaywall = { viewModel.showPremiumPaywall(it) },
                        appLanguage = appLanguage,
                        onDismiss = { viewModel.dismissRemindersManagerDialog() },
                        onSaveReminder = { id, title, amount, categoryId, categoryName, dayOfMonth, reminderHour, reminderMinute, splitType, memberIds, memberNamesDisplay, note, payerId, payerName, payerIdsCsv, payerAmountsCsv, frequency, anchorMonth ->
                            viewModel.saveRecurringExpense(
                                id = id,
                                title = title,
                                amount = amount,
                                categoryId = categoryId,
                                categoryName = categoryName,
                                dayOfMonth = dayOfMonth,
                                note = note,
                                payerMemberId = payerId,
                                payerMemberName = payerName,
                                reminderHour = reminderHour,
                                reminderMinute = reminderMinute,
                                splitType = splitType,
                                memberIds = memberIds,
                                memberNamesDisplay = memberNamesDisplay,
                                payerMemberIdsCsv = payerIdsCsv,
                                payerAmountsCsv = payerAmountsCsv,
                                autoRegisterIfDue = false,
                                frequency = frequency,
                                anchorMonth = anchorMonth
                            )
                        },
                        onToggleActive = { viewModel.toggleRecurringActive(it) },
                        onDeleteReminder = { viewModel.deleteRecurringExpense(it) },
                        onLogExpenseNow = { viewModel.logExpenseFromReminder(it) }
                    )
                    InAppSubPage.GOALS -> SavingsGoalsManagerDialog(
                        goals = savingsGoals,
                        members = members,
                        defaultPayerMember = defaultPayerMember,
                        householdName = currentHousehold?.name.orEmpty(),
                        currencySymbol = currencySymbol,
                        canManage = canManage,
                        appLanguage = appLanguage,
                        onDismiss = { viewModel.dismissSavingsGoalsManagerDialog() },
                        onOpenGoalEditor = { goal -> viewModel.openSavingsGoalDialog(goal) },
                        onDeleteGoal = { goal -> viewModel.deleteSavingsGoal(goal) },
                        onDepositToGoal = { goal, amount, payer, note ->
                            viewModel.depositToGoal(goal, amount, payer, note)
                        }
                    )
                    InAppSubPage.SETTINGS -> AppSettingsDialog(
                        currentUser = currentUser,
                        household = currentHousehold,
                        isSyncing = isSyncing,
                        themeMode = themeMode,
                        appLanguage = appLanguage,
                        launchWithChooser = launchWithChooser,
                        userProfile = userProfile,
                        reminderEnabled = dailyReminderEnabled,
                        reminderHour = dailyReminderHour,
                        reminderMinute = dailyReminderMinute,
                        notificationConsentHandled = notificationConsentCompleted,
                        bankAutoCatcherEnabled = bankAutoCatcherEnabled,
                        isPro = isUserPro,
                        onShowPremiumPaywall = { viewModel.showPremiumPaywall(it) },
                        onAcceptNotificationConsent = { viewModel.acceptNotificationConsent() },
                        onToggleReminder = { viewModel.toggleDailyReminder(it) },
                        onSetReminderTime = { h, m -> viewModel.setDailyReminderTime(h, m) },
                        onToggleBankAutoCatcher = { viewModel.toggleBankAutoCatcher(it) },
                        onThemeChange = { viewModel.setThemeMode(it) },
                        onLanguageChange = { viewModel.setAppLanguage(it) },
                        onToggleLaunchWithChooser = { viewModel.setLaunchWithChooser(it) },
                        onCreateHousehold = { viewModel.createHousehold(it) },
                        onSignOut = { viewModel.signOutUser() },
                        onClearAllDataAndSignOut = { viewModel.clearCurrentHouseholdAllData() },
                        onMessage = { viewModel.showSnackbar(it) },
                        onDismiss = { viewModel.dismissSettingsDialog() }
                    )
                    InAppSubPage.FINSAY_PRO -> FinsayProShowcasePage(
                        isPro = isUserPro,
                        appLanguage = appLanguage,
                        proPlanPrices = proPlanPrices,
                        onLaunchBilling = { productId ->
                            val act = context as? android.app.Activity
                            if (act != null) onLaunchBilling(act, productId) else false
                        },
                        onDismiss = { viewModel.dismissFinsayProPage() }
                    )
                    InAppSubPage.GUIDE -> OnboardingCarouselDialog(
                        appLanguage = appLanguage,
                        isPro = isUserPro,
                        onOpenProPaywall = {
                            viewModel.dismissOnboardingCarousel()
                            viewModel.showPremiumPaywall("guide_pro")
                        },
                        onDismiss = { viewModel.dismissOnboardingCarousel() },
                        onAddMember = if (canManage) {
                            {
                                viewModel.dismissOnboardingCarousel()
                                viewModel.openMemberDialog()
                            }
                        } else null,
                        onAddCategory = if (canManage) {
                            {
                                viewModel.dismissOnboardingCarousel()
                                viewModel.openCategoryDialog()
                            }
                        } else null,
                        onSeedCategories = if (canManage) {
                            {
                                viewModel.dismissOnboardingCarousel()
                                viewModel.seedStandardCategories()
                            }
                        } else null
                    )
                    InAppSubPage.LEGAL -> LegalDocumentsDialog(
                        initialTab = legalDocumentsInitialTab ?: "TERMS",
                        appLanguage = appLanguage,
                        onDismiss = { viewModel.dismissLegalDocumentsPage() }
                    )
                }
            }
            }
            }

            // 5-second Undo deletion floating banner (placed left of VoiceMicButton to prevent overlap)
            UndoDeletionBanner(
                pendingUndo = pendingUndoDeletion,
                currencySymbol = currencySymbol,
                appLanguage = appLanguage,
                onUndo = { viewModel.undoPendingDeletion() },
                onDismiss = { viewModel.dismissUndoDeletion() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, end = 84.dp, bottom = 16.dp)
                    .zIndex(5f)
            )

            // Floating Voice Mic Button positioned identically to Scaffold's FAB, underneath BurgerMenuDrawer
            AnimatedVisibility(
                visible = activeSubPage == null,
                enter = scaleIn(
                    initialScale = 0.82f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(tween(160)),
                exit = scaleOut(targetScale = 0.82f, animationSpec = tween(120)) + fadeOut(tween(120)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                VoiceMicButton(
                    isListening = isListening,
                    onMicClick = { viewModel.openVoiceInputDialog() }
                )
            }

            // Burger Menu Drawer: slides right-to-left over ALL height between TopBar and BottomBar (covering HouseholdActiveBar & Mic)
            BurgerMenuDrawer(
                isOpen = showBurgerMenu,
                currentUser = currentUser,
                userProfile = userProfile,
                isPro = isUserPro,
                currentHousehold = currentHousehold,
                currentTab = currentTab,
                appLanguage = appLanguage,
                onLanguageChange = { viewModel.setAppLanguage(it) },
                onSelectTab = {
                    if (activeSubPage == InAppSubPage.HOUSEHOLDS) viewModel.clearVerifiedUserLookupState()
                    viewModel.dismissAllSubPages()
                    currentTab = it
                },
                onOpenProfile = { viewModel.openUserProfileDialog() },
                onOpenExodologio = { viewModel.openExodologioDetailsDialog() },
                onOpenSettings = { viewModel.openSettingsDialog() },
                onOpenProInfo = { viewModel.openFinsayProPage() },
                onOpenGuide = { viewModel.openOnboardingGuide() },
                onOpenReminders = if (canManage) { { viewModel.openRemindersManagerDialog() } } else null,
                onOpenGoals = { viewModel.openSavingsGoalsManagerDialog() },
                onOpenLegal = { tab -> viewModel.openLegalDocumentsPage(tab) },
                onSignOut = { viewModel.signOutUser() },
                onDismiss = { viewModel.dismissBurgerMenu() },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f)
            )
        }
        }
    }

    // Voice Confirmation Dialog with editable Amount, Category, Members, Date (Requirement 2)
    VoiceConfirmationDialog(
        pendingExpense = pendingParsedExpense,
        categories = categories,
        members = members,
        defaultPayerMember = defaultPayerMember,
        defaultPayerMembers = defaultPayerMembers,
        appLanguage = appLanguage,
        onConfirm = { title, amount, categoryId, categoryName, timestamp, splitType, memberIds, memberNamesDisplay, prompt, payerId, payerName, payerMemberIdsCsv, payerAmountsCsv ->
            viewModel.confirmVoiceExpense(
                title = title,
                amount = amount,
                categoryId = categoryId,
                categoryName = categoryName,
                timestamp = timestamp,
                splitType = splitType,
                memberIds = memberIds,
                memberNamesDisplay = memberNamesDisplay,
                originalPrompt = prompt,
                payerMemberId = payerId,
                payerMemberName = payerName,
                payerMemberIdsCsv = payerMemberIdsCsv,
                payerAmountsCsv = payerAmountsCsv
            )
        },
        onCancel = { viewModel.cancelPendingExpense() }
    )

    // Manual Expense Add/Edit Dialog
    if (showManualExpenseDialog) {
        ManualExpenseDialog(
            expenseToEdit = editingExpense,
            categories = categories,
            members = members,
            appLanguage = appLanguage,
            defaultPayerMember = defaultPayerMember,
            defaultPayerMembers = defaultPayerMembers,
            isPro = isUserPro,
            onShowPremiumPaywall = { viewModel.showPremiumPaywall(it) },
            onSuggestCategory = { m, afm -> viewModel.suggestCategoryForMerchant(m, afm) },
            onLearnCategory = { m, cid, cname, afm -> viewModel.learnCategoryForMerchant(m, cid, cname, afm) },
            onDismiss = { viewModel.dismissManualExpenseDialog() },
            onSave = { id, title, amount, categoryId, categoryName, timestamp, splitType, memberIds, memberNamesDisplay, note, payerId, payerName, payerMemberIdsCsv, payerAmountsCsv ->
                viewModel.saveExpense(
                    id = id,
                    title = title,
                    amount = amount,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    timestamp = timestamp,
                    splitType = splitType,
                    memberIds = memberIds,
                    memberNamesDisplay = memberNamesDisplay,
                    note = note,
                    payerMemberId = payerId,
                    payerMemberName = payerName,
                    payerMemberIdsCsv = payerMemberIdsCsv,
                    payerAmountsCsv = payerAmountsCsv
                )
            }
        )
    }

    // Manual Income Dialog
    if (showManualIncomeDialog) {
        ManualIncomeDialog(
            incomeToEdit = editingIncome,
            members = members,
            incomeCategories = incomeCategories,
            appLanguage = appLanguage,
            onDismiss = { viewModel.dismissManualIncomeDialog() },
            onSave = { id, title, amount, memberId, memberName, category, timestamp, note ->
                viewModel.saveIncome(
                    id = id,
                    title = title,
                    amount = amount,
                    memberId = memberId,
                    memberName = memberName,
                    category = category,
                    timestamp = timestamp,
                    note = note
                )
            }
        )
    }

    // Member Dialog
    if (showMemberDialog) {
        MemberDialog(
            memberToEdit = editingMember,
            appLanguage = appLanguage,
            connectedUsers = connectedHouseholdUsers,
            onDismiss = { viewModel.dismissMemberDialog() },
            onSave = { id, name, role, avatarColorHex, voiceKeywords, linkedUserUid ->
                viewModel.saveMember(id, name, role, avatarColorHex, voiceKeywords, linkedUserUid)
            }
        )
    }

    // Category Dialog
    if (showCategoryDialog) {
        val activeCategoryType = editingCategory?.categoryType ?: editingCategoryType
        CategoryDialog(
            categoryToEdit = editingCategory,
            isIncomeCategory = activeCategoryType.equals("INCOME", ignoreCase = true),
            appLanguage = appLanguage,
            onDismiss = { viewModel.dismissCategoryDialog() },
            onSave = { id, name, iconKey, monthlyBudget, colorHex, voiceKeywords, fontColorHex ->
                viewModel.saveCategory(
                    id = id,
                    name = name,
                    iconKey = iconKey,
                    monthlyBudget = monthlyBudget,
                    colorHex = colorHex,
                    voiceKeywords = voiceKeywords,
                    fontColorHex = fontColorHex,
                    categoryType = activeCategoryType
                )
            }
        )
    }

    // Quick Action Chooser (Triggered on App Launch, Widget, or Manual Action)
    if (showQuickActionChooser) {
        QuickActionChooserDialog(
            appLanguage = appLanguage,
            onVoiceClick = { viewModel.openVoiceInputDialog() },
            onScanReceiptClick = { viewModel.openReceiptScanDialog() },
            onManualClick = { viewModel.openManualExpenseDialog() },
            onIncomeClick = { viewModel.openManualIncomeDialog() },
            onOpenAppClick = { viewModel.dismissQuickActionChooser() },
            onDismiss = { viewModel.dismissQuickActionChooser() }
        )
    }

    // Exodologio Manager Dialog
    if (showExodologioManagerDialog) {
        ExodologioManagerDialog(
            currentHousehold = currentHousehold,
            allHouseholds = userHouseholds,
            isPremium = isUserPro,
            isHouseholdOwner = { viewModel.isUserCreatorOfHousehold(it) },
            getHouseholdRole = { viewModel.getUserRoleForHousehold(it) },
            lookupState = verifiedUserLookupState,
            resolvedUsersByUid = resolvedUsersByUid,
            allMembers = members,
            connectedUsers = connectedHouseholdUsers,
            onAssignDefaultPayer = { uid, mid -> viewModel.assignUserDefaultPayer(uid, mid) },
            onAssignDefaultPayers = { hid, uid, ids -> viewModel.assignUserDefaultPayers(hid, uid, ids) },
            onSearchUser = { email, hid -> viewModel.searchVerifiedUserForSharing(email, hid) },
            onClearLookup = { viewModel.clearVerifiedUserLookupState() },
            onAddSharedUser = { hid, user, role -> viewModel.shareHouseholdWithVerifiedUser(hid, user, role) },
            onChangeSharedUserRole = { hid, uid, email, role -> viewModel.updateSharedUserRole(hid, uid, email, role) },
            onRemoveSharedUser = { hid, uid, email -> viewModel.removeSharedUserFromHousehold(hid, uid, email) },
            appLanguage = appLanguage,
            currentUserId = currentUser?.uid.orEmpty(),
            onSelectHousehold = { viewModel.switchExodologio(it) },
            onCreateNewHousehold = { viewModel.createNewExodologio(it) },
            onCreateNewHouseholdWithCurrency = { name, code -> viewModel.createNewExodologio(name, code) },
            onUpdateHouseholdCurrency = { hid, code -> viewModel.updateHouseholdCurrency(hid, code) },
            onDeleteHousehold = { viewModel.deleteHousehold(it) },
            onShowPremiumPaywall = { viewModel.showPremiumPaywall(it) },
            isGeneratingInvite = isGeneratingInvite,
            onGenerateInviteLink = { h, role -> viewModel.generateHouseholdInvite(h, role) },
            onJoinHouseholdByCode = { code -> viewModel.joinHousehold(code) },
            onDismiss = {
                viewModel.clearVerifiedUserLookupState()
                viewModel.dismissExodologioManager()
            }
        )
    }

    // Premium Paywall Dialog
    if (premiumPaywallReason != null) {
        PremiumPaywallDialog(
            reason = premiumPaywallReason!!,
            appLanguage = appLanguage,
            proPlanPrices = proPlanPrices,
            onUnlockPro = { planId ->
                val act = context as? android.app.Activity
                val launched = if (act != null) onLaunchBilling(act, planId) else false
                if (!launched) {
                    try {
                        val playIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(playIntent)
                    } catch (e: Exception) {
                        Log.w("MainScreen", "Could not open Google Play Store: ${e.message}")
                    }
                    viewModel.showSnackbar(
                        com.example.util.Localization.tr(
                            lang,
                            "Connecting to Google Play... Please try again in a moment.",
                            "Σύνδεση με το Google Play... Παρακαλώ δοκιμάστε ξανά σε λίγο."
                        )
                    )
                }
            },
            onDismiss = { viewModel.dismissPremiumPaywall() }
        )
    }

    // Voice Input Dialog (Robust Speech Recording with Freemium Limit Tracking)
    if (showVoiceInputDialog) {
        VoiceInputDialog(
            isListening = isListening,
            appLanguage = appLanguage,
            monthlyUsage = monthlyVoiceUsage,
            isPro = isUserPro,
            onStartListening = { handleMicToggle() },
            onStopListening = { speechHelper.stopListening() },
            onSubmitVoiceText = { text ->
                viewModel.processVoiceInput(text)
                viewModel.dismissVoiceInputDialog()
            },
            onDismiss = {
                speechHelper.cancelListening()
                viewModel.dismissVoiceInputDialog()
            },
            onShowMessage = { viewModel.showSnackbar(it) }
        )
    }

    // Receipt OCR & AADE QR Scanner Dialog
    if (showReceiptScanDialog) {
        ReceiptScanDialog(
            categories = categories,
            members = members,
            isPro = isUserPro,
            appLanguage = appLanguage,
            defaultPayerMember = defaultPayerMember,
            defaultPayerMembers = defaultPayerMembers,
            onSuggestCategory = { m, afm -> viewModel.suggestCategoryForMerchant(m, afm) },
            onLearnCategory = { m, cid, cname, afm -> viewModel.learnCategoryForMerchant(m, cid, cname, afm) },
            onShowPremiumPaywall = { viewModel.showPremiumPaywall(it) },
            onExpenseConfirmed = { title, amount, category, timestamp, member, note ->
                viewModel.saveExpense(
                    id = 0L,
                    title = title,
                    amount = amount,
                    categoryId = category.id,
                    categoryName = category.name,
                    timestamp = timestamp,
                    splitType = "ALL_FAMILY",
                    memberIds = emptyList(),
                    memberNamesDisplay = com.example.util.Localization.allMembersWord(lang),
                    note = note,
                    payerMemberId = member?.id ?: 0L,
                    payerMemberName = member?.name ?: "",
                    payerMemberIdsCsv = if (member != null) "${member.id}" else "",
                    payerAmountsCsv = ""
                )
            },
            onDismiss = { viewModel.dismissReceiptScanDialog() }
        )
    }

    // Feature 4: Excel / PDF / CSV Full Financial Export Report Dialog
    if (showExportReportDialog) {
        ExportReportDialog(
            expenses = allExpenses,
            incomes = allIncomes,
            members = members,
            householdName = currentHousehold?.name.orEmpty(),
            appLanguage = appLanguage,
            onDismiss = { viewModel.dismissExportReportDialog() },
            onShowMessage = { viewModel.showSnackbar(it) }
        )
    }

    // Feature 5: Savings Goal Add/Edit Dialog (CREATOR/EDITOR only per RBAC 4.2)
    if (canManage && showSavingsGoalDialog) {
        SavingsGoalDialog(
            goalToEdit = editingSavingsGoal,
            appLanguage = appLanguage,
            onDismiss = { viewModel.dismissSavingsGoalDialog() },
            onSave = { id, title, target, current, deadline, note, recordAsExpense ->
                viewModel.saveSavingsGoal(id, title, target, current, deadline, note, recordAsExpense)
            }
        )
    }

    // Feature 2: Recurring Expense Add/Edit Dialog (CREATOR/EDITOR only per RBAC 4.2)
    if (canManage && showRecurringExpenseDialog) {
        RecurringExpenseDialog(
            recurringToEdit = editingRecurringExpense,
            categories = categories,
            members = members,
            defaultPayerMember = defaultPayerMember,
            defaultPayerMembers = defaultPayerMembers,
            appLanguage = appLanguage,
            onDismiss = { viewModel.dismissRecurringExpenseDialog() },
            onSave = { id, title, amount, categoryId, categoryName, dayOfMonth, note, payerId, payerName, payerIdsCsv, payerAmountsCsv, frequency, anchorMonth ->
                viewModel.saveRecurringExpense(
                    id = id,
                    title = title,
                    amount = amount,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    dayOfMonth = dayOfMonth,
                    note = note,
                    payerMemberId = payerId,
                    payerMemberName = payerName,
                    payerMemberIdsCsv = payerIdsCsv,
                    payerAmountsCsv = payerAmountsCsv,
                    frequency = frequency,
                    anchorMonth = anchorMonth
                )
            }
        )
    }

    // Household Activity History Dialog (Feature 3.1)
    if (showActivityDialog) {
        HouseholdActivityDialog(
            activities = householdActivities,
            currencySymbol = currencySymbol,
            appLanguage = appLanguage,
            onDismiss = { viewModel.dismissActivityDialog() }
        )
    }

    // New-User Welcome Onboarding Modal (Streamlined)
    if (showWelcomeCarousel) {
        WelcomeOnboardingCarouselDialog(
            appLanguage = appLanguage,
            onDismiss = { viewModel.dismissWelcomeCarousel() },
            onOpenProPaywall = { viewModel.showPremiumPaywall("welcome_carousel") },
            onOpenLegalDocument = { tab -> viewModel.openLegalDocumentsPage(tab) }
        )
    }

    // GDPR / Privacy Consent Modal on App Start (Legacy fallback)
    if (showPrivacyConsentDialog) {
        PrivacyConsentDialog(
            appLanguage = appLanguage,
            onAccept = { viewModel.acceptPrivacyConsent() }
        )
    }

    // Household Invite Link & QR Modal
    activeHouseholdInvite?.let { invite ->
        HouseholdInviteQrDialog(
            invite = invite,
            appLanguage = appLanguage,
            onDismiss = { viewModel.clearActiveHouseholdInvite() }
        )
    }

    // Incoming Deep-Link Household Invite Confirmation Modal
    pendingIncomingInviteCode?.let { inviteCode ->
        IncomingInviteConfirmDialog(
            inviteCode = inviteCode,
            appLanguage = appLanguage,
            onConfirmJoin = { code ->
                viewModel.consumePendingIncomingInviteCode()
                viewModel.joinHousehold(code)
            },
            onDismiss = { viewModel.consumePendingIncomingInviteCode() }
        )
    }
    }
}
