package com.example.util.i18n

internal object StringsPt {
    val map: Map<String, String> by lazy {
        buildMap(1300) {
            putAll(StringsPtCore.map)
            putAll(StringsPtUi.map)
            putAll(StringsPtGuideLegal.map)
            put("Did you log today's expenses?", "Registou as despesas de hoje?")
            put("Type or speak them now so you don't forget 🎙️", "Escreva ou dite agora para não se esquecer 🎙️")
            put("Quick Expense", "Despesa rápida")
            put("Quick expense entry", "Registo rápido de despesa")
            put("Bank Transaction Detection", "Deteção de transações bancárias")
            put("Alerts when new bank transactions are detected for quick logging", "Alertas quando novas transações bancárias são detetadas")
            put("Income detected", "Receita detetada")
            put("Expense detected", "Despesa detetada")
            put("year", "ano")
            put("month", "mês")
            put("Best value • Save vs monthly", "Melhor valor • Poupe face ao mensal")
            put("Upgrade to Pro", "Atualizar para Pro")
            put("Unlock Lifetime Pro", "Desbloquear Pro Vitalício")
        }
    }
}
