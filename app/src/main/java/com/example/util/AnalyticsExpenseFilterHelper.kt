package com.example.util

import androidx.compose.runtime.Immutable
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import com.example.data.entity.getPayerShares
import com.example.data.entity.resolveReconciledBeneficiaries
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Immutable
data class AnalyticsSpecialFilter(
    val badgeLabel: String = "",
    val exactMemberId: Long? = null,
    val payerMemberId: Long? = null,
    val debtorMemberId: Long? = null,
    val roleGroupFilter: String? = null,
    val exactDayKey: String? = null,
    val incomeCategory: String? = null
)

@Immutable
data class HomeDrillDownRequest(
    val periodKey: String,
    val typeFilter: String = "EXPENSE",
    val memberId: Long? = null,
    val categoryId: Long? = null,
    val searchQuery: String = "",
    val specialFilter: AnalyticsSpecialFilter? = null
)

object AnalyticsExpenseFilterHelper {

    fun resolveMembersForExpense(exp: Expense, members: List<FamilyMember>): List<FamilyMember> {
        val isExplicitAllFamily = exp.splitType == "ALL_FAMILY" &&
            (exp.memberNamesDisplay.isBlank() || exp.memberNamesDisplay.equals("Κοινό Οικογένειας", ignoreCase = true))

        if (isExplicitAllFamily) {
            val combinedText = "${exp.title} ${exp.note}"
            val normCombined = GreekVoiceParser.normalizeGreek(combinedText)
            val byMention = members.filter { m ->
                val nameNorm = GreekVoiceParser.normalizeGreek(m.name)
                val keywords = m.voiceKeywords.split(",")
                    .map { GreekVoiceParser.normalizeGreek(it.trim()) }
                    .filter { it.isNotBlank() }
                (keywords + nameNorm).any { kw -> kw.isNotBlank() && GreekVoiceParser.containsWord(normCombined, kw) }
            }
            if (byMention.isNotEmpty()) return byMention

            val catNorm = GreekVoiceParser.normalizeGreek(exp.categoryName)
            if (catNorm.contains("παιδ") || catNorm.contains("σχολ") || catNorm.contains("φροντιστηρ")) {
                val kids = members.filter { it.role.equals("Παιδί", ignoreCase = true) }
                if (kids.isNotEmpty()) return kids
            }
            return emptyList()
        }

        val reconciledBeneficiaries = exp.resolveReconciledBeneficiaries(members)
        if (reconciledBeneficiaries.isNotEmpty() && reconciledBeneficiaries.size < members.size) {
            return reconciledBeneficiaries
        }
        if (reconciledBeneficiaries.isNotEmpty() && exp.splitType == "SPECIFIC_MEMBERS") {
            return reconciledBeneficiaries
        }

        val combinedText = "${exp.title} ${exp.note}"
        val normCombined = GreekVoiceParser.normalizeGreek(combinedText)
        val byMention = members.filter { m ->
            val nameNorm = GreekVoiceParser.normalizeGreek(m.name)
            val keywords = m.voiceKeywords.split(",")
                .map { GreekVoiceParser.normalizeGreek(it.trim()) }
                .filter { it.isNotBlank() }
            (keywords + nameNorm).any { kw -> kw.isNotBlank() && GreekVoiceParser.containsWord(normCombined, kw) }
        }
        if (byMention.isNotEmpty()) return byMention

        val catNorm = GreekVoiceParser.normalizeGreek(exp.categoryName)
        if (catNorm.contains("παιδ") || catNorm.contains("σχολ") || catNorm.contains("φροντιστηρ")) {
            val kids = members.filter { it.role.equals("Παιδί", ignoreCase = true) }
            if (kids.isNotEmpty()) return kids
        }

        return emptyList()
    }

    fun resolveSettleUpBeneficiaries(exp: Expense, activeMembers: List<FamilyMember>): List<FamilyMember> {
        val splitType = exp.splitType
        if (splitType == "ALL_FAMILY" || splitType == "ALL") {
            return activeMembers
        }
        val reconciled = exp.resolveReconciledBeneficiaries(activeMembers)
        if (reconciled.isNotEmpty()) return reconciled
        return activeMembers
    }

    fun matchesCategory(
        exp: Expense,
        categoryId: Long,
        categories: List<ExpenseCategory>
    ): Boolean {
        if (exp.categoryId == categoryId) return true
        val targetCat = categories.find { it.id == categoryId } ?: return false
        if (exp.categoryName.isBlank()) return false
        val targetNorm = GreekVoiceParser.normalizeGreek(targetCat.name.trim().lowercase())
        val expNorm = GreekVoiceParser.normalizeGreek(exp.categoryName.trim().lowercase())
        return expNorm == targetNorm
    }

    fun matchesPayer(
        exp: Expense,
        payerMemberId: Long,
        members: List<FamilyMember>
    ): Boolean {
        if (members.isEmpty()) return exp.payerMemberId == payerMemberId
        val shares = exp.getPayerShares(members)
        return shares.any { share ->
            val resolved = members.find { it.id == share.memberId }
                ?: members.find { it.name.equals(share.memberName, ignoreCase = true) }
                ?: members.firstOrNull { it.role.equals("Γονέας", ignoreCase = true) }
                ?: members.firstOrNull()
            resolved?.id == payerMemberId
        }
    }

    fun matchesSpecialExpenseFilter(
        exp: Expense,
        special: AnalyticsSpecialFilter,
        members: List<FamilyMember>
    ): Boolean {
        if (special.exactDayKey != null) {
            val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            if (dayFmt.format(Date(exp.timestamp)) != special.exactDayKey) return false
        }
        if (special.payerMemberId != null) {
            if (!matchesPayer(exp, special.payerMemberId, members)) return false
        }
        if (special.debtorMemberId != null) {
            val beneficiaries = resolveSettleUpBeneficiaries(exp, members)
            if (beneficiaries.none { it.id == special.debtorMemberId }) return false
        }
        if (special.exactMemberId != null) {
            val assigned = resolveMembersForExpense(exp, members)
            if (special.exactMemberId == -1L) {
                if (assigned.isNotEmpty()) return false
            } else {
                if (assigned.none { it.id == special.exactMemberId }) return false
            }
        }
        if (special.roleGroupFilter != null) {
            val assigned = resolveMembersForExpense(exp, members)
            val matchRole = when (special.roleGroupFilter) {
                "KIDS" -> assigned.any { it.role.equals("Παιδί", ignoreCase = true) }
                "PARENTS" -> assigned.isEmpty() || assigned.any { !it.role.equals("Παιδί", ignoreCase = true) }
                else -> true
            }
            if (!matchRole) return false
        }
        return true
    }

    fun matchesSpecialIncomeFilter(
        inc: Income,
        special: AnalyticsSpecialFilter,
        members: List<FamilyMember>
    ): Boolean {
        if (special.exactDayKey != null) {
            val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            if (dayFmt.format(Date(inc.timestamp)) != special.exactDayKey) return false
        }
        if (special.exactMemberId != null) {
            if (special.exactMemberId == -1L) {
                if (members.any { it.id == inc.memberId }) return false
            } else {
                if (inc.memberId != special.exactMemberId) return false
            }
        }
        if (special.incomeCategory != null) {
            if (!inc.category.equals(special.incomeCategory, ignoreCase = true)) return false
        }
        return true
    }
}
