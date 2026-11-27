package com.example.util.i18n

internal object StringsDe {
    val map: Map<String, String> by lazy {
        buildMap(1300) {
            putAll(StringsDeCore.map)
            putAll(StringsDeUi.map)
            putAll(StringsDeGuideLegal.map)
            put("Did you log today's expenses?", "Hast du deine heutigen Ausgaben erfasst?")
            put("Type or speak them now so you don't forget 🎙️", "Tippe oder sprich sie jetzt ein, damit du sie nicht vergisst 🎙️")
            put("Quick Expense", "Schnelle Ausgabe")
            put("Quick expense entry", "Schnelle Ausgabenerfassung")
            put("Bank Transaction Detection", "Erkennung von Banktransaktionen")
            put("Alerts when new bank transactions are detected for quick logging", "Benachrichtigungen bei neuen Banktransaktionen zur schnellen Erfassung")
            put("Income detected", "Einnahme erkannt")
            put("Expense detected", "Ausgabe erkannt")
            put("year", "Jahr")
            put("month", "Monat")
            put("Best value • Save vs monthly", "Bestes Angebot • Sparen ggü. monatlich")
            put("Upgrade to Pro", "Auf Pro upgraden")
            put("Unlock Lifetime Pro", "Lifetime Pro freischalten")
        }
    }
}
