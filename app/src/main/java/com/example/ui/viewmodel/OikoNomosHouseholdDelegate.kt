package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.viewModelScope
import com.example.data.entity.FamilyMember
import com.example.data.model.ConnectedHouseholdUser
import com.example.data.model.FirebaseCategory
import com.example.data.model.FirebaseMember
import com.example.data.model.Household
import com.example.data.model.HouseholdRole
import com.example.data.model.UserProfile
import com.example.data.repository.FirestoreSyncRepository
import com.example.data.seed.FirestoreDataSeed
import com.example.util.StartupSafety
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal class OikoNomosHouseholdDelegate(
    private val vm: OikoNomosViewModel
) {
    fun buildConnectedUsersList(
        household: Household?,
        resolvedMap: Map<String, UserProfile>,
        currentUser: FirebaseUser?,
        currentProfile: UserProfile?
    ): List<ConnectedHouseholdUser> {
        if (household == null) {
            val uid = currentUser?.uid.orEmpty()
            if (uid.isBlank()) return emptyList()
            return listOf(
                ConnectedHouseholdUser(
                    uid = uid,
                    email = currentUser?.email ?: currentProfile?.email.orEmpty(),
                    displayName = currentProfile?.displayName?.ifBlank { null }
                        ?: currentUser?.displayName?.ifBlank { null }
                        ?: currentUser?.email?.substringBefore("@")
                        ?: "User",
                    role = HouseholdRole.CREATOR,
                    isCreator = true
                )
            )
        }
        val uids = buildList {
            if (household.ownerId.isNotBlank()) add(household.ownerId)
            household.memberUids.filter { it.isNotBlank() && it != household.ownerId }.forEach { add(it) }
            val selfUid = currentUser?.uid.orEmpty()
            if (selfUid.isNotBlank() && !contains(selfUid)) add(selfUid)
        }.distinct()

        return uids.map { uid ->
            val isCreator = uid == household.ownerId
            val resolved = resolvedMap[uid]
            val selfEmail = if (uid == currentUser?.uid) (currentUser.email ?: currentProfile?.email.orEmpty()) else ""
            val selfName = if (uid == currentUser?.uid) {
                currentProfile?.displayName?.ifBlank { null } ?: currentUser.displayName.orEmpty()
            } else ""
            val email = household.memberEmails[uid]?.takeIf { it.isNotBlank() }
                ?: resolved?.email?.takeIf { it.isNotBlank() }
                ?: selfEmail
            val name = household.memberNames[uid]?.takeIf { it.isNotBlank() }
                ?: resolved?.displayName?.takeIf { it.isNotBlank() }
                ?: selfName
                ?: email.substringBefore("@")
            val rawRole = household.memberRoles[uid]
                ?: if (email.isNotBlank()) household.memberRoles[email.lowercase()] ?: household.memberRoles[email] else null
            val role = HouseholdRole.fromRaw(rawRole, isCreator = isCreator)
            ConnectedHouseholdUser(
                uid = uid,
                email = email,
                displayName = name.ifBlank { email.ifBlank { uid.take(8) } },
                role = role,
                isCreator = isCreator
            )
        }
    }

    fun resolveMissingHouseholdMemberEmails(households: List<Household>) {
        if (households.isEmpty()) return
        val selfUid = vm._currentUser.value?.uid.orEmpty()
        val cached = vm._resolvedUsersByUid.value
        val missingUids = households.flatMap { h ->
            buildList {
                if (h.ownerId.isNotBlank()) add(h.ownerId)
                addAll(h.memberUids)
            }.filter { uid ->
                uid.isNotBlank() &&
                    uid != selfUid &&
                    h.memberEmails[uid].isNullOrBlank() &&
                    !cached.containsKey(uid)
            }
        }.distinct()

        if (missingUids.isEmpty()) return

        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            val fetched = vm.firestoreSyncRepo.resolveUserProfilesByUids(missingUids)
            val resolvedEntries = missingUids.associateWith { uid -> fetched[uid] ?: UserProfile(uid = uid) }
            vm._resolvedUsersByUid.value = vm._resolvedUsersByUid.value + resolvedEntries
            if (fetched.isNotEmpty()) {
                households.filter { vm.isUserCreatorOfHousehold(it) }.forEach { h ->
                    val missingForH = h.memberUids.filter { uid ->
                        uid != h.ownerId && h.memberEmails[uid].isNullOrBlank() && fetched[uid]?.email?.isNotBlank() == true
                    }
                    if (missingForH.isNotEmpty()) {
                        val emails = missingForH.associateWith { fetched[it]!!.email }
                        val names = missingForH.associateWith { fetched[it]!!.displayName.ifBlank { fetched[it]!!.email } }
                        vm.firestoreSyncRepo.backfillHouseholdMemberMetadata(h.id, emails, names)
                    }
                }
            }
        }
    }

    fun isMariosUser(user: FirebaseUser? = null, email: String? = null, uid: String? = null): Boolean {
        val effectiveUser = user ?: vm._currentUser.value ?: vm.auth.currentUser
        val effectiveEmail = (email ?: effectiveUser?.email).orEmpty().trim()
        val effectiveUid = uid ?: effectiveUser?.uid.orEmpty()
        return effectiveEmail.equals(FirestoreDataSeed.DEFAULT_USER_EMAIL, ignoreCase = true) ||
            effectiveUid == FirestoreDataSeed.DEFAULT_USER_UID ||
            effectiveUid == "t5kN4RvPRugIxXUz18Fep2khSrC2" ||
            effectiveUid == "tSLWmkXZ0MTqIKGoX3wd6HdhDw63" ||
            effectiveUid == "jllDLLQl7VgFzOQpVIHi0wo8B013" ||
            effectiveUid == "oA6smvWcC6QYCAKMvYdWEcmVCO43"
    }

    fun getUserRoleForHousehold(household: Household?, user: FirebaseUser? = vm._currentUser.value): HouseholdRole {
        val effectiveUser = user ?: return HouseholdRole.CREATOR
        return StartupSafety.resolveHouseholdRole(
            household = household,
            userUid = effectiveUser.uid,
            userEmail = effectiveUser.email.orEmpty(),
            isMarios = isMariosUser(effectiveUser),
            mariosDefaultIds = vm.mariosDefaultHouseholdIds
        )
    }

    fun getOrEnsureHouseholdId(user: FirebaseUser? = null): String {
        val effectiveUser = user ?: vm._currentUser.value ?: vm.auth.currentUser ?: return ""
        val isMarios = isMariosUser(effectiveUser)
        val mariosHids = vm.mariosDefaultHouseholdIds
        val savedLastHid = vm.prefs.getString("last_household_id", "").orEmpty()
        val userSpecificPrefHid = vm.prefs.getString("user_household_${effectiveUser.uid}", "").orEmpty()
        val cachedProfile = vm._userProfile.value

        val explicitlyKnownHids = buildSet {
            cachedProfile?.householdId?.takeIf { it.isNotBlank() }?.let { add(it) }
            cachedProfile?.householdIds?.filter { it.isNotBlank() }?.forEach { add(it) }
            vm._userHouseholds.value.map { it.id }.filter { it.isNotBlank() }.forEach { add(it) }
            vm._currentHousehold.value?.takeIf { h ->
                h.id.isNotBlank() && (h.ownerId == effectiveUser.uid || h.memberUids.contains(effectiveUser.uid) || h.memberRoles.containsKey(effectiveUser.uid))
            }?.id?.let { add(it) }
            if (userSpecificPrefHid.isNotBlank() && (!mariosHids.contains(userSpecificPrefHid) || userSpecificPrefHid == savedLastHid)) {
                add(userSpecificPrefHid)
            }
        }

        val currentHid = vm._currentHousehold.value?.id.orEmpty()
        if (StartupSafety.isHouseholdAllowedForUser(currentHid, isMarios, mariosHids, explicitlyKnownHids)) return currentHid
        if (isMarios) {
            return cachedProfile?.householdId?.takeIf { it.isNotBlank() } ?: FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID
        }
        if (StartupSafety.isHouseholdAllowedForUser(savedLastHid, false, mariosHids, explicitlyKnownHids)) return savedLastHid
        val profileHid = cachedProfile?.householdId.orEmpty()
        if (StartupSafety.isHouseholdAllowedForUser(profileHid, false, mariosHids, explicitlyKnownHids)) return profileHid
        val profileHids = cachedProfile?.householdIds?.firstOrNull {
            StartupSafety.isHouseholdAllowedForUser(it, false, mariosHids, explicitlyKnownHids)
        }.orEmpty()
        if (profileHids.isNotBlank()) return profileHids
        val userHids = vm._userHouseholds.value.firstOrNull {
            StartupSafety.isHouseholdAllowedForUser(it.id, false, mariosHids, explicitlyKnownHids)
        }?.id.orEmpty()
        if (userHids.isNotBlank()) return userHids
        if (StartupSafety.isHouseholdAllowedForUser(userSpecificPrefHid, false, mariosHids, explicitlyKnownHids)) return userSpecificPrefHid

        val safeUserHid = "household_${effectiveUser.uid}"
        vm.prefs.edit()
            .putString("user_household_${effectiveUser.uid}", safeUserHid)
            .putString("last_household_id", safeUserHid)
            .apply()
        if (vm._currentHousehold.value == null || !StartupSafety.isHouseholdAllowedForUser(vm._currentHousehold.value?.id.orEmpty(), false, mariosHids, explicitlyKnownHids)) {
            val defaultH = Household(
                id = safeUserHid,
                name = "Το Εξοδολόγιό μου",
                ownerId = effectiveUser.uid,
                memberUids = listOf(effectiveUser.uid),
                memberRoles = mapOf(effectiveUser.uid to HouseholdRole.CREATOR.key)
            )
            vm._currentHousehold.value = defaultH
            if (vm._userHouseholds.value.none { it.id == safeUserHid }) {
                vm._userHouseholds.value = listOf(defaultH)
            }
        }
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try {
                vm.firestoreSyncRepo.ensureHouseholdExists(safeUserHid, effectiveUser.uid, "Το Εξοδολόγιό μου")
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "ensureHouseholdExists background check: ${e.message}")
            }
        }
        return safeUserHid
    }

    fun openExodologioDetailsDialog() {
        vm.dismissAllSubPages()
        vm._showExodologioDetailsDialog.value = true
        resolveMissingHouseholdMemberEmails(vm._userHouseholds.value)
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) { refreshUserHouseholds() }
    }

    fun openExodologioManager() {
        vm._showExodologioManagerDialog.value = true
        resolveMissingHouseholdMemberEmails(vm._userHouseholds.value)
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) { refreshUserHouseholds() }
    }

    fun createHousehold(familyName: String) {
        val user = vm._currentUser.value ?: return
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                vm._isSyncing.value = true
                val household = vm.firestoreSyncRepo.createHousehold(user.uid, familyName)
                vm.prefs.edit().putString("last_household_id", household.id).apply()
                vm._currentHousehold.value = household
                vm.observeHouseholdData(household.id)
                vm.showSnackbar("Δημιουργήθηκε το εξοδολόγιο: ${household.name}!")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                vm.showSnackbar("Σφάλμα δημιουργίας: ${e.message}")
            } finally {
                vm._isSyncing.value = false
                vm.trackSyncEnd()
            }
        }
    }

    fun joinHousehold(code: String, role: String? = null) {
        val user = vm._currentUser.value ?: return
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                vm._isSyncing.value = true
                val result = vm.firestoreSyncRepo.joinHouseholdByCode(user.uid, code, role)
                result.onSuccess { h ->
                    vm.prefs.edit().putString("last_household_id", h.id).apply()
                    vm._currentHousehold.value = h
                    vm.observeHouseholdData(h.id)
                    vm.showSnackbar("Συνδεθήκατε στο εξοδολόγιο: ${h.name}!")
                }.onFailure { err ->
                    vm.showSnackbar(err.message ?: "Αποτυχία σύνδεσης")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                vm.showSnackbar("Σφάλμα σύνδεσης: ${e.message}")
            } finally {
                vm._isSyncing.value = false
                vm.trackSyncEnd()
            }
        }
    }

    fun updateHouseholdInviteRole(role: String) {
        val hid = vm._currentHousehold.value?.id ?: return
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try {
                vm.firestoreSyncRepo.updateHouseholdInviteRole(hid, role)
                vm._currentHousehold.value = vm._currentHousehold.value?.copy(pendingInviteRole = role)
            } catch (_: Exception) {}
        }
    }

    fun searchVerifiedUserForSharing(email: String, householdId: String) {
        val user = vm._currentUser.value ?: return
        val cleanEmail = email.trim()
        val isEn = vm._appLanguage.value == "EN"
        if (cleanEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            vm._verifiedUserLookupState.value = OikoNomosViewModel.VerifiedUserLookupState.Error(
                if (isEn) "Enter a valid email" else "Εισάγετε έγκυρο email"
            )
            return
        }
        val targetHousehold = vm._userHouseholds.value.find { it.id == householdId } ?: vm._currentHousehold.value
        if (targetHousehold != null && !vm.isUserCreatorOfHousehold(targetHousehold)) {
            vm._verifiedUserLookupState.value = OikoNomosViewModel.VerifiedUserLookupState.Error(
                if (isEn) "Only the creator can share" else "Μόνο ο δημιουργός μπορεί να κοινοποιήσει"
            )
            return
        }
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm._verifiedUserLookupState.value = OikoNomosViewModel.VerifiedUserLookupState.Searching
            try {
                when (
                    val res = vm.firestoreSyncRepo.findVerifiedUserByEmail(
                        email = cleanEmail,
                        currentUserUid = user.uid,
                        currentUserEmail = user.email ?: vm._userProfile.value?.email.orEmpty()
                    )
                ) {
                    is FirestoreSyncRepository.VerifiedUserLookupResult.Found -> {
                        val found = res.user
                        val alreadyInHousehold = targetHousehold != null && (
                            targetHousehold.ownerId == found.uid ||
                                targetHousehold.memberUids.contains(found.uid) ||
                                targetHousehold.memberEmails.values.any { it.equals(found.email, ignoreCase = true) }
                            )
                        vm._verifiedUserLookupState.value = if (alreadyInHousehold) {
                            OikoNomosViewModel.VerifiedUserLookupState.AlreadyMember
                        } else {
                            OikoNomosViewModel.VerifiedUserLookupState.Found(found)
                        }
                    }
                    FirestoreSyncRepository.VerifiedUserLookupResult.SelfEmail ->
                        vm._verifiedUserLookupState.value = OikoNomosViewModel.VerifiedUserLookupState.SelfEmail
                    FirestoreSyncRepository.VerifiedUserLookupResult.NotVerified ->
                        vm._verifiedUserLookupState.value = OikoNomosViewModel.VerifiedUserLookupState.NotVerified
                    FirestoreSyncRepository.VerifiedUserLookupResult.NotFound ->
                        vm._verifiedUserLookupState.value = OikoNomosViewModel.VerifiedUserLookupState.NotFound
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                vm._verifiedUserLookupState.value = OikoNomosViewModel.VerifiedUserLookupState.Error(
                    if (isEn) "Search error" else "Σφάλμα αναζήτησης"
                )
            }
        }
    }

    fun shareHouseholdWithVerifiedUser(householdId: String, targetUser: UserProfile, role: HouseholdRole) {
        val targetHousehold = vm._userHouseholds.value.find { it.id == householdId } ?: vm._currentHousehold.value
        val isEn = vm._appLanguage.value == "EN"
        if (targetHousehold != null && !vm.isUserCreatorOfHousehold(targetHousehold)) {
            vm.showSnackbar(if (isEn) "Only the creator can share this household" else "Μόνο ο δημιουργός μπορεί να κοινοποιήσει το εξοδολόγιο")
            return
        }
        val assignedRole = if (role == HouseholdRole.CREATOR) HouseholdRole.REGISTAR else role
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                vm._isSyncing.value = true
                val updated = vm.firestoreSyncRepo.shareHouseholdWithUser(householdId, targetUser, assignedRole)
                if (updated != null) {
                    vm._userHouseholds.value = vm._userHouseholds.value.map { if (it.id == householdId) updated else it }
                    if (vm._currentHousehold.value?.id == householdId) vm._currentHousehold.value = updated
                }
                vm._verifiedUserLookupState.value = OikoNomosViewModel.VerifiedUserLookupState.Idle
                val displayName = targetUser.displayName.ifBlank { targetUser.email }
                vm.logHouseholdActivity(
                    actionType = "SHARED_USER_ADDED",
                    targetType = "HOUSEHOLD",
                    summaryEl = "Κοινοποίηση σε $displayName",
                    summaryEn = "Shared with $displayName",
                    householdIdOverride = householdId
                )
                vm.showSnackbar(if (isEn) "Added $displayName" else "Προστέθηκε ο χρήστης $displayName")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                vm.showSnackbar(if (isEn) "Error sharing: ${e.message}" else "Σφάλμα κοινοποίησης: ${e.message}")
            } finally {
                vm._isSyncing.value = false
                vm.trackSyncEnd()
            }
        }
    }

    fun updateSharedUserRole(householdId: String, targetUid: String, targetEmail: String, newRole: HouseholdRole) {
        val targetHousehold = vm._userHouseholds.value.find { it.id == householdId } ?: vm._currentHousehold.value
        val isEn = vm._appLanguage.value == "EN"
        if (targetHousehold != null && !vm.isUserCreatorOfHousehold(targetHousehold)) {
            vm.showSnackbar(if (isEn) "Only the creator can change roles" else "Μόνο ο δημιουργός μπορεί να αλλάξει δικαιώματα")
            return
        }
        if (targetHousehold?.ownerId == targetUid || newRole == HouseholdRole.CREATOR) return
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                val updated = vm.firestoreSyncRepo.updateSharedUserRole(householdId, targetUid, targetEmail, newRole)
                if (updated != null) {
                    vm._userHouseholds.value = vm._userHouseholds.value.map { if (it.id == householdId) updated else it }
                    if (vm._currentHousehold.value?.id == householdId) vm._currentHousehold.value = updated
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                vm.showSnackbar(if (isEn) "Error updating role: ${e.message}" else "Σφάλμα αλλαγής ρόλου: ${e.message}")
            } finally {
                vm.trackSyncEnd()
            }
        }
    }

    fun removeSharedUserFromHousehold(householdId: String, targetUid: String, targetEmail: String) {
        val targetHousehold = vm._userHouseholds.value.find { it.id == householdId } ?: vm._currentHousehold.value
        val isEn = vm._appLanguage.value == "EN"
        if (targetHousehold != null && !vm.isUserCreatorOfHousehold(targetHousehold)) {
            vm.showSnackbar(if (isEn) "Only the creator can remove users" else "Μόνο ο δημιουργός μπορεί να αφαιρέσει χρήστες")
            return
        }
        if (targetHousehold?.ownerId == targetUid) return
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                val updated = vm.firestoreSyncRepo.removeSharedUser(householdId, targetUid, targetEmail)
                if (updated != null) {
                    vm._userHouseholds.value = vm._userHouseholds.value.map { if (it.id == householdId) updated else it }
                    if (vm._currentHousehold.value?.id == householdId) vm._currentHousehold.value = updated
                }
                if (targetUid.isNotBlank()) {
                    // Unlink any member assigned to removed user
                    val linkedMembers = vm._allMembers.value.filter { it.linkedUserUid == targetUid }
                    if (linkedMembers.isNotEmpty()) {
                        val updatedMembers = vm._allMembers.value.map { if (it.linkedUserUid == targetUid) it.copy(linkedUserUid = "") else it }
                        vm._allMembers.value = updatedMembers
                        vm.repository.updateMembers(updatedMembers)
                        linkedMembers.forEach { m ->
                            val cleared = m.copy(linkedUserUid = "")
                            vm.firestoreSyncRepo.syncMember(
                                householdId,
                                FirebaseMember(
                                    id = cleared.id.toString(),
                                    householdId = householdId,
                                    userId = vm._currentUser.value?.uid.orEmpty(),
                                    name = cleared.name,
                                    role = cleared.role,
                                    avatarColorHex = cleared.avatarColorHex,
                                    voiceKeywords = cleared.voiceKeywords,
                                    isDefault = cleared.isDefault,
                                    sortOrder = cleared.sortOrder,
                                    linkedUserUid = ""
                                )
                            )
                        }
                    }
                }
                vm.showSnackbar(if (isEn) "User removed" else "Ο χρήστης αφαιρέθηκε")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                vm.showSnackbar(if (isEn) "Error removing user: ${e.message}" else "Σφάλμα αφαίρεσης: ${e.message}")
            } finally {
                vm.trackSyncEnd()
            }
        }
    }

    /**
     * Feature 1.1: Enforces a 1-to-1 mapping between connected Firebase UID and a household FamilyMember.
     * Passing memberId == null unlinks any existing mapping for targetUid.
     */
    fun assignUserDefaultPayer(targetUid: String, memberId: Long?) {
        if (!vm.hasOwnerPermissions()) {
            val isEn = vm._appLanguage.value == "EN"
            vm.showSnackbar(if (isEn) "Only Creators or Editors can assign default payers" else "Μόνο ο Δημιουργός ή Συντάκτης μπορεί να ορίσει προεπιλεγμένο πληρωτή")
            return
        }
        val cleanUid = targetUid.trim()
        if (cleanUid.isBlank()) return
        val householdId = vm._currentHousehold.value?.id.orEmpty()
        val currentUserUid = vm._currentUser.value?.uid.orEmpty()

        val prevMembers = vm._allMembers.value
        val changedMembers = mutableListOf<FamilyMember>()
        val updatedList = prevMembers.map { m ->
            val updated = when {
                memberId != null && m.id == memberId -> m.copy(linkedUserUid = cleanUid)
                m.linkedUserUid == cleanUid -> m.copy(linkedUserUid = "")
                else -> m
            }
            if (updated != m) changedMembers.add(updated)
            updated
        }
        if (changedMembers.isEmpty()) return

        vm._allMembers.value = updatedList
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                vm.repository.updateMembers(changedMembers)
                if (householdId.isNotBlank()) {
                    changedMembers.forEach { m ->
                        vm.firestoreSyncRepo.syncMember(
                            householdId,
                            FirebaseMember(
                                id = m.id.toString(),
                                householdId = householdId,
                                userId = currentUserUid,
                                name = m.name,
                                role = m.role,
                                avatarColorHex = m.avatarColorHex,
                                voiceKeywords = m.voiceKeywords,
                                isDefault = m.isDefault,
                                sortOrder = m.sortOrder,
                                linkedUserUid = m.linkedUserUid
                            )
                        )
                    }
                }
                val isEn = vm._appLanguage.value == "EN"
                val assignedMember = updatedList.find { memberId != null && it.id == memberId }
                vm.showSnackbar(
                    if (assignedMember != null) {
                        if (isEn) "Default payer set to ${assignedMember.name}" else "Προεπιλεγμένος πληρωτής: ${assignedMember.name}"
                    } else {
                        if (isEn) "Default payer link removed" else "Αφαιρέθηκε η σύνδεση πληρωτή"
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w("OikoNomosVM", "assignUserDefaultPayer error: ${e.message}")
            } finally {
                vm.trackSyncEnd()
            }
        }
    }

    fun inviteMemberByEmail(email: String, role: String = "REGISTAR", @Suppress("UNUSED_PARAMETER") context: Context? = null, onComplete: ((Boolean, String) -> Unit)? = null) {
        val hid = vm._currentHousehold.value?.id ?: return
        val cleanEmail = email.trim()
        val isEn = vm._appLanguage.value == "EN"
        if (cleanEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            val err = if (isEn) "Please enter a valid email address" else "Παρακαλώ εισάγετε έγκυρη διεύθυνση email"
            vm.showSnackbar(err)
            onComplete?.invoke(false, err)
            return
        }
        val targetHousehold = vm._currentHousehold.value
        if (targetHousehold != null && !vm.isUserCreatorOfHousehold(targetHousehold)) {
            val err = if (isEn) "Only the creator can share this household" else "Μόνο ο δημιουργός μπορεί να κοινοποιήσει το εξοδολόγιο"
            vm.showSnackbar(err)
            onComplete?.invoke(false, err)
            return
        }
        val assignedRole = HouseholdRole.fromRaw(role, isCreator = false).let {
            if (it == HouseholdRole.CREATOR) HouseholdRole.REGISTAR else it
        }
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                vm._isSyncing.value = true
                val user = vm._currentUser.value
                when (
                    val lookup = vm.firestoreSyncRepo.findVerifiedUserByEmail(
                        email = cleanEmail,
                        currentUserUid = user?.uid.orEmpty(),
                        currentUserEmail = user?.email ?: vm._userProfile.value?.email.orEmpty()
                    )
                ) {
                    is FirestoreSyncRepository.VerifiedUserLookupResult.Found -> {
                        val updated = vm.firestoreSyncRepo.shareHouseholdWithUser(hid, lookup.user, assignedRole)
                        if (updated != null) {
                            vm._userHouseholds.value = vm._userHouseholds.value.map { if (it.id == hid) updated else it }
                            if (vm._currentHousehold.value?.id == hid) vm._currentHousehold.value = updated
                        }
                        val displayName = lookup.user.displayName.ifBlank { cleanEmail }
                        vm.showSnackbar(if (isEn) "Household shared with $displayName!" else "Το εξοδολόγιο κοινοποιήθηκε στον χρήστη $displayName!")
                        onComplete?.invoke(true, cleanEmail)
                    }
                    else -> {
                        val notFoundMsg = if (isEn) "Not found" else "Δεν βρέθηκε"
                        vm.showSnackbar(notFoundMsg)
                        onComplete?.invoke(false, notFoundMsg)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val errMsg = if (isEn) "Error sharing household: ${e.message}" else "Σφάλμα κοινοποίησης: ${e.message}"
                vm.showSnackbar(errMsg)
                onComplete?.invoke(false, errMsg)
            } finally {
                vm._isSyncing.value = false
                vm.trackSyncEnd()
            }
        }
    }

    fun removeMemberByEmail(email: String) {
        val hid = vm._currentHousehold.value?.id ?: return
        val cleanEmail = email.trim()
        val isEn = vm._appLanguage.value == "EN"
        val ownerEmail = vm._currentUser.value?.email ?: vm._userProfile.value?.email ?: ""
        if (cleanEmail.equals(ownerEmail, ignoreCase = true)) {
            vm.showSnackbar(if (isEn) "Cannot remove the household creator" else "Δεν μπορείτε να αφαιρέσετε τον δημιουργό του εξοδολογίου")
            return
        }
        val uidMatch = vm._currentHousehold.value?.memberEmails?.entries?.firstOrNull {
            it.value.equals(cleanEmail, ignoreCase = true)
        }?.key.orEmpty()
        removeSharedUserFromHousehold(hid, uidMatch, cleanEmail)
    }

    fun getDeletedHouseholdIds(): Set<String> {
        val user = vm._currentUser.value ?: vm.auth.currentUser
        val uid = user?.uid ?: "global"
        val userDeleted = vm.prefs.getStringSet("deleted_households_$uid", emptySet()) ?: emptySet()
        val globalDeleted = vm.prefs.getStringSet("deleted_households_global", emptySet()) ?: emptySet()
        return (userDeleted + globalDeleted).toSet()
    }

    private fun markHouseholdAsDeleted(householdId: String) {
        val user = vm._currentUser.value ?: vm.auth.currentUser
        val uid = user?.uid ?: "global"
        val existing = vm.prefs.getStringSet("deleted_households_$uid", emptySet())?.toMutableSet() ?: mutableSetOf()
        existing.add(householdId)
        val global = vm.prefs.getStringSet("deleted_households_global", emptySet())?.toMutableSet() ?: mutableSetOf()
        global.add(householdId)
        vm.prefs.edit()
            .putStringSet("deleted_households_$uid", existing)
            .putStringSet("deleted_households_global", global)
            .commit()
    }

    fun deleteHousehold(household: Household) {
        val user = vm._currentUser.value ?: vm.auth.currentUser ?: return
        if (!vm.isUserCreatorOfHousehold(household)) {
            vm.showSnackbar(if (vm._appLanguage.value == "EN") "Only the household creator can delete it" else "Μόνο ο δημιουργός μπορεί να διαγράψει το εξοδολόγιο")
            return
        }
        markHouseholdAsDeleted(household.id)
        val isCurrent = vm._currentHousehold.value?.id == household.id
        val remaining = vm._userHouseholds.value.filter { it.id != household.id && !getDeletedHouseholdIds().contains(it.id) }
        val nextHid = if (remaining.isNotEmpty()) remaining.first().id else ""
        vm._userHouseholds.value = remaining
        val p = vm._userProfile.value
        if (p != null) {
            val updatedIds = p.householdIds.filter { it != household.id && !getDeletedHouseholdIds().contains(it) }
            val updatedProfile = p.copy(
                householdIds = updatedIds,
                householdId = if (isCurrent && nextHid.isNotBlank()) nextHid else p.householdId
            )
            vm._userProfile.value = updatedProfile
        }
        vm.dismissExodologioManager()
        vm.dismissExodologioDetailsDialog()
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try {
                vm.firestoreSyncRepo.deleteHousehold(household.id, user.uid, nextHid.ifBlank { null })
                if (remaining.isNotEmpty()) {
                    if (isCurrent) switchExodologio(remaining.first())
                } else {
                    val defaultName = if (vm._appLanguage.value == "EN") "My Household Book" else "Το Εξοδολόγιό μου"
                    val newHousehold = vm.firestoreSyncRepo.createHousehold(user.uid, defaultName)
                    vm._userHouseholds.value = listOf(newHousehold)
                    val currentProf = vm._userProfile.value
                    if (currentProf != null) {
                        val updatedProf = currentProf.copy(householdIds = listOf(newHousehold.id), householdId = newHousehold.id)
                        vm._userProfile.value = updatedProf
                    }
                    switchExodologio(newHousehold)
                }
                vm.showSnackbar(
                    if (vm._appLanguage.value == "EN") "Household \"${household.name}\" deleted successfully"
                    else "Το εξοδολόγιο «${household.name}» διαγράφηκε επιτυχώς"
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                vm.showSnackbar(if (vm._appLanguage.value == "EN") "Error deleting household: ${e.message}" else "Σφάλμα διαγραφής εξοδολογίου: ${e.message}")
            }
        }
    }

    suspend fun refreshUserHouseholds() {
        val user = vm._currentUser.value ?: return
        val userEmail = user.email ?: vm._userProfile.value?.email ?: ""
        val deletedHids = getDeletedHouseholdIds()
        try {
            val allUserHouseholds = vm.firestoreSyncRepo.getAllUserHouseholds(user.uid, userEmail, deletedHids)
            val mariosDefaultIds = if (isMariosUser(user)) {
                setOf(FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID, "e1d4ae67-499")
            } else emptySet()
            val existingMarios = vm._userHouseholds.value.filter {
                mariosDefaultIds.contains(it.id) && allUserHouseholds.none { h -> h.id == it.id } && !deletedHids.contains(it.id)
            }
            val merged = (allUserHouseholds + existingMarios).filter { !deletedHids.contains(it.id) }.distinctBy { it.id }
            if (merged.isNotEmpty()) {
                vm._userHouseholds.value = merged
                resolveMissingHouseholdMemberEmails(merged)
                val cur = vm._currentHousehold.value
                vm._currentHousehold.value = if (cur != null) merged.find { it.id == cur.id } ?: merged.first() else merged.first()
                val p = vm._userProfile.value
                if (p != null) {
                    val validIds = merged.map { it.id }.filter { !deletedHids.contains(it) }.distinct()
                    if (p.householdIds != validIds) {
                        val updated = p.copy(householdIds = validIds)
                        vm._userProfile.value = updated
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w("OikoNomosVM", "refreshUserHouseholds error: ${e.message}")
        }
    }

    fun leaveSharedHousehold(household: Household) {
        val user = vm._currentUser.value ?: return
        val userEmail = user.email ?: vm._userProfile.value?.email ?: ""
        val isEn = vm._appLanguage.value == "EN"
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try {
                vm._isSyncing.value = true
                vm.firestoreSyncRepo.leaveSharedHousehold(household.id, user.uid, userEmail)
                val remaining = vm._userHouseholds.value.filter { it.id != household.id }
                vm._userHouseholds.value = remaining
                if (vm._currentHousehold.value?.id == household.id) {
                    remaining.firstOrNull()?.let { switchExodologio(it) }
                }
                vm.showSnackbar(if (isEn) "You left the household \"${household.name}\"" else "Αποχωρήσατε από το εξοδολόγιο «${household.name}»")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                vm.showSnackbar("Σφάλμα: ${e.localizedMessage}")
            } finally {
                vm._isSyncing.value = false
            }
        }
    }

    fun renameHousehold(householdId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank() || householdId.isBlank()) return
        val target = vm._userHouseholds.value.find { it.id == householdId } ?: vm._currentHousehold.value
        if (target != null && !vm.isUserCreatorOfHousehold(target)) {
            vm.showSnackbar(if (vm._appLanguage.value == "EN") "Only the creator can rename this household" else "Μόνο ο δημιουργός μπορεί να μετονομάσει το εξοδολόγιο")
            return
        }
        val cur = vm._currentHousehold.value
        if (cur?.id == householdId) vm._currentHousehold.value = cur.copy(name = trimmed)
        vm._userHouseholds.value = vm._userHouseholds.value.map { if (it.id == householdId) it.copy(name = trimmed) else it }
        vm.prefs.edit().putString("household_name_$householdId", trimmed).apply()
        vm.showSnackbar(if (vm._appLanguage.value == "EN") "Household renamed to \"$trimmed\"" else "Το εξοδολόγιο μετονομάστηκε σε «$trimmed»")
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try { vm.firestoreSyncRepo.renameHousehold(householdId, trimmed) } catch (_: Exception) {}
        }
    }

    fun renameCurrentHousehold(newName: String) {
        val cur = vm._currentHousehold.value ?: vm._userHouseholds.value.firstOrNull() ?: return
        renameHousehold(cur.id, newName)
    }

    fun createNewExodologio(name: String) {
        val user = vm._currentUser.value ?: return
        val profile = vm._userProfile.value ?: return
        val ownedCount = vm._userHouseholds.value.count { it.ownerId == user.uid }
        if (!profile.isPremium && !isMariosUser(user) && ownedCount >= 1) {
            vm.showPremiumPaywall("multi_exodologio")
            return
        }
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try {
                val newHousehold = vm.firestoreSyncRepo.createHousehold(user.uid, name)
                vm._userHouseholds.value = vm._userHouseholds.value + newHousehold
                val profileName = profile.displayName.ifBlank { "${profile.firstName} ${profile.lastName}".trim() }
                    .ifBlank { user.displayName ?: "Χρήστης" }
                val defaultMember = FirebaseMember(
                    id = "1",
                    householdId = newHousehold.id,
                    userId = user.uid,
                    name = profileName,
                    role = "Γονέας",
                    avatarColorHex = 0xFF059669,
                    voiceKeywords = profileName.lowercase(),
                    isDefault = true,
                    linkedUserUid = user.uid
                )
                vm.firestoreSyncRepo.syncMember(newHousehold.id, defaultMember)
                OikoNomosViewModel.STANDARD_DEFAULT_CATEGORIES.forEach { cat ->
                    vm.firestoreSyncRepo.syncCategory(
                        newHousehold.id,
                        FirebaseCategory(cat.id.toString(), newHousehold.id, user.uid, cat.name, cat.iconKey, 0.0, cat.colorHex, cat.voiceKeywords, cat.sortOrder, cat.fontColorHex)
                    )
                }
                switchExodologio(newHousehold)
                vm.showSnackbar("Το εξοδολόγιο «${newHousehold.name}» δημιουργήθηκε!")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                vm.showSnackbar("Σφάλμα δημιουργίας: ${e.localizedMessage}")
            }
        }
    }

    fun switchExodologio(household: Household) {
        val user = vm._currentUser.value ?: return
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try {
                // Cancel household-scoped listeners first so stale snapshots from the previous Exodologio cannot race
                vm.lastObservedHouseholdId = null
                vm.householdJob?.cancel()
                vm.expensesSyncJob?.cancel()
                vm.incomesSyncJob?.cancel()
                vm.membersSyncJob?.cancel()
                vm.categoriesSyncJob?.cancel()
                vm.savingsGoalsSyncJob?.cancel()
                vm.recurringSyncJob?.cancel()
                vm.activitySyncJob?.cancel()

                vm.prefs.edit().putString("last_household_id", household.id).apply()
                vm.firestoreSyncRepo.switchActiveHousehold(user.uid, household.id)
                vm._currentHousehold.value = household
                vm._userProfile.value = vm._userProfile.value?.copy(householdId = household.id)
                vm.lastHouseholdExpenses = emptyList()
                vm.lastHouseholdIncomes = emptyList()
                vm.lastHouseholdMembers = emptyList()
                vm.lastHouseholdCategories = emptyList()
                vm.lastHouseholdSavingsGoals = emptyList()
                vm.lastUserExpenses = emptyList()
                vm.lastUserIncomes = emptyList()
                vm.lastUserMembers = emptyList()
                vm.lastUserCategories = emptyList()
                vm.savingsGoalCloudIdByLocalId.clear()
                vm.pendingLocalSavingsGoals.clear()
                vm.pendingDeletedSavingsGoals.clear()
                vm.inFlightCreateGoalKeys.clear()
                vm._allExpenses.value = emptyList()
                vm._allIncomes.value = emptyList()
                vm._allMembers.value = emptyList()
                vm._allCategories.value = emptyList()
                vm._allSavingsGoals.value = emptyList()
                vm._householdActivities.value = emptyList()
                vm._pendingUndoDeletion.value = null
                vm.undoCountdownJob?.cancel()
                vm._manualTriggeredReminderIds.value = emptySet()
                vm._reminderClockTickMs.value = System.currentTimeMillis()
                vm.allRecurringExpenses.value.forEach { rec ->
                    com.example.reminder.BillReminderManager.cancelReminder(vm.getApplication(), rec.id)
                }
                vm._homeSelectedMonthKey.value = null
                vm._homeSelectedTypeFilter.value = "EXPENSE"
                vm._selectedMemberFilter.value = null
                vm._selectedCategoryFilter.value = null
                vm._searchQuery.value = ""
                vm._activeSpecialFilter.value = null
                vm.repository.deleteAllExpenses()
                vm.repository.deleteAllIncomes()
                vm.repository.replaceAllMembers(emptyList())
                vm.repository.replaceAllCategories(emptyList())
                vm.repository.replaceAllSavingsGoals(emptyList())
                vm.repository.replaceAllRecurringExpenses(emptyList())
                vm.loadDeletedItems(household.id)
                vm.observeHouseholdData(household.id)
                vm.showSnackbar("Ενεργό εξοδολόγιο: ${household.name}")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                vm.showSnackbar("Σφάλμα αλλαγής: ${e.localizedMessage}")
            }
        }
    }
}
