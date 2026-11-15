package com.example

import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import com.example.util.AnalyticsExpenseFilterHelper
import com.example.util.AnalyticsSpecialFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class AnalyticsExpenseFilterHelperTest {

    private val marios = FamilyMember(id = 1L, name = "Μάριος", role = "Γονέας")
    private val vasiliki = FamilyMember(id = 2L, name = "Βασιλική", role = "Γονέας")
    private val melina = FamilyMember(id = 3L, name = "Μελίνα", role = "Παιδί")
    private val members = listOf(marios, vasiliki, melina)

    private val categories = listOf(
        ExpenseCategory(id = 10L, name = "Σούπερ μάρκετ"),
        ExpenseCategory(id = 20L, name = "Καφέδες")
    )

    @Test
    fun testCategoryMatchesByIdOrNormalizedName() {
        val byId = Expense(id = 1, title = "Lidl", amount = 20.0, categoryId = 10L, categoryName = "Other")
        val byName = Expense(id = 2, title = "Sklavenitis", amount = 30.0, categoryId = 999L, categoryName = "σουπερ μαρκετ")
        val unrelated = Expense(id = 3, title = "Coffee", amount = 3.0, categoryId = 20L, categoryName = "Καφέδες")

        assertTrue(AnalyticsExpenseFilterHelper.matchesCategory(byId, 10L, categories))
        assertTrue(AnalyticsExpenseFilterHelper.matchesCategory(byName, 10L, categories))
        assertFalse(AnalyticsExpenseFilterHelper.matchesCategory(unrelated, 10L, categories))
    }

    @Test
    fun testPayerAndSettleUpBeneficiaryFilter() {
        val sharedPaidByMarios = Expense(
            id = 1,
            title = "Σούπερ",
            amount = 60.0,
            categoryId = 10L,
            categoryName = "Σούπερ μάρκετ",
            splitType = "ALL_FAMILY",
            payerMemberId = 1L,
            payerMemberName = "Μάριος"
        )
        val splitPaidByBoth = Expense(
            id = 2,
            title = "Δείπνο",
            amount = 40.0,
            categoryId = 20L,
            categoryName = "Καφέδες",
            splitType = "SPECIFIC_MEMBERS",
            memberIdsCsv = "2",
            memberNamesDisplay = "Βασιλική",
            payerMemberIdsCsv = "1,2",
            payerMemberName = "Μάριος & Βασιλική"
        )
        val paidByVasilikiForSelf = Expense(
            id = 3,
            title = "Καφές",
            amount = 4.0,
            categoryId = 20L,
            categoryName = "Καφέδες",
            splitType = "SPECIFIC_MEMBERS",
            memberIdsCsv = "2",
            memberNamesDisplay = "Βασιλική",
            payerMemberId = 2L,
            payerMemberName = "Βασιλική"
        )

        val transferFilter = AnalyticsSpecialFilter(
            badgeLabel = "Μάριος ➔ Βασιλική",
            payerMemberId = 1L,
            debtorMemberId = 2L
        )
        assertTrue(AnalyticsExpenseFilterHelper.matchesSpecialExpenseFilter(sharedPaidByMarios, transferFilter, members))
        assertTrue(AnalyticsExpenseFilterHelper.matchesSpecialExpenseFilter(splitPaidByBoth, transferFilter, members))
        assertFalse(AnalyticsExpenseFilterHelper.matchesSpecialExpenseFilter(paidByVasilikiForSelf, transferFilter, members))
    }

    @Test
    fun testRoleGroupAndExactDayAndIncomeFilters() {
        val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val tsDay1 = dayFmt.parse("2026-11-10")!!.time + 3600_000L
        val tsDay2 = dayFmt.parse("2026-11-11")!!.time + 3600_000L

        val kidsExp = Expense(
            id = 1,
            title = "Φροντιστήριο",
            amount = 80.0,
            categoryId = 10L,
            categoryName = "Σχολείο",
            timestamp = tsDay1,
            splitType = "SPECIFIC_MEMBERS",
            memberIdsCsv = "3",
            memberNamesDisplay = "Μελίνα",
            payerMemberId = 1L,
            payerMemberName = "Μάριος"
        )
        val parentsExp = Expense(
            id = 2,
            title = "Καφές",
            amount = 5.0,
            categoryId = 20L,
            categoryName = "Καφέδες",
            timestamp = tsDay2,
            splitType = "ALL_FAMILY",
            payerMemberId = 1L,
            payerMemberName = "Μάριος"
        )

        val kidsFilter = AnalyticsSpecialFilter(roleGroupFilter = "KIDS")
        val parentsFilter = AnalyticsSpecialFilter(roleGroupFilter = "PARENTS")
        val peakDayFilter = AnalyticsSpecialFilter(exactDayKey = "2026-11-10")

        assertTrue(AnalyticsExpenseFilterHelper.matchesSpecialExpenseFilter(kidsExp, kidsFilter, members))
        assertFalse(AnalyticsExpenseFilterHelper.matchesSpecialExpenseFilter(parentsExp, kidsFilter, members))
        assertTrue(AnalyticsExpenseFilterHelper.matchesSpecialExpenseFilter(parentsExp, parentsFilter, members))
        assertFalse(AnalyticsExpenseFilterHelper.matchesSpecialExpenseFilter(kidsExp, parentsFilter, members))
        assertTrue(AnalyticsExpenseFilterHelper.matchesSpecialExpenseFilter(kidsExp, peakDayFilter, members))
        assertFalse(AnalyticsExpenseFilterHelper.matchesSpecialExpenseFilter(parentsExp, peakDayFilter, members))

        val incMarios = Income(id = 1, title = "Μισθός", amount = 1500.0, category = "Μισθός", memberId = 1L, memberName = "Μάριος")
        val incUnassigned = Income(id = 2, title = "Επίδομα", amount = 200.0, category = "Επίδομα", memberId = 0L, memberName = "")

        assertTrue(AnalyticsExpenseFilterHelper.matchesSpecialIncomeFilter(incMarios, AnalyticsSpecialFilter(exactMemberId = 1L), members))
        assertFalse(AnalyticsExpenseFilterHelper.matchesSpecialIncomeFilter(incUnassigned, AnalyticsSpecialFilter(exactMemberId = 1L), members))
        assertTrue(AnalyticsExpenseFilterHelper.matchesSpecialIncomeFilter(incUnassigned, AnalyticsSpecialFilter(exactMemberId = -1L), members))
        assertTrue(AnalyticsExpenseFilterHelper.matchesSpecialIncomeFilter(incMarios, AnalyticsSpecialFilter(incomeCategory = "Μισθός"), members))
        assertFalse(AnalyticsExpenseFilterHelper.matchesSpecialIncomeFilter(incUnassigned, AnalyticsSpecialFilter(incomeCategory = "Μισθός"), members))
    }
}
