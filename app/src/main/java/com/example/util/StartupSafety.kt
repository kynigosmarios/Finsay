package com.example.util

import com.example.data.entity.Expense
import com.example.data.model.FirebaseExpense
import com.example.data.model.FirebaseIncome
import com.example.data.model.Household
import com.example.data.model.HouseholdRole

object StartupSafety {

    fun safePositiveIdHash(rawId: String): Long {
        return (rawId.hashCode().toLong() and 0x7FFFFFFFL).coerceAtLeast(1L)
    }

    fun deduplicateExpenseIds(expenses: List<Expense>): List<Expense> {
        if (expenses.isEmpty()) return emptyList()
        val usedIds = HashSet<Long>(expenses.size * 2)
        var nextAvailableId = 1L
        return expenses.map { exp ->
            val chosenId = if (exp.id > 0L && usedIds.add(exp.id)) {
                exp.id
            } else {
                while (usedIds.contains(nextAvailableId)) {
                    nextAvailableId++
                }
                usedIds.add(nextAvailableId)
                nextAvailableId
            }
            if (chosenId != exp.id) exp.copy(id = chosenId) else exp
        }
    }

    fun resolveHouseholdRole(
        household: Household?,
        userUid: String,
        userEmail: String,
        isMarios: Boolean,
        mariosDefaultIds: Set<String>
    ): HouseholdRole {
        if (household == null) {
            return if (isMarios) HouseholdRole.CREATOR else HouseholdRole.REGISTAR
        }
        if (isMarios && (household.ownerId.isBlank() || household.ownerId == userUid || mariosDefaultIds.contains(household.id))) {
            return HouseholdRole.CREATOR
        }
        if (household.ownerId.isNotBlank() && household.ownerId == userUid) {
            return HouseholdRole.CREATOR
        }
        val uidRole = household.memberRoles[userUid]
        if (!uidRole.isNullOrBlank()) {
            return HouseholdRole.fromRaw(uidRole, isCreator = false)
        }
        val cleanEmail = userEmail.trim()
        if (cleanEmail.isNotBlank()) {
            val emailRole = household.memberRoles[cleanEmail.lowercase()] ?: household.memberRoles[cleanEmail]
            if (!emailRole.isNullOrBlank()) {
                return HouseholdRole.fromRaw(emailRole, isCreator = false)
            }
        }
        return HouseholdRole.REGISTAR
    }

    fun canSafelyWriteRemoteStructure(
        targetHousehold: Household?,
        userUid: String,
        userEmail: String,
        isMarios: Boolean,
        mariosDefaultIds: Set<String>
    ): Boolean {
        if (targetHousehold == null) return false
        return resolveHouseholdRole(
            household = targetHousehold,
            userUid = userUid,
            userEmail = userEmail,
            isMarios = isMarios,
            mariosDefaultIds = mariosDefaultIds
        ).canManageMembersAndCategories
    }

    fun isActualUserSwitch(activeUidInPrefs: String?, currentUid: String?): Boolean {
        if (currentUid.isNullOrBlank()) return true
        if (activeUidInPrefs.isNullOrBlank()) return false
        return activeUidInPrefs != currentUid
    }

    fun isHouseholdAllowedForUser(
        candidateHid: String,
        isMarios: Boolean,
        mariosDefaultIds: Set<String>,
        explicitlyKnownHids: Set<String>
    ): Boolean {
        if (candidateHid.isBlank()) return false
        if (isMarios) return true
        if (!mariosDefaultIds.contains(candidateHid)) return true
        return explicitlyKnownHids.contains(candidateHid)
    }

    /**
     * When an active household exists, `/households/{hid}/expenses` is the single shared source of truth.
     * Personal `/users/{uid}/expenses` mirror items MUST NOT be merged into an active household, because
     * deletions or edits performed by another household member only mutate `/households/{hid}/expenses`
     * and cannot touch another user's private `/users/{otherUid}` subcollection.
     */
    fun resolveAuthoritativeExpenses(
        activeHouseholdId: String,
        householdExpenses: List<FirebaseExpense>,
        userExpenses: List<FirebaseExpense>,
        totalHouseholdCount: Int
    ): List<FirebaseExpense> {
        val source = if (activeHouseholdId.isNotBlank()) {
            householdExpenses.filter { exp ->
                exp.householdId == activeHouseholdId || (exp.householdId.isBlank() && totalHouseholdCount <= 1)
            }
        } else {
            userExpenses
        }
        return source
            .distinctBy { it.id.ifBlank { "${it.title}_${it.timestamp}_${it.amount}" } }
            .sortedWith(
                compareByDescending<FirebaseExpense> { it.timestamp }
                    .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    .thenByDescending { it.id }
            )
    }

    /**
     * Single-source-of-truth resolution for household incomes (`/households/{hid}/incomes`),
     * preventing deleted or edited household incomes from being resurrected by private user mirrors.
     */
    fun resolveAuthoritativeIncomes(
        activeHouseholdId: String,
        householdIncomes: List<FirebaseIncome>,
        userIncomes: List<FirebaseIncome>,
        totalHouseholdCount: Int
    ): List<FirebaseIncome> {
        val source = if (activeHouseholdId.isNotBlank()) {
            householdIncomes.filter { inc ->
                inc.householdId == activeHouseholdId || (inc.householdId.isBlank() && totalHouseholdCount <= 1)
            }
        } else {
            userIncomes
        }
        return source
            .distinctBy { it.id.ifBlank { "${it.title}_${it.timestamp}_${it.amount}" } }
            .sortedWith(
                compareByDescending<FirebaseIncome> { it.timestamp }
                    .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    .thenByDescending { it.id }
            )
    }

    fun findStaleUserMirrorIds(
        activeHouseholdId: String,
        authoritativeIds: Set<String>,
        userMirrorEntries: List<Pair<String, String>>,
        pendingLocalIds: Set<String>
    ): List<String> {
        if (activeHouseholdId.isBlank()) return emptyList()
        return userMirrorEntries
            .asSequence()
            .filter { (id, hid) ->
                id.isNotBlank() &&
                    hid == activeHouseholdId &&
                    !authoritativeIds.contains(id) &&
                    !pendingLocalIds.contains(id)
            }
            .map { it.first }
            .distinct()
            .toList()
    }

    fun pruneExpiredPendingIds(
        pendingIds: MutableMap<String, Long>,
        confirmedRemoteIds: Set<String>,
        nowMs: Long,
        ttlMs: Long = 15_000L
    ): Set<String> {
        confirmedRemoteIds.forEach { pendingIds.remove(it) }
        val iterator = pendingIds.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (nowMs - entry.value > ttlMs) {
                iterator.remove()
            }
        }
        return pendingIds.keys.toSet()
    }
}

