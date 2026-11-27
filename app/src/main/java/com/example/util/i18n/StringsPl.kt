package com.example.util.i18n

internal object StringsPl {
    val map: Map<String, String> by lazy {
        buildMap(1300) {
            putAll(StringsPlCore.map)
            putAll(StringsPlUi.map)
            putAll(StringsPlGuideLegal.map)
            put("Did you log today's expenses?", "Czy zapisałeś dzisiejsze wydatki?")
            put("Type or speak them now so you don't forget 🎙️", "Wpisz lub podyktuj je teraz, aby nie zapomnieć 🎙️")
            put("Quick Expense", "Szybki wydatek")
            put("Quick expense entry", "Szybkie dodawanie wydatku")
            put("Bank Transaction Detection", "Wykrywanie transakcji bankowych")
            put("Alerts when new bank transactions are detected for quick logging", "Powiadomienia o wykrytych nowych transakcjach bankowych")
            put("Income detected", "Wykryto przychód")
            put("Expense detected", "Wykryto wydatek")
            put("year", "rok")
            put("month", "mies.")
            put("Best value • Save vs monthly", "Najlepsza oferta • Oszczędzaj vs miesięczny")
            put("Upgrade to Pro", "Przejdź na Pro")
            put("Unlock Lifetime Pro", "Odblokuj Pro na zawsze")
        }
    }
}
