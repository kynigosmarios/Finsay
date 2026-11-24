package com.example.data.repository

import android.util.Log
import com.example.data.model.FirebaseHouseholdActivity
import com.example.data.model.UserProfile
import com.example.data.model.safeBoolean
import com.example.data.model.safeString
import com.example.data.model.toSafeUserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

internal class FirestoreUserProfileDelegate(
    private val db: FirebaseFirestore,
    private val householdDelegate: FirestoreHouseholdDelegate
) {
    companion object {
        private const val TAG = "FirestoreSyncRepo"
        private const val PRIVATE_SUBCOLLECTION = "private"
        private const val PRIVATE_PROFILE_DOC = "profile"
    }

    private data class PrivateProfileDetails(
        val birthDate: String = "",
        val phoneNumber: String = "",
        val occupation: String = "",
        val city: String = ""
    )

    private fun privateProfileRef(uid: String) =
        db.collection("users").document(uid).collection(PRIVATE_SUBCOLLECTION).document(PRIVATE_PROFILE_DOC)

    private fun buildPrivateDetailsMap(
        birthDate: String,
        phoneNumber: String,
        occupation: String,
        city: String
    ): Map<String, Any> = mapOf(
        "birthDate" to birthDate.trim().take(40),
        "phoneNumber" to phoneNumber.trim().take(40),
        "occupation" to occupation.trim().take(120),
        "city" to city.trim().take(120),
        "updatedAt" to Timestamp.now()
    )

    private fun UserProfile.toPublicFirestoreMap(includePremium: Boolean): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "uid" to uid,
            "email" to email,
            "displayName" to displayName,
            "firstName" to firstName,
            "lastName" to lastName,
            "householdId" to householdId,
            "householdIds" to householdIds,
            "photoUrl" to photoUrl,
            "authProvider" to authProvider,
            "appLanguage" to appLanguage,
            "themeMode" to themeMode,
            "reminderEnabled" to reminderEnabled,
            "reminderHour" to reminderHour,
            "reminderMinute" to reminderMinute,
            "notificationConsentCompleted" to notificationConsentCompleted,
            "requiresEmailVerification" to requiresEmailVerification,
            "emailVerifiedConfirmed" to emailVerifiedConfirmed,
            "updatedAt" to (updatedAt ?: Timestamp.now())
        )
        if (includePremium) {
            map["isPremium"] = isPremium
        }
        createdAt?.let { map["createdAt"] = it }
        return map
    }

    private suspend fun readPrivateProfileOnce(uid: String): DocumentSnapshot? {
        return try {
            privateProfileRef(uid).get().await()
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            null
        }
    }

    private suspend fun migrateLegacyPrivateFieldsIfPresent(
        uid: String,
        publicDoc: DocumentSnapshot,
        privateDoc: DocumentSnapshot?
    ): PrivateProfileDetails {
        val privBirth = privateDoc?.safeString("birthDate").orEmpty()
        val privPhone = privateDoc?.safeString("phoneNumber").orEmpty()
        val privOcc = privateDoc?.safeString("occupation").orEmpty()
        val privCity = privateDoc?.safeString("city").orEmpty()

        val hasLegacyFields = publicDoc.contains("birthDate") ||
            publicDoc.contains("phoneNumber") ||
            publicDoc.contains("occupation") ||
            publicDoc.contains("city")

        val mergedBirth = privBirth.ifBlank { publicDoc.safeString("birthDate").orEmpty() }
        val mergedPhone = privPhone.ifBlank { publicDoc.safeString("phoneNumber").orEmpty() }
        val mergedOcc = privOcc.ifBlank { publicDoc.safeString("occupation").orEmpty() }
        val mergedCity = privCity.ifBlank { publicDoc.safeString("city").orEmpty() }

        if (hasLegacyFields) {
            try {
                // Persist to owner-only sub-document first so existing user data is never lost
                if (mergedBirth.isNotBlank() || mergedPhone.isNotBlank() || mergedOcc.isNotBlank() || mergedCity.isNotBlank()) {
                    privateProfileRef(uid).set(
                        buildPrivateDetailsMap(mergedBirth, mergedPhone, mergedOcc, mergedCity),
                        SetOptions.merge()
                    ).await()
                }
                // Then scrub legacy sensitive fields from the root /users/{uid} document
                db.collection("users").document(uid).update(
                    mapOf(
                        "birthDate" to FieldValue.delete(),
                        "phoneNumber" to FieldValue.delete(),
                        "occupation" to FieldValue.delete(),
                        "city" to FieldValue.delete()
                    )
                ).await()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Legacy private profile migration notice: ${e.message}")
            }
        }

        return PrivateProfileDetails(
            birthDate = mergedBirth,
            phoneNumber = mergedPhone,
            occupation = mergedOcc,
            city = mergedCity
        )
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
        val privateSnap = readPrivateProfileOnce(uid)

        val discoveredHids = householdDelegate.discoverUserHouseholds(uid, email, excludedHouseholdIds)

        return if (snapshot.exists()) {
            val existing = try {
                snapshot.toSafeUserProfile()
            } catch (_: Throwable) {
                null
            } ?: UserProfile(uid = uid, email = email, displayName = displayName)

            val migratedPrivate = migrateLegacyPrivateFieldsIfPresent(uid, snapshot, privateSnap)

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

            val finalBirth = migratedPrivate.birthDate.ifBlank { birthDate.trim() }
            val finalPhone = migratedPrivate.phoneNumber.ifBlank { phoneNumber.trim() }
            val finalOcc = migratedPrivate.occupation.ifBlank { occupation.trim() }
            val finalCity = migratedPrivate.city.ifBlank { city.trim() }

            val merged = existing.copy(
                email = (if (email.isNotBlank()) email else existing.email).trim(),
                displayName = profileDisplayName,
                firstName = derivedFirst,
                lastName = derivedLast,
                birthDate = finalBirth,
                phoneNumber = finalPhone,
                occupation = finalOcc,
                city = finalCity,
                householdId = finalHouseholdId,
                householdIds = finalHouseholdIds,
                authProvider = effectiveAuthProvider,
                requiresEmailVerification = existing.requiresEmailVerification,
                emailVerifiedConfirmed = existing.emailVerifiedConfirmed
            )

            // Write ONLY non-sensitive public fields to /users/{uid}
            userRef.set(merged.toPublicFirestoreMap(includePremium = false), SetOptions.merge()).await()

            // Persist sensitive personal fields strictly to owner-only /users/{uid}/private/profile
            if (finalBirth.isNotBlank() || finalPhone.isNotBlank() || finalOcc.isNotBlank() || finalCity.isNotBlank()) {
                try {
                    privateProfileRef(uid).set(
                        buildPrivateDetailsMap(finalBirth, finalPhone, finalOcc, finalCity),
                        SetOptions.merge()
                    ).await()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w(TAG, "Private profile sync notice: ${e.message}")
                }
            }
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

            val privBirth = privateSnap?.safeString("birthDate").orEmpty().ifBlank { birthDate.trim() }
            val privPhone = privateSnap?.safeString("phoneNumber").orEmpty().ifBlank { phoneNumber.trim() }
            val privOcc = privateSnap?.safeString("occupation").orEmpty().ifBlank { occupation.trim() }
            val privCity = privateSnap?.safeString("city").orEmpty().ifBlank { city.trim() }

            val newProfile = UserProfile(
                uid = uid,
                email = email,
                displayName = profileDisplayName,
                firstName = derivedFirst,
                lastName = derivedLast,
                birthDate = privBirth,
                phoneNumber = privPhone,
                occupation = privOcc,
                city = privCity,
                householdId = initialHouseholdId,
                householdIds = initialHouseholdIds,
                isPremium = false,
                authProvider = authProvider.ifBlank { "password" },
                createdAt = Timestamp.now()
            )
            // Write ONLY non-sensitive public fields to /users/{uid}
            userRef.set(newProfile.toPublicFirestoreMap(includePremium = true), SetOptions.merge()).await()

            // Persist sensitive personal fields strictly to owner-only /users/{uid}/private/profile
            if (privBirth.isNotBlank() || privPhone.isNotBlank() || privOcc.isNotBlank() || privCity.isNotBlank()) {
                try {
                    privateProfileRef(uid).set(
                        buildPrivateDetailsMap(privBirth, privPhone, privOcc, privCity),
                        SetOptions.merge()
                    ).await()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w(TAG, "New private profile save notice: ${e.message}")
                }
            }
            newProfile
        }
    }

    suspend fun getUserProfileOnce(uid: String): UserProfile? {
        if (uid.isBlank()) return null
        return try {
            val snapshot = db.collection("users").document(uid).get().await()
            if (!snapshot.exists()) return null
            val base = snapshot.toSafeUserProfile()
            val priv = readPrivateProfileOnce(uid)
            if (priv != null && priv.exists()) {
                base.copy(
                    birthDate = priv.safeString("birthDate").orEmpty().ifBlank { base.birthDate },
                    phoneNumber = priv.safeString("phoneNumber").orEmpty().ifBlank { base.phoneNumber },
                    occupation = priv.safeString("occupation").orEmpty().ifBlank { base.occupation },
                    city = priv.safeString("city").orEmpty().ifBlank { base.city }
                )
            } else {
                base
            }
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.w(TAG, "getUserProfileOnce error: ${t.message}")
            null
        }
    }

    suspend fun savePendingUserProfile(profile: UserProfile) {
        if (profile.uid.isBlank()) return
        // Never write sensitive personal fields (birthDate, phoneNumber, occupation, city) to root /users/{uid}
        db.collection("users").document(profile.uid)
            .set(profile.toPublicFirestoreMap(includePremium = false), SetOptions.merge())
            .await()

        if (profile.birthDate.isNotBlank() ||
            profile.phoneNumber.isNotBlank() ||
            profile.occupation.isNotBlank() ||
            profile.city.isNotBlank()
        ) {
            privateProfileRef(profile.uid).set(
                buildPrivateDetailsMap(
                    birthDate = profile.birthDate,
                    phoneNumber = profile.phoneNumber,
                    occupation = profile.occupation,
                    city = profile.city
                ),
                SetOptions.merge()
            ).await()
        }
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
        val publicFlow = db.collection("users").document(userId)
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

        val privateFlow = privateProfileRef(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    PrivateProfileDetails(
                        birthDate = snapshot.safeString("birthDate").orEmpty(),
                        phoneNumber = snapshot.safeString("phoneNumber").orEmpty(),
                        occupation = snapshot.safeString("occupation").orEmpty(),
                        city = snapshot.safeString("city").orEmpty()
                    )
                } else {
                    PrivateProfileDetails()
                }
            }
            .catch { t ->
                Log.w(TAG, "observeUserProfile private flow error: ${t.message}")
                emit(PrivateProfileDetails())
            }

        return combine(publicFlow, privateFlow) { pub, priv ->
            pub?.copy(
                birthDate = priv.birthDate.ifBlank { pub.birthDate },
                phoneNumber = priv.phoneNumber.ifBlank { pub.phoneNumber },
                occupation = priv.occupation.ifBlank { pub.occupation },
                city = priv.city.ifBlank { pub.city }
            )
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
        val cleanFirst = firstName.trim()
        val cleanLast = lastName.trim()
        val cleanBirth = birthDate.trim().take(40)
        val cleanPhone = phoneNumber.trim().take(40)
        val cleanOcc = occupation.trim().take(120)
        val cleanCity = city.trim().take(120)

        // 1. Write sensitive personal details strictly to owner-only /users/{uid}/private/profile
        privateProfileRef(uid).set(
            buildPrivateDetailsMap(cleanBirth, cleanPhone, cleanOcc, cleanCity),
            SetOptions.merge()
        ).await()

        // 2. Write only display identity fields to root /users/{uid} and scrub any legacy PII keys
        val publicUpdates = mapOf(
            "displayName" to fullName,
            "firstName" to cleanFirst,
            "lastName" to cleanLast,
            "updatedAt" to Timestamp.now()
        )
        val userRef = db.collection("users").document(uid)
        userRef.set(publicUpdates, SetOptions.merge()).await()
        try {
            userRef.update(
                mapOf(
                    "birthDate" to FieldValue.delete(),
                    "phoneNumber" to FieldValue.delete(),
                    "occupation" to FieldValue.delete(),
                    "city" to FieldValue.delete()
                )
            ).await()
        } catch (_: Exception) {
            // Safe to ignore if legacy fields were already absent
        }

        val doc = userRef.get().await()
        val updatedFromDoc = try { doc.toSafeUserProfile() } catch (_: Exception) { null }
        val finalProfile = (updatedFromDoc ?: UserProfile(uid = uid)).copy(
            uid = uid,
            displayName = if (updatedFromDoc?.displayName.isNullOrBlank()) fullName else updatedFromDoc!!.displayName,
            firstName = if (updatedFromDoc?.firstName.isNullOrBlank()) cleanFirst else updatedFromDoc!!.firstName,
            lastName = if (updatedFromDoc?.lastName.isNullOrBlank()) cleanLast else updatedFromDoc!!.lastName,
            birthDate = cleanBirth,
            phoneNumber = cleanPhone,
            occupation = cleanOcc,
            city = cleanCity
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
