package com.example.util

import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import java.util.UUID

/**
 * Links Savings Goal deposits with household Expense entries so that every
 * monetary contribution to a Savings Goal is transparently reflected on the
 * Home screen (Expenses) and in Statistics (Analytics), with clean two-way sync.
 */
object SavingsGoalExpenseLinkHelper {
    const val GOAL_DEPOSIT_PREFIX = "goaldep_"
    private const val GOAL_ID_SEPARATOR = "__"

    const val DEFAULT_SAVINGS_CATEGORY_NAME_EL = "Αποταμίευση & Στόχοι"
    const val DEFAULT_SAVINGS_CATEGORY_NAME_EN = "Savings & Goals"
    const val DEFAULT_SAVINGS_CATEGORY_ICON = "savings"
    const val DEFAULT_SAVINGS_CATEGORY_COLOR = 0xFF059669L
    const val DEFAULT_SAVINGS_CATEGORY_KEYWORDS =
        "αποταμίευση, στόχος, στόχοι, κουμπαράς, κατάθεση, savings, goal, goals"

    fun buildGoalDepositCloudId(
        goalCloudId: String,
        uniqueSuffix: String = UUID.randomUUID().toString().replace("-", "").take(12)
    ): String {
        val cleanGoalId = goalCloudId.trim().replace(GOAL_ID_SEPARATOR, "_")
        val cleanSuffix = uniqueSuffix.trim().ifBlank { "dep" }
        return "$GOAL_DEPOSIT_PREFIX$cleanGoalId$GOAL_ID_SEPARATOR$cleanSuffix"
    }

    fun extractGoalCloudId(expenseCloudId: String?): String? {
        val raw = expenseCloudId?.trim().orEmpty()
        if (!raw.startsWith(GOAL_DEPOSIT_PREFIX)) return null
        val rest = raw.removePrefix(GOAL_DEPOSIT_PREFIX)
        val sepIdx = rest.lastIndexOf(GOAL_ID_SEPARATOR)
        if (sepIdx <= 0) return null
        return rest.substring(0, sepIdx).takeIf { it.isNotBlank() }
    }

    fun isGoalDepositExpense(expense: Expense): Boolean {
        return extractGoalCloudId(expense.cloudId) != null
    }

    fun findSavingsCategory(categories: List<ExpenseCategory>): ExpenseCategory? {
        val byIcon = categories.firstOrNull { it.iconKey.equals(DEFAULT_SAVINGS_CATEGORY_ICON, ignoreCase = true) }
        if (byIcon != null) return byIcon

        return categories.firstOrNull { cat ->
            val norm = GreekVoiceParser.normalizeGreek(cat.name.trim().lowercase())
            norm.contains("αποταμιευ") ||
                norm.contains("στοχ") ||
                norm.contains("κουμπαρ") ||
                norm.contains("savings") ||
                norm.contains("goal")
        }
    }

    fun defaultSavingsCategoryName(lang: Localization.Language): String =
        Localization.tr(lang, DEFAULT_SAVINGS_CATEGORY_NAME_EN, DEFAULT_SAVINGS_CATEGORY_NAME_EL)

    fun buildDepositExpenseTitle(goalTitle: String, lang: Localization.Language): String {
        val clean = goalTitle.trim()
        return if (clean.isNotBlank()) {
            Localization.trFormat(lang, "Goal: %1\$s", "Στόχος: %1\$s", clean)
        } else {
            Localization.tr(lang, "Savings Goal Deposit", "Κατάθεση σε Στόχο")
        }
    }

    fun buildDepositExpenseTitle(goalTitle: String, isEnglish: Boolean): String =
        buildDepositExpenseTitle(goalTitle, if (isEnglish) Localization.Language.ENGLISH else Localization.Language.GREEK)

    fun buildDepositExpenseNote(goalTitle: String, userNote: String, lang: Localization.Language): String {
        val trimmedNote = userNote.trim()
        if (trimmedNote.isNotBlank()) return trimmedNote
        val cleanTitle = goalTitle.trim()
        return if (cleanTitle.isNotBlank()) {
            Localization.trFormat(lang, "Deposit to savings goal \"%1\$s\"", "Κατάθεση στον στόχο «%1\$s»", cleanTitle)
        } else {
            Localization.tr(lang, "Deposit to savings goal", "Κατάθεση σε στόχο αποταμίευσης")
        }
    }

    fun buildDepositExpenseNote(goalTitle: String, userNote: String, isEnglish: Boolean): String =
        buildDepositExpenseNote(goalTitle, userNote, if (isEnglish) Localization.Language.ENGLISH else Localization.Language.GREEK)
}
