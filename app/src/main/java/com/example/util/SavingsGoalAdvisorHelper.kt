package com.example.util

import com.example.data.entity.Expense
import com.example.data.entity.Income
import com.example.data.entity.RecurringExpense
import com.example.data.entity.SavingsGoal
import java.util.Calendar
import kotlin.math.ceil

data class SavingsGoalPlanItem(
    val goal: SavingsGoal,
    val remainingAmount: Double,
    val progress: Float,
    val isCompleted: Boolean,
    val suggestedMonthlyContribution: Double,
    val estimatedMonthsToTarget: Int?
)

data class SavingsGoalsCashflowSummary(
    val currentMonthIncome: Double,
    val currentMonthExpenses: Double,
    val currentMonthRawBalance: Double,
    val upcomingRecurringThisMonth: Double,
    val availableForSavings: Double,
    val totalGoalTarget: Double,
    val totalGoalSaved: Double,
    val totalGoalRemaining: Double,
    val overallGoalProgress: Float,
    val planItems: List<SavingsGoalPlanItem>
) {
    val hasCashflowData: Boolean
        get() = currentMonthIncome > 0.0 || currentMonthExpenses > 0.0
}

object SavingsGoalAdvisorHelper {

    fun calculateSummary(
        allExpenses: List<Expense>,
        allIncomes: List<Income>,
        recurringExpenses: List<RecurringExpense>,
        savingsGoals: List<SavingsGoal>,
        nowMillis: Long = System.currentTimeMillis()
    ): SavingsGoalsCashflowSummary {
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentMonth = nowCal.get(Calendar.MONTH)
        val todayDom = nowCal.get(Calendar.DAY_OF_MONTH)

        val monthStart = Calendar.getInstance().apply {
            set(currentYear, currentMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val monthEnd = Calendar.getInstance().apply {
            set(currentYear, currentMonth, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val monthIncome = allIncomes
            .filter { it.timestamp in monthStart..monthEnd }
            .sumOf { it.amount.coerceAtLeast(0.0) }

        val monthExpenses = allExpenses
            .filter { it.timestamp in monthStart..monthEnd }
            .sumOf { it.amount.coerceAtLeast(0.0) }

        val rawBalance = monthIncome - monthExpenses

        val upcomingRecurring = recurringExpenses
            .filter { it.isActive }
            .sumOf { rec ->
                val remaining = RecurringScheduleHelper.remainingOccurrencesInMonth(
                    recurring = rec,
                    year = currentYear,
                    monthZeroIndexed = currentMonth,
                    afterDayOfMonth = todayDom
                )
                remaining * rec.amount.coerceAtLeast(0.0)
            }

        val availableForSavings = (rawBalance - upcomingRecurring).coerceAtLeast(0.0)

        val totalTarget = savingsGoals.sumOf { it.targetAmount.coerceAtLeast(0.0) }
        val totalSaved = savingsGoals.sumOf { it.currentAmount.coerceAtLeast(0.0) }
        val totalRemaining = savingsGoals.sumOf { (it.targetAmount - it.currentAmount).coerceAtLeast(0.0) }
        val overallProgress = if (totalTarget > 0.0) {
            (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }

        val totalIncompleteRemaining = savingsGoals.sumOf {
            (it.targetAmount - it.currentAmount).coerceAtLeast(0.0)
        }

        val planItems = savingsGoals.map { goal ->
            val safeTarget = goal.targetAmount.coerceAtLeast(0.0)
            val safeCurrent = goal.currentAmount.coerceAtLeast(0.0)
            val remaining = (safeTarget - safeCurrent).coerceAtLeast(0.0)
            val progress = if (safeTarget > 0.0) (safeCurrent / safeTarget).toFloat().coerceIn(0f, 1f) else 0f
            val isCompleted = safeTarget > 0.0 && safeCurrent >= safeTarget

            val suggested = if (!isCompleted && availableForSavings > 0.0 && totalIncompleteRemaining > 0.0) {
                val share = availableForSavings * (remaining / totalIncompleteRemaining)
                share.coerceAtMost(remaining)
            } else {
                0.0
            }

            val estMonths = if (!isCompleted && suggested > 0.05) {
                ceil(remaining / suggested).toInt().coerceAtLeast(1)
            } else {
                null
            }

            SavingsGoalPlanItem(
                goal = goal,
                remainingAmount = remaining,
                progress = progress,
                isCompleted = isCompleted,
                suggestedMonthlyContribution = suggested,
                estimatedMonthsToTarget = estMonths
            )
        }

        return SavingsGoalsCashflowSummary(
            currentMonthIncome = monthIncome,
            currentMonthExpenses = monthExpenses,
            currentMonthRawBalance = rawBalance,
            upcomingRecurringThisMonth = upcomingRecurring,
            availableForSavings = availableForSavings,
            totalGoalTarget = totalTarget,
            totalGoalSaved = totalSaved,
            totalGoalRemaining = totalRemaining,
            overallGoalProgress = overallProgress,
            planItems = planItems
        )
    }
}
