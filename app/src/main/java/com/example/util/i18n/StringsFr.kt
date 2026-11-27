package com.example.util.i18n

internal object StringsFr {
    val map: Map<String, String> by lazy {
        buildMap(1300) {
            putAll(StringsFrCore.map)
            putAll(StringsFrUi.map)
            putAll(StringsFrGuideLegal.map)
            put("Did you log today's expenses?", "Avez-vous enregistré vos dépenses du jour ?")
            put("Type or speak them now so you don't forget 🎙️", "Saisissez-les ou dictez-les maintenant pour ne pas les oublier 🎙️")
            put("Quick Expense", "Dépense rapide")
            put("Quick expense entry", "Saisie rapide de dépense")
            put("Bank Transaction Detection", "Détection des transactions bancaires")
            put("Alerts when new bank transactions are detected for quick logging", "Alertes lors de la détection de nouvelles transactions bancaires")
            put("Income detected", "Revenu détecté")
            put("Expense detected", "Dépense détectée")
            put("year", "an")
            put("month", "mois")
            put("Best value • Save vs monthly", "Meilleur choix • Économisez par rapport au mensuel")
            put("Upgrade to Pro", "Passer à Pro")
            put("Unlock Lifetime Pro", "Débloquer Pro à vie")
        }
    }
}
