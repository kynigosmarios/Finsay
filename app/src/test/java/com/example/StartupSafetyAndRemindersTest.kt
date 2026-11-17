package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.entity.Expense
import com.example.data.entity.RecurringExpense
import com.example.data.model.FirebaseExpense
import com.example.data.model.FirebaseIncome
import com.example.data.model.Household
import com.example.data.model.HouseholdRole
import com.example.reminder.BillReminderManager
import com.example.reminder.DailyReminderManager
import com.example.util.BankNotificationSystemManager
import com.example.util.PendingBankTransaction
import com.example.util.StartupSafety
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StartupSafetyAndRemindersTest {

    private val mariosDefaults = setOf("3e9a25a0-2e7", "e1d4ae67-499")

    @Test
    fun `safePositiveIdHash always yields positive id including Int MIN_VALUE edge cases`() {
        val trickyInputs = listOf("", "exp_0", "polygenelubricants", "GydZG_", "0", "-1")
        trickyInputs.forEach { raw ->
            val id = StartupSafety.safePositiveIdHash(raw)
            assertTrue("Expected positive ID for '$raw', got $id", id > 0L)
        }
    }

    @Test
    fun `deduplicateExpenseIds eliminates zero and duplicate LazyColumn keys`() {
        val inputs = listOf(
            Expense(id = 0L, cloudId = "a", title = "A", amount = 10.0, categoryId = 1L, categoryName = "Σούπερ μάρκετ"),
            Expense(id = 0L, cloudId = "b", title = "B", amount = 20.0, categoryId = 1L, categoryName = "Σούπερ μάρκετ"),
            Expense(id = 5L, cloudId = "c", title = "C", amount = 30.0, categoryId = 1L, categoryName = "Σούπερ μάρκετ"),
            Expense(id = 5L, cloudId = "d", title = "D", amount = 40.0, categoryId = 1L, categoryName = "Σούπερ μάρκετ"),
            Expense(id = 1L, cloudId = "e", title = "E", amount = 50.0, categoryId = 1L, categoryName = "Σούπερ μάρκετ")
        )
        val result = StartupSafety.deduplicateExpenseIds(inputs)
        assertEquals(inputs.size, result.size)
        assertTrue(result.all { it.id > 0L })
        assertEquals(inputs.size, result.map { it.id }.toSet().size)
    }

    @Test
    fun `canSafelyWriteRemoteStructure blocks null household and REGISTAR while allowing EDITOR and CREATOR`() {
        val sharedHousehold = Household(
            id = "3e9a25a0-2e7",
            name = "Οικογένεια Μάριος",
            ownerId = "owner_uid",
            memberUids = listOf("owner_uid", "wife_uid", "editor_uid"),
            memberRoles = mapOf(
                "owner_uid" to HouseholdRole.CREATOR.key,
                "wife_uid" to HouseholdRole.REGISTAR.key,
                "editor_uid" to HouseholdRole.EDITOR.key
            )
        )

        // Prewarm/reopen race before household finishes loading must never trigger remote writes
        assertFalse(
            StartupSafety.canSafelyWriteRemoteStructure(
                targetHousehold = null,
                userUid = "wife_uid",
                userEmail = "wife@example.com",
                isMarios = false,
                mariosDefaultIds = mariosDefaults
            )
        )

        // Wife (REGISTAR) cannot write members or categories remotely -> prevents PERMISSION_DENIED crash
        assertFalse(
            StartupSafety.canSafelyWriteRemoteStructure(
                targetHousehold = sharedHousehold,
                userUid = "wife_uid",
                userEmail = "wife@example.com",
                isMarios = false,
                mariosDefaultIds = mariosDefaults
            )
        )

        // Editor and Creator can manage members and categories
        assertTrue(
            StartupSafety.canSafelyWriteRemoteStructure(
                targetHousehold = sharedHousehold,
                userUid = "editor_uid",
                userEmail = "editor@example.com",
                isMarios = false,
                mariosDefaultIds = mariosDefaults
            )
        )
        assertTrue(
            StartupSafety.canSafelyWriteRemoteStructure(
                targetHousehold = sharedHousehold,
                userUid = "owner_uid",
                userEmail = "owner@example.com",
                isMarios = false,
                mariosDefaultIds = mariosDefaults
            )
        )
    }

    @Test
    fun `isActualUserSwitch handles reopen with uncached prefs uid without false wipe`() {
        assertFalse(StartupSafety.isActualUserSwitch(activeUidInPrefs = null, currentUid = "wife_uid"))
        assertFalse(StartupSafety.isActualUserSwitch(activeUidInPrefs = "", currentUid = "wife_uid"))
        assertFalse(StartupSafety.isActualUserSwitch(activeUidInPrefs = "wife_uid", currentUid = "wife_uid"))

        assertTrue(StartupSafety.isActualUserSwitch(activeUidInPrefs = "marios_uid", currentUid = "wife_uid"))
        assertTrue(StartupSafety.isActualUserSwitch(activeUidInPrefs = "wife_uid", currentUid = null))
    }

    @Test
    fun `isHouseholdAllowedForUser permits shared Marios household only when explicitly shared with non-Marios user`() {
        assertTrue(
            StartupSafety.isHouseholdAllowedForUser(
                candidateHid = "3e9a25a0-2e7",
                isMarios = false,
                mariosDefaultIds = mariosDefaults,
                explicitlyKnownHids = setOf("3e9a25a0-2e7", "household_wife")
            )
        )
        assertFalse(
            StartupSafety.isHouseholdAllowedForUser(
                candidateHid = "3e9a25a0-2e7",
                isMarios = false,
                mariosDefaultIds = mariosDefaults,
                explicitlyKnownHids = setOf("household_other")
            )
        )
    }

    @Test
    fun `resolveAuthoritativeExpenses and Incomes never resurrect deleted shared records from user mirror`() {
        val hid = "3e9a25a0-2e7"
        val creatorExp1 = FirebaseExpense(id = "exp_creator_1", householdId = hid, title = "Έξοδο Μάριου 1", amount = 25.0, timestamp = 2000L)
        val creatorExp2 = FirebaseExpense(id = "exp_creator_2", householdId = hid, title = "Έξοδο Μάριου 2", amount = 15.0, timestamp = 1500L)
        val deletedTestExpenseInWifeMirror = FirebaseExpense(id = "exp_wife_test", householdId = hid, title = "test έξοδο", amount = 10.0, timestamp = 3000L)

        // Household collection no longer has exp_wife_test (Creator deleted it),
        // even though Wife's /users/{wifeUid}/expenses still has a copy.
        val resolved = StartupSafety.resolveAuthoritativeExpenses(
            activeHouseholdId = hid,
            householdExpenses = listOf(creatorExp1, creatorExp2),
            userExpenses = listOf(deletedTestExpenseInWifeMirror),
            totalHouseholdCount = 2
        )
        assertEquals(listOf("exp_creator_1", "exp_creator_2"), resolved.map { it.id })

        // Even when the LAST expense in the household is deleted, it stays empty and does NOT resurrect
        val resolvedAfterAllDeleted = StartupSafety.resolveAuthoritativeExpenses(
            activeHouseholdId = hid,
            householdExpenses = emptyList(),
            userExpenses = listOf(deletedTestExpenseInWifeMirror),
            totalHouseholdCount = 2
        )
        assertTrue(resolvedAfterAllDeleted.isEmpty())

        // Same invariant holds for shared incomes
        val keptIncome = FirebaseIncome(id = "inc_1", householdId = hid, title = "Μισθός", amount = 1000.0, timestamp = 2000L)
        val deletedIncomeInMirror = FirebaseIncome(id = "inc_deleted", householdId = hid, title = "test έσοδο", amount = 50.0, timestamp = 3000L)
        val resolvedIncomes = StartupSafety.resolveAuthoritativeIncomes(
            activeHouseholdId = hid,
            householdIncomes = listOf(keptIncome),
            userIncomes = listOf(deletedIncomeInMirror),
            totalHouseholdCount = 2
        )
        assertEquals(listOf("inc_1"), resolvedIncomes.map { it.id })

        // When no active household exists (blank hid), personal user expenses are used
        val fallbackNoHid = StartupSafety.resolveAuthoritativeExpenses(
            activeHouseholdId = "",
            householdExpenses = emptyList(),
            userExpenses = listOf(deletedTestExpenseInWifeMirror),
            totalHouseholdCount = 0
        )
        assertEquals(listOf("exp_wife_test"), fallbackNoHid.map { it.id })
    }

    @Test
    fun `findStaleUserMirrorIds identifies orphaned mirror docs while preserving pending writes and other households`() {
        val staleIds = StartupSafety.findStaleUserMirrorIds(
            activeHouseholdId = "3e9a25a0-2e7",
            authoritativeIds = setOf("exp_creator_1", "exp_creator_2"),
            userMirrorEntries = listOf(
                "exp_creator_1" to "3e9a25a0-2e7",
                "exp_wife_test" to "3e9a25a0-2e7",
                "exp_pending" to "3e9a25a0-2e7",
                "exp_other_household" to "household_wife"
            ),
            pendingLocalIds = setOf("exp_pending")
        )
        assertEquals(listOf("exp_wife_test"), staleIds)
    }

    @Test
    fun `pruneExpiredPendingIds clears confirmed and timed-out pending local IDs immediately`() {
        val pending = mutableMapOf(
            "confirmed_by_server" to 10_000L,
            "expired_pending" to 1_000L,
            "still_in_flight" to 19_000L
        )
        val remaining = StartupSafety.pruneExpiredPendingIds(
            pendingIds = pending,
            confirmedRemoteIds = setOf("confirmed_by_server"),
            nowMs = 20_000L,
            ttlMs = 15_000L
        )
        assertEquals(setOf("still_in_flight"), remaining)
    }

    @Test
    fun `reminder and bank notification managers do not throw when notifications or exact alarms are denied`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        DailyReminderManager.scheduleDailyReminder(context, hour = 22, minute = 30)
        DailyReminderManager.showNotification(context)
        DailyReminderManager.cancelDailyReminder(context)

        val recurring = RecurringExpense(
            id = 42L,
            title = "Ρεύμα",
            amount = 85.0,
            categoryId = 3L,
            categoryName = "Λογαριασμοί & Σπίτι",
            dayOfMonth = 15,
            reminderHour = 10,
            reminderMinute = 0,
            isActive = true
        )
        BillReminderManager.scheduleReminder(context, recurring)
        BillReminderManager.showBillNotification(context, 42L, "Ρεύμα", 85.0, 15)
        BillReminderManager.cancelReminder(context, 42L)

        BankNotificationSystemManager.init(context)
        BankNotificationSystemManager.showNotification(
            context,
            PendingBankTransaction(
                id = "tx_1",
                bankName = "Eurobank",
                merchantName = "Σκλαβενίτης",
                amount = 24.50
            )
        )
        BankNotificationSystemManager.cancelNotification(context, "tx_1")
    }
}
