package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import com.example.data.entity.toFirebaseExpense
import com.example.data.model.FirebaseIncome
import com.example.util.BankNotificationHolder
import com.example.util.BankNotificationParser
import com.example.util.BankNotificationSystemManager
import com.example.util.DefaultPayerHelper
import com.example.util.GreekVoiceParser
import com.example.util.MerchantCategoryLearner
import com.example.util.PendingBankTransaction
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class OikoNomosTransactionDelegate(
    private val vm: OikoNomosViewModel
) {
    private val tag = "OikoNomosTx"

    fun openVoiceInputDialog() {
        val currentUsage = vm.prefs.getInt(vm.getMonthlyVoiceUsageKey(), 0)
        vm._monthlyVoiceUsage.value = currentUsage
        if (!vm.isCurrentUserPro() && currentUsage >= 15) {
            vm.showPremiumPaywall("voice_limit")
            vm.showSnackbar(
                if (vm._appLanguage.value == "EN")
                    "Monthly voice entry limit reached (15/15). Upgrade to Pro for unlimited entries!"
                else
                    "Συμπληρώσατε το όριο 15 φωνητικών καταχωρήσεων για αυτόν τον μήνα. Αναβαθμίστε σε Pro για απεριόριστες!"
            )
            return
        }
        vm._showVoiceInputDialog.value = true
    }

    fun suggestCategoryForMerchant(merchantOrText: String, merchantAfm: String = ""): ExpenseCategory? {
        val categories = vm._allCategories.value
        val householdId = vm._currentHousehold.value?.id ?: vm.getOrEnsureHouseholdId()
        val learned = MerchantCategoryLearner.suggest(
            context = vm.getApplication(),
            householdId = householdId,
            merchantOrTitle = merchantOrText,
            merchantAfm = merchantAfm,
            availableCategories = categories
        ) ?: return null
        return categories.find { it.id == learned.categoryId }
            ?: categories.find { it.name.equals(learned.categoryName, ignoreCase = true) }
    }

    fun learnCategoryForMerchant(
        merchantOrText: String,
        categoryId: Long,
        categoryName: String,
        merchantAfm: String = ""
    ) {
        if ((merchantOrText.isBlank() && merchantAfm.isBlank()) || categoryId <= 0L || categoryName.isBlank()) return
        val householdId = vm._currentHousehold.value?.id ?: vm.getOrEnsureHouseholdId()
        MerchantCategoryLearner.learn(
            context = vm.getApplication(),
            householdId = householdId,
            merchantOrTitle = merchantOrText,
            categoryId = categoryId,
            categoryName = categoryName,
            merchantAfm = merchantAfm
        )
    }

    fun confirmBankTransaction(tx: PendingBankTransaction) {
        BankNotificationHolder.removeTransaction(tx.id)
        BankNotificationSystemManager.cancelNotification(vm.getApplication(), tx.id)

        val categories = vm._allCategories.value
        val matchedCategory = BankNotificationParser.categorizeMerchant(tx.merchantName, tx.rawText, categories)
            ?: suggestCategoryForMerchant(tx.merchantName.ifBlank { tx.rawText })
        val defaultPayers = vm.resolveDefaultPayerMembers()
        val resolvedDefault = DefaultPayerHelper.buildEqualPayerFields(defaultPayers, tx.amount, fallbackLabel = "")

        val prefilled = Expense(
            id = 0L,
            title = tx.merchantName,
            amount = tx.amount,
            categoryId = matchedCategory?.id ?: 0L,
            categoryName = matchedCategory?.name ?: "",
            timestamp = tx.timestamp,
            splitType = "ALL_FAMILY",
            memberIdsCsv = "",
            memberNamesDisplay = "Κοινό Οικογένειας",
            note = "Αυτόματος εντοπισμός (${tx.bankName})",
            payerMemberId = resolvedDefault.primaryId,
            payerMemberName = resolvedDefault.displayName,
            payerMemberIdsCsv = resolvedDefault.idsCsv,
            payerAmountsCsv = resolvedDefault.amountsCsv
        )
        vm.openManualExpenseDialog(prefilled)
    }

    fun dismissBankTransaction(tx: PendingBankTransaction) {
        BankNotificationHolder.removeTransaction(tx.id)
        BankNotificationSystemManager.cancelNotification(vm.getApplication(), tx.id)
    }

    fun processVoiceInput(spokenText: String) {
        val currentUsage = vm.prefs.getInt(vm.getMonthlyVoiceUsageKey(), 0)
        vm._monthlyVoiceUsage.value = currentUsage
        if (!vm.isCurrentUserPro() && currentUsage >= 15) {
            vm.showPremiumPaywall("voice_limit")
            vm.showSnackbar(
                if (vm._appLanguage.value == "EN")
                    "Monthly voice entry limit reached (15/15). Upgrade to Pro for unlimited entries!"
                else
                    "Συμπληρώσατε το όριο 15 φωνητικών καταχωρήσεων για αυτόν τον μήνα. Αναβαθμίστε σε Pro για απεριόριστες!"
            )
            return
        }
        if (spokenText.isBlank()) {
            vm.showSnackbar(if (vm._appLanguage.value == "EN") "No speech recognized." else "Δεν αναγνωρίστηκε ομιλία.")
            return
        }
        val categories = vm._allCategories.value
        val members = vm._allMembers.value
        val defaultPayers = vm.resolveDefaultPayerMembers()
        val parsed = GreekVoiceParser.parse(
            prompt = spokenText,
            categories = categories,
            allMembers = members,
            preferredDefaultPayers = defaultPayers
        )
        vm._pendingParsedExpense.value = parsed
    }

    fun cancelPendingExpense() {
        vm._pendingParsedExpense.value = null
    }

    fun confirmPendingExpense() {
        val pending = vm._pendingParsedExpense.value ?: return
        confirmVoiceExpense(
            title = pending.title,
            amount = pending.amount,
            categoryId = pending.categoryId,
            categoryName = pending.categoryName,
            timestamp = pending.timestamp,
            splitType = pending.splitType,
            memberIds = pending.memberIds,
            memberNamesDisplay = pending.memberNamesDisplay,
            originalPrompt = pending.originalPrompt,
            payerMemberId = pending.payerMemberId,
            payerMemberName = pending.payerMemberName,
            payerMemberIdsCsv = pending.payerMemberIdsCsv,
            payerAmountsCsv = pending.payerAmountsCsv
        )
    }

    fun editPendingExpense() {
        val pending = vm._pendingParsedExpense.value ?: return
        vm._pendingParsedExpense.value = null
        val membersCount = pending.memberIds.size.coerceAtLeast(1)
        val prefilled = Expense(
            id = 0L,
            title = pending.title,
            amount = pending.amount,
            categoryId = pending.categoryId,
            categoryName = pending.categoryName,
            timestamp = pending.timestamp,
            splitType = pending.splitType,
            memberIdsCsv = pending.memberIds.joinToString(","),
            memberNamesDisplay = pending.memberNamesDisplay,
            splitAmountPerPerson = pending.amount / membersCount,
            originalVoicePrompt = pending.originalPrompt,
            note = pending.note,
            payerMemberId = pending.payerMemberId,
            payerMemberName = pending.payerMemberName,
            payerMemberIdsCsv = pending.payerMemberIdsCsv,
            payerAmountsCsv = pending.payerAmountsCsv
        )
        vm.openManualExpenseDialog(prefilled)
    }

    fun confirmVoiceExpense(
        title: String,
        amount: Double,
        categoryId: Long,
        categoryName: String,
        timestamp: Long,
        splitType: String,
        memberIds: List<Long>,
        memberNamesDisplay: String,
        originalPrompt: String,
        payerMemberId: Long = 0L,
        payerMemberName: String = "",
        payerMemberIdsCsv: String = "",
        payerAmountsCsv: String = ""
    ) {
        vm._pendingParsedExpense.value = null
        val user = vm._currentUser.value
        val householdId = vm.getOrEnsureHouseholdId(user)
        val cloudId = UUID.randomUUID().toString()
        val finalSplitMembersCount = if (memberIds.isNotEmpty()) memberIds.size else 1
        val resolvedDefaults = DefaultPayerHelper.buildEqualPayerFields(vm.resolveDefaultPayerMembers(), amount)
        val useDefaults = payerMemberId <= 0L && payerMemberIdsCsv.isBlank() && payerMemberName.isBlank()
        val finalPayerId = if (useDefaults) resolvedDefaults.primaryId else payerMemberId
        val finalPayerName = if (useDefaults) resolvedDefaults.displayName else payerMemberName.ifBlank { resolvedDefaults.displayName }
        val finalPayerIdsCsv = if (useDefaults) resolvedDefaults.idsCsv else payerMemberIdsCsv.ifBlank { if (finalPayerId > 0L) "$finalPayerId" else "" }
        val finalPayerAmountsCsv = if (useDefaults) resolvedDefaults.amountsCsv else payerAmountsCsv

        val newExpense = Expense(
            cloudId = cloudId,
            title = title,
            amount = amount,
            categoryId = categoryId,
            categoryName = categoryName,
            timestamp = timestamp,
            splitType = splitType,
            memberIdsCsv = memberIds.joinToString(","),
            memberNamesDisplay = memberNamesDisplay,
            splitAmountPerPerson = amount / finalSplitMembersCount,
            originalVoicePrompt = originalPrompt,
            payerMemberId = finalPayerId,
            payerMemberName = finalPayerName,
            payerMemberIdsCsv = finalPayerIdsCsv,
            payerAmountsCsv = finalPayerAmountsCsv
        )

        val newCount = vm.prefs.getInt(vm.getMonthlyVoiceUsageKey(), 0) + 1
        vm.prefs.edit()
            .putInt(vm.getMonthlyVoiceUsageKey(), newCount)
            .putBoolean("has_entered_any_expense", true)
            .apply()
        vm._monthlyVoiceUsage.value = newCount
        vm._isInitialExpensesLoading.value = false

        learnCategoryForMerchant(title, categoryId, categoryName)

        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                val insertedId = vm.repository.insertExpense(newExpense)
                val saved = newExpense.copy(id = insertedId, cloudId = cloudId)
                vm.pendingLocalExpenseIds[cloudId] = System.currentTimeMillis()
                vm._allExpenses.value = (listOf(saved) + vm._allExpenses.value).distinctBy { it.id }.sortedWith(
                    compareByDescending<Expense> { it.timestamp }.thenByDescending { it.id }
                )

                val creatorName = vm._userProfile.value?.displayName?.ifBlank { null }
                    ?: user?.displayName?.ifBlank { null }
                    ?: "Μέλος"
                val fbExpense = saved.toFirebaseExpense(householdId, user?.uid ?: "", creatorName)
                if (householdId.isNotBlank()) {
                    vm.firestoreSyncRepo.syncExpense(fbExpense)
                } else if (user != null) {
                    vm.firestoreSyncRepo.syncUserExpense(user.uid, fbExpense)
                }

                val label = title.ifBlank { categoryName }
                vm.logHouseholdActivity(
                    actionType = "EXPENSE_ADDED",
                    targetType = "EXPENSE",
                    summaryEl = "Νέο έξοδο: $label",
                    summaryEn = "New expense: $label",
                    amount = amount,
                    householdIdOverride = householdId
                )
                val isEn = vm._appLanguage.value == "EN"
                vm.showSnackbar(
                    if (isEn) "Added: $label (${String.format(Locale.US, "%.2f", amount)}€)"
                    else "Καταχωρήθηκε: $label (${String.format(Locale.US, "%.2f", amount)}€)"
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(tag, "confirmVoiceExpense sync warning: ${e.message}")
            } finally {
                vm.trackSyncEnd()
            }
        }
    }

    fun saveExpense(
        id: Long = 0,
        title: String,
        amount: Double,
        categoryId: Long,
        categoryName: String,
        timestamp: Long,
        splitType: String,
        memberIds: List<Long>,
        memberNamesDisplay: String,
        note: String,
        payerMemberId: Long = 0L,
        payerMemberName: String = "",
        payerMemberIdsCsv: String = "",
        payerAmountsCsv: String = ""
    ) {
        val existing = if (id > 0L) vm._allExpenses.value.find { it.id == id } else null
        val user = vm._currentUser.value
        val householdId = vm.getOrEnsureHouseholdId(user)
        val cloudId = existing?.cloudId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val finalSplitCount = if (memberIds.isNotEmpty()) memberIds.size else 1
        val resolvedDefaults = DefaultPayerHelper.buildEqualPayerFields(vm.resolveDefaultPayerMembers(), amount)
        val useDefaults = payerMemberId <= 0L && payerMemberIdsCsv.isBlank() && payerMemberName.isBlank()
        val finalPayerId = if (useDefaults) resolvedDefaults.primaryId else payerMemberId
        val finalPayerName = if (useDefaults) resolvedDefaults.displayName else payerMemberName.ifBlank { resolvedDefaults.displayName }
        val finalPayerIdsCsv = if (useDefaults) resolvedDefaults.idsCsv else payerMemberIdsCsv.ifBlank { if (finalPayerId > 0L) "$finalPayerId" else "" }
        val finalPayerAmountsCsv = if (useDefaults) resolvedDefaults.amountsCsv else payerAmountsCsv

        val targetExpense = Expense(
            id = id,
            cloudId = cloudId,
            title = title,
            amount = amount,
            categoryId = categoryId,
            categoryName = categoryName,
            timestamp = timestamp,
            splitType = splitType,
            memberIdsCsv = memberIds.joinToString(","),
            memberNamesDisplay = memberNamesDisplay,
            splitAmountPerPerson = amount / finalSplitCount,
            originalVoicePrompt = existing?.originalVoicePrompt ?: "",
            note = note,
            payerMemberId = finalPayerId,
            payerMemberName = finalPayerName,
            payerMemberIdsCsv = finalPayerIdsCsv,
            payerAmountsCsv = finalPayerAmountsCsv
        )

        vm.prefs.edit().putBoolean("has_entered_any_expense", true).apply()
        vm._isInitialExpensesLoading.value = false
        learnCategoryForMerchant(title, categoryId, categoryName)
        vm.dismissManualExpenseDialog()

        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                val savedId = if (id == 0L) {
                    vm.repository.insertExpense(targetExpense)
                } else {
                    vm.repository.saveOrUpdateExpense(targetExpense)
                }
                val saved = targetExpense.copy(id = savedId, cloudId = cloudId)
                vm.pendingLocalExpenseIds[cloudId] = System.currentTimeMillis()
                vm._allExpenses.value = (listOf(saved) + vm._allExpenses.value.filter { it.id != saved.id && it.cloudId != cloudId })
                    .sortedWith(compareByDescending<Expense> { it.timestamp }.thenByDescending { it.id })

                if (existing != null && kotlin.math.abs(amount - existing.amount) >= 0.005) {
                    vm.adjustLinkedGoalForExpenseDelta(cloudId, amount - existing.amount)
                }

                val creatorName = vm._userProfile.value?.displayName?.ifBlank { null }
                    ?: user?.displayName?.ifBlank { null }
                    ?: "Μέλος"
                val fbExpense = saved.toFirebaseExpense(householdId, user?.uid ?: "", creatorName)
                if (householdId.isNotBlank()) {
                    vm.firestoreSyncRepo.syncExpense(fbExpense)
                } else if (user != null) {
                    vm.firestoreSyncRepo.syncUserExpense(user.uid, fbExpense)
                }

                val label = title.ifBlank { categoryName }
                vm.logHouseholdActivity(
                    actionType = if (id == 0L) "EXPENSE_ADDED" else "EXPENSE_UPDATED",
                    targetType = "EXPENSE",
                    summaryEl = if (id == 0L) "Νέο έξοδο: $label" else "Ενημέρωση εξόδου: $label",
                    summaryEn = if (id == 0L) "New expense: $label" else "Updated expense: $label",
                    amount = amount,
                    householdIdOverride = householdId
                )
                val isEn = vm._appLanguage.value == "EN"
                vm.showSnackbar(
                    if (id == 0L) {
                        if (isEn) "Expense \"$label\" added!" else "Το έξοδο «$label» καταχωρήθηκε!"
                    } else {
                        if (isEn) "Expense \"$label\" updated!" else "Το έξοδο «$label» ενημερώθηκε!"
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(tag, "saveExpense sync warning: ${e.message}")
            } finally {
                vm.trackSyncEnd()
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        val user = vm._currentUser.value
        val householdId = vm._currentHousehold.value?.id.orEmpty()
        val resolvedCloudId = expense.cloudId.ifBlank { "exp_${expense.id}" }

        vm._allExpenses.value = vm._allExpenses.value.filter { it.id != expense.id && it.cloudId != resolvedCloudId }
        vm.pendingLocalExpenseIds.remove(resolvedCloudId)
        vm.adjustLinkedGoalForExpenseDelta(resolvedCloudId, -expense.amount)

        startUndoCountdown(
            PendingUndoDeletion(
                title = expense.title.ifBlank { expense.categoryName },
                amount = expense.amount,
                isIncome = false,
                deletedExpense = expense.copy(cloudId = resolvedCloudId),
                remainingSeconds = 5
            )
        )

        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                vm.repository.deleteExpenseById(expense.id)
                if (householdId.isNotBlank()) {
                    vm.firestoreSyncRepo.deleteExpense(householdId, resolvedCloudId)
                }
                if (user != null) {
                    vm.firestoreSyncRepo.deleteUserExpense(user.uid, resolvedCloudId)
                }
                val label = expense.title.ifBlank { expense.categoryName }
                vm.logHouseholdActivity(
                    actionType = "EXPENSE_DELETED",
                    targetType = "EXPENSE",
                    summaryEl = "Διαγραφή εξόδου: $label",
                    summaryEn = "Deleted expense: $label",
                    amount = expense.amount,
                    householdIdOverride = householdId
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(tag, "deleteExpense error: ${e.message}")
            } finally {
                vm.trackSyncEnd()
            }
        }
    }

    fun saveIncome(
        id: Long = 0,
        title: String,
        amount: Double,
        memberId: Long,
        memberName: String,
        category: String,
        timestamp: Long,
        note: String
    ) {
        val existing = if (id > 0L) vm._allIncomes.value.find { it.id == id } else null
        val user = vm._currentUser.value
        val householdId = vm.getOrEnsureHouseholdId(user)
        val cloudId = existing?.cloudId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val resolvedDefaultPayer = vm.resolveDefaultPayerMember()
        val finalMemberId = if (memberId > 0L) memberId else (resolvedDefaultPayer?.id ?: 0L)
        val finalMemberName = memberName.ifBlank { resolvedDefaultPayer?.name ?: "Όλοι" }

        val targetIncome = Income(
            id = id,
            cloudId = cloudId,
            title = title,
            amount = amount,
            memberId = finalMemberId,
            memberName = finalMemberName,
            category = category,
            timestamp = timestamp,
            note = note
        )
        vm.dismissManualIncomeDialog()

        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                val savedId = if (id == 0L) {
                    vm.repository.insertIncome(targetIncome)
                } else {
                    vm.repository.updateIncome(targetIncome)
                    id
                }
                val saved = targetIncome.copy(id = savedId, cloudId = cloudId)
                vm.pendingLocalIncomeIds[cloudId] = System.currentTimeMillis()
                vm._allIncomes.value = (listOf(saved) + vm._allIncomes.value.filter { it.id != saved.id && it.cloudId != cloudId })
                    .sortedWith(compareByDescending<Income> { it.timestamp }.thenByDescending { it.id })

                val creatorName = vm._userProfile.value?.displayName?.ifBlank { null }
                    ?: user?.displayName?.ifBlank { null }
                    ?: "Μέλος"
                val fbIncome = FirebaseIncome(
                    id = cloudId,
                    householdId = householdId,
                    title = title,
                    amount = amount,
                    memberId = finalMemberId,
                    memberName = finalMemberName,
                    category = category,
                    timestamp = timestamp,
                    note = note,
                    createdByUid = user?.uid ?: "",
                    createdByName = creatorName
                )
                if (householdId.isNotBlank()) {
                    vm.firestoreSyncRepo.syncIncome(fbIncome)
                } else if (user != null) {
                    vm.firestoreSyncRepo.syncUserIncome(user.uid, fbIncome)
                }

                val label = title.ifBlank { category }
                vm.logHouseholdActivity(
                    actionType = if (id == 0L) "INCOME_ADDED" else "INCOME_UPDATED",
                    targetType = "INCOME",
                    summaryEl = if (id == 0L) "Νέο έσοδο: $label" else "Ενημέρωση εσόδου: $label",
                    summaryEn = if (id == 0L) "New income: $label" else "Updated income: $label",
                    amount = amount,
                    householdIdOverride = householdId
                )
                val isEn = vm._appLanguage.value == "EN"
                vm.showSnackbar(
                    if (id == 0L) {
                        if (isEn) "Income \"$label\" added!" else "Το έσοδο «$label» καταχωρήθηκε!"
                    } else {
                        if (isEn) "Income \"$label\" updated!" else "Το έσοδο «$label» ενημερώθηκε!"
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(tag, "saveIncome error: ${e.message}")
            } finally {
                vm.trackSyncEnd()
            }
        }
    }

    fun deleteIncome(income: Income) {
        val user = vm._currentUser.value
        val householdId = vm._currentHousehold.value?.id.orEmpty()
        val resolvedCloudId = income.cloudId.ifBlank { "inc_${income.id}" }

        vm._allIncomes.value = vm._allIncomes.value.filter { it.id != income.id && it.cloudId != resolvedCloudId }
        vm.pendingLocalIncomeIds.remove(resolvedCloudId)

        startUndoCountdown(
            PendingUndoDeletion(
                title = income.title.ifBlank { income.category },
                amount = income.amount,
                isIncome = true,
                deletedIncome = income.copy(cloudId = resolvedCloudId),
                remainingSeconds = 5
            )
        )

        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                vm.repository.deleteIncomeById(income.id)
                if (householdId.isNotBlank()) {
                    vm.firestoreSyncRepo.deleteIncome(householdId, resolvedCloudId)
                }
                if (user != null) {
                    vm.firestoreSyncRepo.deleteUserIncome(user.uid, resolvedCloudId)
                }
                val label = income.title.ifBlank { income.category }
                vm.logHouseholdActivity(
                    actionType = "INCOME_DELETED",
                    targetType = "INCOME",
                    summaryEl = "Διαγραφή εσόδου: $label",
                    summaryEn = "Deleted income: $label",
                    amount = income.amount,
                    householdIdOverride = householdId
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(tag, "deleteIncome error: ${e.message}")
            } finally {
                vm.trackSyncEnd()
            }
        }
    }

    // 1.2: 5-second Undo Banner Countdown & Restore
    private fun startUndoCountdown(pending: PendingUndoDeletion) {
        vm.undoCountdownJob?.cancel()
        vm._pendingUndoDeletion.value = pending
        vm.undoCountdownJob = vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            for (sec in 4 downTo 1) {
                delay(1000L)
                val cur = vm._pendingUndoDeletion.value ?: return@launch
                if (cur.createdAt != pending.createdAt) return@launch
                vm._pendingUndoDeletion.value = cur.copy(remainingSeconds = sec)
            }
            delay(1000L)
            val cur = vm._pendingUndoDeletion.value
            if (cur?.createdAt == pending.createdAt) {
                vm._pendingUndoDeletion.value = null
            }
        }
    }

    fun dismissUndoDeletion() {
        vm.undoCountdownJob?.cancel()
        vm._pendingUndoDeletion.value = null
    }

    fun undoPendingDeletion() {
        vm.undoCountdownJob?.cancel()
        val pending = vm._pendingUndoDeletion.value ?: return
        vm._pendingUndoDeletion.value = null

        val user = vm._currentUser.value
        val householdId = vm.getOrEnsureHouseholdId(user)
        val creatorName = vm._userProfile.value?.displayName?.ifBlank { null }
            ?: user?.displayName?.ifBlank { null }
            ?: "Μέλος"
        val isEn = vm._appLanguage.value == "EN"

        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                if (!pending.isIncome && pending.deletedExpense != null) {
                    val exp = pending.deletedExpense
                    val docId = exp.cloudId.ifBlank { UUID.randomUUID().toString() }
                    val newId = vm.repository.insertExpense(exp.copy(id = 0L, cloudId = docId))
                    val restored = exp.copy(id = newId, cloudId = docId)
                    vm.pendingLocalExpenseIds[docId] = System.currentTimeMillis()
                    vm._allExpenses.value = (listOf(restored) + vm._allExpenses.value.filter { it.id != restored.id && it.cloudId != docId })
                        .sortedWith(compareByDescending<Expense> { it.timestamp }.thenByDescending { it.id })
                    vm.adjustLinkedGoalForExpenseDelta(docId, restored.amount)

                    val fbExpense = restored.toFirebaseExpense(householdId, user?.uid ?: "", creatorName)
                    if (householdId.isNotBlank()) vm.firestoreSyncRepo.syncExpense(fbExpense)
                    else if (user != null) vm.firestoreSyncRepo.syncUserExpense(user.uid, fbExpense)

                    vm.logHouseholdActivity(
                        actionType = "EXPENSE_RESTORED",
                        targetType = "EXPENSE",
                        summaryEl = "Επαναφορά εξόδου: ${pending.title}",
                        summaryEn = "Restored expense: ${pending.title}",
                        amount = exp.amount,
                        householdIdOverride = householdId
                    )
                } else if (pending.isIncome && pending.deletedIncome != null) {
                    val inc = pending.deletedIncome
                    val docId = inc.cloudId.ifBlank { UUID.randomUUID().toString() }
                    val newId = vm.repository.insertIncome(inc.copy(id = 0L, cloudId = docId))
                    val restored = inc.copy(id = newId, cloudId = docId)
                    vm.pendingLocalIncomeIds[docId] = System.currentTimeMillis()
                    vm._allIncomes.value = (listOf(restored) + vm._allIncomes.value.filter { it.id != restored.id && it.cloudId != docId })
                        .sortedWith(compareByDescending<Income> { it.timestamp }.thenByDescending { it.id })

                    val fbIncome = FirebaseIncome(
                        id = docId,
                        householdId = householdId,
                        title = restored.title,
                        amount = restored.amount,
                        memberId = restored.memberId,
                        memberName = restored.memberName,
                        category = restored.category,
                        timestamp = restored.timestamp,
                        note = restored.note,
                        createdByUid = user?.uid ?: "",
                        createdByName = creatorName
                    )
                    if (householdId.isNotBlank()) vm.firestoreSyncRepo.syncIncome(fbIncome)
                    else if (user != null) vm.firestoreSyncRepo.syncUserIncome(user.uid, fbIncome)

                    vm.logHouseholdActivity(
                        actionType = "INCOME_RESTORED",
                        targetType = "INCOME",
                        summaryEl = "Επαναφορά εσόδου: ${pending.title}",
                        summaryEn = "Restored income: ${pending.title}",
                        amount = inc.amount,
                        householdIdOverride = householdId
                    )
                }
                vm.showSnackbar(
                    if (isEn) "Restored: ${pending.title}" else "Επαναφέρθηκε: ${pending.title}"
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(tag, "undoPendingDeletion error: ${e.message}")
            } finally {
                vm.trackSyncEnd()
            }
        }
    }

    // 1.3: Instant Settle-Up Transfer Recording
    fun recordSettlementTransfer(
        fromMember: FamilyMember,
        toMember: FamilyMember,
        amount: Double,
        targetPeriodTimestamp: Long
    ) {
        if (amount <= 0.0) return
        val user = vm._currentUser.value
        val householdId = vm.getOrEnsureHouseholdId(user)
        val isEn = vm._appLanguage.value == "EN"
        val categories = vm._allCategories.value
        val otherCat = categories.find {
            it.name.contains("Χρωστούμενα", ignoreCase = true) ||
                it.name.contains("Εξόφληση", ignoreCase = true) ||
                it.iconKey == "debt"
        } ?: categories.find {
            it.name.contains("Άλλο", ignoreCase = true) || it.iconKey == "other"
        } ?: categories.lastOrNull()

        val cloudId = UUID.randomUUID().toString()
        val settlementTitle = if (isEn) {
            "Settlement: ${fromMember.name} → ${toMember.name}"
        } else {
            "Εξόφληση: ${fromMember.name} → ${toMember.name}"
        }
        val settlementNote = if (isEn) "Debt settlement transfer" else "Εξόφληση οφειλής"
        val payerCsv = "${fromMember.id}:${String.format(Locale.US, "%.2f", amount)}"

        val settlementExpense = Expense(
            cloudId = cloudId,
            title = settlementTitle,
            amount = amount,
            categoryId = otherCat?.id ?: 7L,
            categoryName = otherCat?.name ?: "Άλλο",
            timestamp = targetPeriodTimestamp,
            splitType = "SPECIFIC_MEMBERS",
            memberIdsCsv = "${toMember.id}",
            memberNamesDisplay = toMember.name,
            splitAmountPerPerson = amount,
            note = settlementNote,
            payerMemberId = fromMember.id,
            payerMemberName = fromMember.name,
            payerMemberIdsCsv = "${fromMember.id}",
            payerAmountsCsv = payerCsv
        )

        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                val insertedId = vm.repository.insertExpense(settlementExpense)
                val saved = settlementExpense.copy(id = insertedId, cloudId = cloudId)
                vm.pendingLocalExpenseIds[cloudId] = System.currentTimeMillis()
                vm._allExpenses.value = (listOf(saved) + vm._allExpenses.value)
                    .sortedWith(compareByDescending<Expense> { it.timestamp }.thenByDescending { it.id })

                val creatorName = vm._userProfile.value?.displayName?.ifBlank { null }
                    ?: user?.displayName?.ifBlank { null }
                    ?: "Μέλος"
                val fbExpense = saved.toFirebaseExpense(householdId, user?.uid ?: "", creatorName)
                if (householdId.isNotBlank()) vm.firestoreSyncRepo.syncExpense(fbExpense)
                else if (user != null) vm.firestoreSyncRepo.syncUserExpense(user.uid, fbExpense)

                vm.logHouseholdActivity(
                    actionType = "SETTLED",
                    targetType = "SETTLEMENT",
                    summaryEl = "Εξόφληση: ${fromMember.name} → ${toMember.name}",
                    summaryEn = "Settled: ${fromMember.name} → ${toMember.name}",
                    amount = amount,
                    householdIdOverride = householdId
                )
                vm.showSnackbar(
                    if (isEn) {
                        "Settled ${String.format(Locale.US, "%.2f", amount)}€ (${fromMember.name} → ${toMember.name})"
                    } else {
                        "Καταχωρήθηκε εξόφληση ${String.format(Locale.US, "%.2f", amount)}€ (${fromMember.name} → ${toMember.name})"
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(tag, "recordSettlementTransfer error: ${e.message}")
            } finally {
                vm.trackSyncEnd()
            }
        }
    }
}
