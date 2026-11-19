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

    fun buildDepositExpenseTitle(goalTitle: String, isEnglish: Boolean): String {
        val clean = goalTitle.trim()
        return if (isEnglish) {
            if (clean.isNotBlank()) "Goal: $clean" else "Savings Goal Deposit"
        } else {
            if (clean.isNotBlank()) "Στόχος: $clean" else "Κατάθεση σε Στόχο"
        }
    }

    fun buildDepositExpenseNote(goalTitle: String, userNote: String, isEnglish: Boolean): String {
        val trimmedNote = userNote.trim()
        if (trimmedNote.isNotBlank()) return trimmedNote
        val cleanTitle = goalTitle.trim()
        return if (isEnglish) {
            if (cleanTitle.isNotBlank()) "Deposit to savings goal \"$cleanTitle\"" else "Deposit to savings goal"
        } else {
            if (cleanTitle.isNotBlank()) "Κατάθεση στον στόχο «$cleanTitle»" else "Κατάθεση σε στόχο αποταμίευσης"
        }
    }
}
