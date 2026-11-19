package com.example.util

import com.example.data.entity.FamilyMember
import com.example.data.model.Household
import java.util.Locale

data class ResolvedPayerFields(
    val primaryId: Long,
    val displayName: String,
    val idsCsv: String,
    val amountsCsv: String
)

/**
 * Pure helper for per-user multi-select default payers and strict owner-only household RBAC checks.
 */
object DefaultPayerHelper {

    fun parseMemberIdsCsv(csv: String?): List<Long> {
        if (csv.isNullOrBlank()) return emptyList()
        return csv.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .filter { it > 0L }
            .distinct()
    }

    fun formatMemberIdsCsv(memberIds: Collection<Long>): String {
        return memberIds
            .filter { it > 0L }
            .distinct()
            .joinToString(",")
    }

    /**
     * Resolves explicitly assigned default payers (0, 1, 2, or more) for [uid] in [household].
     * Priority:
     * 1. Explicit entry in `household.memberDefaultPayers[uid]` (empty string means explicitly cleared).
     * 2. Local SharedPreferences override (if present before cloud echo).
     * 3. Legacy `FamilyMember.linkedUserUid == uid` fallback for backward compatibility.
     */
    fun resolveAssignedDefaultPayers(
        uid: String,
        household: Household?,
        members: List<FamilyMember>,
        localOverrideCsv: String? = null
    ): List<FamilyMember> {
        val cleanUid = uid.trim()
        if (cleanUid.isBlank() || members.isEmpty()) return emptyList()

        val membersById = members.associateBy { it.id }

        if (household != null && household.memberDefaultPayers.containsKey(cleanUid)) {
            val ids = parseMemberIdsCsv(household.memberDefaultPayers[cleanUid])
            return ids.mapNotNull { membersById[it] }
        }

        if (localOverrideCsv != null) {
            val ids = parseMemberIdsCsv(localOverrideCsv)
            return ids.mapNotNull { membersById[it] }
        }

        return members.filter { it.linkedUserUid == cleanUid }
    }

    /**
     * Resolves effective default payers for expense creation flows.
     * Returns all explicitly assigned default payers (1, 2, or more), or falls back to
     * the household default/first member if none are explicitly assigned.
     */
    fun resolveEffectiveDefaultPayers(
        uid: String,
        household: Household?,
        members: List<FamilyMember>,
        localOverrideCsv: String? = null
    ): List<FamilyMember> {
        val assigned = resolveAssignedDefaultPayers(uid, household, members, localOverrideCsv)
        if (assigned.isNotEmpty()) return assigned
        val fallback = members.firstOrNull { it.isDefault } ?: members.firstOrNull()
        return listOfNotNull(fallback)
    }

    fun buildEqualPayerFields(
        payers: List<FamilyMember>,
        amount: Double,
        fallbackLabel: String = "Οικογένεια"
    ): ResolvedPayerFields {
        if (payers.isEmpty()) {
            return ResolvedPayerFields(
                primaryId = 0L,
                displayName = fallbackLabel,
                idsCsv = "",
                amountsCsv = ""
            )
        }
        val primary = payers.first()
        val displayName = if (payers.size >= 2) {
            payers.joinToString(" & ") { it.name }
        } else {
            primary.name
        }
        val idsCsv = payers.joinToString(",") { it.id.toString() }
        val amountsCsv = if (amount > 0.0) {
            val equalShare = amount / payers.size
            payers.joinToString(",") { "${it.id}:${String.format(Locale.US, "%.2f", equalShare)}" }
        } else {
            ""
        }
        return ResolvedPayerFields(
            primaryId = primary.id,
            displayName = displayName,
            idsCsv = idsCsv,
            amountsCsv = amountsCsv
        )
    }

    /**
     * Any household member (CREATOR, EDITOR, or REGISTAR) may assign their OWN default payer(s).
     * Only the CREATOR (`household.ownerId == actorUid`) may assign default payers for another user.
     */
    fun canAssignDefaultPayers(
        household: Household?,
        actorUid: String,
        targetUid: String
    ): Boolean {
        val cleanActor = actorUid.trim()
        val cleanTarget = targetUid.trim()
        if (cleanActor.isBlank() || cleanTarget.isBlank()) return false
        if (cleanActor == cleanTarget) {
            if (household == null) return true
            return household.ownerId == cleanActor || cleanActor in household.memberUids
        }
        return household != null && household.ownerId == cleanActor
    }

    /**
     * Strictly owner-only: EDITOR and REGISTAR cannot change roles (own or others) or remove users (including themselves).
     */
    fun canModifyRolesOrRemoveUsers(household: Household?, actorUid: String): Boolean {
        val cleanActor = actorUid.trim()
        return household != null && cleanActor.isNotBlank() && household.ownerId == cleanActor
    }
}
