package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Robust, fail-safe Firestore mappers that safely extract fields from DocumentSnapshot
 * or raw field maps. These protect against field type mismatches (Double vs Long/Int/String,
 * Boolean vs Long/String, Timestamp vs Number/Date), conflicting annotations, and corrupted
 * cached entries so that queries and cache replays NEVER throw unhandled RuntimeExceptions.
 */

internal fun coerceSafeString(raw: Any?): String? = try {
    when (raw) {
        is String -> raw
        is Number, is Boolean -> raw.toString()
        else -> null
    }
} catch (_: Throwable) {
    null
}

internal fun coerceSafeDouble(raw: Any?): Double? = try {
    when (raw) {
        is Number -> raw.toDouble().takeIf { !it.isNaN() && !it.isInfinite() }
        is String -> raw.replace(',', '.').trim().toDoubleOrNull()?.takeIf { !it.isNaN() && !it.isInfinite() }
        else -> null
    }
} catch (_: Throwable) {
    null
}

internal fun coerceSafeLong(raw: Any?): Long? = try {
    when (raw) {
        is Number -> raw.toLong()
        is String -> raw.trim().toLongOrNull()
        is Timestamp -> raw.toDate().time
        is Date -> raw.time
        else -> null
    }
} catch (_: Throwable) {
    null
}

internal fun coerceSafeInt(raw: Any?): Int? = try {
    when (raw) {
        is Number -> raw.toInt()
        is String -> raw.trim().toIntOrNull()
        else -> null
    }
} catch (_: Throwable) {
    null
}

internal fun coerceSafeBoolean(raw: Any?): Boolean? = try {
    when (raw) {
        is Boolean -> raw
        is Number -> raw.toInt() != 0
        is String -> when (raw.trim().lowercase()) {
            "true", "1", "yes" -> true
            "false", "0", "no" -> false
            else -> null
        }
        else -> null
    }
} catch (_: Throwable) {
    null
}

internal fun coerceSafeTimestamp(raw: Any?): Timestamp? = try {
    when (raw) {
        is Timestamp -> raw
        is Date -> Timestamp(raw)
        is Number -> Timestamp(Date(raw.toLong()))
        else -> null
    }
} catch (_: Throwable) {
    null
}

internal fun coerceSafeTimestampMillis(raw: Any?): Long? = try {
    when (raw) {
        is Number -> raw.toLong()
        is String -> raw.trim().toLongOrNull()
        is Timestamp -> raw.toDate().time
        is Date -> raw.time
        else -> null
    }
} catch (_: Throwable) {
    null
}

internal fun coerceSafeStringList(raw: Any?): List<String>? = try {
    when (raw) {
        is Iterable<*> -> raw.mapNotNull { coerceSafeString(it) }
        else -> null
    }
} catch (_: Throwable) {
    null
}

internal fun coerceSafeStringMap(raw: Any?): Map<String, String>? = try {
    when (raw) {
        is Map<*, *> -> raw.entries.mapNotNull { (k, v) ->
            val key = k?.toString()?.takeIf { it.isNotBlank() }
            val value = coerceSafeString(v)
            if (key != null && value != null) key to value else null
        }.toMap()
        else -> null
    }
} catch (_: Throwable) {
    null
}

fun DocumentSnapshot.safeGet(field: String): Any? = try {
    get(field)
} catch (_: Throwable) {
    null
}

fun DocumentSnapshot.safeString(field: String): String? = coerceSafeString(safeGet(field))

fun DocumentSnapshot.safeLong(field: String): Long? = coerceSafeLong(safeGet(field))

fun DocumentSnapshot.safeBoolean(field: String): Boolean? = coerceSafeBoolean(safeGet(field))

fun DocumentSnapshot.safeStringList(field: String): List<String>? = coerceSafeStringList(safeGet(field))

private fun DocumentSnapshot.safeDocId(): String = try {
    id
} catch (_: Throwable) {
    ""
}

