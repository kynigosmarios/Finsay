package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import com.example.data.entity.RecurringExpense
import com.example.data.entity.SavingsGoal
import com.example.data.model.ConnectedHouseholdUser
import com.example.data.model.FirebaseCategory
import com.example.data.model.FirebaseExpense
import com.example.data.model.FirebaseHouseholdActivity
import com.example.data.model.FirebaseIncome
import com.example.data.model.FirebaseMember
import com.example.data.model.FirebaseSavingsGoal
import com.example.data.model.Household
import com.example.data.model.HouseholdRole
import com.example.data.model.SyncStatusState
import com.example.data.model.UserProfile
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.FirestoreSyncRepository
import com.example.util.AnalyticsExpenseFilterHelper
import com.example.util.AnalyticsSpecialFilter
import com.example.util.BankNotificationHolder
import com.example.util.BankNotificationSystemManager
import com.example.util.CurrencyFormatter
import com.example.util.DefaultPayerHelper
import com.example.util.GreekVoiceParser
import com.example.util.HomeDrillDownRequest
import com.example.util.NetworkConnectivityMonitor
import com.example.util.ParsedVoiceExpense
import com.example.util.PendingBankTransaction
import com.example.util.RecurringExpenseAutoProcessor
import com.example.util.RecurringScheduleHelper
import com.example.util.StartupSafety
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class CountdownState(
    val remainingSeconds: Int = 3,
    val isActive: Boolean = false
)

data class PendingUndoDeletion(
    val title: String,
    val amount: Double,
    val isIncome: Boolean,
    val deletedExpense: Expense? = null,
    val deletedIncome: Income? = null,
    val remainingSeconds: Int = 5,
    val createdAt: Long = System.currentTimeMillis()
)

class OikoNomosViewModel(application: Application) : AndroidViewModel(application) {

