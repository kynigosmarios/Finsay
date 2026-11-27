package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.FirebaseExpense
import com.example.util.Localization

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cloudId: String = "",
    val title: String,
    val amount: Double,
    val categoryId: Long,
    val categoryName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val splitType: String = "ALL_FAMILY", // "ALL_FAMILY" or "SPECIFIC_MEMBERS"
    val memberIdsCsv: String = "", // e.g. "1,2"
    val memberNamesDisplay: String = "Κοινό Οικογένειας", // e.g. "Κοινό Οικογένειας" ή ονόματα μελών
    val splitAmountPerPerson: Double = 0.0,
    val originalVoicePrompt: String = "",
    val note: String = "",
    val payerMemberId: Long = 0L,
    val payerMemberName: String = "",
    val payerMemberIdsCsv: String = "",
    val payerAmountsCsv: String = ""
)

fun Expense.toFirebaseExpense(
    householdId: String,
    userId: String,
    userName: String
): FirebaseExpense {
    val docId = if (cloudId.isNotBlank()) cloudId else if (id > 0) "exp_$id" else java.util.UUID.randomUUID().toString()
    return FirebaseExpense(
        id = docId,
        householdId = householdId,
        title = title,
        amount = amount,
        categoryId = categoryId,
        categoryName = categoryName,
        timestamp = timestamp,
        splitType = splitType,
        memberIdsCsv = memberIdsCsv,
        memberNamesDisplay = memberNamesDisplay,
        note = note,
        payerMemberId = payerMemberId,
        payerMemberName = payerMemberName,
        payerMemberIdsCsv = payerMemberIdsCsv,
        payerAmountsCsv = payerAmountsCsv,
        createdByUid = userId,
        createdByName = userName
    )
}

data class PayerShare(
    val memberId: Long,
    val memberName: String,
    val amount: Double
)

internal fun resolveMembersFromDisplayLabel(
    rawLabel: String,
    members: List<FamilyMember>
): List<FamilyMember> {
    if (members.isEmpty()) return emptyList()
    val clean = rawLabel.replace(Regex("\\s*\\([^)]*\\)"), "").trim()
    if (clean.isBlank()) return emptyList()

    if (Localization.isAllMembersLabel(clean)) {
        return members
    }

    if (Localization.isPluralRoleDisplay(clean)) {
        val roleKey = Localization.canonicalRoleKey(clean)
        val byRole = members.filter { Localization.canonicalRoleKey(it.role) == roleKey }
        if (byRole.isNotEmpty()) return byRole
    }

    val parts = clean
        .split(Regex("[,&+/]+|\\s+και\\s+|\\s+and\\s+", RegexOption.IGNORE_CASE))
        .map { it.trim() }
        .filter { it.isNotBlank() }

    if (parts.isEmpty()) return emptyList()

    val matched = mutableListOf<FamilyMember>()
    val seenIds = HashSet<Long>()
    for (part in parts) {
        if (Localization.isPluralRoleDisplay(part)) {
            val roleKey = Localization.canonicalRoleKey(part)
            members.filter { Localization.canonicalRoleKey(it.role) == roleKey }.forEach { m ->
                if (seenIds.add(m.id)) matched.add(m)
            }
            continue
        }
        val normPart = com.example.util.GreekVoiceParser.normalizeGreek(part.lowercase())
        val found = members.find { it.name.equals(part, ignoreCase = true) }
            ?: members.find { com.example.util.GreekVoiceParser.normalizeGreek(it.name.lowercase()) == normPart }
            ?: members.find {
                val normName = com.example.util.GreekVoiceParser.normalizeGreek(it.name.lowercase())
                normName.isNotBlank() && normPart.length >= 2 && (normName.contains(normPart) || normPart.contains(normName))
            }
        if (found != null && seenIds.add(found.id)) {
            matched.add(found)
        }
    }
    return matched
}

/**
 * Reconciles payer member IDs against current household [members] without silently dropping
 * any payer whose numeric ID drifted/changed or whose legacy record only stored a single primary ID
 * alongside a collective/multi-name label (e.g., "Γονείς", "Parents", "Μάριος & Βασιλική").
 */
