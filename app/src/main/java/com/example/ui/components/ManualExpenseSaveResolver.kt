package com.example.ui.components

import com.example.data.entity.FamilyMember
import com.example.util.Localization
import java.util.Locale

internal data class ResolvedManualExpenseSplit(
    val splitType: String,
    val finalMemberIds: List<Long>,
    val memberDisplay: String,
    val finalPayerId: Long,
    val finalPayerName: String,
    val finalPayerMemberIdsCsv: String,
    val finalPayerAmountsCsv: String
)

internal object ManualExpenseSaveResolver {

    fun resolveSplitAndPayers(
        parsedAmount: Double,
        isAllFamily: Boolean,
        selectedMemberIds: List<Long>,
        selectedPayerIds: List<Long>,
        fallbackDefaultPayer: FamilyMember?,
        isCustomPayerSplit: Boolean,
        customPayerAmounts: Map<Long, String>,
        members: List<FamilyMember>,
        lang: Localization.Language
    ): ResolvedManualExpenseSplit {
        val splitType = if (isAllFamily) "ALL_FAMILY" else "SPECIFIC_MEMBERS"
        val finalMemberIds = if (isAllFamily) members.map { it.id } else selectedMemberIds
        val memberDisplay = if (isAllFamily) {
            if (lang == Localization.Language.ENGLISH) "All" else "Όλοι"
        } else {
            val active = members.filter { finalMemberIds.contains(it.id) }
            Localization.resolveMemberGroupDisplay(active, members, lang)
        }

        val finalPayerIds = selectedPayerIds.ifEmpty {
            if (fallbackDefaultPayer != null) listOf(fallbackDefaultPayer.id) else emptyList()
        }
        val finalPayerMember = if (finalPayerIds.isNotEmpty()) members.find { it.id == finalPayerIds[0] } else null
        val finalPayerId = finalPayerMember?.id ?: 0L
        val finalPayerMemberIdsCsv = finalPayerIds.joinToString(",")

        val finalPayerAmountsCsv: String
        val finalPayerName: String
        if (finalPayerIds.size <= 1) {
            finalPayerAmountsCsv = if (finalPayerIds.isNotEmpty()) {
                "${finalPayerIds[0]}:${String.format(Locale.US, "%.2f", parsedAmount)}"
            } else {
                ""
            }
            finalPayerName = finalPayerMember?.name ?: ""
        } else if (isCustomPayerSplit) {
            finalPayerAmountsCsv = finalPayerIds.joinToString(",") { id ->
                val amt = customPayerAmounts[id]?.toDoubleOrNull() ?: (parsedAmount / finalPayerIds.size)
                String.format(Locale.US, "%d:%.2f", id, amt)
            }
            finalPayerName = finalPayerIds.mapNotNull { id -> members.find { it.id == id } }
                .joinToString(" & ") { m ->
                    val amt = customPayerAmounts[m.id]?.toDoubleOrNull() ?: (parsedAmount / finalPayerIds.size)
                    "${m.name} (${String.format(Locale.US, "%.2f", amt)} ${com.example.util.CurrencyFormatter.currentSymbol})"
                }
        } else {
            val perPerson = parsedAmount / finalPayerIds.size
            finalPayerAmountsCsv = finalPayerIds.joinToString(",") { id ->
                String.format(Locale.US, "%d:%.2f", id, perPerson)
            }
            val payerMembers = finalPayerIds.mapNotNull { id -> members.find { it.id == id } }
            finalPayerName = Localization.resolveMemberGroupDisplay(payerMembers, members, lang)
        }

        return ResolvedManualExpenseSplit(
            splitType = splitType,
            finalMemberIds = finalMemberIds,
            memberDisplay = memberDisplay,
            finalPayerId = finalPayerId,
            finalPayerName = finalPayerName,
            finalPayerMemberIdsCsv = finalPayerMemberIdsCsv,
            finalPayerAmountsCsv = finalPayerAmountsCsv
        )
    }
}