fun mapExpenseFromFields(
    docId: String,
    existing: FirebaseExpense? = null,
    getField: (String) -> Any?
): FirebaseExpense = try {
    val rawAmount = getField("amount")
    val amt = coerceSafeDouble(rawAmount) ?: existing?.amount ?: 0.0
    val catId = coerceSafeLong(getField("categoryId")) ?: existing?.categoryId ?: 1L
    val createdAtTs = coerceSafeTimestamp(getField("createdAt")) ?: existing?.createdAt
    val ts = coerceSafeTimestampMillis(getField("timestamp"))
        ?: try { createdAtTs?.toDate()?.time } catch (_: Throwable) { null }
        ?: existing?.timestamp
        ?: 0L
    val payerId = coerceSafeLong(getField("payerMemberId")) ?: existing?.payerMemberId ?: 0L
    val resolvedId = coerceSafeString(getField("id"))?.ifBlank { docId } ?: docId

    (existing ?: FirebaseExpense()).copy(
        id = resolvedId,
        householdId = coerceSafeString(getField("householdId")) ?: existing?.householdId ?: "",
        title = coerceSafeString(getField("title")) ?: existing?.title ?: "",
        amount = amt,
        categoryId = catId,
        categoryName = coerceSafeString(getField("categoryName")) ?: existing?.categoryName ?: "",
        timestamp = ts,
        splitType = coerceSafeString(getField("splitType")) ?: existing?.splitType ?: "ALL",
        memberIdsCsv = coerceSafeString(getField("memberIdsCsv")) ?: existing?.memberIdsCsv ?: "",
        memberNamesDisplay = coerceSafeString(getField("memberNamesDisplay")) ?: existing?.memberNamesDisplay ?: "",
        note = coerceSafeString(getField("note")) ?: existing?.note ?: "",
        payerMemberId = payerId,
        payerMemberName = coerceSafeString(getField("payerMemberName")) ?: existing?.payerMemberName ?: "",
        payerMemberIdsCsv = coerceSafeString(getField("payerMemberIdsCsv")) ?: existing?.payerMemberIdsCsv ?: "",
        payerAmountsCsv = coerceSafeString(getField("payerAmountsCsv")) ?: existing?.payerAmountsCsv ?: "",
        createdByUid = coerceSafeString(getField("createdByUid")) ?: existing?.createdByUid ?: "",
        createdByName = coerceSafeString(getField("createdByName")) ?: existing?.createdByName ?: "",
        isDeleted = coerceSafeBoolean(getField("isDeleted")) ?: existing?.isDeleted ?: false,
        createdAt = createdAtTs
    )
} catch (_: Throwable) {
    existing ?: FirebaseExpense(id = docId)
}

fun DocumentSnapshot.toSafeExpense(): FirebaseExpense {
    val docId = safeDocId()
    val existing = try {
        this.toObject(FirebaseExpense::class.java)
    } catch (_: Throwable) {
        null
    }
    return mapExpenseFromFields(docId, existing) { field -> safeGet(field) }
}

fun mapMemberFromFields(
    docId: String,
    existing: FirebaseMember? = null,
    getField: (String) -> Any?
): FirebaseMember = try {
    val rawDefault = getField("default") ?: getField("isDefault")
    val defVal = coerceSafeBoolean(rawDefault) ?: existing?.isDefault ?: false
    val color = coerceSafeLong(getField("avatarColorHex")) ?: existing?.avatarColorHex ?: 0xFF059669
    val sort = coerceSafeInt(getField("sortOrder")) ?: existing?.sortOrder ?: 0
    val resolvedId = coerceSafeString(getField("id"))?.ifBlank { docId } ?: docId

    (existing ?: FirebaseMember()).copy(
        id = resolvedId,
        householdId = coerceSafeString(getField("householdId")) ?: existing?.householdId ?: "",
        userId = coerceSafeString(getField("userId")) ?: existing?.userId ?: "",
        name = coerceSafeString(getField("name")) ?: existing?.name ?: "",
        role = coerceSafeString(getField("role")) ?: existing?.role ?: "Γονέας",
        avatarColorHex = color,
        voiceKeywords = coerceSafeString(getField("voiceKeywords")) ?: existing?.voiceKeywords ?: "",
        isDefault = defVal,
        sortOrder = sort,
        isDeleted = coerceSafeBoolean(getField("isDeleted")) ?: existing?.isDeleted ?: false,
        linkedUserUid = coerceSafeString(getField("linkedUserUid")) ?: existing?.linkedUserUid ?: "",
        createdAt = coerceSafeTimestamp(getField("createdAt")) ?: existing?.createdAt
    )
} catch (_: Throwable) {
    existing ?: FirebaseMember(id = docId)
}

fun DocumentSnapshot.toSafeMember(): FirebaseMember {
    val docId = safeDocId()
    val existing = try {
        this.toObject(FirebaseMember::class.java)
    } catch (_: Throwable) {
        null
    }
    return mapMemberFromFields(docId, existing) { field -> safeGet(field) }
}

