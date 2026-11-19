package com.example.util

import com.example.data.entity.Expense
import com.example.data.entity.Income
import com.example.data.entity.RecurringExpense
import java.util.Calendar
import kotlin.math.roundToInt

data class MonthComparisonForecastResult(
    val selectedMonthExpenses: Double,
    val previousMonthExpenses: Double,
    val expenseDeltaAmount: Double,
    val expenseDeltaPercent: Int?,
    val selectedMonthIncomes: Double,
    val previousMonthIncomes: Double,
    val incomeDeltaAmount: Double,
    val incomeDeltaPercent: Int?,
    val isCurrentCalendarMonth: Boolean,
    val elapsedDaysInMonth: Int,
    val totalDaysInMonth: Int,
    val dailyAveragePace: Double,
    val remainingScheduledRecurringTotal: Double,
    val projectedEndOfMonthExpenses: Double,
    val projectedEndOfMonthNet: Double
)

object AnalyticsForecastHelper {

    fun calculate(
        allExpenses: List<Expense>,
        allIncomes: List<Income>,
        recurringExpenses: List<RecurringExpense>,
        selectedYear: Int,
        selectedMonthZeroIndexed: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): MonthComparisonForecastResult {
        val prevCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonthZeroIndexed)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -1)
        }
        val prevYear = prevCal.get(Calendar.YEAR)
        val prevMonth = prevCal.get(Calendar.MONTH)

        var selectedExpTotal = 0.0
        var prevExpTotal = 0.0
        val expCal = Calendar.getInstance()
        for (exp in allExpenses) {
            expCal.timeInMillis = exp.timestamp
            val y = expCal.get(Calendar.YEAR)
            val m = expCal.get(Calendar.MONTH)
            if (y == selectedYear && m == selectedMonthZeroIndexed) {
                selectedExpTotal += exp.amount
            } else if (y == prevYear && m == prevMonth) {
                prevExpTotal += exp.amount
            }
        }

        var selectedIncTotal = 0.0
        var prevIncTotal = 0.0
        for (inc in allIncomes) {
            expCal.timeInMillis = inc.timestamp
            val y = expCal.get(Calendar.YEAR)
            val m = expCal.get(Calendar.MONTH)
            if (y == selectedYear && m == selectedMonthZeroIndexed) {
                selectedIncTotal += inc.amount
            } else if (y == prevYear && m == prevMonth) {
                prevIncTotal += inc.amount
            }
        }

        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val isCurrentCalendarMonth = nowCal.get(Calendar.YEAR) == selectedYear &&
            nowCal.get(Calendar.MONTH) == selectedMonthZeroIndexed

        val targetMonthCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonthZeroIndexed)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val totalDaysInMonth = targetMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val elapsedDays = if (isCurrentCalendarMonth) {
            nowCal.get(Calendar.DAY_OF_MONTH).coerceIn(1, totalDaysInMonth)
        } else {
            totalDaysInMonth
        }

        val dailyPace = if (elapsedDays > 0) selectedExpTotal / elapsedDays.toDouble() else 0.0

        val remainingRecurringTotal = if (isCurrentCalendarMonth) {
            recurringExpenses.filter { it.isActive }.sumOf { rec ->
                val count = RecurringScheduleHelper.remainingOccurrencesInMonth(
                    recurring = rec,
                    year = selectedYear,
                    monthZeroIndexed = selectedMonthZeroIndexed,
                    afterDayOfMonth = elapsedDays
                )
                rec.amount * count
            }
        } else {
            0.0
        }

        val projectedEomExpenses = if (isCurrentCalendarMonth) {
            val remainingDays = (totalDaysInMonth - elapsedDays).coerceAtLeast(0)
            val paceAddition = dailyPace * remainingDays
            selectedExpTotal + paceAddition + remainingRecurringTotal
        } else {
            selectedExpTotal
        }

        val expenseDeltaAmount = selectedExpTotal - prevExpTotal
        val expenseDeltaPct = if (prevExpTotal > 0.01) {
            (((selectedExpTotal - prevExpTotal) / prevExpTotal) * 100.0).roundToInt()
        } else null

        val incomeDeltaAmount = selectedIncTotal - prevIncTotal
        val incomeDeltaPct = if (prevIncTotal > 0.01) {
            (((selectedIncTotal - prevIncTotal) / prevIncTotal) * 100.0).roundToInt()
        } else null

        return MonthComparisonForecastResult(
            selectedMonthExpenses = selectedExpTotal,
            previousMonthExpenses = prevExpTotal,
            expenseDeltaAmount = expenseDeltaAmount,
            expenseDeltaPercent = expenseDeltaPct,
            selectedMonthIncomes = selectedIncTotal,
            previousMonthIncomes = prevIncTotal,
            incomeDeltaAmount = incomeDeltaAmount,
            incomeDeltaPercent = incomeDeltaPct,
            isCurrentCalendarMonth = isCurrentCalendarMonth,
            elapsedDaysInMonth = elapsedDays,
            totalDaysInMonth = totalDaysInMonth,
            dailyAveragePace = dailyPace,
            remainingScheduledRecurringTotal = remainingRecurringTotal,
            projectedEndOfMonthExpenses = projectedEomExpenses,
            projectedEndOfMonthNet = selectedIncTotal - projectedEomExpenses
        )
    }
}
