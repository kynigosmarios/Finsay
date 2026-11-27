package com.example.util.i18n

internal object StringsIt {
    val map: Map<String, String> by lazy {
        buildMap(1300) {
            putAll(StringsItCore.map)
            putAll(StringsItUi.map)
            putAll(StringsItGuideLegal.map)
            put("Did you log today's expenses?", "Hai registrato le spese di oggi?")
            put("Type or speak them now so you don't forget 🎙️", "Scrivile o dettale ora per non dimenticarle 🎙️")
            put("Quick Expense", "Spesa rapida")
            put("Quick expense entry", "Inserimento rapido spesa")
            put("Bank Transaction Detection", "Rilevamento transazioni bancarie")
            put("Alerts when new bank transactions are detected for quick logging", "Avvisi quando vengono rilevate nuove transazioni bancarie")
            put("Income detected", "Entrata rilevata")
            put("Expense detected", "Spesa rilevata")
            put("year", "anno")
            put("month", "mese")
            put("Best value • Save vs monthly", "Miglior valore • Risparmia rispetto al mensile")
            put("Upgrade to Pro", "Passa a Pro")
            put("Unlock Lifetime Pro", "Sblocca Pro a vita")
        }
    }
}
