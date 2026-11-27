package com.example.util.i18n

internal object StringsEs {
    val map: Map<String, String> by lazy {
        buildMap(1300) {
            putAll(StringsEsCore.map)
            putAll(StringsEsUi.map)
            putAll(StringsEsGuideLegal.map)
            put("Did you log today's expenses?", "¿Registraste los gastos de hoy?")
            put("Type or speak them now so you don't forget 🎙️", "Escríbelos o díctalos ahora para no olvidarlos 🎙️")
            put("Quick Expense", "Gasto rápido")
            put("Quick expense entry", "Registro rápido de gasto")
            put("Bank Transaction Detection", "Detección de transacciones bancarias")
            put("Alerts when new bank transactions are detected for quick logging", "Alertas cuando se detectan nuevas transacciones bancarias para registrarlas rápido")
            put("Income detected", "Ingreso detectado")
            put("Expense detected", "Gasto detectado")
            put("year", "año")
            put("month", "mes")
            put("Best value • Save vs monthly", "Mejor valor • Ahorra frente al plan mensual")
            put("Upgrade to Pro", "Mejorar a Pro")
            put("Unlock Lifetime Pro", "Obtener Pro de por vida")
        }
    }
}
