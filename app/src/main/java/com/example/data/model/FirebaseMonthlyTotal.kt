package com.example.data.model

/**
 * Lightweight pre-aggregated monthly summary stored at:
 * households/{householdId}/monthly_totals/{yyyy-MM}
 * Enables instant historical overview and older-year detection without reading thousands of raw transactions.
 */
data class FirebaseMonthlyTotal(
    val monthKey: String = "",
    val expenseTotal: Double = 0.0,
    val incomeTotal: Double = 0.0,
    val expenseCount: Int = 0,
    val incomeCount: Int = 0,
    val updatedAt: Long = 0L
)
