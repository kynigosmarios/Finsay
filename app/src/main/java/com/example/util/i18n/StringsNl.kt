package com.example.util.i18n

internal object StringsNl {
    val map: Map<String, String> by lazy {
        buildMap(1300) {
            putAll(StringsNlCore.map)
            putAll(StringsNlUi.map)
            putAll(StringsNlGuideLegal.map)
            put("Did you log today's expenses?", "Heb je de uitgaven van vandaag ingevoerd?")
            put("Type or speak them now so you don't forget 🎙️", "Typ of spreek ze nu in zodat je ze niet vergeet 🎙️")
            put("Quick Expense", "Snelle uitgave")
            put("Quick expense entry", "Snelle uitgave invoeren")
            put("Bank Transaction Detection", "Detectie van banktransacties")
            put("Alerts when new bank transactions are detected for quick logging", "Meldingen wanneer nieuwe banktransacties worden gedetecteerd")
            put("Income detected", "Inkomst gedetecteerd")
            put("Expense detected", "Uitgave gedetecteerd")
            put("year", "jaar")
            put("month", "maand")
            put("Best value • Save vs monthly", "Beste keuze • Bespaar t.o.v. maandelijks")
            put("Upgrade to Pro", "Upgraden naar Pro")
            put("Unlock Lifetime Pro", "Levenslang Pro ontgrendelen")
        }
    }
}