fun Expense.resolveReconciledPayers(members: List<FamilyMember>): List<FamilyMember> {
    if (members.isEmpty()) return emptyList()

    val csvIds = when {
        payerAmountsCsv.isNotBlank() -> payerAmountsCsv.split(",")
            .mapNotNull { it.split(":").firstOrNull()?.trim()?.toLongOrNull() }
            .filter { it > 0L }
            .distinct()
        payerMemberIdsCsv.isNotBlank() -> payerMemberIdsCsv.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .filter { it > 0L }
            .distinct()
        payerMemberId > 0L -> listOf(payerMemberId)
        else -> emptyList()
    }

    val matchedById = csvIds.mapNotNull { id -> members.find { it.id == id } }.distinctBy { it.id }
    val matchedByLabel = resolveMembersFromDisplayLabel(payerMemberName, members)

    val hasUnresolvedIds = csvIds.isNotEmpty() && matchedById.size < csvIds.size
    if (hasUnresolvedIds && matchedByLabel.isNotEmpty()) {
        // Reconcile missing/shifted ID(s) using the preserved payer label (e.g. "Γονείς" or "Μάριος & Βασιλική")
        return (matchedById + matchedByLabel).distinctBy { it.id }
    }

    if (matchedById.isNotEmpty()) {
        // Legacy single-ID fabrication safeguard:
        // If only 1 ID was stored (e.g. payerMemberIdsCsv="1" or "") while payerMemberName explicitly
        // records multiple payers or a collective group ("Γονείς", "Μάριος & Βασιλική") containing that member,
        // expand to all members represented by the label instead of silently collapsing to 1 person.
        val hasExplicitMultiAmounts = payerAmountsCsv.split(",").count { it.contains(":") } >= 2
        if (csvIds.size == 1 && !hasExplicitMultiAmounts && matchedByLabel.size > 1 &&
            matchedByLabel.any { it.id == matchedById.first().id }
        ) {
            return matchedByLabel
        }
        return matchedById
    }

    if (matchedByLabel.isNotEmpty()) return matchedByLabel

    if (payerMemberId > 0L) {
        members.find { it.id == payerMemberId }?.let { return listOf(it) }
    }
    return emptyList()
}

/**
 * Reconciles beneficiary ("ποιον αφορά") member IDs against current household [members]
 * without silently dropping any beneficiary whose numeric ID drifted/changed.
 */
fun Expense.resolveReconciledBeneficiaries(members: List<FamilyMember>): List<FamilyMember> {
    if (members.isEmpty()) return emptyList()
    val raw = memberNamesDisplay.replace(Regex("\\s*\\([^)]*\\)"), "").trim()
    val isAll = splitType == "ALL_FAMILY" || splitType == "ALL" ||
        Localization.isAllMembersLabel(raw)
    if (isAll) return members

    val csvIds = if (memberIdsCsv.isNotBlank()) {
        memberIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }.filter { it > 0L }.distinct()
    } else emptyList()

    val matchedById = csvIds.mapNotNull { id -> members.find { it.id == id } }.distinctBy { it.id }
    val matchedByLabel = resolveMembersFromDisplayLabel(memberNamesDisplay, members)

    val hasUnresolvedIds = csvIds.isNotEmpty() && matchedById.size < csvIds.size
    if (hasUnresolvedIds && matchedByLabel.isNotEmpty()) {
        return (matchedById + matchedByLabel).distinctBy { it.id }
    }

    if (matchedById.isNotEmpty()) {
        if (csvIds.size == 1 && matchedByLabel.size > 1 &&
            matchedByLabel.any { it.id == matchedById.first().id }
        ) {
            return matchedByLabel
        }
        return matchedById
    }

    return matchedByLabel
}

