package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import com.example.data.entity.RecurringExpense
import com.example.data.entity.SavingsGoal
import com.example.data.model.FirebaseSavingsGoal
import com.example.data.model.Household
import com.example.data.model.HouseholdRole
import com.example.ui.components.GoalDeadlineDateHelper
import com.example.ui.components.ManualExpenseSaveResolver
import com.example.util.AnalyticsForecastHelper
import com.example.util.BankNotificationHolder
import com.example.util.DefaultPayerHelper
import com.example.util.GreekVoiceParser
import com.example.util.Localization
import com.example.util.MerchantCategoryLearner
import com.example.util.PendingBankTransaction
import com.example.util.RecurringScheduleHelper
import com.example.util.SavingsGoalAdvisorHelper
import com.example.util.SavingsGoalExpenseLinkHelper
import com.example.util.StartupSafety
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinsayImprovementsTest {

    private val categories = listOf(
        ExpenseCategory(id = 1L, name = "Σούπερ μάρκετ", iconKey = "cart", voiceKeywords = "σουπερ μαρκετ,ψωνια,σκλαβενιτης"),
        ExpenseCategory(id = 2L, name = "Καφές & Ποτό", iconKey = "coffee", voiceKeywords = "καφες,φρεντο"),
        ExpenseCategory(id = 3L, name = "Λογαριασμοί & Σπίτι", iconKey = "bill", voiceKeywords = "ρευμα,νερο,δεη")
    )

    private val marios = FamilyMember(id = 10L, name = "Μάριος", role = "Γονέας", linkedUserUid = "uid_marios")
    private val vasiliki = FamilyMember(id = 20L, name = "Βασιλική", role = "Γονέας", linkedUserUid = "uid_vasiliki")
    private val members = listOf(marios, vasiliki)

    @Test
    fun `merchant category learner normalizes merchant and AFM and scopes per household`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("σκλαβενιτης", MerchantCategoryLearner.normalizeMerchantKey("  ΣΚΛΑΒΕΝΙΤΗΣ #124! "))
        assertEquals("094014201", MerchantCategoryLearner.normalizeAfm("EL 094-014-201"))
        assertEquals("", MerchantCategoryLearner.normalizeAfm("12345"))

        MerchantCategoryLearner.learn(
            context = context,
            householdId = "hh_alpha",
            merchantOrTitle = "Σκλαβενίτης 45",
            categoryId = 1L,
            categoryName = "Σούπερ μάρκετ",
            merchantAfm = "094014201"
        )

        val byAfm = MerchantCategoryLearner.suggest(
            context = context,
            householdId = "hh_alpha",
            merchantOrTitle = "Άγνωστο Κατάστημα",
            merchantAfm = "094014201",
            availableCategories = categories
        )
        assertNotNull(byAfm)
        assertEquals(1L, byAfm?.categoryId)

        val byPrefix = MerchantCategoryLearner.suggest(
            context = context,
            householdId = "hh_alpha",
            merchantOrTitle = "ΣΚΛΑΒΕΝΙΤΗΣ ΜΑΡΟΥΣΙ",
            availableCategories = categories
        )
        assertNotNull(byPrefix)
        assertEquals("Σούπερ μάρκετ", byPrefix?.categoryName)

        // Household isolation: a different household must not inherit hh_alpha's learned preference
        val otherHousehold = MerchantCategoryLearner.suggest(
            context = context,
            householdId = "hh_beta",
            merchantOrTitle = "Σκλαβενίτης",
            merchantAfm = "094014201",
            availableCategories = categories
        )
        assertNull(otherHousehold)
    }

    @Test
    fun `recurring schedule helper handles all 5 frequencies cleanly`() {
        val sampleTimeCal = Calendar.getInstance().apply {
            set(2026, Calendar.APRIL, 15, 8, 30, 0) // Wednesday April 15, 2026
            set(Calendar.MILLISECOND, 0)
        }
        val nowMillis = sampleTimeCal.timeInMillis

        val weekly = RecurringExpense(
            id = 1L, title = "Πάγιο Εβδομάδας", amount = 25.0,
            categoryId = 1L, categoryName = "Σούπερ μάρκετ",
            frequency = RecurringScheduleHelper.FREQ_WEEKLY, dayOfMonth = 3 // Wednesday
        )
        val biweekly = weekly.copy(id = 2L, frequency = RecurringScheduleHelper.FREQ_BIWEEKLY, dayOfMonth = 1)
        val monthly = weekly.copy(id = 3L, frequency = RecurringScheduleHelper.FREQ_MONTHLY, dayOfMonth = 15)
        val quarterly = weekly.copy(
            id = 4L, frequency = RecurringScheduleHelper.FREQ_QUARTERLY,
            dayOfMonth = 15, anchorMonth = Calendar.JANUARY // Jan, Apr, Jul, Oct
        )
        val yearly = weekly.copy(
            id = 5L, frequency = RecurringScheduleHelper.FREQ_YEARLY,
            dayOfMonth = 15, anchorMonth = Calendar.APRIL
        )

        assertTrue(RecurringScheduleHelper.periodKeyFor(weekly, nowMillis).contains("-W"))
        assertTrue(RecurringScheduleHelper.periodKeyFor(biweekly, nowMillis).contains("-BW"))
        assertEquals("2026-04", RecurringScheduleHelper.periodKeyFor(monthly, nowMillis))
        assertEquals("2026-Q2", RecurringScheduleHelper.periodKeyFor(quarterly, nowMillis))
        assertEquals("2026", RecurringScheduleHelper.periodKeyFor(yearly, nowMillis))

        assertTrue(RecurringScheduleHelper.isDueNow(weekly, nowMillis))
        assertTrue(RecurringScheduleHelper.isDueNow(monthly, nowMillis))
        assertTrue(RecurringScheduleHelper.isDueNow(quarterly, nowMillis))
        assertTrue(RecurringScheduleHelper.isDueNow(yearly, nowMillis))
        assertFalse(
            RecurringScheduleHelper.isDueNow(
                yearly.copy(anchorMonth = Calendar.NOVEMBER),
                nowMillis
            )
        )

        // Next triggers are always strictly in the future
        listOf(weekly, biweekly, monthly, quarterly, yearly).forEach { rec ->
            val next = RecurringScheduleHelper.calculateNextTriggerTimeMillis(
                frequency = rec.frequency,
                dayOfMonth = rec.dayOfMonth,
                anchorMonth = rec.anchorMonth,
                hour = rec.reminderHour,
                minute = rec.reminderMinute,
                nowMillis = nowMillis
            )
            assertTrue("Expected future trigger for ${rec.frequency}", next > nowMillis)
            assertTrue(RecurringScheduleHelper.scheduleSummary(rec, Localization.Language.GREEK).isNotBlank())
            assertTrue(RecurringScheduleHelper.scheduleSummary(rec, Localization.Language.ENGLISH).isNotBlank())
        }

        // Remaining occurrences in April 2026 after day 15: Wednesdays remaining = Apr 22 & Apr 29 (2 occurrences)
        assertEquals(2, RecurringScheduleHelper.remainingOccurrencesInMonth(weekly, 2026, Calendar.APRIL, afterDayOfMonth = 15))
        // Biweekly caps at 1 remaining occurrence in remaining month
        assertEquals(1, RecurringScheduleHelper.remainingOccurrencesInMonth(biweekly, 2026, Calendar.APRIL, afterDayOfMonth = 15))
        // Yearly due on April 20 should count as 1 remaining after day 15
        assertEquals(1, RecurringScheduleHelper.remainingOccurrencesInMonth(yearly.copy(dayOfMonth = 20), 2026, Calendar.APRIL, afterDayOfMonth = 15))
        // Yearly due on April 10 is already past day 15
        assertEquals(0, RecurringScheduleHelper.remainingOccurrencesInMonth(yearly.copy(dayOfMonth = 10), 2026, Calendar.APRIL, afterDayOfMonth = 15))
    }

    @Test
    fun `analytics forecast helper computes MoM deltas and end of month projection accurately`() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.APRIL, 10, 12, 0, 0) // 10 of 30 days elapsed in April 2026
            set(Calendar.MILLISECOND, 0)
        }
        val aprilExp = Calendar.getInstance().apply { set(2026, Calendar.APRIL, 5, 10, 0, 0) }.timeInMillis
        val marchExp = Calendar.getInstance().apply { set(2026, Calendar.MARCH, 12, 10, 0, 0) }.timeInMillis

        val expenses = listOf(
            Expense(id = 1L, title = "Ψώνια", amount = 300.0, categoryId = 1L, categoryName = "Σούπερ μάρκετ", timestamp = aprilExp),
            Expense(id = 2L, title = "Λογαριασμός", amount = 200.0, categoryId = 3L, categoryName = "Λογαριασμοί & Σπίτι", timestamp = marchExp)
        )
        val incomes = listOf(
            Income(id = 1L, title = "Μισθός Απρ", amount = 2000.0, memberId = 10L, memberName = "Μάριος", timestamp = aprilExp),
            Income(id = 2L, title = "Μισθός Μαρ", amount = 1600.0, memberId = 10L, memberName = "Μάριος", timestamp = marchExp)
        )
        val recurring = listOf(
            RecurringExpense(
                id = 1L, title = "Ενοίκιο", amount = 500.0,
                categoryId = 3L, categoryName = "Λογαριασμοί & Σπίτι",
                frequency = RecurringScheduleHelper.FREQ_MONTHLY, dayOfMonth = 25, isActive = true
            )
        )

        val result = AnalyticsForecastHelper.calculate(
            allExpenses = expenses,
            allIncomes = incomes,
            recurringExpenses = recurring,
            selectedYear = 2026,
            selectedMonthZeroIndexed = Calendar.APRIL,
            nowMillis = nowCal.timeInMillis
        )

        assertTrue(result.isCurrentCalendarMonth)
        assertEquals(10, result.elapsedDaysInMonth)
        assertEquals(30, result.totalDaysInMonth)
        assertEquals(300.0, result.selectedMonthExpenses, 0.01)
        assertEquals(200.0, result.previousMonthExpenses, 0.01)
        assertEquals(100.0, result.expenseDeltaAmount, 0.01)
        assertEquals(50, result.expenseDeltaPercent)
        assertEquals(400.0, result.incomeDeltaAmount, 0.01)
        assertEquals(25, result.incomeDeltaPercent)
        // Daily pace = 300 / 10 = 30/day * 20 remaining days = 600 + 300 spent + 500 remaining rent = 1400
        assertEquals(30.0, result.dailyAveragePace, 0.01)
        assertEquals(500.0, result.remainingScheduledRecurringTotal, 0.01)
        assertEquals(1400.0, result.projectedEndOfMonthExpenses, 0.01)
        assertEquals(600.0, result.projectedEndOfMonthNet, 0.01)
    }

    @Test
    fun `greek voice parser respects preferredDefaultPayer only when payer is not explicit`() {
        val withoutPayer = GreekVoiceParser.parse(
            prompt = "12 ευρώ καφές",
            categories = categories,
            allMembers = members,
            preferredDefaultPayer = vasiliki
        )
        assertFalse(withoutPayer.hasExplicitPayer)
        assertEquals(vasiliki.id, withoutPayer.payerMemberId)
        assertEquals("Βασιλική", withoutPayer.payerMemberName)

        val withExplicitPayer = GreekVoiceParser.parse(
            prompt = "12 ευρώ καφές πλήρωσε ο Μάριος",
            categories = categories,
            allMembers = members,
            preferredDefaultPayer = vasiliki
        )
        assertTrue(withExplicitPayer.hasExplicitPayer)
        assertEquals(marios.id, withExplicitPayer.payerMemberId)
        assertEquals("Μάριος", withExplicitPayer.payerMemberName)
    }

    @Test
    fun `RBAC restricts goals and recurring structure management to CREATOR and EDITOR and payer resolver handles splits`() {
        val household = Household(
            id = "hh_shared",
            name = "Οικογένεια",
            ownerId = "uid_marios",
            memberUids = listOf("uid_marios", "uid_vasiliki", "uid_editor"),
            memberRoles = mapOf(
                "uid_marios" to HouseholdRole.CREATOR.key,
                "uid_vasiliki" to HouseholdRole.REGISTAR.key,
                "uid_editor" to HouseholdRole.EDITOR.key
            )
        )

        assertTrue(HouseholdRole.CREATOR.canManageMembersAndCategories)
        assertTrue(HouseholdRole.EDITOR.canManageMembersAndCategories)
        assertFalse(HouseholdRole.REGISTAR.canManageMembersAndCategories)

        // REGISTAR cannot perform remote structure writes (Goals / Recurring / Members / Categories)
        assertFalse(
            StartupSafety.canSafelyWriteRemoteStructure(
                targetHousehold = household,
                userUid = "uid_vasiliki",
                userEmail = "vasiliki@example.com",
                isMarios = false,
                mariosDefaultIds = emptySet()
            )
        )
        assertTrue(
            StartupSafety.canSafelyWriteRemoteStructure(
                targetHousehold = household,
                userUid = "uid_editor",
                userEmail = "editor@example.com",
                isMarios = false,
                mariosDefaultIds = emptySet()
            )
        )

        val resolved = ManualExpenseSaveResolver.resolveSplitAndPayers(
            parsedAmount = 100.0,
            isAllFamily = true,
            selectedMemberIds = listOf(marios.id, vasiliki.id),
            selectedPayerIds = emptyList(),
            fallbackDefaultPayer = vasiliki,
            isCustomPayerSplit = false,
            customPayerAmounts = emptyMap(),
            members = members,
            lang = Localization.Language.GREEK
        )
        assertEquals("ALL_FAMILY", resolved.splitType)
        assertEquals(vasiliki.id, resolved.finalPayerId)
        assertEquals("Βασιλική", resolved.finalPayerName)
        assertEquals("20:100.00", resolved.finalPayerAmountsCsv)
    }

    @Test
    fun `isReminderDueAtExactTime triggers ONLY on exact scheduled day and after exact hour and minute`() {
        val monthly = RecurringExpense(
            id = 100L,
            title = "Ενοίκιο",
            amount = 500.0,
            categoryId = 3L,
            categoryName = "Λογαριασμοί & Σπίτι",
            frequency = RecurringScheduleHelper.FREQ_MONTHLY,
            dayOfMonth = 15,
            reminderHour = 10,
            reminderMinute = 30,
            isActive = true
        )

        fun timeAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
            Calendar.getInstance().apply {
                set(year, month, day, hour, minute, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

        // Scheduled day (April 15) at 10:29 -> NOT due yet
        assertFalse(RecurringScheduleHelper.isReminderDueAtExactTime(monthly, timeAt(2026, Calendar.APRIL, 15, 10, 29)))
        // Scheduled day (April 15) at 10:30 -> DUE
        assertTrue(RecurringScheduleHelper.isReminderDueAtExactTime(monthly, timeAt(2026, Calendar.APRIL, 15, 10, 30)))
        // Scheduled day (April 15) at 14:00 -> DUE
        assertTrue(RecurringScheduleHelper.isReminderDueAtExactTime(monthly, timeAt(2026, Calendar.APRIL, 15, 14, 0)))
        // Next day (April 16) at 14:00 -> NOT due (prevents showing on household switch on another day)
        assertFalse(RecurringScheduleHelper.isReminderDueAtExactTime(monthly, timeAt(2026, Calendar.APRIL, 16, 14, 0)))
        // Day before (April 14) at 14:00 -> NOT due
        assertFalse(RecurringScheduleHelper.isReminderDueAtExactTime(monthly, timeAt(2026, Calendar.APRIL, 14, 14, 0)))

        // Weekly on Wednesday (iso=3) at 09:00
        val weekly = monthly.copy(
            id = 101L,
            frequency = RecurringScheduleHelper.FREQ_WEEKLY,
            dayOfMonth = 3,
            reminderHour = 9,
            reminderMinute = 0
        )
        // Wednesday April 15, 2026
        assertFalse(RecurringScheduleHelper.isReminderDueAtExactTime(weekly, timeAt(2026, Calendar.APRIL, 15, 8, 59)))
        assertTrue(RecurringScheduleHelper.isReminderDueAtExactTime(weekly, timeAt(2026, Calendar.APRIL, 15, 9, 0)))
        // Thursday April 16, 2026 -> NOT due
        assertFalse(RecurringScheduleHelper.isReminderDueAtExactTime(weekly, timeAt(2026, Calendar.APRIL, 16, 12, 0)))

        // Quarterly (Jan anchor -> Jan, Apr, Jul, Oct) on day 15 at 10:30
        val quarterly = monthly.copy(
            id = 102L,
            frequency = RecurringScheduleHelper.FREQ_QUARTERLY,
            anchorMonth = Calendar.JANUARY
        )
        assertTrue(RecurringScheduleHelper.isReminderDueAtExactTime(quarterly, timeAt(2026, Calendar.APRIL, 15, 10, 30)))
        assertFalse(RecurringScheduleHelper.isReminderDueAtExactTime(quarterly, timeAt(2026, Calendar.MAY, 15, 10, 30)))

        // Yearly (April anchor) on day 15 at 10:30
        val yearly = monthly.copy(
            id = 103L,
            frequency = RecurringScheduleHelper.FREQ_YEARLY,
            anchorMonth = Calendar.APRIL
        )
        assertTrue(RecurringScheduleHelper.isReminderDueAtExactTime(yearly, timeAt(2026, Calendar.APRIL, 15, 10, 30)))
        assertFalse(RecurringScheduleHelper.isReminderDueAtExactTime(yearly, timeAt(2026, Calendar.MAY, 15, 10, 30)))
    }

    @Test
    fun `bank notification holder stamps active householdId persists it and strictly scopes per household`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        BankNotificationHolder.init(context)
        BankNotificationHolder.clearAll()

        val appPrefs = context.getSharedPreferences("oikonomos_app_prefs", Context.MODE_PRIVATE)
        appPrefs.edit().putString("last_household_id", "hh_alpha").commit()

        val capturedInAlpha = PendingBankTransaction(
            id = "tx_alpha_1",
            bankName = "Eurobank",
            merchantName = "Σκλαβενίτης",
            amount = 32.50,
            timestamp = 1_700_000_000_000L
        )
        assertTrue(BankNotificationHolder.addTransaction(capturedInAlpha, context))

        // Switch active household to hh_beta and capture another transaction
        appPrefs.edit().putString("last_household_id", "hh_beta").commit()
        val capturedInBeta = PendingBankTransaction(
            id = "tx_beta_1",
            bankName = "Alpha Bank",
            merchantName = "Καφέ",
            amount = 4.20,
            timestamp = 1_700_000_050_000L
        )
        assertTrue(BankNotificationHolder.addTransaction(capturedInBeta, context))

        // Reload from SharedPreferences to verify householdId serialization
        BankNotificationHolder.reloadFromPrefs(context)
        val allStored = BankNotificationHolder.pendingTransactions.value
        assertEquals(2, allStored.size)

        val visibleInAlpha = BankNotificationHolder.filterForHousehold(allStored, "hh_alpha")
        assertEquals(listOf("tx_alpha_1"), visibleInAlpha.map { it.id })

        val visibleInBeta = BankNotificationHolder.filterForHousehold(allStored, "hh_beta")
        assertEquals(listOf("tx_beta_1"), visibleInBeta.map { it.id })

        val visibleWithNoActiveHousehold = BankNotificationHolder.filterForHousehold(allStored, "")
        assertTrue(visibleWithNoActiveHousehold.isEmpty())

        BankNotificationHolder.clearAll()
    }

    @Test
    fun `savings goal advisor reads live monthly income expenses and upcoming bills for goal capacity`() {
        val nowMillis = Calendar.getInstance().apply {
            set(2026, Calendar.APRIL, 10, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val inMonthTs = Calendar.getInstance().apply { set(2026, Calendar.APRIL, 5, 10, 0, 0) }.timeInMillis
        val prevMonthTs = Calendar.getInstance().apply { set(2026, Calendar.MARCH, 20, 10, 0, 0) }.timeInMillis

        val incomes = listOf(
            Income(id = 1L, title = "Μισθός", amount = 2000.0, memberId = 10L, memberName = "Μάριος", timestamp = inMonthTs),
            Income(id = 2L, title = "Μισθός Μαρ", amount = 1500.0, memberId = 10L, memberName = "Μάριος", timestamp = prevMonthTs)
        )
        val expenses = listOf(
            Expense(id = 1L, title = "Σούπερ μάρκετ", amount = 400.0, categoryId = 1L, categoryName = "Σούπερ μάρκετ", timestamp = inMonthTs),
            Expense(id = 2L, title = "Παλιό έξοδο", amount = 900.0, categoryId = 1L, categoryName = "Σούπερ μάρκετ", timestamp = prevMonthTs)
        )
        val recurring = listOf(
            RecurringExpense(
                id = 1L,
                title = "ΔΕΗ",
                amount = 100.0,
                categoryId = 3L,
                categoryName = "Λογαριασμοί & Σπίτι",
                frequency = RecurringScheduleHelper.FREQ_MONTHLY,
                dayOfMonth = 20, // After April 10 -> counted as upcoming obligation
                isActive = true
            )
        )
        val goals = listOf(
            SavingsGoal(id = 1L, title = "Διακοπές", targetAmount = 1500.0, currentAmount = 500.0), // remaining 1000
            SavingsGoal(id = 2L, title = "Ταμείο Ανάγκης", targetAmount = 2000.0, currentAmount = 0.0) // remaining 2000
        )

        val summary = SavingsGoalAdvisorHelper.calculateSummary(
            allExpenses = expenses,
            allIncomes = incomes,
            recurringExpenses = recurring,
            savingsGoals = goals,
            nowMillis = nowMillis
        )

        assertEquals(2000.0, summary.currentMonthIncome, 0.01)
        assertEquals(400.0, summary.currentMonthExpenses, 0.01)
        assertEquals(1600.0, summary.currentMonthRawBalance, 0.01)
        assertEquals(100.0, summary.upcomingRecurringThisMonth, 0.01)
        assertEquals(1500.0, summary.availableForSavings, 0.01)
        assertEquals(3500.0, summary.totalGoalTarget, 0.01)
        assertEquals(500.0, summary.totalGoalSaved, 0.01)
        assertEquals(3000.0, summary.totalGoalRemaining, 0.01)

        // Goal 1 (remaining 1000 / 3000 = 1/3 of 1500 = 500 -> 2 months)
        assertEquals(500.0, summary.planItems[0].suggestedMonthlyContribution, 0.01)
        assertEquals(2, summary.planItems[0].estimatedMonthsToTarget)
        // Goal 2 (remaining 2000 / 3000 = 2/3 of 1500 = 1000 -> 2 months)
        assertEquals(1000.0, summary.planItems[1].suggestedMonthlyContribution, 0.01)
        assertEquals(2, summary.planItems[1].estimatedMonthsToTarget)

        // When expenses exceed income, availableForSavings clamps at 0 and suggestions are 0
        val deficitSummary = SavingsGoalAdvisorHelper.calculateSummary(
            allExpenses = listOf(Expense(id = 9L, title = "Έκτακτο", amount = 2500.0, categoryId = 1L, categoryName = "Σούπερ μάρκετ", timestamp = inMonthTs)),
            allIncomes = incomes,
            recurringExpenses = emptyList(),
            savingsGoals = goals,
            nowMillis = nowMillis
        )
        assertEquals(-500.0, deficitSummary.currentMonthRawBalance, 0.01)
        assertEquals(0.0, deficitSummary.availableForSavings, 0.01)
        assertEquals(0.0, deficitSummary.planItems[0].suggestedMonthlyContribution, 0.01)
        assertNull(deficitSummary.planItems[0].estimatedMonthsToTarget)
    }

    @Test
    fun `resolveAuthoritativeSavingsGoals isolates active household prevents duplicates and blocks delete flicker`() {
        val t0 = 1_700_000_000_000L
        val pendingMutations = mutableMapOf<String, StartupSafety.PendingSavingsGoalMutation>()
        val pendingDeleted = mutableMapOf<String, Pair<String, Long>>()

        val optimisticNew = SavingsGoal(
            id = StartupSafety.safePositiveIdHash("goal_new_1"),
            title = "Αυτοκίνητο",
            targetAmount = 5000.0,
            currentAmount = 500.0,
            deadlineDate = "31/12/2026",
            note = "Νέος"
        )
        pendingMutations["goal_new_1"] = StartupSafety.PendingSavingsGoalMutation(
            cloudId = "goal_new_1",
            householdId = "hh_alpha",
            localEntity = optimisticNew,
            updatedAtMs = t0
        )

        // Personal userGoals or another household's goal must NOT leak into hh_alpha
        val userLeakGoal = FirebaseSavingsGoal(
            id = "goal_user_leak",
            householdId = "",
            title = "Προσωπικός Στόχος",
            targetAmount = 100.0
        )
        val otherHouseholdGoal = FirebaseSavingsGoal(
            id = "goal_other_hh",
            householdId = "hh_beta",
            title = "Άλλο Εξοδολόγιο",
            targetAmount = 900.0
        )

        // Step 1: Remote hasn't echoed goal_new_1 yet -> still shown once via pendingMutations, no leaks
        val step1 = StartupSafety.resolveAuthoritativeSavingsGoals(
            activeHouseholdId = "hh_alpha",
            householdGoals = listOf(otherHouseholdGoal),
            userGoals = listOf(userLeakGoal),
            pendingLocalMutations = pendingMutations,
            pendingDeletedGoals = pendingDeleted,
            nowMs = t0 + 200L
        )
        assertEquals(listOf("Αυτοκίνητο"), step1.goals.map { it.title })
        assertEquals("goal_new_1", step1.cloudIdByLocalId[step1.goals.first().id])

        // Step 2: Duplicate remote snapshot + confirmed goal_new_1 arrives -> still appears strictly ONCE, pending mutation cleared
        val confirmedRemote = FirebaseSavingsGoal(
            id = "goal_new_1",
            householdId = "hh_alpha",
            title = "Αυτοκίνητο",
            targetAmount = 5000.0,
            currentAmount = 500.0,
            deadlineDate = "31/12/2026",
            note = "Νέος"
        )
        val step2 = StartupSafety.resolveAuthoritativeSavingsGoals(
            activeHouseholdId = "hh_alpha",
            householdGoals = listOf(confirmedRemote, confirmedRemote.copy()),
            userGoals = listOf(userLeakGoal),
            pendingLocalMutations = pendingMutations,
            pendingDeletedGoals = pendingDeleted,
            nowMs = t0 + 500L
        )
        assertEquals(1, step2.goals.size)
        assertEquals("Αυτοκίνητο", step2.goals.first().title)
        assertTrue(pendingMutations.isEmpty())

        // Step 3: User deletes goal_new_1 -> tombstone prevents stale snapshot emitted mid-delete from flickering it back
        pendingDeleted["goal_new_1"] = Pair("hh_alpha", t0 + 1_000L)
        val step3 = StartupSafety.resolveAuthoritativeSavingsGoals(
            activeHouseholdId = "hh_alpha",
            householdGoals = listOf(confirmedRemote),
            userGoals = emptyList(),
            pendingLocalMutations = pendingMutations,
            pendingDeletedGoals = pendingDeleted,
            nowMs = t0 + 1_300L
        )
        assertTrue(step3.goals.isEmpty())
    }

    @Test
    fun `goal deadline date helper parses and formats calendar UTC dates cleanly`() {
        val utcMillis = GoalDeadlineDateHelper.parseDeadlineToUtcMillis("25/07/2027")
        assertNotNull(utcMillis)
        assertEquals("25/07/2027", GoalDeadlineDateHelper.formatUtcMillisToDeadline(utcMillis!!))
        assertNull(GoalDeadlineDateHelper.parseDeadlineToUtcMillis("Καλοκαίρι 2027"))
        assertNull(GoalDeadlineDateHelper.parseDeadlineToUtcMillis(""))
    }

    @Test
    fun `savings goal expense link helper encodes decodes cloudId matches category and formats titles`() {
        val goalIdWithUnderscore = "goal_hh_alpha_42"
        val depositCloudId = SavingsGoalExpenseLinkHelper.buildGoalDepositCloudId(goalIdWithUnderscore, "abc123def456")
        assertEquals("goaldep_goal_hh_alpha_42__abc123def456", depositCloudId)
        assertEquals(goalIdWithUnderscore, SavingsGoalExpenseLinkHelper.extractGoalCloudId(depositCloudId))

        val depositExpense = Expense(
            id = 99L,
            cloudId = depositCloudId,
            title = "Στόχος: Διακοπές",
            amount = 150.0,
            categoryId = 4L,
            categoryName = "Αποταμίευση & Στόχοι"
        )
        val regularExpense = Expense(
            id = 100L,
            cloudId = "exp_regular_123",
            title = "Καφές",
            amount = 3.5,
            categoryId = 2L,
            categoryName = "Καφές & Ποτό"
        )
        assertTrue(SavingsGoalExpenseLinkHelper.isGoalDepositExpense(depositExpense))
        assertFalse(SavingsGoalExpenseLinkHelper.isGoalDepositExpense(regularExpense))
        assertNull(SavingsGoalExpenseLinkHelper.extractGoalCloudId("exp_regular_123"))
        assertNull(SavingsGoalExpenseLinkHelper.extractGoalCloudId(""))

        val savingsCat = ExpenseCategory(
            id = 4L,
            name = SavingsGoalExpenseLinkHelper.DEFAULT_SAVINGS_CATEGORY_NAME_EL,
            iconKey = SavingsGoalExpenseLinkHelper.DEFAULT_SAVINGS_CATEGORY_ICON
        )
        assertNull(SavingsGoalExpenseLinkHelper.findSavingsCategory(categories))
        assertEquals(savingsCat, SavingsGoalExpenseLinkHelper.findSavingsCategory(categories + savingsCat))

        val namedSavingsCat = ExpenseCategory(id = 5L, name = "Κουμπαράς Σπιτιού", iconKey = "wallet")
        assertEquals(namedSavingsCat, SavingsGoalExpenseLinkHelper.findSavingsCategory(categories + namedSavingsCat))

        assertEquals("Στόχος: Διακοπές", SavingsGoalExpenseLinkHelper.buildDepositExpenseTitle("Διακοπές", isEnglish = false))
        assertEquals("Goal: Holidays", SavingsGoalExpenseLinkHelper.buildDepositExpenseTitle("Holidays", isEnglish = true))
        assertEquals("Κατάθεση στον στόχο «Διακοπές»", SavingsGoalExpenseLinkHelper.buildDepositExpenseNote("Διακοπές", "", isEnglish = false))
        assertEquals("Δώρο", SavingsGoalExpenseLinkHelper.buildDepositExpenseNote("Διακοπές", "  Δώρο  ", isEnglish = false))
    }

    @Test
    fun `default payer helper supports multi-select 2 plus payers explicit clear and strict owner-only role removal rules`() {
        val kid = FamilyMember(id = 30L, name = "Γιάννης", role = "Παιδί")
        val trio = listOf(marios, vasiliki, kid)

        // CSV parse & format deduplicates and ignores invalid IDs
        assertEquals(listOf(10L, 20L, 30L), DefaultPayerHelper.parseMemberIdsCsv("10, 20, 10, abc, -5, 30"))
        assertEquals("10,20,30", DefaultPayerHelper.formatMemberIdsCsv(listOf(10L, 20L, 10L, 0L, 30L)))

        val household = Household(
            id = "hh_multi",
            name = "Οικογένεια",
            ownerId = "uid_owner",
            memberUids = listOf("uid_owner", "uid_editor", "uid_registar"),
            memberRoles = mapOf(
                "uid_owner" to HouseholdRole.CREATOR.key,
                "uid_editor" to HouseholdRole.EDITOR.key,
                "uid_registar" to HouseholdRole.REGISTAR.key
            ),
            memberDefaultPayers = mapOf(
                "uid_owner" to "10,20",
                "uid_editor" to "10,20,30",
                "uid_registar" to "" // Explicitly cleared by registar
            )
        )

        // 1. Multi-select resolution (2 and 3 members)
        val ownerPayers = DefaultPayerHelper.resolveEffectiveDefaultPayers(
            uid = "uid_owner",
            household = household,
            members = trio
        )
        assertEquals(listOf(10L, 20L), ownerPayers.map { it.id })

        val editorPayers = DefaultPayerHelper.resolveEffectiveDefaultPayers(
            uid = "uid_editor",
            household = household,
            members = trio
        )
        assertEquals(listOf(10L, 20L, 30L), editorPayers.map { it.id })

        // 2. Explicit clear ("") must NOT fall back to legacy linkedUserUid in resolveAssignedDefaultPayers
        val registarWithLegacyMember = FamilyMember(id = 40L, name = "Νίκος", role = "Γονέας", linkedUserUid = "uid_registar")
        val registarAssigned = DefaultPayerHelper.resolveAssignedDefaultPayers(
            uid = "uid_registar",
            household = household,
            members = trio + registarWithLegacyMember
        )
        assertTrue("Explicitly cleared default payers must remain empty", registarAssigned.isEmpty())

        // 3. When key is absent from memberDefaultPayers, legacy linkedUserUid fallback still works
        val legacyFallback = DefaultPayerHelper.resolveEffectiveDefaultPayers(
            uid = "uid_marios",
            household = household,
            members = trio
        )
        assertEquals(listOf(10L), legacyFallback.map { it.id })

        // 4. Equal split formatting for 2 payers and 3 payers
        val split2 = DefaultPayerHelper.buildEqualPayerFields(listOf(marios, vasiliki), 12.0)
        assertEquals(10L, split2.primaryId)
        assertEquals("Μάριος & Βασιλική", split2.displayName)
        assertEquals("10,20", split2.idsCsv)
        assertEquals("10:6.00,20:6.00", split2.amountsCsv)

        val split3 = DefaultPayerHelper.buildEqualPayerFields(trio, 30.0)
        assertEquals("Μάριος & Βασιλική & Γιάννης", split3.displayName)
        assertEquals("10,20,30", split3.idsCsv)
        assertEquals("10:10.00,20:10.00,30:10.00", split3.amountsCsv)

        // 5. Permission checks: CREATOR, EDITOR, REGISTAR can all set their OWN default payers
        assertTrue(DefaultPayerHelper.canAssignDefaultPayers(household, "uid_owner", "uid_owner"))
        assertTrue(DefaultPayerHelper.canAssignDefaultPayers(household, "uid_editor", "uid_editor"))
        assertTrue(DefaultPayerHelper.canAssignDefaultPayers(household, "uid_registar", "uid_registar"))
        // CREATOR can also assign for shared users; EDITOR and REGISTAR CANNOT assign for others
        assertTrue(DefaultPayerHelper.canAssignDefaultPayers(household, "uid_owner", "uid_editor"))
        assertTrue(DefaultPayerHelper.canAssignDefaultPayers(household, "uid_owner", "uid_registar"))
        assertFalse(DefaultPayerHelper.canAssignDefaultPayers(household, "uid_editor", "uid_owner"))
        assertFalse(DefaultPayerHelper.canAssignDefaultPayers(household, "uid_editor", "uid_registar"))
        assertFalse(DefaultPayerHelper.canAssignDefaultPayers(household, "uid_registar", "uid_owner"))
        assertFalse(DefaultPayerHelper.canAssignDefaultPayers(household, "uid_registar", "uid_editor"))

        // 6. Strict Owner-Only Role Change & User Removal (NO self-role change, NO self-removal for EDITOR/REGISTAR)
        assertTrue(DefaultPayerHelper.canModifyRolesOrRemoveUsers(household, "uid_owner"))
        assertFalse(DefaultPayerHelper.canModifyRolesOrRemoveUsers(household, "uid_editor"))
        assertFalse(DefaultPayerHelper.canModifyRolesOrRemoveUsers(household, "uid_registar"))
        assertFalse(DefaultPayerHelper.canModifyRolesOrRemoveUsers(household, "uid_outsider"))
    }

    @Test
    fun `greek voice parser splits equally across multiple preferredDefaultPayers when no explicit payer is spoken`() {
        val withoutPayer = GreekVoiceParser.parse(
            prompt = "12 ευρώ καφές",
            categories = categories,
            allMembers = members,
            preferredDefaultPayers = listOf(marios, vasiliki)
        )
        assertFalse(withoutPayer.hasExplicitPayer)
        assertEquals(marios.id, withoutPayer.payerMemberId)
        assertEquals("Μάριος & Βασιλική", withoutPayer.payerMemberName)
        assertEquals("10,20", withoutPayer.payerMemberIdsCsv)
        assertEquals("10:6.00,20:6.00", withoutPayer.payerAmountsCsv)

        val withExplicitPayer = GreekVoiceParser.parse(
            prompt = "12 ευρώ καφές πλήρωσε η Βασιλική",
            categories = categories,
            allMembers = members,
            preferredDefaultPayers = listOf(marios, vasiliki)
        )
        assertTrue(withExplicitPayer.hasExplicitPayer)
        assertEquals(vasiliki.id, withExplicitPayer.payerMemberId)
        assertEquals("Βασιλική", withExplicitPayer.payerMemberName)
        assertEquals("20", withExplicitPayer.payerMemberIdsCsv)
    }
}
