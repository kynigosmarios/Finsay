package com.example.util.i18n

internal object StringsRo {
    val map: Map<String, String> by lazy {
        buildMap(1300) {
            putAll(StringsRoCore.map)
            putAll(StringsRoUi.map)
            putAll(StringsRoGuideLegal.map)
            put("Did you log today's expenses?", "Ai înregistrat cheltuielile de astăzi?")
            put("Type or speak them now so you don't forget 🎙️", "Scrie-le sau dictează-le acum ca să nu uiți 🎙️")
            put("Quick Expense", "Cheltuială rapidă")
            put("Quick expense entry", "Adăugare rapidă cheltuială")
            put("Bank Transaction Detection", "Detectare tranzacții bancare")
            put("Alerts when new bank transactions are detected for quick logging", "Alerte când sunt detectate noi tranzacții bancare")
            put("Income detected", "Venit detectat")
            put("Expense detected", "Cheltuială detectată")
            put("year", "an")
            put("month", "lună")
            put("Best value • Save vs monthly", "Cea mai bună ofertă • Economisești față de lunar")
            put("Upgrade to Pro", "Treci la Pro")
            put("Unlock Lifetime Pro", "Deblochează Pro pe viață")
        }
    }
}