    internal val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("OikoNomosVM", "CoroutineExceptionHandler caught unhandled exception: ${throwable.message}", throwable)
    }

    internal val repository: ExpenseRepository =
        ExpenseRepository(AppDatabase.getDatabase(application).expenseDao(), application)
    internal val firestoreSyncRepo = FirestoreSyncRepository.create(application)
    internal val auth: FirebaseAuth = Firebase.auth
    internal val prefs = application.getSharedPreferences("oikonomos_app_prefs", Context.MODE_PRIVATE)
    internal val networkMonitor = NetworkConnectivityMonitor(application)

    // Delegates
    private val authDelegate = OikoNomosAuthDelegate(this)
    private val syncDelegate = OikoNomosSyncDelegate(this)
    private val householdDelegate = OikoNomosHouseholdDelegate(this)
    private val transactionDelegate = OikoNomosTransactionDelegate(this)
    private val recurringGoalsDelegate = OikoNomosRecurringGoalsDelegate(this)
    private val structureDelegate = OikoNomosStructureDelegate(this)

    // Auth & Profile States
    internal val _isAuthInitialized = MutableStateFlow(auth.currentUser != null)
    val isAuthInitialized = _isAuthInitialized.asStateFlow()

    internal var suppressAuthStateListener = false

    internal val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser = _currentUser.asStateFlow()

    internal val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    internal val _isUserVerifiedState = MutableStateFlow(isUserVerified(auth.currentUser))
    val isUserVerifiedState = _isUserVerifiedState.asStateFlow()

    internal val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading = _isAuthLoading.asStateFlow()

    internal val _isProUser = MutableStateFlow(false)
    val isProUser: StateFlow<Boolean> = _isProUser.asStateFlow()

    internal val _currentHousehold = MutableStateFlow<Household?>(null)
    val currentHousehold = _currentHousehold.asStateFlow()

    internal val _userHouseholds = MutableStateFlow<List<Household>>(emptyList())
    val userHouseholds = _userHouseholds.asStateFlow()

    sealed class VerifiedUserLookupState {
        object Idle : VerifiedUserLookupState()
        object Searching : VerifiedUserLookupState()
        data class Found(val user: UserProfile) : VerifiedUserLookupState()
        object NotFound : VerifiedUserLookupState()
        object NotVerified : VerifiedUserLookupState()
        object SelfEmail : VerifiedUserLookupState()
        object AlreadyMember : VerifiedUserLookupState()
        data class Error(val message: String) : VerifiedUserLookupState()
    }

    internal val _verifiedUserLookupState = MutableStateFlow<VerifiedUserLookupState>(VerifiedUserLookupState.Idle)
    val verifiedUserLookupState = _verifiedUserLookupState.asStateFlow()

    internal val _resolvedUsersByUid = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    val resolvedUsersByUid = _resolvedUsersByUid.asStateFlow()

    // Role Flows
    val currentHouseholdRole: StateFlow<HouseholdRole> = combine(
        _currentUser,
        _currentHousehold,
        _userProfile
    ) { user, household, _ ->
        getUserRoleForHousehold(household, user)
    }
        .catch { t ->
            Log.w("OikoNomosVM", "currentHouseholdRole flow warning: ${t.message}")
            emit(HouseholdRole.REGISTAR)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HouseholdRole.REGISTAR)

    val isCurrentHouseholdCreator: StateFlow<Boolean> = currentHouseholdRole
        .map { it == HouseholdRole.CREATOR }
        .catch { emit(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val canManageMembersAndCategories: StateFlow<Boolean> = currentHouseholdRole
        .map { it.canManageMembersAndCategories }
        .catch { emit(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    internal val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    internal val _activeSyncOperations = MutableStateFlow(0)
    val syncStatusState: StateFlow<SyncStatusState> = combine(
        networkMonitor.isOnline,
        _isSyncing,
        _activeSyncOperations
    ) { online, manualSync, activeCount ->
        when {
            !online -> SyncStatusState.OFFLINE
            manualSync || activeCount > 0 -> SyncStatusState.SYNCING
            else -> SyncStatusState.SYNCED
        }
    }
        .catch { emit(SyncStatusState.SYNCED) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SyncStatusState.SYNCED)

    internal val _isInitialExpensesLoading = MutableStateFlow(prefs.getBoolean("has_entered_any_expense", false))
    val isInitialExpensesLoading = _isInitialExpensesLoading.asStateFlow()

    // Sync Job handles
    internal var roomClearJob: Job? = null
    internal var loadProfileJob: Job? = null
    internal var activeLoadProfileUid: String? = null
    internal var householdJob: Job? = null
    internal var expensesSyncJob: Job? = null
    internal var userExpensesSyncJob: Job? = null
    internal var incomesSyncJob: Job? = null
    internal var userIncomesSyncJob: Job? = null
    internal var membersSyncJob: Job? = null
    internal var userMembersSyncJob: Job? = null
    internal var categoriesSyncJob: Job? = null
    internal var userCategoriesSyncJob: Job? = null
    internal var savingsGoalsSyncJob: Job? = null
    internal var userSavingsGoalsSyncJob: Job? = null
    internal var recurringSyncJob: Job? = null
    internal var userRecurringSyncJob: Job? = null
    internal var activitySyncJob: Job? = null
    internal var userProfileSyncJob: Job? = null
    internal var userHouseholdsSyncJob: Job? = null
    internal var countdownJob: Job? = null
    internal var undoCountdownJob: Job? = null
    internal var memberReorderJob: Job? = null
    internal var categoryReorderJob: Job? = null
    internal var reminderClockJob: Job? = null

    internal var lastHouseholdExpenses = listOf<FirebaseExpense>()
    internal var lastUserExpenses = listOf<FirebaseExpense>()
    internal var lastHouseholdIncomes = listOf<FirebaseIncome>()
    internal var lastUserIncomes = listOf<FirebaseIncome>()
    internal var lastHouseholdMembers = listOf<FirebaseMember>()
    internal var lastUserMembers = listOf<FirebaseMember>()
    internal var lastHouseholdCategories = listOf<FirebaseCategory>()
    internal var lastUserCategories = listOf<FirebaseCategory>()
    internal var lastHouseholdSavingsGoals = listOf<FirebaseSavingsGoal>()
    internal var lastUserSavingsGoals = listOf<FirebaseSavingsGoal>()
    internal var lastObservedHouseholdId: String? = null
    internal var hasRunRecurringCheckThisSession = false
    internal val pendingLocalExpenseIds = java.util.concurrent.ConcurrentHashMap<String, Long>()
    internal val pendingLocalIncomeIds = java.util.concurrent.ConcurrentHashMap<String, Long>()
    internal val savingsGoalCloudIdByLocalId = java.util.concurrent.ConcurrentHashMap<Long, String>()
    internal val pendingLocalSavingsGoals = java.util.concurrent.ConcurrentHashMap<String, StartupSafety.PendingSavingsGoalMutation>()
    internal val pendingDeletedSavingsGoals = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>()
    internal val inFlightCreateGoalKeys = java.util.concurrent.ConcurrentHashMap<String, Long>()

    // Domain Data Flows
    internal val _allExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val allExpenses: StateFlow<List<Expense>> = _allExpenses.asStateFlow()

    internal val _allIncomes = MutableStateFlow<List<Income>>(emptyList())
    val allIncomes: StateFlow<List<Income>> = _allIncomes.asStateFlow()

    internal val _allMembers = MutableStateFlow<List<FamilyMember>>(emptyList())
    val allMembers: StateFlow<List<FamilyMember>> = _allMembers.asStateFlow()

    internal val _deletedMembers = MutableStateFlow<List<FamilyMember>>(emptyList())
    val deletedMembers: StateFlow<List<FamilyMember>> = _deletedMembers.asStateFlow()

    internal val _allCategories = MutableStateFlow<List<ExpenseCategory>>(emptyList())
    val allCategories: StateFlow<List<ExpenseCategory>> = _allCategories.asStateFlow()

    internal val _deletedCategories = MutableStateFlow<List<ExpenseCategory>>(emptyList())
    val deletedCategories: StateFlow<List<ExpenseCategory>> = _deletedCategories.asStateFlow()

    // 1.1 Connected users & auto-default payer
    val connectedHouseholdUsers: StateFlow<List<ConnectedHouseholdUser>> = combine(
        _currentHousehold,
        _resolvedUsersByUid,
        _currentUser,
        _userProfile
    ) { household, resolvedMap, curUser, profile ->
        householdDelegate.buildConnectedUsersList(household, resolvedMap, curUser, profile)
    }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    internal val _localDefaultPayersOverride = MutableStateFlow<Map<String, String>>(emptyMap())

    internal fun getLocalDefaultPayersCsv(householdId: String, uid: String): String? {
        if (householdId.isBlank() || uid.isBlank()) return null
        val mapKey = "$householdId|$uid"
        _localDefaultPayersOverride.value[mapKey]?.let { return it }
        val prefKey = "default_payers_v2_${householdId}_${uid}"
        return if (prefs.contains(prefKey)) prefs.getString(prefKey, "") else null
    }

    val defaultPayerMembers: StateFlow<List<FamilyMember>> = combine(
        _currentUser,
        _currentHousehold,
        _allMembers,
        _localDefaultPayersOverride
    ) { user, household, members, overrides ->
        val uid = user?.uid.orEmpty()
        val hid = household?.id.orEmpty()
        val localCsv = if (hid.isNotBlank() && uid.isNotBlank()) {
            overrides["$hid|$uid"] ?: getLocalDefaultPayersCsv(hid, uid)
        } else null
        DefaultPayerHelper.resolveEffectiveDefaultPayers(
            uid = uid,
            household = household,
            members = members,
            localOverrideCsv = localCsv
        )
    }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultPayerMember: StateFlow<FamilyMember?> = defaultPayerMembers
        .map { it.firstOrNull() }
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 1.2 Undo deletion banner state
    internal val _pendingUndoDeletion = MutableStateFlow<PendingUndoDeletion?>(null)
    val pendingUndoDeletion: StateFlow<PendingUndoDeletion?> = _pendingUndoDeletion.asStateFlow()

    // 3.1 Activity Feed
    internal val _householdActivities = MutableStateFlow<List<FirebaseHouseholdActivity>>(emptyList())
    val householdActivities: StateFlow<List<FirebaseHouseholdActivity>> = _householdActivities.asStateFlow()

    internal val _showActivityDialog = MutableStateFlow(false)
    val showActivityDialog: StateFlow<Boolean> = _showActivityDialog.asStateFlow()

    // Onboarding & Consents
    internal val _showOnboardingCarousel = MutableStateFlow(false)
    val showOnboardingCarousel = _showOnboardingCarousel.asStateFlow()

    internal val _showWelcomeCarousel = MutableStateFlow(false)
    val showWelcomeCarousel = _showWelcomeCarousel.asStateFlow()

    internal val _notificationConsentCompleted = MutableStateFlow(
        prefs.getBoolean("notification_consent_handled_${auth.currentUser?.uid ?: "guest"}", false)
    )
    val notificationConsentCompleted = _notificationConsentCompleted.asStateFlow()

    // Filters
    private data class ExpenseFilterCriteria(
        val memberId: Long?,
        val catId: Long?,
        val query: String,
        val special: AnalyticsSpecialFilter?
    )

    internal val _selectedMemberFilter = MutableStateFlow<Long?>(null)
    val selectedMemberFilter = _selectedMemberFilter.asStateFlow()

    internal val _selectedCategoryFilter = MutableStateFlow<Long?>(null)
    val selectedCategoryFilter = _selectedCategoryFilter.asStateFlow()

    internal val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    internal val _homeSelectedMonthKey = MutableStateFlow<String?>(null)
    val homeSelectedMonthKey = _homeSelectedMonthKey.asStateFlow()

    internal val _homeSelectedTypeFilter = MutableStateFlow("EXPENSE")
    val homeSelectedTypeFilter = _homeSelectedTypeFilter.asStateFlow()

    internal val _activeSpecialFilter = MutableStateFlow<AnalyticsSpecialFilter?>(null)
    val activeSpecialFilter = _activeSpecialFilter.asStateFlow()

    internal val _homeDrillDownScrollTrigger = MutableStateFlow(0)
    val homeDrillDownScrollTrigger = _homeDrillDownScrollTrigger.asStateFlow()

    internal val _homeScrollToTopTrigger = MutableStateFlow(0)
    val homeScrollToTopTrigger = _homeScrollToTopTrigger.asStateFlow()

    // Dialog States
    internal val _showManualIncomeDialog = MutableStateFlow(false)
    val showManualIncomeDialog = _showManualIncomeDialog.asStateFlow()

    internal val _editingIncome = MutableStateFlow<Income?>(null)
    val editingIncome = _editingIncome.asStateFlow()

    internal val _pendingParsedExpense = MutableStateFlow<ParsedVoiceExpense?>(null)
    val pendingParsedExpense = _pendingParsedExpense.asStateFlow()

    internal val _countdownState = MutableStateFlow(CountdownState())
    val countdownState = _countdownState.asStateFlow()

    internal val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    internal val _micSoundLevel = MutableStateFlow(0f)
    val micSoundLevel = _micSoundLevel.asStateFlow()

    internal val _showManualExpenseDialog = MutableStateFlow(false)
    val showManualExpenseDialog = _showManualExpenseDialog.asStateFlow()

    internal val _editingExpense = MutableStateFlow<Expense?>(null)
    val editingExpense = _editingExpense.asStateFlow()

    internal val _showMemberDialog = MutableStateFlow(false)
    val showMemberDialog = _showMemberDialog.asStateFlow()

    internal val _editingMember = MutableStateFlow<FamilyMember?>(null)
    val editingMember = _editingMember.asStateFlow()

    internal val _showCategoryDialog = MutableStateFlow(false)
    val showCategoryDialog = _showCategoryDialog.asStateFlow()

    internal val _editingCategory = MutableStateFlow<ExpenseCategory?>(null)
    val editingCategory = _editingCategory.asStateFlow()

    internal val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog = _showSettingsDialog.asStateFlow()

    internal val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM")
    val themeMode = _themeMode.asStateFlow()

    internal val _uiStyle = MutableStateFlow(prefs.getString("ui_style", "MODERN") ?: "MODERN")
    val uiStyle = _uiStyle.asStateFlow()

    internal val _appLanguage = MutableStateFlow(
        if (prefs.getBoolean("user_explicitly_set_language_v2", false))
            (prefs.getString("app_language", "EN") ?: "EN")
        else "EN"
    )
    val appLanguage = _appLanguage.asStateFlow()

    internal val _currencySymbol = MutableStateFlow(prefs.getString("currency_symbol", "€") ?: "€")
    val currencySymbol = _currencySymbol.asStateFlow()

    internal val _pendingInAppReviewPrompt = MutableStateFlow(false)
    val pendingInAppReviewPrompt = _pendingInAppReviewPrompt.asStateFlow()

    fun consumeInAppReviewPrompt() {
        _pendingInAppReviewPrompt.value = false
    }

    internal fun syncCurrencyFromHousehold(household: Household?) {
        val code = household?.currencyCode?.takeIf { it.isNotBlank() } ?: CurrencyFormatter.DEFAULT_CURRENCY_CODE
        val symbol = CurrencyFormatter.symbolForCode(code)
        CurrencyFormatter.updateActiveCurrency(code)
        if (_currencySymbol.value != symbol) {
            _currencySymbol.value = symbol
        }
        prefs.edit().putString("currency_symbol", symbol).putString("currency_code", code).apply()
    }

    internal val _launchWithChooser = MutableStateFlow(prefs.getBoolean("launch_with_chooser", false))
    val launchWithChooser = _launchWithChooser.asStateFlow()

    internal val _showQuickActionChooser = MutableStateFlow(false)
    val showQuickActionChooser = _showQuickActionChooser.asStateFlow()

    internal val _showVoiceInputDialog = MutableStateFlow(false)
    val showVoiceInputDialog = _showVoiceInputDialog.asStateFlow()

    internal val _showExodologioManagerDialog = MutableStateFlow(false)
    val showExodologioManagerDialog = _showExodologioManagerDialog.asStateFlow()

    internal val _showBurgerMenu = MutableStateFlow(false)
    val showBurgerMenu = _showBurgerMenu.asStateFlow()

    internal val _showUserProfileDialog = MutableStateFlow(false)
    val showUserProfileDialog = _showUserProfileDialog.asStateFlow()

    internal val _showExodologioDetailsDialog = MutableStateFlow(false)
    val showExodologioDetailsDialog = _showExodologioDetailsDialog.asStateFlow()

    internal val _legalDocumentsInitialTab = MutableStateFlow<String?>(null)
    val legalDocumentsInitialTab = _legalDocumentsInitialTab.asStateFlow()

    internal val _premiumPaywallReason = MutableStateFlow<String?>(null)
    val premiumPaywallReason = _premiumPaywallReason.asStateFlow()

    internal val _showExportReportDialog = MutableStateFlow(false)
    val showExportReportDialog = _showExportReportDialog.asStateFlow()

    internal val _showSavingsGoalDialog = MutableStateFlow(false)
    val showSavingsGoalDialog = _showSavingsGoalDialog.asStateFlow()

    internal val _showSavingsGoalsManagerDialog = MutableStateFlow(false)
    val showSavingsGoalsManagerDialog = _showSavingsGoalsManagerDialog.asStateFlow()

    internal val _editingSavingsGoal = MutableStateFlow<SavingsGoal?>(null)
    val editingSavingsGoal = _editingSavingsGoal.asStateFlow()

    internal val _showRecurringExpenseDialog = MutableStateFlow(false)
    val showRecurringExpenseDialog = _showRecurringExpenseDialog.asStateFlow()

    internal val _editingRecurringExpense = MutableStateFlow<RecurringExpense?>(null)
    val editingRecurringExpense = _editingRecurringExpense.asStateFlow()

    internal val _showRemindersManagerDialog = MutableStateFlow(false)
    val showRemindersManagerDialog = _showRemindersManagerDialog.asStateFlow()

    internal val _dismissedReminderKeys = MutableStateFlow(
        prefs.getStringSet("dismissed_reminder_keys", emptySet()).orEmpty().toSet()
    )
    internal val _manualTriggeredReminderIds = MutableStateFlow<Set<Long>>(emptySet())
    internal val _reminderClockTickMs = MutableStateFlow(System.currentTimeMillis())

    internal val _showReceiptScanDialog = MutableStateFlow(false)
    val showReceiptScanDialog = _showReceiptScanDialog.asStateFlow()

    internal val _monthlyVoiceUsage = MutableStateFlow(prefs.getInt(getMonthlyVoiceUsageKey(), 0))
    val monthlyVoiceUsage = _monthlyVoiceUsage.asStateFlow()

    val pendingBankTransactions: StateFlow<List<PendingBankTransaction>> = combine(
        BankNotificationHolder.pendingTransactions,
        _currentHousehold
    ) { transactions, household ->
        BankNotificationHolder.filterForHousehold(transactions, household?.id.orEmpty())
    }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    internal val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage = _snackbarMessage.asStateFlow()

    internal val _dailyReminderEnabled = MutableStateFlow(com.example.reminder.DailyReminderManager.isReminderEnabled(getApplication()))
    val dailyReminderEnabled = _dailyReminderEnabled.asStateFlow()

    internal val _dailyReminderHour = MutableStateFlow(com.example.reminder.DailyReminderManager.getReminderHour(getApplication()))
    val dailyReminderHour = _dailyReminderHour.asStateFlow()

    internal val _dailyReminderMinute = MutableStateFlow(com.example.reminder.DailyReminderManager.getReminderMinute(getApplication()))
    val dailyReminderMinute = _dailyReminderMinute.asStateFlow()

    internal val _bankAutoCatcherEnabled = MutableStateFlow(
        BankNotificationSystemManager.isAutoCatcherActiveForPro(getApplication())
    )
    val bankAutoCatcherEnabled: StateFlow<Boolean> = _bankAutoCatcherEnabled.asStateFlow()

    internal val _showPrivacyConsentDialog = MutableStateFlow(false)
    val showPrivacyConsentDialog = _showPrivacyConsentDialog.asStateFlow()

    val allRecurringExpenses: StateFlow<List<RecurringExpense>> = repository.allRecurringExpenses
        .map { RecurringExpenseAutoProcessor.deduplicateRecurringExpenses(it) }
        .catch { t ->
            Log.e("OikoNomosVM", "allRecurringExpenses flow error: ${t.message}", t)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    internal val _allSavingsGoals = MutableStateFlow<List<SavingsGoal>>(emptyList())
    val allSavingsGoals: StateFlow<List<SavingsGoal>> = _allSavingsGoals.asStateFlow()

    val filteredExpenses: StateFlow<List<Expense>> = combine(
        allExpenses,
        allMembers,
        allCategories,
        combine(_selectedMemberFilter, _selectedCategoryFilter, _searchQuery, _activeSpecialFilter) { memberId, catId, query, special ->
            ExpenseFilterCriteria(memberId, catId, query, special)
        }
    ) { expenses, members, categories, criteria ->
        val memberId = criteria.memberId
        val catId = criteria.catId
        val query = criteria.query
        val special = criteria.special

        expenses.filter { exp ->
            val matchMember = when {
                special?.exactMemberId != null -> true
                memberId == null -> true
                memberId == -1L -> {
                    exp.splitType == "ALL_FAMILY" &&
                        AnalyticsExpenseFilterHelper.resolveMembersForExpense(exp, members).isEmpty()
                }
                else -> {
                    val ids = exp.memberIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
                    val assigned = AnalyticsExpenseFilterHelper.resolveMembersForExpense(exp, members)
                    ids.contains(memberId) ||
                        assigned.any { it.id == memberId } ||
                        (exp.splitType == "ALL_FAMILY" && assigned.isEmpty())
                }
            }

            val matchCat = catId == null || AnalyticsExpenseFilterHelper.matchesCategory(exp, catId, categories)

            val matchSpecial = special == null ||
                AnalyticsExpenseFilterHelper.matchesSpecialExpenseFilter(exp, special, members)

            val matchQuery = if (query.isBlank()) {
                true
            } else {
                val q = GreekVoiceParser.normalizeGreek(query.trim())
                val titleNorm = GreekVoiceParser.normalizeGreek(exp.title)
                val catNorm = GreekVoiceParser.normalizeGreek(exp.categoryName)
                val membersNorm = GreekVoiceParser.normalizeGreek(exp.memberNamesDisplay)
                val noteNorm = GreekVoiceParser.normalizeGreek(exp.note)
                titleNorm.contains(q) || catNorm.contains(q) || membersNorm.contains(q) || noteNorm.contains(q)
            }

            matchMember && matchCat && matchSpecial && matchQuery
        }
    }
        .catch { t ->
            Log.e("OikoNomosVM", "filteredExpenses flow error: ${t.message}", t)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filteredIncomes: StateFlow<List<Income>> = combine(
        allIncomes,
        allMembers,
        _selectedMemberFilter,
        _searchQuery,
        _activeSpecialFilter
    ) { incomes, members, memberId, query, special ->
        incomes.filter { inc ->
            val matchMember = when {
                special?.exactMemberId != null -> true
                memberId == null -> true
                memberId == -1L -> inc.memberId == 0L || members.none { it.id == inc.memberId }
                else -> inc.memberId == memberId || inc.memberId == 0L
            }
            val matchSpecial = special == null ||
                AnalyticsExpenseFilterHelper.matchesSpecialIncomeFilter(inc, special, members)
            val matchQuery = if (query.isBlank()) {
                true
            } else {
                val q = GreekVoiceParser.normalizeGreek(query.trim())
                val titleNorm = GreekVoiceParser.normalizeGreek(inc.title)
                val catNorm = GreekVoiceParser.normalizeGreek(inc.category)
                val memberNorm = GreekVoiceParser.normalizeGreek(inc.memberName)
                val noteNorm = GreekVoiceParser.normalizeGreek(inc.note)
                titleNorm.contains(q) || catNorm.contains(q) || memberNorm.contains(q) || noteNorm.contains(q)
            }
            matchMember && matchSpecial && matchQuery
        }
    }
        .catch { t ->
            Log.e("OikoNomosVM", "filteredIncomes flow error: ${t.message}", t)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // 4.2 + 1.5: Exact day & exact hour:minute trigger (no false display on wrong day/time or household switch)
    val activeDueReminders: StateFlow<List<RecurringExpense>> = combine(
        allRecurringExpenses,
        _dismissedReminderKeys,
        _manualTriggeredReminderIds,
        combine(currentHouseholdRole, _currentHousehold, _reminderClockTickMs) { role, household, nowMs ->
            Triple(role, household?.id.orEmpty(), nowMs)
        }
    ) { allRecurring, dismissedKeys, manualTriggered, (role, hid, nowMs) ->
        if (!role.canManageMembersAndCategories) return@combine emptyList()
        allRecurring.filter { rec ->
            if (!rec.isActive) return@filter false
            val periodKey = RecurringScheduleHelper.periodKeyFor(rec, nowMs)
            val dismissKey = "${hid}|${rec.id}|${periodKey}"
            if (dismissedKeys.contains(dismissKey)) return@filter false
            if (manualTriggered.contains(rec.id)) return@filter true
            RecurringScheduleHelper.isReminderDueAtExactTime(rec, nowMs)
        }
    }
        .catch { t ->
            Log.e("OikoNomosVM", "activeDueReminders flow error: ${t.message}", t)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        try {
            val hasExplicitLanguage = prefs.getBoolean("user_explicitly_set_language_v2", false)
            val savedLang = if (hasExplicitLanguage) (prefs.getString("app_language", "EN") ?: "EN") else "EN"
            auth.setLanguageCode(if (savedLang == "EN") "en" else "el")
        } catch (t: Throwable) {
            Log.w("OikoNomosVM", "auth.setLanguageCode warning: ${t.message}")
        }

        CurrencyFormatter.updateActiveCurrency(_currencySymbol.value)
        viewModelScope.launch(coroutineExceptionHandler) {
            _currentHousehold.collect { household ->
                if (household != null) {
                    syncCurrencyFromHousehold(household)
                }
            }
        }

        val clearJob = viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            try {
                repository.clearAllData()
            } catch (t: Throwable) {
                Log.d("OikoNomosVM", "Room clear on start: ${t.message}")
            }
        }
        roomClearJob = clearJob

        _allExpenses.value = emptyList()
        _allIncomes.value = emptyList()
        _allMembers.value = emptyList()
        _allCategories.value = emptyList()
        _allSavingsGoals.value = emptyList()
        lastHouseholdSavingsGoals = emptyList()
        lastUserSavingsGoals = emptyList()
        savingsGoalCloudIdByLocalId.clear()
        pendingLocalSavingsGoals.clear()
        pendingDeletedSavingsGoals.clear()
        inFlightCreateGoalKeys.clear()
        _isInitialExpensesLoading.value = true

        val existingUser = try { auth.currentUser } catch (_: Throwable) { null }
        if (existingUser != null) {
            try {
                if (prefs.getString("cached_active_uid", null).isNullOrBlank()) {
                    prefs.edit().putString("cached_active_uid", existingUser.uid).apply()
                }
                if (isUserVerified(existingUser)) {
                    val cachedProfile = authDelegate.loadUserProfileFromPrefs()
                    if (cachedProfile != null && (cachedProfile.uid == existingUser.uid || cachedProfile.email.equals(existingUser.email, ignoreCase = true))) {
                        _userProfile.value = cachedProfile
                    }
                    syncBankCatcherProEntitlement()
                    val prewarmHid = getOrEnsureHouseholdId(existingUser)
                    if (prewarmHid.isNotBlank()) {
                        viewModelScope.launch(coroutineExceptionHandler) {
                            try {
                                clearJob.join()
                                observeHouseholdData(prewarmHid)
                            } catch (t: Throwable) {
                                if (t is CancellationException) throw t
                                Log.w("OikoNomosVM", "Prewarm hid observation error: ${t.message}")
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.w("OikoNomosVM", "Startup prewarm warning: ${t.message}")
            }
            viewModelScope.launch(coroutineExceptionHandler) {
                try {
                    existingUser.reload().await()
                    val refreshed = auth.currentUser ?: existingUser
                    if (!isUserVerified(refreshed)) {
                        _isUserVerifiedState.value = false
                        cancelAllSyncJobs()
                    }
                } catch (e: FirebaseAuthInvalidUserException) {
                    Log.w("OikoNomosVM", "Startup user no longer valid: ${e.message}")
                    signOutUser()
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    Log.w("OikoNomosVM", "Startup reload warning: ${t.message}")
                }
            }
        }

        var lastObservedUid: String? = null
        auth.addAuthStateListener { firebaseAuth ->
            try {
                if (suppressAuthStateListener) return@addAuthStateListener
                val user = firebaseAuth.currentUser
                val currentUid = user?.uid
                _isAuthInitialized.value = true
                val verified = isUserVerified(user)
                _isUserVerifiedState.value = verified
                _notificationConsentCompleted.value = prefs.getBoolean("notification_consent_handled_${currentUid ?: "guest"}", false)
                val activeUidInPrefs = prefs.getString("cached_active_uid", null)
                val isUserSwitch = StartupSafety.isActualUserSwitch(activeUidInPrefs, currentUid)
                if (isUserSwitch) {
                    syncDelegate.resetStateForUserSwitch(user, currentUid)
                }

                if (user != null) {
                    if (activeUidInPrefs.isNullOrBlank()) {
                        prefs.edit().putString("cached_active_uid", user.uid).apply()
                    }
                    _currentUser.value = user
                    val uidChanged = lastObservedUid != currentUid
                    lastObservedUid = currentUid
                    if (verified) {
                        syncDelegate.seedInitialHouseholdPlaceholderOnAuth(user)
                        if (uidChanged || isUserSwitch || _userProfile.value == null) {
                            scheduleLoadUserProfileAndHousehold(user)
                        }
                    }
                } else {
                    _currentUser.value = null
                    _isUserVerifiedState.value = false
                    _userProfile.value = null
                    _currentHousehold.value = null
                    _userHouseholds.value = emptyList()
                    _showOnboardingCarousel.value = false
                    _showWelcomeCarousel.value = false
                    syncBankCatcherProEntitlement()
                    _allExpenses.value = emptyList()
                    _allIncomes.value = emptyList()
                    _allMembers.value = emptyList()
                    _allCategories.value = emptyList()
                    _allSavingsGoals.value = emptyList()
                    _householdActivities.value = emptyList()
                    lastHouseholdSavingsGoals = emptyList()
                    lastUserSavingsGoals = emptyList()
                    savingsGoalCloudIdByLocalId.clear()
                    pendingLocalSavingsGoals.clear()
                    pendingDeletedSavingsGoals.clear()
                    inFlightCreateGoalKeys.clear()
                    lastObservedUid = null
                }
            } catch (t: Throwable) {
                Log.e("OikoNomosVM", "AuthStateListener error: ${t.message}", t)
                _isAuthInitialized.value = true
            }
        }
        if (!_isAuthInitialized.value) {
            viewModelScope.launch(coroutineExceptionHandler) {
                kotlinx.coroutines.delay(2_000L)
                if (!_isAuthInitialized.value) {
                    _currentUser.value = auth.currentUser
                    _isUserVerifiedState.value = isUserVerified(auth.currentUser)
                    _isAuthInitialized.value = true
                }
            }
        }
        reminderClockJob = viewModelScope.launch(coroutineExceptionHandler) {
            while (true) {
                kotlinx.coroutines.delay(30_000L)
                _reminderClockTickMs.value = System.currentTimeMillis()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        networkMonitor.unregister()
        undoCountdownJob?.cancel()
        reminderClockJob?.cancel()
        cancelAllSyncJobs()
    }

    internal fun trackSyncStart() {
        _activeSyncOperations.value = _activeSyncOperations.value + 1
    }

    internal fun trackSyncEnd() {
        _activeSyncOperations.value = (_activeSyncOperations.value - 1).coerceAtLeast(0)
    }

    internal fun cancelAllSyncJobs() {
        householdJob?.cancel()
        expensesSyncJob?.cancel()
        userExpensesSyncJob?.cancel()
        incomesSyncJob?.cancel()
        userIncomesSyncJob?.cancel()
        membersSyncJob?.cancel()
        userMembersSyncJob?.cancel()
        categoriesSyncJob?.cancel()
        userCategoriesSyncJob?.cancel()
        savingsGoalsSyncJob?.cancel()
        userSavingsGoalsSyncJob?.cancel()
        recurringSyncJob?.cancel()
        userRecurringSyncJob?.cancel()
        activitySyncJob?.cancel()
        userProfileSyncJob?.cancel()
        userHouseholdsSyncJob?.cancel()
    }

    internal fun scheduleLoadUserProfileAndHousehold(user: FirebaseUser, force: Boolean = false) =
        syncDelegate.scheduleLoadUserProfileAndHousehold(user, force)

    internal suspend fun loadUserProfileAndHousehold(user: FirebaseUser) =
        syncDelegate.loadUserProfileAndHousehold(user)

    internal fun observeHouseholdData(householdId: String) =
        syncDelegate.observeHouseholdData(householdId)

    internal fun observeUserData(userId: String) =
        syncDelegate.observeUserData(userId)

    internal suspend fun syncAndMergeMembers(
        incomingMembers: List<FirebaseMember>,
        explicitProfile: UserProfile? = null,
        explicitUser: FirebaseUser? = null,
        explicitHouseholdId: String? = null
    ) = structureDelegate.syncAndMergeMembers(incomingMembers, explicitProfile, explicitUser, explicitHouseholdId)

    internal suspend fun syncAndMergeCategories(
        incomingCats: List<FirebaseCategory>,
        explicitHouseholdId: String? = null,
        explicitUserId: String? = null
    ) = structureDelegate.syncAndMergeCategories(incomingCats, explicitHouseholdId, explicitUserId)

    internal fun logHouseholdActivity(
        actionType: String,
        targetType: String,
        summaryEl: String,
        summaryEn: String,
        amount: Double = 0.0,
        householdIdOverride: String? = null
    ) = syncDelegate.logHouseholdActivity(actionType, targetType, summaryEl, summaryEn, amount, householdIdOverride)

    internal fun resolveDefaultPayerMembers(
        targetUserId: String? = null,
        household: Household? = _currentHousehold.value
    ): List<FamilyMember> {
        val uid = targetUserId ?: _currentUser.value?.uid.orEmpty()
        val hid = household?.id.orEmpty()
        val localCsv = if (hid.isNotBlank() && uid.isNotBlank()) getLocalDefaultPayersCsv(hid, uid) else null
        return DefaultPayerHelper.resolveEffectiveDefaultPayers(
            uid = uid,
            household = household,
            members = _allMembers.value,
            localOverrideCsv = localCsv
        )
    }

    internal fun resolveDefaultPayerMember(targetUserId: String? = null): FamilyMember? {
        return resolveDefaultPayerMembers(targetUserId).firstOrNull()
    }

    internal fun getMonthlyVoiceUsageKey(): String {
        val uid = _currentUser.value?.uid ?: auth.currentUser?.uid ?: "guest"
        val monthKey = SimpleDateFormat("yyyy_MM", Locale.ROOT).format(Date())
        return "voice_usage_${uid}_${monthKey}"
    }

    // =========================================================================
    // Public API (Preserved 100% + Features 1.1..4.2)
    // =========================================================================

    fun getOrEnsureHouseholdId(user: FirebaseUser? = null): String =
        householdDelegate.getOrEnsureHouseholdId(user)

    fun getUserRoleForHousehold(household: Household?, user: FirebaseUser? = _currentUser.value): HouseholdRole =
        householdDelegate.getUserRoleForHousehold(household, user)

    fun isUserCreatorOfHousehold(household: Household?): Boolean = getUserRoleForHousehold(household) == HouseholdRole.CREATOR
    fun isUserOwnerOfHousehold(household: Household?): Boolean = isUserCreatorOfHousehold(household)
    fun canUserManageMembersAndCategories(household: Household?): Boolean = getUserRoleForHousehold(household).canManageMembersAndCategories
    fun hasCreatorPermissions(): Boolean = _currentHousehold.value?.let { isUserCreatorOfHousehold(it) } ?: false
    fun hasOwnerPermissions(): Boolean = _currentHousehold.value?.let { canUserManageMembersAndCategories(it) } ?: false

    fun clearVerifiedUserLookupState() { _verifiedUserLookupState.value = VerifiedUserLookupState.Idle }
    fun createHousehold(familyName: String) = householdDelegate.createHousehold(familyName)
    fun joinHousehold(code: String, role: String? = null) = householdDelegate.joinHousehold(code, role)
    fun updateHouseholdInviteRole(role: String) = householdDelegate.updateHouseholdInviteRole(role)
    fun searchVerifiedUserForSharing(email: String, householdId: String) = householdDelegate.searchVerifiedUserForSharing(email, householdId)
    fun shareHouseholdWithVerifiedUser(householdId: String, targetUser: UserProfile, role: HouseholdRole) = householdDelegate.shareHouseholdWithVerifiedUser(householdId, targetUser, role)
    fun updateSharedUserRole(householdId: String, targetUid: String, targetEmail: String, newRole: HouseholdRole) = householdDelegate.updateSharedUserRole(householdId, targetUid, targetEmail, newRole)
    fun removeSharedUserFromHousehold(householdId: String, targetUid: String, targetEmail: String) = householdDelegate.removeSharedUserFromHousehold(householdId, targetUid, targetEmail)
    fun inviteMemberByEmail(email: String, role: String = "REGISTAR", context: Context? = null, onComplete: ((Boolean, String) -> Unit)? = null) = householdDelegate.inviteMemberByEmail(email, role, context, onComplete)
    fun removeMemberByEmail(email: String) = householdDelegate.removeMemberByEmail(email)
    fun assignUserDefaultPayer(targetUid: String, memberId: Long?) =
        householdDelegate.assignUserDefaultPayers(_currentHousehold.value?.id.orEmpty(), targetUid, listOfNotNull(memberId))
    fun assignUserDefaultPayers(householdId: String, targetUid: String, memberIds: List<Long>) =
        householdDelegate.assignUserDefaultPayers(householdId, targetUid, memberIds)
    fun isCurrentUserPro(): Boolean = _isProUser.value

    internal fun syncBankCatcherProEntitlement() {
        val isPro = isCurrentUserPro()
        BankNotificationSystemManager.setProEntitled(getApplication(), isPro)
        _bankAutoCatcherEnabled.value = isPro && BankNotificationSystemManager.isAutoCatcherEnabled(getApplication())
    }
    fun deleteHousehold(household: Household) = householdDelegate.deleteHousehold(household)
    suspend fun refreshUserHouseholds() = householdDelegate.refreshUserHouseholds()
    fun isMyHousehold(household: Household): Boolean = isUserCreatorOfHousehold(household)
    fun leaveSharedHousehold(household: Household) = householdDelegate.leaveSharedHousehold(household)
    fun renameHousehold(householdId: String, newName: String) = householdDelegate.renameHousehold(householdId, newName)
    fun renameCurrentHousehold(newName: String) = householdDelegate.renameCurrentHousehold(newName)
    fun updateHouseholdCurrency(householdId: String, currencyCode: String) = householdDelegate.updateHouseholdCurrency(householdId, currencyCode)
    fun createNewExodologio(name: String, currencyCode: String = "EUR") = householdDelegate.createNewExodologio(name, currencyCode)
    fun switchExodologio(household: Household) = householdDelegate.switchExodologio(household)

    fun onAppResumed() = syncDelegate.onAppResumed()
    fun refreshCloudSync() = syncDelegate.refreshCloudSync()

    fun acceptNotificationConsent() = authDelegate.acceptNotificationConsent()
    fun skipNotificationConsent() = authDelegate.skipNotificationConsent()
    fun isUserVerified(user: FirebaseUser?): Boolean = authDelegate.isUserVerified(user)
    fun checkEmailVerification(onResult: (Boolean) -> Unit) = authDelegate.checkEmailVerification(onResult)
    fun resendVerificationEmail(onSuccess: () -> Unit, onError: (String) -> Unit) = authDelegate.resendVerificationEmail(onSuccess, onError)
    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) = authDelegate.sendPasswordResetEmail(email, onSuccess, onError)
    fun signInWithEmail(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) = authDelegate.signInWithEmail(email, pass, onSuccess, onError)
    fun signUpWithEmail(firstName: String, lastName: String, birthDate: String, phoneNumber: String, occupation: String, city: String, email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) =
        authDelegate.signUpWithEmail(firstName, lastName, birthDate, phoneNumber, occupation, city, email, pass, onSuccess, onError)
    fun signInWithGoogleToken(idToken: String, isSignUpMode: Boolean = false, onSuccess: () -> Unit, onError: (String) -> Unit) =
        authDelegate.signInWithGoogleToken(idToken, isSignUpMode, onSuccess, onError)
    fun signOutUser() = authDelegate.signOutUser()
    fun factoryResetAndSignOut() = authDelegate.factoryResetAndSignOut()
    fun deleteCurrentUserAccount() = authDelegate.deleteCurrentUserAccount()
    fun updateUserDisplayName(newName: String) = authDelegate.updateUserDisplayName(newName)
    fun updateUserFullProfile(firstName: String, lastName: String, birthDate: String, phoneNumber: String, occupation: String, city: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) =
        authDelegate.updateUserFullProfile(firstName, lastName, birthDate, phoneNumber, occupation, city, onSuccess, onError)
    fun updateUserProfilePhoto(photoUrl: String) = authDelegate.updateUserProfilePhoto(photoUrl)
    fun acceptPrivacyConsent() = authDelegate.acceptPrivacyConsent()
    fun openPrivacyConsentDialog() { _showPrivacyConsentDialog.value = true }
    fun dismissPrivacyConsentDialog() { _showPrivacyConsentDialog.value = false }
    fun dismissOnboardingCarousel() = authDelegate.dismissOnboardingCarousel()
    fun dismissWelcomeCarousel() = authDelegate.dismissWelcomeCarousel()
    fun openOnboardingGuide() {
        dismissAllSubPages()
        _showOnboardingCarousel.value = true
    }

    // Transactions, Undo, Settle-Up, Smart Learning, Voice, Receipt, Bank
    fun refreshMonthlyVoiceUsage() { _monthlyVoiceUsage.value = prefs.getInt(getMonthlyVoiceUsageKey(), 0) }
    fun openVoiceInputDialog() = transactionDelegate.openVoiceInputDialog()
    fun dismissVoiceInputDialog() { _showVoiceInputDialog.value = false }
    fun openReceiptScanDialog() {
        if (!isCurrentUserPro()) {
            showPremiumPaywall("ocr_receipt")
            return
        }
        _showReceiptScanDialog.value = true
    }
    fun dismissReceiptScanDialog() { _showReceiptScanDialog.value = false }
    fun suggestCategoryForMerchant(merchantOrText: String, merchantAfm: String = ""): ExpenseCategory? =
        transactionDelegate.suggestCategoryForMerchant(merchantOrText, merchantAfm)
    fun learnCategoryForMerchant(merchantOrText: String, categoryId: Long, categoryName: String, merchantAfm: String = "") =
        transactionDelegate.learnCategoryForMerchant(merchantOrText, categoryId, categoryName, merchantAfm)
    fun resolveSuggestedCategoryForBankTx(tx: PendingBankTransaction): ExpenseCategory? =
        transactionDelegate.resolveSuggestedCategoryForBankTx(tx)
    fun confirmBankTransaction(tx: PendingBankTransaction) = transactionDelegate.confirmBankTransaction(tx)
    fun dismissBankTransaction(tx: PendingBankTransaction) = transactionDelegate.dismissBankTransaction(tx)
    fun processVoiceInput(spokenText: String) = transactionDelegate.processVoiceInput(spokenText)
    fun cancelPendingExpense() = transactionDelegate.cancelPendingExpense()
    fun confirmVoiceExpense(title: String, amount: Double, categoryId: Long, categoryName: String, timestamp: Long, splitType: String, memberIds: List<Long>, memberNamesDisplay: String, originalPrompt: String, payerMemberId: Long = 0L, payerMemberName: String = "", payerMemberIdsCsv: String = "", payerAmountsCsv: String = "") =
        transactionDelegate.confirmVoiceExpense(title, amount, categoryId, categoryName, timestamp, splitType, memberIds, memberNamesDisplay, originalPrompt, payerMemberId, payerMemberName, payerMemberIdsCsv, payerAmountsCsv)
    fun confirmPendingExpense() = transactionDelegate.confirmPendingExpense()
    fun editPendingExpense() = transactionDelegate.editPendingExpense()
    fun openManualExpenseDialog(expense: Expense? = null) { _editingExpense.value = expense; _showManualExpenseDialog.value = true }
    fun dismissManualExpenseDialog() {
        transactionDelegate.clearPendingBankMerchantForLearning()
        _editingExpense.value = null
        _showManualExpenseDialog.value = false
    }
    fun saveExpense(id: Long = 0, title: String, amount: Double, categoryId: Long, categoryName: String, timestamp: Long, splitType: String, memberIds: List<Long>, memberNamesDisplay: String, note: String, payerMemberId: Long = 0L, payerMemberName: String = "", payerMemberIdsCsv: String = "", payerAmountsCsv: String = "") =
        transactionDelegate.saveExpense(id, title, amount, categoryId, categoryName, timestamp, splitType, memberIds, memberNamesDisplay, note, payerMemberId, payerMemberName, payerMemberIdsCsv, payerAmountsCsv)
    fun duplicateExpenseToday(expense: Expense) = transactionDelegate.duplicateExpenseToday(expense)
    fun deleteExpense(expense: Expense) = transactionDelegate.deleteExpense(expense)
    fun openManualIncomeDialog(income: Income? = null) { _editingIncome.value = income; _showManualIncomeDialog.value = true }
    fun dismissManualIncomeDialog() { _editingIncome.value = null; _showManualIncomeDialog.value = false }
    fun saveIncome(id: Long = 0, title: String, amount: Double, memberId: Long, memberName: String, category: String, timestamp: Long, note: String) =
        transactionDelegate.saveIncome(id, title, amount, memberId, memberName, category, timestamp, note)
    fun deleteIncome(income: Income) = transactionDelegate.deleteIncome(income)
    fun undoPendingDeletion() = transactionDelegate.undoPendingDeletion()
    fun dismissUndoDeletion() = transactionDelegate.dismissUndoDeletion()
    fun recordSettlementTransfer(fromMember: FamilyMember, toMember: FamilyMember, amount: Double, targetPeriodTimestamp: Long) =
        transactionDelegate.recordSettlementTransfer(fromMember, toMember, amount, targetPeriodTimestamp)

    // Recurring Expenses, Reminders & Savings Goals (RBAC protected 4.2 + Frequencies 1.5)
    fun openSavingsGoalsManagerDialog() {
        dismissAllSubPages()
        _showSavingsGoalsManagerDialog.value = true
    }
    fun dismissSavingsGoalsManagerDialog() { _showSavingsGoalsManagerDialog.value = false }
    fun openSavingsGoalDialog(goal: SavingsGoal? = null) = recurringGoalsDelegate.openSavingsGoalDialog(goal)
    fun dismissSavingsGoalDialog() { _editingSavingsGoal.value = null; _showSavingsGoalDialog.value = false }
    fun saveSavingsGoal(
        id: Long,
        title: String,
        targetAmount: Double,
        currentAmount: Double,
        deadline: String,
        note: String,
        recordAsExpense: Boolean = false
    ) = recurringGoalsDelegate.saveSavingsGoal(id, title, targetAmount, currentAmount, deadline, note, recordAsExpense)
    fun deleteSavingsGoal(goal: SavingsGoal) = recurringGoalsDelegate.deleteSavingsGoal(goal)
    fun depositToGoal(
        goal: SavingsGoal,
        amount: Double,
        payerMember: FamilyMember? = null,
        note: String = ""
    ) = recurringGoalsDelegate.depositToGoal(goal, amount, payerMember, note)
    internal fun adjustLinkedGoalForExpenseDelta(expenseCloudId: String, amountDelta: Double) =
        recurringGoalsDelegate.adjustLinkedGoalForExpenseDelta(expenseCloudId, amountDelta)
    fun openRecurringExpenseDialog(recurring: RecurringExpense? = null) = recurringGoalsDelegate.openRecurringExpenseDialog(recurring)
    fun dismissRecurringExpenseDialog() { _editingRecurringExpense.value = null; _showRecurringExpenseDialog.value = false }
    fun saveRecurringExpense(id: Long, title: String, amount: Double, categoryId: Long, categoryName: String, dayOfMonth: Int, note: String, payerMemberId: Long = 0L, payerMemberName: String = "", reminderHour: Int = 9, reminderMinute: Int = 0, splitType: String = "ALL_FAMILY", memberIds: List<Long> = emptyList(), memberNamesDisplay: String = "Κοινό Οικογένειας", payerMemberIdsCsv: String = "", payerAmountsCsv: String = "", autoRegisterIfDue: Boolean = true, frequency: String = "MONTHLY", anchorMonth: Int = 0) =
        recurringGoalsDelegate.saveRecurringExpense(id, title, amount, categoryId, categoryName, dayOfMonth, note, payerMemberId, payerMemberName, reminderHour, reminderMinute, splitType, memberIds, memberNamesDisplay, payerMemberIdsCsv, payerAmountsCsv, autoRegisterIfDue, frequency, anchorMonth)
    fun deleteRecurringExpense(recurring: RecurringExpense) = recurringGoalsDelegate.deleteRecurringExpense(recurring)
    fun toggleRecurringActive(recurring: RecurringExpense) = recurringGoalsDelegate.toggleRecurringActive(recurring)
    fun executeRecurringExpenseNow(recurring: RecurringExpense) = recurringGoalsDelegate.executeRecurringExpenseNow(recurring)
    fun checkAndProcessDueRecurringExpenses(force: Boolean = false) = recurringGoalsDelegate.checkAndProcessDueRecurringExpenses(force)
    fun openRemindersManagerDialog() = recurringGoalsDelegate.openRemindersManagerDialog()
    fun dismissRemindersManagerDialog() { _showRemindersManagerDialog.value = false }
    fun triggerReminderById(reminderId: Long) = recurringGoalsDelegate.triggerReminderById(reminderId)
    fun dismissDueReminder(reminder: RecurringExpense) = recurringGoalsDelegate.dismissDueReminder(reminder)
    fun logExpenseFromReminder(reminder: RecurringExpense) = recurringGoalsDelegate.logExpenseFromReminder(reminder)

    // Structure (Members, Categories, Deleted Items) & UI Settings
    fun getDeletedMemberIds(householdId: String): Set<String> = structureDelegate.getDeletedMemberIds(householdId)
    fun getDeletedCategoryIds(householdId: String): Set<String> = structureDelegate.getDeletedCategoryIds(householdId)
    fun loadDeletedItems(householdId: String) = structureDelegate.loadDeletedItems(householdId)
    fun seedStandardCategories() = structureDelegate.seedStandardCategories()
    fun openMemberDialog(member: FamilyMember? = null) = structureDelegate.openMemberDialog(member)
    fun dismissMemberDialog() { _editingMember.value = null; _showMemberDialog.value = false }
    fun saveMember(id: Long = 0, name: String, role: String, avatarColorHex: Long, voiceKeywords: String, linkedUserUid: String = "") =
        structureDelegate.saveMember(id, name, role, avatarColorHex, voiceKeywords, linkedUserUid)
    fun updateMemberColor(member: FamilyMember, newColorHex: Long) = structureDelegate.updateMemberColor(member, newColorHex)
    fun deleteMember(member: FamilyMember) = softDeleteMember(member)
    fun softDeleteMember(member: FamilyMember) = structureDelegate.softDeleteMember(member)
    fun restoreMember(member: FamilyMember) = structureDelegate.restoreMember(member)
    fun permanentDeleteMember(member: FamilyMember) = structureDelegate.permanentDeleteMember(member)
    fun openCategoryDialog(category: ExpenseCategory? = null) = structureDelegate.openCategoryDialog(category)
    fun dismissCategoryDialog() { _editingCategory.value = null; _showCategoryDialog.value = false }
    fun saveCategory(id: Long, name: String, iconKey: String, monthlyBudget: Double, colorHex: Long, voiceKeywords: String, fontColorHex: Long = 0L) =
        structureDelegate.saveCategory(id, name, iconKey, monthlyBudget, colorHex, voiceKeywords, fontColorHex)
    fun deleteCategory(category: ExpenseCategory) = softDeleteCategory(category)
    fun softDeleteCategory(category: ExpenseCategory) = structureDelegate.softDeleteCategory(category)
    fun restoreCategory(category: ExpenseCategory) = structureDelegate.restoreCategory(category)
    fun permanentDeleteCategory(category: ExpenseCategory) = structureDelegate.permanentDeleteCategory(category)
    fun moveMemberUp(member: FamilyMember) = structureDelegate.moveMemberUp(member)
    fun moveMemberDown(member: FamilyMember) = structureDelegate.moveMemberDown(member)
    fun moveCategoryUp(category: ExpenseCategory) = structureDelegate.moveCategoryUp(category)
    fun moveCategoryDown(category: ExpenseCategory) = structureDelegate.moveCategoryDown(category)
    fun clearCurrentExodologioExpenses() = structureDelegate.clearCurrentExodologioExpenses()
    fun clearCurrentHouseholdAllData() = structureDelegate.clearCurrentHouseholdAllData()

    // Simple UI State & Preferences Helpers
    fun openBurgerMenu() { _showBurgerMenu.value = true }
    fun dismissBurgerMenu() { _showBurgerMenu.value = false }
    fun dismissAllSubPages() {
        _showUserProfileDialog.value = false
        _showExodologioDetailsDialog.value = false
        _showRemindersManagerDialog.value = false
        _showSavingsGoalsManagerDialog.value = false
        _showSettingsDialog.value = false
        _legalDocumentsInitialTab.value = null
        if (_showOnboardingCarousel.value) {
            dismissOnboardingCarousel()
        }
    }
    fun openLegalDocumentsPage(initialTab: String) {
        dismissAllSubPages()
        _legalDocumentsInitialTab.value = initialTab
    }
    fun dismissLegalDocumentsPage() { _legalDocumentsInitialTab.value = null }
    fun openUserProfileDialog() {
        dismissAllSubPages()
        _showUserProfileDialog.value = true
    }
    fun dismissUserProfileDialog() { _showUserProfileDialog.value = false }
    fun openExodologioDetailsDialog() { householdDelegate.openExodologioDetailsDialog() }
    fun dismissExodologioDetailsDialog() { _showExodologioDetailsDialog.value = false }
    fun openExodologioManager() { householdDelegate.openExodologioManager() }
    fun dismissExodologioManager() { _showExodologioManagerDialog.value = false }
    fun openActivityDialog() { _showActivityDialog.value = true }
    fun dismissActivityDialog() { _showActivityDialog.value = false }
    fun showPremiumPaywall(reason: String) { _premiumPaywallReason.value = reason }
    fun dismissPremiumPaywall() { _premiumPaywallReason.value = null }
    fun activateProFromBilling() = structureDelegate.activateProFromBilling()
    fun unlockPro() = structureDelegate.unlockPro()
    fun revokePro() = structureDelegate.revokePro()
    fun onPurchasePending() = structureDelegate.onPurchasePending()
    fun openExportReportDialog() {
        if (!isCurrentUserPro()) {
            showPremiumPaywall("export_report")
            return
        }
        _showExportReportDialog.value = true
    }
    fun dismissExportReportDialog() { _showExportReportDialog.value = false }
    fun setLaunchWithChooser(enabled: Boolean) { _launchWithChooser.value = enabled; prefs.edit().putBoolean("launch_with_chooser", enabled).apply() }
    fun openSettingsDialog() {
        dismissAllSubPages()
        _showSettingsDialog.value = true
    }
    fun dismissSettingsDialog() { _showSettingsDialog.value = false }
    fun setThemeMode(mode: String, syncToCloud: Boolean = true) = structureDelegate.setThemeMode(mode, syncToCloud)
    fun setUiStyle(style: String) { _uiStyle.value = style; prefs.edit().putString("ui_style", style).apply() }
    fun setAppLanguage(lang: String, syncToCloud: Boolean = true) = structureDelegate.setAppLanguage(lang, syncToCloud)
    fun setCurrencySymbol(symbol: String) { _currencySymbol.value = symbol; prefs.edit().putString("currency_symbol", symbol).apply() }
    fun openQuickActionChooser() { _showQuickActionChooser.value = true }
    fun dismissQuickActionChooser() { _showQuickActionChooser.value = false }
    fun setMemberFilter(memberId: Long?) = structureDelegate.setMemberFilter(memberId)
    fun setCategoryFilter(categoryId: Long?) = structureDelegate.setCategoryFilter(categoryId)
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setHomeSelectedMonthKey(monthKey: String?) { _homeSelectedMonthKey.value = monthKey }
    fun setHomeSelectedTypeFilter(typeFilter: String) { _homeSelectedTypeFilter.value = typeFilter }
    fun clearSpecialDrillDownFilter() = structureDelegate.clearSpecialDrillDownFilter()
    fun applyHomeDrillDown(request: HomeDrillDownRequest) = structureDelegate.applyHomeDrillDown(request)
    fun requestHomeScrollToTop() { _homeScrollToTopTrigger.value += 1 }
    fun setListening(listening: Boolean) { _isListening.value = listening; if (!listening) _micSoundLevel.value = 0f }
    fun setMicSoundLevel(level: Float) { _micSoundLevel.value = level }
    fun showSnackbar(message: String) { _snackbarMessage.value = message }
    fun clearSnackbar() { _snackbarMessage.value = null }
    fun toggleDailyReminder(enabled: Boolean) = structureDelegate.toggleDailyReminder(enabled)
    fun setDailyReminderTime(hour: Int, minute: Int) = structureDelegate.setDailyReminderTime(hour, minute)
    fun toggleBankAutoCatcher(enabled: Boolean) = structureDelegate.toggleBankAutoCatcher(enabled)

    companion object {
        val STANDARD_DEFAULT_CATEGORIES: List<ExpenseCategory> = listOf(
            ExpenseCategory(1L, "Σούπερ μάρκετ", "cart", 0.0, 4278556265L, "σούπερ μάρκετ, σουπερμάρκετ, lidl, σκλαβενίτης, βασιλόπουλος, μάρκετ, ψώνια", 0, 0L),
            ExpenseCategory(2L, "Φαγητό & Καφές", "food", 0.0, 4278556265L, "φαγητό, καφές, delivery, σουβλάκια, πίτσα, εστιατόριο", 1, 0L),
            ExpenseCategory(3L, "Λογαριασμοί & Σπίτι", "house", 0.0, 4279080072L, "λογαριασμός, ρεύμα, νερό, ενοίκιο, κοινόχρηστα, ιντερνετ, τηλέφωνο", 2, 0L),
            ExpenseCategory(4L, "Μετακινήσεις & Καύσιμα", "gas_station", 0.0, 4293548044L, "βενζίνη, καύσιμα, διόδια, πάρκινγκ, λεωφορείο, μετρό, ταξί", 3, 0L),
            ExpenseCategory(5L, "Υγεία & Φαρμακείο", "medical", 0.0, 4293675161L, "γιατρός, φαρμακείο, φάρμακα, εξετάσεις", 4, 0L),
            ExpenseCategory(6L, "Διασκέδαση & Ψυχαγωγία", "entertainment", 0.0, 4287323382L, "διασκέδαση, έξοδος, ποτό, σινεμά", 5, 0L),
            ExpenseCategory(7L, "Άλλο", "other", 0.0, 4283124564L, "άλλο, διάφορα", 6, 0L)
        )
    }
}
