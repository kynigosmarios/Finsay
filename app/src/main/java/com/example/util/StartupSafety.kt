package com.example.util

import com.example.data.entity.Expense
import com.example.data.entity.SavingsGoal
import com.example.data.model.FirebaseExpense
import com.example.data.model.FirebaseIncome
import com.example.data.model.FirebaseSavingsGoal
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

    fun allocateNextSafeId(existingIds: Collection<Long>): Long {
        val maxId = existingIds.filter { it > 0L }.maxOrNull() ?: 0L
        return maxId + 1L
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
                exp.householdId == activeHouseholdId || exp.householdId.isBlank()
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
                inc.householdId == activeHouseholdId || inc.householdId.isBlank()
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

    data class PendingSavingsGoalMutation(
        val cloudId: String,
        val householdId: String,
        val localEntity: SavingsGoal,
        val updatedAtMs: Long
    )

    data class ResolvedSavingsGoalsResult(
        val goals: List<SavingsGoal>,
        val cloudIdByLocalId: Map<Long, String>
    )

    fun resolveAuthoritativeSavingsGoals(
        activeHouseholdId: String,
        householdGoals: List<FirebaseSavingsGoal>,
        userGoals: List<FirebaseSavingsGoal>,
        pendingLocalMutations: MutableMap<String, PendingSavingsGoalMutation>,
        pendingDeletedGoals: MutableMap<String, Pair<String, Long>>,
        nowMs: Long = System.currentTimeMillis(),
        ttlMs: Long = 15_000L
    ): ResolvedSavingsGoalsResult {
        val rawRemote = if (activeHouseholdId.isNotBlank()) {
            householdGoals.filter { sg ->
                sg.householdId == activeHouseholdId || sg.householdId.isBlank()
            }
        } else {
            userGoals
        }

        // Deduplicate remote goals by docId first
        val uniqueRemote = rawRemote.distinctBy { sg ->
            sg.id.ifBlank { "${sg.title.trim().lowercase()}_${sg.targetAmount}" }
        }

        val remoteCloudIds = uniqueRemote.mapNotNull { it.id.takeIf(String::isNotBlank) }.toSet()

        // Prune expired tombstones or tombstones for docs that are absent past settle grace window
        val delIter = pendingDeletedGoals.entries.iterator()
        while (delIter.hasNext()) {
            val entry = delIter.next()
            val (hid, ts) = entry.value
            val key = entry.key
            val expired = nowMs - ts > ttlMs
            val wrongHousehold = activeHouseholdId.isNotBlank() && hid.isNotBlank() && hid != activeHouseholdId
            if (expired || wrongHousehold) {
                delIter.remove()
            } else if (!remoteCloudIds.contains(key) && nowMs - ts > 1_500L) {
                delIter.remove()
            }
        }

        // Prune confirmed or expired local mutations
        val mutIter = pendingLocalMutations.entries.iterator()
        while (mutIter.hasNext()) {
            val entry = mutIter.next()
            val mut = entry.value
            val expired = nowMs - mut.updatedAtMs > ttlMs
            val wrongHousehold = mut.householdId != activeHouseholdId
            if (expired || wrongHousehold) {
                mutIter.remove()
                continue
            }
            val matchingRemote = uniqueRemote.find { it.id == mut.cloudId }
            if (matchingRemote != null &&
                matchingRemote.title == mut.localEntity.title &&
                kotlin.math.abs(matchingRemote.targetAmount - mut.localEntity.targetAmount) < 0.005 &&
                kotlin.math.abs(matchingRemote.currentAmount - mut.localEntity.currentAmount) < 0.005 &&
                matchingRemote.deadlineDate == mut.localEntity.deadlineDate &&
                matchingRemote.note == mut.localEntity.note
            ) {
                mutIter.remove()
            }
        }

        val activeDeletedKeys = pendingDeletedGoals.entries
            .filter { (_, v) -> v.first == activeHouseholdId || v.first.isBlank() }
            .map { it.key }
            .toSet()

        val activeMutations = pendingLocalMutations.values
            .filter { it.householdId == activeHouseholdId }
            .associateBy { it.cloudId }

        val mergedEntries = linkedMapOf<String, SavingsGoal>()
        val usedIds = HashSet<Long>((uniqueRemote.size + activeMutations.size) * 2 + 4)
        var nextAlloc = 1L
        fun allocId(preferred: Long): Long {
            if (preferred > 0L && usedIds.add(preferred)) return preferred
            while (usedIds.contains(nextAlloc)) nextAlloc++
            usedIds.add(nextAlloc)
            return nextAlloc
        }

        uniqueRemote.forEach { sg ->
            val cloudId = sg.id.ifBlank { safePositiveIdHash("${sg.title}_${sg.targetAmount}").toString() }
            val numericPreferred = cloudId.trim().toLongOrNull()?.takeIf { it > 0L } ?: safePositiveIdHash(cloudId)
            val deleteMatch = activeDeletedKeys.contains(cloudId) || activeDeletedKeys.contains(numericPreferred.toString())
            if (deleteMatch) return@forEach

            val localOverride = activeMutations[cloudId]
            val entity = if (localOverride != null) {
                val preferred = localOverride.localEntity.id.takeIf { it > 0L } ?: numericPreferred
                localOverride.localEntity.copy(id = allocId(preferred))
            } else {
                SavingsGoal(
                    id = allocId(numericPreferred),
                    title = sg.title,
                    targetAmount = sg.targetAmount,
                    currentAmount = sg.currentAmount,
                    deadlineDate = sg.deadlineDate,
                    iconKey = sg.iconKey,
                    colorHex = sg.colorHex,
                    note = sg.note
                )
            }
            mergedEntries[cloudId] = entity
        }

        // Include newly created in-flight goals that haven't appeared in the snapshot yet
        activeMutations.forEach { (cloudId, mut) ->
            if (!mergedEntries.containsKey(cloudId) && !activeDeletedKeys.contains(cloudId)) {
                val preferred = mut.localEntity.id.takeIf { it > 0L } ?: safePositiveIdHash(cloudId)
                mergedEntries[cloudId] = mut.localEntity.copy(id = allocId(preferred))
            }
        }

        val cloudMap = HashMap<Long, String>(mergedEntries.size * 2)
        mergedEntries.forEach { (cloudId, entity) ->
            cloudMap[entity.id] = cloudId
        }

        val sortedGoals = mergedEntries.values.sortedWith(
            compareBy<SavingsGoal> { it.title.trim().lowercase() }.thenBy { it.id }
        )

        return ResolvedSavingsGoalsResult(
            goals = sortedGoals,
            cloudIdByLocalId = cloudMap
        )
    }
}