fun mapIncomeFromFields(
    docId: String,
    existing: FirebaseIncome? = null,
    getField: (String) -> Any?
): FirebaseIncome = try {
    val amt = coerceSafeDouble(getField("amount")) ?: existing?.amount ?: 0.0
    val mId = coerceSafeLong(getField("memberId")) ?: existing?.memberId ?: 0L
    val createdAtTs = coerceSafeTimestamp(getField("createdAt")) ?: existing?.createdAt
    val ts = coerceSafeTimestampMillis(getField("timestamp"))
        ?: try { createdAtTs?.toDate()?.time } catch (_: Throwable) { null }
        ?: existing?.timestamp
        ?: 0L
    val resolvedId = coerceSafeString(getField("id"))?.ifBlank { docId } ?: docId

    (existing ?: FirebaseIncome()).copy(
        id = resolvedId,
        householdId = coerceSafeString(getField("householdId")) ?: existing?.householdId ?: "",
        title = coerceSafeString(getField("title")) ?: existing?.title ?: "",
        amount = amt,
        memberId = mId,
        memberName = coerceSafeString(getField("memberName")) ?: existing?.memberName ?: "",
        category = coerceSafeString(getField("category")) ?: existing?.category ?: "Μισθός",
        timestamp = ts,
        note = coerceSafeString(getField("note")) ?: existing?.note ?: "",
        createdByUid = coerceSafeString(getField("createdByUid")) ?: existing?.createdByUid ?: "",
        createdByName = coerceSafeString(getField("createdByName")) ?: existing?.createdByName ?: "",
        isDeleted = coerceSafeBoolean(getField("isDeleted")) ?: existing?.isDeleted ?: false,
        createdAt = createdAtTs
    )
} catch (_: Throwable) {
    existing ?: FirebaseIncome(id = docId)
}

fun DocumentSnapshot.toSafeIncome(): FirebaseIncome {
    val docId = safeDocId()
    val existing = try {
        this.toObject(FirebaseIncome::class.java)
    } catch (_: Throwable) {
        null
    }
    return mapIncomeFromFields(docId, existing) { field -> safeGet(field) }
}

fun mapCategoryFromFields(
    docId: String,
    existing: FirebaseCategory? = null,
    getField: (String) -> Any?
): FirebaseCategory = try {
    val budget = coerceSafeDouble(getField("monthlyBudget")) ?: existing?.monthlyBudget ?: 0.0
    val color = coerceSafeLong(getField("colorHex")) ?: existing?.colorHex ?: 0xFF059669
    val fontColor = coerceSafeLong(getField("fontColorHex")) ?: existing?.fontColorHex ?: 0L
    val sort = coerceSafeInt(getField("sortOrder")) ?: existing?.sortOrder ?: 0
    val resolvedId = coerceSafeString(getField("id"))?.ifBlank { docId } ?: docId

    (existing ?: FirebaseCategory()).copy(
        id = resolvedId,
        householdId = coerceSafeString(getField("householdId")) ?: existing?.householdId ?: "",
        userId = coerceSafeString(getField("userId")) ?: existing?.userId ?: "",
        name = coerceSafeString(getField("name")) ?: existing?.name ?: "",
        iconKey = coerceSafeString(getField("iconKey")) ?: existing?.iconKey ?: "cart",
        monthlyBudget = budget,
        colorHex = color,
        voiceKeywords = coerceSafeString(getField("voiceKeywords")) ?: existing?.voiceKeywords ?: "",
        sortOrder = sort,
        fontColorHex = fontColor,
        isDeleted = coerceSafeBoolean(getField("isDeleted")) ?: existing?.isDeleted ?: false,
        categoryType = coerceSafeString(getField("categoryType"))?.takeIf { it.isNotBlank() } ?: existing?.categoryType ?: "EXPENSE",
        createdAt = coerceSafeTimestamp(getField("createdAt")) ?: existing?.createdAt
    )
} catch (_: Throwable) {
    existing ?: FirebaseCategory(id = docId)
}

fun DocumentSnapshot.toSafeCategory(): FirebaseCategory {
    val docId = safeDocId()
    val existing = try {
        this.toObject(FirebaseCategory::class.java)
    } catch (_: Throwable) {
        null
    }
    return mapCategoryFromFields(docId, existing) { field -> safeGet(field) }
}

