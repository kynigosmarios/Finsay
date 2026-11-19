package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_expenses")
data class RecurringExpense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val categoryId: Long,
    val categoryName: String,
    val frequency: String = "MONTHLY", // "WEEKLY", "BIWEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"
    val dayOfMonth: Int = 1, // 1..31 for MONTHLY/QUARTERLY/YEARLY; 1..7 (Mon..Sun) for WEEKLY/BIWEEKLY
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val splitType: String = "ALL_FAMILY", // "ALL_FAMILY" or "SPECIFIC_MEMBERS"
    val memberIdsCsv: String = "",
    val memberNamesDisplay: String = "Κοινό Οικογένειας",
    val note: String = "",
    val isActive: Boolean = true,
    val lastGeneratedMonth: String = "", // e.g. "2026-10", "2026-W42", "2026-Q4", "2026"
    val payerMemberId: Long = 0L,
    val payerMemberName: String = "",
    val payerMemberIdsCsv: String = "",
    val payerAmountsCsv: String = "",
    val anchorMonth: Int = 0 // 0..11 for YEARLY or QUARTERLY anchor month
)
