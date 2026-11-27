package com.example

import com.example.data.entity.Expense
import com.example.data.entity.FamilyMember
import com.example.data.entity.getBeneficiaryCategoryOrNamesDisplay
import com.example.data.entity.getPayerCategoryOrNamesDisplay
import com.example.data.entity.getPayerShares
import com.example.data.entity.hasMultiplePayers
import com.example.data.entity.resolveReconciledBeneficiaries
import com.example.data.entity.resolveReconciledPayers
import com.example.data.model.FirebaseExpense
import com.example.data.model.FirebaseIncome
import com.example.util.AnalyticsExpenseFilterHelper
import com.example.util.Localization
import com.example.util.SettleUpCalculator
import com.example.util.StartupSafety
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataIntegrityHardeningTest {

    private val marios = FamilyMember(id = 1L, name = "Μάριος", role = "Γονέας", isDefault = true, sortOrder = 0)
    private val vasilikiShifted = FamilyMember(id = 5L, name = "Βασιλική", role = "Γονέας", isDefault = false, sortOrder = 1)
    private val melina = FamilyMember(id = 3L, name = "Μελίνα", role = "Παιδί", isDefault = false, sortOrder = 2)
    private val currentMembers = listOf(marios, vasilikiShifted, melina)

    @Test
    fun `shifted spouse member ID in payerMemberIdsCsv is reconciled via payerMemberName so both parents stay payers`() {
        val expense = Expense(
            id = 100L,
            title = "Σούπερ Μάρκετ",
            amount = 80.0,
            categoryId = 1L,
            categoryName = "Σούπερ μάρκετ",
            splitType = "ALL_FAMILY",
            memberIdsCsv = "1,5,3",
            memberNamesDisplay = "Όλοι",
            payerMemberId = 1L,
            payerMemberName = "Γονείς",
            payerMemberIdsCsv = "1,2", // old spouse ID 2 drifted to 5
            payerAmountsCsv = ""
        )

        val resolvedPayers = expense.resolveReconciledPayers(currentMembers)
        assertEquals(listOf(1L, 5L), resolvedPayers.map { it.id })
        assertTrue(expense.hasMultiplePayers(currentMembers))
        assertEquals("Γονείς", expense.getPayerCategoryOrNamesDisplay(currentMembers, Localization.Language.GREEK))
        assertEquals("Parents", expense.getPayerCategoryOrNamesDisplay(currentMembers, Localization.Language.ENGLISH))

        val shareMap = expense.getPayerShares(currentMembers).associate { it.memberId to it.amount }
        assertEquals(2, shareMap.size)
        assertEquals(40.0, shareMap[1L] ?: 0.0, 0.01)
        assertEquals(40.0, shareMap[5L] ?: 0.0, 0.01)

        val settleUp = SettleUpCalculator.calculate(listOf(expense), currentMembers)
        val mariosRow = settleUp.memberSummaries.first { it.member.id == 1L }
        val vasilikiRow = settleUp.memberSummaries.first { it.member.id == 5L }
        assertEquals(40.0, mariosRow.totalPaid, 0.01)
        assertEquals(40.0, vasilikiRow.totalPaid, 0.01)
    }

    @Test
    fun `shifted spouse ID in custom payerAmountsCsv remaps amount to current spouse ID instead of dropping it`() {
        val expense = Expense(
            id = 101L,
            title = "Ενοίκιο",
            amount = 40.0,
            categoryId = 1L,
            categoryName = "Σπίτι",
            splitType = "ALL_FAMILY",
            memberIdsCsv = "1,5",
            memberNamesDisplay = "Γονείς",
            payerMemberId = 1L,
            payerMemberName = "Γονείς",
            payerMemberIdsCsv = "1,2",
            payerAmountsCsv = "1:30.00,2:10.00"
        )

        val shareMap = expense.getPayerShares(currentMembers).associate { it.memberId to it.amount }
        assertEquals(2, shareMap.size)
        assertEquals(30.0, shareMap[1L] ?: 0.0, 0.01)
        assertEquals(10.0, shareMap[5L] ?: 0.0, 0.01)
    }

    @Test
    fun `legacy single-ID fallback with multi-payer label Γονείς expands to both parents`() {
        val expense = Expense(
            id = 102L,
            title = "ΔΕΗ",
            amount = 100.0,
            categoryId = 2L,
            categoryName = "Λογαριασμοί",
            splitType = "ALL_FAMILY",
            memberIdsCsv = "",
            memberNamesDisplay = "Όλοι",
            payerMemberId = 1L,
            payerMemberName = "Γονείς",
            payerMemberIdsCsv = "1",
            payerAmountsCsv = "1:100.00"
        )

        val resolvedPayers = expense.resolveReconciledPayers(currentMembers)
        assertEquals(listOf(1L, 5L), resolvedPayers.map { it.id })
        assertTrue(expense.hasMultiplePayers(currentMembers))

        val shareMap = expense.getPayerShares(currentMembers).associate { it.memberId to it.amount }
        assertEquals(2, shareMap.size)
        assertEquals(50.0, shareMap[1L] ?: 0.0, 0.01)
        assertEquals(50.0, shareMap[5L] ?: 0.0, 0.01)
    }

    @Test
    fun `explicit single payer stays single payer with zero false expansion`() {
        val expense = Expense(
            id = 103L,
            title = "Καφές",
            amount = 4.5,
            categoryId = 3L,
            categoryName = "Καφές",
            splitType = "INDIVIDUAL",
            memberIdsCsv = "1",
            memberNamesDisplay = "Μάριος",
            payerMemberId = 1L,
            payerMemberName = "Μάριος",
            payerMemberIdsCsv = "1",
            payerAmountsCsv = "1:4.50"
        )

        val resolvedPayers = expense.resolveReconciledPayers(currentMembers)
        assertEquals(listOf(1L), resolvedPayers.map { it.id })
        assertFalse(expense.hasMultiplePayers(currentMembers))
        assertEquals("Μάριος", expense.getPayerCategoryOrNamesDisplay(currentMembers, Localization.Language.GREEK))

        val shareMap = expense.getPayerShares(currentMembers).associate { it.memberId to it.amount }
        assertEquals(mapOf(1L to 4.5), shareMap)
    }

    @Test
    fun `shifted beneficiary ID in memberIdsCsv is reconciled across display, analytics, and settle-up`() {
        val expense = Expense(
            id = 104L,
            title = "Δείπνο Γονέων",
            amount = 60.0,
            categoryId = 4L,
            categoryName = "Φαγητό",
            splitType = "CUSTOM",
            memberIdsCsv = "1,2", // spouse ID drifted from 2 to 5
            memberNamesDisplay = "Γονείς",
            payerMemberId = 1L,
            payerMemberName = "Μάριος",
            payerMemberIdsCsv = "1",
            payerAmountsCsv = "1:60.00"
        )

        val beneficiaries = expense.resolveReconciledBeneficiaries(currentMembers)
        assertEquals(listOf(1L, 5L), beneficiaries.map { it.id })
        assertEquals("Γονείς", expense.getBeneficiaryCategoryOrNamesDisplay(currentMembers, Localization.Language.GREEK))

        val analyticsMembers = AnalyticsExpenseFilterHelper.resolveMembersForExpense(expense, currentMembers)
        assertEquals(listOf(1L, 5L), analyticsMembers.map { it.id })

        val settleUp = SettleUpCalculator.calculate(listOf(expense), currentMembers)
        val mariosRow = settleUp.memberSummaries.first { it.member.id == 1L }
        val vasilikiRow = settleUp.memberSummaries.first { it.member.id == 5L }
        val melinaRow = settleUp.memberSummaries.first { it.member.id == 3L }
        assertEquals(30.0, mariosRow.totalShare, 0.01)
        assertEquals(30.0, vasilikiRow.totalShare, 0.01)
        assertEquals(0.0, melinaRow.totalShare, 0.01)
    }

    @Test
    fun `allocateNextSafeId never reuses active, trashed, or historically referenced IDs`() {
        assertEquals(1L, StartupSafety.allocateNextSafeId(emptyList()))
        assertEquals(6L, StartupSafety.allocateNextSafeId(listOf(1L, 2L, 5L)))
        assertEquals(10L, StartupSafety.allocateNextSafeId(listOf(-1L, 0L, 4L, 9L)))
    }

    @Test
    fun `resolveAuthoritativeExpenses and Incomes keep legacy blank-householdId docs inside active household even with multiple households`() {
        val legacyBlankHidExpense = FirebaseExpense(
            id = "legacy-exp-1",
            householdId = "",
            title = "Παλιό Έξοδο",
            amount = 25.0,
            categoryId = 1L,
            categoryName = "Σούπερ μάρκετ"
        )
        val otherHouseholdExpense = FirebaseExpense(
            id = "other-exp-2",
            householdId = "h_other",
            title = "Άλλο Εξοδολόγιο",
            amount = 50.0,
            categoryId = 1L,
            categoryName = "Σούπερ μάρκετ"
        )

        val resolvedExpenses = StartupSafety.resolveAuthoritativeExpenses(
            activeHouseholdId = "h_primary",
            totalHouseholdCount = 3,
            householdExpenses = listOf(legacyBlankHidExpense, otherHouseholdExpense),
            userExpenses = emptyList()
        )
        assertEquals(listOf("legacy-exp-1"), resolvedExpenses.map { it.id })

        val legacyBlankHidIncome = FirebaseIncome(
            id = "legacy-inc-1",
            householdId = "",
            title = "Μισθός",
            amount = 1500.0,
            category = "Μισθός"
        )
        val resolvedIncomes = StartupSafety.resolveAuthoritativeIncomes(
            activeHouseholdId = "h_primary",
            totalHouseholdCount = 3,
            householdIncomes = listOf(legacyBlankHidIncome),
            userIncomes = emptyList()
        )
        assertEquals(listOf("legacy-inc-1"), resolvedIncomes.map { it.id })
    }

    @Test
    fun `expense swipe policy ignores partial swipes and only triggers near edge`() {
        val cardWidth = 1000f

        // Partial swipes (even up to 71%) must NEVER trigger delete or duplicate
        assertEquals(
            com.example.ui.screens.ExpenseSwipeAction.NONE,
            com.example.ui.screens.ExpenseSwipePolicy.evaluateAction(0f, cardWidth, canDuplicate = true, wasCancelled = false)
        )
        assertEquals(
            com.example.ui.screens.ExpenseSwipeAction.NONE,
            com.example.ui.screens.ExpenseSwipePolicy.evaluateAction(-300f, cardWidth, canDuplicate = true, wasCancelled = false)
        )
        assertEquals(
            com.example.ui.screens.ExpenseSwipeAction.NONE,
            com.example.ui.screens.ExpenseSwipePolicy.evaluateAction(-710f, cardWidth, canDuplicate = true, wasCancelled = false)
        )
        assertEquals(
            com.example.ui.screens.ExpenseSwipeAction.NONE,
            com.example.ui.screens.ExpenseSwipePolicy.evaluateAction(710f, cardWidth, canDuplicate = true, wasCancelled = false)
        )

        // Cancelled gesture must NEVER trigger even at 90% width
        assertEquals(
            com.example.ui.screens.ExpenseSwipeAction.NONE,
            com.example.ui.screens.ExpenseSwipePolicy.evaluateAction(-900f, cardWidth, canDuplicate = true, wasCancelled = true)
        )

        // Full edge-to-edge drag (>= 72%) triggers DELETE on left and DUPLICATE on right
        assertEquals(
            com.example.ui.screens.ExpenseSwipeAction.DELETE,
            com.example.ui.screens.ExpenseSwipePolicy.evaluateAction(-720f, cardWidth, canDuplicate = true, wasCancelled = false)
        )
        assertEquals(
            com.example.ui.screens.ExpenseSwipeAction.DUPLICATE,
            com.example.ui.screens.ExpenseSwipePolicy.evaluateAction(720f, cardWidth, canDuplicate = true, wasCancelled = false)
        )

        // Right drag when duplicate is disabled stays NONE
        assertEquals(
            com.example.ui.screens.ExpenseSwipeAction.NONE,
            com.example.ui.screens.ExpenseSwipePolicy.evaluateAction(850f, cardWidth, canDuplicate = false, wasCancelled = false)
        )
    }
}