fun mapHouseholdFromFields(
    docId: String,
    existing: Household? = null,
    getField: (String) -> Any?
): Household = try {
    val members = coerceSafeStringList(getField("memberUids")) ?: existing?.memberUids ?: emptyList()
    val invited = coerceSafeStringList(getField("invitedEmails")) ?: existing?.invitedEmails ?: emptyList()
    val rawPrem = getField("isPremium") ?: getField("premium")
    val isPrem = coerceSafeBoolean(rawPrem) ?: existing?.isPremium ?: false
    val resolvedId = coerceSafeString(getField("id"))?.ifBlank { docId } ?: docId

    val rolesMap = coerceSafeStringMap(getField("memberRoles")) ?: existing?.memberRoles ?: emptyMap()
    val emailsMap = coerceSafeStringMap(getField("memberEmails")) ?: existing?.memberEmails ?: emptyMap()
    val namesMap = coerceSafeStringMap(getField("memberNames")) ?: existing?.memberNames ?: emptyMap()
    val defaultPayersMap = coerceSafeStringMap(getField("memberDefaultPayers")) ?: existing?.memberDefaultPayers ?: emptyMap()

    (existing ?: Household()).copy(
        id = resolvedId,
        name = coerceSafeString(getField("name"))?.ifBlank { null } ?: existing?.name ?: "My Household Book",
        ownerId = coerceSafeString(getField("ownerId")) ?: existing?.ownerId ?: "",
        inviteCode = coerceSafeString(getField("inviteCode")) ?: existing?.inviteCode ?: "",
        memberUids = members,
        invitedEmails = invited,
        memberRoles = rolesMap,
        memberEmails = emailsMap,
        memberNames = namesMap,
        memberDefaultPayers = defaultPayersMap,
        pendingInviteRole = coerceSafeString(getField("pendingInviteRole")) ?: existing?.pendingInviteRole ?: "REGISTAR",
        currencyCode = coerceSafeString(getField("currencyCode"))?.ifBlank { null } ?: existing?.currencyCode?.ifBlank { null } ?: "EUR",
        isPremium = isPrem,
        createdAt = coerceSafeTimestamp(getField("createdAt")) ?: existing?.createdAt,
        updatedAt = coerceSafeTimestamp(getField("updatedAt")) ?: existing?.updatedAt
    )
} catch (_: Throwable) {
    existing ?: Household(id = docId)
}

fun DocumentSnapshot.toSafeHousehold(): Household {
    val docId = safeDocId()
    val existing = try {
        this.toObject(Household::class.java)
    } catch (_: Throwable) {
        null
    }
    return mapHouseholdFromFields(docId, existing) { field -> safeGet(field) }
}

fun mapUserProfileFromFields(
    docId: String,
    existing: UserProfile? = null,
    getField: (String) -> Any?
): UserProfile = try {
    val hids = coerceSafeStringList(getField("householdIds")) ?: existing?.householdIds ?: emptyList()
    val rawPrem = getField("isPremium") ?: getField("premium")
    val isPrem = coerceSafeBoolean(rawPrem) ?: existing?.isPremium ?: false
    val uidVal = coerceSafeString(getField("uid"))?.ifBlank { docId } ?: docId
    val remHour = (coerceSafeInt(getField("reminderHour")) ?: existing?.reminderHour ?: 21).coerceIn(0, 23)
    val remMinute = (coerceSafeInt(getField("reminderMinute")) ?: existing?.reminderMinute ?: 30).coerceIn(0, 59)

    (existing ?: UserProfile()).copy(
        uid = uidVal,
        email = coerceSafeString(getField("email")) ?: existing?.email ?: "",
        displayName = coerceSafeString(getField("displayName")) ?: existing?.displayName ?: "",
        firstName = coerceSafeString(getField("firstName")) ?: existing?.firstName ?: "",
        lastName = coerceSafeString(getField("lastName")) ?: existing?.lastName ?: "",
        birthDate = coerceSafeString(getField("birthDate")) ?: existing?.birthDate ?: "",
        phoneNumber = coerceSafeString(getField("phoneNumber")) ?: existing?.phoneNumber ?: "",
        occupation = coerceSafeString(getField("occupation")) ?: existing?.occupation ?: "",
        city = coerceSafeString(getField("city")) ?: existing?.city ?: "",
        householdId = coerceSafeString(getField("householdId")) ?: existing?.householdId ?: "",
        householdIds = hids,
        isPremium = isPrem,
        photoUrl = coerceSafeString(getField("photoUrl")) ?: existing?.photoUrl ?: "",
        authProvider = coerceSafeString(getField("authProvider")) ?: existing?.authProvider ?: "password",
        appLanguage = coerceSafeString(getField("appLanguage")) ?: existing?.appLanguage ?: "EN",
        themeMode = coerceSafeString(getField("themeMode")) ?: existing?.themeMode ?: "SYSTEM",
        reminderEnabled = coerceSafeBoolean(getField("reminderEnabled")) ?: existing?.reminderEnabled ?: false,
        reminderHour = remHour,
        reminderMinute = remMinute,
        notificationConsentCompleted = coerceSafeBoolean(getField("notificationConsentCompleted"))
            ?: existing?.notificationConsentCompleted ?: false,
        requiresEmailVerification = coerceSafeBoolean(getField("requiresEmailVerification"))
            ?: existing?.requiresEmailVerification ?: false,
        emailVerifiedConfirmed = coerceSafeBoolean(getField("emailVerifiedConfirmed"))
            ?: existing?.emailVerifiedConfirmed ?: true,
        createdAt = coerceSafeTimestamp(getField("createdAt")) ?: existing?.createdAt,
        updatedAt = coerceSafeTimestamp(getField("updatedAt")) ?: existing?.updatedAt
    )
} catch (_: Throwable) {
    existing ?: UserProfile(uid = docId)
}

