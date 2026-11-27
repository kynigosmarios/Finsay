package com.example.data.repository

import android.util.Log
import com.example.data.model.Household
import com.example.data.model.HouseholdRole
import com.example.data.model.UserProfile
import com.example.data.model.safeString
import com.example.data.model.safeStringList
import com.example.data.model.toSafeHousehold
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.random.Random

internal class FirestoreHouseholdDelegate(
    private val db: FirebaseFirestore
) {
    companion object {
        private const val TAG = "FirestoreSyncRepo"
    }

    lateinit var userProfileDelegate: FirestoreUserProfileDelegate

    suspend fun getAllUserHouseholds(
        uid: String,
        email: String,
        excludedHouseholdIds: Set<String> = emptySet()
    ): List<Household> {
        val foundMap = mutableMapOf<String, Household>()

        fun parseAndAccept(doc: DocumentSnapshot): Household? = try {
            if (excludedHouseholdIds.contains(doc.id)) null
            else {
                val deletedFor = doc.safeStringList("deletedForUsers") ?: emptyList()
                if (uid.isNotBlank() && deletedFor.contains(uid)) null
                else {
                    val h = doc.toSafeHousehold()
                    if (h.id.isBlank()) h.id = doc.id
                    val rawName = doc.safeString("name")
                    if (!rawName.isNullOrBlank()) h.name = rawName
                    h
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "parseAndAccept household ${doc.id} failed: ${t.message}")
            null
        }

        if (uid.isNotBlank()) {
            try {
                val qOwner = db.collection("households").whereEqualTo("ownerId", uid).get().await()
                for (doc in qOwner.documents) {
                    parseAndAccept(doc)?.let { foundMap[it.id] = it }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.w(TAG, "getAllUserHouseholds owner error: ${t.message}")
            }

            try {
                val qMembers = db.collection("households").whereArrayContains("memberUids", uid).get().await()
                for (doc in qMembers.documents) {
                    parseAndAccept(doc)?.let { foundMap[it.id] = it }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.w(TAG, "getAllUserHouseholds memberUids error: ${t.message}")
            }

            var existingProfileHouseholdIds: Set<String> = emptySet()
            try {
                val uDoc = db.collection("users").document(uid).get().await()
                if (uDoc.exists()) {
                    val hid = uDoc.safeString("householdId")
                    val hids = uDoc.safeStringList("householdIds") ?: emptyList()
                    existingProfileHouseholdIds = hids.filter { it.isNotBlank() }.toSet()
                    val allProfileHids = (listOfNotNull(hid) + hids)
                        .filter { it.isNotBlank() && !excludedHouseholdIds.contains(it) }
                        .distinct()
                    for (targetHid in allProfileHids) {
                        if (!foundMap.containsKey(targetHid)) {
                            try {
                                val hDoc = db.collection("households").document(targetHid).get().await()
                                if (hDoc.exists()) parseAndAccept(hDoc)?.let { foundMap[it.id] = it }
                            } catch (_: Throwable) {}
                        }
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.w(TAG, "getAllUserHouseholds user profile error: ${t.message}")
            }

            val resultList = foundMap.values.toList()
            resultList.forEach { h ->
                if (h.id.isNotBlank() && h.memberUids.contains(uid)) {
                    ensuredHouseholdsCache["${h.id}:$uid"] = h
                }
            }

            if (resultList.isNotEmpty()) {
                val validIds = resultList.map { it.id }.filter { it.isNotBlank() }.distinct()
                val hasMissingIds = validIds.any { it !in existingProfileHouseholdIds }
                if (hasMissingIds) {
                    try {
                        db.collection("users").document(uid).set(
                            mapOf("householdIds" to FieldValue.arrayUnion(*validIds.toTypedArray())),
                            SetOptions.merge()
                        ).await()
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Log.w(TAG, "getAllUserHouseholds profile arrayUnion notice: ${e.message}")
                    }
                }
            }

            return resultList
        }

        return foundMap.values.toList()
    }

    suspend fun discoverUserHouseholds(
        uid: String,
        email: String,
        excludedHouseholdIds: Set<String> = emptySet()
    ): List<String> = getAllUserHouseholds(uid, email, excludedHouseholdIds).map { it.id }

    suspend fun findHouseholdWithExpenses(householdIds: List<String>): String? {
        for (hid in householdIds.filter { it.isNotBlank() }.distinct()) {
            try {
                val snap = db.collection("households").document(hid)
                    .collection("expenses").limit(1).get().await()
                if (!snap.isEmpty) return hid
            } catch (_: Exception) {}
        }
        return null
    }

    suspend fun hasExpenses(householdId: String): Boolean {
        if (householdId.isBlank()) return false
        return try {
            val snap = db.collection("households").document(householdId)
                .collection("expenses").limit(1).get().await()
            !snap.isEmpty
        } catch (_: Exception) {
            false
        }
    }

    suspend fun createHousehold(ownerUid: String, exodologioName: String, currencyCode: String = "EUR"): Household {
        val householdId = UUID.randomUUID().toString().take(12)
        val inviteCode = "OIKO-" + Random.nextInt(1000, 9999).toString()
        val cleanCurrency = currencyCode.trim().uppercase().ifBlank { "EUR" }

        val household = Household(
            id = householdId,
            name = exodologioName.ifBlank { "My Household Book" },
            ownerId = ownerUid,
            inviteCode = inviteCode,
            memberUids = listOf(ownerUid),
            memberRoles = mapOf(ownerUid to HouseholdRole.CREATOR.key),
            pendingInviteRole = HouseholdRole.REGISTAR.key,
            currencyCode = cleanCurrency,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        try {
            db.collection("households").document(householdId).set(household).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error saving new household: ${e.message}")
        }

        try {
            db.collection("users").document(ownerUid).set(
                mapOf(
                    "householdId" to householdId,
                    "householdIds" to FieldValue.arrayUnion(householdId)
                ),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Error updating user with new household: ${e.message}")
        }

        return household
    }

    private val ensuredHouseholdsCache = java.util.concurrent.ConcurrentHashMap<String, Household>()

    suspend fun ensureHouseholdExists(
        householdId: String,
        ownerUid: String,
        householdName: String = "My Household Book"
    ): Household {
        if (householdId.isBlank() || ownerUid.isBlank()) {
            return Household(id = householdId, name = householdName, ownerId = ownerUid, memberUids = listOf(ownerUid))
        }
        val cacheKey = "$householdId:$ownerUid"
        ensuredHouseholdsCache[cacheKey]?.let { return it }

        val docRef = db.collection("households").document(householdId)
        val snapshot = try {
            docRef.get().await()
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            // CRITICAL: Never treat a transient network/auth error as "household missing"
            // or we would overwrite name, memberUids, memberRoles, and inviteCode in Firestore!
            Log.w(TAG, "ensureHouseholdExists read failed for $householdId (skipping write): ${t.message}")
            return Household(id = householdId, name = householdName, ownerId = ownerUid, memberUids = listOf(ownerUid))
        }
        if (snapshot.exists()) {
            val existing = try { snapshot.toSafeHousehold() } catch (_: Throwable) {
                Household(id = householdId, name = householdName, ownerId = ownerUid, memberUids = listOf(ownerUid))
            }
            if (!existing.memberUids.contains(ownerUid)) {
                try {
                    docRef.update("memberUids", FieldValue.arrayUnion(ownerUid)).await()
                } catch (_: Throwable) {}
            }
            ensuredHouseholdsCache[cacheKey] = existing
            return existing
        }

        val newHousehold = Household(
            id = householdId,
            name = householdName.ifBlank { "My Household Book" },
            ownerId = ownerUid,
            inviteCode = "OIKO-" + Random.nextInt(1000, 9999).toString(),
            memberUids = listOf(ownerUid),
            memberRoles = mapOf(ownerUid to HouseholdRole.CREATOR.key),
            pendingInviteRole = HouseholdRole.REGISTAR.key,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        try {
            docRef.set(newHousehold, SetOptions.merge()).await()
            db.collection("users").document(ownerUid).set(
                mapOf(
                    "householdId" to householdId,
                    "householdIds" to FieldValue.arrayUnion(householdId)
                ),
                SetOptions.merge()
            ).await()
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.e(TAG, "Error ensuring household in Firestore: ${t.message}")
        }
        return newHousehold
    }

    suspend fun fetchUserHouseholds(householdIds: List<String>, excludedHouseholdIds: Set<String> = emptySet()): List<Household> {
        val filteredIds = householdIds.filter { it.isNotBlank() && !excludedHouseholdIds.contains(it) }
        if (filteredIds.isEmpty()) return emptyList()
        val list = mutableListOf<Household>()
        for (id in filteredIds) {
            try {
                val doc = db.collection("households").document(id).get().await()
                if (doc.exists()) {
                    val h = doc.toSafeHousehold()
                    if (h.id.isBlank()) h.id = doc.id
                    doc.safeString("name")?.takeIf { it.isNotBlank() }?.let { h.name = it }
                    list.add(h)
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Log.w(TAG, "Error fetching household $id", t)
            }
        }
        return list
    }

    suspend fun switchActiveHousehold(uid: String, householdId: String) {
        if (uid.isBlank() || householdId.isBlank()) return
        try {
            db.collection("users").document(uid)
                .set(mapOf("householdId" to householdId), SetOptions.merge())
                .await()
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.w(TAG, "switchActiveHousehold notice: ${t.message}")
        }
    }

    suspend fun renameHousehold(householdId: String, newName: String) {
        if (householdId.isBlank() || newName.isBlank()) return
        db.collection("households").document(householdId).set(
            mapOf("id" to householdId, "name" to newName.trim(), "updatedAt" to Timestamp.now()),
            SetOptions.merge()
        ).await()
    }

    suspend fun updateHouseholdCurrency(householdId: String, currencyCode: String) {
        if (householdId.isBlank() || currencyCode.isBlank()) return
        val cleanCode = currencyCode.trim().uppercase()
        db.collection("households").document(householdId).set(
            mapOf("id" to householdId, "currencyCode" to cleanCode, "updatedAt" to Timestamp.now()),
            SetOptions.merge()
        ).await()
    }

    suspend fun updateHouseholdInviteRole(householdId: String, role: String) {
        if (householdId.isBlank()) return
        try {
            db.collection("households").document(householdId).update("pendingInviteRole", role).await()
        } catch (_: Exception) {}
    }

    suspend fun joinHouseholdByCode(userUid: String, inviteCode: String, role: String? = null): Result<Household> {
        val normalizedCode = inviteCode.trim().uppercase()
        val querySnapshot = db.collection("households")
            .whereEqualTo("inviteCode", normalizedCode)
            .get()
            .await()

        if (querySnapshot.isEmpty) {
            return Result.failure(Exception("Δεν βρέθηκε εξοδολόγιο με τον κωδικό: $normalizedCode"))
        }

        val doc = querySnapshot.documents.first()
        val household = try { doc.toSafeHousehold() } catch (_: Exception) { null }
            ?: return Result.failure(Exception("Σφάλμα ανάγνωσης στοιχείων εξοδολογίου"))

        val assignedRole = role?.uppercase() ?: (doc.getString("pendingInviteRole") ?: "CONTRIBUTOR")
        val updates = mutableMapOf<String, Any>(
            "memberUids" to FieldValue.arrayUnion(userUid),
            "memberRoles.$userUid" to assignedRole
        )
        try {
            doc.reference.update(updates).await()
        } catch (_: Exception) {
            doc.reference.update("memberUids", FieldValue.arrayUnion(userUid)).await()
        }

        db.collection("users").document(userUid).update(
            "householdId", household.id,
            "householdIds", FieldValue.arrayUnion(household.id)
        ).await()

        val updatedRoles = household.memberRoles.toMutableMap().apply { put(userUid, assignedRole) }
        return Result.success(
            household.copy(
                memberUids = (household.memberUids + userUid).distinct(),
                memberRoles = updatedRoles
            )
        )
    }

    fun observeHousehold(householdId: String): Flow<Household?> {
        if (householdId.isBlank()) return flowOf(null)
        return db.collection("households").document(householdId)
            .snapshots()
            .map { snapshot ->
                try {
                    if (snapshot.exists()) {
                        val h = snapshot.toSafeHousehold()
                        if (h.id.isBlank()) h.id = snapshot.id
                        snapshot.safeString("name")?.takeIf { it.isNotBlank() }?.let { h.name = it }
                        h
                    } else null
                } catch (t: Throwable) {
                    Log.w(TAG, "observeHousehold map warning: ${t.message}")
                    null
                }
            }
            .catch { t ->
                Log.w(TAG, "observeHousehold flow error (preserving current state): ${t.message}")
            }
    }

    fun observeUserHouseholds(uid: String, email: String): Flow<List<Household>> {
        if (uid.isBlank()) return flowOf(emptyList())

        var memberEmitted = false
        val memberFlow = db.collection("households")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                memberEmitted = true
                snap.documents.mapNotNull { doc ->
                    try {
                        val deletedFor = doc.safeStringList("deletedForUsers") ?: emptyList()
                        if (deletedFor.contains(uid)) return@mapNotNull null
                        doc.toSafeHousehold().apply {
                            if (id.isBlank()) id = doc.id
                            doc.safeString("name")?.let { nm -> if (nm.isNotBlank()) name = nm }
                        }
                    } catch (t: Throwable) {
                        Log.w(TAG, "observeUserHouseholds member map error ${doc.id}: ${t.message}")
                        null
                    }
                }
            }
            .catch { t ->
                Log.w(TAG, "observeUserHouseholds memberFlow error: ${t.message}")
                if (!memberEmitted) emit(emptyList())
            }

        var ownerEmitted = false
        val ownerFlow = db.collection("households")
            .whereEqualTo("ownerId", uid)
            .snapshots()
            .map { snap ->
                ownerEmitted = true
                snap.documents.mapNotNull { doc ->
                    try {
                        val deletedFor = doc.safeStringList("deletedForUsers") ?: emptyList()
                        if (deletedFor.contains(uid)) return@mapNotNull null
                        doc.toSafeHousehold().apply {
                            if (id.isBlank()) id = doc.id
                            doc.safeString("name")?.let { nm -> if (nm.isNotBlank()) name = nm }
                        }
                    } catch (t: Throwable) {
                        Log.w(TAG, "observeUserHouseholds owner map error ${doc.id}: ${t.message}")
                        null
                    }
                }
            }
            .catch { t ->
                Log.w(TAG, "observeUserHouseholds ownerFlow error: ${t.message}")
                if (!ownerEmitted) emit(emptyList())
            }

        return combine(memberFlow, ownerFlow) { mList, oList ->
            (oList + mList).distinctBy { it.id }
        }.catch { t ->
            Log.w(TAG, "observeUserHouseholds combined flow error (preserving current state): ${t.message}")
        }
    }

    suspend fun backfillHouseholdMemberMetadata(
        householdId: String,
        emailsByUid: Map<String, String>,
        namesByUid: Map<String, String>
    ) {
        if (householdId.isBlank() || (emailsByUid.isEmpty() && namesByUid.isEmpty())) return
        try {
            val hDocRef = db.collection("households").document(householdId)
            val doc = hDocRef.get().await()
            if (!doc.exists()) return
            val existing = doc.toSafeHousehold()
            val updatedEmails = existing.memberEmails.toMutableMap()
            val updatedNames = existing.memberNames.toMutableMap()
            var changed = false
            emailsByUid.forEach { (uid, email) ->
                if (uid.isNotBlank() && email.isNotBlank() && updatedEmails[uid].isNullOrBlank()) {
                    updatedEmails[uid] = email
                    changed = true
                }
            }
            namesByUid.forEach { (uid, name) ->
                if (uid.isNotBlank() && name.isNotBlank() && updatedNames[uid].isNullOrBlank()) {
                    updatedNames[uid] = name
                    changed = true
                }
            }
            if (changed) {
                hDocRef.update(
                    mapOf(
                        "memberEmails" to updatedEmails,
                        "memberNames" to updatedNames,
                        "updatedAt" to Timestamp.now()
                    )
                ).await()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "backfillHouseholdMemberMetadata error: ${e.message}")
        }
    }

    suspend fun shareHouseholdWithUser(
        householdId: String,
        targetUser: UserProfile,
        role: HouseholdRole
    ): Household? {
        if (householdId.isBlank() || targetUser.uid.isBlank()) return null
        val targetUid = targetUser.uid.trim()
        val cleanEmail = targetUser.email.trim()
        val lowerEmail = cleanEmail.lowercase()
        val resolvedName = targetUser.displayName.ifBlank {
            "${targetUser.firstName} ${targetUser.lastName}".trim()
        }.ifBlank { cleanEmail.substringBefore("@").ifBlank { "Χρήστης" } }

        val hDocRef = db.collection("households").document(householdId)
        val doc = hDocRef.get().await()
        if (!doc.exists()) return null

        val existing = doc.toSafeHousehold()
        val updatedMemberUids = (existing.memberUids + targetUid).filter { it.isNotBlank() }.distinct()
        val updatedInvitedEmails = (existing.invitedEmails + listOfNotNull(lowerEmail.ifBlank { null })).distinct()
        val updatedRoles = existing.memberRoles.toMutableMap().apply {
            put(targetUid, role.key)
            if (cleanEmail.isNotBlank()) remove(cleanEmail)
            if (lowerEmail.isNotBlank()) remove(lowerEmail)
        }
        val updatedEmails = existing.memberEmails.toMutableMap().apply {
            if (cleanEmail.isNotBlank()) put(targetUid, cleanEmail)
        }
        val updatedNames = existing.memberNames.toMutableMap().apply {
            put(targetUid, resolvedName)
        }
        val deletedFor = (doc.safeStringList("deletedForUsers") ?: emptyList()).filterNot { it == targetUid }

        val payload = hashMapOf<String, Any>(
            "memberUids" to updatedMemberUids,
            "invitedEmails" to updatedInvitedEmails,
            "memberRoles" to updatedRoles,
            "memberEmails" to updatedEmails,
            "memberNames" to updatedNames,
            "deletedForUsers" to deletedFor,
            "updatedAt" to Timestamp.now()
        )
        hDocRef.update(payload).await()

        return existing.copy(
            memberUids = updatedMemberUids,
            invitedEmails = updatedInvitedEmails,
            memberRoles = updatedRoles,
            memberEmails = updatedEmails,
            memberNames = updatedNames
        )
    }

    suspend fun updateSharedUserRole(
        householdId: String,
        targetUid: String,
        targetEmail: String,
        newRole: HouseholdRole
    ): Household? {
        if (householdId.isBlank() || targetUid.isBlank()) return null
        val hDocRef = db.collection("households").document(householdId)
        val doc = hDocRef.get().await()
        if (!doc.exists()) return null

        val existing = doc.toSafeHousehold()
        val updatedRoles = existing.memberRoles.toMutableMap().apply {
            put(targetUid, newRole.key)
            val clean = targetEmail.trim()
            if (clean.isNotBlank()) {
                remove(clean)
                remove(clean.lowercase())
            }
        }
        hDocRef.update(mapOf("memberRoles" to updatedRoles, "updatedAt" to Timestamp.now())).await()
        return existing.copy(memberRoles = updatedRoles)
    }

    suspend fun removeSharedUser(householdId: String, targetUid: String, targetEmail: String): Household? {
        if (householdId.isBlank()) return null
        val hDocRef = db.collection("households").document(householdId)
        val doc = hDocRef.get().await()
        if (!doc.exists()) return null

        val existing = doc.toSafeHousehold()
        val cleanEmail = targetEmail.trim().ifBlank { existing.memberEmails[targetUid].orEmpty().trim() }
        val updatedMemberUids = existing.memberUids.filterNot { it == targetUid }
        val updatedInvited = if (cleanEmail.isNotBlank()) {
            existing.invitedEmails.filterNot { it.equals(cleanEmail, ignoreCase = true) }
        } else existing.invitedEmails
        val updatedRoles = existing.memberRoles.toMutableMap().apply {
            if (targetUid.isNotBlank()) remove(targetUid)
            if (cleanEmail.isNotBlank()) {
                remove(cleanEmail)
                remove(cleanEmail.lowercase())
            }
        }
        val updatedEmails = existing.memberEmails.toMutableMap().apply { if (targetUid.isNotBlank()) remove(targetUid) }
        val updatedNames = existing.memberNames.toMutableMap().apply { if (targetUid.isNotBlank()) remove(targetUid) }
        val updatedDefaultPayers = existing.memberDefaultPayers.toMutableMap().apply { if (targetUid.isNotBlank()) remove(targetUid) }

        hDocRef.update(
            mapOf(
                "memberUids" to updatedMemberUids,
                "invitedEmails" to updatedInvited,
                "memberRoles" to updatedRoles,
                "memberEmails" to updatedEmails,
                "memberNames" to updatedNames,
                "memberDefaultPayers" to updatedDefaultPayers,
                "updatedAt" to Timestamp.now()
            )
        ).await()

        return existing.copy(
            memberUids = updatedMemberUids,
            invitedEmails = updatedInvited,
            memberRoles = updatedRoles,
            memberEmails = updatedEmails,
            memberNames = updatedNames,
            memberDefaultPayers = updatedDefaultPayers
        )
    }

    /**
     * Updates a user's default payer member IDs (CSV, supporting 0, 1, 2, or more members).
     * Uses FieldPath so Firestore rules allow any household member (CREATOR, EDITOR, REGISTAR)
     * to update strictly their own UID entry, while CREATOR may also update shared users.
     */
    suspend fun updateUserDefaultPayers(
        householdId: String,
        targetUid: String,
        memberIdsCsv: String
    ): Household? {
        val cleanHid = householdId.trim()
        val cleanUid = targetUid.trim()
        if (cleanHid.isBlank() || cleanUid.isBlank()) return null
        val hDocRef = db.collection("households").document(cleanHid)
        val now = Timestamp.now()
        hDocRef.update(
            FieldPath.of("memberDefaultPayers", cleanUid),
            memberIdsCsv,
            FieldPath.of("updatedAt"),
            now
        ).await()
        val refreshed = try {
            val snap = hDocRef.get().await()
            if (snap.exists()) snap.toSafeHousehold() else null
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
        return refreshed
    }

    suspend fun inviteMemberToHousehold(
        householdId: String,
        email: String,
        role: String = "REGISTAR",
        targetUser: UserProfile? = null
    ) {
        if (householdId.isBlank() || email.isBlank()) return
        val cleanEmail = email.trim()
        val resolvedUser = targetUser ?: userProfileDelegate.findUserByEmail(cleanEmail)
        val parsedRole = HouseholdRole.fromRaw(role, isCreator = false)
        if (resolvedUser != null && resolvedUser.uid.isNotBlank()) {
            shareHouseholdWithUser(householdId, resolvedUser.copy(email = resolvedUser.email.ifBlank { cleanEmail }), parsedRole)
        }
    }

    suspend fun removeMemberFromHousehold(householdId: String, email: String) {
        if (householdId.isBlank() || email.isBlank()) return
        val cleanEmail = email.trim()
        val matched = userProfileDelegate.findUserByEmail(cleanEmail)
        removeSharedUser(householdId, matched?.uid.orEmpty(), cleanEmail)
    }

    suspend fun deleteHousehold(householdId: String, userId: String, nextHouseholdId: String? = null) {
        if (householdId.isBlank()) return
        try {
            if (userId.isNotBlank()) {
                val userUpdate = mutableMapOf<String, Any>("householdIds" to FieldValue.arrayRemove(householdId))
                if (!nextHouseholdId.isNullOrBlank()) userUpdate["householdId"] = nextHouseholdId
                db.collection("users").document(userId).set(userUpdate, SetOptions.merge()).await()
            }
            try {
                db.collection("households").document(householdId).update(
                    mapOf(
                        "deletedForUsers" to FieldValue.arrayUnion(userId),
                        "memberUids" to FieldValue.arrayRemove(userId),
                        "updatedAt" to Timestamp.now()
                    )
                ).await()
            } catch (he: Exception) {
                if (he is CancellationException) throw he
                Log.w(TAG, "Household document disassociation notice: ${he.message}")
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "deleteHousehold error: ${e.message}", e)
        }
    }

    suspend fun leaveSharedHousehold(householdId: String, userId: String, userEmail: String) {
        // Policy: EDITOR and REGISTAR users cannot remove themselves from a shared household.
        // Only the household owner (CREATOR) may remove users via removeSharedUser().
        Log.w(TAG, "leaveSharedHousehold blocked for non-owner userId=$userId on householdId=$householdId")
    }

    suspend fun deleteUserEntireData(userId: String, householdIds: List<String> = emptyList()) {
        if (userId.isBlank()) return
        try {
            val subcollections = listOf("private", "expenses", "incomes", "categories", "members", "savings_goals", "recurring_expenses")
            for (sub in subcollections) {
                try {
                    val docs = db.collection("users").document(userId).collection(sub).get().await()
                    for (d in docs.documents) d.reference.delete().await()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w(TAG, "Error cleaning user subcollection $sub: ${e.message}")
                }
            }
            db.collection("users").document(userId).delete().await()

            for (hId in householdIds.filter { it.isNotBlank() }.distinct()) {
                ensuredHouseholdsCache.remove("$hId:$userId")
                try {
                    val hRef = db.collection("households").document(hId)
                    val hDoc = hRef.get().await()
                    if (hDoc.exists()) {
                        val ownerId = hDoc.getString("ownerId") ?: ""
                        val members = (hDoc.get("memberUids") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                        if (ownerId == userId && (members.isEmpty() || members == listOf(userId))) {
                            val hSubs = listOf("expenses", "incomes", "categories", "members", "savings_goals", "recurring_expenses", "activity")
                            for (sub in hSubs) {
                                try {
                                    val docs = hRef.collection(sub).get().await()
                                    for (d in docs.documents) d.reference.delete().await()
                                } catch (e: Exception) {
                                    if (e is CancellationException) throw e
                                    Log.w(TAG, "Error cleaning household $hId subcollection $sub: ${e.message}")
                                }
                            }
                            hRef.delete().await()
                        } else if (ownerId != userId && members.contains(userId)) {
                            hRef.update(
                                mapOf(
                                    "memberUids" to FieldValue.arrayRemove(userId),
                                    "memberRoles.$userId" to FieldValue.delete(),
                                    "memberEmails.$userId" to FieldValue.delete(),
                                    "memberNames.$userId" to FieldValue.delete(),
                                    "memberDefaultPayers.$userId" to FieldValue.delete(),
                                    "updatedAt" to Timestamp.now()
                                )
                            ).await()
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w(TAG, "Error removing user from household $hId: ${e.message}")
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "deleteUserEntireData error", e)
        }
    }
}
