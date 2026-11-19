package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.entity.ExpenseCategory
import java.util.Locale

data class LearnedCategorySuggestion(
    val categoryId: Long,
    val categoryName: String
)

/**
 * Persists learned (householdId, normalized merchant/title or AFM) -> (categoryId, categoryName)
 * mappings locally in SharedPreferences so future receipt scans and bank auto-catcher entries
 * automatically inherit the user's preferred category.
 */
object MerchantCategoryLearner {
    private const val PREFS_NAME = "finsay_merchant_category_learner"
    private const val KEY_MERCHANT_PREFIX = "m|"
    private const val KEY_AFM_PREFIX = "a|"

    private fun getPrefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun normalizeMerchantKey(raw: String): String {
        if (raw.isBlank()) return ""
        val cleaned = GreekVoiceParser.normalizeGreek(raw.trim().lowercase(Locale.ROOT))
            .replace(Regex("[^\\p{L}\\p{Nd}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleaned.length < 2) return ""
        // Drop pure numbers at the end such as branch IDs or receipt numbers
        return cleaned.replace(Regex("\\s+#?\\d{2,}$"), "").trim()
    }

    fun normalizeAfm(rawAfm: String): String {
        val digits = rawAfm.filter { it.isDigit() }
        return if (digits.length == 9) digits else ""
    }

    fun learn(
        context: Context,
        householdId: String,
        merchantOrTitle: String,
        categoryId: Long,
        categoryName: String,
        merchantAfm: String = ""
    ) {
        if (categoryName.isBlank() || categoryId <= 0L) return
        val scope = householdId.ifBlank { "default" }
        val encodedValue = "$categoryId|$categoryName"
        val prefs = getPrefs(context)
        val editor = prefs.edit()

        val normTitle = normalizeMerchantKey(merchantOrTitle)
        if (normTitle.length >= 2) {
            editor.putString("$KEY_MERCHANT_PREFIX$scope|$normTitle", encodedValue)
        }
        val cleanAfm = normalizeAfm(merchantAfm)
        if (cleanAfm.isNotEmpty()) {
            editor.putString("$KEY_AFM_PREFIX$scope|$cleanAfm", encodedValue)
        }
        editor.apply()
    }

    fun suggest(
        context: Context,
        householdId: String,
        merchantOrTitle: String,
        merchantAfm: String = "",
        availableCategories: List<ExpenseCategory>
    ): LearnedCategorySuggestion? {
        if (availableCategories.isEmpty()) return null
        val scope = householdId.ifBlank { "default" }
        val prefs = getPrefs(context)

        val cleanAfm = normalizeAfm(merchantAfm)
        if (cleanAfm.isNotEmpty()) {
            decodeAndResolve(prefs.getString("$KEY_AFM_PREFIX$scope|$cleanAfm", null), availableCategories)
                ?.let { return it }
        }

        val normTitle = normalizeMerchantKey(merchantOrTitle)
        if (normTitle.length >= 2) {
            decodeAndResolve(prefs.getString("$KEY_MERCHANT_PREFIX$scope|$normTitle", null), availableCategories)
                ?.let { return it }

            // Prefix/substring fallback across saved keys in same household scope
            val prefix = "$KEY_MERCHANT_PREFIX$scope|"
            val allEntries = prefs.all
            val bestMatch = allEntries.entries
                .asSequence()
                .filter { it.key.startsWith(prefix) }
                .mapNotNull { entry ->
                    val savedMerchant = entry.key.removePrefix(prefix)
                    if (savedMerchant.length >= 4 &&
                        (normTitle.contains(savedMerchant) || savedMerchant.contains(normTitle))
                    ) {
                        savedMerchant.length to (entry.value as? String)
                    } else null
                }
                .maxByOrNull { it.first }
                ?.second

            decodeAndResolve(bestMatch, availableCategories)?.let { return it }
        }

        return null
    }

    private fun decodeAndResolve(
        rawValue: String?,
        availableCategories: List<ExpenseCategory>
    ): LearnedCategorySuggestion? {
        if (rawValue.isNullOrBlank()) return null
        val parts = rawValue.split("|", limit = 2)
        if (parts.size != 2) return null
        val savedId = parts[0].toLongOrNull() ?: 0L
        val savedName = parts[1].trim()

        val matched = availableCategories.find { it.id == savedId }
            ?: availableCategories.find { it.name.equals(savedName, ignoreCase = true) }
            ?: return null

        return LearnedCategorySuggestion(
            categoryId = matched.id,
            categoryName = matched.name
        )
    }
}