fun DocumentSnapshot.toSafeUserProfile(): UserProfile {
    val docId = safeDocId()
    val existing = try {
        this.toObject(UserProfile::class.java)
    } catch (_: Throwable) {
        null
    }
    return mapUserProfileFromFields(docId, existing) { field -> safeGet(field) }
}

fun mapSavingsGoalFromFields(
    docId: String,
    existing: FirebaseSavingsGoal? = null,
    getField: (String) -> Any?
): FirebaseSavingsGoal = try {
    val target = coerceSafeDouble(getField("targetAmount")) ?: existing?.targetAmount ?: 0.0
    val current = coerceSafeDouble(getField("currentAmount")) ?: existing?.currentAmount ?: 0.0
    val color = coerceSafeLong(getField("colorHex")) ?: existing?.colorHex ?: 0xFF059669
    val resolvedId = coerceSafeString(getField("id"))?.ifBlank { docId } ?: docId

    (existing ?: FirebaseSavingsGoal()).copy(
        id = resolvedId,
        householdId = coerceSafeString(getField("householdId")) ?: existing?.householdId ?: "",
        userId = coerceSafeString(getField("userId")) ?: existing?.userId ?: "",
        title = coerceSafeString(getField("title")) ?: existing?.title ?: "",
        targetAmount = target,
        currentAmount = current,
        deadlineDate = coerceSafeString(getField("deadlineDate")) ?: existing?.deadlineDate ?: "",
        iconKey = coerceSafeString(getField("iconKey")) ?: existing?.iconKey ?: "vacation",
        colorHex = color,
        note = coerceSafeString(getField("note")) ?: existing?.note ?: "",
        isDeleted = coerceSafeBoolean(getField("isDeleted")) ?: existing?.isDeleted ?: false,
        createdAt = coerceSafeTimestamp(getField("createdAt")) ?: existing?.createdAt
    )
} catch (_: Throwable) {
    existing ?: FirebaseSavingsGoal(id = docId)
}

fun DocumentSnapshot.toSafeSavingsGoal(): FirebaseSavingsGoal {
    val docId = safeDocId()
    val existing = try {
        this.toObject(FirebaseSavingsGoal::class.java)
    } catch (_: Throwable) {
        null
    }
    return mapSavingsGoalFromFields(docId, existing) { field -> safeGet(field) }
}

