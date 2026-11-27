package com.example.util

import androidx.compose.runtime.compositionLocalOf
import java.util.Locale

data class CurrencyOption(
    val code: String,
    val symbol: String,
    val nameEl: String,
    val nameEn: String
) {
    fun localizedName(lang: Localization.Language): String = Localization.tr(lang, nameEn, nameEl)
}

object CurrencyFormatter {
    const val DEFAULT_CURRENCY_CODE = "EUR"
    const val DEFAULT_CURRENCY_SYMBOL = "€"

    val supportedCurrencies: List<CurrencyOption> = listOf(
        CurrencyOption("EUR", "€", "Ευρώ", "Euro"),
        CurrencyOption("USD", "$", "Δολάριο ΗΠΑ", "US Dollar"),
        CurrencyOption("GBP", "£", "Λίρα Αγγλίας", "British Pound"),
        CurrencyOption("CHF", "CHF", "Ελβετικό Φράγκο", "Swiss Franc"),
        CurrencyOption("RON", "lei", "Λέου Ρουμανίας", "Romanian Leu"),
        CurrencyOption("PLN", "zł", "Ζλότι Πολωνίας", "Polish Zloty"),
        CurrencyOption("BGN", "лв", "Λεβ Βουλγαρίας", "Bulgarian Lev"),
        CurrencyOption("SEK", "kr", "Κορώνα Σουηδίας", "Swedish Krona"),
        CurrencyOption("TRY", "₺", "Τουρκική Λίρα", "Turkish Lira"),
        CurrencyOption("CAD", "C$", "Δολάριο Καναδά", "Canadian Dollar"),
        CurrencyOption("AUD", "A$", "Δολάριο Αυστραλίας", "Australian Dollar"),
        CurrencyOption("JPY", "¥", "Γιεν Ιαπωνίας", "Japanese Yen")
    )

    @Volatile
    var currentCode: String = DEFAULT_CURRENCY_CODE

    @Volatile
    var currentSymbol: String = DEFAULT_CURRENCY_SYMBOL

    fun updateActiveCurrency(codeOrSymbol: String?) {
        val option = optionForCodeOrSymbol(codeOrSymbol)
        currentCode = option.code
        currentSymbol = option.symbol
    }

    fun optionForCode(code: String?): CurrencyOption {
        val normalized = code?.trim()?.uppercase(Locale.ROOT)
        return supportedCurrencies.firstOrNull { it.code == normalized }
            ?: supportedCurrencies.first()
    }

    fun optionForSymbol(symbol: String?): CurrencyOption {
        val normalized = symbol?.trim()
        return supportedCurrencies.firstOrNull { it.symbol.equals(normalized, ignoreCase = true) }
            ?: supportedCurrencies.first()
    }

    fun optionForCodeOrSymbol(value: String?): CurrencyOption {
        if (value.isNullOrBlank()) return supportedCurrencies.first()
        val trimmed = value.trim()
        return supportedCurrencies.firstOrNull { it.code.equals(trimmed, ignoreCase = true) }
            ?: supportedCurrencies.firstOrNull { it.symbol.equals(trimmed, ignoreCase = true) }
            ?: supportedCurrencies.first()
    }

    fun symbolForCode(code: String?): String = optionForCode(code).symbol

    fun format(
        amount: Double,
        symbol: String = currentSymbol,
        decimals: Int = 2,
        signed: Boolean = false
    ): String {
        val pattern = if (signed) "%+.${decimals}f" else "%.${decimals}f"
        val formattedNumber = String.format(Locale.US, pattern, amount)
        return "$formattedNumber$symbol"
    }
}

val LocalCurrencySymbol = compositionLocalOf { CurrencyFormatter.DEFAULT_CURRENCY_SYMBOL }
