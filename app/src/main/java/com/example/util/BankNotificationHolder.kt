package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object BankNotificationHolder {
    private val _pendingTransactions = MutableStateFlow<List<PendingBankTransaction>>(emptyList())
    val pendingTransactions: StateFlow<List<PendingBankTransaction>> = _pendingTransactions.asStateFlow()

    var onTransactionRemoved: ((String) -> Unit)? = null

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences("bank_notif_holder_prefs", Context.MODE_PRIVATE)
            loadFromPrefs()
        }
    }

    fun reloadFromPrefs(context: Context? = null) {
        if (context != null && prefs == null) {
            init(context)
        }
        loadFromPrefs()
    }

    fun resolveActiveHouseholdId(context: Context): String {
        return try {
            context.applicationContext
                .getSharedPreferences("oikonomos_app_prefs", Context.MODE_PRIVATE)
                .getString("last_household_id", "")
                .orEmpty()
                .trim()
        } catch (_: Exception) {
            ""
        }
    }

    fun filterForHousehold(
        transactions: List<PendingBankTransaction>,
        activeHouseholdId: String
    ): List<PendingBankTransaction> {
        val hid = activeHouseholdId.trim()
        if (hid.isBlank()) return emptyList()
        return transactions.filter { it.householdId.trim() == hid }
    }

    fun loadFromPrefs() {
        val p = prefs ?: return
        val raw = p.getString("pending_tx_json", null) ?: return
        try {
            val array = JSONArray(raw)
            val list = mutableListOf<PendingBankTransaction>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PendingBankTransaction(
                        id = obj.getString("id"),
                        bankName = obj.getString("bankName"),
                        merchantName = obj.getString("merchantName"),
                        amount = obj.getDouble("amount"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        rawText = obj.optString("rawText", ""),
                        householdId = obj.optString("householdId", ""),
                        isIncome = obj.optBoolean("isIncome", false)
                    )
                )
            }
            _pendingTransactions.value = list
        } catch (_: Exception) {}
    }

    private fun saveToPrefs() {
        val p = prefs ?: return
        val list = _pendingTransactions.value
        try {
            val array = JSONArray()
            for (tx in list) {
                val obj = JSONObject()
                obj.put("id", tx.id)
                obj.put("bankName", tx.bankName)
                obj.put("merchantName", tx.merchantName)
                obj.put("amount", tx.amount)
                obj.put("timestamp", tx.timestamp)
                obj.put("rawText", tx.rawText)
                obj.put("householdId", tx.householdId)
                obj.put("isIncome", tx.isIncome)
                array.put(obj)
            }
            p.edit().putString("pending_tx_json", array.toString()).apply()
        } catch (_: Exception) {}
    }

    fun isSignatureHandled(sig: String, context: Context? = null): Boolean {
        if (prefs == null && context != null) {
            init(context)
        }
        val set = prefs?.getStringSet("handled_notif_sigs", emptySet()) ?: emptySet()
        return set.contains(sig)
    }

    fun markSignatureHandled(sig: String, context: Context? = null) {
        if (prefs == null && context != null) {
            init(context)
        }
        val p = prefs ?: return
        val existing = p.getStringSet("handled_notif_sigs", emptySet())?.toMutableList() ?: mutableListOf()
        if (!existing.contains(sig)) {
            existing.add(sig)
            val trimmed = if (existing.size > 200) existing.takeLast(200) else existing
            p.edit().putStringSet("handled_notif_sigs", trimmed.toSet()).apply()
        }
    }

    fun addTransaction(tx: PendingBankTransaction, context: Context? = null): Boolean {
        if (prefs == null && context != null) {
            init(context)
        }
        val effectiveHid = tx.householdId.trim().ifBlank {
            if (context != null) resolveActiveHouseholdId(context) else ""
        }
        val stampedTx = if (effectiveHid != tx.householdId) tx.copy(householdId = effectiveHid) else tx

        val current = _pendingTransactions.value.toMutableList()

        // Deduplicate both identical notifications and cross-app duplicates within the same household
        val duplicateIndex = current.indexOfFirst { existing ->
            val sameHousehold = existing.householdId.trim() == stampedTx.householdId.trim()
            val sameType = existing.isIncome == stampedTx.isIncome
            val sameAmount = kotlin.math.abs(existing.amount - stampedTx.amount) < 0.01
            val closeInTime = kotlin.math.abs(existing.timestamp - stampedTx.timestamp) < 120_000L
            val sameMerchant = existing.merchantName.equals(stampedTx.merchantName, ignoreCase = true) ||
                existing.merchantName.contains(stampedTx.merchantName, ignoreCase = true) ||
                stampedTx.merchantName.contains(existing.merchantName, ignoreCase = true)
            val crossWalletAndBank = (existing.bankName == "Google Pay" && stampedTx.bankName != "Google Pay") ||
                (stampedTx.bankName == "Google Pay" && existing.bankName != "Google Pay")
            sameHousehold && sameType && sameAmount && closeInTime && (sameMerchant || crossWalletAndBank)
        }

        if (duplicateIndex >= 0) {
            val existing = current[duplicateIndex]
            val existingIsGeneric = existing.merchantName.startsWith("Συναλλαγή ") || existing.merchantName.startsWith("Πίστωση ")
            val newIsRicher = !stampedTx.merchantName.startsWith("Συναλλαγή ") &&
                !stampedTx.merchantName.startsWith("Πίστωση ") &&
                (existingIsGeneric || (stampedTx.bankName == "Google Pay" && existing.merchantName.length < stampedTx.merchantName.length))
            if (newIsRicher) {
                current[duplicateIndex] = existing.copy(merchantName = stampedTx.merchantName)
                _pendingTransactions.value = current
                saveToPrefs()
            }
            return false
        }

        current.add(0, stampedTx)
        _pendingTransactions.value = current.sortedByDescending { it.timestamp }.take(15)
        saveToPrefs()
        return true
    }

    fun removeTransaction(id: String) {
        val current = _pendingTransactions.value.toMutableList()
        val removed = current.removeAll { it.id == id }
        if (removed) {
            _pendingTransactions.value = current
            saveToPrefs()
            onTransactionRemoved?.invoke(id)
        }
    }

    fun clearAll() {
        val list = _pendingTransactions.value
        _pendingTransactions.value = emptyList()
        saveToPrefs()
        list.forEach { tx -> onTransactionRemoved?.invoke(tx.id) }
    }

    fun wipeLocalStore(context: Context) {
        clearAll()
        try {
            val targetPrefs = prefs ?: context.applicationContext.getSharedPreferences("bank_notif_holder_prefs", Context.MODE_PRIVATE)
            targetPrefs.edit().clear().apply()
        } catch (_: Exception) {}
    }
}
