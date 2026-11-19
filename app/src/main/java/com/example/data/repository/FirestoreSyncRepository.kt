package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.FirebaseCategory
import com.example.data.model.FirebaseExpense
import com.example.data.model.FirebaseHouseholdActivity
import com.example.data.model.FirebaseIncome
import com.example.data.model.FirebaseMember
import com.example.data.model.FirebaseRecurringExpense
import com.example.data.model.FirebaseSavingsGoal
import com.example.data.model.Household
import com.example.data.model.HouseholdRole
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.flow.Flow

class FirestoreSyncRepository(
    private val db: FirebaseFirestore
) {
    private val householdDelegate = FirestoreHouseholdDelegate(db)
    private val userProfileDelegate = FirestoreUserProfileDelegate(db, householdDelegate).also {
        householdDelegate.userProfileDelegate = it
    }
    private val transactionsDelegate = FirestoreTransactionsDelegate(db, householdDelegate)
    private val structureDelegate = FirestoreStructureDelegate(db, householdDelegate)

    sealed class VerifiedUserLookupResult {
        data class Found(val user: UserProfile) : VerifiedUserLookupResult()
        object NotFound : VerifiedUserLookupResult()
        object NotVerified : VerifiedUserLookupResult()
        object SelfEmail : VerifiedUserLookupResult()
    }

    companion object {
        private const val TAG = "FirestoreSyncRepo"

        fun create(context: Context): FirestoreSyncRepository {
            val databaseId = try {
                context.applicationContext.getString(R.string.firestore_database_id)
            } catch (_: Throwable) {
                "(default)"
            }
            val firestoreInstance = if (databaseId.isNotBlank() && databaseId != "(default)") {
                try {
                    FirebaseFirestore.getInstance(databaseId)
                } catch (_: Throwable) {
                    FirebaseFirestore.getInstance()
                }
            } else {
                FirebaseFirestore.getInstance()
            }
            try {
                val diskCacheSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        PersistentCacheSettings.newBuilder()
                            .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                            .build()
                    )
                    .build()
                firestoreInstance.firestoreSettings = diskCacheSettings
            } catch (t: Throwable) {
                Log.w(TAG, "Persistent cache settings could not be set: ${t.message}")
            }
            return FirestoreSyncRepository(firestoreInstance)
        }
    }

    // Household Discovery & Lifecycle
    suspend fun getAllUserHouseholds(uid: String, email: String, excludedHouseholdIds: Set<String> = emptySet()): List<Household> =
        householdDelegate.getAllUserHouseholds(uid, email, excludedHouseholdIds)

    suspend fun discoverUserHouseholds(uid: String, email: String, excludedHouseholdIds: Set<String> = emptySet()): List<String> =
        householdDelegate.discoverUserHouseholds(uid, email, excludedHouseholdIds)

    suspend fun findHouseholdWithExpenses(householdIds: List<String>): String? =
        householdDelegate.findHouseholdWithExpenses(householdIds)

    suspend fun hasExpenses(householdId: String): Boolean =
        householdDelegate.hasExpenses(householdId)

    suspend fun createHousehold(ownerUid: String, exodologioName: String): Household =
        householdDelegate.createHousehold(ownerUid, exodologioName)

    suspend fun ensureHouseholdExists(householdId: String, ownerUid: String, householdName: String = "Το Εξοδολόγιό μου"): Household =
        householdDelegate.ensureHouseholdExists(householdId, ownerUid, householdName)

    suspend fun fetchUserHouseholds(householdIds: List<String>, excludedHouseholdIds: Set<String> = emptySet()): List<Household> =
        householdDelegate.fetchUserHouseholds(householdIds, excludedHouseholdIds)

    suspend fun switchActiveHousehold(uid: String, householdId: String) =
        householdDelegate.switchActiveHousehold(uid, householdId)

    suspend fun renameHousehold(householdId: String, newName: String) =
        householdDelegate.renameHousehold(householdId, newName)

    suspend fun updateHouseholdInviteRole(householdId: String, role: String) =
        householdDelegate.updateHouseholdInviteRole(householdId, role)

    suspend fun joinHouseholdByCode(userUid: String, inviteCode: String, role: String? = null): Result<Household> =
        householdDelegate.joinHouseholdByCode(userUid, inviteCode, role)

    fun observeHousehold(householdId: String): Flow<Household?> =
        householdDelegate.observeHousehold(householdId)

    fun observeUserHouseholds(uid: String, email: String): Flow<List<Household>> =
        householdDelegate.observeUserHouseholds(uid, email)

    suspend fun backfillHouseholdMemberMetadata(householdId: String, emailsByUid: Map<String, String>, namesByUid: Map<String, String>) =
        householdDelegate.backfillHouseholdMemberMetadata(householdId, emailsByUid, namesByUid)

    suspend fun shareHouseholdWithUser(householdId: String, targetUser: UserProfile, role: HouseholdRole): Household? =
        householdDelegate.shareHouseholdWithUser(householdId, targetUser, role)

    suspend fun updateSharedUserRole(householdId: String, targetUid: String, targetEmail: String, newRole: HouseholdRole): Household? =
        householdDelegate.updateSharedUserRole(householdId, targetUid, targetEmail, newRole)

    suspend fun removeSharedUser(householdId: String, targetUid: String, targetEmail: String): Household? =
        householdDelegate.removeSharedUser(householdId, targetUid, targetEmail)

    suspend fun inviteMemberToHousehold(householdId: String, email: String, role: String = "REGISTAR", targetUser: UserProfile? = null) =
        householdDelegate.inviteMemberToHousehold(householdId, email, role, targetUser)

    suspend fun removeMemberFromHousehold(householdId: String, email: String) =
        householdDelegate.removeMemberFromHousehold(householdId, email)

    suspend fun deleteHousehold(householdId: String, userId: String, nextHouseholdId: String? = null) =
        householdDelegate.deleteHousehold(householdId, userId, nextHouseholdId)

    suspend fun leaveSharedHousehold(householdId: String, userId: String, userEmail: String) =
        householdDelegate.leaveSharedHousehold(householdId, userId, userEmail)

    suspend fun deleteUserEntireData(userId: String, householdIds: List<String> = emptyList()) =
        householdDelegate.deleteUserEntireData(userId, householdIds)

    // User Profiles
    suspend fun getOrCreateUserProfile(
        uid: String,
        email: String,
        displayName: String,
        firstName: String = "",
        lastName: String = "",
        birthDate: String = "",
        phoneNumber: String = "",
        occupation: String = "",
        city: String = "",
        authProvider: String = "",
        excludedHouseholdIds: Set<String> = emptySet()
    ): UserProfile = userProfileDelegate.getOrCreateUserProfile(
        uid, email, displayName, firstName, lastName, birthDate, phoneNumber, occupation, city, authProvider, excludedHouseholdIds
    )

    suspend fun getUserProfileOnce(uid: String): UserProfile? =
        userProfileDelegate.getUserProfileOnce(uid)

    suspend fun savePendingUserProfile(profile: UserProfile) =
        userProfileDelegate.savePendingUserProfile(profile)

    suspend fun markEmailVerifiedConfirmed(uid: String) =
        userProfileDelegate.markEmailVerifiedConfirmed(uid)

    suspend fun updateUserAuthProvider(uid: String, provider: String) =
        userProfileDelegate.updateUserAuthProvider(uid, provider)

    suspend fun updateUserProfileName(uid: String, newDisplayName: String) =
        userProfileDelegate.updateUserProfileName(uid, newDisplayName)

    suspend fun updateUserProfilePhoto(uid: String, photoUrl: String) =
        userProfileDelegate.updateUserProfilePhoto(uid, photoUrl)

    suspend fun updateUserProfilePreferences(
        uid: String,
        appLanguage: String? = null,
        themeMode: String? = null,
        reminderEnabled: Boolean? = null,
        reminderHour: Int? = null,
        reminderMinute: Int? = null,
        notificationConsentCompleted: Boolean? = null
    ) = userProfileDelegate.updateUserProfilePreferences(
        uid, appLanguage, themeMode, reminderEnabled, reminderHour, reminderMinute, notificationConsentCompleted
    )

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        userProfileDelegate.observeUserProfile(userId)

    suspend fun updateUserFullProfile(
        uid: String,
        firstName: String,
        lastName: String,
        birthDate: String,
        phoneNumber: String,
        occupation: String,
        city: String
    ): Result<UserProfile> = userProfileDelegate.updateUserFullProfile(uid, firstName, lastName, birthDate, phoneNumber, occupation, city)

    suspend fun findUserByPhoneNumber(phone: String): UserProfile? =
        userProfileDelegate.findUserByPhoneNumber(phone)

    suspend fun findUserByEmail(email: String): UserProfile? =
        userProfileDelegate.findUserByEmail(email)

    suspend fun findVerifiedUserByEmail(
        email: String,
        currentUserUid: String = "",
        currentUserEmail: String = ""
    ): VerifiedUserLookupResult = userProfileDelegate.findVerifiedUserByEmail(email, currentUserUid, currentUserEmail)

    suspend fun findRegisteredUserByEmail(email: String): UserProfile? =
        userProfileDelegate.findUserByEmail(email)

    suspend fun updateUserProfileHouseholdIds(uid: String, householdIds: List<String>) =
        userProfileDelegate.updateUserProfileHouseholdIds(uid, householdIds)

    suspend fun savePasswordResetPhoneCode(phone: String, code: String, userEmail: String) =
        userProfileDelegate.savePasswordResetPhoneCode(phone, code, userEmail)

    suspend fun getPasswordResetPhoneCode(phone: String): Pair<String, String>? =
        userProfileDelegate.getPasswordResetPhoneCode(phone)

    suspend fun setPremiumStatus(uid: String, isPremium: Boolean) =
        userProfileDelegate.setPremiumStatus(uid, isPremium)

    suspend fun resolveUserProfilesByUids(uids: List<String>): Map<String, UserProfile> =
        userProfileDelegate.resolveUserProfilesByUids(uids)

    // Transactions & Shared Activity Feed
    fun observeExpenses(householdId: String): Flow<List<FirebaseExpense>> =
        transactionsDelegate.observeExpenses(householdId)

    suspend fun syncExpense(expense: FirebaseExpense) =
        transactionsDelegate.syncExpense(expense)

    suspend fun deleteExpense(householdId: String, expenseId: String) =
        transactionsDelegate.deleteExpense(householdId, expenseId)

    fun observeUserExpenses(userId: String): Flow<List<FirebaseExpense>> =
        transactionsDelegate.observeUserExpenses(userId)

    suspend fun syncUserExpense(userId: String, expense: FirebaseExpense) =
        transactionsDelegate.syncUserExpense(userId, expense)

    suspend fun deleteUserExpense(userId: String, expenseId: String) =
        transactionsDelegate.deleteUserExpense(userId, expenseId)

    suspend fun clearAllExpenses(householdId: String, userId: String) =
        transactionsDelegate.clearAllExpenses(householdId, userId)

    fun observeIncomes(householdId: String): Flow<List<FirebaseIncome>> =
        transactionsDelegate.observeIncomes(householdId)

    suspend fun syncIncome(income: FirebaseIncome) =
        transactionsDelegate.syncIncome(income)

    suspend fun deleteIncome(householdId: String, incomeId: String) =
        transactionsDelegate.deleteIncome(householdId, incomeId)

    fun observeUserIncomes(userId: String): Flow<List<FirebaseIncome>> =
        transactionsDelegate.observeUserIncomes(userId)

    suspend fun syncUserIncome(userId: String, income: FirebaseIncome) =
        transactionsDelegate.syncUserIncome(userId, income)

    suspend fun deleteUserIncome(userId: String, incomeId: String) =
        transactionsDelegate.deleteUserIncome(userId, incomeId)

    suspend fun enableNetwork() =
        transactionsDelegate.enableNetwork()

    suspend fun getExpensesFromCache(householdId: String): List<FirebaseExpense> =
        transactionsDelegate.getExpensesFromCache(householdId)

    suspend fun getIncomesFromCache(householdId: String): List<FirebaseIncome> =
        transactionsDelegate.getIncomesFromCache(householdId)

    suspend fun getExpensesOnce(householdId: String): List<FirebaseExpense> =
        transactionsDelegate.getExpensesOnceOrNull(householdId) ?: emptyList()

    suspend fun getExpensesOnceOrNull(householdId: String): List<FirebaseExpense>? =
        transactionsDelegate.getExpensesOnceOrNull(householdId)

    suspend fun getIncomesOnce(householdId: String): List<FirebaseIncome> =
        transactionsDelegate.getIncomesOnceOrNull(householdId) ?: emptyList()

    suspend fun getIncomesOnceOrNull(householdId: String): List<FirebaseIncome>? =
        transactionsDelegate.getIncomesOnceOrNull(householdId)

    suspend fun getUserExpensesOnce(userId: String): List<FirebaseExpense> =
        transactionsDelegate.getUserExpensesOnce(userId)

    suspend fun getUserIncomesOnce(userId: String): List<FirebaseIncome> =
        transactionsDelegate.getUserIncomesOnce(userId)

    suspend fun fetchAllHouseholdExpenses(householdIds: List<String>): List<FirebaseExpense> =
        transactionsDelegate.fetchAllHouseholdExpenses(householdIds)

    suspend fun fetchAllHouseholdIncomes(householdIds: List<String>): List<FirebaseIncome> =
        transactionsDelegate.fetchAllHouseholdIncomes(householdIds)

    suspend fun getAllExpensesAcrossAllScopes(householdIds: List<String>, userId: String): List<FirebaseExpense> =
        transactionsDelegate.getAllExpensesAcrossAllScopes(householdIds, userId)

    suspend fun getAllIncomesAcrossAllScopes(householdIds: List<String>, userId: String): List<FirebaseIncome> =
        transactionsDelegate.getAllIncomesAcrossAllScopes(householdIds, userId)

    suspend fun logHouseholdActivity(householdId: String, activity: FirebaseHouseholdActivity) =
        transactionsDelegate.logHouseholdActivity(householdId, activity)

    fun observeHouseholdActivity(householdId: String, limit: Long = 50L): Flow<List<FirebaseHouseholdActivity>> =
        transactionsDelegate.observeHouseholdActivity(householdId, limit)

    // Structure (Seed, Members, Categories, Savings Goals, Recurring Expenses)
    suspend fun seedFirestoreIfEmpty(householdId: String, userId: String, isMarios: Boolean) =
        structureDelegate.seedFirestoreIfEmpty(householdId, userId, isMarios)

    fun observeMembers(householdId: String): Flow<List<FirebaseMember>> =
        structureDelegate.observeMembers(householdId)

    suspend fun syncMember(householdId: String, member: FirebaseMember) =
        structureDelegate.syncMember(householdId, member)

    suspend fun deleteMember(householdId: String, memberId: String) =
        structureDelegate.deleteMember(householdId, memberId)

    suspend fun setMemberDeleted(householdId: String, memberId: String, memberName: String, isDeleted: Boolean) =
        structureDelegate.setMemberDeleted(householdId, memberId, memberName, isDeleted)

    suspend fun permanentDeleteMemberExact(householdId: String, memberId: String, memberName: String) =
        structureDelegate.permanentDeleteMemberExact(householdId, memberId, memberName)

    suspend fun getMembersOnce(householdId: String): List<FirebaseMember> =
        structureDelegate.getMembersOnce(householdId)

    suspend fun getUserMembersOnce(userId: String): List<FirebaseMember> =
        structureDelegate.getUserMembersOnce(userId)

    fun observeUserMembers(userId: String): Flow<List<FirebaseMember>> =
        structureDelegate.observeUserMembers(userId)

    suspend fun syncUserMember(userId: String, member: FirebaseMember) =
        structureDelegate.syncUserMember(userId, member)

    suspend fun deleteUserMember(userId: String, memberId: String) =
        structureDelegate.deleteUserMember(userId, memberId)

    fun observeCategories(householdId: String): Flow<List<FirebaseCategory>> =
        structureDelegate.observeCategories(householdId)

    suspend fun syncCategory(householdId: String, category: FirebaseCategory) =
        structureDelegate.syncCategory(householdId, category)

    suspend fun deleteCategory(householdId: String, categoryId: String) =
        structureDelegate.deleteCategory(householdId, categoryId)

    suspend fun setCategoryDeleted(householdId: String, categoryId: String, categoryName: String, isDeleted: Boolean) =
        structureDelegate.setCategoryDeleted(householdId, categoryId, categoryName, isDeleted)

    suspend fun permanentDeleteCategoryExact(householdId: String, categoryId: String, categoryName: String) =
        structureDelegate.permanentDeleteCategoryExact(householdId, categoryId, categoryName)

    suspend fun getCategoriesOnce(householdId: String): List<FirebaseCategory> =
        structureDelegate.getCategoriesOnce(householdId)

    suspend fun getUserCategoriesOnce(userId: String): List<FirebaseCategory> =
        structureDelegate.getUserCategoriesOnce(userId)

    fun observeUserCategories(userId: String): Flow<List<FirebaseCategory>> =
        structureDelegate.observeUserCategories(userId)

    suspend fun syncUserCategory(userId: String, category: FirebaseCategory) =
        structureDelegate.syncUserCategory(userId, category)

    suspend fun deleteUserCategory(userId: String, categoryId: String) =
        structureDelegate.deleteUserCategory(userId, categoryId)

    fun observeSavingsGoals(householdId: String): Flow<List<FirebaseSavingsGoal>> =
        structureDelegate.observeSavingsGoals(householdId)

    fun observeUserSavingsGoals(userId: String): Flow<List<FirebaseSavingsGoal>> =
        structureDelegate.observeUserSavingsGoals(userId)

    suspend fun getSavingsGoalsOnce(householdId: String): List<FirebaseSavingsGoal> =
        structureDelegate.getSavingsGoalsOnce(householdId)

    suspend fun getUserSavingsGoalsOnce(userId: String): List<FirebaseSavingsGoal> =
        structureDelegate.getUserSavingsGoalsOnce(userId)

    suspend fun syncSavingsGoal(householdId: String, goal: FirebaseSavingsGoal) =
        structureDelegate.syncSavingsGoal(householdId, goal)

    suspend fun syncUserSavingsGoal(userId: String, goal: FirebaseSavingsGoal) =
        structureDelegate.syncUserSavingsGoal(userId, goal)

    suspend fun deleteSavingsGoal(householdId: String, goalId: String, title: String = "") =
        structureDelegate.deleteSavingsGoal(householdId, goalId, title)

    suspend fun deleteUserSavingsGoal(userId: String, goalId: String, title: String = "") =
        structureDelegate.deleteUserSavingsGoal(userId, goalId, title)

    fun observeRecurringExpenses(householdId: String): Flow<List<FirebaseRecurringExpense>> =
        structureDelegate.observeRecurringExpenses(householdId)

    fun observeUserRecurringExpenses(userId: String): Flow<List<FirebaseRecurringExpense>> =
        structureDelegate.observeUserRecurringExpenses(userId)

    suspend fun getRecurringExpensesOnce(householdId: String): List<FirebaseRecurringExpense> =
        structureDelegate.getRecurringExpensesOnce(householdId)

    suspend fun getUserRecurringExpensesOnce(userId: String): List<FirebaseRecurringExpense> =
        structureDelegate.getUserRecurringExpensesOnce(userId)

    suspend fun syncRecurringExpense(householdId: String, recurring: FirebaseRecurringExpense) =
        structureDelegate.syncRecurringExpense(householdId, recurring)

    suspend fun syncUserRecurringExpense(userId: String, recurring: FirebaseRecurringExpense) =
        structureDelegate.syncUserRecurringExpense(userId, recurring)

    suspend fun deleteRecurringExpense(householdId: String, recurringId: String, title: String = "") =
        structureDelegate.deleteRecurringExpense(householdId, recurringId, title)

    suspend fun deleteUserRecurringExpense(userId: String, recurringId: String, title: String = "") =
        structureDelegate.deleteUserRecurringExpense(userId, recurringId, title)

    suspend fun getAllMembersAcrossAllScopes(householdIds: List<String>, userId: String): List<FirebaseMember> =
        structureDelegate.getAllMembersAcrossAllScopes(householdIds, userId)

    suspend fun syncAndCleanMember(householdIds: List<String>, userId: String, member: FirebaseMember, oldName: String = "") =
        structureDelegate.syncAndCleanMember(householdIds, userId, member, oldName)

    suspend fun deleteMemberFromAllScopes(memberId: String, householdIds: List<String>, userId: String, memberName: String = "") =
        structureDelegate.deleteMemberFromAllScopes(memberId, householdIds, userId, memberName)

    suspend fun getAllCategoriesAcrossAllScopes(householdIds: List<String>, userId: String): List<FirebaseCategory> =
        structureDelegate.getAllCategoriesAcrossAllScopes(householdIds, userId)

    suspend fun syncAndCleanCategory(householdIds: List<String>, userId: String, category: FirebaseCategory, oldName: String = "") =
        structureDelegate.syncAndCleanCategory(householdIds, userId, category, oldName)

    suspend fun deleteCategoryFromAllScopes(categoryId: String, householdIds: List<String>, userId: String) =
        structureDelegate.deleteCategoryFromAllScopes(categoryId, householdIds, userId)

    suspend fun batchUpdateMemberSortOrder(householdIds: List<String>, userId: String, members: List<FirebaseMember>) =
        structureDelegate.batchUpdateMemberSortOrder(householdIds, userId, members)

    suspend fun batchUpdateCategorySortOrder(householdIds: List<String>, userId: String, categories: List<FirebaseCategory>) =
        structureDelegate.batchUpdateCategorySortOrder(householdIds, userId, categories)

    suspend fun fetchAllHouseholdMembers(householdIds: List<String>): List<FirebaseMember> =
        structureDelegate.fetchAllHouseholdMembers(householdIds)

    suspend fun fetchAllHouseholdCategories(householdIds: List<String>): List<FirebaseCategory> =
        structureDelegate.fetchAllHouseholdCategories(householdIds)

    suspend fun fetchAllHouseholdSavingsGoals(householdIds: List<String>): List<FirebaseSavingsGoal> =
        structureDelegate.fetchAllHouseholdSavingsGoals(householdIds)

    suspend fun fetchAllHouseholdRecurringExpenses(householdIds: List<String>): List<FirebaseRecurringExpense> =
        structureDelegate.fetchAllHouseholdRecurringExpenses(householdIds)
}
