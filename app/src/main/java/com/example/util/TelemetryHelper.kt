package com.example.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

/**
 * Lightweight, fail-safe wrapper around Firebase Crashlytics and Firebase Performance Monitoring.
 * Never logs PII (no emails, no user names, no transaction titles/amounts) and safely no-ops
 * in local JVM / Robolectric tests where FirebaseApp is not initialized.
 */
object TelemetryHelper {
    private const val TAG = "TelemetryHelper"

    fun setContextKeys(
        appLanguage: String? = null,
        isPro: Boolean? = null,
        householdCount: Int? = null
    ) {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            appLanguage?.let { crashlytics.setCustomKey("app_language", it) }
            isPro?.let { crashlytics.setCustomKey("is_pro", it) }
            householdCount?.let { crashlytics.setCustomKey("household_count", it) }
        } catch (t: Throwable) {
            Log.d(TAG, "Crashlytics setContextKeys skipped: ${t.message}")
        }
    }

    private val emailRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")

    private fun sanitizeException(throwable: Throwable): Throwable {
        val rawMsg = throwable.message ?: return throwable
        if (!emailRegex.containsMatchIn(rawMsg)) return throwable
        val sanitizedMsg = emailRegex.replace(rawMsg, "[redacted-email]")
        return RuntimeException("${throwable.javaClass.simpleName}: $sanitizedMsg").apply {
            stackTrace = throwable.stackTrace
        }
    }

    fun recordNonFatal(tag: String, throwable: Throwable) {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.log("Non-fatal in $tag: ${throwable.javaClass.simpleName}")
            crashlytics.recordException(sanitizeException(throwable))
        } catch (t: Throwable) {
            Log.d(TAG, "Crashlytics recordNonFatal skipped: ${t.message}")
        }
    }

    fun startTrace(name: String): Trace? {
        return try {
            FirebasePerformance.getInstance().newTrace(name).apply { start() }
        } catch (t: Throwable) {
            Log.d(TAG, "Performance trace '$name' skipped: ${t.message}")
            null
        }
    }

    fun stopTrace(trace: Trace?, metrics: Map<String, Long> = emptyMap()) {
        if (trace == null) return
        try {
            metrics.forEach { (k, v) -> trace.putMetric(k, v) }
            trace.stop()
        } catch (t: Throwable) {
            Log.d(TAG, "Performance stopTrace skipped: ${t.message}")
        }
    }
}
