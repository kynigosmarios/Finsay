package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.data.entity.Expense
import com.example.data.entity.Income
import com.example.data.entity.RecurringExpense
import com.example.data.entity.SavingsGoal
import com.example.data.model.FirebaseExpense
import com.example.data.model.FirebaseHouseholdActivity
import com.example.data.model.FirebaseIncome
import com.example.data.model.Household
import com.example.data.model.HouseholdRole
import com.example.data.model.UserProfile
import com.example.reminder.BillReminderManager
import com.example.util.HouseholdActivityNotificationManager
import com.example.util.RecurringExpenseAutoProcessor
import com.example.util.StartupSafety
import com.google.firebase.auth.FirebaseUser
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

internal class OikoNomosSyncDelegate(
    private val vm: OikoNomosViewModel
) {
    private val tag = "OikoNomosSync"

    fun resetStateForUserSwitch(user: FirebaseUser?, currentUid: String?) {
        vm.cancelAllSyncJobs()
        vm.loadProfileJob?.cancel()
        vm.activeLoadProfileUid = null
        vm.lastObservedHouseholdId = null
        vm.hasRunRecurringCheckThisSession = false
        vm.pendingLocalExpenseIds.clear()
        vm.pendingLocalIncomeIds.clear()
        vm.savingsGoalCloudIdByLocalId.clear()
        vm.pendingLocalSavingsGoals.clear()
        vm.pendingDeletedSavingsGoals.clear()
        vm.inFlightCreateGoalKeys.clear()
        vm.lastHouseholdExpenses = emptyList()
        vm.lastUserExpenses = emptyList()
        vm.lastHouseholdIncomes = emptyList()
        vm.lastUserIncomes = emptyList()
        vm.lastHouseholdMembers = emptyList()
        vm.lastUserMembers = emptyList()
        vm.lastHouseholdCategories = emptyList()
        vm.lastUserCategories = emptyList()
        vm.lastHouseholdSavingsGoals = emptyList()
        vm.lastUserSavingsGoals = emptyList()

        vm._allExpenses.value = emptyList()
        vm._allIncomes.value = emptyList()
        vm._allMembers.value = emptyList()
        vm._allCategories.value = emptyList()
        vm._incomeCategories.value = emptyList()
        vm._allSavingsGoals.value = emptyList()
        vm._deletedMembers.value = emptyList()
        vm._deletedCategories.value = emptyList()
        vm._householdActivities.value = emptyList()
        vm._pendingUndoDeletion.value = null
        vm.undoCountdownJob?.cancel()
        vm._userHouseholds.value = emptyList()
        vm._currentHousehold.value = null
        vm._userProfile.value = null
        vm._isProUser.value = false
        vm.syncBankCatcherProEntitlement()
        vm._resolvedUsersByUid.value = emptyMap()

        if (!currentUid.isNullOrBlank()) {
            vm.prefs.edit()
                .remove("last_household_id")
                .putString("cached_active_uid", currentUid)
                .apply()
        } else {
            vm.prefs.edit()
                .remove("last_household_id")
                .remove("cached_active_uid")
                .apply()
        }

        vm.roomClearJob = vm.viewModelScope.launch(Dispatchers.IO + vm.coroutineExceptionHandler) {
            try {
                vm.repository.clearAllData()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.w(tag, "resetStateForUserSwitch room clear warning: ${t.message}")
            }
        }
    }

    fun seedInitialHouseholdPlaceholderOnAuth(user: FirebaseUser) {
        if (vm._currentHousehold.value != null) return
        val hid = vm.getOrEnsureHouseholdId(user)
        if (hid.isBlank()) return
        val placeholder = Household(
            id = hid,
            name = vm.prefs.getString("household_name_$hid", null)?.takeIf { it.isNotBlank() }
                ?: com.example.util.Localization.myExodologio(com.example.util.Localization.Language.fromCode(vm._appLanguage.value)),
            ownerId = user.uid,
            memberUids = listOf(user.uid),
            memberRoles = mapOf(user.uid to HouseholdRole.CREATOR.key)
        )
        vm._currentHousehold.value = placeholder
        if (vm.prefs.getString("last_household_id", "").isNullOrBlank()) {
            vm.prefs.edit().putString("last_household_id", hid).apply()
        }
        if (vm._userHouseholds.value.none { it.id == hid }) {
            vm._userHouseholds.value = listOf(placeholder)
        }
    }

    fun scheduleLoadUserProfileAndHousehold(user: FirebaseUser, force: Boolean = false) {
        if (!force && vm.loadProfileJob?.isActive == true && vm.activeLoadProfileUid == user.uid) {
            return
        }
        vm.loadProfileJob?.cancel()
        vm.activeLoadProfileUid = user.uid
        vm.loadProfileJob = vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try {
                loadUserProfileAndHousehold(user)
            } finally {
                if (vm.activeLoadProfileUid == user.uid) {
                    vm.activeLoadProfileUid = null
                }
            }
        }
    }

    suspend fun loadUserProfileAndHousehold(user: FirebaseUser) {
        val verified = vm.isUserVerified(user)
        vm._isUserVerifiedState.value = verified
        if (!verified) {
            vm.cancelAllSyncJobs()
            return
        }

        vm.trackSyncStart()
        vm._isSyncing.value = true
        try {
            try {
                vm.roomClearJob?.join()
            } catch (_: Throwable) {}

            val deletedHids = vm.prefs.getStringSet("deleted_households_${user.uid}", emptySet()).orEmpty() +
                vm.prefs.getStringSet("deleted_households_global", emptySet()).orEmpty()

            val provider = when {
                user.providerData.any { it.providerId == "google.com" } -> "google"
                user.providerData.any { it.providerId == "facebook.com" } -> "facebook"
                else -> "password"
            }
            val displayName = user.displayName.orEmpty().trim()
            val parts = displayName.split(" ").filter { it.isNotBlank() }
            val currentLang = vm._appLanguage.value.ifBlank { "EN" }
            val currentLangEnum = com.example.util.Localization.Language.fromCode(currentLang)
            val profile = vm.firestoreSyncRepo.getOrCreateUserProfile(
                uid = user.uid,
                email = user.email.orEmpty(),
                displayName = displayName.ifBlank { user.email?.substringBefore("@") ?: com.example.util.Localization.tr(currentLangEnum, "User", "Χρήστης") },
                firstName = parts.firstOrNull().orEmpty(),
                lastName = if (parts.size > 1) parts.drop(1).joinToString(" ") else "",
                authProvider = provider,
                excludedHouseholdIds = deletedHids,
                appLanguage = currentLang
            )
            val remoteAccepted = profile.acceptedLegalVersion == com.example.ui.components.LegalConstants.CURRENT_LEGAL_VERSION
            val localAccepted = vm.prefs.getString("legal_accepted_version_${user.uid}", "") ==
                com.example.ui.components.LegalConstants.CURRENT_LEGAL_VERSION
            val effectiveProfile = when {
                remoteAccepted -> {
                    val acceptedAt = if (profile.acceptedLegalAt > 0L) profile.acceptedLegalAt else System.currentTimeMillis()
                    vm.prefs.edit()
                        .putString("legal_accepted_version_${user.uid}", profile.acceptedLegalVersion)
                        .putLong("legal_accepted_at_${user.uid}", acceptedAt)
                        .apply()
                    vm._hasAcceptedCurrentLegal.value = true
                    vm._isLegalStatusResolved.value = true
                    profile
                }
                localAccepted -> {
                    val localAt = vm.prefs.getLong("legal_accepted_at_${user.uid}", System.currentTimeMillis())
                        .takeIf { it > 0L } ?: System.currentTimeMillis()
                    try {
                        vm.firestoreSyncRepo.recordLegalAcceptance(
                            user.uid,
                            com.example.ui.components.LegalConstants.CURRENT_LEGAL_VERSION,
                            localAt
                        )
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Log.w(tag, "Backfill legal acceptance warning: ${e.message}")
                    }
                    vm._hasAcceptedCurrentLegal.value = true
                    vm._isLegalStatusResolved.value = true
                    profile.copy(
                        acceptedLegalVersion = com.example.ui.components.LegalConstants.CURRENT_LEGAL_VERSION,
                        acceptedLegalAt = localAt
                    )
                }
                else -> {
                    vm._hasAcceptedCurrentLegal.value = false
                    vm._isLegalStatusResolved.value = true
                    profile
                }
            }
            vm._userProfile.value = effectiveProfile
            persistProfileToPrefs(effectiveProfile)
            vm.recomputeProEntitlement()

            if (effectiveProfile.appLanguage.isNotBlank()) {
                vm._appLanguage.value = effectiveProfile.appLanguage
                vm.prefs.edit().putString("app_language", effectiveProfile.appLanguage).apply()
                try {
                    vm.auth.setLanguageCode(com.example.util.Localization.Language.fromCode(effectiveProfile.appLanguage).firebaseCode)
                } catch (_: Throwable) {}
            }
            if (effectiveProfile.themeMode.isNotBlank()) {
                vm._themeMode.value = effectiveProfile.themeMode
            }

            val discoveredHouseholds = vm.firestoreSyncRepo.getAllUserHouseholds(
                uid = user.uid,
                email = profile.email.ifBlank { user.email.orEmpty() },
                excludedHouseholdIds = deletedHids
            ).filter { !deletedHids.contains(it.id) }.toMutableList()

            val activeHousehold = when {
                discoveredHouseholds.isEmpty() -> {
                    val targetHid = profile.householdId.takeIf {
                        StartupSafety.isHouseholdAllowedForUser(it, false, emptySet(), profile.householdIds.toSet())
                    } ?: vm.getOrEnsureHouseholdId(user)
                    val defaultHouseholdName = com.example.util.Localization.myExodologio(
                        com.example.util.Localization.Language.fromCode(vm._appLanguage.value)
                    )
                    val ensured = vm.firestoreSyncRepo.ensureHouseholdExists(
                        householdId = targetHid,
                        ownerUid = user.uid,
                        householdName = defaultHouseholdName
                    )
                    discoveredHouseholds.add(ensured)
                    ensured
                }
                else -> {
                    val savedLast = vm.prefs.getString("last_household_id", "").orEmpty()
                    discoveredHouseholds.find { it.id == savedLast }
                        ?: discoveredHouseholds.find { it.id == profile.householdId }
                        ?: discoveredHouseholds.first()
                }
            }

            val finalHouseholds = discoveredHouseholds.distinctBy { it.id }
            vm._userHouseholds.value = finalHouseholds
            vm._currentHousehold.value = activeHousehold
            vm.syncBankCatcherProEntitlement()
            if (!vm.prefs.getBoolean("onboarding_shown_${user.uid}", false)) {
                vm._showWelcomeCarousel.value = true
            }
            vm.prefs.edit()
                .putString("last_household_id", activeHousehold.id)
                .putString("user_household_${user.uid}", activeHousehold.id)
                .apply()

            vm.loadDeletedItems(activeHousehold.id)
            observeUserData(user.uid)
            observeHouseholdData(activeHousehold.id)

            val householdIdsForProfile = finalHouseholds.map { it.id }.distinct()
            if (householdIdsForProfile.isNotEmpty() && householdIdsForProfile != profile.householdIds) {
                try {
                    vm.firestoreSyncRepo.updateUserProfileHouseholdIds(user.uid, householdIdsForProfile)
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(tag, "loadUserProfileAndHousehold error: ${e.message}", e)
        } finally {
            vm._isLegalStatusResolved.value = true
            vm._isSyncing.value = false
            vm._isInitialExpensesLoading.value = false
            vm.trackSyncEnd()
        }
    }

    fun observeUserData(userId: String) {
        if (userId.isBlank()) return
        vm.userProfileSyncJob?.cancel()
        vm.userHouseholdsSyncJob?.cancel()
        vm.userExpensesSyncJob?.cancel()
        vm.userIncomesSyncJob?.cancel()
        vm.userSavingsGoalsSyncJob?.cancel()
        vm.userRecurringSyncJob?.cancel()

        vm.userProfileSyncJob = vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.firestoreSyncRepo.observeUserProfile(userId)
                .catch { t -> Log.w(tag, "observeUserProfile error: ${t.message}") }
                .collect { remoteProfile ->
                    if (remoteProfile != null) {
                        val currentVersion = com.example.ui.components.LegalConstants.CURRENT_LEGAL_VERSION
                        val resolvedProfile = if (remoteProfile.acceptedLegalVersion == currentVersion) {
                            vm.prefs.edit()
                                .putString("legal_accepted_version_$userId", currentVersion)
                                .putLong("legal_accepted_at_$userId", remoteProfile.acceptedLegalAt)
                                .apply()
                            vm._hasAcceptedCurrentLegal.value = true
                            vm._isLegalStatusResolved.value = true
                            remoteProfile
                        } else if (vm.prefs.getString("legal_accepted_version_$userId", "") == currentVersion) {
                            remoteProfile.copy(
                                acceptedLegalVersion = currentVersion,
                                acceptedLegalAt = vm.prefs.getLong("legal_accepted_at_$userId", 0L)
                            )
                        } else {
                            remoteProfile
                        }
                        vm._userProfile.value = resolvedProfile
                        persistProfileToPrefs(resolvedProfile)
                        vm.recomputeProEntitlement()
                    }
                }
        }

        val email = vm._currentUser.value?.email ?: vm._userProfile.value?.email.orEmpty()
        vm.userHouseholdsSyncJob = vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.firestoreSyncRepo.observeUserHouseholds(userId, email)
                .catch { t -> Log.w(tag, "observeUserHouseholds error: ${t.message}") }
                .collect { list ->
                    val deletedHids = vm.prefs.getStringSet("deleted_households_$userId", emptySet()).orEmpty() +
                        vm.prefs.getStringSet("deleted_households_global", emptySet()).orEmpty()
                    val filtered = list.filter { !deletedHids.contains(it.id) }.distinctBy { it.id }
                    if (filtered.isNotEmpty()) {
                        vm._userHouseholds.value = filtered
                        val curId = vm._currentHousehold.value?.id
                        val updatedCur = if (!curId.isNullOrBlank()) {
                            filtered.find { it.id == curId } ?: filtered.first()
                        } else {
                            filtered.first()
                        }
                        vm._currentHousehold.value = updatedCur
                        vm.syncBankCatcherProEntitlement()
                        vm.prefs.edit().putString("last_household_id", updatedCur.id).apply()
                        if (curId != updatedCur.id) {
                            vm.loadDeletedItems(updatedCur.id)
                            observeHouseholdData(updatedCur.id)
                        }
                    }
                }
        }

        // Only attach personal fallback listeners when the user has no active household yet;
        // once observeHouseholdData(hid) starts, these 4 redundant listeners are cancelled to save Firestore reads.
        if (vm._currentHousehold.value?.id.isNullOrBlank()) {
            vm.userExpensesSyncJob = vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
                vm.firestoreSyncRepo.observeUserExpenses(userId)
                    .catch { t -> Log.w(tag, "observeUserExpenses error: ${t.message}") }
                    .collect { list ->
                        vm.lastUserExpenses = list
                        if (vm._currentHousehold.value?.id.isNullOrBlank()) {
                            applyResolvedExpenses()
                        }
                    }
            }

            vm.userIncomesSyncJob = vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
                vm.firestoreSyncRepo.observeUserIncomes(userId)
                    .catch { t -> Log.w(tag, "observeUserIncomes error: ${t.message}") }
                    .collect { list ->
                        vm.lastUserIncomes = list
                        if (vm._currentHousehold.value?.id.isNullOrBlank()) {
                            applyResolvedIncomes()
                        }
                    }
            }

            vm.userSavingsGoalsSyncJob = vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
                vm.firestoreSyncRepo.observeUserSavingsGoals(userId)
                    .catch { t -> Log.w(tag, "observeUserSavingsGoals error: ${t.message}") }
                    .collect { list ->
                        vm.lastUserSavingsGoals = list
                        if (vm._currentHousehold.value?.id.isNullOrBlank() && vm._userHouseholds.value.isEmpty()) {
                            applyResolvedSavingsGoals(expectedHouseholdId = "")
                        }
                    }
            }

            vm.userRecurringSyncJob = vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
                vm.firestoreSyncRepo.observeUserRecurringExpenses(userId)
                    .catch { t -> Log.w(tag, "observeUserRecurringExpenses error: ${t.message}") }
                    .collect { list ->
                        if (vm._currentHousehold.value?.id.isNullOrBlank() && list.isNotEmpty()) {
                            val mapped = list.map { rr ->
                                RecurringExpense(
                                    id = RecurringExpenseAutoProcessor.resolveCloudEntityId(rr.id),
                                    title = rr.title,
                                    amount = rr.amount,
                                    categoryId = rr.categoryId,
                                    categoryName = rr.categoryName,
                                    frequency = rr.frequency,
                                    dayOfMonth = rr.dayOfMonth,
                                    reminderHour = rr.reminderHour,
                                    reminderMinute = rr.reminderMinute,
                                    splitType = rr.splitType,
                                    memberIdsCsv = rr.memberIdsCsv,
                                    memberNamesDisplay = rr.memberNamesDisplay,
                                    note = rr.note,
                                    isActive = rr.isActive,
                                    lastGeneratedMonth = rr.lastGeneratedMonth,
                                    payerMemberId = rr.payerMemberId,
                                    payerMemberName = rr.payerMemberName,
                                    payerMemberIdsCsv = rr.payerMemberIdsCsv,
                                    payerAmountsCsv = rr.payerAmountsCsv,
                                    anchorMonth = rr.anchorMonth
                                )
                            }
                            vm.repository.replaceAllRecurringExpenses(mapped)
                        }
                    }
            }
        }
    }

    fun observeHouseholdData(householdId: String) {
        if (householdId.isBlank()) return
        // Once an active household is observed, close redundant personal-mirror listeners
        vm.userExpensesSyncJob?.cancel()
        vm.userExpensesSyncJob = null
        vm.userIncomesSyncJob?.cancel()
        vm.userIncomesSyncJob = null
        vm.userSavingsGoalsSyncJob?.cancel()
        vm.userSavingsGoalsSyncJob = null
        vm.userRecurringSyncJob?.cancel()
        vm.userRecurringSyncJob = null

        if (vm.lastObservedHouseholdId == householdId &&
            vm.expensesSyncJob?.isActive == true &&
            vm.membersSyncJob?.isActive == true &&
            vm.savingsGoalsSyncJob?.isActive == true &&
            vm.activitySyncJob?.isActive == true
        ) {
            return
        }
        val householdChanged = !vm.lastObservedHouseholdId.isNullOrBlank() && vm.lastObservedHouseholdId != householdId
        vm.lastObservedHouseholdId = householdId
        if (householdChanged) {
            vm.lastHouseholdSavingsGoals = emptyList()
            vm.savingsGoalCloudIdByLocalId.clear()
            vm.pendingLocalSavingsGoals.clear()
            vm.pendingDeletedSavingsGoals.clear()
            vm.inFlightCreateGoalKeys.clear()
            vm._allSavingsGoals.value = emptyList()
        }

        vm.householdJob?.cancel()
        vm.expensesSyncJob?.cancel()
        vm.incomesSyncJob?.cancel()
        vm.membersSyncJob?.cancel()
        vm.categoriesSyncJob?.cancel()
        vm.savingsGoalsSyncJob?.cancel()
        vm.recurringSyncJob?.cancel()
        vm.activitySyncJob?.cancel()

        vm.householdJob = vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.firestoreSyncRepo.observeHousehold(householdId)
                .catch { t -> Log.w(tag, "observeHousehold error: ${t.message}") }
                .collect { h ->
                    if (h != null) {
                        vm._currentHousehold.value = h
                        vm.syncBankCatcherProEntitlement()
                        vm.prefs.edit().putString("last_household_id", h.id).apply()
                        vm._userHouseholds.value = vm._userHouseholds.value.map { if (it.id == h.id) h else it }
                            .ifEmpty { listOf(h) }
                    }
                }
        }

        vm.expensesSyncJob = vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.firestoreSyncRepo.observeExpenses(householdId)
                .catch { t -> Log.w(tag, "observeExpenses error: ${t.message}") }
                .collect { expenses ->
                    if (vm._currentHousehold.value?.id != householdId) return@collect
                    vm.lastHouseholdExpenses = expenses
                    applyResolvedExpenses()
                }
        }

        vm.incomesSyncJob = vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.firestoreSyncRepo.observeIncomes(householdId)
                .catch { t -> Log.w(tag, "observeIncomes error: ${t.message}") }
                .collect { incomes ->
                    if (vm._currentHousehold.value?.id != householdId) return@collect
                    vm.lastHouseholdIncomes = incomes
                    applyResolvedIncomes()
                }
        }

        vm.membersSyncJob = vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.firestoreSyncRepo.observeMembers(householdId)
                .catch { t -> Log.w(tag, "observeMembers error: ${t.message}") }
                .collect { members ->
                    if (vm._currentHousehold.value?.id != householdId) return@collect
                    vm.lastHouseholdMembers = members
                    vm.syncAndMergeMembers(members, explicitHouseholdId = householdId)
                }
        }

        vm.categoriesSyncJob = vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.firestoreSyncRepo.observeCategories(householdId)
                .catch { t -> Log.w(tag, "observeCategories error: ${t.message}") }
                .collect { cats ->
                    if (vm._currentHousehold.value?.id != householdId) return@collect
                    vm.lastHouseholdCategories = cats
                    vm.syncAndMergeCategories(cats, explicitHouseholdId = householdId)
                }
        }

        vm.savingsGoalsSyncJob = vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.firestoreSyncRepo.observeSavingsGoals(householdId)
                .catch { t -> Log.w(tag, "observeSavingsGoals error: ${t.message}") }
                .collect { goals ->
                    if (vm._currentHousehold.value?.id != householdId) return@collect
                    vm.lastHouseholdSavingsGoals = goals
                    applyResolvedSavingsGoals(expectedHouseholdId = householdId)
                }
        }

        vm.recurringSyncJob = vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.firestoreSyncRepo.observeRecurringExpenses(householdId)
                .catch { t -> Log.w(tag, "observeRecurringExpenses error: ${t.message}") }
                .collect { list ->
                    if (vm._currentHousehold.value?.id != householdId) return@collect
                    val mapped = list.map { rr ->
                        RecurringExpense(
                            id = RecurringExpenseAutoProcessor.resolveCloudEntityId(rr.id),
                            title = rr.title,
                            amount = rr.amount,
                            categoryId = rr.categoryId,
                            categoryName = rr.categoryName,
                            frequency = rr.frequency,
                            dayOfMonth = rr.dayOfMonth,
                            reminderHour = rr.reminderHour,
                            reminderMinute = rr.reminderMinute,
                            splitType = rr.splitType,
                            memberIdsCsv = rr.memberIdsCsv,
                            memberNamesDisplay = rr.memberNamesDisplay,
                            note = rr.note,
                            isActive = rr.isActive,
                            lastGeneratedMonth = rr.lastGeneratedMonth,
                            payerMemberId = rr.payerMemberId,
                            payerMemberName = rr.payerMemberName,
                            payerMemberIdsCsv = rr.payerMemberIdsCsv,
                            payerAmountsCsv = rr.payerAmountsCsv,
                            anchorMonth = rr.anchorMonth
                        )
                    }
                    vm.repository.replaceAllRecurringExpenses(mapped)
                    if (vm.hasOwnerPermissions()) {
                        mapped.filter { it.isActive }.forEach { rec ->
                            BillReminderManager.scheduleReminder(vm.getApplication(), rec)
                        }
                    }
                }
        }

        // 3.1: Real-time shared household activity feed + remote local notifications
        vm.activitySyncJob = vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            var initialBatchLoaded = false
            val notifiedIds = HashSet<String>()
            vm.firestoreSyncRepo.observeHouseholdActivity(householdId, 50L)
                .catch { t -> Log.w(tag, "observeHouseholdActivity error: ${t.message}") }
                .collect { activities ->
                    vm._householdActivities.value = activities
                    if (!initialBatchLoaded) {
                        activities.forEach { if (it.id.isNotBlank()) notifiedIds.add(it.id) }
                        initialBatchLoaded = true
                        return@collect
                    }

                    val currentUid = vm._currentUser.value?.uid ?: vm.auth.currentUser?.uid.orEmpty()
                    val household = vm._currentHousehold.value
                    val isSharedHousehold = household != null && (
                        household.memberUids.size > 1 ||
                            (!household.ownerId.isNullOrBlank() && household.ownerId != currentUid)
                        )
                    if (!isSharedHousehold || currentUid.isBlank()) {
                        activities.forEach { if (it.id.isNotBlank()) notifiedIds.add(it.id) }
                        return@collect
                    }

                    val newRemote = activities.filter { act ->
                        act.id.isNotBlank() &&
                            !notifiedIds.contains(act.id) &&
                            act.actorUid.isNotBlank() &&
                            act.actorUid != currentUid
                    }
                    activities.forEach { if (it.id.isNotBlank()) notifiedIds.add(it.id) }

                    val notifLang = com.example.util.Localization.Language.fromCode(vm._appLanguage.value)
                    val householdName = household?.name ?: "Finsay"
                    newRemote.take(2).forEachIndexed { idx, act ->
                        val summaryText = if (notifLang == com.example.util.Localization.Language.GREEK) {
                            act.summaryEl.ifBlank { act.summaryEn }
                        } else {
                            act.summaryEn.ifBlank { act.summaryEl }
                        }
                        HouseholdActivityNotificationManager.notifyRemoteActivity(
                            context = vm.getApplication(),
                            householdName = householdName,
                            actorName = act.actorName,
                            summaryText = summaryText,
                            notificationId = act.id.hashCode() + idx,
                            isGreek = notifLang == com.example.util.Localization.Language.GREEK,
                            lang = notifLang
                        )
                    }
                }
        }
    }

    private suspend fun applyResolvedExpenses() {
        val activeHid = vm._currentHousehold.value?.id.orEmpty()
        val authoritativeRemote = StartupSafety.resolveAuthoritativeExpenses(
            activeHouseholdId = activeHid,
            householdExpenses = vm.lastHouseholdExpenses,
            userExpenses = vm.lastUserExpenses,
            totalHouseholdCount = vm._userHouseholds.value.size
        )

        val nowMs = System.currentTimeMillis()
        val confirmedIds = authoritativeRemote.map { it.id }.filter { it.isNotBlank() }.toSet()
        val pendingIds = StartupSafety.pruneExpiredPendingIds(vm.pendingLocalExpenseIds, confirmedIds, nowMs)

        val remoteEntities = authoritativeRemote.map { fe -> firebaseExpenseToEntity(fe) }
        val localPendingExtras = if (pendingIds.isNotEmpty()) {
            vm._allExpenses.value.filter { it.cloudId.isNotBlank() && pendingIds.contains(it.cloudId) && !confirmedIds.contains(it.cloudId) }
        } else {
            emptyList()
        }

        val combined = StartupSafety.deduplicateExpenseIds(
            (remoteEntities + localPendingExtras).distinctBy {
                it.cloudId.ifBlank { "${it.id}_${it.title}_${it.timestamp}_${it.amount}" }
            }
        ).sortedWith(compareByDescending<Expense> { it.timestamp }.thenByDescending { it.id })

        vm._allExpenses.value = combined
        vm._isInitialExpensesLoading.value = false
        try {
            vm.repository.replaceAllExpenses(combined)
        } catch (t: Throwable) {
            Log.w(tag, "replaceAllExpenses warning: ${t.message}")
        }
    }

    private suspend fun applyResolvedIncomes() {
        val activeHid = vm._currentHousehold.value?.id.orEmpty()
        val authoritativeRemote = StartupSafety.resolveAuthoritativeIncomes(
            activeHouseholdId = activeHid,
            householdIncomes = vm.lastHouseholdIncomes,
            userIncomes = vm.lastUserIncomes,
            totalHouseholdCount = vm._userHouseholds.value.size
        )

        val nowMs = System.currentTimeMillis()
        val confirmedIds = authoritativeRemote.map { it.id }.filter { it.isNotBlank() }.toSet()
        val pendingIds = StartupSafety.pruneExpiredPendingIds(vm.pendingLocalIncomeIds, confirmedIds, nowMs)

        val usedIds = HashSet<Long>(authoritativeRemote.size * 2)
        var nextId = 1L
        fun allocId(preferred: Long): Long {
            if (preferred > 0L && usedIds.add(preferred)) return preferred
            while (usedIds.contains(nextId)) nextId++
            usedIds.add(nextId)
            return nextId
        }

        val remoteEntities = authoritativeRemote.map { fi ->
            val numericPreferred = fi.id.removePrefix("inc_").toLongOrNull() ?: StartupSafety.safePositiveIdHash(fi.id)
            Income(
                id = allocId(numericPreferred),
                cloudId = fi.id,
                title = fi.title,
                amount = fi.amount,
                memberId = fi.memberId,
                memberName = fi.memberName,
                category = fi.category,
                timestamp = fi.timestamp,
                note = fi.note
            )
        }

        val localPendingExtras = if (pendingIds.isNotEmpty()) {
            vm._allIncomes.value.filter { it.cloudId.isNotBlank() && pendingIds.contains(it.cloudId) && !confirmedIds.contains(it.cloudId) }
        } else {
            emptyList()
        }

        val combined = (remoteEntities + localPendingExtras)
            .distinctBy { it.cloudId.ifBlank { "${it.id}_${it.title}_${it.timestamp}_${it.amount}" } }
            .sortedWith(compareByDescending<Income> { it.timestamp }.thenByDescending { it.id })

        vm._allIncomes.value = combined
        try {
            vm.repository.replaceAllIncomes(combined)
        } catch (t: Throwable) {
            Log.w(tag, "replaceAllIncomes warning: ${t.message}")
        }
    }

    private suspend fun applyResolvedSavingsGoals(expectedHouseholdId: String? = null) {
        val activeHid = vm._currentHousehold.value?.id.orEmpty()
        if (expectedHouseholdId != null && activeHid != expectedHouseholdId) return
        val resolved = StartupSafety.resolveAuthoritativeSavingsGoals(
            activeHouseholdId = activeHid,
            householdGoals = vm.lastHouseholdSavingsGoals,
            userGoals = vm.lastUserSavingsGoals,
            pendingLocalMutations = vm.pendingLocalSavingsGoals,
            pendingDeletedGoals = vm.pendingDeletedSavingsGoals,
            nowMs = System.currentTimeMillis()
        )
        if (vm._currentHousehold.value?.id.orEmpty() != activeHid) return
        vm.savingsGoalCloudIdByLocalId.clear()
        vm.savingsGoalCloudIdByLocalId.putAll(resolved.cloudIdByLocalId)
        vm._allSavingsGoals.value = resolved.goals
        try {
            vm.repository.replaceAllSavingsGoals(resolved.goals)
        } catch (t: Throwable) {
            Log.w(tag, "replaceAllSavingsGoals warning: ${t.message}")
        }
    }

    private fun firebaseExpenseToEntity(fe: FirebaseExpense): Expense {
        val numericId = fe.id.removePrefix("exp_").toLongOrNull() ?: StartupSafety.safePositiveIdHash(fe.id)
        return Expense(
            id = numericId,
            cloudId = fe.id,
            title = fe.title,
            amount = fe.amount,
            categoryId = fe.categoryId,
            categoryName = fe.categoryName,
            timestamp = fe.timestamp,
            splitType = fe.splitType,
            memberIdsCsv = fe.memberIdsCsv,
            memberNamesDisplay = fe.memberNamesDisplay,
            splitAmountPerPerson = 0.0,
            originalVoicePrompt = "",
            note = fe.note,
            payerMemberId = fe.payerMemberId,
            payerMemberName = fe.payerMemberName,
            payerMemberIdsCsv = fe.payerMemberIdsCsv,
            payerAmountsCsv = fe.payerAmountsCsv
        )
    }

    @Volatile
    private var lastResumeHouseholdRefreshMs: Long = 0L

    fun onAppResumed() {
        val nowMs = System.currentTimeMillis()
        vm._reminderClockTickMs.value = nowMs
        val user = vm.auth.currentUser ?: vm._currentUser.value ?: return
        if (!vm.isUserVerified(user)) return
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try {
                vm.firestoreSyncRepo.enableNetwork()
                val shouldRefreshHouseholds = vm._userHouseholds.value.isEmpty() ||
                    (nowMs - lastResumeHouseholdRefreshMs) >= 300_000L
                if (shouldRefreshHouseholds) {
                    lastResumeHouseholdRefreshMs = nowMs
                    vm.refreshUserHouseholds()
                }
                val hid = vm._currentHousehold.value?.id ?: vm.getOrEnsureHouseholdId(user)
                if (hid.isNotBlank()) {
                    observeHouseholdData(hid)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(tag, "onAppResumed warning: ${e.message}")
            }
        }
    }

    fun refreshCloudSync() {
        vm._reminderClockTickMs.value = System.currentTimeMillis()
        val user = vm.auth.currentUser ?: vm._currentUser.value ?: return
        if (!vm.isUserVerified(user)) return
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            vm._isSyncing.value = true
            try {
                vm.firestoreSyncRepo.enableNetwork()
                vm.refreshUserHouseholds()
                val hid = vm._currentHousehold.value?.id ?: vm.getOrEnsureHouseholdId(user)
                if (hid.isNotBlank()) {
                    observeHouseholdData(hid)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(tag, "refreshCloudSync warning: ${e.message}")
            } finally {
                vm._isSyncing.value = false
                vm.trackSyncEnd()
            }
        }
    }

    fun logHouseholdActivity(
        actionType: String,
        targetType: String,
        summaryEl: String,
        summaryEn: String,
        amount: Double = 0.0,
        householdIdOverride: String? = null
    ) {
        val user = vm._currentUser.value ?: vm.auth.currentUser ?: return
        val uid = user.uid
        val hid = householdIdOverride?.takeIf { it.isNotBlank() } ?: vm._currentHousehold.value?.id.orEmpty()
        if (uid.isBlank() || hid.isBlank() || actionType.isBlank() || summaryEl.isBlank()) return

        val profile = vm._userProfile.value
        val actorName = profile?.displayName?.ifBlank { null }
            ?: user.displayName?.ifBlank { null }
            ?: user.email?.substringBefore("@")
            ?: "Μέλος"

        val activity = FirebaseHouseholdActivity(
            id = UUID.randomUUID().toString(),
            householdId = hid,
            actorUid = uid,
            actorName = actorName,
            actionType = actionType,
            targetType = targetType,
            summaryEl = summaryEl,
            summaryEn = summaryEn.ifBlank { summaryEl },
            amount = amount,
            createdAt = System.currentTimeMillis()
        )

        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try {
                vm.firestoreSyncRepo.logHouseholdActivity(hid, activity)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(tag, "logHouseholdActivity warning: ${e.message}")
            }
        }
    }

    private fun persistProfileToPrefs(profile: UserProfile) {
        try {
            val cleanIds = profile.householdIds.map { it.trim() }.filter { it.isNotBlank() }.toSet()
            vm.prefs.edit()
                .putString("cached_user_uid", profile.uid)
                .putString("cached_user_email", profile.email)
                .putString("cached_user_display_name", profile.displayName)
                .putString("cached_user_first_name", profile.firstName)
                .putString("cached_user_last_name", profile.lastName)
                .putString("cached_user_birth_date", profile.birthDate)
                .putString("cached_user_phone", profile.phoneNumber)
                .putString("cached_user_occupation", profile.occupation)
                .putString("cached_user_city", profile.city)
                .putString("cached_user_photo_url", profile.photoUrl)
                .putBoolean("cached_user_is_premium", profile.isPremium)
                .putString("cached_user_household_id", profile.householdId)
                .putStringSet("cached_user_household_ids", cleanIds)
                .putString("cached_user_app_language", profile.appLanguage)
                .putString("cached_user_theme_mode", profile.themeMode)
                .putBoolean("cached_user_reminder_enabled", profile.reminderEnabled)
                .putInt("cached_user_reminder_hour", profile.reminderHour)
                .putInt("cached_user_reminder_minute", profile.reminderMinute)
                .putBoolean("cached_user_notification_consent_completed", profile.notificationConsentCompleted)
                .putString("cached_user_legal_version", profile.acceptedLegalVersion)
                .putLong("cached_user_legal_at", profile.acceptedLegalAt)
                .apply()
        } catch (_: Throwable) {}
    }
}
