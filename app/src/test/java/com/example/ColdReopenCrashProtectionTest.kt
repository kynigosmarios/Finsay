package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.billing.BillingManager
import com.example.data.model.Household
import com.example.data.model.HouseholdRole
import com.example.data.model.mapCategoryFromFields
import com.example.data.model.mapExpenseFromFields
import com.example.data.model.mapHouseholdFromFields
import com.example.data.model.mapIncomeFromFields
import com.example.data.model.mapMemberFromFields
import com.example.data.model.mapRecurringExpenseFromFields
import com.example.data.model.mapSavingsGoalFromFields
import com.example.data.model.mapUserProfileFromFields
import com.example.ui.viewmodel.OikoNomosViewModel
import com.example.util.BankNotificationSystemManager
import com.example.util.StartupSafety
import com.google.firebase.FirebaseApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ColdReopenCrashProtectionTest {

    @Test
    fun `pure Firestore mappers safely coerce mixed and malformed field types`() {
        val sampleDate = Date(1_700_000_000_000L)

        val expenseFields = mapOf<String, Any?>(
            "id" to "",
            "householdId" to "3e9a25a0-2e7",
            "title" to "Σούπερ μάρκετ",
            "amount" to "42,50",
            "categoryId" to "3",
            "payerMemberId" to 7,
            "createdAt" to sampleDate
        )
        val expense = mapExpenseFromFields("exp_doc_1") { expenseFields[it] }
        assertEquals("exp_doc_1", expense.id)
        assertEquals("3e9a25a0-2e7", expense.householdId)
        assertEquals(42.50, expense.amount, 0.001)
        assertEquals(3L, expense.categoryId)
        assertEquals(7L, expense.payerMemberId)
        assertEquals(1_700_000_000_000L, expense.timestamp)

        val nanExpense = mapExpenseFromFields("exp_nan") { field ->
            if (field == "amount") Double.NaN else null
        }
        assertEquals(0.0, nanExpense.amount, 0.0)

        val memberFields = mapOf<String, Any?>(
            "name" to "Μαρία",
            "default" to "yes",
            "isDeleted" to 1L,
            "sortOrder" to "2"
        )
        val member = mapMemberFromFields("mem_1") { memberFields[it] }
        assertEquals("mem_1", member.id)
        assertTrue(member.isDefault)
        assertTrue(member.isDeleted)
        assertEquals(2, member.sortOrder)

        val incomeFields = mapOf<String, Any?>(
            "title" to "Μισθός",
            "amount" to "1250,75",
            "memberId" to "4",
            "timestamp" to "1700000001000"
        )
        val income = mapIncomeFromFields("inc_1") { incomeFields[it] }
        assertEquals(1250.75, income.amount, 0.001)
        assertEquals(4L, income.memberId)
        assertEquals(1_700_000_001_000L, income.timestamp)

        val categoryFields = mapOf<String, Any?>(
            "name" to "Κατοικία",
            "monthlyBudget" to "350.5",
            "sortOrder" to 5L,
            "isDeleted" to "false"
        )
        val category = mapCategoryFromFields("cat_1") { categoryFields[it] }
        assertEquals(350.5, category.monthlyBudget, 0.001)
        assertEquals(5, category.sortOrder)
        assertFalse(category.isDeleted)

        val householdFields = mapOf<String, Any?>(
            "name" to "Οικογένεια",
            "memberUids" to listOf("owner_uid", "wife_uid", 123, null),
            "invitedEmails" to listOf("wife@example.com", null),
            "memberRoles" to mapOf("wife_uid" to "REGISTAR", "owner_uid" to 42),
            "isPremium" to 1
        )
        val household = mapHouseholdFromFields("3e9a25a0-2e7") { householdFields[it] }
        assertEquals("3e9a25a0-2e7", household.id)
        assertTrue(household.isPremium)
        assertEquals(listOf("owner_uid", "wife_uid", "123"), household.memberUids)
        assertEquals("REGISTAR", household.memberRoles["wife_uid"])
        assertEquals("42", household.memberRoles["owner_uid"])

        val profileFields = mapOf<String, Any?>(
            "email" to "wife@example.com",
            "householdId" to "3e9a25a0-2e7",
            "householdIds" to listOf("3e9a25a0-2e7", 99, null, true),
            "isPremium" to "true",
            "reminderEnabled" to 0L,
            "reminderHour" to 99,
            "reminderMinute" to -5
        )
        val profile = mapUserProfileFromFields("wife_uid") { profileFields[it] }
        assertEquals("wife_uid", profile.uid)
        assertTrue(profile.isPremium)
        assertFalse(profile.reminderEnabled)
        assertEquals(23, profile.reminderHour)
        assertEquals(0, profile.reminderMinute)
        assertEquals(listOf("3e9a25a0-2e7", "99", "true"), profile.householdIds)

        val goalFields = mapOf<String, Any?>(
            "title" to "Διακοπές",
            "targetAmount" to "2000,00",
            "currentAmount" to 500
        )
        val goal = mapSavingsGoalFromFields("goal_1") { goalFields[it] }
        assertEquals(2000.0, goal.targetAmount, 0.001)
        assertEquals(500.0, goal.currentAmount, 0.001)

        val recurringFields = mapOf<String, Any?>(
            "title" to "Ρεύμα",
            "amount" to "89,90",
            "dayOfMonth" to "15",
            "reminderHour" to 45,
            "reminderMinute" to -10,
            "active" to "true"
        )
        val recurring = mapRecurringExpenseFromFields("rec_1") { recurringFields[it] }
        assertEquals(89.90, recurring.amount, 0.001)
        assertEquals(15, recurring.dayOfMonth)
        assertEquals(23, recurring.reminderHour)
        assertEquals(0, recurring.reminderMinute)
        assertTrue(recurring.isActive)
    }

    @Test
    fun `pure Firestore mappers never throw even when underlying snapshot accessor throws`() {
        val throwingAccessor: (String) -> Any? = {
            throw RuntimeException("Simulated corrupted Firestore cache field")
        }
        val throwingAssertionAccessor: (String) -> Any? = {
            throw AssertionError("Simulated fatal cache assertion")
        }

        assertEquals("e1", mapExpenseFromFields("e1", getField = throwingAccessor).id)
        assertEquals("m1", mapMemberFromFields("m1", getField = throwingAssertionAccessor).id)
        assertEquals("i1", mapIncomeFromFields("i1", getField = throwingAccessor).id)
        assertEquals("c1", mapCategoryFromFields("c1", getField = throwingAssertionAccessor).id)
        assertEquals("h1", mapHouseholdFromFields("h1", getField = throwingAccessor).id)
        assertEquals("u1", mapUserProfileFromFields("u1", getField = throwingAssertionAccessor).uid)
        assertEquals("g1", mapSavingsGoalFromFields("g1", getField = throwingAccessor).id)
        assertEquals("r1", mapRecurringExpenseFromFields("r1", getField = throwingAssertionAccessor).id)
    }

    @Test
    fun `persisted cached_user_household_ids preserves shared REGISTAR household on cold reopen`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("oikonomos_app_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet("cached_user_household_ids", setOf("3e9a25a0-2e7", "household_wife"))
            .commit()

        val restoredIds = prefs.getStringSet("cached_user_household_ids", emptySet()).orEmpty()
        val mariosDefaults = setOf("3e9a25a0-2e7", "e1d4ae67-499")

        assertTrue(
            StartupSafety.isHouseholdAllowedForUser(
                candidateHid = "3e9a25a0-2e7",
                isMarios = false,
                mariosDefaultIds = mariosDefaults,
                explicitlyKnownHids = restoredIds
            )
        )

        val sharedHousehold = Household(
            id = "3e9a25a0-2e7",
            name = "Οικογένεια Μάριος",
            ownerId = "marios_uid",
            memberUids = listOf("marios_uid", "wife_uid"),
            memberRoles = mapOf(
                "marios_uid" to HouseholdRole.CREATOR.key,
                "wife_uid" to HouseholdRole.REGISTAR.key
            )
        )
        val role = StartupSafety.resolveHouseholdRole(
            household = sharedHousehold,
            userUid = "wife_uid",
            userEmail = "wife@example.com",
            isMarios = false,
            mariosDefaultIds = mariosDefaults
        )
        assertEquals(HouseholdRole.REGISTAR, role)
        assertFalse(role.canManageMembersAndCategories)
    }

    @Test
    fun `BillingManager and BankNotificationSystemManager remain crash-free when services or permissions are unavailable`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val billingManager = BillingManager(context) {}
        assertNotNull(billingManager)

        billingManager.startConnection()
        billingManager.queryAllProducts()
        billingManager.queryActivePurchases()
        billingManager.endConnection()

        BankNotificationSystemManager.init(context)
        BankNotificationSystemManager.cancelNotification(context, "missing_tx_id")
    }

    @Test
    fun `OikoNomosViewModel initializes all StateFlows and profile fields before init on cold reopen`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        if (FirebaseApp.getApps(app).isEmpty()) {
            FirebaseApp.initializeApp(app)
        }
        val prefs = app.getSharedPreferences("oikonomos_app_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("cached_active_uid", "wife_uid_123")
            .putString("cached_user_uid", "wife_uid_123")
            .putString("cached_user_email", "wife@example.com")
            .putString("cached_user_household_id", "3e9a25a0-2e7")
            .putStringSet("cached_user_household_ids", setOf("3e9a25a0-2e7"))
            .putString("last_household_id", "3e9a25a0-2e7")
            .putBoolean("onboarding_shown_v3", true)
            .putBoolean("has_entered_any_expense", true)
            .commit()

        val viewModel = OikoNomosViewModel(app)

        // Ensure JVM declaration order guarantees _userProfile precedes _isUserVerifiedState
        val declaredFields = OikoNomosViewModel::class.java.declaredFields
        val userProfileIdx = declaredFields.indexOfFirst { it.name == "_userProfile" }
        val verifiedStateIdx = declaredFields.indexOfFirst { it.name == "_isUserVerifiedState" }
        assertTrue("_userProfile field must exist", userProfileIdx >= 0)
        assertTrue("_isUserVerifiedState field must exist", verifiedStateIdx >= 0)
        assertTrue(
            "_userProfile must be declared before _isUserVerifiedState in JVM class init order",
            userProfileIdx < verifiedStateIdx
        )

        // Ensure every StateFlow/cache field is non-null immediately after construction
        val nullableOptionalFieldNames = setOf(
            "roomClearJob",
            "loadProfileJob",
            "activeLoadProfileUid",
            "householdJob",
            "expensesSyncJob",
            "userExpensesSyncJob",
            "incomesSyncJob",
            "userIncomesSyncJob",
            "membersSyncJob",
            "userMembersSyncJob",
            "categoriesSyncJob",
            "userCategoriesSyncJob",
            "savingsGoalsSyncJob",
            "userSavingsGoalsSyncJob",
            "recurringSyncJob",
            "userRecurringSyncJob",
            "userProfileSyncJob",
            "userHouseholdsSyncJob",
            "countdownJob",
            "memberReorderJob",
            "categoryReorderJob",
            "lastObservedHouseholdId"
        )
        for (field in declaredFields) {
            if (field.name in nullableOptionalFieldNames) continue
            if (field.type.isPrimitive) continue
            field.isAccessible = true
            assertNotNull(
                "Field ${field.name} must not be null after OikoNomosViewModel construction",
                field.get(viewModel)
            )
        }

        assertFalse(viewModel.isUserVerified(null))
        assertEquals("", viewModel.getOrEnsureHouseholdId(null))
    }

    @Test
    fun `MainActivity launches cleanly under cold reopen persisted state`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        if (FirebaseApp.getApps(app).isEmpty()) {
            FirebaseApp.initializeApp(app)
        }
        val prefs = app.getSharedPreferences("oikonomos_app_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("cached_active_uid", "wife_uid_123")
            .putString("last_household_id", "3e9a25a0-2e7")
            .putBoolean("onboarding_shown_v3", true)
            .commit()

        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)
        controller.pause().stop().destroy()
    }
}

