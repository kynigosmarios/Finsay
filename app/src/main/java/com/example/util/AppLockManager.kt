package com.example.util

import android.content.Context
import android.os.SystemClock
import androidx.biometric.BiometricManager
import java.security.MessageDigest
import java.security.SecureRandom

object AppLockManager {
    private const val PREFS_NAME = "finsay_app_lock_prefs"
    private const val KEY_ENABLED = "app_lock_enabled"
    private const val KEY_BIOMETRIC_ENABLED = "app_lock_biometric_enabled"
    private const val KEY_PIN_HASH = "app_lock_pin_hash"
    private const val KEY_PIN_SALT = "app_lock_pin_salt"

    // 5-second grace period so camera, photo picker, share sheet, or permission dialogs
    // do not immediately re-lock the user when returning to Finsay.
    private const val RE_LOCK_GRACE_MS = 5_000L

    @Volatile
    private var lastBackgroundElapsedMs: Long = 0L

    @Volatile
    private var unlockedThisSession: Boolean = false

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isLockEnabled(context: Context): Boolean {
        val p = prefs(context)
        return p.getBoolean(KEY_ENABLED, false) && hasPinConfigured(context)
    }

    fun hasPinConfigured(context: Context): Boolean =
        !prefs(context).getString(KEY_PIN_HASH, null).isNullOrBlank()

    fun isBiometricEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, true)

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun canUseBiometrics(context: Context): Boolean {
        return try {
            val manager = BiometricManager.from(context)
            val result = manager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            result == BiometricManager.BIOMETRIC_SUCCESS
        } catch (_: Throwable) {
            false
        }
    }

    fun enableWithPin(context: Context, pin: String, useBiometrics: Boolean = canUseBiometrics(context)) {
        val salt = generateSalt()
        val hash = sha256Hex("$salt:$pin")
        prefs(context).edit()
            .putString(KEY_PIN_SALT, salt)
            .putString(KEY_PIN_HASH, hash)
            .putBoolean(KEY_BIOMETRIC_ENABLED, useBiometrics)
            .putBoolean(KEY_ENABLED, true)
            .apply()
        unlockedThisSession = true
        lastBackgroundElapsedMs = 0L
    }

    fun updatePin(context: Context, newPin: String) {
        val salt = generateSalt()
        val hash = sha256Hex("$salt:$newPin")
        prefs(context).edit()
            .putString(KEY_PIN_SALT, salt)
            .putString(KEY_PIN_HASH, hash)
            .apply()
    }

    fun disableLock(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, false)
            .apply()
        unlockedThisSession = true
        lastBackgroundElapsedMs = 0L
    }

    fun verifyPin(context: Context, candidatePin: String): Boolean {
        val p = prefs(context)
        val salt = p.getString(KEY_PIN_SALT, null) ?: return false
        val expectedHash = p.getString(KEY_PIN_HASH, null) ?: return false
        val actualHash = sha256Hex("$salt:$candidatePin")
        return MessageDigest.isEqual(
            expectedHash.toByteArray(Charsets.UTF_8),
            actualHash.toByteArray(Charsets.UTF_8)
        )
    }

    fun markUnlocked() {
        unlockedThisSession = true
        lastBackgroundElapsedMs = 0L
    }

    fun onAppBackgrounded() {
        lastBackgroundElapsedMs = SystemClock.elapsedRealtime()
    }

    fun shouldShowLockOnLaunch(context: Context): Boolean {
        if (!isLockEnabled(context)) return false
        return !unlockedThisSession
    }

    fun shouldRelockOnResume(context: Context): Boolean {
        if (!isLockEnabled(context)) return false
        if (!unlockedThisSession) return true
        val bgAt = lastBackgroundElapsedMs
        if (bgAt <= 0L) return false
        val elapsed = SystemClock.elapsedRealtime() - bgAt
        if (elapsed >= RE_LOCK_GRACE_MS) {
            unlockedThisSession = false
            return true
        }
        return false
    }

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