fun Expense.getPayerShares(members: List<FamilyMember> = emptyList()): List<PayerShare> {
    // Strip amount annotations in parentheses first, e.g. "(από 20,00 €)" or "(30,00 €)"
    // so that decimal commas inside currency numbers do not corrupt name splitting!
    val cleanPayerString = payerMemberName.replace(Regex("\\([^)]*\\)"), "").trim()
    val individualNames = cleanPayerString
        .split(Regex("[&,/]+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }

    val labelMembers = resolveMembersFromDisplayLabel(payerMemberName, members)

    // 1. If explicit custom amounts are stored in payerAmountsCsv (e.g. "1:30.00,2:10.00")
    if (payerAmountsCsv.isNotBlank()) {
        val parsed = payerAmountsCsv.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val id = parts[0].trim().toLongOrNull()
                val amt = parts[1].trim().toDoubleOrNull()
                if (id != null && amt != null) Pair(id, amt) else null
            } else null
        }
        if (parsed.isNotEmpty()) {
            // Safeguard: if a legacy single-entry payerAmountsCsv ("1:20.00") conflicts with a multi-member
            // collective label ("Γονείς", "Μάριος & Βασιλική") containing that member, split across reconciled payers.
            if (parsed.size == 1 && labelMembers.size > 1 &&
                labelMembers.any { it.id == parsed.first().first }
            ) {
                val totalAmt = if (amount > 0.0) amount else parsed.first().second
                val equalAmt = totalAmt / labelMembers.size
                return labelMembers.map { m -> PayerShare(memberId = m.id, memberName = m.name, amount = equalAmt) }
            }

            // Map any unresolved/shifted IDs in payerAmountsCsv to the remaining unmatched members from labelMembers
            val directlyMatchedIds = parsed.mapNotNull { (id, _) -> members.find { it.id == id }?.id }.toSet()
            val unmatchedLabelQueue = ArrayDeque(labelMembers.filter { !directlyMatchedIds.contains(it.id) })

            return parsed.mapIndexed { index, (id, amt) ->
                val directMember = members.find { it.id == id }
                val resolvedMember = directMember
                    ?: if (index < individualNames.size) members.find { it.name.equals(individualNames[index], ignoreCase = true) } else null
                    ?: unmatchedLabelQueue.removeFirstOrNull()
                val resolvedId = resolvedMember?.id ?: id
                val resolvedName = resolvedMember?.name
                    ?: if (index < individualNames.size) individualNames[index]
                    else if (id == payerMemberId && cleanPayerString.isNotBlank() && !cleanPayerString.contains("&")) cleanPayerString
                    else "Μέλος #$id"
                PayerShare(memberId = resolvedId, memberName = resolvedName, amount = amt)
            }
        }
    }

    // 2. Reconciled payers (handles multi-ID CSV, shifted/orphaned IDs, and collective labels like "Γονείς")
    val reconciled = resolveReconciledPayers(members)
    if (reconciled.isNotEmpty()) {
        val shareAmt = if (amount > 0.0) amount / reconciled.size else 0.0
        return reconciled.map { m -> PayerShare(memberId = m.id, memberName = m.name, amount = shareAmt) }
    }

    // 3. Fallback when members list is empty: parse raw IDs or multiple names
    if (payerMemberIdsCsv.isNotBlank()) {
        val ids = payerMemberIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
        if (ids.isNotEmpty()) {
            val shareAmt = if (amount > 0.0) amount / ids.size else 0.0
            return ids.mapIndexed { index, id ->
                val name = if (index < individualNames.size) individualNames[index]
                else if (id == payerMemberId && cleanPayerString.isNotBlank() && !cleanPayerString.contains("&")) cleanPayerString
                else "Μέλος #$id"
                PayerShare(memberId = id, memberName = name, amount = shareAmt)
            }
        }
    }

    if (individualNames.size > 1) {
        val shareAmt = if (amount > 0.0) amount / individualNames.size else 0.0
        return individualNames.mapIndexed { index, name ->
            val member = members.find { it.name.equals(name, ignoreCase = true) }
            val id = member?.id ?: (index + 1L)
            PayerShare(memberId = id, memberName = member?.name ?: name, amount = shareAmt)
        }
    }

    // 4. Fallback to single payer
    val singlePayer = members.find { it.id == payerMemberId }
        ?: if (cleanPayerString.isNotBlank()) members.find { it.name.equals(cleanPayerString, ignoreCase = true) } else null
        ?: members.firstOrNull { it.role.equals("Γονέας", ignoreCase = true) }
        ?: members.firstOrNull()

    return if (singlePayer != null) {
        listOf(PayerShare(memberId = singlePayer.id, memberName = singlePayer.name, amount = amount))
    } else {
        listOf(PayerShare(memberId = payerMemberId, memberName = cleanPayerString.ifBlank { "Όλοι" }, amount = amount))
    }
}

fun Expense.hasMultiplePayers(members: List<FamilyMember> = emptyList()): Boolean {
    if (members.isNotEmpty() && resolveReconciledPayers(members).size > 1) return true
    if (payerMemberIdsCsv.isNotBlank() && payerMemberIdsCsv.split(",").filter { it.isNotBlank() }.size > 1) return true
    if (payerAmountsCsv.isNotBlank() && payerAmountsCsv.split(",").filter { it.isNotBlank() }.size > 1) return true
    if (payerMemberName.contains("&") || payerMemberName.contains(",")) return true
    if (Localization.isPluralRoleDisplay(payerMemberName.replace(Regex("\\s*\\([^)]*\\)"), "").trim())) return true
    return false
}

