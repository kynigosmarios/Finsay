package com.example.ui.components

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.ui.theme.Emerald700
import com.example.util.AppLockManager
import com.example.util.Localization

@Composable
fun AppLockScreen(
    appLanguage: String,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val lang = Localization.Language.fromCode(appLanguage)
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val canUseBiometrics = remember(context) {
        AppLockManager.isBiometricEnabled(context) && AppLockManager.canUseBiometrics(context)
    }

    val triggerBiometric: () -> Unit = remember(context, lang, onUnlocked) {
        {
            val activity = context as? FragmentActivity
            if (activity != null && AppLockManager.canUseBiometrics(context)) {
                try {
                    val executor = ContextCompat.getMainExecutor(activity)
                    val prompt = BiometricPrompt(
                        activity,
                        executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                AppLockManager.markUnlocked()
                                onUnlocked()
                            }
                        }
                    )
                    val info = BiometricPrompt.PromptInfo.Builder()
                        .setTitle(Localization.tr(lang, "Unlock Finsay", "Ξεκλείδωμα Finsay"))
                        .setSubtitle(
                            Localization.tr(
                                lang,
                                "Verify your identity to access your finances",
                                "Επιβεβαιώστε την ταυτότητά σας για πρόσβαση"
                            )
                        )
                        .setNegativeButtonText(Localization.tr(lang, "Use PIN", "Χρήση PIN"))
                        .setAllowedAuthenticators(
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.BIOMETRIC_WEAK
                        )
                        .build()
                    prompt.authenticate(info)
                } catch (_: Throwable) {
                    // User can still unlock via 4-digit PIN
                }
            }
        }
    }

    LaunchedEffect(canUseBiometrics) {
        if (canUseBiometrics) {
            triggerBiometric()
        }
    }

    fun onDigitPressed(digit: String) {
        if (enteredPin.length >= 4) return
        isError = false
        val next = enteredPin + digit
        enteredPin = next
        if (next.length == 4) {
            if (AppLockManager.verifyPin(context, next)) {
                AppLockManager.markUnlocked()
                onUnlocked()
            } else {
                isError = true
                enteredPin = ""
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_lock_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Brand Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Emerald700.copy(alpha = 0.12f))
                        .border(1.dp, Emerald700.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.finsay_logo),
                        contentDescription = "Finsay",
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = Localization.tr(lang, "Finsay is locked", "Το Finsay είναι κλειδωμένο"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isError) {
                        Localization.tr(lang, "Incorrect PIN. Please try again.", "Λάθος PIN. Δοκιμάστε ξανά.")
                    } else {
                        Localization.tr(lang, "Enter your 4-digit PIN to continue", "Εισάγετε το 4-ψήφιο PIN σας")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 4-dot PIN indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val filled = index < enteredPin.length
                        val dotColor by animateColorAsState(
                            targetValue = when {
                                isError -> MaterialTheme.colorScheme.error
                                filled -> Emerald700
                                else -> MaterialTheme.colorScheme.outlineVariant
                            },
                            label = "pin_dot_$index"
                        )
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (filled) dotColor else Color.Transparent)
                                .border(2.dp, dotColor, CircleShape)
                        )
                    }
                }
            }

            // Numeric Keypad
            PinKeypad(
                showBiometricKey = canUseBiometrics,
                onDigit = ::onDigitPressed,
                onBackspace = {
                    if (enteredPin.isNotEmpty()) {
                        enteredPin = enteredPin.dropLast(1)
                        isError = false
                    }
                },
                onBiometricClick = triggerBiometric
            )
        }
    }
}

@Composable
fun AppLockPinSetupDialog(
    appLanguage: String,
    isChangingExisting: Boolean = false,
    onPinConfirmed: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val lang = Localization.Language.fromCode(appLanguage)
    var step by remember { mutableIntStateOf(0) } // 0 = enter new PIN, 1 = confirm PIN
    var firstPin by remember { mutableStateOf("") }
    var currentInput by remember { mutableStateOf("") }
    var mismatchError by remember { mutableStateOf(false) }

    fun handleDigit(digit: String) {
        if (currentInput.length >= 4) return
        mismatchError = false
        val next = currentInput + digit
        currentInput = next
        if (next.length == 4) {
            if (step == 0) {
                firstPin = next
                currentInput = ""
                step = 1
            } else {
                if (next == firstPin) {
                    onPinConfirmed(next)
                } else {
                    mismatchError = true
                    currentInput = ""
                    firstPin = ""
                    step = 0
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 420.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Emerald700.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Emerald700,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when {
                        step == 1 -> Localization.tr(lang, "Confirm 4-digit PIN", "Επιβεβαίωση 4-ψήφιου PIN")
                        isChangingExisting -> Localization.tr(lang, "Set New PIN", "Ορισμός Νέου PIN")
                        else -> Localization.tr(lang, "Create 4-digit PIN", "Δημιουργία 4-ψήφιου PIN")
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when {
                        mismatchError -> Localization.tr(lang, "PINs did not match. Try again.", "Τα PIN δεν ταιριάζουν. Δοκιμάστε ξανά.")
                        step == 1 -> Localization.tr(lang, "Re-enter your PIN to confirm", "Πληκτρολογήστε ξανά το PIN σας")
                        else -> Localization.tr(
                            lang,
                            "Used as fallback alongside Fingerprint / Face ID",
                            "Χρησιμοποιείται μαζί με Δακτυλικό Αποτύπωμα / Face ID"
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (mismatchError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val filled = index < currentInput.length
                        val color = when {
                            mismatchError -> MaterialTheme.colorScheme.error
                            filled -> Emerald700
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (filled) color else Color.Transparent)
                                .border(2.dp, color, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                PinKeypad(
                    showBiometricKey = false,
                    compact = true,
                    onDigit = ::handleDigit,
                    onBackspace = {
                        if (currentInput.isNotEmpty()) {
                            currentInput = currentInput.dropLast(1)
                            mismatchError = false
                        }
                    },
                    onBiometricClick = {}
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("app_lock_pin_cancel")
                ) {
                    Text(
                        text = Localization.cancel(lang),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PinKeypad(
    showBiometricKey: Boolean,
    compact: Boolean = false,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onBiometricClick: () -> Unit
) {
    val keySize = if (compact) 62.dp else 72.dp
    val rowSpacing = if (compact) 12.dp else 16.dp
    val colSpacing = if (compact) 20.dp else 26.dp

    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(rowSpacing)
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(colSpacing)) {
                row.forEach { digit ->
                    PinDigitButton(
                        digit = digit,
                        size = keySize,
                        onClick = { onDigit(digit) }
                    )
                }
            }
        }

        // Bottom row: Biometric | 0 | Backspace
        Row(
            horizontalArrangement = Arrangement.spacedBy(colSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBiometricKey) {
                Surface(
                    modifier = Modifier
                        .size(keySize)
                        .clip(CircleShape)
                        .clickable(onClick = onBiometricClick)
                        .testTag("app_lock_biometric_btn"),
                    shape = CircleShape,
                    color = Emerald700.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric unlock",
                            tint = Emerald700,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(keySize))
            }

            PinDigitButton(
                digit = "0",
                size = keySize,
                onClick = { onDigit("0") }
            )

            Surface(
                modifier = Modifier
                    .size(keySize)
                    .clip(CircleShape)
                    .clickable(onClick = onBackspace)
                    .testTag("app_lock_backspace_btn"),
                shape = CircleShape,
                color = Color.Transparent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Delete digit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PinDigitButton(
    digit: String,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("pin_key_$digit"),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