fun mapRecurringExpenseFromFields(
    docId: String,
    existing: FirebaseRecurringExpense? = null,
    getField: (String) -> Any?
): FirebaseRecurringExpense = try {
    val amt = coerceSafeDouble(getField("amount")) ?: existing?.amount ?: 0.0
    val catId = coerceSafeLong(getField("categoryId")) ?: existing?.categoryId ?: 0L
    val day = coerceSafeInt(getField("dayOfMonth")) ?: existing?.dayOfMonth ?: 1
    val rHour = (coerceSafeInt(getField("reminderHour")) ?: existing?.reminderHour ?: 9).coerceIn(0, 23)
    val rMinute = (coerceSafeInt(getField("reminderMinute")) ?: existing?.reminderMinute ?: 0).coerceIn(0, 59)
    val rawAct = getField("isActive") ?: getField("active")
    val isAct = coerceSafeBoolean(rawAct) ?: existing?.isActive ?: true
    val payerId = coerceSafeLong(getField("payerMemberId")) ?: existing?.payerMemberId ?: 0L
    val resolvedId = coerceSafeString(getField("id"))?.ifBlank { docId } ?: docId

    (existing ?: FirebaseRecurringExpense()).copy(
        id = resolvedId,
        householdId = coerceSafeString(getField("householdId")) ?: existing?.householdId ?: "",
        userId = coerceSafeString(getField("userId")) ?: existing?.userId ?: "",
        title = coerceSafeString(getField("title")) ?: existing?.title ?: "",
        amount = amt,
        categoryId = catId,
        categoryName = coerceSafeString(getField("categoryName")) ?: existing?.categoryName ?: "",
        frequency = coerceSafeString(getField("frequency")) ?: existing?.frequency ?: "MONTHLY",
        dayOfMonth = day,
        reminderHour = rHour,
        reminderMinute = rMinute,
        splitType = coerceSafeString(getField("splitType")) ?: existing?.splitType ?: "ALL_FAMILY",
        memberIdsCsv = coerceSafeString(getField("memberIdsCsv")) ?: existing?.memberIdsCsv ?: "",
        memberNamesDisplay = coerceSafeString(getField("memberNamesDisplay")) ?: existing?.memberNamesDisplay ?: "Κοινό Οικογένειας",
        note = coerceSafeString(getField("note")) ?: existing?.note ?: "",
        isActive = isAct,
        lastGeneratedMonth = coerceSafeString(getField("lastGeneratedMonth")) ?: existing?.lastGeneratedMonth ?: "",
        payerMemberId = payerId,
        payerMemberName = coerceSafeString(getField("payerMemberName")) ?: existing?.payerMemberName ?: "",
        payerMemberIdsCsv = coerceSafeString(getField("payerMemberIdsCsv")) ?: existing?.payerMemberIdsCsv ?: "",
        payerAmountsCsv = coerceSafeString(getField("payerAmountsCsv")) ?: existing?.payerAmountsCsv ?: "",
        anchorMonth = (coerceSafeInt(getField("anchorMonth")) ?: existing?.anchorMonth ?: 0).coerceIn(0, 11),
        isDeleted = coerceSafeBoolean(getField("isDeleted")) ?: existing?.isDeleted ?: false,
        createdAt = coerceSafeTimestamp(getField("createdAt")) ?: existing?.createdAt
    )
} catch (_: Throwable) {
    existing ?: FirebaseRecurringExpense(id = docId)
}

fun DocumentSnapshot.toSafeRecurringExpense(): FirebaseRecurringExpense {
    val docId = safeDocId()
    val existing = try {
        this.toObject(FirebaseRecurringExpense::class.java)
    } catch (_: Throwable) {
        null
    }
    return mapRecurringExpenseFromFields(docId, existing) { field -> safeGet(field) }
}

fun mapActivityFromFields(
    docId: String,
    existing: FirebaseHouseholdActivity? = null,
    getField: (String) -> Any?
): FirebaseHouseholdActivity = try {
    val resolvedId = coerceSafeString(getField("id"))?.ifBlank { docId } ?: docId
    val amt = coerceSafeDouble(getField("amount")) ?: existing?.amount ?: 0.0
    val created = coerceSafeTimestampMillis(getField("createdAt")) ?: existing?.createdAt ?: 0L

    (existing ?: FirebaseHouseholdActivity()).copy(
        id = resolvedId,
        householdId = coerceSafeString(getField("householdId")) ?: existing?.householdId ?: "",
        actorUid = coerceSafeString(getField("actorUid")) ?: existing?.actorUid ?: "",
        actorName = coerceSafeString(getField("actorName")) ?: existing?.actorName ?: "",
        actionType = coerceSafeString(getField("actionType")) ?: existing?.actionType ?: "",
        targetType = coerceSafeString(getField("targetType")) ?: existing?.targetType ?: "",
        summaryEl = coerceSafeString(getField("summaryEl")) ?: existing?.summaryEl ?: "",
        summaryEn = coerceSafeString(getField("summaryEn")) ?: existing?.summaryEn ?: "",
        amount = amt,
        createdAt = created
    )
} catch (_: Throwable) {
    existing ?: FirebaseHouseholdActivity(id = docId)
}

fun DocumentSnapshot.toSafeHouseholdActivity(): FirebaseHouseholdActivity {
    val docId = safeDocId()
    val existing = try {
        this.toObject(FirebaseHouseholdActivity::class.java)
    } catch (_: Throwable) {
        null
    }
    return mapActivityFromFields(docId, existing) { field -> safeGet(field) }
}
