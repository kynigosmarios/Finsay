package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.model.FirebaseCategory
import com.example.data.model.FirebaseMember
import com.example.data.model.UserProfile
import com.example.reminder.BillReminderManager
import com.example.reminder.DailyReminderManager
import com.example.util.AnalyticsSpecialFilter
import com.example.util.BankNotificationSystemManager
import com.example.util.HomeDrillDownRequest
import com.example.util.StartupSafety
import com.google.firebase.auth.FirebaseUser
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

internal class OikoNomosStructureDelegate(
    private val vm: OikoNomosViewModel
) {
    private val tag = "OikoNomosStruct"

    private fun normalizeDeletedKey(name: String): String =
        name.trim().lowercase(Locale.ROOT)
            .replace("ά", "α").replace("έ", "ε").replace("ή", "η")
            .replace("ί", "ι").replace("ό", "ο").replace("ύ", "υ").replace("ώ", "ω")

    fun getDeletedMemberIds(householdId: String): Set<String> {
        val fromIds = vm.prefs.getStringSet("deleted_member_ids_$householdId", emptySet()) ?: emptySet()
        val fromList = vm._deletedMembers.value.map { it.id.toString() }.toSet()
        return fromIds + fromList
    }

    fun getDeletedCategoryIds(householdId: String): Set<String> {
        val fromIds = vm.prefs.getStringSet("deleted_category_ids_$householdId", emptySet()) ?: emptySet()
        val fromList = vm._deletedCategories.value.map { it.id.toString() }.toSet()
        return fromIds + fromList
    }

    private fun getDeletedMemberNames(householdId: String): Set<String> {
        val fromPrefs = vm.prefs.getStringSet("deleted_member_names_$householdId", emptySet()) ?: emptySet()
        val fromList = vm._deletedMembers.value.map { normalizeDeletedKey(it.name) }.toSet()
        return fromPrefs + fromList
    }

    private fun getDeletedCategoryNames(householdId: String): Set<String> {
        val fromPrefs = vm.prefs.getStringSet("deleted_category_names_$householdId", emptySet()) ?: emptySet()
        val fromList = vm._deletedCategories.value.map { normalizeDeletedKey(it.name) }.toSet()
        return fromPrefs + fromList
    }

    fun loadDeletedItems(householdId: String) {
        if (householdId.isBlank()) return
        try {
            val membersJson = vm.prefs.getString("deleted_members_$householdId", null)
            if (membersJson != null) {
                val arr = JSONArray(membersJson)
                val list = mutableListOf<FamilyMember>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        FamilyMember(
                            id = o.getLong("id"),
                            name = o.getString("name"),
                            role = o.optString("role", "Γονέας"),
                            avatarColorHex = o.optLong("avatarColorHex", 0xFF059669),
                            voiceKeywords = o.optString("voiceKeywords", ""),
                            isDefault = o.optBoolean("isDefault", false),
                            sortOrder = o.optInt("sortOrder", i),
                            linkedUserUid = o.optString("linkedUserUid", "")
                        )
                    )
                }
                vm._deletedMembers.value = list
            } else {
                vm._deletedMembers.value = emptyList()
            }

            val catsJson = vm.prefs.getString("deleted_categories_$householdId", null)
            if (catsJson != null) {
                val arr = JSONArray(catsJson)
                val list = mutableListOf<ExpenseCategory>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        ExpenseCategory(
                            id = o.getLong("id"),
                            name = o.getString("name"),
                            iconKey = o.optString("iconKey", "other"),
                            monthlyBudget = o.optDouble("monthlyBudget", 0.0),
                            colorHex = o.optLong("colorHex", 0xFF059669),
                            voiceKeywords = o.optString("voiceKeywords", ""),
                            sortOrder = o.optInt("sortOrder", i),
                            fontColorHex = o.optLong("fontColorHex", 0L)
                        )
                    )
                }
                vm._deletedCategories.value = list
            } else {
                vm._deletedCategories.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error loading deleted items: ${e.message}")
        }
    }

    private fun persistDeletedMembers(householdId: String, list: List<FamilyMember>) {
        if (householdId.isBlank()) return
        try {
            val arr = JSONArray()
            val idSet = mutableSetOf<String>()
            val nameSet = mutableSetOf<String>()
            for (m in list) {
                idSet.add(m.id.toString())
                nameSet.add(normalizeDeletedKey(m.name))
                val o = JSONObject()
                o.put("id", m.id)
                o.put("name", m.name)
                o.put("role", m.role)
                o.put("avatarColorHex", m.avatarColorHex)
                o.put("voiceKeywords", m.voiceKeywords)
                o.put("isDefault", m.isDefault)
                o.put("sortOrder", m.sortOrder)
                o.put("linkedUserUid", m.linkedUserUid)
                arr.put(o)
            }
            vm.prefs.edit()
                .putString("deleted_members_$householdId", arr.toString())
                .putStringSet("deleted_member_ids_$householdId", idSet)
                .putStringSet("deleted_member_names_$householdId", nameSet)
                .apply()
        } catch (e: Exception) {
            Log.e(tag, "Error persisting deleted members: ${e.message}")
        }
    }

    private fun persistDeletedCategories(householdId: String, list: List<ExpenseCategory>) {
        if (householdId.isBlank()) return
        try {
            val arr = JSONArray()
            val idSet = mutableSetOf<String>()
            val nameSet = mutableSetOf<String>()
            for (c in list) {
                idSet.add(c.id.toString())
                nameSet.add(normalizeDeletedKey(c.name))
                val o = JSONObject()
                o.put("id", c.id)
                o.put("name", c.name)
                o.put("iconKey", c.iconKey)
                o.put("monthlyBudget", c.monthlyBudget)
                o.put("colorHex", c.colorHex)
                o.put("voiceKeywords", c.voiceKeywords)
                o.put("sortOrder", c.sortOrder)
                o.put("fontColorHex", c.fontColorHex)
                arr.put(o)
            }
            vm.prefs.edit()
                .putString("deleted_categories_$householdId", arr.toString())
                .putStringSet("deleted_category_ids_$householdId", idSet)
                .putStringSet("deleted_category_names_$householdId", nameSet)
                .apply()
        } catch (e: Exception) {
            Log.e(tag, "Error persisting deleted categories: ${e.message}")
        }
    }

    suspend fun syncAndMergeMembers(
        incomingMembers: List<FirebaseMember>,
        explicitProfile: UserProfile? = null,
        explicitUser: FirebaseUser? = null,
        explicitHouseholdId: String? = null
    ) {
        val currentHouseholdId = explicitHouseholdId?.takeIf { it.isNotBlank() } ?: vm._currentHousehold.value?.id.orEmpty()
        val deletedIds = getDeletedMemberIds(currentHouseholdId)
        val deletedNames = getDeletedMemberNames(currentHouseholdId)
        val uid = explicitUser?.uid ?: vm._currentUser.value?.uid ?: ""
        val userEmail = explicitProfile?.email ?: explicitUser?.email ?: vm._userProfile.value?.email ?: vm._currentUser.value?.email ?: ""

        val remoteDeleted = incomingMembers.filter { it.isDeleted }
        if (remoteDeleted.isNotEmpty() && currentHouseholdId.isNotBlank()) {
            val remoteDeletedEntities = remoteDeleted.map { rd ->
                val rawNumId = rd.id.toLongOrNull() ?: StartupSafety.safePositiveIdHash(rd.id)
                FamilyMember(
                    id = rawNumId,
                    name = rd.name,
                    role = rd.role,
                    avatarColorHex = rd.avatarColorHex,
                    voiceKeywords = rd.voiceKeywords,
                    isDefault = rd.isDefault,
                    sortOrder = rd.sortOrder,
                    linkedUserUid = rd.linkedUserUid
                )
            }
            val currentTrash = vm._deletedMembers.value.toMutableList()
            var changed = false
            for (rd in remoteDeletedEntities) {
                if (currentTrash.none { it.id == rd.id || normalizeDeletedKey(it.name) == normalizeDeletedKey(rd.name) }) {
                    currentTrash.add(rd)
                    changed = true
                }
            }
            if (changed) {
                vm._deletedMembers.value = currentTrash
                persistDeletedMembers(currentHouseholdId, currentTrash)
            }
        }

        val effectiveDeletedIds = getDeletedMemberIds(currentHouseholdId) + remoteDeleted.map { it.id }.toSet()
        val effectiveDeletedNames = getDeletedMemberNames(currentHouseholdId) + remoteDeleted.map { normalizeDeletedKey(it.name) }.toSet()
        val activeIncoming = incomingMembers.filter { !it.isDeleted }

        val finalMembers: List<FamilyMember> = if (activeIncoming.isNotEmpty()) {
            val mapped = activeIncoming.map { fm ->
                val resolvedId = fm.id.toLongOrNull() ?: StartupSafety.safePositiveIdHash(fm.id)
                Triple(resolvedId, fm, normalizeDeletedKey(fm.name))
            }
                .filter { (resolvedId, _, normName) ->
                    !effectiveDeletedIds.contains(resolvedId.toString()) &&
                        !deletedIds.contains(resolvedId.toString()) &&
                        !effectiveDeletedNames.contains(normName) &&
                        !deletedNames.contains(normName)
                }
                .groupBy { it.third }
                .map { (_, group) ->
                    val chosen = group.minByOrNull { it.second.sortOrder }!!
                    FamilyMember(
                        id = chosen.first,
                        name = chosen.second.name,
                        role = chosen.second.role,
                        avatarColorHex = chosen.second.avatarColorHex,
                        voiceKeywords = chosen.second.voiceKeywords,
                        isDefault = chosen.second.isDefault,
                        sortOrder = chosen.second.sortOrder,
                        linkedUserUid = chosen.second.linkedUserUid
                    )
                }
                .sortedWith(compareBy<FamilyMember> { it.sortOrder }.thenBy { it.id })

            val localExisting = vm.repository.getAllMembersList()
            val areDifferent = localExisting.size != mapped.size || localExisting.zip(mapped).any { (e, m) ->
                e.name != m.name || e.role != m.role || e.avatarColorHex != m.avatarColorHex ||
                    e.sortOrder != m.sortOrder || e.linkedUserUid != m.linkedUserUid
            }
            if (areDifferent) {
                vm.repository.replaceAllMembers(mapped)
            }
            mapped
        } else if (remoteDeleted.isNotEmpty()) {
            vm.repository.replaceAllMembers(emptyList())
            emptyList()
        } else {
            val currentLocal = vm.repository.getAllMembersList()
            if (currentLocal.isNotEmpty() && !currentLocal.all { it.name == "Εγώ" }) {
                currentLocal
            } else {
                val u = explicitUser ?: vm._currentUser.value ?: vm.auth.currentUser
                val p = explicitProfile ?: vm._userProfile.value
                val profileName = p?.displayName?.ifBlank { null }
                    ?: "${p?.firstName ?: ""} ${p?.lastName ?: ""}".trim().ifBlank { null }
                    ?: u?.displayName?.ifBlank { null }
                    ?: u?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                    ?: "Εγώ"

                val defaultMember = FamilyMember(
                    id = 1L,
                    name = profileName,
                    role = "Γονέας",
                    avatarColorHex = 0xFF059669,
                    voiceKeywords = profileName.lowercase(),
                    isDefault = true,
                    sortOrder = 0,
                    linkedUserUid = uid
                )
                val list = listOf(defaultMember)
                vm.repository.replaceAllMembers(list)
                val canWriteStructure = StartupSafety.canSafelyWriteRemoteStructure(
                    targetHousehold = vm._currentHousehold.value,
                    userUid = uid,
                    userEmail = userEmail,
                    isMarios = false,
                    mariosDefaultIds = emptySet()
                )
                if (currentHouseholdId.isNotBlank() && uid.isNotBlank() && canWriteStructure) {
                    vm.firestoreSyncRepo.syncMember(
                        currentHouseholdId,
                        FirebaseMember("1", currentHouseholdId, uid, defaultMember.name, defaultMember.role, defaultMember.avatarColorHex, defaultMember.voiceKeywords, true, 0, false, uid)
                    )
                }
                list
            }
        }
        vm._allMembers.value = finalMembers
    }

    suspend fun syncAndMergeCategories(
        incomingCats: List<FirebaseCategory>,
        explicitHouseholdId: String? = null,
        explicitUserId: String? = null
    ) {
        val currentHouseholdId = explicitHouseholdId?.takeIf { it.isNotBlank() } ?: vm._currentHousehold.value?.id.orEmpty()
        val uid = explicitUserId?.takeIf { it.isNotBlank() } ?: vm._currentUser.value?.uid ?: vm.auth.currentUser?.uid ?: ""
        val deletedIds = getDeletedCategoryIds(currentHouseholdId)
        val deletedNames = getDeletedCategoryNames(currentHouseholdId)

        val remoteDeleted = incomingCats.filter { it.isDeleted }
        if (remoteDeleted.isNotEmpty() && currentHouseholdId.isNotBlank()) {
            val remoteDeletedEntities = remoteDeleted.map { rc ->
                val rawNumId = rc.id.toLongOrNull() ?: StartupSafety.safePositiveIdHash(rc.id)
                ExpenseCategory(rawNumId, rc.name, rc.iconKey, rc.monthlyBudget, rc.colorHex, rc.voiceKeywords, rc.sortOrder, rc.fontColorHex)
            }
            val currentTrash = vm._deletedCategories.value.toMutableList()
            var changed = false
            for (rc in remoteDeletedEntities) {
                if (currentTrash.none { it.id == rc.id || normalizeDeletedKey(it.name) == normalizeDeletedKey(rc.name) }) {
                    currentTrash.add(rc)
                    changed = true
                }
            }
            if (changed) {
                vm._deletedCategories.value = currentTrash
                persistDeletedCategories(currentHouseholdId, currentTrash)
            }
        }

        val effectiveDeletedIds = getDeletedCategoryIds(currentHouseholdId) + remoteDeleted.map { it.id }.toSet()
        val effectiveDeletedNames = getDeletedCategoryNames(currentHouseholdId) + remoteDeleted.map { normalizeDeletedKey(it.name) }.toSet()
        val activeIncoming = incomingCats.filter { !it.isDeleted }

        val finalCats: List<ExpenseCategory> = if (activeIncoming.isNotEmpty()) {
            val mapped = activeIncoming.map { fc ->
                val resolvedId = fc.id.toLongOrNull() ?: StartupSafety.safePositiveIdHash(fc.id)
                Triple(resolvedId, fc, normalizeDeletedKey(fc.name))
            }
                .filter { (resolvedId, _, normName) ->
                    !effectiveDeletedIds.contains(resolvedId.toString()) &&
                        !deletedIds.contains(resolvedId.toString()) &&
                        !effectiveDeletedNames.contains(normName) &&
                        !deletedNames.contains(normName)
                }
                .groupBy { it.third }
                .map { (_, group) ->
                    val chosen = group.minByOrNull { it.second.sortOrder }!!
                    ExpenseCategory(
                        id = chosen.first,
                        name = chosen.second.name,
                        iconKey = chosen.second.iconKey,
                        monthlyBudget = chosen.second.monthlyBudget,
                        colorHex = chosen.second.colorHex,
                        voiceKeywords = chosen.second.voiceKeywords,
                        sortOrder = chosen.second.sortOrder,
                        fontColorHex = chosen.second.fontColorHex
                    )
                }
                .sortedWith(compareBy<ExpenseCategory> { it.sortOrder }.thenBy { it.id })

            val localExisting = vm.repository.getAllCategoriesList()
            val areDifferent = localExisting.size != mapped.size || localExisting.zip(mapped).any { (e, m) ->
                e.name != m.name || e.monthlyBudget != m.monthlyBudget || e.colorHex != m.colorHex ||
                    e.fontColorHex != m.fontColorHex || e.sortOrder != m.sortOrder
            }
            if (areDifferent) {
                vm.repository.replaceAllCategories(mapped)
            }
            mapped
        } else if (remoteDeleted.isNotEmpty()) {
            vm.repository.replaceAllCategories(emptyList())
            emptyList()
        } else {
            val seedList = OikoNomosViewModel.STANDARD_DEFAULT_CATEGORIES
                .map { it.copy(monthlyBudget = 0.0) }
                .filter {
                    !deletedIds.contains(it.id.toString()) && !deletedNames.contains(normalizeDeletedKey(it.name))
                }
            vm.repository.replaceAllCategories(seedList)
            if (currentHouseholdId.isNotBlank() && uid.isNotBlank() && vm.hasOwnerPermissions()) {
                seedList.forEach { cat ->
                    vm.firestoreSyncRepo.syncCategory(
                        currentHouseholdId,
                        FirebaseCategory(cat.id.toString(), currentHouseholdId, uid, cat.name, cat.iconKey, cat.monthlyBudget, cat.colorHex, cat.voiceKeywords, cat.sortOrder, cat.fontColorHex)
                    )
                }
            }
            seedList
        }
        vm._allCategories.value = finalCats
    }

    fun seedStandardCategories() {
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.repository.seedStandardCategories()
            val list = vm.repository.getAllCategoriesList()
            vm._allCategories.value = list
            val hid = vm._currentHousehold.value?.id.orEmpty()
            val uid = vm._currentUser.value?.uid.orEmpty()
            if (hid.isNotBlank() && uid.isNotBlank() && vm.hasOwnerPermissions()) {
                list.forEach { cat ->
                    vm.firestoreSyncRepo.syncCategory(
                        hid,
                        FirebaseCategory(cat.id.toString(), hid, uid, cat.name, cat.iconKey, cat.monthlyBudget, cat.colorHex, cat.voiceKeywords, cat.sortOrder, cat.fontColorHex)
                    )
                }
            }
        }
    }

    fun openMemberDialog(member: FamilyMember? = null) {
        if (!vm.hasOwnerPermissions()) {
            vm.showSnackbar(
                if (vm._appLanguage.value == "EN") "Only Creators or Editors can manage members"
                else "Μόνο ο Δημιουργός ή Συντάκτης μπορεί να επεξεργαστεί μέλη"
            )
            return
        }
        if (member == null && !vm.isCurrentUserPro() && vm._allMembers.value.size >= 4) {
            vm.showPremiumPaywall("member_limit")
            vm.showSnackbar("Η δωρεάν έκδοση υποστηρίζει έως 4 Μέλη")
            return
        }
        vm._editingMember.value = member
        vm._showMemberDialog.value = true
    }

    fun saveMember(
        id: Long = 0,
        name: String,
        role: String,
        avatarColorHex: Long,
        voiceKeywords: String,
        linkedUserUid: String = ""
    ) {
        if (!vm.hasOwnerPermissions()) {
            vm.showSnackbar(
                if (vm._appLanguage.value == "EN") "Only Creators or Editors can save members"
                else "Μόνο ο Δημιουργός ή Συντάκτης μπορεί να αποθηκεύσει μέλη"
            )
            return
        }
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            val cleanLinkedUid = linkedUserUid.trim()
            val currentList = vm._allMembers.value
            val existingById = if (id != 0L) currentList.find { it.id == id } else null
            val sortOrder = existingById?.sortOrder ?: ((currentList.maxOfOrNull { it.sortOrder } ?: -1) + 1)

            val member = FamilyMember(
                id = id,
                name = name.trim(),
                role = role,
                avatarColorHex = avatarColorHex,
                voiceKeywords = voiceKeywords.ifBlank { name.trim().lowercase() },
                isDefault = existingById?.isDefault ?: (id == 0L && currentList.isEmpty()),
                sortOrder = sortOrder,
                linkedUserUid = cleanLinkedUid
            )
            val savedId = if (id == 0L) {
                vm.repository.insertMember(member)
            } else {
                vm.repository.updateMember(member)
                id
            }
            val finalSaved = member.copy(id = savedId)

            // 1.1: Enforce 1-to-1 unique UID link across members
            val unlinkedOthers = mutableListOf<FamilyMember>()
            val newList = currentList
                .filter { it.id != savedId }
                .map { other ->
                    if (cleanLinkedUid.isNotBlank() && other.linkedUserUid == cleanLinkedUid) {
                        val cleared = other.copy(linkedUserUid = "")
                        unlinkedOthers.add(cleared)
                        cleared
                    } else other
                } + finalSaved

            if (unlinkedOthers.isNotEmpty()) {
                vm.repository.updateMembers(unlinkedOthers)
            }

            vm._allMembers.value = newList.sortedWith(compareBy<FamilyMember> { it.sortOrder }.thenBy { it.id })
            val user = vm._currentUser.value
            val householdId = vm.getOrEnsureHouseholdId(user)
            val userHouseholds = vm._userHouseholds.value.map { it.id }.ifEmpty { listOf(householdId).filter { it.isNotBlank() } }

            if (user != null || householdId.isNotBlank()) {
                val fbMember = FirebaseMember(
                    id = finalSaved.id.toString(),
                    householdId = householdId,
                    userId = user?.uid ?: "",
                    name = finalSaved.name,
                    role = finalSaved.role,
                    avatarColorHex = finalSaved.avatarColorHex,
                    voiceKeywords = finalSaved.voiceKeywords,
                    isDefault = finalSaved.isDefault,
                    sortOrder = finalSaved.sortOrder,
                    linkedUserUid = finalSaved.linkedUserUid
                )
                vm.firestoreSyncRepo.syncAndCleanMember(userHouseholds, user?.uid ?: "", fbMember, existingById?.name ?: "")
                unlinkedOthers.forEach { other ->
                    vm.firestoreSyncRepo.syncMember(
                        householdId,
                        FirebaseMember(
                            id = other.id.toString(),
                            householdId = householdId,
                            userId = user?.uid ?: "",
                            name = other.name,
                            role = other.role,
                            avatarColorHex = other.avatarColorHex,
                            voiceKeywords = other.voiceKeywords,
                            isDefault = other.isDefault,
                            sortOrder = other.sortOrder,
                            linkedUserUid = ""
                        )
                    )
                }
            }
            vm.logHouseholdActivity(
                actionType = if (id == 0L) "MEMBER_ADDED" else "MEMBER_UPDATED",
                targetType = "MEMBER",
                summaryEl = if (id == 0L) "Νέο μέλος: ${finalSaved.name}" else "Ενημέρωση μέλους: ${finalSaved.name}",
                summaryEn = if (id == 0L) "New member: ${finalSaved.name}" else "Updated member: ${finalSaved.name}",
                householdIdOverride = householdId
            )
            vm.showSnackbar(
                if (id == 0L) "Το μέλος «${finalSaved.name}» προστέθηκε!"
                else "Το μέλος «${finalSaved.name}» ενημερώθηκε!"
            )
        }
    }

    fun updateMemberColor(member: FamilyMember, newColorHex: Long) {
        if (!vm.hasOwnerPermissions()) return
        val updated = member.copy(avatarColorHex = newColorHex)
        vm._allMembers.value = vm._allMembers.value.map { if (it.id == member.id) updated else it }
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.repository.updateMember(updated)
            val householdId = vm._currentHousehold.value?.id.orEmpty()
            val uid = vm._currentUser.value?.uid.orEmpty()
            if (householdId.isNotBlank()) {
                vm.firestoreSyncRepo.syncMember(
                    householdId,
                    FirebaseMember(updated.id.toString(), householdId, uid, updated.name, updated.role, updated.avatarColorHex, updated.voiceKeywords, updated.isDefault, updated.sortOrder, false, updated.linkedUserUid)
                )
            }
        }
    }

    fun softDeleteMember(member: FamilyMember) {
        if (!vm.hasOwnerPermissions()) {
            vm.showSnackbar("Μόνο ο Δημιουργός ή Συντάκτης μπορεί να διαγράψει μέλη")
            return
        }
        if (vm._allMembers.value.size <= 1) {
            vm.showSnackbar("Πρέπει να υπάρχει τουλάχιστον ένα μέλος στην οικογένεια")
            return
        }
        val householdId = vm._currentHousehold.value?.id.orEmpty()
        val remaining = vm._allMembers.value.filter { it.id != member.id }.toMutableList()
        if (member.isDefault && remaining.isNotEmpty()) {
            remaining[0] = remaining[0].copy(isDefault = true)
        }
        vm._allMembers.value = remaining
        val trash = vm._deletedMembers.value + member
        vm._deletedMembers.value = trash
        persistDeletedMembers(householdId, trash)

        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.repository.deleteMember(member)
            if (member.isDefault && remaining.isNotEmpty()) {
                vm.repository.updateMember(remaining[0])
            }
            if (householdId.isNotBlank()) {
                vm.firestoreSyncRepo.setMemberDeleted(householdId, member.id.toString(), member.name, true)
                if (member.isDefault && remaining.isNotEmpty()) {
                    val r = remaining[0]
                    vm.firestoreSyncRepo.syncMember(
                        householdId,
                        FirebaseMember(r.id.toString(), householdId, vm._currentUser.value?.uid ?: "", r.name, r.role, r.avatarColorHex, r.voiceKeywords, true, r.sortOrder, false, r.linkedUserUid)
                    )
                }
            }
            vm.showSnackbar("Το μέλος «${member.name}» μεταφέρθηκε στον κάδο")
        }
    }

    fun restoreMember(member: FamilyMember) {
        if (!vm.hasOwnerPermissions()) return
        val householdId = vm._currentHousehold.value?.id.orEmpty()
        val remainingTrash = vm._deletedMembers.value.filter { it.id != member.id }
        vm._deletedMembers.value = remainingTrash
        persistDeletedMembers(householdId, remainingTrash)

        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            val insertedId = vm.repository.insertMember(member)
            val restored = member.copy(id = insertedId)
            vm._allMembers.value = (vm._allMembers.value + restored).distinctBy { it.id }.sortedWith(compareBy<FamilyMember> { it.sortOrder }.thenBy { it.id })
            if (householdId.isNotBlank()) {
                vm.firestoreSyncRepo.syncMember(
                    householdId,
                    FirebaseMember(restored.id.toString(), householdId, vm._currentUser.value?.uid ?: "", restored.name, restored.role, restored.avatarColorHex, restored.voiceKeywords, restored.isDefault, restored.sortOrder, false, restored.linkedUserUid)
                )
            }
            vm.showSnackbar("Το μέλος «${member.name}» επανήλθε!")
        }
    }

    fun permanentDeleteMember(member: FamilyMember) {
        if (!vm.hasOwnerPermissions()) return
        val householdId = vm._currentHousehold.value?.id.orEmpty()
        val remainingTrash = vm._deletedMembers.value.filter { it.id != member.id }
        vm._deletedMembers.value = remainingTrash
        persistDeletedMembers(householdId, remainingTrash)
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            if (householdId.isNotBlank()) {
                vm.firestoreSyncRepo.permanentDeleteMemberExact(householdId, member.id.toString(), member.name)
            }
        }
    }

    fun openCategoryDialog(category: ExpenseCategory? = null) {
        if (!vm.hasOwnerPermissions()) {
            vm.showSnackbar(
                if (vm._appLanguage.value == "EN") "Only Creators or Editors can manage categories"
                else "Μόνο ο Δημιουργός ή Συντάκτης μπορεί να διαχειριστεί κατηγορίες"
            )
            return
        }
        vm._editingCategory.value = category
        vm._showCategoryDialog.value = true
    }

    fun saveCategory(
        id: Long,
        name: String,
        iconKey: String,
        monthlyBudget: Double,
        colorHex: Long,
        voiceKeywords: String,
        fontColorHex: Long = 0L
    ) {
        if (!vm.hasOwnerPermissions()) return
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            val currentList = vm._allCategories.value
            val existingById = if (id != 0L) currentList.find { it.id == id } else null
            val sortOrder = existingById?.sortOrder ?: ((currentList.maxOfOrNull { it.sortOrder } ?: -1) + 1)

            val cat = ExpenseCategory(
                id = id,
                name = name.trim(),
                iconKey = iconKey,
                monthlyBudget = monthlyBudget,
                colorHex = colorHex,
                voiceKeywords = voiceKeywords,
                sortOrder = sortOrder,
                fontColorHex = fontColorHex
            )
            val savedId = if (id == 0L) vm.repository.insertCategory(cat) else {
                vm.repository.updateCategory(cat)
                id
            }
            val finalSaved = cat.copy(id = savedId)
            vm._allCategories.value = (currentList.filter { it.id != savedId } + finalSaved)
                .sortedWith(compareBy<ExpenseCategory> { it.sortOrder }.thenBy { it.id })

            val user = vm._currentUser.value
            val householdId = vm.getOrEnsureHouseholdId(user)
            val userHouseholds = vm._userHouseholds.value.map { it.id }.ifEmpty { listOf(householdId).filter { it.isNotBlank() } }
            val fbCat = FirebaseCategory(finalSaved.id.toString(), householdId, user?.uid ?: "", finalSaved.name, finalSaved.iconKey, finalSaved.monthlyBudget, finalSaved.colorHex, finalSaved.voiceKeywords, finalSaved.sortOrder, finalSaved.fontColorHex)
            vm.firestoreSyncRepo.syncAndCleanCategory(userHouseholds, user?.uid ?: "", fbCat, existingById?.name ?: "")

            vm.logHouseholdActivity(
                actionType = if (id == 0L) "CATEGORY_ADDED" else "CATEGORY_UPDATED",
                targetType = "CATEGORY",
                summaryEl = if (id == 0L) "Νέα κατηγορία: ${finalSaved.name}" else "Ενημέρωση κατηγορίας: ${finalSaved.name}",
                summaryEn = if (id == 0L) "New category: ${finalSaved.name}" else "Updated category: ${finalSaved.name}",
                householdIdOverride = householdId
            )
            vm.showSnackbar(
                if (id == 0L) "Η κατηγορία «${finalSaved.name}» δημιουργήθηκε!"
                else "Η κατηγορία «${finalSaved.name}» ενημερώθηκε!"
            )
        }
    }

    fun softDeleteCategory(category: ExpenseCategory) {
        if (!vm.hasOwnerPermissions()) {
            vm.showSnackbar("Μόνο ο Δημιουργός ή Συντάκτης μπορεί να διαγράψει κατηγορίες")
            return
        }
        val householdId = vm._currentHousehold.value?.id.orEmpty()
        vm._allCategories.value = vm._allCategories.value.filter { it.id != category.id }
        val trash = vm._deletedCategories.value + category
        vm._deletedCategories.value = trash
        persistDeletedCategories(householdId, trash)

        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.repository.deleteCategory(category)
            if (householdId.isNotBlank()) {
                vm.firestoreSyncRepo.setCategoryDeleted(householdId, category.id.toString(), category.name, true)
            }
            vm.showSnackbar("Η κατηγορία «${category.name}» μεταφέρθηκε στον κάδο")
        }
    }

    fun restoreCategory(category: ExpenseCategory) {
        if (!vm.hasOwnerPermissions()) return
        val householdId = vm._currentHousehold.value?.id.orEmpty()
        val remainingTrash = vm._deletedCategories.value.filter { it.id != category.id }
        vm._deletedCategories.value = remainingTrash
        persistDeletedCategories(householdId, remainingTrash)

        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            val insertedId = vm.repository.insertCategory(category)
            val restored = category.copy(id = insertedId)
            vm._allCategories.value = (vm._allCategories.value + restored).distinctBy { it.id }.sortedWith(compareBy<ExpenseCategory> { it.sortOrder }.thenBy { it.id })
            if (householdId.isNotBlank()) {
                vm.firestoreSyncRepo.syncCategory(
                    householdId,
                    FirebaseCategory(restored.id.toString(), householdId, vm._currentUser.value?.uid ?: "", restored.name, restored.iconKey, restored.monthlyBudget, restored.colorHex, restored.voiceKeywords, restored.sortOrder, restored.fontColorHex)
                )
            }
            vm.showSnackbar("Η κατηγορία «${category.name}» επανήλθε!")
        }
    }

    fun permanentDeleteCategory(category: ExpenseCategory) {
        if (!vm.hasOwnerPermissions()) return
        val householdId = vm._currentHousehold.value?.id.orEmpty()
        val remainingTrash = vm._deletedCategories.value.filter { it.id != category.id }
        vm._deletedCategories.value = remainingTrash
        persistDeletedCategories(householdId, remainingTrash)
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            if (householdId.isNotBlank()) {
                vm.firestoreSyncRepo.permanentDeleteCategoryExact(householdId, category.id.toString(), category.name)
            }
        }
    }

    fun moveMemberUp(member: FamilyMember) {
        val list = vm._allMembers.value.toMutableList()
        val idx = list.indexOfFirst { it.id == member.id }
        if (idx > 0) {
            val prev = list[idx - 1]
            list[idx - 1] = member
            list[idx] = prev
            val reordered = list.mapIndexed { index, m -> m.copy(sortOrder = index) }
            vm._allMembers.value = reordered
            scheduleMemberReorderSave(reordered)
        }
    }

    fun moveMemberDown(member: FamilyMember) {
        val list = vm._allMembers.value.toMutableList()
        val idx = list.indexOfFirst { it.id == member.id }
        if (idx in 0 until list.lastIndex) {
            val next = list[idx + 1]
            list[idx + 1] = member
            list[idx] = next
            val reordered = list.mapIndexed { index, m -> m.copy(sortOrder = index) }
            vm._allMembers.value = reordered
            scheduleMemberReorderSave(reordered)
        }
    }

    private fun scheduleMemberReorderSave(reordered: List<FamilyMember>) {
        vm.memberReorderJob?.cancel()
        vm.memberReorderJob = vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            delay(400)
            vm.repository.updateMembers(reordered)
            val householdId = vm._currentHousehold.value?.id.orEmpty()
            val uid = vm._currentUser.value?.uid.orEmpty()
            val householdIds = vm._userHouseholds.value.map { it.id }.ifEmpty { listOf(householdId).filter { it.isNotBlank() } }
            val fbList = reordered.map { m ->
                FirebaseMember(m.id.toString(), householdId, uid, m.name, m.role, m.avatarColorHex, m.voiceKeywords, m.isDefault, m.sortOrder, false, m.linkedUserUid)
            }
            vm.firestoreSyncRepo.batchUpdateMemberSortOrder(householdIds, uid, fbList)
        }
    }

    fun moveCategoryUp(category: ExpenseCategory) {
        val list = vm._allCategories.value.toMutableList()
        val idx = list.indexOfFirst { it.id == category.id }
        if (idx > 0) {
            val prev = list[idx - 1]
            list[idx - 1] = category
            list[idx] = prev
            val reordered = list.mapIndexed { index, c -> c.copy(sortOrder = index) }
            vm._allCategories.value = reordered
            scheduleCategoryReorderSave(reordered)
        }
    }

    fun moveCategoryDown(category: ExpenseCategory) {
        val list = vm._allCategories.value.toMutableList()
        val idx = list.indexOfFirst { it.id == category.id }
        if (idx in 0 until list.lastIndex) {
            val next = list[idx + 1]
            list[idx + 1] = category
            list[idx] = next
            val reordered = list.mapIndexed { index, c -> c.copy(sortOrder = index) }
            vm._allCategories.value = reordered
            scheduleCategoryReorderSave(reordered)
        }
    }

    private fun scheduleCategoryReorderSave(reordered: List<ExpenseCategory>) {
        vm.categoryReorderJob?.cancel()
        vm.categoryReorderJob = vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            delay(400)
            vm.repository.updateCategories(reordered)
            val householdId = vm._currentHousehold.value?.id.orEmpty()
            val uid = vm._currentUser.value?.uid.orEmpty()
            val householdIds = vm._userHouseholds.value.map { it.id }.ifEmpty { listOf(householdId).filter { it.isNotBlank() } }
            val fbList = reordered.map { c ->
                FirebaseCategory(c.id.toString(), householdId, uid, c.name, c.iconKey, c.monthlyBudget, c.colorHex, c.voiceKeywords, c.sortOrder, c.fontColorHex)
            }
            vm.firestoreSyncRepo.batchUpdateCategorySortOrder(householdIds, uid, fbList)
        }
    }

    fun clearCurrentExodologioExpenses() {
        val household = vm._currentHousehold.value
        val uid = vm._currentUser.value?.uid ?: ""
        if (household != null && !vm.isUserCreatorOfHousehold(household)) {
            vm.showSnackbar(
                if (vm._appLanguage.value == "EN") "Only the creator can clear all expenses"
                else "Μόνο ο δημιουργός μπορεί να διαγράψει όλα τα έξοδα"
            )
            return
        }
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm.repository.deleteAllExpenses()
            vm._allExpenses.value = emptyList()
            if (household != null) {
                vm.firestoreSyncRepo.clearAllExpenses(household.id, uid)
            }
            vm.showSnackbar(
                if (vm._appLanguage.value == "EN") "All expenses cleared" else "Όλα τα έξοδα διαγράφηκαν"
            )
        }
    }

    fun clearCurrentHouseholdAllData() {
        val household = vm._currentHousehold.value
        val uid = vm._currentUser.value?.uid ?: vm.auth.currentUser?.uid ?: ""
        val isEn = vm._appLanguage.value == "EN"
        if (household == null || household.id.isBlank()) {
            vm.showSnackbar(
                if (isEn) "No active household selected" else "Δεν βρέθηκε ενεργό εξοδολόγιο"
            )
            return
        }
        if (!vm.isUserCreatorOfHousehold(household)) {
            vm.showSnackbar(
                if (isEn) "Only the household owner can clear all household data"
                else "Μόνο ο ιδιοκτήτης μπορεί να εκκαθαρίσει όλα τα δεδομένα του εξοδολογίου"
            )
            return
        }
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            vm._isSyncing.value = true
            try {
                // Cancel scheduled local bill reminders for this household
                val existingRecurring = try {
                    vm.repository.getAllRecurringExpensesList()
                } catch (_: Exception) {
                    emptyList()
                }
                existingRecurring.forEach { rec ->
                    try {
                        BillReminderManager.cancelReminder(vm.getApplication(), rec.id)
                    } catch (_: Exception) {}
                }

                // Soft-archive ALL household documents in Firebase Firestore (isDeleted = true + archiveBatchId)
                // so that zero records are lost and the owner can restore them from Firebase if needed.
                val archiveBatchId = vm.firestoreSyncRepo.archiveAllHouseholdData(household.id, uid)

                // Clear local Room cache & in-memory state for this household (without signing out)
                vm.pendingLocalExpenseIds.clear()
                vm.pendingLocalIncomeIds.clear()
                vm.savingsGoalCloudIdByLocalId.clear()
                vm.pendingLocalSavingsGoals.clear()
                vm.pendingDeletedSavingsGoals.clear()
                vm.inFlightCreateGoalKeys.clear()
                vm.lastHouseholdExpenses = emptyList()
                vm.lastHouseholdIncomes = emptyList()
                vm.lastHouseholdMembers = emptyList()
                vm.lastHouseholdCategories = emptyList()
                vm.lastHouseholdSavingsGoals = emptyList()
                vm._allExpenses.value = emptyList()
                vm._allIncomes.value = emptyList()
                vm._allMembers.value = emptyList()
                vm._allCategories.value = emptyList()
                vm._allSavingsGoals.value = emptyList()
                vm._pendingUndoDeletion.value = null
                vm.undoCountdownJob?.cancel()
                vm.repository.clearAllData()

                // Log activity for household audit trail
                vm.logHouseholdActivity(
                    actionType = "HOUSEHOLD_CLEARED",
                    targetType = "HOUSEHOLD",
                    summaryEl = "Εκκαθαρίστηκαν όλα τα δεδομένα του εξοδολογίου (αντίγραφο στο Firebase: ${archiveBatchId.take(8)})",
                    summaryEn = "Cleared all household data (archived in Firebase: ${archiveBatchId.take(8)})",
                    amount = 0.0
                )

                vm.showSnackbar(
                    if (isEn) "Household data cleared (safely archived in Firebase)"
                    else "Τα δεδομένα του εξοδολογίου εκκαθαρίστηκαν (διατηρούνται στο Firebase)"
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                vm.showSnackbar(
                    if (isEn) "Could not clear household data: ${e.message ?: "Error"}"
                    else "Αποτυχία εκκαθάρισης δεδομένων εξοδολογίου"
                )
            } finally {
                vm._isSyncing.value = false
            }
        }
    }

    fun activateProFromBilling() {
        val wasPro = vm._isProUser.value
        val hadPaywallOpen = vm._premiumPaywallReason.value != null
        vm._isProUser.value = true
        vm.syncBankCatcherProEntitlement()
        vm._premiumPaywallReason.value = null
        if (!wasPro && hadPaywallOpen) {
            vm.showSnackbar(
                if (vm._appLanguage.value == "EN") "🎉 Welcome to Finsay Pro!"
                else "🎉 Καλώς ήρθατε στο Finsay Pro!"
            )
        }
    }

    fun unlockPro() {
        activateProFromBilling()
    }

    fun revokePro() {
        vm._isProUser.value = false
        vm.syncBankCatcherProEntitlement()
    }

    fun onPurchasePending() {
        val isEn = vm._appLanguage.value == "EN"
        vm.showSnackbar(
            if (isEn) "Your payment is pending confirmation from Google Play. Pro features will unlock automatically once approved."
            else "Η πληρωμή σας εκκρεμεί επιβεβαίωσης από το Google Play. Οι λειτουργίες Pro θα ξεκλειδωθούν αυτόματα μόλις εγκριθεί."
        )
    }

    fun setThemeMode(mode: String, syncToCloud: Boolean = true) {
        vm._themeMode.value = mode
        vm.prefs.edit().putString("theme_mode", mode).apply()
        val uid = vm._currentUser.value?.uid
        if (syncToCloud && uid != null) {
            vm._userProfile.value = vm._userProfile.value?.copy(themeMode = mode)
            vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
                vm.firestoreSyncRepo.updateUserProfilePreferences(uid, themeMode = mode)
            }
        }
    }

    fun setAppLanguage(lang: String, syncToCloud: Boolean = true) {
        vm._appLanguage.value = lang
        vm.prefs.edit()
            .putString("app_language", lang)
            .putBoolean("user_explicitly_set_language_v2", true)
            .apply()
        val uid = vm._currentUser.value?.uid
        if (syncToCloud && uid != null) {
            vm._userProfile.value = vm._userProfile.value?.copy(appLanguage = lang)
            vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
                vm.firestoreSyncRepo.updateUserProfilePreferences(uid, appLanguage = lang)
            }
        }
    }

    fun setMemberFilter(memberId: Long?) {
        vm._activeSpecialFilter.value = null
        vm._selectedMemberFilter.value = memberId
    }

    fun setCategoryFilter(categoryId: Long?) {
        vm._activeSpecialFilter.value = null
        vm._selectedCategoryFilter.value = categoryId
    }

    fun clearSpecialDrillDownFilter() {
        vm._activeSpecialFilter.value = null
        vm._selectedMemberFilter.value = null
        vm._selectedCategoryFilter.value = null
        vm._searchQuery.value = ""
    }

    fun applyHomeDrillDown(request: HomeDrillDownRequest) {
        vm._homeSelectedMonthKey.value = request.periodKey
        vm._homeSelectedTypeFilter.value = request.typeFilter
        vm._selectedMemberFilter.value = request.memberId
        vm._selectedCategoryFilter.value = request.categoryId
        vm._searchQuery.value = request.searchQuery
        vm._activeSpecialFilter.value = request.specialFilter ?: AnalyticsSpecialFilter()
            .takeIf { request.memberId != null || request.categoryId != null }
        vm._homeDrillDownScrollTrigger.value = vm._homeDrillDownScrollTrigger.value + 1
    }

    fun toggleDailyReminder(enabled: Boolean) {
        DailyReminderManager.setReminderEnabled(vm.getApplication(), enabled)
        vm._dailyReminderEnabled.value = enabled
        val uid = vm._currentUser.value?.uid
        if (uid != null) {
            vm._userProfile.value = vm._userProfile.value?.copy(reminderEnabled = enabled)
            vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
                vm.firestoreSyncRepo.updateUserProfilePreferences(uid, reminderEnabled = enabled)
            }
        }
    }

    fun setDailyReminderTime(hour: Int, minute: Int) {
        DailyReminderManager.setReminderTime(vm.getApplication(), hour, minute)
        vm._dailyReminderHour.value = hour
        vm._dailyReminderMinute.value = minute
        val uid = vm._currentUser.value?.uid
        if (uid != null) {
            vm._userProfile.value = vm._userProfile.value?.copy(reminderHour = hour, reminderMinute = minute)
            vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
                vm.firestoreSyncRepo.updateUserProfilePreferences(uid, reminderHour = hour, reminderMinute = minute)
            }
        }
    }

    fun toggleBankAutoCatcher(enabled: Boolean) {
        if (enabled && !vm.isCurrentUserPro()) {
            BankNotificationSystemManager.setProEntitled(vm.getApplication(), false)
            BankNotificationSystemManager.setAutoCatcherEnabled(vm.getApplication(), false)
            vm._bankAutoCatcherEnabled.value = false
            vm.showPremiumPaywall("bank_auto_catcher")
            return
        }
        BankNotificationSystemManager.setProEntitled(vm.getApplication(), vm.isCurrentUserPro())
        BankNotificationSystemManager.setAutoCatcherEnabled(vm.getApplication(), enabled)
        vm._bankAutoCatcherEnabled.value = enabled && vm.isCurrentUserPro()
        val isEn = vm._appLanguage.value == "EN"
        vm.showSnackbar(
            if (enabled) {
                if (isEn) "Automatic bank notifications enabled" else "Ενεργοποιήθηκαν οι αυτόματες τραπεζικές ειδοποιήσεις"
            } else {
                if (isEn) "Automatic bank notifications paused" else "Απενεργοποιήθηκαν οι αυτόματες τραπεζικές ειδοποιήσεις"
            }
        )
    }
}
