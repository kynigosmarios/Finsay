package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.FirebaseCategory
import com.example.data.model.FirebaseExpense
import com.example.data.model.FirebaseIncome
import com.example.data.model.FirebaseMember
import com.example.data.model.HouseholdRole
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class HouseholdRbacRulesTest : FirestoreEmulatorTestBase() {

    private suspend fun seedVerifiedUserDoc(uid: String, email: String, name: String, verified: Boolean = true) {
        firestore.collection("users").document(uid).set(
            mapOf(
                "uid" to uid,
                "email" to email,
                "displayName" to name,
                "requiresEmailVerification" to !verified,
                "emailVerifiedConfirmed" to verified,
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    @Test
    fun creator_canFindVerifiedUser_shareWithEditorAndRegistar_andEnforceSubcollectionRules() = runBlocking {
        val creatorEmail = "creator_${UUID.randomUUID().toString().take(6)}@test.com"
        val editorEmail = "editor_${UUID.randomUUID().toString().take(6)}@test.com"
        val registarEmail = "registar_${UUID.randomUUID().toString().take(6)}@test.com"
        val unverifiedEmail = "unverified_${UUID.randomUUID().toString().take(6)}@test.com"

        val editorUid = signInTestUser(editorEmail)
        withTimeout(TIMEOUT_MS) { seedVerifiedUserDoc(editorUid, editorEmail, "Editor User", verified = true) }

        val registarUid = signInTestUser(registarEmail)
        withTimeout(TIMEOUT_MS) { seedVerifiedUserDoc(registarUid, registarEmail, "Registar User", verified = true) }

        val unverifiedUid = signInTestUser(unverifiedEmail)
        withTimeout(TIMEOUT_MS) { seedVerifiedUserDoc(unverifiedUid, unverifiedEmail, "Unverified", verified = false) }

        val creatorUid = signInTestUser(creatorEmail)
        withTimeout(TIMEOUT_MS) { seedVerifiedUserDoc(creatorUid, creatorEmail, "Creator User", verified = true) }

        val repo = FirestoreSyncRepository(firestore)

        // Verified lookup finds verified editor, rejects unverified user
        val foundEditor = withTimeout(TIMEOUT_MS) {
            repo.findVerifiedUserByEmail(editorEmail, creatorUid, creatorEmail)
        }
        assertTrue(foundEditor is FirestoreSyncRepository.VerifiedUserLookupResult.Found)

        val unverifiedRes = withTimeout(TIMEOUT_MS) {
            repo.findVerifiedUserByEmail(unverifiedEmail, creatorUid, creatorEmail)
        }
        assertTrue(unverifiedRes is FirestoreSyncRepository.VerifiedUserLookupResult.NotVerified)

        val resolvedProfiles = withTimeout(TIMEOUT_MS) {
            repo.resolveUserProfilesByUids(listOf(editorUid, registarUid))
        }
        assertEquals(editorEmail, resolvedProfiles[editorUid]?.email)
        assertEquals(registarEmail, resolvedProfiles[registarUid]?.email)

        // Creator creates household and shares with Editor & Registar
        val household = withTimeout(TIMEOUT_MS) { repo.createHousehold(creatorUid, "Shared Test") }
        val editorProfile = (foundEditor as FirestoreSyncRepository.VerifiedUserLookupResult.Found).user
        withTimeout(TIMEOUT_MS) { repo.shareHouseholdWithUser(household.id, editorProfile, HouseholdRole.EDITOR) }
        withTimeout(TIMEOUT_MS) {
            repo.shareHouseholdWithUser(
                household.id,
                UserProfile(uid = registarUid, email = registarEmail, displayName = "Registar User"),
                HouseholdRole.REGISTAR
            )
        }

        // Editor signs in: sees shared household, can write Expense, Income, Member, Category
        signInTestUser(editorEmail)
        val editorHouseholds = withTimeout(TIMEOUT_MS) { repo.getAllUserHouseholds(editorUid, editorEmail) }
        assertTrue(editorHouseholds.any { it.id == household.id })

        withTimeout(TIMEOUT_MS) {
            repo.syncExpense(
                FirebaseExpense(
                    id = "exp_ed_1",
                    householdId = household.id,
                    title = "Editor Expense",
                    amount = 15.0,
                    timestamp = System.currentTimeMillis(),
                    createdByUid = editorUid
                )
            )
            repo.syncMember(
                household.id,
                FirebaseMember(id = "mem_ed_1", householdId = household.id, userId = editorUid, name = "Editor Member")
            )
            repo.syncCategory(
                household.id,
                FirebaseCategory(id = "cat_ed_1", householdId = household.id, userId = editorUid, name = "Editor Cat")
            )
        }

        // Registar signs in: sees shared household, can write Expense & Income, CANNOT write Member or Category
        signInTestUser(registarEmail)
        val registarHouseholds = withTimeout(TIMEOUT_MS) { repo.getAllUserHouseholds(registarUid, registarEmail) }
        assertTrue(registarHouseholds.any { it.id == household.id })

        withTimeout(TIMEOUT_MS) {
            repo.syncExpense(
                FirebaseExpense(
                    id = "exp_reg_1",
                    householdId = household.id,
                    title = "Registar Expense",
                    amount = 10.0,
                    timestamp = System.currentTimeMillis(),
                    createdByUid = registarUid
                )
            )
            repo.syncIncome(
                FirebaseIncome(
                    id = "inc_reg_1",
                    householdId = household.id,
                    title = "Registar Income",
                    amount = 100.0,
                    timestamp = System.currentTimeMillis(),
                    createdByUid = registarUid
                )
            )
        }

        var memberDenied = false
        try {
            withTimeout(TIMEOUT_MS) {
                firestore.collection("households").document(household.id)
                    .collection("members").document("mem_bad")
                    .set(mapOf("name" to "Bad Member")).await()
            }
        } catch (e: Exception) {
            memberDenied = e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Registar must be denied member creation by Firestore rules", memberDenied)

        var categoryDenied = false
        try {
            withTimeout(TIMEOUT_MS) {
                firestore.collection("households").document(household.id)
                    .collection("categories").document("cat_bad")
                    .set(mapOf("name" to "Bad Category")).await()
            }
        } catch (e: Exception) {
            categoryDenied = e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Registar must be denied category creation by Firestore rules", categoryDenied)

        // Unauthenticated caller must be denied
        auth.signOut()
        var unauthDenied = false
        try {
            withTimeout(TIMEOUT_MS) {
                firestore.collection("households").document(household.id).get().await()
            }
        } catch (e: Exception) {
            unauthDenied = e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Unauthenticated read must fail with PERMISSION_DENIED", unauthDenied)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, FirebaseFirestoreException.Code.PERMISSION_DENIED)
    }

    private companion object {
        const val TIMEOUT_MS = 5000L
    }
}
