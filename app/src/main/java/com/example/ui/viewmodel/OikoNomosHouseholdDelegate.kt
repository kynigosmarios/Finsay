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
import com.example.data.repository.FirestoreTransactionsDelegate
import com.example.util.CurrencyFormatter
import com.example.util.DefaultPayerHelper
import com.example.util.Localization
import com.example.util.QrCodeHelper
import com.example.util.StartupSafety
import com.example.util.TelemetryHelper
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

    fun getUserRoleForHousehold(household: Household?, user: FirebaseUser? = vm._currentUser.value): HouseholdRole {
        val effectiveUser = user ?: return HouseholdRole.CREATOR
        return StartupSafety.resolveHouseholdRole(
            household = household,
            userUid = effectiveUser.uid,
            userEmail = effectiveUser.email.orEmpty(),
            isMarios = false,
            mariosDefaultIds = emptySet()
        )
    }

    fun getOrEnsureHouseholdId(user: FirebaseUser? = null): String {
        val effectiveUser = user ?: vm._currentUser.value ?: vm.auth.currentUser ?: return ""
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
            if (userSpecificPrefHid.isNotBlank()) {
                add(userSpecificPrefHid)
            }
        }

        val currentHid = vm._currentHousehold.value?.id.orEmpty()
        if (StartupSafety.isHouseholdAllowedForUser(currentHid, false, emptySet(), explicitlyKnownHids)) return currentHid
        if (StartupSafety.isHouseholdAllowedForUser(savedLastHid, false, emptySet(), explicitlyKnownHids)) return savedLastHid
        val profileHid = cachedProfile?.householdId.orEmpty()
        if (StartupSafety.isHouseholdAllowedForUser(profileHid, false, emptySet(), explicitlyKnownHids)) return profileHid
        val profileHids = cachedProfile?.householdIds?.firstOrNull {
            StartupSafety.isHouseholdAllowedForUser(it, false, emptySet(), explicitlyKnownHids)
        }.orEmpty()
        if (profileHids.isNotBlank()) return profileHids
        val userHids = vm._userHouseholds.value.firstOrNull {
            StartupSafety.isHouseholdAllowedForUser(it.id, false, emptySet(), explicitlyKnownHids)
        }?.id.orEmpty()
        if (userHids.isNotBlank()) return userHids
        if (StartupSafety.isHouseholdAllowedForUser(userSpecificPrefHid, false, emptySet(), explicitlyKnownHids)) return userSpecificPrefHid

        val safeUserHid = "household_${effectiveUser.uid}"
        vm.prefs.edit()
            .putString("user_household_${effectiveUser.uid}", safeUserHid)
            .putString("last_household_id", safeUserHid)
            .apply()
        val lang = Localization.Language.fromCode(vm._appLanguage.value)
        val defaultHouseholdName = Localization.myExodologio(lang)
        if (vm._currentHousehold.value == null || !StartupSafety.isHouseholdAllowedForUser(vm._currentHousehold.value?.id.orEmpty(), false, emptySet(), explicitlyKnownHids)) {
            val defaultH = Household(
                id = safeUserHid,
                name = defaultHouseholdName,
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
                vm.firestoreSyncRepo.ensureHouseholdExists(safeUserHid, effectiveUser.uid, defaultHouseholdName)
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
                val lang = Localization.Language.fromCode(vm._appLanguage.value)
                vm.showSnackbar(
                    Localization.trFormat(
                        lang,
                        "Household book created: %1\$s!",
                        "Δημιουργήθηκε το εξοδολόγιο: %1\$s!",
                        household.name
                    )
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val lang = Localization.Language.fromCode(vm._appLanguage.value)
                vm.showSnackbar(
                    Localization.trFormat(
                        lang,
                        "Creation error: %1\$s",
                        "Σφάλμα δημιουργίας: %1\$s",
                        e.localizedMessage ?: e.message.orEmpty()
                    )
                )
            } finally {
                vm._isSyncing.value = false
                vm.trackSyncEnd()
            }
        }
    }

    fun generateHouseholdInvite(household: Household, role: HouseholdRole) {
        val user = vm._currentUser.value ?: vm.auth.currentUser ?: return
        val lang = Localization.Language.fromCode(vm._appLanguage.value)
        if (!vm.isUserCreatorOfHousehold(household)) {
            vm.showSnackbar(
                Localization.tr(
                    lang,
                    "Only the creator can share this household",
                    "Μόνο ο δημιουργός μπορεί να κοινοποιήσει το εξοδολόγιο"
                )
            )
            return
        }
        val safeRole = if (role == HouseholdRole.CREATOR) HouseholdRole.REGISTAR else role
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm._isGeneratingInvite.value = true
            try {
                val res = vm.firestoreSyncRepo.createHouseholdInvite(household, user.uid, safeRole)
                res.onSuccess { invite ->
                    vm._activeHouseholdInvite.value = invite
                }.onFailure { err ->
                    TelemetryHelper.recordNonFatal("generateHouseholdInvite", err)
                    vm.showSnackbar(
                        Localization.tr(
                            lang,
                            "Could not create invite link",
                            "Δεν ήταν δυνατή η δημιουργία συνδέσμου πρόσκλησης"
                        )
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                TelemetryHelper.recordNonFatal("generateHouseholdInvite", e)
                vm.showSnackbar(
                    Localization.tr(
                        lang,
                        "Could not create invite link",
                        "Δεν ήταν δυνατή η δημιουργία συνδέσμου πρόσκλησης"
                    )
                )
            } finally {
                vm._isGeneratingInvite.value = false
            }
        }
    }

    fun handleIncomingInviteCode(rawCode: String) {
        val extracted = QrCodeHelper.extractInviteCode(rawCode) ?: return
        vm._pendingIncomingInviteCode.value = extracted
    }

    fun joinHousehold(code: String, role: String? = null) {
        val user = vm._currentUser.value ?: vm.auth.currentUser ?: return
        val cleanCode = QrCodeHelper.extractInviteCode(code) ?: code.trim()
        val lang = Localization.Language.fromCode(vm._appLanguage.value)
        if (cleanCode.isBlank()) {
            vm.showSnackbar(Localization.tr(lang, "Invalid invite code", "Μη έγκυρος κωδικός πρόσκλησης"))
            return
        }
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                vm._isSyncing.value = true
                val userEmail = user.email ?: vm._userProfile.value?.email.orEmpty()
                val userDisplayName = vm._userProfile.value?.displayName?.ifBlank { null }
                    ?: user.displayName?.ifBlank { null }
                    ?: userEmail.substringBefore("@")
                val result = vm.firestoreSyncRepo.joinHouseholdByCode(
                    userUid = user.uid,
                    inviteCode = cleanCode,
                    role = role,
                    userEmail = userEmail,
                    userDisplayName = userDisplayName
                )
                result.onSuccess { h ->
                    vm._pendingIncomingInviteCode.value = null
                    val mergedList = (vm._userHouseholds.value.filter { it.id != h.id } + h).distinctBy { it.id }
                    vm._userHouseholds.value = mergedList
                    switchExodologio(h)
                    vm.showSnackbar(
                        Localization.trFormat(
                            lang,
                            "Joined household book: %1\$s!",
                            "Συνδεθήκατε στο εξοδολόγιο: %1\$s!",
                            h.name
                        )
                    )
                }.onFailure { err ->
                    val localizedErr = when (err.message) {
                        "INVALID_CODE" -> Localization.tr(lang, "Invalid invite code", "Μη έγκυρος κωδικός πρόσκλησης")
                        "INVITE_NOT_FOUND" -> Localization.tr(lang, "Invite link not found or revoked", "Η πρόσκληση δεν βρέθηκε ή έχει ανακληθεί")
                        "INVITE_EXPIRED" -> Localization.tr(lang, "Invite link has expired", "Η πρόσκληση έχει λήξει")
                        else -> err.message ?: Localization.tr(lang, "Failed to join", "Αποτυχία σύνδεσης")
                    }
                    vm.showSnackbar(localizedErr)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                TelemetryHelper.recordNonFatal("joinHousehold", e)
                vm.showSnackbar(
                    Localization.trFormat(
                        lang,
                        "Connection error: %1\$s",
                        "Σφάλμα σύνδεσης: %1\$s",
                        e.localizedMessage ?: e.message.orEmpty()
                    )
                )
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
        val lang = Localization.Language.fromCode(vm._appLanguage.value)
        if (cleanEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            vm._verifiedUserLookupState.value = OikoNomosViewModel.VerifiedUserLookupState.Error(
                Localization.tr(lang, "Enter a valid email", "Εισάγετε έγκυρο email")
            )
            return
        }
        val targetHousehold = vm._userHouseholds.value.find { it.id == householdId } ?: vm._currentHousehold.value
        if (targetHousehold != null && !vm.isUserCreatorOfHousehold(targetHousehold)) {
            vm._verifiedUserLookupState.value = OikoNomosViewModel.VerifiedUserLookupState.Error(
                Localization.tr(lang, "Only the creator can share", "Μόνο ο δημιουργός μπορεί να κοινοποιήσει")
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
                    Localization.tr(lang, "Search error", "Σφάλμα αναζήτησης")
                )
            }
        }
    }

    fun shareHouseholdWithVerifiedUser(householdId: String, targetUser: UserProfile, role: HouseholdRole) {
        val targetHousehold = vm._userHouseholds.value.find { it.id == householdId } ?: vm._currentHousehold.value
        val lang = Localization.Language.fromCode(vm._appLanguage.value)
        if (targetHousehold != null && !vm.isUserCreatorOfHousehold(targetHousehold)) {
            vm.showSnackbar(Localization.tr(lang, "Only the creator can share this household", "Μόνο ο δημιουργός μπορεί να κοινοποιήσει το εξοδολόγιο"))
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
                vm.showSnackbar(Localization.trFormat(lang, "Added %1\$s", "Προστέθηκε ο χρήστης %1\$s", displayName))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                vm.showSnackbar(Localization.trFormat(lang, "Error sharing: %1\$s", "Σφάλμα κοινοποίησης: %1\$s", e.message.orEmpty()))
            } finally {
                vm._isSyncing.value = false
                vm.trackSyncEnd()
            }
        }
    }

    fun updateSharedUserRole(householdId: String, targetUid: String, targetEmail: String, newRole: HouseholdRole) {
        val targetHousehold = vm._userHouseholds.value.find { it.id == householdId } ?: vm._currentHousehold.value
        val lang = Localization.Language.fromCode(vm._appLanguage.value)
        if (targetHousehold != null && !vm.isUserCreatorOfHousehold(targetHousehold)) {
            vm.showSnackbar(Localization.tr(lang, "Only the creator can change roles", "Μόνο ο δημιουργός μπορεί να αλλάξει δικαιώματα"))
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
                vm.showSnackbar(Localization.trFormat(lang, "Error updating role: %1\$s", "Σφάλμα αλλαγής ρόλου: %1\$s", e.message.orEmpty()))
            } finally {
                vm.trackSyncEnd()
            }
        }
    }

    fun removeSharedUserFromHousehold(householdId: String, targetUid: String, targetEmail: String) {
        val targetHousehold = vm._userHouseholds.value.find { it.id == householdId } ?: vm._currentHousehold.value
        val lang = Localization.Language.fromCode(vm._appLanguage.value)
        if (targetHousehold != null && !vm.isUserCreatorOfHousehold(targetHousehold)) {
            vm.showSnackbar(Localization.tr(lang, "Only the creator can remove users", "Μόνο ο δημιουργός μπορεί να αφαιρέσει χρήστες"))
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
                vm.showSnackbar(Localization.tr(lang, "User removed", "Ο χρήστης αφαιρέθηκε"))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                vm.showSnackbar(Localization.trFormat(lang, "Error removing user: %1\$s", "Σφάλμα αφαίρεσης: %1\$s", e.message.orEmpty()))
            } finally {
                vm.trackSyncEnd()
            }
        }
    }

    /**
     * Allows ANY household member (CREATOR, EDITOR, or REGISTAR) to set 0, 1, 2, or more
     * default payers for themselves on a household. The CREATOR may also configure default
     * payers for shared users.
     */
    fun assignUserDefaultPayers(householdId: String, targetUid: String, memberIds: List<Long>) {
        val cleanUid = targetUid.trim()
        val currentUserUid = vm._currentUser.value?.uid.orEmpty().trim()
        if (cleanUid.isBlank() || currentUserUid.isBlank()) return

        val resolvedHid = householdId.ifBlank { vm._currentHousehold.value?.id.orEmpty() }.trim()
        val targetHousehold = vm._userHouseholds.value.find { it.id == resolvedHid }
            ?: vm._currentHousehold.value?.takeIf { resolvedHid.isBlank() || it.id == resolvedHid }
        val lang = Localization.Language.fromCode(vm._appLanguage.value)

        if (!DefaultPayerHelper.canAssignDefaultPayers(targetHousehold, currentUserUid, cleanUid)) {
            vm.showSnackbar(
                Localization.tr(
                    lang,
                    "Only the owner can change another user's default payer",
                    "Μόνο ο ιδιοκτήτης μπορεί να αλλάξει τον προεπιλεγμένο πληρωτή άλλου χρήστη"
                )
            )
            return
        }

        val validIds = memberIds.filter { it > 0L }.distinct()
        val csv = DefaultPayerHelper.formatMemberIdsCsv(validIds)

        if (resolvedHid.isNotBlank()) {
            vm.prefs.edit().putString("default_payers_v2_${resolvedHid}_${cleanUid}", csv).apply()
            vm._localDefaultPayersOverride.value = vm._localDefaultPayersOverride.value + ("$resolvedHid|$cleanUid" to csv)
        }

        if (targetHousehold != null) {
            val updatedMap = targetHousehold.memberDefaultPayers.toMutableMap().apply {
                put(cleanUid, csv)
            }
            val optimisticHousehold = targetHousehold.copy(memberDefaultPayers = updatedMap)
            vm._userHouseholds.value = vm._userHouseholds.value.map {
                if (it.id == targetHousehold.id) optimisticHousehold else it
            }
            if (vm._currentHousehold.value?.id == targetHousehold.id) {
                vm._currentHousehold.value = optimisticHousehold
            }
        }

        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.trackSyncStart()
            try {
                if (resolvedHid.isNotBlank()) {
                    val synced = vm.firestoreSyncRepo.updateUserDefaultPayers(resolvedHid, cleanUid, csv)
                    if (synced != null) {
                        vm._userHouseholds.value = vm._userHouseholds.value.map {
                            if (it.id == resolvedHid) synced else it
                        }
                        if (vm._currentHousehold.value?.id == resolvedHid) {
                            vm._currentHousehold.value = synced
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w("OikoNomosVM", "assignUserDefaultPayers sync error: ${e.message}")
            } finally {
                vm.trackSyncEnd()
            }
        }
    }

    fun assignUserDefaultPayer(targetUid: String, memberId: Long?) {
        assignUserDefaultPayers(vm._currentHousehold.value?.id.orEmpty(), targetUid, listOfNotNull(memberId))
    }

    fun inviteMemberByEmail(email: String, role: String = "REGISTAR", @Suppress("UNUSED_PARAMETER") context: Context? = null, onComplete: ((Boolean, String) -> Unit)? = null) {
        val hid = vm._currentHousehold.value?.id ?: return
        val cleanEmail = email.trim()
        val lang = Localization.Language.fromCode(vm._appLanguage.value)
        if (cleanEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            val err = Localization.tr(lang, "Please enter a valid email address", "Παρακαλώ εισάγετε έγκυρη διεύθυνση email")
            vm.showSnackbar(err)
            onComplete?.invoke(false, err)
            return
        }
        val targetHousehold = vm._currentHousehold.value
        if (targetHousehold != null && !vm.isUserCreatorOfHousehold(targetHousehold)) {
            val err = Localization.tr(lang, "Only the creator can share this household", "Μόνο ο δημιουργός μπορεί να κοινοποιήσει το εξοδολόγιο")
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
                        vm.showSnackbar(
                            Localization.trFormat(
                                lang,
                                "Household shared with %1\$s!",
                                "Το εξοδολόγιο κοινοποιήθηκε στον χρήστη %1\$s!",
                                displayName
                            )
                        )
                        onComplete?.invoke(true, cleanEmail)
                    }
                    else -> {
                        val notFoundMsg = Localization.tr(lang, "Not found", "Δεν βρέθηκε")
                        vm.showSnackbar(notFoundMsg)
                        onComplete?.invoke(false, notFoundMsg)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val errMsg = Localization.trFormat(lang, "Error sharing household: %1\$s", "Σφάλμα κοινοποίησης: %1\$s", e.message.orEmpty())
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
        val lang = Localization.Language.fromCode(vm._appLanguage.value)
        val ownerEmail = vm._currentUser.value?.email ?: vm._userProfile.value?.email ?: ""
        if (cleanEmail.equals(ownerEmail, ignoreCase = true)) {
            vm.showSnackbar(Localization.tr(lang, "Cannot remove the household creator", "Δεν μπορείτε να αφαιρέσετε τον δημιουργό του εξοδολογίου"))
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
        val lang = Localization.Language.fromCode(vm._appLanguage.value)
        if (!vm.isUserCreatorOfHousehold(household)) {
            vm.showSnackbar(Localization.tr(lang, "Only the household creator can delete it", "Μόνο ο δημιουργός μπορεί να διαγράψει το εξοδολόγιο"))
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
                    val defaultName = Localization.myExodologio(lang)
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
                    Localization.trFormat(
                        lang,
                        "Household \"%1\$s\" deleted successfully",
                        "Το εξοδολόγιο «%1\$s» διαγράφηκε επιτυχώς",
                        household.name
                    )
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                vm.showSnackbar(
                    Localization.trFormat(
                        lang,
                        "Error deleting household: %1\$s",
                        "Σφάλμα διαγραφής εξοδολογίου: %1\$s",
                        e.message.orEmpty()
                    )
                )
            }
        }
    }

    suspend fun refreshUserHouseholds() {
        val user = vm._currentUser.value ?: return
        val userEmail = user.email ?: vm._userProfile.value?.email ?: ""
        val deletedHids = getDeletedHouseholdIds()
        try {
            val allUserHouseholds = vm.firestoreSyncRepo.getAllUserHouseholds(user.uid, userEmail, deletedHids)
            val merged = allUserHouseholds.filter { !deletedHids.contains(it.id) }.distinctBy { it.id }
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
        val lang = Localization.Language.fromCode(vm._appLanguage.value)
        vm.showSnackbar(
            Localization.tr(
                lang,
                "Only the household owner can remove users",
                "Μόνο ο ιδιοκτήτης μπορεί να αφαιρέσει χρήστες από το εξοδολόγιο"
            )
        )
    }

    fun renameHousehold(householdId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank() || householdId.isBlank()) return
        val lang = Localization.Language.fromCode(vm._appLanguage.value)
        val target = vm._userHouseholds.value.find { it.id == householdId } ?: vm._currentHousehold.value
        if (target != null && !vm.isUserCreatorOfHousehold(target)) {
            vm.showSnackbar(Localization.tr(lang, "Only the creator can rename this household", "Μόνο ο δημιουργός μπορεί να μετονομάσει το εξοδολόγιο"))
            return
        }
        val cur = vm._currentHousehold.value
        if (cur?.id == householdId) vm._currentHousehold.value = cur.copy(name = trimmed)
        vm._userHouseholds.value = vm._userHouseholds.value.map { if (it.id == householdId) it.copy(name = trimmed) else it }
        vm.prefs.edit().putString("household_name_$householdId", trimmed).apply()
        vm.showSnackbar(
            Localization.trFormat(
                lang,
                "Household renamed to \"%1\$s\"",
                "Το εξοδολόγιο μετονομάστηκε σε «%1\$s»",
                trimmed
            )
        )
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try { vm.firestoreSyncRepo.renameHousehold(householdId, trimmed) } catch (_: Exception) {}
        }
    }

    fun renameCurrentHousehold(newName: String) {
        val cur = vm._currentHousehold.value ?: vm._userHouseholds.value.firstOrNull() ?: return
        renameHousehold(cur.id, newName)
    }

    fun updateHouseholdCurrency(householdId: String, currencyCode: String) {
        val cleanCode = CurrencyFormatter.optionForCode(currencyCode).code
        val option = CurrencyFormatter.optionForCode(cleanCode)
        val lang = Localization.Language.fromCode(vm._appLanguage.value)
        val target = vm._userHouseholds.value.find { it.id == householdId }
            ?: vm._currentHousehold.value?.takeIf { it.id == householdId }
        if (target != null && !vm.isUserCreatorOfHousehold(target)) {
            vm.showSnackbar(
                Localization.tr(
                    lang,
                    "Only the household creator can change its currency.",
                    "Μόνο ο δημιουργός μπορεί να αλλάξει το νόμισμα του εξοδολογίου."
                )
            )
            return
        }
        vm._userHouseholds.value = vm._userHouseholds.value.map {
            if (it.id == householdId) it.copy(currencyCode = cleanCode) else it
        }
        if (vm._currentHousehold.value?.id == householdId) {
            val updated = vm._currentHousehold.value?.copy(currencyCode = cleanCode)
            vm._currentHousehold.value = updated
            vm.syncCurrencyFromHousehold(updated)
        }
        vm.showSnackbar(
            Localization.trFormat(
                lang,
                "Currency updated to %1\$s (%2\$s)",
                "Το νόμισμα άλλαξε σε %1\$s (%2\$s)",
                option.symbol,
                option.code
            )
        )
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try { vm.firestoreSyncRepo.updateHouseholdCurrency(householdId, cleanCode) } catch (_: Exception) {}
        }
    }

    fun createNewExodologio(name: String, currencyCode: String = "EUR") {
        val user = vm._currentUser.value ?: return
        val profile = vm._userProfile.value ?: return
        val ownedCount = vm._userHouseholds.value.count { it.ownerId == user.uid }
        if (!vm.isCurrentUserPro() && ownedCount >= 1) {
            vm.showPremiumPaywall("multi_exodologio")
            return
        }
        val cleanCurrency = CurrencyFormatter.optionForCode(currencyCode).code
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            val lang = Localization.Language.fromCode(vm._appLanguage.value)
            try {
                val newHousehold = vm.firestoreSyncRepo.createHousehold(user.uid, name, cleanCurrency)
                vm._userHouseholds.value = vm._userHouseholds.value + newHousehold
                val defaultRole = Localization.translateRole("Individual", lang)
                val profileName = profile.displayName.ifBlank { "${profile.firstName} ${profile.lastName}".trim() }
                    .ifBlank { user.displayName ?: Localization.tr(lang, "User", "Χρήστης") }
                val defaultMember = FirebaseMember(
                    id = "1",
                    householdId = newHousehold.id,
                    userId = user.uid,
                    name = profileName,
                    role = defaultRole,
                    avatarColorHex = 0xFF059669,
                    voiceKeywords = profileName.lowercase(),
                    isDefault = true,
                    linkedUserUid = user.uid
                )
                vm.firestoreSyncRepo.syncMember(newHousehold.id, defaultMember)
                val defaultExpCats = OikoNomosViewModel.getStandardDefaultCategories(lang).let { list ->
                    if (vm.isCurrentUserPro()) list else list.take(OikoNomosViewModel.FREE_MAX_EXPENSE_CATEGORIES)
                }
                defaultExpCats.forEach { cat ->
                    vm.firestoreSyncRepo.syncCategory(
                        newHousehold.id,
                        FirebaseCategory(cat.id.toString(), newHousehold.id, user.uid, cat.name, cat.iconKey, 0.0, cat.colorHex, cat.voiceKeywords, cat.sortOrder, cat.fontColorHex, false, "EXPENSE")
                    )
                }
                val defaultIncCats = OikoNomosViewModel.getStandardDefaultIncomeCategories(lang).let { list ->
                    if (vm.isCurrentUserPro()) list else list.take(OikoNomosViewModel.FREE_MAX_INCOME_CATEGORIES)
                }
                defaultIncCats.forEach { cat ->
                    vm.firestoreSyncRepo.syncCategory(
                        newHousehold.id,
                        FirebaseCategory(cat.id.toString(), newHousehold.id, user.uid, cat.name, cat.iconKey, 0.0, cat.colorHex, cat.voiceKeywords, cat.sortOrder, cat.fontColorHex, false, "INCOME")
                    )
                }
                switchExodologio(newHousehold)
                vm.showSnackbar(
                    Localization.trFormat(
                        lang,
                        "Household book \"%1\$s\" created!",
                        "Το εξοδολόγιο «%1\$s» δημιουργήθηκε!",
                        newHousehold.name
                    )
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                vm.showSnackbar(
                    Localization.trFormat(
                        lang,
                        "Creation error: %1\$s",
                        "Σφάλμα δημιουργίας: %1\$s",
                        e.localizedMessage ?: e.message.orEmpty()
                    )
                )
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
                vm._incomeCategories.value = emptyList()
                vm._allSavingsGoals.value = emptyList()
                vm._householdActivities.value = emptyList()
                vm._pendingUndoDeletion.value = null
                vm.undoCountdownJob?.cancel()
                vm._manualTriggeredReminderIds.value = emptySet()
                vm._reminderClockTickMs.value = System.currentTimeMillis()
                vm.allRecurringExpenses.value.forEach { rec ->
                    com.example.reminder.BillReminderManager.cancelReminder(vm.getApplication(), rec.id)
                }
                val freshCutoff = FirestoreTransactionsDelegate.defaultLiveWindowCutoffMs()
                vm.liveWindowCutoffMs = freshCutoff
                vm.olderExpensesCache = emptyList()
                vm.olderIncomesCache = emptyList()
                vm.oldestLoadedExpenseCursorMs = freshCutoff
                vm.oldestLoadedIncomeCursorMs = freshCutoff
                vm.hasMoreOlderExpenses = true
                vm.hasMoreOlderIncomes = true
                vm._hasMoreOlderHistory.value = true
                vm._isLoadingOlderHistory.value = false
                vm._monthlyTotalsSummary.value = emptyList()
                vm._homeSelectedMonthKey.value = null
                vm._homeSelectedTypeFilter.value = "EXPENSE"
                vm._selectedMemberFilter.value = null
                vm._selectedCategoryFilter.value = null
                vm._searchQuery.value = ""
                vm._activeSpecialFilter.value = null
                vm._advancedFilter.value = AdvancedExpenseFilter()
                vm.repository.deleteAllExpenses()
                vm.repository.deleteAllIncomes()
                vm.repository.replaceAllMembers(emptyList())
                vm.repository.replaceAllCategories(emptyList())
                vm.repository.replaceAllSavingsGoals(emptyList())
                vm.repository.replaceAllRecurringExpenses(emptyList())
                vm.loadDeletedItems(household.id)
                vm.observeHouseholdData(household.id)
                val lang = Localization.Language.fromCode(vm._appLanguage.value)
                vm.showSnackbar(
                    Localization.trFormat(
                        lang,
                        "Active household book: %1\$s",
                        "Ενεργό εξοδολόγιο: %1\$s",
                        household.name
                    )
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val lang = Localization.Language.fromCode(vm._appLanguage.value)
                vm.showSnackbar(
                    Localization.trFormat(
                        lang,
                        "Error switching book: %1\$s",
                        "Σφάλμα αλλαγής: %1\$s",
                        e.localizedMessage ?: e.message.orEmpty()
                    )
                )
            }
        }
    }
}
