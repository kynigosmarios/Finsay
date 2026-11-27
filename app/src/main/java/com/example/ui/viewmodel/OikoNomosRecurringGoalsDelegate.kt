package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.RecurringExpense
import com.example.data.entity.SavingsGoal
import com.example.data.entity.resolveMembersFromDisplayLabel
import com.example.data.entity.toFirebaseExpense
import com.example.data.model.FirebaseCategory
import com.example.data.model.FirebaseRecurringExpense
import com.example.data.model.FirebaseSavingsGoal
import com.example.reminder.BillReminderManager
import com.example.util.DefaultPayerHelper
import com.example.util.GreekVoiceParser
import com.example.util.Localization
import com.example.util.RecurringExpenseAutoProcessor
import com.example.util.RecurringScheduleHelper
import com.example.util.SavingsGoalExpenseLinkHelper
import com.example.util.StartupSafety
import com.google.firebase.auth.FirebaseUser
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal class OikoNomosRecurringGoalsDelegate(
    private val vm: OikoNomosViewModel
) {
    private fun denyRegistarWrite(actionEl: String, actionEn: String): Boolean {
        if (vm.hasOwnerPermissions()) return false
        vm.showSnackbar(Localization.tr(vm._appLanguage.value, actionEn, actionEl))
        return true
    }

    // Savings Goals (4.2 RBAC protected for CREATOR & EDITOR)
    fun openSavingsGoalDialog(goal: SavingsGoal? = null) {
        if (denyRegistarWrite(
                "Μόνο ο Δημιουργός ή Συντάκτης μπορεί να διαχειριστεί στόχους",
                "Only Creators or Editors can manage savings goals"
            )
        ) return
        if (goal == null && !vm.isCurrentUserPro() && vm.allSavingsGoals.value.size >= 1) {
            vm.showPremiumPaywall("savings_goal_limit")
            vm.showSnackbar(
                Localization.tr(
                    vm._appLanguage.value,
                    "Free edition supports up to 1 Savings Goal",
                    "Η δωρεάν έκδοση υποστηρίζει έως 1 Στόχο Αποταμίευσης"
                )
            )
            return
        }
        vm._editingSavingsGoal.value = goal
        vm._showSavingsGoalDialog.value = true
    }

    fun saveSavingsGoal(
        id: Long,
        title: String,
        targetAmount: Double,
        currentAmount: Double,
        deadline: String,
        note: String,
        recordAsExpense: Boolean = false
    ) {
        if (denyRegistarWrite(
                "Μόνο ο Δημιουργός ή Συντάκτης μπορεί να αποθηκεύσει στόχους",
                "Only Creators or Editors can save savings goals"
            )
        ) {
            vm.dismissSavingsGoalDialog()
            return
        }
        if (id == 0L && !vm.isCurrentUserPro() && vm._allSavingsGoals.value.size >= 1) {
            vm.dismissSavingsGoalDialog()
            vm.showPremiumPaywall("savings_goal_limit")
            vm.showSnackbar(
                Localization.tr(
                    vm._appLanguage.value,
                    "Free edition supports up to 1 Savings Goal",
                    "Η δωρεάν έκδοση υποστηρίζει έως 1 Στόχο Αποταμίευσης"
                )
            )
            return
        }

        val cleanTitle = title.trim()
        val user = vm._currentUser.value
        val activeHouseholdId = vm._currentHousehold.value?.id?.takeIf { it.isNotBlank() }
            ?: vm.getOrEnsureHouseholdId(user)
        val nowMs = System.currentTimeMillis()

        // Single-creation guard: drop duplicate submissions within 3 seconds for new goals
        if (id == 0L) {
            val expiredKeys = vm.inFlightCreateGoalKeys.entries
                .filter { nowMs - it.value > 3_000L }
                .map { it.key }
            expiredKeys.forEach { vm.inFlightCreateGoalKeys.remove(it) }

            val createKey = "$activeHouseholdId|${cleanTitle.lowercase()}|$targetAmount|$currentAmount|${deadline.trim().lowercase()}"
            val prevTs = vm.inFlightCreateGoalKeys.putIfAbsent(createKey, nowMs)
            if (prevTs != null && nowMs - prevTs <= 3_000L) {
                vm.dismissSavingsGoalDialog()
                return
            }
            if (prevTs != null) {
                vm.inFlightCreateGoalKeys[createKey] = nowMs
            }
        }

        vm.dismissSavingsGoalDialog()

        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                val existingGoal = if (id > 0L) vm._allSavingsGoals.value.find { it.id == id } else null
                val cloudId = if (id > 0L) {
                    vm.savingsGoalCloudIdByLocalId[id] ?: id.toString()
                } else {
                    "goal_${UUID.randomUUID()}"
                }

                val usedIds = vm._allSavingsGoals.value.map { it.id }.toSet()
                val targetLocalId = if (id > 0L) {
                    id
                } else {
                    var candidate = StartupSafety.safePositiveIdHash(cloudId)
                    while (usedIds.contains(candidate)) {
                        candidate++
                    }
                    candidate
                }

                val goal = SavingsGoal(
                    id = targetLocalId,
                    title = cleanTitle,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    deadlineDate = deadline.trim(),
                    iconKey = existingGoal?.iconKey ?: "savings",
                    colorHex = existingGoal?.colorHex ?: 4278556265L,
                    note = note.trim()
                )

                vm.savingsGoalCloudIdByLocalId[targetLocalId] = cloudId
                vm.pendingDeletedSavingsGoals.remove(cloudId)
                vm.pendingDeletedSavingsGoals.remove(targetLocalId.toString())
                vm.pendingLocalSavingsGoals[cloudId] = StartupSafety.PendingSavingsGoalMutation(
                    cloudId = cloudId,
                    householdId = activeHouseholdId,
                    localEntity = goal,
                    updatedAtMs = nowMs
                )

                // Immediately update in-memory UI state to prevent any flicker
                val updatedList = (vm._allSavingsGoals.value.filterNot { it.id == targetLocalId } + goal)
                    .sortedWith(compareBy<SavingsGoal> { it.title.trim().lowercase() }.thenBy { it.id })
                vm._allSavingsGoals.value = updatedList

                val previousAmount = existingGoal?.currentAmount ?: 0.0
                val positiveDelta = currentAmount - previousAmount

                if (id == 0L) {
                    vm.repository.insertSavingsGoal(goal)
                } else {
                    vm.repository.updateSavingsGoal(goal)
                }

                val fbGoal = FirebaseSavingsGoal(
                    id = cloudId,
                    householdId = activeHouseholdId,
                    userId = user?.uid ?: "",
                    title = cleanTitle,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    deadlineDate = deadline.trim(),
                    iconKey = goal.iconKey,
                    colorHex = goal.colorHex,
                    note = note.trim()
                )
                if (activeHouseholdId.isNotBlank()) {
                    vm.firestoreSyncRepo.syncSavingsGoal(activeHouseholdId, fbGoal)
                } else if (user != null) {
                    vm.firestoreSyncRepo.syncUserSavingsGoal(user.uid, fbGoal)
                }

                val recordedExpense = if (recordAsExpense && positiveDelta > 0.009) {
                    recordGoalDepositExpense(
                        goal = goal,
                        goalCloudId = cloudId,
                        amount = positiveDelta,
                        payerMember = null,
                        noteText = note,
                        activeHouseholdId = activeHouseholdId,
                        user = user
                    )
                    true
                } else {
                    false
                }

                val lang = Localization.Language.fromCode(vm._appLanguage.value)
                if (id == 0L) {
                    vm.showSnackbar(
                        if (recordedExpense) {
                            Localization.trFormat(
                                lang,
                                "Goal \"%1\$s\" created & deposit logged in expenses!",
                                "Ο στόχος «%1\$s» δημιουργήθηκε και το ποσό καταχωρήθηκε στα έξοδα!",
                                cleanTitle
                            )
                        } else {
                            Localization.trFormat(
                                lang,
                                "Goal \"%1\$s\" created!",
                                "Ο στόχος «%1\$s» δημιουργήθηκε!",
                                cleanTitle
                            )
                        }
                    )
                } else {
                    vm.showSnackbar(
                        if (recordedExpense) {
                            Localization.trFormat(
                                lang,
                                "Goal \"%1\$s\" updated & deposit logged in expenses!",
                                "Ο στόχος «%1\$s» ενημερώθηκε και το ποσό καταχωρήθηκε στα έξοδα!",
                                cleanTitle
                            )
                        } else {
                            Localization.trFormat(
                                lang,
                                "Goal \"%1\$s\" updated!",
                                "Ο στόχος «%1\$s» ενημερώθηκε!",
                                cleanTitle
                            )
                        }
                    )
                }

                vm.logHouseholdActivity(
                    actionType = if (id == 0L) "GOAL_CREATED" else "GOAL_UPDATED",
                    targetType = "GOAL",
                    summaryEl = if (id == 0L) "Νέος στόχος: $cleanTitle" else "Ενημέρωση στόχου: $cleanTitle",
                    summaryEn = if (id == 0L) "New goal: $cleanTitle" else "Updated goal: $cleanTitle",
                    amount = targetAmount,
                    householdIdOverride = activeHouseholdId.ifBlank { null }
                )
            } finally {
                vm.trackSyncEnd()
            }
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoal) {
        if (denyRegistarWrite(
                "Μόνο ο Δημιουργός ή Συντάκτης μπορεί να διαγράψει στόχους",
                "Only Creators or Editors can delete savings goals"
            )
        ) return
        val activeHouseholdId = vm._currentHousehold.value?.id.orEmpty()
        val user = vm._currentUser.value
        val cloudId = vm.savingsGoalCloudIdByLocalId.remove(goal.id) ?: goal.id.toString()
        val nowMs = System.currentTimeMillis()

        // Record deletion tombstones synchronously before async remote delete so intermediate snapshots cannot resurrect it
        vm.pendingLocalSavingsGoals.remove(cloudId)
        vm.pendingDeletedSavingsGoals[cloudId] = Pair(activeHouseholdId, nowMs)
        vm.pendingDeletedSavingsGoals[goal.id.toString()] = Pair(activeHouseholdId, nowMs)
        vm._allSavingsGoals.value = vm._allSavingsGoals.value.filterNot { it.id == goal.id }

        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                vm.repository.deleteSavingsGoal(goal)
                val legacyTitleFilter = if (cloudId.startsWith("goal_")) "" else goal.title
                if (activeHouseholdId.isNotBlank()) {
                    vm.firestoreSyncRepo.deleteSavingsGoal(activeHouseholdId, cloudId, legacyTitleFilter)
                    if (cloudId != goal.id.toString()) {
                        vm.firestoreSyncRepo.deleteSavingsGoal(activeHouseholdId, goal.id.toString(), "")
                    }
                } else if (user != null) {
                    vm.firestoreSyncRepo.deleteUserSavingsGoal(user.uid, cloudId, legacyTitleFilter)
                    if (cloudId != goal.id.toString()) {
                        vm.firestoreSyncRepo.deleteUserSavingsGoal(user.uid, goal.id.toString(), "")
                    }
                }
                vm.logHouseholdActivity(
                    actionType = "GOAL_DELETED",
                    targetType = "GOAL",
                    summaryEl = "Διαγραφή στόχου: ${goal.title}",
                    summaryEn = "Deleted goal: ${goal.title}",
                    amount = goal.targetAmount,
                    householdIdOverride = activeHouseholdId.ifBlank { null }
                )
                vm.showSnackbar(
                    Localization.trFormat(
                        vm._appLanguage.value,
                        "Goal \"%1\$s\" deleted",
                        "Ο στόχος «%1\$s» διαγράφηκε",
                        goal.title
                    )
                )
            } finally {
                vm.trackSyncEnd()
            }
        }
    }

    fun depositToGoal(
        goal: SavingsGoal,
        amount: Double,
        payerMember: FamilyMember? = null,
        note: String = ""
    ) {
        if (amount <= 0.0) return
        if (denyRegistarWrite(
                "Μόνο ο Δημιουργός ή Συντάκτης μπορεί να καταθέσει σε στόχο",
                "Only Creators or Editors can deposit to savings goals"
            )
        ) return
        val activeHouseholdId = vm._currentHousehold.value?.id?.takeIf { it.isNotBlank() }
            ?: vm.getOrEnsureHouseholdId(vm._currentUser.value)
        val user = vm._currentUser.value
        val cloudId = vm.savingsGoalCloudIdByLocalId[goal.id] ?: goal.id.toString()
        val nowMs = System.currentTimeMillis()
        val updated = goal.copy(currentAmount = goal.currentAmount + amount)

        vm.savingsGoalCloudIdByLocalId[updated.id] = cloudId
        vm.pendingLocalSavingsGoals[cloudId] = StartupSafety.PendingSavingsGoalMutation(
            cloudId = cloudId,
            householdId = activeHouseholdId,
            localEntity = updated,
            updatedAtMs = nowMs
        )
        vm._allSavingsGoals.value = vm._allSavingsGoals.value.map { if (it.id == updated.id) updated else it }

        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                vm.repository.updateSavingsGoal(updated)
                val fbGoal = FirebaseSavingsGoal(
                    id = cloudId,
                    householdId = activeHouseholdId,
                    userId = user?.uid ?: "",
                    title = updated.title,
                    targetAmount = updated.targetAmount,
                    currentAmount = updated.currentAmount,
                    deadlineDate = updated.deadlineDate,
                    iconKey = updated.iconKey,
                    colorHex = updated.colorHex,
                    note = updated.note
                )
                if (activeHouseholdId.isNotBlank()) {
                    vm.firestoreSyncRepo.syncSavingsGoal(activeHouseholdId, fbGoal)
                } else if (user != null) {
                    vm.firestoreSyncRepo.syncUserSavingsGoal(user.uid, fbGoal)
                }

                // Always log the goal deposit as a real household Expense so it shows on Home & Statistics
                recordGoalDepositExpense(
                    goal = updated,
                    goalCloudId = cloudId,
                    amount = amount,
                    payerMember = payerMember,
                    noteText = note,
                    activeHouseholdId = activeHouseholdId,
                    user = user
                )

                vm.logHouseholdActivity(
                    actionType = "GOAL_DEPOSIT",
                    targetType = "GOAL",
                    summaryEl = "Κατάθεση στον στόχο: ${goal.title}",
                    summaryEn = "Deposit to goal: ${goal.title}",
                    amount = amount,
                    householdIdOverride = activeHouseholdId.ifBlank { null }
                )

                val lang = Localization.Language.fromCode(vm._appLanguage.value)
                val amountFormatted = if (kotlin.math.abs(amount - amount.toLong()) < 0.005) {
                    String.format(Locale.US, "%.0f", amount)
                } else {
                    String.format(Locale.US, "%.2f", amount)
                }
                if (updated.currentAmount >= updated.targetAmount) {
                    vm.showSnackbar(
                        Localization.trFormat(
                            lang,
                            "🎉 Goal \"%1\$s\" reached & logged in expenses!",
                            "🎉 Συγχαρητήρια! Πετύχατε τον στόχο «%1\$s» (καταχωρήθηκε και στα έξοδα)!",
                            goal.title
                        )
                    )
                } else {
                    val curSymbol = com.example.util.CurrencyFormatter.currentSymbol
                    vm.showSnackbar(
                        Localization.trFormat(
                            lang,
                            "Deposited %1\$s%2\$s to \"%3\$s\" & logged in expenses",
                            "Κατατέθηκαν %1\$s%2\$s στον στόχο «%3\$s» (εμφανίζεται σε Έξοδα & Στατιστικά)",
                            amountFormatted,
                            curSymbol,
                            goal.title
                        )
                    )
                }
            } finally {
                vm.trackSyncEnd()
            }
        }
    }

    /**
     * Keeps the linked SavingsGoal synchronized when a goal-deposit Expense is edited,
     * deleted, or restored (undo) from the Home screen.
     */
    fun adjustLinkedGoalForExpenseDelta(expenseCloudId: String, amountDelta: Double) {
        if (kotlin.math.abs(amountDelta) < 0.005) return
        if (!vm.hasOwnerPermissions()) return
        val goalCloudId = SavingsGoalExpenseLinkHelper.extractGoalCloudId(expenseCloudId) ?: return

        val matchedGoal = vm._allSavingsGoals.value.find { goal ->
            val mappedCloud = vm.savingsGoalCloudIdByLocalId[goal.id] ?: goal.id.toString()
            mappedCloud == goalCloudId || goal.id.toString() == goalCloudId
        } ?: return

        val activeHouseholdId = vm._currentHousehold.value?.id.orEmpty()
        val user = vm._currentUser.value
        val nowMs = System.currentTimeMillis()
        val newAmount = (matchedGoal.currentAmount + amountDelta).coerceAtLeast(0.0)
        if (kotlin.math.abs(newAmount - matchedGoal.currentAmount) < 0.005) return

        val updatedGoal = matchedGoal.copy(currentAmount = newAmount)
        val effectiveCloudId = vm.savingsGoalCloudIdByLocalId[matchedGoal.id] ?: goalCloudId

        vm.savingsGoalCloudIdByLocalId[updatedGoal.id] = effectiveCloudId
        vm.pendingLocalSavingsGoals[effectiveCloudId] = StartupSafety.PendingSavingsGoalMutation(
            cloudId = effectiveCloudId,
            householdId = activeHouseholdId,
            localEntity = updatedGoal,
            updatedAtMs = nowMs
        )
        vm._allSavingsGoals.value = vm._allSavingsGoals.value.map { if (it.id == updatedGoal.id) updatedGoal else it }

        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try {
                vm.repository.updateSavingsGoal(updatedGoal)
                val fbGoal = FirebaseSavingsGoal(
                    id = effectiveCloudId,
                    householdId = activeHouseholdId,
                    userId = user?.uid ?: "",
                    title = updatedGoal.title,
                    targetAmount = updatedGoal.targetAmount,
                    currentAmount = updatedGoal.currentAmount,
                    deadlineDate = updatedGoal.deadlineDate,
                    iconKey = updatedGoal.iconKey,
                    colorHex = updatedGoal.colorHex,
                    note = updatedGoal.note
                )
                if (activeHouseholdId.isNotBlank()) {
                    vm.firestoreSyncRepo.syncSavingsGoal(activeHouseholdId, fbGoal)
                } else if (user != null) {
                    vm.firestoreSyncRepo.syncUserSavingsGoal(user.uid, fbGoal)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w("OikoNomosVM", "adjustLinkedGoalForExpenseDelta sync warning: ${e.message}")
            }
        }
    }

    private suspend fun ensureSavingsCategory(
        activeHouseholdId: String,
        user: FirebaseUser?
    ): ExpenseCategory {
        val currentCats = vm._allCategories.value
        SavingsGoalExpenseLinkHelper.findSavingsCategory(currentCats)?.let { return it }

        val lang = Localization.Language.fromCode(vm._appLanguage.value)
        val nextSortOrder = (currentCats.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val safeCatId = vm.allocateNextSafeCategoryId()
        val newCategory = ExpenseCategory(
            id = safeCatId,
            name = SavingsGoalExpenseLinkHelper.defaultSavingsCategoryName(lang),
            iconKey = SavingsGoalExpenseLinkHelper.DEFAULT_SAVINGS_CATEGORY_ICON,
            monthlyBudget = 0.0,
            colorHex = SavingsGoalExpenseLinkHelper.DEFAULT_SAVINGS_CATEGORY_COLOR,
            voiceKeywords = SavingsGoalExpenseLinkHelper.DEFAULT_SAVINGS_CATEGORY_KEYWORDS,
            sortOrder = nextSortOrder,
            fontColorHex = 0L
        )
        vm.repository.insertCategory(newCategory)
        val savedId = safeCatId
        val persisted = newCategory.copy(id = savedId)
        vm._allCategories.value = (vm._allCategories.value.filter { it.id != savedId } + persisted)
            .sortedWith(compareBy<ExpenseCategory> { it.sortOrder }.thenBy { it.id })

        if (activeHouseholdId.isNotBlank()) {
            try {
                val fbCat = FirebaseCategory(
                    id = persisted.id.toString(),
                    householdId = activeHouseholdId,
                    userId = user?.uid ?: "",
                    name = persisted.name,
                    iconKey = persisted.iconKey,
                    monthlyBudget = persisted.monthlyBudget,
                    colorHex = persisted.colorHex,
                    voiceKeywords = persisted.voiceKeywords,
                    sortOrder = persisted.sortOrder,
                    fontColorHex = persisted.fontColorHex
                )
                vm.firestoreSyncRepo.syncAndCleanCategory(listOf(activeHouseholdId), user?.uid ?: "", fbCat, "")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w("OikoNomosVM", "ensureSavingsCategory cloud sync warning: ${e.message}")
            }
        }
        return persisted
    }

    private suspend fun recordGoalDepositExpense(
        goal: SavingsGoal,
        goalCloudId: String,
        amount: Double,
        payerMember: FamilyMember?,
        noteText: String,
        activeHouseholdId: String,
        user: FirebaseUser?
    ) {
        val lang = Localization.Language.fromCode(vm._appLanguage.value)
        val savingsCat = ensureSavingsCategory(activeHouseholdId, user)
        val fallbackLabel = Localization.allMembersWord(lang)
        val payerFields = if (payerMember != null) {
            DefaultPayerHelper.buildEqualPayerFields(listOf(payerMember), amount, fallbackLabel = fallbackLabel)
        } else {
            val configuredDefaults = vm.resolveDefaultPayerMembers()
            val chosen = if (configuredDefaults.isNotEmpty()) configuredDefaults else listOfNotNull(vm._allMembers.value.firstOrNull())
            DefaultPayerHelper.buildEqualPayerFields(chosen, amount, fallbackLabel = fallbackLabel)
        }
        val expenseCloudId = SavingsGoalExpenseLinkHelper.buildGoalDepositCloudId(goalCloudId)
        val nowMs = System.currentTimeMillis()

        val depositExpense = Expense(
            cloudId = expenseCloudId,
            title = SavingsGoalExpenseLinkHelper.buildDepositExpenseTitle(goal.title, lang),
            amount = amount,
            categoryId = savingsCat.id,
            categoryName = savingsCat.name,
            timestamp = nowMs,
            splitType = "ALL_FAMILY",
            memberIdsCsv = "",
            memberNamesDisplay = "Κοινό Οικογένειας",
            splitAmountPerPerson = amount,
            note = SavingsGoalExpenseLinkHelper.buildDepositExpenseNote(goal.title, noteText, lang),
            payerMemberId = payerFields.primaryId,
            payerMemberName = payerFields.displayName,
            payerMemberIdsCsv = payerFields.idsCsv,
            payerAmountsCsv = payerFields.amountsCsv
        )

        vm.prefs.edit().putBoolean("has_entered_any_expense", true).apply()
        vm._isInitialExpensesLoading.value = false

        val insertedId = vm.repository.insertExpense(depositExpense)
        val savedExpense = depositExpense.copy(id = insertedId, cloudId = expenseCloudId)
        vm.pendingLocalExpenseIds[expenseCloudId] = System.currentTimeMillis()
        vm._allExpenses.value = (listOf(savedExpense) + vm._allExpenses.value.filter { it.id != savedExpense.id && it.cloudId != expenseCloudId })
            .sortedWith(compareByDescending<Expense> { it.timestamp }.thenByDescending { it.id })

        val creatorName = vm._userProfile.value?.displayName?.ifBlank { null }
            ?: user?.displayName?.ifBlank { null }
            ?: "Μέλος"
        val fbExpense = savedExpense.toFirebaseExpense(activeHouseholdId, user?.uid ?: "", creatorName)
        try {
            if (activeHouseholdId.isNotBlank()) {
                vm.firestoreSyncRepo.syncExpense(fbExpense)
            } else if (user != null) {
                vm.firestoreSyncRepo.syncUserExpense(user.uid, fbExpense)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w("OikoNomosVM", "recordGoalDepositExpense cloud sync warning: ${e.message}")
        }
    }

    // Standing Orders & Reminders (4.2 RBAC + 1.5 Multi-Frequency)
    fun openRemindersManagerDialog() {
        if (denyRegistarWrite(
                "Μόνο ο Δημιουργός ή Συντάκτης έχει πρόσβαση στις υπενθυμίσεις",
                "Only Creators or Editors can access reminders"
            )
        ) return
        vm.dismissAllSubPages()
        vm._showRemindersManagerDialog.value = true
    }

    fun triggerReminderById(reminderId: Long) {
        if (!vm.hasOwnerPermissions() || reminderId <= 0L) return
        val hid = vm._currentHousehold.value?.id.orEmpty()
        val nowMs = System.currentTimeMillis()
        val matchingRec = vm.allRecurringExpenses.value.find { it.id == reminderId }
        if (matchingRec != null) {
            val periodKey = RecurringScheduleHelper.periodKeyFor(matchingRec, nowMs)
            val key = "${hid}|${reminderId}|${periodKey}"
            val updated = vm._dismissedReminderKeys.value - key
            vm._dismissedReminderKeys.value = updated
            vm.prefs.edit().putStringSet("dismissed_reminder_keys", updated).apply()
        }
        vm._manualTriggeredReminderIds.value = vm._manualTriggeredReminderIds.value + reminderId
    }

    fun dismissDueReminder(reminder: RecurringExpense) {
        val hid = vm._currentHousehold.value?.id.orEmpty()
        val periodKey = RecurringScheduleHelper.periodKeyFor(reminder, System.currentTimeMillis())
        val key = "${hid}|${reminder.id}|${periodKey}"
        val updated = (vm._dismissedReminderKeys.value + key).toList().takeLast(150).toSet()
        vm._dismissedReminderKeys.value = updated
        vm.prefs.edit().putStringSet("dismissed_reminder_keys", updated).apply()
        vm._manualTriggeredReminderIds.value = vm._manualTriggeredReminderIds.value - reminder.id

        // Persist lastGeneratedMonth to Room & Firestore so logout/login or reinstall never re-shows this period's reminder
        if (reminder.lastGeneratedMonth != periodKey) {
            vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
                try {
                    val stamped = reminder.copy(lastGeneratedMonth = periodKey)
                    vm.repository.updateRecurringExpense(stamped)
                    val user = vm._currentUser.value
                    val householdId = vm._currentHousehold.value?.id ?: ""
                    val fbRecurring = FirebaseRecurringExpense(
                        id = stamped.id.toString(),
                        householdId = householdId,
                        userId = user?.uid ?: "",
                        title = stamped.title,
                        amount = stamped.amount,
                        categoryId = stamped.categoryId,
                        categoryName = stamped.categoryName,
                        frequency = stamped.frequency,
                        dayOfMonth = stamped.dayOfMonth,
                        reminderHour = stamped.reminderHour,
                        reminderMinute = stamped.reminderMinute,
                        splitType = stamped.splitType,
                        memberIdsCsv = stamped.memberIdsCsv,
                        memberNamesDisplay = stamped.memberNamesDisplay,
                        note = stamped.note,
                        isActive = stamped.isActive,
                        lastGeneratedMonth = stamped.lastGeneratedMonth,
                        payerMemberId = stamped.payerMemberId,
                        payerMemberName = stamped.payerMemberName,
                        payerMemberIdsCsv = stamped.payerMemberIdsCsv,
                        payerAmountsCsv = stamped.payerAmountsCsv,
                        anchorMonth = stamped.anchorMonth
                    )
                    if (householdId.isNotBlank()) {
                        vm.firestoreSyncRepo.syncRecurringExpense(householdId, fbRecurring)
                    } else if (user != null) {
                        vm.firestoreSyncRepo.syncUserRecurringExpense(user.uid, fbRecurring)
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w("OikoNomosVM", "dismissDueReminder cloud persist warning: ${e.message}")
                }
            }
        }
    }

    fun logExpenseFromReminder(reminder: RecurringExpense) {
        if (denyRegistarWrite(
                "Μόνο ο Δημιουργός ή Συντάκτης μπορεί να καταχωρήσει από υπενθύμιση",
                "Only Creators or Editors can log expenses from reminders"
            )
        ) return
        dismissDueReminder(reminder)
        val hasReminderPayer = reminder.payerMemberId != 0L || reminder.payerMemberIdsCsv.isNotBlank() || reminder.payerMemberName.isNotBlank()
        val fallbackFields = com.example.util.DefaultPayerHelper.buildEqualPayerFields(
            vm.resolveDefaultPayerMembers(),
            reminder.amount,
            fallbackLabel = ""
        )
        val resolvedPayerId = if (hasReminderPayer) reminder.payerMemberId else fallbackFields.primaryId
        val resolvedPayerName = if (hasReminderPayer) reminder.payerMemberName else fallbackFields.displayName
        val resolvedPayerIdsCsv = if (hasReminderPayer) {
            reminder.payerMemberIdsCsv.ifBlank { if (resolvedPayerId != 0L) "$resolvedPayerId" else "" }
        } else {
            fallbackFields.idsCsv
        }
        val resolvedPayerAmountsCsv = if (hasReminderPayer) reminder.payerAmountsCsv else fallbackFields.amountsCsv

        val prefilledExpense = Expense(
            id = 0L,
            title = reminder.title,
            amount = reminder.amount,
            categoryId = reminder.categoryId,
            categoryName = reminder.categoryName,
            timestamp = System.currentTimeMillis(),
            splitType = reminder.splitType.ifBlank { "ALL_FAMILY" },
            memberIdsCsv = reminder.memberIdsCsv,
            memberNamesDisplay = reminder.memberNamesDisplay.ifBlank { "Κοινό Οικογένειας" },
            note = reminder.note.ifBlank { "Πάγιος λογαριασμός" },
            payerMemberId = resolvedPayerId,
            payerMemberName = resolvedPayerName,
            payerMemberIdsCsv = resolvedPayerIdsCsv,
            payerAmountsCsv = resolvedPayerAmountsCsv
        )
        vm.openManualExpenseDialog(prefilledExpense)
    }

    fun openRecurringExpenseDialog(recurring: RecurringExpense? = null) {
        if (denyRegistarWrite(
                "Μόνο ο Δημιουργός ή Συντάκτης μπορεί να διαχειριστεί πάγιες εντολές",
                "Only Creators or Editors can manage standing orders"
            )
        ) return
        if (recurring == null && !vm.isCurrentUserPro() && vm.allRecurringExpenses.value.size >= 2) {
            vm.showPremiumPaywall("recurring_expense_limit")
            vm.showSnackbar(
                Localization.tr(
                    vm._appLanguage.value,
                    "Free edition supports up to 2 Standing Orders",
                    "Η δωρεάν έκδοση υποστηρίζει έως 2 Πάγιες Εντολές"
                )
            )
            return
        }
        vm._editingRecurringExpense.value = recurring
        vm._showRecurringExpenseDialog.value = true
    }

    fun saveRecurringExpense(
        id: Long,
        title: String,
        amount: Double,
        categoryId: Long,
        categoryName: String,
        dayOfMonth: Int,
        note: String,
        payerMemberId: Long = 0L,
        payerMemberName: String = "",
        reminderHour: Int = 9,
        reminderMinute: Int = 0,
        splitType: String = "ALL_FAMILY",
        memberIds: List<Long> = emptyList(),
        memberNamesDisplay: String = "Κοινό Οικογένειας",
        payerMemberIdsCsv: String = "",
        payerAmountsCsv: String = "",
        autoRegisterIfDue: Boolean = true,
        frequency: String = RecurringScheduleHelper.FREQ_MONTHLY,
        anchorMonth: Int = 0
    ) {
        if (denyRegistarWrite(
                "Μόνο ο Δημιουργός ή Συντάκτης μπορεί να αποθηκεύσει πάγιες εντολές",
                "Only Creators or Editors can save standing orders"
            )
        ) {
            vm.dismissRecurringExpenseDialog()
            return
        }
        if (id == 0L && !vm.isCurrentUserPro() && vm.allRecurringExpenses.value.size >= 2) {
            vm.dismissRecurringExpenseDialog()
            vm.showPremiumPaywall("recurring_expense_limit")
            vm.showSnackbar(
                Localization.tr(
                    vm._appLanguage.value,
                    "Free edition supports up to 2 Standing Orders",
                    "Η δωρεάν έκδοση υποστηρίζει έως 2 Πάγιες Εντολές"
                )
            )
            return
        }
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                val normFreq = RecurringScheduleHelper.normalizeFrequency(frequency)
                val clampedDay = if (normFreq == RecurringScheduleHelper.FREQ_WEEKLY || normFreq == RecurringScheduleHelper.FREQ_BIWEEKLY) {
                    dayOfMonth.coerceIn(1, 7)
                } else {
                    dayOfMonth.coerceIn(1, 31)
                }
                val clampedAnchor = anchorMonth.coerceIn(0, 11)
                val clampedHour = reminderHour.coerceIn(0, 23)
                val clampedMinute = reminderMinute.coerceIn(0, 59)

                val normalizedNewTitle = GreekVoiceParser.normalizeGreek(title.trim().lowercase())
                val allExisting = vm.repository.getAllRecurringExpensesList()
                val existing = if (id != 0L) {
                    allExisting.find { it.id == id }
                } else {
                    allExisting.find {
                        GreekVoiceParser.normalizeGreek(it.title.trim().lowercase()) == normalizedNewTitle &&
                            it.frequency == normFreq &&
                            it.dayOfMonth == clampedDay &&
                            it.anchorMonth == clampedAnchor &&
                            it.reminderHour == clampedHour &&
                            it.reminderMinute == clampedMinute
                    }
                }
                val targetId = existing?.id ?: id

                val resolvedHour = if (!autoRegisterIfDue) clampedHour else (existing?.reminderHour ?: clampedHour)
                val resolvedMinute = if (!autoRegisterIfDue) clampedMinute else (existing?.reminderMinute ?: clampedMinute)
                val resolvedSplitType = if (!autoRegisterIfDue) splitType else (existing?.splitType ?: splitType)
                val resolvedMemberIdsCsv = if (!autoRegisterIfDue) memberIds.joinToString(",") else (existing?.memberIdsCsv ?: "")
                val resolvedMemberNamesDisplay = if (!autoRegisterIfDue) memberNamesDisplay else (existing?.memberNamesDisplay ?: memberNamesDisplay)
                val resolvedPayerIdsCsv = if (payerMemberIdsCsv.isNotBlank() || !autoRegisterIfDue) payerMemberIdsCsv else (existing?.payerMemberIdsCsv ?: "")
                val resolvedPayerAmountsCsv = if (payerAmountsCsv.isNotBlank() || !autoRegisterIfDue) payerAmountsCsv else (existing?.payerAmountsCsv ?: "")

                val fallbackFields = DefaultPayerHelper.buildEqualPayerFields(
                    vm.resolveDefaultPayerMembers(),
                    amount,
                    fallbackLabel = ""
                )
                val useDefaultPayers = payerMemberId <= 0L && resolvedPayerIdsCsv.isBlank() && payerMemberName.isBlank()
                var effPayerId = if (useDefaultPayers) fallbackFields.primaryId else payerMemberId
                val effPayerName = if (useDefaultPayers) fallbackFields.displayName else payerMemberName.ifBlank { fallbackFields.displayName }
                var effPayerIdsCsv = if (useDefaultPayers) fallbackFields.idsCsv else resolvedPayerIdsCsv
                var effPayerAmountsCsv = if (useDefaultPayers) fallbackFields.amountsCsv else resolvedPayerAmountsCsv

                if (effPayerIdsCsv.isBlank() && effPayerName.isNotBlank()) {
                    val labelMembers = resolveMembersFromDisplayLabel(effPayerName, vm._allMembers.value)
                    if (labelMembers.size >= 2) {
                        val multi = DefaultPayerHelper.buildEqualPayerFields(labelMembers, amount)
                        effPayerId = multi.primaryId
                        effPayerIdsCsv = multi.idsCsv
                        if (effPayerAmountsCsv.isBlank()) effPayerAmountsCsv = multi.amountsCsv
                    } else if (effPayerId > 0L) {
                        effPayerIdsCsv = "$effPayerId"
                    }
                }

                var recurring = RecurringExpense(
                    id = targetId,
                    title = title,
                    amount = amount,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    frequency = normFreq,
                    dayOfMonth = clampedDay,
                    reminderHour = resolvedHour,
                    reminderMinute = resolvedMinute,
                    splitType = resolvedSplitType,
                    memberIdsCsv = resolvedMemberIdsCsv,
                    memberNamesDisplay = resolvedMemberNamesDisplay,
                    note = note,
                    isActive = existing?.isActive ?: true,
                    lastGeneratedMonth = existing?.lastGeneratedMonth ?: "",
                    payerMemberId = effPayerId,
                    payerMemberName = effPayerName,
                    payerMemberIdsCsv = effPayerIdsCsv,
                    payerAmountsCsv = effPayerAmountsCsv,
                    anchorMonth = clampedAnchor
                )

                val generatedId = if (targetId == 0L) vm.repository.insertRecurringExpense(recurring) else {
                    vm.repository.updateRecurringExpense(recurring)
                    targetId
                }
                recurring = recurring.copy(id = generatedId)

                val user = vm._currentUser.value
                val householdId = vm.getOrEnsureHouseholdId(user)
                val nowMillis = System.currentTimeMillis()
                val periodKey = RecurringScheduleHelper.periodKeyFor(recurring, nowMillis)
                var autoRegisteredThisPeriod = false

                // For pure reminders (!autoRegisterIfDue), if scheduled time today has already passed,
                // stamp current periodKey so it waits for the next scheduled day & time instead of firing now or on re-login.
                // Conversely, if the user rescheduled to a future time today, clear lastGeneratedMonth so it fires on time.
                if (!autoRegisterIfDue) {
                    val scheduleChanged = existing == null ||
                        existing.frequency != recurring.frequency ||
                        existing.dayOfMonth != recurring.dayOfMonth ||
                        existing.anchorMonth != recurring.anchorMonth ||
                        existing.reminderHour != recurring.reminderHour ||
                        existing.reminderMinute != recurring.reminderMinute
                    if (scheduleChanged) {
                        val updatedPeriodStamp = if (RecurringScheduleHelper.isReminderTimeAlreadyPassedToday(recurring, nowMillis)) {
                            periodKey
                        } else {
                            ""
                        }
                        if (recurring.lastGeneratedMonth != updatedPeriodStamp) {
                            recurring = recurring.copy(lastGeneratedMonth = updatedPeriodStamp)
                            vm.repository.updateRecurringExpense(recurring)
                        }
                    }
                }

                if (autoRegisterIfDue && RecurringScheduleHelper.isDueNow(recurring, nowMillis)) {
                    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
                    val currentYear = cal.get(Calendar.YEAR)
                    val currentMonth = cal.get(Calendar.MONTH)
                    val monthStart = Calendar.getInstance().apply {
                        set(currentYear, currentMonth, 1, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val monthEnd = Calendar.getInstance().apply {
                        set(currentYear, currentMonth, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis

                    val allExpenses = vm.repository.getAllExpensesList()
                    val relevantExpenses = if (normFreq == RecurringScheduleHelper.FREQ_WEEKLY || normFreq == RecurringScheduleHelper.FREQ_BIWEEKLY) {
                        val windowMs = if (normFreq == RecurringScheduleHelper.FREQ_WEEKLY) 7L * 86_400_000L else 14L * 86_400_000L
                        allExpenses.filter { (nowMillis - it.timestamp) in 0L..windowMs }
                    } else {
                        allExpenses.filter { it.timestamp in monthStart..monthEnd }
                    }

                    if (!RecurringExpenseAutoProcessor.isAlreadyGeneratedForPeriod(recurring, periodKey, relevantExpenses)) {
                        val targetTimestamp = RecurringScheduleHelper.calculateTargetTimestampForPeriod(recurring, nowMillis)
                        val noteLang = Localization.Language.fromCode(vm._appLanguage.value)
                        val noteText = note.ifBlank {
                            Localization.trFormat(
                                noteLang,
                                "Standing order (%1\$s)",
                                "Πάγια εντολή (%1\$s)",
                                RecurringScheduleHelper.scheduleSummary(recurring, noteLang)
                            )
                        }
                        val docId = "recurring_${generatedId}_${periodKey}"

                        val newExpense = Expense(
                            cloudId = docId,
                            title = title,
                            amount = amount,
                            categoryId = categoryId,
                            categoryName = categoryName,
                            timestamp = targetTimestamp,
                            splitType = recurring.splitType,
                            memberIdsCsv = recurring.memberIdsCsv,
                            memberNamesDisplay = recurring.memberNamesDisplay,
                            note = noteText,
                            payerMemberId = effPayerId,
                            payerMemberName = effPayerName,
                            payerMemberIdsCsv = recurring.payerMemberIdsCsv,
                            payerAmountsCsv = recurring.payerAmountsCsv
                        )
                        val insertedId = vm.repository.insertExpense(newExpense)
                        val savedExpense = newExpense.copy(id = insertedId, cloudId = docId)
                        vm.pendingLocalExpenseIds[docId] = System.currentTimeMillis()
                        vm._allExpenses.value = (listOf(savedExpense) + vm._allExpenses.value).distinctBy { it.id }.sortedWith(
                            compareByDescending<Expense> { it.timestamp }.thenByDescending { it.id }
                        )
                        if (householdId.isNotBlank()) {
                            try {
                                vm.firestoreSyncRepo.syncExpense(savedExpense.toFirebaseExpense(householdId, user?.uid ?: "", user?.displayName ?: "Μέλος"))
                            } catch (e: Exception) {
                                Log.w("OikoNomosVM", "Cloud household expense sync: ${e.message}")
                            }
                        } else if (user != null) {
                            try {
                                vm.firestoreSyncRepo.syncUserExpense(user.uid, savedExpense.toFirebaseExpense(householdId, user.uid, user.displayName ?: "Χρήστης"))
                            } catch (e: Exception) {
                                Log.w("OikoNomosVM", "Cloud user expense sync: ${e.message}")
                            }
                        }
                        recurring = recurring.copy(lastGeneratedMonth = periodKey)
                        vm.repository.updateRecurringExpense(recurring)
                        autoRegisteredThisPeriod = true
                    }
                }

                BillReminderManager.scheduleReminder(vm.getApplication(), recurring)

                // Clear any prior dismissal/manual flag for this period so the new exact schedule evaluates cleanly
                val periodDismissKey = "${householdId}|${generatedId}|${periodKey}"
                val updatedDismissed = vm._dismissedReminderKeys.value - periodDismissKey
                vm._dismissedReminderKeys.value = updatedDismissed
                vm.prefs.edit().putStringSet("dismissed_reminder_keys", updatedDismissed).apply()
                vm._manualTriggeredReminderIds.value = vm._manualTriggeredReminderIds.value - generatedId
                vm._reminderClockTickMs.value = System.currentTimeMillis()

                val fbRecurring = FirebaseRecurringExpense(
                    id = generatedId.toString(),
                    householdId = householdId,
                    userId = user?.uid ?: "",
                    title = title,
                    amount = amount,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    frequency = recurring.frequency,
                    dayOfMonth = recurring.dayOfMonth,
                    reminderHour = recurring.reminderHour,
                    reminderMinute = recurring.reminderMinute,
                    splitType = recurring.splitType,
                    memberIdsCsv = recurring.memberIdsCsv,
                    memberNamesDisplay = recurring.memberNamesDisplay,
                    note = note,
                    isActive = recurring.isActive,
                    lastGeneratedMonth = recurring.lastGeneratedMonth,
                    payerMemberId = effPayerId,
                    payerMemberName = effPayerName,
                    payerMemberIdsCsv = recurring.payerMemberIdsCsv,
                    payerAmountsCsv = recurring.payerAmountsCsv,
                    anchorMonth = recurring.anchorMonth
                )
                if (householdId.isNotBlank()) {
                    vm.firestoreSyncRepo.syncRecurringExpense(householdId, fbRecurring)
                } else if (user != null) {
                    vm.firestoreSyncRepo.syncUserRecurringExpense(user.uid, fbRecurring)
                }
                vm.logHouseholdActivity(
                    actionType = if (id == 0L) "RECURRING_CREATED" else "RECURRING_UPDATED",
                    targetType = "RECURRING",
                    summaryEl = if (id == 0L) "Νέα πάγια εντολή: $title" else "Ενημέρωση πάγιας εντολής: $title",
                    summaryEn = if (id == 0L) "New standing order: $title" else "Updated standing order: $title",
                    amount = amount
                )

                val lang = Localization.Language.fromCode(vm._appLanguage.value)
                val schedSummary = RecurringScheduleHelper.scheduleSummary(recurring, lang)
                if (!autoRegisterIfDue) {
                    val timeStr = String.format(Locale.US, "%02d:%02d", recurring.reminderHour, recurring.reminderMinute)
                    vm.showSnackbar(
                        if (id == 0L) {
                            Localization.trFormat(
                                lang,
                                "Reminder \"%1\$s\" saved (%2\$s, %3\$s)!",
                                "Η υπενθύμιση «%1\$s» αποθηκεύτηκε (%2\$s, %3\$s)!",
                                title,
                                schedSummary,
                                timeStr
                            )
                        } else {
                            Localization.trFormat(
                                lang,
                                "Reminder \"%1\$s\" updated (%2\$s, %3\$s)!",
                                "Η υπενθύμιση «%1\$s» ενημερώθηκε (%2\$s, %3\$s)!",
                                title,
                                schedSummary,
                                timeStr
                            )
                        }
                    )
                } else if (autoRegisteredThisPeriod) {
                    vm.showSnackbar(
                        Localization.trFormat(
                            lang,
                            "Standing order \"%1\$s\" automatically logged to expenses!",
                            "Η πάγια εντολή «%1\$s» καταχωρήθηκε αυτόματα στα έξοδα!",
                            title
                        )
                    )
                } else if (id == 0L) {
                    vm.showSnackbar(
                        Localization.trFormat(
                            lang,
                            "Standing order \"%1\$s\" saved (%2\$s).",
                            "Η πάγια εντολή «%1\$s» αποθηκεύτηκε (%2\$s).",
                            title,
                            schedSummary
                        )
                    )
                } else {
                    vm.showSnackbar(
                        Localization.trFormat(
                            lang,
                            "Standing order \"%1\$s\" updated successfully!",
                            "Η πάγια εντολή «%1\$s» ενημερώθηκε επιτυχώς!",
                            title
                        )
                    )
                }
            } finally {
                vm.trackSyncEnd()
            }
        }
    }

    fun deleteRecurringExpense(recurring: RecurringExpense) {
        if (denyRegistarWrite(
                "Μόνο ο Δημιουργός ή Συντάκτης μπορεί να διαγράψει πάγιες εντολές",
                "Only Creators or Editors can delete standing orders"
            )
        ) return
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                val normTitle = GreekVoiceParser.normalizeGreek(recurring.title.trim().lowercase())
                val matchingCopies = vm.repository.getAllRecurringExpensesList().filter {
                    it.id == recurring.id || (normTitle.isNotBlank() && GreekVoiceParser.normalizeGreek(it.title.trim().lowercase()) == normTitle)
                }
                matchingCopies.forEach { copy ->
                    BillReminderManager.cancelReminder(vm.getApplication(), copy.id)
                    vm.repository.deleteRecurringExpense(copy)
                }
                if (matchingCopies.none { it.id == recurring.id }) {
                    BillReminderManager.cancelReminder(vm.getApplication(), recurring.id)
                    vm.repository.deleteRecurringExpense(recurring)
                }
                val householdId = vm._currentHousehold.value?.id ?: ""
                val user = vm._currentUser.value
                if (householdId.isNotBlank()) vm.firestoreSyncRepo.deleteRecurringExpense(householdId, recurring.id.toString(), recurring.title)
                if (user != null) vm.firestoreSyncRepo.deleteUserRecurringExpense(user.uid, recurring.id.toString(), recurring.title)
                vm.logHouseholdActivity(
                    actionType = "RECURRING_DELETED",
                    targetType = "RECURRING",
                    summaryEl = "Διαγραφή πάγιας εντολής: ${recurring.title}",
                    summaryEn = "Deleted standing order: ${recurring.title}",
                    amount = recurring.amount
                )
                vm.showSnackbar(
                    Localization.trFormat(
                        vm._appLanguage.value,
                        "Reminder \"%1\$s\" deleted",
                        "Η υπενθύμιση «%1\$s» διαγράφηκε",
                        recurring.title
                    )
                )
            } finally {
                vm.trackSyncEnd()
            }
        }
    }

    fun toggleRecurringActive(recurring: RecurringExpense) {
        if (denyRegistarWrite(
                "Μόνο ο Δημιουργός ή Συντάκτης μπορεί να τροποποιήσει πάγιες εντολές",
                "Only Creators or Editors can toggle standing orders"
            )
        ) return
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            val updated = recurring.copy(isActive = !recurring.isActive)
            vm.repository.updateRecurringExpense(updated)
            if (updated.isActive) BillReminderManager.scheduleReminder(vm.getApplication(), updated)
            else BillReminderManager.cancelReminder(vm.getApplication(), updated.id)
            val householdId = vm._currentHousehold.value?.id ?: ""
            val user = vm._currentUser.value
            val fbRecurring = FirebaseRecurringExpense(
                id = updated.id.toString(),
                householdId = householdId,
                userId = user?.uid ?: "",
                title = updated.title,
                amount = updated.amount,
                categoryId = updated.categoryId,
                categoryName = updated.categoryName,
                frequency = updated.frequency,
                dayOfMonth = updated.dayOfMonth,
                reminderHour = updated.reminderHour,
                reminderMinute = updated.reminderMinute,
                splitType = updated.splitType,
                memberIdsCsv = updated.memberIdsCsv,
                memberNamesDisplay = updated.memberNamesDisplay,
                note = updated.note,
                isActive = updated.isActive,
                lastGeneratedMonth = updated.lastGeneratedMonth,
                payerMemberId = updated.payerMemberId,
                payerMemberName = updated.payerMemberName,
                payerMemberIdsCsv = updated.payerMemberIdsCsv,
                payerAmountsCsv = updated.payerAmountsCsv,
                anchorMonth = updated.anchorMonth
            )
            if (householdId.isNotBlank()) vm.firestoreSyncRepo.syncRecurringExpense(householdId, fbRecurring)
            else if (user != null) vm.firestoreSyncRepo.syncUserRecurringExpense(user.uid, fbRecurring)
            val lang = Localization.Language.fromCode(vm._appLanguage.value)
            vm.showSnackbar(
                if (updated.isActive) {
                    Localization.trFormat(lang, "Reminder \"%1\$s\" enabled", "Η υπενθύμιση «%1\$s» ενεργοποιήθηκε", recurring.title)
                } else {
                    Localization.trFormat(lang, "Reminder \"%1\$s\" disabled", "Η υπενθύμιση «%1\$s» απενεργοποιήθηκε", recurring.title)
                }
            )
        }
    }

    fun executeRecurringExpenseNow(recurring: RecurringExpense) {
        if (denyRegistarWrite(
                "Μόνο ο Δημιουργός ή Συντάκτης μπορεί να εκτελέσει πάγια εντολή",
                "Only Creators or Editors can execute standing orders"
            )
        ) return
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                val nowMillis = System.currentTimeMillis()
                val periodKey = RecurringScheduleHelper.periodKeyFor(recurring, nowMillis)
                val targetTimestamp = RecurringScheduleHelper.calculateTargetTimestampForPeriod(recurring, nowMillis)
                val docId = "recurring_${recurring.id}_${periodKey}"

                val expense = Expense(
                    cloudId = docId,
                    title = recurring.title,
                    amount = recurring.amount,
                    categoryId = recurring.categoryId,
                    categoryName = recurring.categoryName,
                    timestamp = targetTimestamp,
                    splitType = recurring.splitType,
                    memberIdsCsv = recurring.memberIdsCsv,
                    memberNamesDisplay = recurring.memberNamesDisplay,
                    note = recurring.note.ifBlank {
                        val noteLang = Localization.Language.fromCode(vm._appLanguage.value)
                        Localization.trFormat(
                            noteLang,
                            "Standing order executed (%1\$s)",
                            "Εκτέλεση πάγιας εντολής (%1\$s)",
                            RecurringScheduleHelper.scheduleSummary(recurring, noteLang)
                        )
                    },
                    payerMemberId = recurring.payerMemberId,
                    payerMemberName = recurring.payerMemberName,
                    payerMemberIdsCsv = recurring.payerMemberIdsCsv,
                    payerAmountsCsv = recurring.payerAmountsCsv
                )
                val newId = vm.repository.insertExpense(expense)
                val savedExpense = expense.copy(id = newId, cloudId = docId)
                vm.pendingLocalExpenseIds[docId] = System.currentTimeMillis()
                vm._allExpenses.value = (listOf(savedExpense) + vm._allExpenses.value).distinctBy { it.id }.sortedWith(
                    compareByDescending<Expense> { it.timestamp }.thenByDescending { it.id }
                )

                val updatedRecurring = recurring.copy(lastGeneratedMonth = periodKey)
                vm.repository.updateRecurringExpense(updatedRecurring)

                val user = vm._currentUser.value
                val householdId = vm._currentHousehold.value?.id ?: ""
                if (householdId.isNotBlank()) {
                    try {
                        vm.firestoreSyncRepo.syncExpense(savedExpense.toFirebaseExpense(householdId, user?.uid ?: "", user?.displayName ?: "Μέλος"))
                    } catch (_: Exception) {}
                } else if (user != null) {
                    try {
                        vm.firestoreSyncRepo.syncUserExpense(user.uid, savedExpense.toFirebaseExpense(householdId, user.uid, user.displayName ?: "Χρήστης"))
                    } catch (_: Exception) {}
                }

                val fbRecurring = FirebaseRecurringExpense(
                    id = updatedRecurring.id.toString(),
                    householdId = householdId,
                    userId = user?.uid ?: "",
                    title = updatedRecurring.title,
                    amount = updatedRecurring.amount,
                    categoryId = updatedRecurring.categoryId,
                    categoryName = updatedRecurring.categoryName,
                    frequency = updatedRecurring.frequency,
                    dayOfMonth = updatedRecurring.dayOfMonth,
                    reminderHour = updatedRecurring.reminderHour,
                    reminderMinute = updatedRecurring.reminderMinute,
                    splitType = updatedRecurring.splitType,
                    memberIdsCsv = updatedRecurring.memberIdsCsv,
                    memberNamesDisplay = updatedRecurring.memberNamesDisplay,
                    note = updatedRecurring.note,
                    isActive = updatedRecurring.isActive,
                    lastGeneratedMonth = updatedRecurring.lastGeneratedMonth,
                    payerMemberId = updatedRecurring.payerMemberId,
                    payerMemberName = updatedRecurring.payerMemberName,
                    payerMemberIdsCsv = updatedRecurring.payerMemberIdsCsv,
                    payerAmountsCsv = updatedRecurring.payerAmountsCsv,
                    anchorMonth = updatedRecurring.anchorMonth
                )
                if (householdId.isNotBlank()) vm.firestoreSyncRepo.syncRecurringExpense(householdId, fbRecurring)
                else if (user != null) vm.firestoreSyncRepo.syncUserRecurringExpense(user.uid, fbRecurring)

                vm.logHouseholdActivity(
                    actionType = "RECURRING_EXECUTED",
                    targetType = "EXPENSE",
                    summaryEl = "Εκτέλεση πάγιας εντολής: ${recurring.title}",
                    summaryEn = "Executed standing order: ${recurring.title}",
                    amount = recurring.amount
                )
                val curSymbol = com.example.util.CurrencyFormatter.currentSymbol
                vm.showSnackbar(
                    Localization.trFormat(
                        vm._appLanguage.value,
                        "Executed standing order: \"%1\$s\" (%2\$s%3\$s)",
                        "Εκτελέστηκε η πάγια: «%1\$s» (%2\$s%3\$s)",
                        recurring.title,
                        String.format(Locale.US, "%.2f", recurring.amount),
                        curSymbol
                    )
                )
            } finally {
                vm.trackSyncEnd()
            }
        }
    }

    fun checkAndProcessDueRecurringExpenses(force: Boolean = false) {
        if (!vm.hasOwnerPermissions()) return
        if (vm.hasRunRecurringCheckThisSession && !force) return
        vm.hasRunRecurringCheckThisSession = true
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try {
                val householdId = vm._currentHousehold.value?.id ?: ""
                val user = vm._currentUser.value
                val newExpenses = RecurringExpenseAutoProcessor.processDueRecurringExpenses(
                    repository = vm.repository,
                    firestoreSyncRepo = vm.firestoreSyncRepo,
                    householdId = householdId,
                    user = user
                )
                if (newExpenses.isNotEmpty()) {
                    val existingIds = vm._allExpenses.value.map { it.id }.toSet()
                    val genuinelyNew = newExpenses.filter { it.id !in existingIds }
                    if (genuinelyNew.isNotEmpty()) {
                        vm._allExpenses.value = (genuinelyNew + vm._allExpenses.value).sortedWith(
                            compareByDescending<Expense> { it.timestamp }.thenByDescending { it.id }
                        )
                        val lang = Localization.Language.fromCode(vm._appLanguage.value)
                        val names = genuinelyNew.take(2).joinToString(", ") {
                            if (lang == Localization.Language.GREEK) "«${it.title}»" else "\"${it.title}\""
                        }
                        val suffix = if (genuinelyNew.size > 2) {
                            Localization.trFormat(lang, " and %1\$d more", " και άλλες %1\$d", genuinelyNew.size - 2)
                        } else ""
                        vm.showSnackbar(
                            Localization.trFormat(
                                lang,
                                "Auto-logged to expenses: %1\$s%2\$s!",
                                "Αυτόματη καταχώρηση: %1\$s%2\$s στα έξοδα!",
                                names,
                                suffix
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("OikoNomosVM", "checkAndProcessDueRecurringExpenses error: ${e.message}")
            }
        }
    }
}
