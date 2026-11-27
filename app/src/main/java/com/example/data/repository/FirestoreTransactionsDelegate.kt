package com.example.data.repository

import android.util.Log
import com.example.data.model.FirebaseExpense
import com.example.data.model.FirebaseHouseholdActivity
import com.example.data.model.FirebaseIncome
import com.example.data.model.FirebaseMonthlyTotal
import com.example.data.model.toSafeExpense
import com.example.data.model.toSafeHouseholdActivity
import com.example.data.model.toSafeIncome
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class FirestoreHistoryPage<T>(
    val items: List<T>,
    val rawDocumentCount: Int,
    val oldestRawTimestampMs: Long
)

internal class FirestoreTransactionsDelegate(
    private val db: FirebaseFirestore,
    private val householdDelegate: FirestoreHouseholdDelegate
) {
    companion object {
        private const val TAG = "FirestoreSyncRepo"
        const val DEFAULT_LIVE_WINDOW_MONTHS = 12
        const val OLDER_HISTORY_PAGE_SIZE = 200L

        /**
         * Returns the start-of-month epoch millis 12 months prior to [nowMs],
         * bounding real-time snapshot listeners to recent history for low FinOps read costs.
         */
        fun defaultLiveWindowCutoffMs(nowMs: Long = System.currentTimeMillis()): Long {
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = nowMs
                add(java.util.Calendar.MONTH, -DEFAULT_LIVE_WINDOW_MONTHS)
                set(java.util.Calendar.DAY_OF_MONTH, 1)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }
    }

    private fun sortExpenses(list: List<FirebaseExpense>): List<FirebaseExpense> =
        list.sortedWith(
            compareByDescending<FirebaseExpense> { it.timestamp }
                .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                .thenByDescending { it.id }
        )

    private fun sortIncomes(list: List<FirebaseIncome>): List<FirebaseIncome> =
        list.sortedWith(
            compareByDescending<FirebaseIncome> { it.timestamp }
                .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                .thenByDescending { it.id }
        )

    fun observeExpenses(
        householdId: String,
        sinceTimestampMs: Long = defaultLiveWindowCutoffMs()
    ): Flow<List<FirebaseExpense>> {
        if (householdId.isBlank()) return flowOf(emptyList())
        val baseCol = db.collection("households").document(householdId).collection("expenses")
        val query = if (sinceTimestampMs > 0L) {
            baseCol.whereGreaterThanOrEqualTo("timestamp", sinceTimestampMs)
        } else {
            baseCol
        }
        return query
            .snapshots()
            .map { querySnapshot ->
                sortExpenses(
                    querySnapshot.documents.mapNotNull { doc ->
                        val obj = try { doc.toSafeExpense() } catch (_: Throwable) { null }
                        if (obj != null) {
                            if (obj.id.isBlank()) obj.id = doc.id
                            if (obj.householdId.isBlank()) obj.householdId = householdId
                        }
                        obj?.takeIf { !it.isDeleted }
                    }
                )
            }
            .catch { t ->
                Log.w(TAG, "observeExpenses flow error (preserving current state): ${t.message}")
            }
    }

    suspend fun fetchOlderExpenses(
        householdId: String,
        beforeTimestampMs: Long,
        limit: Long = OLDER_HISTORY_PAGE_SIZE
    ): FirestoreHistoryPage<FirebaseExpense> {
        if (householdId.isBlank() || beforeTimestampMs <= 0L) {
            return FirestoreHistoryPage(emptyList(), 0, beforeTimestampMs)
        }
        return try {
            val snap = db.collection("households").document(householdId)
                .collection("expenses")
                .whereLessThan("timestamp", beforeTimestampMs)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.coerceIn(1L, 500L))
                .get()
                .await()
            val rawCount = snap.documents.size
            val oldestRawTs = snap.documents.mapNotNull { (it.get("timestamp") as? Number)?.toLong() }
                .filter { it > 0L }
                .minOrNull() ?: beforeTimestampMs
            val activeItems = sortExpenses(
                snap.documents.mapNotNull { doc ->
                    val obj = try { doc.toSafeExpense() } catch (_: Throwable) { null }
                    if (obj != null) {
                        if (obj.id.isBlank()) obj.id = doc.id
                        if (obj.householdId.isBlank()) obj.householdId = householdId
                    }
                    obj?.takeIf { !it.isDeleted }
                }
            )
            FirestoreHistoryPage(activeItems, rawCount, oldestRawTs)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "fetchOlderExpenses warning: ${e.message}")
            FirestoreHistoryPage(emptyList(), 0, beforeTimestampMs)
        }
    }

    suspend fun syncExpense(expense: FirebaseExpense) {
        if (expense.householdId.isBlank()) return
        if (expense.createdByUid.isNotBlank()) {
            try { householdDelegate.ensureHouseholdExists(expense.householdId, expense.createdByUid) } catch (_: Exception) {}
        }
        val docId = if (expense.id.isNotBlank()) expense.id else UUID.randomUUID().toString()
        val expenseToSave = expense.copy(id = docId, createdAt = expense.createdAt ?: Timestamp.now())
        db.collection("households").document(expense.householdId)
            .collection("expenses").document(docId)
            .set(expenseToSave)
            .await()
    }

    suspend fun syncExpensesBatch(householdId: String, expenses: List<FirebaseExpense>) {
        if (householdId.isBlank() || expenses.isEmpty()) return
        val firstCreator = expenses.firstOrNull { it.createdByUid.isNotBlank() }?.createdByUid.orEmpty()
        if (firstCreator.isNotBlank()) {
            try { householdDelegate.ensureHouseholdExists(householdId, firstCreator) } catch (_: Exception) {}
        }
        val col = db.collection("households").document(householdId).collection("expenses")
        for (chunk in expenses.chunked(400)) {
            val batch = db.batch()
            chunk.forEach { expense ->
                val docId = if (expense.id.isNotBlank()) expense.id else UUID.randomUUID().toString()
                val expenseToSave = expense.copy(
                    id = docId,
                    householdId = householdId,
                    createdAt = expense.createdAt ?: Timestamp.now()
                )
                batch.set(col.document(docId), expenseToSave)
            }
            batch.commit().await()
        }
    }

    suspend fun syncUserExpensesBatch(userId: String, expenses: List<FirebaseExpense>) {
        if (userId.isBlank() || expenses.isEmpty()) return
        val col = db.collection("users").document(userId).collection("expenses")
        for (chunk in expenses.chunked(400)) {
            val batch = db.batch()
            chunk.forEach { expense ->
                val docId = if (expense.id.isNotBlank()) expense.id else UUID.randomUUID().toString()
                val expenseToSave = expense.copy(id = docId, createdAt = expense.createdAt ?: Timestamp.now())
                batch.set(col.document(docId), expenseToSave)
            }
            batch.commit().await()
        }
    }

    suspend fun deleteExpense(householdId: String, expenseId: String) {
        if (householdId.isBlank() || expenseId.isBlank()) return
        db.collection("households").document(householdId)
            .collection("expenses").document(expenseId)
            .delete()
            .await()
    }

    fun observeUserExpenses(userId: String): Flow<List<FirebaseExpense>> {
        if (userId.isBlank()) return flowOf(emptyList())
        return db.collection("users").document(userId)
            .collection("expenses")
            .snapshots()
            .map { querySnapshot ->
                sortExpenses(
                    querySnapshot.documents.mapNotNull { doc ->
                        val obj = try { doc.toSafeExpense() } catch (_: Throwable) { null }
                        if (obj != null && obj.id.isBlank()) obj.id = doc.id
                        obj?.takeIf { !it.isDeleted }
                    }
                )
            }
            .catch { t ->
                Log.w(TAG, "observeUserExpenses flow error (preserving current state): ${t.message}")
            }
    }

    suspend fun syncUserExpense(userId: String, expense: FirebaseExpense) {
        if (userId.isBlank()) return
        val docId = if (expense.id.isNotBlank()) expense.id else UUID.randomUUID().toString()
        val expenseToSave = expense.copy(id = docId, createdAt = expense.createdAt ?: Timestamp.now())
        db.collection("users").document(userId)
            .collection("expenses").document(docId)
            .set(expenseToSave)
            .await()
    }

    suspend fun deleteUserExpense(userId: String, expenseId: String) {
        if (userId.isBlank() || expenseId.isBlank()) return
        db.collection("users").document(userId)
            .collection("expenses").document(expenseId)
            .delete()
            .await()
    }

    suspend fun clearAllExpenses(householdId: String, userId: String) {
        try {
            if (householdId.isNotBlank()) {
                val docs = db.collection("households").document(householdId).collection("expenses").get().await()
                for (doc in docs.documents) doc.reference.delete().await()
            }
            if (userId.isNotBlank() && householdId.isNotBlank()) {
                val docs = db.collection("users").document(userId).collection("expenses").get().await()
                for (doc in docs.documents) {
                    if (doc.getString("householdId") == householdId) {
                        doc.reference.delete().await()
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Error clearing cloud expenses", e)
        }
    }

    suspend fun archiveAllHouseholdTransactions(
        householdId: String,
        userId: String,
        archiveBatchId: String
    ): Int {
        if (householdId.isBlank()) return 0
        var archivedCount = 0
        val archivedAt = Timestamp.now()

        suspend fun archiveCollection(colRef: com.google.firebase.firestore.CollectionReference, matchHouseholdOnly: Boolean) {
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
                    if (doc.get("title") !is String) {
                        updates["title"] = doc.getString("title") ?: ""
                    }
                    val rawAmt = (doc.get("amount") as? Number)?.toDouble()
                    if (rawAmt == null || rawAmt < 0.0) {
                        updates["amount"] = (rawAmt ?: 0.0).coerceAtLeast(0.0)
                    }
                    batch.update(doc.reference, updates)
                    archivedCount++
                }
                batch.commit().await()
            }
        }

        try {
            val hDoc = db.collection("households").document(householdId)
            archiveCollection(hDoc.collection("expenses"), matchHouseholdOnly = false)
            archiveCollection(hDoc.collection("incomes"), matchHouseholdOnly = false)
            if (userId.isNotBlank()) {
                val uDoc = db.collection("users").document(userId)
                archiveCollection(uDoc.collection("expenses"), matchHouseholdOnly = true)
                archiveCollection(uDoc.collection("incomes"), matchHouseholdOnly = true)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Error archiving household transactions: ${e.message}", e)
            throw e
        }
        return archivedCount
    }

    suspend fun restoreArchivedHouseholdTransactions(
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

        restoreCollection(hDoc.collection("expenses"))
        restoreCollection(hDoc.collection("incomes"))
        return restoredCount
    }

    fun observeIncomes(
        householdId: String,
        sinceTimestampMs: Long = defaultLiveWindowCutoffMs()
    ): Flow<List<FirebaseIncome>> {
        if (householdId.isBlank()) return flowOf(emptyList())
        val baseCol = db.collection("households").document(householdId).collection("incomes")
        val query = if (sinceTimestampMs > 0L) {
            baseCol.whereGreaterThanOrEqualTo("timestamp", sinceTimestampMs)
        } else {
            baseCol
        }
        return query
            .snapshots()
            .map { querySnapshot ->
                sortIncomes(
                    querySnapshot.documents.mapNotNull { doc ->
                        val obj = try { doc.toSafeIncome() } catch (_: Throwable) { null }
                        if (obj != null) {
                            if (obj.id.isBlank()) obj.id = doc.id
                            if (obj.householdId.isBlank()) obj.householdId = householdId
                        }
                        obj?.takeIf { !it.isDeleted }
                    }
                )
            }
            .catch { t ->
                Log.w(TAG, "observeIncomes flow error (preserving current state): ${t.message}")
            }
    }

    suspend fun fetchOlderIncomes(
        householdId: String,
        beforeTimestampMs: Long,
        limit: Long = OLDER_HISTORY_PAGE_SIZE
    ): FirestoreHistoryPage<FirebaseIncome> {
        if (householdId.isBlank() || beforeTimestampMs <= 0L) {
            return FirestoreHistoryPage(emptyList(), 0, beforeTimestampMs)
        }
        return try {
            val snap = db.collection("households").document(householdId)
                .collection("incomes")
                .whereLessThan("timestamp", beforeTimestampMs)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.coerceIn(1L, 500L))
                .get()
                .await()
            val rawCount = snap.documents.size
            val oldestRawTs = snap.documents.mapNotNull { (it.get("timestamp") as? Number)?.toLong() }
                .filter { it > 0L }
                .minOrNull() ?: beforeTimestampMs
            val activeItems = sortIncomes(
                snap.documents.mapNotNull { doc ->
                    val obj = try { doc.toSafeIncome() } catch (_: Throwable) { null }
                    if (obj != null) {
                        if (obj.id.isBlank()) obj.id = doc.id
                        if (obj.householdId.isBlank()) obj.householdId = householdId
                    }
                    obj?.takeIf { !it.isDeleted }
                }
            )
            FirestoreHistoryPage(activeItems, rawCount, oldestRawTs)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "fetchOlderIncomes warning: ${e.message}")
            FirestoreHistoryPage(emptyList(), 0, beforeTimestampMs)
        }
    }

    suspend fun upsertMonthlyTotal(householdId: String, summary: FirebaseMonthlyTotal) {
        if (householdId.isBlank() || summary.monthKey.isBlank()) return
        try {
            db.collection("households").document(householdId)
                .collection("monthly_totals").document(summary.monthKey)
                .set(summary.copy(updatedAt = System.currentTimeMillis()))
                .await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.d(TAG, "upsertMonthlyTotal skipped: ${e.message}")
        }
    }

    suspend fun getMonthlyTotalsOnce(householdId: String): List<FirebaseMonthlyTotal> {
        if (householdId.isBlank()) return emptyList()
        return try {
            val snap = db.collection("households").document(householdId)
                .collection("monthly_totals")
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                try {
                    FirebaseMonthlyTotal(
                        monthKey = doc.getString("monthKey")?.ifBlank { doc.id } ?: doc.id,
                        expenseTotal = (doc.get("expenseTotal") as? Number)?.toDouble() ?: 0.0,
                        incomeTotal = (doc.get("incomeTotal") as? Number)?.toDouble() ?: 0.0,
                        expenseCount = (doc.get("expenseCount") as? Number)?.toInt() ?: 0,
                        incomeCount = (doc.get("incomeCount") as? Number)?.toInt() ?: 0,
                        updatedAt = (doc.get("updatedAt") as? Number)?.toLong() ?: 0L
                    )
                } catch (_: Throwable) {
                    null
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.d(TAG, "getMonthlyTotalsOnce skipped: ${e.message}")
            emptyList()
        }
    }

    suspend fun syncIncome(income: FirebaseIncome) {
        if (income.householdId.isBlank()) return
        if (income.createdByUid.isNotBlank()) {
            try { householdDelegate.ensureHouseholdExists(income.householdId, income.createdByUid) } catch (_: Exception) {}
        }
        val docId = if (income.id.isNotBlank()) income.id else UUID.randomUUID().toString()
        val incomeToSave = income.copy(id = docId, createdAt = income.createdAt ?: Timestamp.now())
        db.collection("households").document(income.householdId)
            .collection("incomes").document(docId)
            .set(incomeToSave)
            .await()
    }

    suspend fun deleteIncome(householdId: String, incomeId: String) {
        if (householdId.isBlank() || incomeId.isBlank()) return
        db.collection("households").document(householdId)
            .collection("incomes").document(incomeId)
            .delete()
            .await()
    }

    fun observeUserIncomes(userId: String): Flow<List<FirebaseIncome>> {
        if (userId.isBlank()) return flowOf(emptyList())
        return db.collection("users").document(userId)
            .collection("incomes")
            .snapshots()
            .map { querySnapshot ->
                sortIncomes(
                    querySnapshot.documents.mapNotNull { doc ->
                        val obj = try { doc.toSafeIncome() } catch (_: Throwable) { null }
                        if (obj != null && obj.id.isBlank()) obj.id = doc.id
                        obj?.takeIf { !it.isDeleted }
                    }
                )
            }
            .catch { t ->
                Log.w(TAG, "observeUserIncomes flow error (preserving current state): ${t.message}")
            }
    }

    suspend fun syncUserIncome(userId: String, income: FirebaseIncome) {
        if (userId.isBlank()) return
        val docId = if (income.id.isNotBlank()) income.id else UUID.randomUUID().toString()
        val incomeToSave = income.copy(id = docId, createdAt = income.createdAt ?: Timestamp.now())
        db.collection("users").document(userId)
            .collection("incomes").document(docId)
            .set(incomeToSave)
            .await()
    }

    suspend fun deleteUserIncome(userId: String, incomeId: String) {
        if (userId.isBlank() || incomeId.isBlank()) return
        db.collection("users").document(userId)
            .collection("incomes").document(incomeId)
            .delete()
            .await()
    }

    suspend fun enableNetwork() {
        try {
            db.enableNetwork().await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "enableNetwork warning: ${e.message}")
        }
    }

    suspend fun getExpensesFromCache(householdId: String): List<FirebaseExpense> {
        if (householdId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("households").document(householdId)
                .collection("expenses").get(Source.CACHE).await()
            sortExpenses(
                snapshot.documents.mapNotNull { doc ->
                    val obj = try { doc.toSafeExpense() } catch (_: Throwable) { null }
                    if (obj != null) {
                        if (obj.id.isBlank()) obj.id = doc.id
                        if (obj.householdId.isBlank()) obj.householdId = householdId
                    }
                    obj?.takeIf { !it.isDeleted }
                }
            )
        } catch (_: Throwable) {
            emptyList()
        }
    }

    suspend fun getIncomesFromCache(householdId: String): List<FirebaseIncome> {
        if (householdId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("households").document(householdId)
                .collection("incomes").get(Source.CACHE).await()
            sortIncomes(
                snapshot.documents.mapNotNull { doc ->
                    val obj = try { doc.toSafeIncome() } catch (_: Throwable) { null }
                    if (obj != null) {
                        if (obj.id.isBlank()) obj.id = doc.id
                        if (obj.householdId.isBlank()) obj.householdId = householdId
                    }
                    obj?.takeIf { !it.isDeleted }
                }
            )
        } catch (_: Throwable) {
            emptyList()
        }
    }

    suspend fun getExpensesOnceOrNull(householdId: String): List<FirebaseExpense>? {
        if (householdId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("households").document(householdId).collection("expenses").get().await()
            sortExpenses(
                snapshot.documents.mapNotNull { doc ->
                    val obj = try { doc.toSafeExpense() } catch (_: Throwable) { null }
                    if (obj != null) {
                        if (obj.id.isBlank()) obj.id = doc.id
                        if (obj.householdId.isBlank()) obj.householdId = householdId
                    }
                    obj?.takeIf { !it.isDeleted }
                }
            )
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.w(TAG, "Error fetching expenses once", t)
            null
        }
    }

    suspend fun getIncomesOnceOrNull(householdId: String): List<FirebaseIncome>? {
        if (householdId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("households").document(householdId).collection("incomes").get().await()
            sortIncomes(
                snapshot.documents.mapNotNull { doc ->
                    val obj = try { doc.toSafeIncome() } catch (_: Throwable) { null }
                    if (obj != null) {
                        if (obj.id.isBlank()) obj.id = doc.id
                        if (obj.householdId.isBlank()) obj.householdId = householdId
                    }
                    obj?.takeIf { !it.isDeleted }
                }
            )
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.w(TAG, "Error fetching incomes once", t)
            null
        }
    }

    suspend fun getUserExpensesOnce(userId: String): List<FirebaseExpense> {
        if (userId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("users").document(userId).collection("expenses").get().await()
            sortExpenses(
                snapshot.documents.mapNotNull { doc ->
                    val obj = try { doc.toSafeExpense() } catch (_: Throwable) { null }
                    if (obj != null && obj.id.isBlank()) obj.id = doc.id
                    obj?.takeIf { !it.isDeleted }
                }
            )
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.w(TAG, "Error fetching user expenses once", t)
            emptyList()
        }
    }

    suspend fun getUserIncomesOnce(userId: String): List<FirebaseIncome> {
        if (userId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("users").document(userId).collection("incomes").get().await()
            sortIncomes(
                snapshot.documents.mapNotNull { doc ->
                    val obj = try { doc.toSafeIncome() } catch (_: Throwable) { null }
                    if (obj != null && obj.id.isBlank()) obj.id = doc.id
                    obj?.takeIf { !it.isDeleted }
                }
            )
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.w(TAG, "Error fetching user incomes once", t)
            emptyList()
        }
    }

    suspend fun fetchAllHouseholdExpenses(householdIds: List<String>): List<FirebaseExpense> {
        val result = mutableListOf<FirebaseExpense>()
        for (hId in householdIds.filter { it.isNotBlank() }.distinct()) {
            result.addAll(getExpensesOnceOrNull(hId) ?: emptyList())
        }
        return result.distinctBy { it.id.ifBlank { "${it.title}_${it.timestamp}_${it.amount}" } }
    }

    suspend fun fetchAllHouseholdIncomes(householdIds: List<String>): List<FirebaseIncome> {
        val result = mutableListOf<FirebaseIncome>()
        for (hId in householdIds.filter { it.isNotBlank() }.distinct()) {
            result.addAll(getIncomesOnceOrNull(hId) ?: emptyList())
        }
        return result.distinctBy { it.id.ifBlank { "${it.title}_${it.timestamp}_${it.amount}" } }
    }

    suspend fun getAllExpensesAcrossAllScopes(householdIds: List<String>, userId: String): List<FirebaseExpense> {
        val result = mutableListOf<FirebaseExpense>()
        for (hId in householdIds.filter { it.isNotBlank() }.distinct()) {
            result.addAll(getExpensesOnceOrNull(hId) ?: emptyList())
        }
        if (userId.isNotBlank()) result.addAll(getUserExpensesOnce(userId))
        return sortExpenses(result.distinctBy { it.id.ifBlank { "${it.title}_${it.timestamp}_${it.amount}" } })
    }

    suspend fun getAllIncomesAcrossAllScopes(householdIds: List<String>, userId: String): List<FirebaseIncome> {
        val result = mutableListOf<FirebaseIncome>()
        for (hId in householdIds.filter { it.isNotBlank() }.distinct()) {
            result.addAll(getIncomesOnceOrNull(hId) ?: emptyList())
        }
        if (userId.isNotBlank()) result.addAll(getUserIncomesOnce(userId))
        return sortIncomes(result.distinctBy { it.id.ifBlank { "${it.title}_${it.timestamp}_${it.amount}" } })
    }

    // 3.1: Shared Household Activity Feed (append-only)
    suspend fun logHouseholdActivity(householdId: String, activity: FirebaseHouseholdActivity) {
        if (householdId.isBlank() || activity.actorUid.isBlank() ||
            activity.actionType.isBlank() || activity.summaryEl.isBlank()
        ) return
        val docId = if (activity.id.isNotBlank()) activity.id else UUID.randomUUID().toString()
        val createdMs = if (activity.createdAt > 0L) activity.createdAt else System.currentTimeMillis()
        val payload = hashMapOf<String, Any>(
            "id" to docId,
            "householdId" to householdId,
            "actorUid" to activity.actorUid,
            "actorName" to activity.actorName,
            "actionType" to activity.actionType,
            "targetType" to activity.targetType,
            "summaryEl" to activity.summaryEl,
            "summaryEn" to activity.summaryEn,
            "amount" to activity.amount,
            "createdAt" to createdMs
        )
        try {
            db.collection("households").document(householdId)
                .collection("activity").document(docId)
                .set(payload)
                .await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "logHouseholdActivity warning: ${e.message}")
        }
    }

    fun observeHouseholdActivity(householdId: String, limit: Long = 50L): Flow<List<FirebaseHouseholdActivity>> {
        if (householdId.isBlank()) return flowOf(emptyList())
        return db.collection("households").document(householdId)
            .collection("activity")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    val obj = try { doc.toSafeHouseholdActivity() } catch (_: Throwable) { null }
                    if (obj != null && obj.id.isBlank()) obj.id = doc.id
                    obj
                }
            }
            .catch { t ->
                Log.w(TAG, "observeHouseholdActivity flow error: ${t.message}")
                emit(emptyList())
            }
    }
}
