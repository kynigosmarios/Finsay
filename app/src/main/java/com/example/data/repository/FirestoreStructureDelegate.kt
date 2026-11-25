package com.example.data.repository

import android.util.Log
import com.example.data.entity.toFirebaseExpense
import com.example.data.model.FirebaseCategory
import com.example.data.model.FirebaseIncome
import com.example.data.model.FirebaseMember
import com.example.data.model.FirebaseRecurringExpense
import com.example.data.model.FirebaseSavingsGoal
import com.example.data.model.Household
import com.example.data.model.toSafeCategory
import com.example.data.model.toSafeMember
import com.example.data.model.toSafeRecurringExpense
import com.example.data.model.toSafeSavingsGoal
import com.example.util.GreekVoiceParser
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

internal class FirestoreStructureDelegate(
    private val db: FirebaseFirestore,
    private val householdDelegate: FirestoreHouseholdDelegate
) {
    companion object {
        private const val TAG = "FirestoreSyncRepo"
    }

    // Household & User Members
    fun observeMembers(householdId: String): Flow<List<FirebaseMember>> {
        if (householdId.isBlank()) return flowOf(emptyList())
        return db.collection("households").document(householdId)
            .collection("members")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    try { doc.toSafeMember() } catch (t: Throwable) {
                        Log.w(TAG, "Error deserializing member ${doc.id}: ${t.message}")
                        null
                    }
                }.sortedWith(compareBy<FirebaseMember> { it.sortOrder }.thenBy { it.id })
            }
            .catch { t ->
                Log.w(TAG, "observeMembers flow error: ${t.message}")
                emit(emptyList())
            }
    }

    suspend fun syncMember(householdId: String, member: FirebaseMember) {
        if (householdId.isBlank()) return
        if (member.userId.isNotBlank()) {
            try { householdDelegate.ensureHouseholdExists(householdId, member.userId) } catch (_: Exception) {}
        }
        val docId = if (member.id.isNotBlank()) member.id else UUID.randomUUID().toString()
        val toSave = member.copy(id = docId, householdId = householdId, createdAt = Timestamp.now())
        db.collection("households").document(householdId)
            .collection("members").document(docId)
            .set(toSave)
            .await()
    }

    suspend fun deleteMember(householdId: String, memberId: String) {
        if (householdId.isBlank() || memberId.isBlank()) return
        db.collection("households").document(householdId)
            .collection("members").document(memberId).delete().await()
    }

    suspend fun setMemberDeleted(householdId: String, memberId: String, memberName: String, isDeleted: Boolean) {
        if (householdId.isBlank()) return
        try {
            val membersCol = db.collection("households").document(householdId).collection("members")
            val targetNameNorm = GreekVoiceParser.normalizeGreek(memberName.trim().lowercase())
            val snap = membersCol.get().await()
            var matched = false
            for (doc in snap.documents) {
                val docName = GreekVoiceParser.normalizeGreek(doc.getString("name").orEmpty().trim().lowercase())
                val docId = doc.getString("id") ?: doc.id
                if (docId == memberId || doc.id == memberId || (targetNameNorm.isNotBlank() && docName == targetNameNorm)) {
                    doc.reference.update("isDeleted", isDeleted).await()
                    matched = true
                }
            }
            if (!matched && memberId.isNotBlank()) {
                membersCol.document(memberId).set(mapOf("isDeleted" to isDeleted), SetOptions.merge()).await()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Error setting member isDeleted in household $householdId: ${e.message}")
        }
    }

    suspend fun permanentDeleteMemberExact(householdId: String, memberId: String, memberName: String) {
        if (householdId.isBlank()) return
        try {
            val membersCol = db.collection("households").document(householdId).collection("members")
            val targetNameNorm = GreekVoiceParser.normalizeGreek(memberName.trim().lowercase())
            val snap = membersCol.get().await()
            for (doc in snap.documents) {
                val docName = GreekVoiceParser.normalizeGreek(doc.getString("name").orEmpty().trim().lowercase())
                val docId = doc.getString("id") ?: doc.id
                if (docId == memberId || doc.id == memberId || (targetNameNorm.isNotBlank() && docName == targetNameNorm)) {
                    doc.reference.delete().await()
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Error permanently deleting member from household $householdId: ${e.message}")
        }
    }

    suspend fun getMembersOnce(householdId: String): List<FirebaseMember> {
        if (householdId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("households").document(householdId).collection("members").get().await()
            snapshot.documents.mapNotNull { doc ->
                try { doc.toSafeMember() } catch (_: Throwable) { null }
            }.sortedWith(compareBy<FirebaseMember> { it.sortOrder }.thenBy { it.id })
        } catch (_: Throwable) {
            emptyList()
        }
    }

    suspend fun getUserMembersOnce(userId: String): List<FirebaseMember> {
        if (userId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("users").document(userId).collection("members").get().await()
            snapshot.documents.mapNotNull { doc ->
                try { doc.toSafeMember() } catch (_: Throwable) { null }
            }.sortedWith(compareBy<FirebaseMember> { it.sortOrder }.thenBy { it.id })
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun observeUserMembers(userId: String): Flow<List<FirebaseMember>> {
        if (userId.isBlank()) return flowOf(emptyList())
        return db.collection("users").document(userId)
            .collection("members")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    try { doc.toSafeMember() } catch (_: Throwable) { null }
                }.sortedWith(compareBy<FirebaseMember> { it.sortOrder }.thenBy { it.id })
            }
            .catch { t ->
                Log.w(TAG, "observeUserMembers flow error: ${t.message}")
                emit(emptyList())
            }
    }

    suspend fun syncUserMember(userId: String, member: FirebaseMember) {
        if (userId.isBlank()) return
        val docId = if (member.id.isNotBlank()) member.id else UUID.randomUUID().toString()
        val toSave = member.copy(id = docId, userId = userId, createdAt = Timestamp.now())
        db.collection("users").document(userId).collection("members").document(docId).set(toSave).await()
    }

    suspend fun deleteUserMember(userId: String, memberId: String) {
        if (userId.isBlank() || memberId.isBlank()) return
        db.collection("users").document(userId).collection("members").document(memberId).delete().await()
    }

    // Household & User Categories
    fun observeCategories(householdId: String): Flow<List<FirebaseCategory>> {
        if (householdId.isBlank()) return flowOf(emptyList())
        return db.collection("households").document(householdId)
            .collection("categories")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    try { doc.toSafeCategory() } catch (t: Throwable) {
                        Log.w(TAG, "Error deserializing category ${doc.id}: ${t.message}")
                        null
                    }
                }.sortedWith(compareBy<FirebaseCategory> { it.sortOrder }.thenBy { it.id })
            }
            .catch { t ->
                Log.w(TAG, "observeCategories flow error: ${t.message}")
                emit(emptyList())
            }
    }

    suspend fun syncCategory(householdId: String, category: FirebaseCategory) {
        if (householdId.isBlank()) return
        if (category.userId.isNotBlank()) {
            try { householdDelegate.ensureHouseholdExists(householdId, category.userId) } catch (_: Exception) {}
        }
        val docId = if (category.id.isNotBlank()) category.id else UUID.randomUUID().toString()
        val toSave = category.copy(id = docId, householdId = householdId, createdAt = Timestamp.now())
        db.collection("households").document(householdId).collection("categories").document(docId).set(toSave).await()
    }

    suspend fun deleteCategory(householdId: String, categoryId: String) {
        if (householdId.isBlank() || categoryId.isBlank()) return
        db.collection("households").document(householdId).collection("categories").document(categoryId).delete().await()
    }

    suspend fun setCategoryDeleted(
        householdId: String,
        categoryId: String,
        categoryName: String,
        isDeleted: Boolean,
        categoryType: String = "EXPENSE"
    ) {
        if (householdId.isBlank()) return
        try {
            val catsCol = db.collection("households").document(householdId).collection("categories")
            val targetNameNorm = GreekVoiceParser.normalizeGreek(categoryName.trim().lowercase())
            val targetTypeNorm = categoryType.ifBlank { "EXPENSE" }.uppercase()
            val snap = catsCol.get().await()
            var matched = false
            for (doc in snap.documents) {
                val docName = GreekVoiceParser.normalizeGreek(doc.getString("name").orEmpty().trim().lowercase())
                val docId = doc.getString("id") ?: doc.id
                val docType = doc.getString("categoryType").orEmpty().ifBlank { "EXPENSE" }.uppercase()
                val matchesId = docId == categoryId || doc.id == categoryId
                val matchesNameAndType = targetNameNorm.isNotBlank() && docName == targetNameNorm && docType == targetTypeNorm
                if (matchesId || matchesNameAndType) {
                    doc.reference.update("isDeleted", isDeleted).await()
                    matched = true
                }
            }
            if (!matched && categoryId.isNotBlank()) {
                catsCol.document(categoryId).set(mapOf("isDeleted" to isDeleted, "categoryType" to targetTypeNorm), SetOptions.merge()).await()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Error setting category isDeleted in household $householdId: ${e.message}")
        }
    }

    suspend fun permanentDeleteCategoryExact(
        householdId: String,
        categoryId: String,
        categoryName: String,
        categoryType: String = "EXPENSE"
    ) {
        if (householdId.isBlank()) return
        try {
            val catsCol = db.collection("households").document(householdId).collection("categories")
            val targetNameNorm = GreekVoiceParser.normalizeGreek(categoryName.trim().lowercase())
            val targetTypeNorm = categoryType.ifBlank { "EXPENSE" }.uppercase()
            val snap = catsCol.get().await()
            for (doc in snap.documents) {
                val docName = GreekVoiceParser.normalizeGreek(doc.getString("name").orEmpty().trim().lowercase())
                val docId = doc.getString("id") ?: doc.id
                val docType = doc.getString("categoryType").orEmpty().ifBlank { "EXPENSE" }.uppercase()
                val matchesId = docId == categoryId || doc.id == categoryId
                val matchesNameAndType = targetNameNorm.isNotBlank() && docName == targetNameNorm && docType == targetTypeNorm
                if (matchesId || matchesNameAndType) {
                    doc.reference.delete().await()
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Error permanently deleting category from household $householdId: ${e.message}")
        }
    }

    suspend fun getCategoriesOnce(householdId: String): List<FirebaseCategory> {
        if (householdId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("households").document(householdId).collection("categories").get().await()
            snapshot.documents.mapNotNull { doc ->
                try { doc.toSafeCategory() } catch (_: Throwable) { null }
            }.sortedWith(compareBy<FirebaseCategory> { it.sortOrder }.thenBy { it.id })
        } catch (_: Throwable) {
            emptyList()
        }
    }

    suspend fun getUserCategoriesOnce(userId: String): List<FirebaseCategory> {
        if (userId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("users").document(userId).collection("categories").get().await()
            snapshot.documents.mapNotNull { doc ->
                try { doc.toSafeCategory() } catch (_: Throwable) { null }
            }.sortedWith(compareBy<FirebaseCategory> { it.sortOrder }.thenBy { it.id })
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun observeUserCategories(userId: String): Flow<List<FirebaseCategory>> {
        if (userId.isBlank()) return flowOf(emptyList())
        return db.collection("users").document(userId)
            .collection("categories")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    try { doc.toSafeCategory() } catch (_: Throwable) { null }
                }.sortedWith(compareBy<FirebaseCategory> { it.sortOrder }.thenBy { it.id })
            }
            .catch { t ->
                Log.w(TAG, "observeUserCategories flow error: ${t.message}")
                emit(emptyList())
            }
    }

    suspend fun syncUserCategory(userId: String, category: FirebaseCategory) {
        if (userId.isBlank()) return
        val docId = if (category.id.isNotBlank()) category.id else UUID.randomUUID().toString()
        val toSave = category.copy(id = docId, userId = userId, createdAt = Timestamp.now())
        db.collection("users").document(userId).collection("categories").document(docId).set(toSave).await()
    }

    suspend fun deleteUserCategory(userId: String, categoryId: String) {
        if (userId.isBlank() || categoryId.isBlank()) return
        db.collection("users").document(userId).collection("categories").document(categoryId).delete().await()
    }

    // Savings Goals
    fun observeSavingsGoals(householdId: String): Flow<List<FirebaseSavingsGoal>> {
        if (householdId.isBlank()) return flowOf(emptyList())
        return db.collection("households").document(householdId)
            .collection("savings_goals")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    val obj = try { doc.toSafeSavingsGoal() } catch (_: Throwable) { null }
                    obj?.copy(
                        id = doc.id,
                        householdId = obj.householdId.ifBlank { householdId }
                    )?.takeIf { !it.isDeleted }
                }.sortedWith(compareBy<FirebaseSavingsGoal> { it.title.lowercase() }.thenBy { it.id })
            }
            .catch { t ->
                Log.w(TAG, "observeSavingsGoals flow error: ${t.message}")
                emit(emptyList())
            }
    }

    fun observeUserSavingsGoals(userId: String): Flow<List<FirebaseSavingsGoal>> {
        if (userId.isBlank()) return flowOf(emptyList())
        return db.collection("users").document(userId)
            .collection("savings_goals")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    val obj = try { doc.toSafeSavingsGoal() } catch (_: Throwable) { null }
                    obj?.copy(id = doc.id)?.takeIf { !it.isDeleted }
                }.sortedWith(compareBy<FirebaseSavingsGoal> { it.title.lowercase() }.thenBy { it.id })
            }
            .catch { t ->
                Log.w(TAG, "observeUserSavingsGoals flow error: ${t.message}")
                emit(emptyList())
            }
    }

    suspend fun getSavingsGoalsOnce(householdId: String): List<FirebaseSavingsGoal> {
        if (householdId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("households").document(householdId).collection("savings_goals").get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                try { doc.toSafeSavingsGoal().copy(id = doc.id).takeIf { !it.isDeleted } } catch (_: Throwable) { null }
            }
            if (list.isNotEmpty()) list else {
                val alt = db.collection("households").document(householdId).collection("savingsGoals").get().await()
                alt.documents.mapNotNull { doc ->
                    try { doc.toSafeSavingsGoal().copy(id = doc.id).takeIf { !it.isDeleted } } catch (_: Throwable) { null }
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    suspend fun getUserSavingsGoalsOnce(userId: String): List<FirebaseSavingsGoal> {
        if (userId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("users").document(userId).collection("savings_goals").get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                try { doc.toSafeSavingsGoal().copy(id = doc.id).takeIf { !it.isDeleted } } catch (_: Throwable) { null }
            }
            if (list.isNotEmpty()) list else {
                val alt = db.collection("users").document(userId).collection("savingsGoals").get().await()
                alt.documents.mapNotNull { doc ->
                    try { doc.toSafeSavingsGoal().copy(id = doc.id).takeIf { !it.isDeleted } } catch (_: Throwable) { null }
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    suspend fun syncSavingsGoal(householdId: String, goal: FirebaseSavingsGoal) {
        if (householdId.isBlank()) return
        val docId = if (goal.id.isNotBlank()) goal.id else UUID.randomUUID().toString()
        val toSave = goal.copy(id = docId, householdId = householdId, createdAt = goal.createdAt ?: Timestamp.now())
        db.collection("households").document(householdId).collection("savings_goals").document(docId).set(toSave).await()
    }

    suspend fun syncUserSavingsGoal(userId: String, goal: FirebaseSavingsGoal) {
        if (userId.isBlank()) return
        val docId = if (goal.id.isNotBlank()) goal.id else UUID.randomUUID().toString()
        val toSave = goal.copy(id = docId, userId = userId, createdAt = goal.createdAt ?: Timestamp.now())
        db.collection("users").document(userId).collection("savings_goals").document(docId).set(toSave).await()
    }

    suspend fun deleteSavingsGoal(householdId: String, goalId: String, title: String = "") {
        if (householdId.isBlank()) return
        try {
            val hDoc = db.collection("households").document(householdId)
            val refsToDelete = linkedSetOf<com.google.firebase.firestore.DocumentReference>()
            if (goalId.isNotBlank()) {
                refsToDelete.add(hDoc.collection("savings_goals").document(goalId))
                refsToDelete.add(hDoc.collection("savingsGoals").document(goalId))
                val byIdField = hDoc.collection("savings_goals").whereEqualTo("id", goalId).get().await()
                byIdField.documents.forEach { refsToDelete.add(it.reference) }
            }
            if (title.isNotBlank()) {
                val q1 = hDoc.collection("savings_goals").whereEqualTo("title", title).get().await()
                q1.documents.forEach { refsToDelete.add(it.reference) }
                val q2 = hDoc.collection("savingsGoals").whereEqualTo("title", title).get().await()
                q2.documents.forEach { refsToDelete.add(it.reference) }
            }
            if (refsToDelete.isNotEmpty()) {
                val batch = db.batch()
                refsToDelete.forEach { batch.delete(it) }
                batch.commit().await()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }

    suspend fun deleteUserSavingsGoal(userId: String, goalId: String, title: String = "") {
        if (userId.isBlank()) return
        try {
            val uDoc = db.collection("users").document(userId)
            val refsToDelete = linkedSetOf<com.google.firebase.firestore.DocumentReference>()
            if (goalId.isNotBlank()) {
                refsToDelete.add(uDoc.collection("savings_goals").document(goalId))
                refsToDelete.add(uDoc.collection("savingsGoals").document(goalId))
                val byIdField = uDoc.collection("savings_goals").whereEqualTo("id", goalId).get().await()
                byIdField.documents.forEach { refsToDelete.add(it.reference) }
            }
            if (title.isNotBlank()) {
                val q1 = uDoc.collection("savings_goals").whereEqualTo("title", title).get().await()
                q1.documents.forEach { refsToDelete.add(it.reference) }
                val q2 = uDoc.collection("savingsGoals").whereEqualTo("title", title).get().await()
                q2.documents.forEach { refsToDelete.add(it.reference) }
            }
            if (refsToDelete.isNotEmpty()) {
                val batch = db.batch()
                refsToDelete.forEach { batch.delete(it) }
                batch.commit().await()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }

    // Recurring Expenses
    fun observeRecurringExpenses(householdId: String): Flow<List<FirebaseRecurringExpense>> {
        if (householdId.isBlank()) return flowOf(emptyList())
        return db.collection("households").document(householdId)
            .collection("recurring_expenses")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    val obj = try { doc.toSafeRecurringExpense() } catch (_: Throwable) { null }
                    if (obj != null && obj.id.isBlank()) obj.id = doc.id
                    obj?.takeIf { !it.isDeleted }
                }.sortedBy { it.title }
            }
            .catch { t ->
                Log.w(TAG, "observeRecurringExpenses flow error: ${t.message}")
                emit(emptyList())
            }
    }

    fun observeUserRecurringExpenses(userId: String): Flow<List<FirebaseRecurringExpense>> {
        if (userId.isBlank()) return flowOf(emptyList())
        return db.collection("users").document(userId)
            .collection("recurring_expenses")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    val obj = try { doc.toSafeRecurringExpense() } catch (_: Throwable) { null }
                    if (obj != null && obj.id.isBlank()) obj.id = doc.id
                    obj?.takeIf { !it.isDeleted }
                }.sortedBy { it.title }
            }
            .catch { t ->
                Log.w(TAG, "observeUserRecurringExpenses flow error: ${t.message}")
                emit(emptyList())
            }
    }

    suspend fun getRecurringExpensesOnce(householdId: String): List<FirebaseRecurringExpense> {
        if (householdId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("households").document(householdId).collection("recurring_expenses").get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                try { doc.toSafeRecurringExpense().takeIf { !it.isDeleted } } catch (_: Throwable) { null }
            }
            if (list.isNotEmpty()) list else {
                val alt = db.collection("households").document(householdId).collection("recurringExpenses").get().await()
                alt.documents.mapNotNull { doc ->
                    try { doc.toSafeRecurringExpense().takeIf { !it.isDeleted } } catch (_: Throwable) { null }
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    suspend fun getUserRecurringExpensesOnce(userId: String): List<FirebaseRecurringExpense> {
        if (userId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("users").document(userId).collection("recurring_expenses").get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                try { doc.toSafeRecurringExpense().takeIf { !it.isDeleted } } catch (_: Throwable) { null }
            }
            if (list.isNotEmpty()) list else {
                val alt = db.collection("users").document(userId).collection("recurringExpenses").get().await()
                alt.documents.mapNotNull { doc ->
                    try { doc.toSafeRecurringExpense().takeIf { !it.isDeleted } } catch (_: Throwable) { null }
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    suspend fun archiveAllHouseholdStructure(
        householdId: String,
        userId: String,
        archiveBatchId: String
    ): Int {
        if (householdId.isBlank()) return 0
        var archivedCount = 0
        val archivedAt = Timestamp.now()

        suspend fun archiveNamedStructure(
            colRef: com.google.firebase.firestore.CollectionReference,
            matchHouseholdOnly: Boolean
        ) {
            val snap = colRef.get().await()
            val toArchive = snap.documents.filter { doc ->
                val alreadyDeleted = doc.getBoolean("isDeleted") == true
                val matchesHousehold = !matchHouseholdOnly || doc.getString("householdId") == householdId
                !alreadyDeleted && matchesHousehold
            }
            for (chunk in toArchive.chunked(400)) {
                val batch = db.batch()
                for (doc in chunk) {
                    val hasValidName = !doc.getString("name").isNullOrBlank()
                    val updates: Map<String, Any> = if (hasValidName) {
                        mapOf(
                            "isDeleted" to true,
                            "archivedAt" to archivedAt,
                            "archivedByUid" to userId,
                            "archiveBatchId" to archiveBatchId
                        )
                    } else {
                        mapOf("isDeleted" to true)
                    }
                    batch.update(doc.reference, updates)
                    archivedCount++
                }
                batch.commit().await()
            }
        }

        suspend fun archiveGenericCollection(
            colRef: com.google.firebase.firestore.CollectionReference,
            matchHouseholdOnly: Boolean,
            deactivateRecurring: Boolean = false
        ) {
            val snap = colRef.get().await()
            val toArchive = snap.documents.filter { doc ->
                val alreadyDeleted = doc.getBoolean("isDeleted") == true
                val matchesHousehold = !matchHouseholdOnly || doc.getString("householdId") == householdId
                !alreadyDeleted && matchesHousehold
            }
            for (chunk in toArchive.chunked(400)) {
                val batch = db.batch()
                for (doc in chunk) {
                    val updates = hashMapOf<String, Any>(
                        "isDeleted" to true,
                        "archivedAt" to archivedAt,
                        "archivedByUid" to userId,
                        "archiveBatchId" to archiveBatchId
                    )
                    if (deactivateRecurring) {
                        updates["isActive"] = false
                    }
                    batch.update(doc.reference, updates)
                    archivedCount++
                }
                batch.commit().await()
            }
        }

        try {
            val hDoc = db.collection("households").document(householdId)
            archiveNamedStructure(hDoc.collection("members"), matchHouseholdOnly = false)
            archiveNamedStructure(hDoc.collection("categories"), matchHouseholdOnly = false)
            archiveGenericCollection(hDoc.collection("savings_goals"), matchHouseholdOnly = false)
            archiveGenericCollection(hDoc.collection("savingsGoals"), matchHouseholdOnly = false)
            archiveGenericCollection(hDoc.collection("recurring_expenses"), matchHouseholdOnly = false, deactivateRecurring = true)
            archiveGenericCollection(hDoc.collection("recurringExpenses"), matchHouseholdOnly = false, deactivateRecurring = true)

            if (userId.isNotBlank()) {
                val uDoc = db.collection("users").document(userId)
                archiveNamedStructure(uDoc.collection("members"), matchHouseholdOnly = true)
                archiveNamedStructure(uDoc.collection("categories"), matchHouseholdOnly = true)
                archiveGenericCollection(uDoc.collection("savings_goals"), matchHouseholdOnly = true)
                archiveGenericCollection(uDoc.collection("savingsGoals"), matchHouseholdOnly = true)
                archiveGenericCollection(uDoc.collection("recurring_expenses"), matchHouseholdOnly = true, deactivateRecurring = true)
                archiveGenericCollection(uDoc.collection("recurringExpenses"), matchHouseholdOnly = true, deactivateRecurring = true)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Error archiving household structure: ${e.message}", e)
            throw e
        }
        return archivedCount
    }

    suspend fun restoreArchivedHouseholdStructure(
        householdId: String,
        archiveBatchId: String? = null
    ): Int {
        if (householdId.isBlank()) return 0
        var restoredCount = 0
        val hDoc = db.collection("households").document(householdId)

        suspend fun restoreCollection(colRef: com.google.firebase.firestore.CollectionReference) {
            val snap = colRef.whereEqualTo("isDeleted", true).get().await()
            val toRestore = snap.documents.filter { doc ->
                archiveBatchId.isNullOrBlank() || doc.getString("archiveBatchId") == archiveBatchId
            }
            for (chunk in toRestore.chunked(400)) {
                val batch = db.batch()
                for (doc in chunk) {
                    batch.update(doc.reference, mapOf("isDeleted" to false))
                    restoredCount++
                }
                batch.commit().await()
            }
        }

        restoreCollection(hDoc.collection("members"))
        restoreCollection(hDoc.collection("categories"))
        restoreCollection(hDoc.collection("savings_goals"))
        restoreCollection(hDoc.collection("savingsGoals"))
        restoreCollection(hDoc.collection("recurring_expenses"))
        restoreCollection(hDoc.collection("recurringExpenses"))
        return restoredCount
    }

    suspend fun syncRecurringExpense(householdId: String, recurring: FirebaseRecurringExpense) {
        if (householdId.isBlank()) return
        val docId = if (recurring.id.isNotBlank()) recurring.id else UUID.randomUUID().toString()
        val toSave = recurring.copy(id = docId, householdId = householdId, createdAt = Timestamp.now())
        db.collection("households").document(householdId).collection("recurring_expenses").document(docId).set(toSave).await()
    }

    suspend fun syncUserRecurringExpense(userId: String, recurring: FirebaseRecurringExpense) {
        if (userId.isBlank()) return
        val docId = if (recurring.id.isNotBlank()) recurring.id else UUID.randomUUID().toString()
        val toSave = recurring.copy(id = docId, userId = userId, createdAt = Timestamp.now())
        db.collection("users").document(userId).collection("recurring_expenses").document(docId).set(toSave).await()
    }

    suspend fun deleteRecurringExpense(householdId: String, recurringId: String, title: String = "") {
        if (householdId.isBlank()) return
        try {
            if (recurringId.isNotBlank()) {
                db.collection("households").document(householdId).collection("recurring_expenses").document(recurringId).delete().await()
                db.collection("households").document(householdId).collection("recurringExpenses").document(recurringId).delete().await()
            }
            if (title.isNotBlank()) {
                val q1 = db.collection("households").document(householdId).collection("recurring_expenses").whereEqualTo("title", title).get().await()
                for (doc in q1.documents) doc.reference.delete().await()
                val q2 = db.collection("households").document(householdId).collection("recurringExpenses").whereEqualTo("title", title).get().await()
                for (doc in q2.documents) doc.reference.delete().await()
            }
        } catch (_: Exception) {}
    }

    suspend fun deleteUserRecurringExpense(userId: String, recurringId: String, title: String = "") {
        if (userId.isBlank()) return
        try {
            if (recurringId.isNotBlank()) {
                db.collection("users").document(userId).collection("recurring_expenses").document(recurringId).delete().await()
                db.collection("users").document(userId).collection("recurringExpenses").document(recurringId).delete().await()
            }
            if (title.isNotBlank()) {
                val q1 = db.collection("users").document(userId).collection("recurring_expenses").whereEqualTo("title", title).get().await()
                for (doc in q1.documents) doc.reference.delete().await()
                val q2 = db.collection("users").document(userId).collection("recurringExpenses").whereEqualTo("title", title).get().await()
                for (doc in q2.documents) doc.reference.delete().await()
            }
        } catch (_: Exception) {}
    }

    // Cross-Scope Recovery, Clean & Batch Ordering
    suspend fun getAllMembersAcrossAllScopes(householdIds: List<String>, userId: String): List<FirebaseMember> {
        val result = mutableListOf<FirebaseMember>()
        for (hId in householdIds.filter { it.isNotBlank() }.distinct()) result.addAll(getMembersOnce(hId))
        if (userId.isNotBlank()) result.addAll(getUserMembersOnce(userId))
        return result
    }

    suspend fun syncAndCleanMember(
        householdIds: List<String>,
        userId: String,
        member: FirebaseMember,
        oldName: String = ""
    ) {
        val canonicalDocId = member.id.ifBlank { "1" }
        val toSave = member.copy(id = canonicalDocId, createdAt = Timestamp.now())

        for (hId in householdIds.filter { it.isNotBlank() }.distinct()) {
            try {
                db.collection("households").document(hId)
                    .collection("members").document(canonicalDocId)
                    .set(toSave.copy(householdId = hId)).await()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Error syncing member to household $hId: ${e.message}")
            }
        }
        if (userId.isNotBlank()) {
            try {
                db.collection("users").document(userId)
                    .collection("members").document(canonicalDocId)
                    .set(toSave.copy(userId = userId)).await()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Error syncing member to user $userId: ${e.message}")
            }
        }

        val namesToPurge = mutableListOf<String>()
        if (oldName.isNotBlank() && !oldName.equals(member.name, ignoreCase = true)) {
            namesToPurge.add(oldName.trim().lowercase())
        }

        for (hId in householdIds.filter { it.isNotBlank() }.distinct()) {
            try {
                val snapshot = db.collection("households").document(hId).collection("members").get().await()
                for (doc in snapshot.documents) {
                    if (doc.id == canonicalDocId) continue
                    val remoteName = doc.getString("name").orEmpty().trim().lowercase()
                    val remoteIsDef = doc.getBoolean("isDefault") ?: false
                    if ((namesToPurge.isNotEmpty() && namesToPurge.contains(remoteName)) ||
                        (canonicalDocId == "1" && remoteIsDef) ||
                        (remoteName == member.name.trim().lowercase())
                    ) {
                        doc.reference.delete().await()
                    }
                }
            } catch (_: Exception) {}
        }

        if (userId.isNotBlank()) {
            try {
                val snapshot = db.collection("users").document(userId).collection("members").get().await()
                for (doc in snapshot.documents) {
                    if (doc.id == canonicalDocId) continue
                    val remoteName = doc.getString("name").orEmpty().trim().lowercase()
                    val remoteIsDef = doc.getBoolean("isDefault") ?: false
                    if ((namesToPurge.isNotEmpty() && namesToPurge.contains(remoteName)) ||
                        (canonicalDocId == "1" && remoteIsDef) ||
                        (remoteName == member.name.trim().lowercase())
                    ) {
                        doc.reference.delete().await()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun deleteMemberFromAllScopes(memberId: String, householdIds: List<String>, userId: String, memberName: String = "") {
        if (memberId.isBlank() && memberName.isBlank()) return
        val targetNameNorm = memberName.trim().lowercase()
        if (userId.isNotBlank()) {
            try {
                if (memberId.isNotBlank()) deleteUserMember(userId, memberId)
                if (targetNameNorm.isNotBlank()) {
                    val snap = db.collection("users").document(userId).collection("members").get().await()
                    for (doc in snap.documents) {
                        if (doc.getString("name").orEmpty().trim().lowercase() == targetNameNorm) doc.reference.delete().await()
                    }
                }
            } catch (_: Exception) {}
        }
        for (hId in householdIds.filter { it.isNotBlank() }.distinct()) {
            try {
                if (memberId.isNotBlank()) deleteMember(hId, memberId)
                if (targetNameNorm.isNotBlank()) {
                    val snap = db.collection("households").document(hId).collection("members").get().await()
                    for (doc in snap.documents) {
                        if (doc.getString("name").orEmpty().trim().lowercase() == targetNameNorm) doc.reference.delete().await()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun getAllCategoriesAcrossAllScopes(householdIds: List<String>, userId: String): List<FirebaseCategory> {
        val result = mutableListOf<FirebaseCategory>()
        for (hId in householdIds.filter { it.isNotBlank() }.distinct()) result.addAll(getCategoriesOnce(hId))
        if (userId.isNotBlank()) result.addAll(getUserCategoriesOnce(userId))
        return result
    }

    suspend fun syncAndCleanCategory(
        householdIds: List<String>,
        userId: String,
        category: FirebaseCategory,
        oldName: String = ""
    ) {
        val canonicalDocId = category.id.ifBlank { UUID.randomUUID().toString() }
        val toSave = category.copy(id = canonicalDocId, createdAt = Timestamp.now())

        for (hId in householdIds.filter { it.isNotBlank() }.distinct()) {
            try {
                db.collection("households").document(hId)
                    .collection("categories").document(canonicalDocId)
                    .set(toSave.copy(householdId = hId)).await()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Error syncing category to household $hId: ${e.message}")
            }
        }
        if (userId.isNotBlank()) {
            try {
                db.collection("users").document(userId)
                    .collection("categories").document(canonicalDocId)
                    .set(toSave.copy(userId = userId)).await()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Error syncing category to user $userId: ${e.message}")
            }
        }

        val namesToPurge = mutableListOf<String>()
        if (oldName.isNotBlank() && !oldName.equals(category.name, ignoreCase = true)) {
            namesToPurge.add(oldName.trim().lowercase())
        }

        val targetTypeNorm = category.categoryType.ifBlank { "EXPENSE" }.uppercase()
        for (hId in householdIds.filter { it.isNotBlank() }.distinct()) {
            try {
                val snapshot = db.collection("households").document(hId).collection("categories").get().await()
                for (doc in snapshot.documents) {
                    if (doc.id == canonicalDocId) continue
                    val remoteType = doc.getString("categoryType").orEmpty().ifBlank { "EXPENSE" }.uppercase()
                    if (remoteType != targetTypeNorm) continue
                    val remoteName = doc.getString("name").orEmpty().trim().lowercase()
                    if ((namesToPurge.isNotEmpty() && namesToPurge.contains(remoteName)) ||
                        (remoteName == category.name.trim().lowercase())
                    ) {
                        doc.reference.delete().await()
                    }
                }
            } catch (_: Exception) {}
        }

        if (userId.isNotBlank()) {
            try {
                val snapshot = db.collection("users").document(userId).collection("categories").get().await()
                for (doc in snapshot.documents) {
                    if (doc.id == canonicalDocId) continue
                    val remoteType = doc.getString("categoryType").orEmpty().ifBlank { "EXPENSE" }.uppercase()
                    if (remoteType != targetTypeNorm) continue
                    val remoteName = doc.getString("name").orEmpty().trim().lowercase()
                    if ((namesToPurge.isNotEmpty() && namesToPurge.contains(remoteName)) ||
                        (remoteName == category.name.trim().lowercase())
                    ) {
                        doc.reference.delete().await()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun deleteCategoryFromAllScopes(categoryId: String, householdIds: List<String>, userId: String) {
        if (categoryId.isBlank()) return
        if (userId.isNotBlank()) try { deleteUserCategory(userId, categoryId) } catch (_: Exception) {}
        for (hId in householdIds.filter { it.isNotBlank() }.distinct()) {
            try { deleteCategory(hId, categoryId) } catch (_: Exception) {}
        }
    }

    suspend fun batchUpdateMemberSortOrder(householdIds: List<String>, userId: String, members: List<FirebaseMember>) {
        if (members.isEmpty()) return
        val validHouseholdIds = householdIds.filter { it.isNotBlank() }.distinct()
        try {
            val batch = db.batch()
            for (m in members) {
                val docId = m.id.ifBlank { "1" }
                val updates = mapOf("sortOrder" to m.sortOrder, "updatedAt" to Timestamp.now())
                for (hId in validHouseholdIds) {
                    batch.set(db.collection("households").document(hId).collection("members").document(docId), updates, SetOptions.merge())
                }
                if (userId.isNotBlank()) {
                    batch.set(db.collection("users").document(userId).collection("members").document(docId), updates, SetOptions.merge())
                }
            }
            batch.commit().await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "batchUpdateMemberSortOrder error: ${e.message}")
        }
    }

    suspend fun batchUpdateCategorySortOrder(householdIds: List<String>, userId: String, categories: List<FirebaseCategory>) {
        if (categories.isEmpty()) return
        val validHouseholdIds = householdIds.filter { it.isNotBlank() }.distinct()
        try {
            val batch = db.batch()
            for (c in categories) {
                val docId = c.id.ifBlank { "1" }
                val updates = mapOf("sortOrder" to c.sortOrder, "updatedAt" to Timestamp.now())
                for (hId in validHouseholdIds) {
                    batch.set(db.collection("households").document(hId).collection("categories").document(docId), updates, SetOptions.merge())
                }
                if (userId.isNotBlank()) {
                    batch.set(db.collection("users").document(userId).collection("categories").document(docId), updates, SetOptions.merge())
                }
            }
            batch.commit().await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "batchUpdateCategorySortOrder error: ${e.message}")
        }
    }

    suspend fun fetchAllHouseholdMembers(householdIds: List<String>): List<FirebaseMember> =
        householdIds.filter { it.isNotBlank() }.distinct().flatMap { getMembersOnce(it) }

    suspend fun fetchAllHouseholdCategories(householdIds: List<String>): List<FirebaseCategory> =
        householdIds.filter { it.isNotBlank() }.distinct().flatMap { getCategoriesOnce(it) }

    suspend fun fetchAllHouseholdSavingsGoals(householdIds: List<String>): List<FirebaseSavingsGoal> =
        householdIds.flatMap { getSavingsGoalsOnce(it) }.distinctBy { it.id.ifBlank { it.title } }

    suspend fun fetchAllHouseholdRecurringExpenses(householdIds: List<String>): List<FirebaseRecurringExpense> =
        householdIds.flatMap { getRecurringExpensesOnce(it) }.distinctBy { it.id.ifBlank { it.title } }
}
