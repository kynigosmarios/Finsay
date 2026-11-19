package com.example.data.repository

import android.util.Log
import com.example.data.model.FirebaseHouseholdActivity
import com.example.data.model.UserProfile
import com.example.data.model.safeBoolean
import com.example.data.model.safeString
import com.example.data.model.toSafeUserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

internal class FirestoreUserProfileDelegate(
    private val db: FirebaseFirestore,
    private val householdDelegate: FirestoreHouseholdDelegate
) {
    companion object {
        private const val TAG = "FirestoreSyncRepo"
    }

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
    ): UserProfile {
        val userRef = db.collection("users").document(uid)
        val snapshot = userRef.get().await()

        val discoveredHids = householdDelegate.discoverUserHouseholds(uid, email, excludedHouseholdIds)

        return if (snapshot.exists()) {
            val existing = try {
                snapshot.toSafeUserProfile()
            } catch (_: Throwable) {
                null
            } ?: UserProfile(uid = uid, email = email, displayName = displayName)

            val profileDisplayName = if (existing.displayName.isNotBlank()) existing.displayName else displayName.ifBlank { "Χρήστης Finsay" }
            val derivedFirst = if (existing.firstName.isNotBlank()) {
                existing.firstName
            } else if (firstName.isNotBlank()) {
                firstName
            } else {
                profileDisplayName.split(" ").firstOrNull() ?: ""
            }

            val derivedLast = if (existing.lastName.isNotBlank()) {
                existing.lastName
            } else if (lastName.isNotBlank()) {
                lastName
            } else {
                val parts = profileDisplayName.split(" ")
                if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
            }

            val effectiveAuthProvider = if (authProvider.isNotBlank()) {
                authProvider
            } else if (existing.authProvider.isNotBlank()) {
                existing.authProvider
            } else "password"

            val rawHids = (existing.householdIds + listOf(existing.householdId) + discoveredHids)
                .filter { it.isNotBlank() && !excludedHouseholdIds.contains(it) }
                .distinct()
            val allHids = rawHids
            val cleanExistingHid = existing.householdId

            val bestActiveHid = if (cleanExistingHid.isNotBlank()) {
                val currentHasExp = try {
                    !db.collection("households").document(cleanExistingHid).collection("expenses").limit(1).get().await().isEmpty
                } catch (_: Exception) { false }
                if (currentHasExp) {
                    cleanExistingHid
                } else {
                    householdDelegate.findHouseholdWithExpenses(allHids) ?: cleanExistingHid
                }
            } else {
                householdDelegate.findHouseholdWithExpenses(allHids) ?: allHids.firstOrNull() ?: ""
            }

            val finalHouseholdId = if (bestActiveHid.isNotBlank()) {
                bestActiveHid
            } else if (allHids.isNotEmpty()) {
                allHids.first()
            } else {
                val newHousehold = householdDelegate.createHousehold(uid, "Το Εξοδολόγιό μου")
                newHousehold.id
            }

            val finalHouseholdIds = (allHids + finalHouseholdId).filter { it.isNotBlank() }.distinct()

            val merged = existing.copy(
                email = (if (email.isNotBlank()) email else existing.email).trim(),
                displayName = profileDisplayName,
                firstName = derivedFirst,
                lastName = derivedLast,
                birthDate = if (existing.birthDate.isNotBlank()) existing.birthDate else birthDate,
                phoneNumber = if (existing.phoneNumber.isNotBlank()) existing.phoneNumber else phoneNumber,
                occupation = if (existing.occupation.isNotBlank()) existing.occupation else occupation,
                city = if (existing.city.isNotBlank()) existing.city else city,
                householdId = finalHouseholdId,
                householdIds = finalHouseholdIds,
                authProvider = effectiveAuthProvider,
                requiresEmailVerification = existing.requiresEmailVerification,
                emailVerifiedConfirmed = existing.emailVerifiedConfirmed
            )

            userRef.set(merged, SetOptions.merge()).await()
            merged
        } else {
            val allDiscovered = discoveredHids.filter { it.isNotBlank() }.distinct()
            val (initialHouseholdId, initialHouseholdIds) = if (allDiscovered.isNotEmpty()) {
                val withExp = householdDelegate.findHouseholdWithExpenses(allDiscovered) ?: allDiscovered.first()
                Pair(withExp, allDiscovered)
            } else {
                val newHousehold = householdDelegate.createHousehold(uid, "Το Εξοδολόγιό μου")
                Pair(newHousehold.id, listOf(newHousehold.id))
            }

            val profileDisplayName = displayName.ifBlank {
                if (firstName.isNotBlank() || lastName.isNotBlank()) "$firstName $lastName".trim() else "Χρήστης Finsay"
            }
            val derivedFirst = if (firstName.isNotBlank()) firstName else profileDisplayName.split(" ").firstOrNull().orEmpty()
            val derivedLast = if (lastName.isNotBlank()) lastName else {
                val parts = profileDisplayName.split(" ")
                if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
            }

            val newProfile = UserProfile(
                uid = uid,
                email = email,
                displayName = profileDisplayName,
                firstName = derivedFirst,
                lastName = derivedLast,
                birthDate = birthDate,
                phoneNumber = phoneNumber,
                occupation = occupation,
                city = city,
                householdId = initialHouseholdId,
                householdIds = initialHouseholdIds,
                isPremium = false,
                authProvider = authProvider.ifBlank { "password" },
                createdAt = Timestamp.now()
            )
            userRef.set(newProfile, SetOptions.merge()).await()
            newProfile
        }
    }

    suspend fun getUserProfileOnce(uid: String): UserProfile? {
        if (uid.isBlank()) return null
        return try {
            val snapshot = db.collection("users").document(uid).get().await()
            if (!snapshot.exists()) return null
            snapshot.toSafeUserProfile()
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.w(TAG, "getUserProfileOnce error: ${t.message}")
            null
        }
    }

    suspend fun savePendingUserProfile(profile: UserProfile) {
        if (profile.uid.isBlank()) return
        db.collection("users").document(profile.uid).set(profile, SetOptions.merge()).await()
    }

    suspend fun markEmailVerifiedConfirmed(uid: String) {
        if (uid.isBlank()) return
        db.collection("users").document(uid).set(
            mapOf(
                "emailVerifiedConfirmed" to true,
                "updatedAt" to Timestamp.now()
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun updateUserAuthProvider(uid: String, provider: String) {
        if (uid.isBlank()) return
        db.collection("users").document(uid).set(mapOf("authProvider" to provider), SetOptions.merge()).await()
    }

    suspend fun updateUserProfileName(uid: String, newDisplayName: String) {
        if (uid.isBlank() || newDisplayName.isBlank()) return
        db.collection("users").document(uid).set(
            mapOf("displayName" to newDisplayName.trim()),
            SetOptions.merge()
        ).await()
    }

    suspend fun updateUserProfilePhoto(uid: String, photoUrl: String) {
        if (uid.isBlank()) return
        db.collection("users").document(uid).set(
            mapOf("photoUrl" to photoUrl),
            SetOptions.merge()
        ).await()
    }

    suspend fun updateUserProfilePreferences(
        uid: String,
        appLanguage: String? = null,
        themeMode: String? = null,
        reminderEnabled: Boolean? = null,
        reminderHour: Int? = null,
        reminderMinute: Int? = null,
        notificationConsentCompleted: Boolean? = null
    ) {
        if (uid.isBlank()) return
        val updates = mutableMapOf<String, Any>()
        if (appLanguage != null) updates["appLanguage"] = appLanguage
        if (themeMode != null) updates["themeMode"] = themeMode
        if (reminderEnabled != null) updates["reminderEnabled"] = reminderEnabled
        if (reminderHour != null) updates["reminderHour"] = reminderHour
        if (reminderMinute != null) updates["reminderMinute"] = reminderMinute
        if (notificationConsentCompleted != null) updates["notificationConsentCompleted"] = notificationConsentCompleted
        updates["updatedAt"] = Timestamp.now()
        try {
            db.collection("users").document(uid).set(updates, SetOptions.merge()).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Error updating profile preferences: ${e.message}")
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        if (userId.isBlank()) return flowOf(null)
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                try {
                    if (snapshot.exists()) {
                        val p = snapshot.toSafeUserProfile()
                        if (p.uid.isBlank()) p.uid = snapshot.id
                        p
                    } else null
                } catch (t: Throwable) {
                    Log.w(TAG, "observeUserProfile map warning: ${t.message}")
                    null
                }
            }
            .catch { t ->
                Log.w(TAG, "observeUserProfile flow error: ${t.message}")
                emit(null)
            }
    }

    suspend fun updateUserFullProfile(
        uid: String,
        firstName: String,
        lastName: String,
        birthDate: String,
        phoneNumber: String,
        occupation: String,
        city: String
    ): Result<UserProfile> {
        if (uid.isBlank()) return Result.failure(Exception("Μη έγκυρο αναγνωριστικό χρήστη"))
        val fullName = "$firstName $lastName".trim().ifBlank { "Χρήστης Finsay" }
        val updates = mapOf(
            "displayName" to fullName,
            "firstName" to firstName.trim(),
            "lastName" to lastName.trim(),
            "birthDate" to birthDate.trim(),
            "phoneNumber" to phoneNumber.trim(),
            "occupation" to occupation.trim(),
            "city" to city.trim(),
            "updatedAt" to Timestamp.now()
        )
        db.collection("users").document(uid).set(updates, SetOptions.merge()).await()
        val doc = db.collection("users").document(uid).get().await()
        val updatedFromDoc = try { doc.toSafeUserProfile() } catch (_: Exception) { null }
        val finalProfile = (updatedFromDoc ?: UserProfile(uid = uid)).copy(
            uid = uid,
            displayName = if (updatedFromDoc?.displayName.isNullOrBlank()) fullName else updatedFromDoc!!.displayName,
            firstName = if (updatedFromDoc?.firstName.isNullOrBlank()) firstName.trim() else updatedFromDoc!!.firstName,
            lastName = if (updatedFromDoc?.lastName.isNullOrBlank()) lastName.trim() else updatedFromDoc!!.lastName,
            birthDate = if (updatedFromDoc?.birthDate.isNullOrBlank()) birthDate.trim() else updatedFromDoc!!.birthDate,
            phoneNumber = if (updatedFromDoc?.phoneNumber.isNullOrBlank()) phoneNumber.trim() else updatedFromDoc!!.phoneNumber,
            occupation = if (updatedFromDoc?.occupation.isNullOrBlank()) occupation.trim() else updatedFromDoc!!.occupation,
            city = if (updatedFromDoc?.city.isNullOrBlank()) city.trim() else updatedFromDoc!!.city
        )
        return Result.success(finalProfile)
    }

    suspend fun findUserByEmail(email: String): UserProfile? {
        val clean = email.trim()
        if (clean.isBlank()) return null
        val lower = clean.lowercase()

        try {
            val q1 = db.collection("users").whereEqualTo("email", clean).limit(1).get().await()
            if (!q1.isEmpty) {
                val doc = q1.documents.first()
                val profile = try { doc.toSafeUserProfile() } catch (_: Throwable) { null }
                if (profile != null) {
                    if (profile.uid.isBlank()) profile.uid = doc.id
                    if (profile.email.isBlank()) profile.email = doc.safeString("email") ?: clean
                    return profile
                }
            }
        } catch (_: Throwable) {}

        if (lower != clean) {
            try {
                val q2 = db.collection("users").whereEqualTo("email", lower).limit(1).get().await()
                if (!q2.isEmpty) {
                    val doc = q2.documents.first()
                    val profile = try { doc.toSafeUserProfile() } catch (_: Throwable) { null }
                    if (profile != null) {
                        if (profile.uid.isBlank()) profile.uid = doc.id
                        if (profile.email.isBlank()) profile.email = doc.safeString("email") ?: lower
                        return profile
                    }
                }
            } catch (_: Throwable) {}
        }

        return null
    }

    private fun isUserDocumentVerified(doc: DocumentSnapshot): Boolean {
        val confirmed = doc.safeBoolean("emailVerifiedConfirmed")
        val requiresVerif = doc.safeBoolean("requiresEmailVerification") == true
        return confirmed == true || (confirmed == null && !requiresVerif)
    }

    suspend fun findVerifiedUserByEmail(
        email: String,
        currentUserUid: String = "",
        currentUserEmail: String = ""
    ): FirestoreSyncRepository.VerifiedUserLookupResult {
        val clean = email.trim()
        if (clean.isBlank()) return FirestoreSyncRepository.VerifiedUserLookupResult.NotFound
        val lower = clean.lowercase()
        if (currentUserEmail.isNotBlank() && clean.equals(currentUserEmail.trim(), ignoreCase = true)) {
            return FirestoreSyncRepository.VerifiedUserLookupResult.SelfEmail
        }

        var matchedDoc: DocumentSnapshot? = null

        try {
            val q1 = db.collection("users").whereEqualTo("email", clean).limit(1).get().await()
            if (!q1.isEmpty) matchedDoc = q1.documents.first()
        } catch (_: Throwable) {}

        if (matchedDoc == null && lower != clean) {
            try {
                val q2 = db.collection("users").whereEqualTo("email", lower).limit(1).get().await()
                if (!q2.isEmpty) matchedDoc = q2.documents.first()
            } catch (_: Throwable) {}
        }

        if (matchedDoc != null) {
            val profile = try {
                matchedDoc.toSafeUserProfile().let { p ->
                    val resolvedEmail = p.email.ifBlank {
                        matchedDoc.safeString("email")?.trim()
                            ?: matchedDoc.safeString("userEmail")?.trim()
                            ?: matchedDoc.safeString("contactEmail")?.trim()
                            ?: clean
                    }
                    p.copy(uid = p.uid.ifBlank { matchedDoc.id }, email = resolvedEmail)
                }
            } catch (_: Throwable) {
                null
            }
            if (profile != null) {
                if (currentUserUid.isNotBlank() && profile.uid == currentUserUid) {
                    return FirestoreSyncRepository.VerifiedUserLookupResult.SelfEmail
                }
                return if (isUserDocumentVerified(matchedDoc)) {
                    FirestoreSyncRepository.VerifiedUserLookupResult.Found(profile)
                } else {
                    FirestoreSyncRepository.VerifiedUserLookupResult.NotVerified
                }
            }
        }

        return FirestoreSyncRepository.VerifiedUserLookupResult.NotFound
    }

    suspend fun updateUserProfileHouseholdIds(uid: String, householdIds: List<String>) {
        if (uid.isBlank()) return
        try {
            db.collection("users").document(uid).set(
                mapOf(
                    "householdIds" to householdIds.filter { it.isNotBlank() }.distinct(),
                    "updatedAt" to Timestamp.now()
                ),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "updateUserProfileHouseholdIds notice: ${e.message}")
        }
    }

    suspend fun resolveUserProfilesByUids(uids: List<String>): Map<String, UserProfile> {
        val cleanUids = uids.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (cleanUids.isEmpty()) return emptyMap()
        return coroutineScope {
            cleanUids.map { uid ->
                async {
                    try {
                        var doc = db.collection("users").document(uid).get().await()
                        if (!doc.exists()) {
                            val querySnap = db.collection("users")
                                .whereEqualTo("uid", uid)
                                .limit(1)
                                .get()
                                .await()
                            if (!querySnap.isEmpty) doc = querySnap.documents.first()
                        }
                        if (doc.exists()) {
                            val profile = doc.toSafeUserProfile()
                            val email = profile.email.trim().ifBlank {
                                doc.safeString("email")?.trim()
                                    ?: doc.safeString("userEmail")?.trim()
                                    ?: doc.safeString("contactEmail")?.trim()
                                    ?: ""
                            }
                            val name = profile.displayName.trim().ifBlank {
                                "${profile.firstName} ${profile.lastName}".trim()
                            }.ifBlank { email }
                            uid to profile.copy(uid = uid, email = email, displayName = name)
                        } else {
                            null
                        }
                    } catch (t: Throwable) {
                        if (t is CancellationException) throw t
                        Log.w(TAG, "resolveUserProfilesByUids($uid) error: ${t.message}")
                        null
                    }
                }
            }.awaitAll().filterNotNull().toMap()
        }
    }
}