fun Expense.getPayerDisplay(members: List<FamilyMember> = emptyList()): String {
    val shares = getPayerShares(members)
    if (shares.isEmpty()) {
        return payerMemberName.ifBlank { "Όλοι" }
    }
    if (shares.size == 1) {
        return shares[0].memberName
    }
    // Check if shares are virtually equal
    val firstAmt = shares[0].amount
    val areEqual = shares.all { Math.abs(it.amount - firstAmt) < 0.01 }
    val curSymbol = com.example.util.CurrencyFormatter.currentSymbol
    return if (areEqual) {
        // e.g. "Μαρία & Γιάννης (από 20,00 €)"
        "${shares.joinToString(" & ") { it.memberName }} (από ${String.format(java.util.Locale.US, "%.2f", firstAmt)} $curSymbol)"
    } else {
        // e.g. "Μαρία (30,00 €) & Γιάννης (10,00 €)"
        shares.joinToString(" & ") { "${it.memberName} (${String.format(java.util.Locale.US, "%.2f", it.amount)} $curSymbol)" }
    }
}

fun getMemberInitials(name: String): String {
    val clean = name.trim()
    if (clean.isBlank()) return ""
    if (clean.equals("Οικογένεια", ignoreCase = true) || clean.equals("Family", ignoreCase = true) ||
        clean.equals("Όλοι", ignoreCase = true) || clean.equals("All", ignoreCase = true)
    ) {
        return if (clean.equals("Family", ignoreCase = true) || clean.equals("All", ignoreCase = true)) "All" else "Όλοι"
    }
    val parts = clean.split(Regex("[\\s&+,/]+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> ""
        parts.size == 1 -> "${parts[0].take(1).uppercase()}."
        else -> "${parts[0].take(1).uppercase()}.${parts[1].take(1).uppercase()}."
    }
}

fun Expense.getPayerInitialsDisplay(members: List<FamilyMember> = emptyList()): String {
    val shares = getPayerShares(members)
    if (shares.isEmpty()) {
        val raw = payerMemberName.ifBlank { "Όλοι" }
        return getMemberInitials(raw)
    }
    if (shares.size == 1) {
        return getMemberInitials(shares[0].memberName)
    }
    return shares.joinToString(" & ") { getMemberInitials(it.memberName) }
}

fun Expense.getBeneficiaryInitialsDisplay(members: List<FamilyMember> = emptyList()): String? {
    if (splitType == "ALL_FAMILY") {
        return "Όλοι"
    }
    val raw = memberNamesDisplay.trim()
    if (raw.isBlank()) return null
    val names = raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
    if (names.isEmpty()) return null
    return names.joinToString(", ") { getMemberInitials(it) }
}

fun Expense.getPayerCategoryOrNamesDisplay(
    members: List<FamilyMember> = emptyList(),
    lang: Localization.Language = Localization.Language.GREEK
): String {
    // Strip any existing parenthesis like "(από 1,50€)", "(από 2.00 €)", or "(...)"
    val cleanRawPayer = payerMemberName.replace(Regex("\\s*\\([^)]*\\)"), "").trim()

    // 1. Explicit "All" collective labels
    if (Localization.isAllMembersLabel(cleanRawPayer)) {
        return Localization.allMembersWord(lang)
    }

    // 2. Reconcile payer members without ever silently dropping shifted/missing IDs
    val matchedPayers = resolveReconciledPayers(members)

    // 3. Resolved members -> apply exact role-group / all / individual-names rules
    if (matchedPayers.isNotEmpty()) {
        return Localization.resolveMemberGroupDisplay(matchedPayers, members, lang)
    }

    // 4. Fallback for legacy expenses storing only a plural role label without member IDs
    Localization.translateCollectiveRoleLabel(cleanRawPayer, lang)?.let { return it }

    return cleanRawPayer.ifBlank {
        Localization.allMembersWord(lang)
    }
}

fun Expense.getBeneficiaryCategoryOrNamesDisplay(
    members: List<FamilyMember> = emptyList(),
    lang: Localization.Language = Localization.Language.GREEK
): String {
    val raw = memberNamesDisplay.replace(Regex("\\s*\\([^)]*\\)"), "").trim()

    // 1. Explicit all-members keywords
    if (splitType == "ALL_FAMILY" || splitType == "ALL" || Localization.isAllMembersLabel(raw)) {
        return Localization.allMembersWord(lang)
    }

    // 2. Reconcile beneficiary members without ever silently dropping shifted/missing IDs
    val matchedMembers = resolveReconciledBeneficiaries(members)

    // 3. Resolved members -> apply exact role-group / all / individual-names rules
    if (matchedMembers.isNotEmpty()) {
        return Localization.resolveMemberGroupDisplay(matchedMembers, members, lang)
    }

    // 4. Fallback for legacy expenses storing only a plural role label without member IDs
    Localization.translateCollectiveRoleLabel(raw, lang)?.let { return it }

    // Fallback if matchedMembers was empty
    if (raw.isNotBlank()) {
        return raw
    }

    return Localization.tr(lang, "Personal", "Ατομικό")
}


